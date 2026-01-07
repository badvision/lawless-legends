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

/**
 * TREE storage strategy: master index + sub-indexes + data blocks (>131072 bytes).
 * The key block points to a master index block which contains pointers to up to 256 sub-index blocks.
 * Each sub-index block contains pointers to up to 256 data blocks.
 * Maximum file size: 256 * 256 * 512 = 33,554,432 bytes (32 MB).
 *
 * This implementation is extracted from proven production code in FileNode.java lines 192-205.
 */
public class TreeStrategy implements FileStorageStrategy {

    private static final int MAX_SUB_INDEXES = 256;
    private static final int MAX_DATA_BLOCKS_PER_SUB_INDEX = 256;
    private static final long MIN_TREE_SIZE = (256L * 512) + 1; // Larger than SAPLING max

    @Override
    public StorageType getStorageType() {
        return StorageType.TREE;
    }

    @Override
    public long getMaxFileSize() {
        return MAX_SUB_INDEXES * MAX_DATA_BLOCKS_PER_SUB_INDEX * ProDOSConstants.BLOCK_SIZE;
    }

    @Override
    public byte[] readFile(BlockReader reader, int keyBlock, long fileSize) throws IOException {
        if (fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + fileSize);
        }
        if (fileSize > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "File size exceeds TREE maximum: " + fileSize + " > " + getMaxFileSize());
        }

        // Read master index block
        byte[] masterIndex = reader.readBlock(keyBlock);

        // Calculate number of data blocks needed
        int totalDataBlocks = (int) ((fileSize + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);

        // Allocate output buffer
        byte[] result = new byte[(int) fileSize];

        int dataBlocksRead = 0;

        // Iterate through sub-indexes
        for (int subIndexNum = 0; subIndexNum < MAX_SUB_INDEXES && dataBlocksRead < totalDataBlocks; subIndexNum++) {
            // Get sub-index block number from master index (little-endian 16-bit)
            int subIndexBlock = (masterIndex[subIndexNum] & 0xFF) | ((masterIndex[256 + subIndexNum] & 0xFF) << 8);

            // Read sub-index block
            byte[] subIndex = reader.readBlock(subIndexBlock);

            // Read data blocks from this sub-index
            for (int dataIndexInSubIndex = 0; dataIndexInSubIndex < MAX_DATA_BLOCKS_PER_SUB_INDEX && dataBlocksRead < totalDataBlocks; dataIndexInSubIndex++) {
                // Get data block number from sub-index (little-endian 16-bit)
                int dataBlockNumber = (subIndex[dataIndexInSubIndex] & 0xFF) | ((subIndex[256 + dataIndexInSubIndex] & 0xFF) << 8);

                // Read data block
                byte[] dataBlock = reader.readBlock(dataBlockNumber);

                // Calculate how many bytes to copy from this block
                long bytesRemaining = fileSize - (dataBlocksRead * ProDOSConstants.BLOCK_SIZE);
                int bytesToCopy = (int) Math.min(ProDOSConstants.BLOCK_SIZE, bytesRemaining);

                // Copy data to result
                System.arraycopy(dataBlock, 0, result, dataBlocksRead * ProDOSConstants.BLOCK_SIZE, bytesToCopy);

                dataBlocksRead++;
            }
        }

        return result;
    }

    @Override
    public int writeFile(BlockWriter writer, byte[] data) throws IOException {
        if (data.length < MIN_TREE_SIZE) {
            throw new IllegalArgumentException(
                "File size too small for TREE storage: " + data.length + " < " + MIN_TREE_SIZE +
                " (use SAPLING instead)");
        }
        if (data.length > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "Data size exceeds TREE maximum: " + data.length + " > " + getMaxFileSize());
        }

        // Calculate number of data blocks needed
        int totalDataBlocks = (data.length + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;

        // Calculate number of sub-indexes needed
        int subIndexCount = (totalDataBlocks + MAX_DATA_BLOCKS_PER_SUB_INDEX - 1) / MAX_DATA_BLOCKS_PER_SUB_INDEX;

        // Allocate master index block
        int masterIndexBlock = writer.allocateBlock();
        byte[] masterIndex = new byte[ProDOSConstants.BLOCK_SIZE];

        // Allocate all sub-index blocks
        int[] subIndexBlocks = new int[subIndexCount];
        for (int i = 0; i < subIndexCount; i++) {
            subIndexBlocks[i] = writer.allocateBlock();

            // Write sub-index block number to master index (little-endian)
            masterIndex[i] = (byte) (subIndexBlocks[i] & 0xFF);
            masterIndex[256 + i] = (byte) ((subIndexBlocks[i] >> 8) & 0xFF);
        }

        // Write master index block
        writer.writeBlock(masterIndexBlock, masterIndex);

        // Write data blocks and build sub-indexes
        int dataBlocksWritten = 0;

        for (int subIndexNum = 0; subIndexNum < subIndexCount; subIndexNum++) {
            byte[] subIndex = new byte[ProDOSConstants.BLOCK_SIZE];

            // Calculate how many data blocks this sub-index will reference
            int dataBlocksInThisSubIndex = Math.min(MAX_DATA_BLOCKS_PER_SUB_INDEX, totalDataBlocks - dataBlocksWritten);

            // Allocate and write data blocks for this sub-index
            for (int dataIndexInSubIndex = 0; dataIndexInSubIndex < dataBlocksInThisSubIndex; dataIndexInSubIndex++) {
                // Allocate data block
                int dataBlockNumber = writer.allocateBlock();

                // Write data block number to sub-index (little-endian)
                subIndex[dataIndexInSubIndex] = (byte) (dataBlockNumber & 0xFF);
                subIndex[256 + dataIndexInSubIndex] = (byte) ((dataBlockNumber >> 8) & 0xFF);

                // Prepare data block
                byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
                int offset = dataBlocksWritten * ProDOSConstants.BLOCK_SIZE;
                int bytesToCopy = Math.min(ProDOSConstants.BLOCK_SIZE, data.length - offset);

                // Copy data and zero-pad if necessary
                System.arraycopy(data, offset, blockData, 0, bytesToCopy);

                // Write data block
                writer.writeBlock(dataBlockNumber, blockData);

                dataBlocksWritten++;
            }

            // Write sub-index block
            writer.writeBlock(subIndexBlocks[subIndexNum], subIndex);
        }

        return masterIndexBlock;
    }

    @Override
    public int calculateBlocksNeeded(long fileSize) {
        // Calculate data blocks
        int dataBlocks = (int) ((fileSize + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);

        // Calculate sub-indexes needed
        int subIndexes = (dataBlocks + MAX_DATA_BLOCKS_PER_SUB_INDEX - 1) / MAX_DATA_BLOCKS_PER_SUB_INDEX;

        // Total: 1 master index + N sub-indexes + M data blocks
        return 1 + subIndexes + dataBlocks;
    }
}
