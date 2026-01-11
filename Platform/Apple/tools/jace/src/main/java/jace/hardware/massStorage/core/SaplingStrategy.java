/**
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.hardware.massStorage.core;

import java.io.IOException;
import java.util.Arrays;

/**
 * SAPLING storage strategy: index block + data blocks (513-131072 bytes, max 256 blocks).
 * The key block points to an index block which contains pointers to up to 256 data blocks.
 */
public class SaplingStrategy implements FileStorageStrategy {

    private static final int MAX_DATA_BLOCKS = 256;

    @Override
    public StorageType getStorageType() {
        return StorageType.SAPLING;
    }

    @Override
    public long getMaxFileSize() {
        return MAX_DATA_BLOCKS * ProDOSConstants.BLOCK_SIZE;
    }

    @Override
    public byte[] readFile(BlockReader reader, int keyBlock, long fileSize) throws IOException {
        if (fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + fileSize);
        }
        if (fileSize > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "File size exceeds SAPLING maximum: " + fileSize + " > " + getMaxFileSize());
        }

        // Read index block
        byte[] indexBlock = reader.readBlock(keyBlock);

        // Calculate number of data blocks needed
        int dataBlockCount = (int) ((fileSize + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);

        // Allocate output buffer
        byte[] result = new byte[(int) fileSize];

        // Read each data block
        for (int i = 0; i < dataBlockCount; i++) {
            // Get block number from index (little-endian 16-bit)
            int blockNumber = (indexBlock[i] & 0xFF) | ((indexBlock[256 + i] & 0xFF) << 8);

            // Read data block
            byte[] dataBlock = reader.readBlock(blockNumber);

            // Calculate how many bytes to copy from this block
            int bytesToCopy = (int) Math.min(ProDOSConstants.BLOCK_SIZE,
                                             fileSize - (i * ProDOSConstants.BLOCK_SIZE));

            // Copy data to result
            System.arraycopy(dataBlock, 0, result, i * ProDOSConstants.BLOCK_SIZE, bytesToCopy);
        }

        return result;
    }

    @Override
    public int writeFile(BlockWriter writer, byte[] data) throws IOException {
        if (data.length > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "Data size exceeds SAPLING maximum: " + data.length + " > " + getMaxFileSize());
        }

        // Calculate number of data blocks needed
        int dataBlockCount = (data.length + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;
        if (dataBlockCount == 0) {
            dataBlockCount = 1; // At least one block
        }

        // Allocate index block
        int indexBlock = writer.allocateBlock();

        // Allocate data blocks and build index
        byte[] index = new byte[ProDOSConstants.BLOCK_SIZE];
        int[] dataBlocks = new int[dataBlockCount];

        for (int i = 0; i < dataBlockCount; i++) {
            dataBlocks[i] = writer.allocateBlock();

            // Write block number to index (little-endian)
            index[i] = (byte) (dataBlocks[i] & 0xFF);
            index[256 + i] = (byte) ((dataBlocks[i] >> 8) & 0xFF);
        }

        // Write index block
        writer.writeBlock(indexBlock, index);

        // Write data blocks
        for (int i = 0; i < dataBlockCount; i++) {
            byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];

            // Calculate how many bytes to copy to this block
            int offset = i * ProDOSConstants.BLOCK_SIZE;
            int bytesToCopy = Math.min(ProDOSConstants.BLOCK_SIZE, data.length - offset);

            // Copy data and zero-pad if necessary
            System.arraycopy(data, offset, blockData, 0, bytesToCopy);

            writer.writeBlock(dataBlocks[i], blockData);
        }

        return indexBlock;
    }

    @Override
    public int calculateBlocksNeeded(long fileSize) {
        // Calculate data blocks
        int dataBlocks = (int) ((fileSize + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);
        if (dataBlocks == 0) {
            dataBlocks = 1;
        }

        // Add 1 for index block
        return dataBlocks + 1;
    }
}
