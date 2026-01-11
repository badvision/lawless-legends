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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

/**
 * Unit tests for SaplingStrategy.
 */
public class SaplingStrategyTest {

    private SaplingStrategy strategy;
    private MockBlockWriter mockWriter;

    @Before
    public void setUp() {
        strategy = new SaplingStrategy();
        mockWriter = new MockBlockWriter();
    }

    @Test
    public void testGetStorageType() {
        assertEquals(StorageType.SAPLING, strategy.getStorageType());
    }

    @Test
    public void testGetMaxFileSize() {
        assertEquals(256L * 512, strategy.getMaxFileSize());
    }

    @Test
    public void testCalculateBlocksNeededSingleBlock() {
        // 1 data block + 1 index block = 2 blocks
        assertEquals(2, strategy.calculateBlocksNeeded(1));
        assertEquals(2, strategy.calculateBlocksNeeded(512));
    }

    @Test
    public void testCalculateBlocksNeededMultipleBlocks() {
        // 2 data blocks + 1 index block = 3 blocks
        assertEquals(3, strategy.calculateBlocksNeeded(513));
        assertEquals(3, strategy.calculateBlocksNeeded(1024));
    }

    @Test
    public void testCalculateBlocksNeededMaxSize() {
        // 256 data blocks + 1 index block = 257 blocks
        assertEquals(257, strategy.calculateBlocksNeeded(256 * 512));
    }

    @Test
    public void testReadFileSingleBlock() throws IOException {
        // Set up index block pointing to data block 10
        byte[] indexBlock = new byte[512];
        indexBlock[0] = 10;  // Low byte of block 10
        indexBlock[256] = 0; // High byte of block 10
        mockWriter.writeBlock(5, indexBlock);

        // Set up data block
        byte[] dataBlock = new byte[512];
        Arrays.fill(dataBlock, (byte) 0x42);
        mockWriter.writeBlock(10, dataBlock);

        byte[] result = strategy.readFile(mockWriter, 5, 512);

        assertEquals(512, result.length);
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0x42, result[i]);
        }
    }

    @Test
    public void testReadFileMultipleBlocks() throws IOException {
        // Set up index block pointing to data blocks 10, 11, 12
        byte[] indexBlock = new byte[512];
        indexBlock[0] = 10;
        indexBlock[256] = 0;
        indexBlock[1] = 11;
        indexBlock[257] = 0;
        indexBlock[2] = 12;
        indexBlock[258] = 0;
        mockWriter.writeBlock(5, indexBlock);

        // Set up data blocks with different patterns
        byte[] dataBlock1 = new byte[512];
        Arrays.fill(dataBlock1, (byte) 0x11);
        mockWriter.writeBlock(10, dataBlock1);

        byte[] dataBlock2 = new byte[512];
        Arrays.fill(dataBlock2, (byte) 0x22);
        mockWriter.writeBlock(11, dataBlock2);

        byte[] dataBlock3 = new byte[512];
        Arrays.fill(dataBlock3, (byte) 0x33);
        mockWriter.writeBlock(12, dataBlock3);

        byte[] result = strategy.readFile(mockWriter, 5, 1536);

        assertEquals(1536, result.length);
        // Check first block
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0x11, result[i]);
        }
        // Check second block
        for (int i = 512; i < 1024; i++) {
            assertEquals((byte) 0x22, result[i]);
        }
        // Check third block
        for (int i = 1024; i < 1536; i++) {
            assertEquals((byte) 0x33, result[i]);
        }
    }

    @Test
    public void testReadFilePartialLastBlock() throws IOException {
        // Set up index block
        byte[] indexBlock = new byte[512];
        indexBlock[0] = 10;
        indexBlock[256] = 0;
        indexBlock[1] = 11;
        indexBlock[257] = 0;
        mockWriter.writeBlock(5, indexBlock);

        // Set up data blocks
        byte[] dataBlock1 = new byte[512];
        Arrays.fill(dataBlock1, (byte) 0xAA);
        mockWriter.writeBlock(10, dataBlock1);

        byte[] dataBlock2 = new byte[512];
        Arrays.fill(dataBlock2, (byte) 0xBB);
        mockWriter.writeBlock(11, dataBlock2);

        // Read 600 bytes (512 + 88)
        byte[] result = strategy.readFile(mockWriter, 5, 600);

        assertEquals(600, result.length);
        // Check first block
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0xAA, result[i]);
        }
        // Check partial second block
        for (int i = 512; i < 600; i++) {
            assertEquals((byte) 0xBB, result[i]);
        }
    }

    @Test
    public void testWriteFileSingleBlock() throws IOException {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) 0x55);

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);

        // Verify index block was written
        byte[] indexBlock = mockWriter.readBlock(keyBlock);
        assertNotNull(indexBlock);

        // Get data block number from index
        int dataBlockNum = (indexBlock[0] & 0xFF) | ((indexBlock[256] & 0xFF) << 8);

        // Verify data block was written
        byte[] writtenData = mockWriter.readBlock(dataBlockNum);
        assertNotNull(writtenData);
        assertArrayEquals(data, writtenData);
    }

    @Test
    public void testWriteFileMultipleBlocks() throws IOException {
        byte[] data = new byte[1536]; // 3 blocks
        for (int i = 0; i < 512; i++) data[i] = (byte) 0x11;
        for (int i = 512; i < 1024; i++) data[i] = (byte) 0x22;
        for (int i = 1024; i < 1536; i++) data[i] = (byte) 0x33;

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);

        // Verify index block
        byte[] indexBlock = mockWriter.readBlock(keyBlock);
        assertNotNull(indexBlock);

        // Verify all 3 data blocks
        for (int i = 0; i < 3; i++) {
            int dataBlockNum = (indexBlock[i] & 0xFF) | ((indexBlock[256 + i] & 0xFF) << 8);
            byte[] dataBlock = mockWriter.readBlock(dataBlockNum);
            assertNotNull("Data block " + i + " should exist", dataBlock);

            byte expectedByte = (byte) (0x11 + i * 0x11);
            for (int j = 0; j < 512; j++) {
                assertEquals("Block " + i + " byte " + j, expectedByte, dataBlock[j]);
            }
        }
    }

    @Test
    public void testWriteFileMaxSize() throws IOException {
        byte[] data = new byte[256 * 512];
        Arrays.fill(data, (byte) 0xFF);

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);

        // Verify index block has 256 entries
        byte[] indexBlock = mockWriter.readBlock(keyBlock);
        assertNotNull(indexBlock);

        // Verify first and last data blocks exist
        int firstDataBlock = (indexBlock[0] & 0xFF) | ((indexBlock[256] & 0xFF) << 8);
        assertNotNull(mockWriter.readBlock(firstDataBlock));

        int lastDataBlock = (indexBlock[255] & 0xFF) | ((indexBlock[511] & 0xFF) << 8);
        assertNotNull(mockWriter.readBlock(lastDataBlock));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteFileTooLarge() throws IOException {
        byte[] data = new byte[256 * 512 + 1];
        strategy.writeFile(mockWriter, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFileNegativeSize() throws IOException {
        mockWriter.writeBlock(1, new byte[512]);
        strategy.readFile(mockWriter, 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFileSizeTooLarge() throws IOException {
        mockWriter.writeBlock(1, new byte[512]);
        strategy.readFile(mockWriter, 1, 256 * 512 + 1);
    }
}
