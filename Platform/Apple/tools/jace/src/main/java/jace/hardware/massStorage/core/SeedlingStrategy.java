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
 * SEEDLING storage strategy: single data block (0-512 bytes).
 * The key block points directly to the file data.
 */
public class SeedlingStrategy implements FileStorageStrategy {

    @Override
    public StorageType getStorageType() {
        return StorageType.SEEDLING;
    }

    @Override
    public long getMaxFileSize() {
        return ProDOSConstants.BLOCK_SIZE;
    }

    @Override
    public byte[] readFile(BlockReader reader, int keyBlock, long fileSize) throws IOException {
        if (fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + fileSize);
        }
        if (fileSize > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "File size exceeds SEEDLING maximum: " + fileSize + " > " + getMaxFileSize());
        }

        if (fileSize == 0) {
            return new byte[0];
        }

        byte[] blockData = reader.readBlock(keyBlock);
        return Arrays.copyOf(blockData, (int) fileSize);
    }

    @Override
    public int writeFile(BlockWriter writer, byte[] data) throws IOException {
        if (data.length > getMaxFileSize()) {
            throw new IllegalArgumentException(
                "Data size exceeds SEEDLING maximum: " + data.length + " > " + getMaxFileSize());
        }

        int keyBlock = writer.allocateBlock();

        // Create 512-byte block with data and zero padding
        byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
        System.arraycopy(data, 0, blockData, 0, data.length);

        writer.writeBlock(keyBlock, blockData);

        return keyBlock;
    }

    @Override
    public int calculateBlocksNeeded(long fileSize) {
        // SEEDLING always uses exactly 1 data block
        return 1;
    }
}
