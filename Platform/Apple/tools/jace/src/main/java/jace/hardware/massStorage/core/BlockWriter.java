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
 * Interface for writing blocks to ProDOS storage.
 * Provides abstraction over physical disk access and block allocation.
 */
public interface BlockWriter extends BlockReader {
    /**
     * Writes a single 512-byte block to the disk.
     *
     * @param blockNumber the block number to write (0-based)
     * @param data the 512-byte array to write
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if blockNumber is negative, exceeds disk capacity,
     *                                  or data is not exactly 512 bytes
     */
    void writeBlock(int blockNumber, byte[] data) throws IOException;

    /**
     * Allocates a free block from the disk and marks it as used.
     *
     * @return the block number of the newly allocated block
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if no free blocks are available
     */
    int allocateBlock() throws IOException;

    /**
     * Frees a previously allocated block, marking it as available.
     *
     * @param blockNumber the block number to free
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if blockNumber is invalid
     */
    void freeBlock(int blockNumber) throws IOException;

    /**
     * Gets the number of free blocks available on the disk.
     *
     * @return the count of free blocks
     */
    int getFreeBlockCount();
}
