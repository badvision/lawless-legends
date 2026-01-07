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
 * Unit tests for MockBlockWriter test utility.
 */
public class MockBlockWriterTest {

    private MockBlockWriter writer;

    @Before
    public void setUp() {
        writer = new MockBlockWriter();
    }

    @Test
    public void testAllocateBlockReturnsSequentialNumbers() throws IOException {
        assertEquals(0, writer.allocateBlock());
        assertEquals(1, writer.allocateBlock());
        assertEquals(2, writer.allocateBlock());
    }

    @Test
    public void testWriteAndReadBlock() throws IOException {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) 0x42);

        writer.writeBlock(10, data);
        byte[] result = writer.readBlock(10);

        assertEquals(512, result.length);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadBlockReturnsDefensiveCopy() throws IOException {
        byte[] original = new byte[512];
        Arrays.fill(original, (byte) 0x55);
        writer.writeBlock(5, original);

        byte[] first = writer.readBlock(5);
        byte[] second = writer.readBlock(5);

        assertNotSame(first, second);
        assertArrayEquals(first, second);

        // Mutate first copy
        first[0] = (byte) 0xFF;

        // Second copy should be unchanged
        assertEquals((byte) 0x55, second[0]);
    }

    @Test
    public void testWriteBlockStoresDefensiveCopy() throws IOException {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) 0xAA);

        writer.writeBlock(7, data);

        // Mutate original array
        data[0] = (byte) 0xFF;

        // Stored block should be unchanged
        byte[] result = writer.readBlock(7);
        assertEquals((byte) 0xAA, result[0]);
    }

    @Test(expected = IOException.class)
    public void testReadNonExistentBlock() throws IOException {
        writer.readBlock(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBlockWithWrongSize() throws IOException {
        byte[] data = new byte[256]; // Wrong size
        writer.writeBlock(1, data);
    }

    @Test
    public void testFreeBlock() throws IOException {
        byte[] data = new byte[512];
        writer.writeBlock(5, data);

        // Verify block exists
        assertNotNull(writer.readBlock(5));

        // Free the block
        writer.freeBlock(5);

        // Now reading should fail
        try {
            writer.readBlock(5);
            fail("Should have thrown IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Block not found"));
        }
    }

    @Test
    public void testFreeNonExistentBlock() throws IOException {
        // Should not throw exception
        writer.freeBlock(999);
    }

    @Test
    public void testGetFreeBlockCount() {
        assertEquals(100000, writer.getFreeBlockCount());
    }

    @Test
    public void testGetTotalBlocks() {
        assertEquals(160000, writer.getTotalBlocks());
    }

    @Test
    public void testMultipleBlocksIndependent() throws IOException {
        byte[] data1 = new byte[512];
        Arrays.fill(data1, (byte) 0x11);

        byte[] data2 = new byte[512];
        Arrays.fill(data2, (byte) 0x22);

        writer.writeBlock(10, data1);
        writer.writeBlock(20, data2);

        byte[] result1 = writer.readBlock(10);
        byte[] result2 = writer.readBlock(20);

        // Verify blocks are independent
        assertEquals((byte) 0x11, result1[0]);
        assertEquals((byte) 0x22, result2[0]);
    }

    @Test
    public void testOverwriteBlock() throws IOException {
        byte[] original = new byte[512];
        Arrays.fill(original, (byte) 0xAA);
        writer.writeBlock(5, original);

        byte[] updated = new byte[512];
        Arrays.fill(updated, (byte) 0xBB);
        writer.writeBlock(5, updated);

        byte[] result = writer.readBlock(5);
        assertEquals((byte) 0xBB, result[0]);
    }
}
