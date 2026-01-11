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
 * Strategy interface for ProDOS file storage types.
 * Each storage type (SEEDLING, SAPLING, TREE) has different block allocation
 * and access patterns.
 */
public interface FileStorageStrategy {
    /**
     * Gets the storage type handled by this strategy.
     *
     * @return the StorageType (SEEDLING, SAPLING, or TREE)
     */
    StorageType getStorageType();

    /**
     * Gets the maximum file size this strategy can handle.
     *
     * @return the maximum file size in bytes
     */
    long getMaxFileSize();

    /**
     * Reads a file from disk using this storage strategy.
     *
     * @param reader the block reader for accessing disk blocks
     * @param keyBlock the key block (starting block) of the file
     * @param fileSize the size of the file in bytes (from directory entry)
     * @return the complete file data
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if parameters are invalid
     */
    byte[] readFile(BlockReader reader, int keyBlock, long fileSize) throws IOException;

    /**
     * Writes a file to disk using this storage strategy.
     *
     * @param writer the block writer for accessing disk blocks and allocation
     * @param data the file data to write
     * @return the key block number where the file was written
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if data exceeds max file size for this strategy
     * @throws IllegalStateException if insufficient free blocks are available
     */
    int writeFile(BlockWriter writer, byte[] data) throws IOException;

    /**
     * Calculates the number of blocks required to store a file of given size.
     *
     * @param fileSize the file size in bytes
     * @return the total number of blocks needed (including index blocks)
     */
    int calculateBlocksNeeded(long fileSize);
}
