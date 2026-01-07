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
 * Unit tests for SeedlingStrategy.
 */
public class SeedlingStrategyTest {

    private SeedlingStrategy strategy;
    private MockBlockWriter mockWriter;

    @Before
    public void setUp() {
        strategy = new SeedlingStrategy();
        mockWriter = new MockBlockWriter();
    }

    @Test
    public void testGetStorageType() {
        assertEquals(StorageType.SEEDLING, strategy.getStorageType());
    }

    @Test
    public void testGetMaxFileSize() {
        assertEquals(512L, strategy.getMaxFileSize());
    }

    @Test
    public void testCalculateBlocksNeededSmall() {
        assertEquals(1, strategy.calculateBlocksNeeded(100));
    }

    @Test
    public void testCalculateBlocksNeededMax() {
        assertEquals(1, strategy.calculateBlocksNeeded(512));
    }

    @Test
    public void testCalculateBlocksNeededZero() {
        assertEquals(1, strategy.calculateBlocksNeeded(0));
    }

    @Test
    public void testReadFileSmall() throws IOException {
        byte[] blockData = new byte[512];
        Arrays.fill(blockData, (byte) 0x42);
        mockWriter.writeBlock(10, blockData);

        byte[] result = strategy.readFile(mockWriter, 10, 100);

        assertEquals(100, result.length);
        for (int i = 0; i < 100; i++) {
            assertEquals((byte) 0x42, result[i]);
        }
    }

    @Test
    public void testReadFileMaxSize() throws IOException {
        byte[] blockData = new byte[512];
        Arrays.fill(blockData, (byte) 0xAA);
        mockWriter.writeBlock(5, blockData);

        byte[] result = strategy.readFile(mockWriter, 5, 512);

        assertEquals(512, result.length);
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0xAA, result[i]);
        }
    }

    @Test
    public void testReadFileZeroSize() throws IOException {
        byte[] blockData = new byte[512];
        mockWriter.writeBlock(1, blockData);

        byte[] result = strategy.readFile(mockWriter, 1, 0);

        assertEquals(0, result.length);
    }

    @Test
    public void testWriteFileSmall() throws IOException {
        byte[] data = new byte[100];
        Arrays.fill(data, (byte) 0x33);

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);
        byte[] written = mockWriter.readBlock(keyBlock);
        assertNotNull(written);
        assertEquals(512, written.length);
        // Check data portion
        for (int i = 0; i < 100; i++) {
            assertEquals((byte) 0x33, written[i]);
        }
        // Check padding is zero
        for (int i = 100; i < 512; i++) {
            assertEquals((byte) 0, written[i]);
        }
    }

    @Test
    public void testWriteFileMaxSize() throws IOException {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) 0xFF);

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);
        byte[] written = mockWriter.readBlock(keyBlock);
        assertNotNull(written);
        assertEquals(512, written.length);
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0xFF, written[i]);
        }
    }

    @Test
    public void testWriteFileZeroSize() throws IOException {
        byte[] data = new byte[0];

        int keyBlock = strategy.writeFile(mockWriter, data);

        assertTrue(keyBlock >= 0);
        byte[] written = mockWriter.readBlock(keyBlock);
        assertNotNull(written);
        assertEquals(512, written.length);
        // All should be zero
        for (int i = 0; i < 512; i++) {
            assertEquals((byte) 0, written[i]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteFileTooLarge() throws IOException {
        byte[] data = new byte[513];
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
        strategy.readFile(mockWriter, 1, 513);
    }
}
