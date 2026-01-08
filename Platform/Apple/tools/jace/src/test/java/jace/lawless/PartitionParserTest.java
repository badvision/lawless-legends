package jace.lawless;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PartitionParserTest {

    @Test
    public void testParseChunks_SimplePartition() {
        // Create a minimal partition:
        // Header: [size:2] [type:1] [num:1] [len:2] [0:terminator]
        // Data: [chunk data]
        byte[] partition = new byte[] {
            0x07, 0x00,       // Header size = 7 bytes
            0x02,             // Type = CODE
            0x01,             // Num = 1
            0x04, 0x00,       // Length = 4 (uncompressed)
            0x00,             // Terminator
            0x01, 0x02, 0x03, 0x04  // Chunk data
        };

        List<PartitionParser.ChunkInfo> chunks = PartitionParser.parseChunks(partition);

        assertEquals(1, chunks.size());
        PartitionParser.ChunkInfo chunk = chunks.get(0);
        assertEquals(0x02, chunk.type);
        assertEquals(0x01, chunk.num);
        assertEquals(4, chunk.length);
        assertEquals(4, chunk.uncompressedLength);
        assertFalse(chunk.compressed);
        assertEquals(7, chunk.dataOffset);
    }

    @Test
    public void testParseChunks_CompressedChunk() {
        // Compressed chunk has high bit set in length's high byte and includes uncompressed length
        byte[] partition = new byte[] {
            0x09, 0x00,       // Header size = 9 bytes
            0x02,             // Type = CODE
            0x01,             // Num = 1
            0x04, (byte) 0x80, // Length = 4, compressed flag set
            0x08, 0x00,       // Uncompressed length = 8
            0x00,             // Terminator
            0x01, 0x02, 0x03, 0x04  // Compressed data (4 bytes)
        };

        List<PartitionParser.ChunkInfo> chunks = PartitionParser.parseChunks(partition);

        assertEquals(1, chunks.size());
        PartitionParser.ChunkInfo chunk = chunks.get(0);
        assertEquals(0x02, chunk.type);
        assertEquals(4, chunk.length);
        assertEquals(8, chunk.uncompressedLength);
        assertTrue(chunk.compressed);
        assertEquals(9, chunk.dataOffset);
    }

    @Test
    public void testParseChunks_MultipleChunks() {
        // Multiple chunks in one partition
        byte[] partition = new byte[] {
            0x0D, 0x00,       // Header size = 13 bytes
            0x01,             // Type = 1
            0x01,             // Num = 1
            0x02, 0x00,       // Length = 2
            0x02,             // Type = 2
            0x02,             // Num = 2
            0x03, (byte) 0x80, // Length = 3, compressed
            0x06, 0x00,       // Uncompressed = 6
            0x00,             // Terminator
            0x11, 0x22,       // Chunk 1 data (offset 13)
            0x33, 0x44, 0x55  // Chunk 2 data (offset 15)
        };

        List<PartitionParser.ChunkInfo> chunks = PartitionParser.parseChunks(partition);

        assertEquals(2, chunks.size());

        PartitionParser.ChunkInfo chunk1 = chunks.get(0);
        assertEquals(0x01, chunk1.type);
        assertEquals(2, chunk1.length);
        assertEquals(13, chunk1.dataOffset);
        assertFalse(chunk1.compressed);

        PartitionParser.ChunkInfo chunk2 = chunks.get(1);
        assertEquals(0x02, chunk2.type);
        assertEquals(3, chunk2.length);
        assertEquals(6, chunk2.uncompressedLength);
        assertEquals(15, chunk2.dataOffset);
        assertTrue(chunk2.compressed);
    }

    @Test
    public void testFindResourceIndexChunk() {
        // Create partition with CODE chunk (type 0x02) that's compressed
        byte[] partition = new byte[] {
            0x09, 0x00,       // Header size = 9 bytes
            0x01,             // Type = CODE (0x01)
            0x01,             // Num = 1
            0x05, (byte) 0x80, // Length = 5, compressed
            0x0A, 0x00,       // Uncompressed = 10
            0x00,             // Terminator
            0x01, 0x02, 0x03, 0x04, 0x05  // Data
        };

        PartitionParser.ChunkInfo chunk = PartitionParser.findResourceIndexChunk(partition);

        assertNotNull(chunk);
        assertEquals(0x01, chunk.type);
        assertTrue(chunk.compressed);
    }

    @Test
    public void testFindResourceIndexChunk_NotFound() {
        // Partition without CODE chunk (using type 0x02 which is 2D_MAP, not CODE)
        byte[] partition = new byte[] {
            0x07, 0x00,       // Header size = 7 bytes
            0x02,             // Type = 2 (2D_MAP, not CODE)
            0x01,             // Num = 1
            0x03, 0x00,       // Length = 3
            0x00,             // Terminator
            0x11, 0x22, 0x33  // Data
        };

        PartitionParser.ChunkInfo chunk = PartitionParser.findResourceIndexChunk(partition);

        assertNull(chunk);
    }

    @Test
    public void testExtractChunkData() {
        byte[] partition = new byte[] {
            0x07, 0x00,       // Header
            0x01, 0x01, 0x04, 0x00, 0x00,  // Chunk entry
            0x41, 0x42, 0x43, 0x44  // Chunk data at offset 7
        };

        List<PartitionParser.ChunkInfo> chunks = PartitionParser.parseChunks(partition);
        byte[] data = PartitionParser.extractChunkData(partition, chunks.get(0));

        assertEquals(4, data.length);
        assertEquals(0x41, data[0]);
        assertEquals(0x42, data[1]);
        assertEquals(0x43, data[2]);
        assertEquals(0x44, data[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseChunks_TooSmall() {
        byte[] partition = new byte[] { 0x01 }; // Only 1 byte
        PartitionParser.parseChunks(partition);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseChunks_InvalidHeaderSize() {
        byte[] partition = new byte[] {
            (byte) 0xFF, (byte) 0xFF,  // Header size = 65535 (too large)
            0x01, 0x01, 0x02, 0x00, 0x00
        };
        PartitionParser.parseChunks(partition);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractChunkData_BeyondPartition() {
        byte[] partition = new byte[] { 0x07, 0x00, 0x01, 0x01, 0x04, 0x00, 0x00 };
        List<PartitionParser.ChunkInfo> chunks = PartitionParser.parseChunks(partition);
        // Try to extract data that extends beyond partition
        PartitionParser.extractChunkData(partition, chunks.get(0));
    }
}
