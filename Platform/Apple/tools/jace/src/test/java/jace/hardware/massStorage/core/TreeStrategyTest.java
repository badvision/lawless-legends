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
 * Unit tests for TreeStrategy.
 */
public class TreeStrategyTest {

    private TreeStrategy strategy;
    private MockBlockWriter mockWriter;

    @Before
    public void setUp() {
        strategy = new TreeStrategy();
        mockWriter = new MockBlockWriter();
    }

    @Test
    public void testGetStorageType() {
        assertEquals(StorageType.TREE, strategy.getStorageType());
    }

    @Test
    public void testGetMaxFileSize() {
        assertEquals(256L * 256 * 512, strategy.getMaxFileSize());
    }

    @Test
    public void testCalculateBlocksNeededMinSize() {
        // 257 data blocks: 1 master index + 2 sub-indexes + 257 data blocks = 260 blocks
        long fileSize = 257L * 512;
        assertEquals(260, strategy.calculateBlocksNeeded(fileSize));
    }

    @Test
    public void testCalculateBlocksNeededSingleSubIndex() {
        // 300 blocks: 1 master + 2 sub-indexes + 300 data = 303 blocks
        long fileSize = 300L * 512;
        assertEquals(303, strategy.calculateBlocksNeeded(fileSize));
    }

    @Test
    public void testCalculateBlocksNeededMultipleSubIndexes() {
        // 512 blocks: 1 master + 2 sub-indexes + 512 data = 515 blocks
        long fileSize = 512L * 512;
        assertEquals(515, strategy.calculateBlocksNeeded(fileSize));
    }

    @Test
    public void testCalculateBlocksNeededMaxSize() {
        // 65536 blocks: 1 master + 256 sub-indexes + 65536 data = 65793 blocks
        long fileSize = 256L * 256 * 512;
        assertEquals(65793, strategy.calculateBlocksNeeded(fileSize));
    }

    @Test
    public void testWriteAndReadSmallTree() throws IOException {
        // Create data spanning 2 sub-indexes (257 blocks minimum for TREE)
        byte[] data = new byte[257 * 512];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        int keyBlock = strategy.writeFile(mockWriter, data);
        assertTrue(keyBlock >= 0);

        // Read it back
        byte[] result = strategy.readFile(mockWriter, keyBlock, data.length);

        assertArrayEquals(data, result);
    }

    @Test
    public void testWriteAndReadMultipleSubIndexes() throws IOException {
        // Create data spanning 3 sub-indexes (512 blocks)
        byte[] data = new byte[512 * 512];
        for (int i = 0; i < 512; i++) {
            Arrays.fill(data, i * 512, (i + 1) * 512, (byte) i);
        }

        int keyBlock = strategy.writeFile(mockWriter, data);
        assertTrue(keyBlock >= 0);

        // Read it back
        byte[] result = strategy.readFile(mockWriter, keyBlock, data.length);

        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFileMasterIndexStructure() throws IOException {
        // Write a small tree file
        byte[] data = new byte[300 * 512];
        Arrays.fill(data, (byte) 0x55);

        int keyBlock = strategy.writeFile(mockWriter, data);

        // Verify master index exists and has correct structure
        byte[] masterIndex = mockWriter.readBlock(keyBlock);
        assertNotNull(masterIndex);

        // Master index should have pointers to sub-indexes
        // For 300 blocks, we need 2 sub-indexes (256 + 44)
        int subIndex1 = (masterIndex[0] & 0xFF) | ((masterIndex[256] & 0xFF) << 8);
        int subIndex2 = (masterIndex[1] & 0xFF) | ((masterIndex[257] & 0xFF) << 8);

        assertTrue(subIndex1 > 0);
        assertTrue(subIndex2 > 0);
        assertNotEquals(subIndex1, subIndex2);

        // Verify sub-indexes exist
        assertNotNull(mockWriter.readBlock(subIndex1));
        assertNotNull(mockWriter.readBlock(subIndex2));
    }

    @Test
    public void testReadFileSubIndexStructure() throws IOException {
        // Write a small tree file
        byte[] data = new byte[300 * 512];
        Arrays.fill(data, (byte) 0xAA);

        int keyBlock = strategy.writeFile(mockWriter, data);

        // Get master index
        byte[] masterIndex = mockWriter.readBlock(keyBlock);

        // Get first sub-index
        int subIndex1Num = (masterIndex[0] & 0xFF) | ((masterIndex[256] & 0xFF) << 8);
        byte[] subIndex1 = mockWriter.readBlock(subIndex1Num);
        assertNotNull(subIndex1);

        // Sub-index should have 256 data block pointers
        for (int i = 0; i < 256; i++) {
            int dataBlockNum = (subIndex1[i] & 0xFF) | ((subIndex1[256 + i] & 0xFF) << 8);
            assertTrue("Data block " + i + " should be allocated", dataBlockNum > 0);
            assertNotNull("Data block " + i + " should exist", mockWriter.readBlock(dataBlockNum));
        }
    }

    @Test
    public void testWriteFilePartialLastSubIndex() throws IOException {
        // 270 blocks = 1 full sub-index (256) + partial sub-index (14)
        byte[] data = new byte[270 * 512];
        for (int i = 0; i < 270; i++) {
            Arrays.fill(data, i * 512, (i + 1) * 512, (byte) (i & 0xFF));
        }

        int keyBlock = strategy.writeFile(mockWriter, data);

        // Verify we can read it back correctly
        byte[] result = strategy.readFile(mockWriter, keyBlock, data.length);

        assertArrayEquals(data, result);
    }

    @Test
    public void testReadFilePartialLastBlock() throws IOException {
        // Test reading when file doesn't end on block boundary
        byte[] data = new byte[257 * 512 + 100]; // 257.something blocks
        Arrays.fill(data, (byte) 0xCC);

        int keyBlock = strategy.writeFile(mockWriter, data);

        byte[] result = strategy.readFile(mockWriter, keyBlock, data.length);

        assertEquals(data.length, result.length);
        assertArrayEquals(data, result);
    }

    @Test
    public void testWriteMaxSizeFile() throws IOException {
        // Maximum size: 256 sub-indexes * 256 data blocks = 65536 data blocks
        // This is a large test, so we'll just verify structure
        long maxSize = 256L * 256 * 512;
        byte[] data = new byte[(int) maxSize];

        // Fill with pattern
        for (int i = 0; i < 1000; i++) {
            data[i] = (byte) i;
        }

        int keyBlock = strategy.writeFile(mockWriter, data);

        // Verify master index has 256 sub-index entries
        byte[] masterIndex = mockWriter.readBlock(keyBlock);
        assertNotNull(masterIndex);

        // Check first and last sub-index pointers
        int firstSubIndex = (masterIndex[0] & 0xFF) | ((masterIndex[256] & 0xFF) << 8);
        int lastSubIndex = (masterIndex[255] & 0xFF) | ((masterIndex[511] & 0xFF) << 8);

        assertTrue(firstSubIndex > 0);
        assertTrue(lastSubIndex > 0);
        assertNotEquals(firstSubIndex, lastSubIndex);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteFileTooLarge() throws IOException {
        byte[] data = new byte[(int) (256L * 256 * 512 + 1)];
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
        strategy.readFile(mockWriter, 1, 256L * 256 * 512 + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteFileTooSmallForTree() throws IOException {
        // TREE should not be used for files that fit in SAPLING
        byte[] data = new byte[256 * 512]; // Max SAPLING size
        strategy.writeFile(mockWriter, data);
    }
}
