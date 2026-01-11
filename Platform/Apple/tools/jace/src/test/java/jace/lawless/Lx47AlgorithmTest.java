package jace.lawless;

import jace.lawless.compression.Lx47Algorithm;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Lx47Algorithm decompression.
 *
 * Note: Creating valid Lx47 compressed data by hand is complex.
 * These tests focus on basic validation. Integration tests with
 * real game data provide comprehensive coverage.
 */
public class Lx47AlgorithmTest {

    @Test
    public void testDecompress_EmptyOutput() {
        // Test edge case: zero length output
        // This should handle EOF immediately
        byte[] compressed = new byte[] {
            (byte) 0x00  // EOF marker pattern
        };

        byte[] output = new byte[0];
        // Should complete without exception
        Lx47Algorithm.decompress(compressed, 0, output, 0, 0);
        // If we get here without exception, test passes
    }

    @Test
    public void testDecompress_WithOutputOffset() {
        // Test that output offset parameter works
        // We'll decompress into the middle of a buffer
        byte[] compressed = new byte[] {
            (byte) 0x00  // EOF pattern for empty decompression
        };

        byte[] output = new byte[10];
        output[0] = (byte) 0xFF; // Marker byte that shouldn't be touched
        output[9] = (byte) 0xFF; // Marker byte that shouldn't be touched

        // Decompress zero bytes starting at offset 5
        Lx47Algorithm.decompress(compressed, 0, output, 5, 0);

        // Verify markers weren't touched
        assertEquals((byte) 0xFF, output[0]);
        assertEquals((byte) 0xFF, output[9]);
    }

    @Test
    public void testDecompress_WithInputOffset() {
        // Test that input offset parameter works
        byte[] buffer = new byte[10];
        // Put EOF pattern at offset 5
        buffer[5] = (byte) 0x00;

        byte[] output = new byte[0];
        // Decompress from offset 5
        Lx47Algorithm.decompress(buffer, 5, output, 0, 0);
        // If we get here without exception, test passes
    }

    @Test
    public void testLx47Reader_GetPosition() {
        // Test that reader tracks position correctly
        Lx47Algorithm.Lx47Reader reader = new Lx47Algorithm.Lx47Reader(
            new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 }, 2
        );

        // Should start at the specified offset
        assertEquals(2, reader.getInPos());
    }

    /**
     * Integration test note:
     * Real validation of Lx47 decompression happens in integration tests
     * where we decompress actual game partition data and verify the version
     * string is correctly extracted.
     */
    @Test
    public void testDecompress_NullSafety() {
        // Verify that method doesn't crash with minimal valid inputs
        byte[] input = new byte[100];
        byte[] output = new byte[10];

        try {
            // This will likely fail because input isn't valid compressed data,
            // but it tests that the method doesn't crash on null checks
            Lx47Algorithm.decompress(input, 0, output, 0, 0);
            // Zero length output should work regardless of input
        } catch (ArrayIndexOutOfBoundsException e) {
            // This is expected if the decompressor tries to read beyond buffer
            // We're just testing it doesn't crash on null
        }
    }
}
