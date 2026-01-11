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
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of BlockWriter for testing.
 * Provides in-memory block storage with configurable capacity.
 */
public class MockBlockWriter implements BlockWriter {
    private final Map<Integer, byte[]> blocks = new HashMap<>();
    private int nextBlock = 0;
    private final int freeBlockCount;
    private final int totalBlocks;

    /**
     * Creates a MockBlockWriter with default capacity.
     * Default: 100,000 free blocks out of 160,000 total.
     */
    public MockBlockWriter() {
        this(100000, 160000);
    }

    /**
     * Creates a MockBlockWriter with specified capacity.
     *
     * @param freeBlockCount Number of free blocks available
     * @param totalBlocks Total number of blocks in the volume
     */
    public MockBlockWriter(int freeBlockCount, int totalBlocks) {
        this.freeBlockCount = freeBlockCount;
        this.totalBlocks = totalBlocks;
    }

    @Override
    public byte[] readBlock(int blockNumber) throws IOException {
        byte[] block = blocks.get(blockNumber);
        if (block == null) {
            throw new IOException("Block not found: " + blockNumber);
        }
        return Arrays.copyOf(block, block.length);
    }

    @Override
    public void writeBlock(int blockNumber, byte[] data) throws IOException {
        if (data.length != 512) {
            throw new IllegalArgumentException("Block must be 512 bytes");
        }
        blocks.put(blockNumber, Arrays.copyOf(data, data.length));
    }

    @Override
    public int allocateBlock() throws IOException {
        return nextBlock++;
    }

    @Override
    public void freeBlock(int blockNumber) throws IOException {
        blocks.remove(blockNumber);
    }

    @Override
    public int getFreeBlockCount() {
        return freeBlockCount;
    }

    @Override
    public int getTotalBlocks() {
        return totalBlocks;
    }
}
