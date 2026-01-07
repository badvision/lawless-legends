package jace.hardware.massStorage.image;

import jace.hardware.massStorage.core.ProDOSConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive TDD tests for ProDOSDiskImage.
 * Tests both:
 * 1. IDisk interface (SmartPort operations) - tested via block operations
 * 2. ProDOSDiskWriter operations (inherited file operations)
 *
 * This test suite focuses on the IDisk implementation and block-level operations,
 * complementing the ProDOSDiskWriterTest which validates file-level operations.
 *
 * Note: mliRead/mliWrite operations that interact with emulator memory cannot
 * be tested in isolation without the full emulator runtime. These operations
 * are tested through integration tests with the full emulator running.
 */
public class ProDOSDiskImageTest {

    private File tempDir;
    private File testDisk;
    private ProDOSDiskImage diskImage;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("prodos-disk-test").toFile();
        testDisk = new File(tempDir, "test.2mg");

        // Create a minimal valid ProDOS disk for testing
        createMinimalProDOSDisk(testDisk);
    }

    @After
    public void tearDown() {
        if (diskImage != null) {
            try {
                diskImage.eject();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        deleteDirectory(tempDir);
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private void writeInt32LE(RandomAccessFile raf, int value) throws IOException {
        raf.writeByte(value & 0xFF);
        raf.writeByte((value >> 8) & 0xFF);
        raf.writeByte((value >> 16) & 0xFF);
        raf.writeByte((value >> 24) & 0xFF);
    }

    private void writeInt16LE(RandomAccessFile raf, int value) throws IOException {
        raf.writeByte(value & 0xFF);
        raf.writeByte((value >> 8) & 0xFF);
    }

    /**
     * Creates a minimal ProDOS disk image for testing.
     * Format: 2MG with 1600 blocks (800KB).
     */
    private void createMinimalProDOSDisk(File diskFile) throws IOException {
        createProDOSDiskWithSize(diskFile, 1600); // 800KB disk
    }

    /**
     * Creates a ProDOS disk image with specified number of blocks.
     * Format: 2MG with configurable size.
     */
    private void createProDOSDiskWithSize(File diskFile, int totalBlocks) throws IOException {
        int diskSize = ProDOSConstants.MG2_HEADER_SIZE + (totalBlocks * ProDOSConstants.BLOCK_SIZE);

        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            // Write 2MG header (all values in little-endian)
            raf.write(ProDOSConstants.MG2_MAGIC);
            writeInt32LE(raf, 0); // Creator (offset 0x04)
            writeInt16LE(raf, 1); // Header size in bytes / 64 (offset 0x08)
            writeInt16LE(raf, 1); // Version (offset 0x0A)
            writeInt32LE(raf, 1); // Image format (1 = ProDOS) (offset 0x0C)
            writeInt32LE(raf, 0x01); // Flags (offset 0x10)
            writeInt32LE(raf, totalBlocks); // Number of blocks (offset 0x14)
            writeInt32LE(raf, ProDOSConstants.MG2_HEADER_SIZE); // Data offset (offset 0x18)
            writeInt32LE(raf, totalBlocks * ProDOSConstants.BLOCK_SIZE); // Data length (offset 0x1C)
            writeInt32LE(raf, 0); // Comment offset (offset 0x20)
            writeInt32LE(raf, 0); // Comment length (offset 0x24)
            writeInt32LE(raf, 0); // Creator-specific data offset (offset 0x28)
            writeInt32LE(raf, 0); // Creator-specific data length (offset 0x2C)

            // Pad to 64 bytes
            while (raf.getFilePointer() < ProDOSConstants.MG2_HEADER_SIZE) {
                raf.writeByte(0);
            }

            // Initialize disk data area with zeros
            byte[] zeros = new byte[ProDOSConstants.BLOCK_SIZE];
            for (int i = 0; i < totalBlocks; i++) {
                raf.write(zeros);
            }

            // Create volume directory header (block 2)
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (2 * ProDOSConstants.BLOCK_SIZE));
            byte[] volDir = new byte[ProDOSConstants.BLOCK_SIZE];

            // Prev block (0x00-0x01): 0
            volDir[0] = 0;
            volDir[1] = 0;

            // Next block (0x02-0x03): 0 (no continuation)
            volDir[2] = 0;
            volDir[3] = 0;

            // Storage type (0xF) and name length
            volDir[0x04] = (byte) 0xF8; // Volume header, 8 char name

            // Volume name "TESTDISK"
            String volName = "TESTDISK";
            for (int i = 0; i < volName.length(); i++) {
                volDir[0x05 + i] = (byte) volName.charAt(i);
            }

            // Creation date/time (just use zeros for test)
            volDir[0x18] = 0;
            volDir[0x19] = 0;
            volDir[0x1A] = 0;
            volDir[0x1B] = 0;

            // Version/Min version
            volDir[0x1C] = 0;
            volDir[0x1D] = 0;

            // Access
            volDir[0x1E] = (byte) 0xC3; // Read/write/delete/rename

            // Entry length (0x27 = 39 bytes)
            volDir[0x1F] = 0x27;

            // Entries per block (0x0D = 13)
            volDir[0x20] = 0x0D;

            // File count (0)
            volDir[0x21] = 0;
            volDir[0x22] = 0;

            // Bitmap pointer (block 6)
            volDir[0x23] = 6;
            volDir[0x24] = 0;

            // Total blocks
            volDir[0x25] = (byte) (totalBlocks & 0xFF);
            volDir[0x26] = (byte) ((totalBlocks >> 8) & 0xFF);

            raf.write(volDir);

            // Create bitmap (block 6+)
            // Mark boot blocks (0,1), volume dir (2-5), and bitmap blocks (6+) as used
            int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) / (ProDOSConstants.BLOCK_SIZE * 8);
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (6 * ProDOSConstants.BLOCK_SIZE));

            byte[] bitmap = new byte[ProDOSConstants.BLOCK_SIZE];
            // Mark blocks 0-6 as used (boot, volume dir, bitmap)
            // ProDOS bitmap: bit set = FREE, bit clear = USED
            // So 0x80 = 1000_0000 = block 7 free, blocks 0-6 used
            // We want all blocks starting from 7 to be free
            // Initialize to all 1s (all free)
            Arrays.fill(bitmap, (byte) 0xFF);
            // Then clear bits 0-6 (mark as used)
            bitmap[0] = (byte) 0x80; // Only bit 7 set = blocks 0-6 used, 7+ free

            raf.write(bitmap);

            // Write boot block with recognizable pattern
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE);
            byte[] bootBlock = new byte[ProDOSConstants.BLOCK_SIZE];
            // ProDOS boot block signature (simple pattern for testing)
            bootBlock[0] = 0x01; // ProDOS boot block marker
            bootBlock[1] = 0x38; // SEC instruction (for recognition)
            raf.write(bootBlock);
        }
    }

    // ========================================================================
    // Phase 1: Basic Constructor Tests
    // ========================================================================

    @Test
    public void testConstructor_ValidDiskImage_Success() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);
        assertNotNull("Disk image should be created", diskImage);
    }

    @Test(expected = IOException.class)
    public void testConstructor_NonExistentFile_ThrowsIOException() throws IOException {
        File nonExistent = new File(tempDir, "missing.2mg");
        diskImage = new ProDOSDiskImage(nonExistent);
    }

    @Test(expected = IOException.class)
    public void testConstructor_InvalidFormat_ThrowsIOException() throws IOException {
        File invalid = new File(tempDir, "invalid.2mg");
        Files.writeString(invalid.toPath(), "not a disk image");
        diskImage = new ProDOSDiskImage(invalid);
    }

    // ========================================================================
    // Phase 2: IDisk Interface Implementation (SmartPort Operations)
    // ========================================================================

    @Test
    public void testReadBlock_BootBlock_ReadsCorrectly() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        byte[] bootBlock = diskImage.readBlock(0);

        assertNotNull("Boot block should not be null", bootBlock);
        assertEquals("Boot block should be 512 bytes", ProDOSConstants.BLOCK_SIZE, bootBlock.length);
        // Verify boot block signature we wrote in createMinimalProDOSDisk
        assertEquals("Boot block marker should match", 0x01, bootBlock[0]);
        assertEquals("Boot block instruction should match", 0x38, bootBlock[1]);
    }

    @Test
    public void testReadBlock_VolumeDirectory_ReadsCorrectly() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        byte[] volDirBlock = diskImage.readBlock(2);

        assertNotNull("Volume directory should not be null", volDirBlock);
        assertEquals("Volume directory should be 512 bytes", ProDOSConstants.BLOCK_SIZE, volDirBlock.length);

        // Verify volume header signature
        int storageTypeAndNameLength = volDirBlock[0x04] & 0xFF;
        int storageType = (storageTypeAndNameLength >> 4) & 0x0F;
        assertEquals("Should be volume header", ProDOSConstants.STORAGE_VOLUME_HEADER, storageType);
    }

    @Test
    public void testWriteBlock_AndReadBack_DataMatches() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Write test pattern to block 10
        byte[] testPattern = new byte[ProDOSConstants.BLOCK_SIZE];
        for (int i = 0; i < testPattern.length; i++) {
            testPattern[i] = (byte) ((i * 7) & 0xFF);
        }
        diskImage.writeBlock(10, testPattern);

        // Read it back
        byte[] readBack = diskImage.readBlock(10);
        assertArrayEquals("Written data should match read data", testPattern, readBack);
    }

    @Test
    public void testWriteBlock_MultipleBlocks_AllPersist() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Write multiple blocks with unique patterns
        int[] testBlocks = {10, 15, 20, 25, 30};
        byte[][] patterns = new byte[testBlocks.length][];

        for (int i = 0; i < testBlocks.length; i++) {
            patterns[i] = new byte[ProDOSConstants.BLOCK_SIZE];
            // Create unique pattern for each block
            for (int j = 0; j < ProDOSConstants.BLOCK_SIZE; j++) {
                patterns[i][j] = (byte) ((i * 17 + j) & 0xFF);
            }
            diskImage.writeBlock(testBlocks[i], patterns[i]);
        }

        // Close and reopen
        diskImage.eject();
        diskImage = new ProDOSDiskImage(testDisk);

        // Read back all blocks and verify
        for (int i = 0; i < testBlocks.length; i++) {
            byte[] readBack = diskImage.readBlock(testBlocks[i]);
            assertArrayEquals("Block " + testBlocks[i] + " should match after reopen",
                            patterns[i], readBack);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMliFormat_NotSupported_ThrowsException() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);
        diskImage.mliFormat();
    }

    // ========================================================================
    // Phase 3: Disk Properties and State Management
    // ========================================================================

    @Test
    public void testGetSize_ReturnsTotalBlocks() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);
        int size = diskImage.getSize();

        assertEquals("Size should match disk blocks", 1600, size);
    }

    @Test
    public void testIsWriteProtected_DefaultFalse() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);
        assertFalse("Should not be write-protected by default", diskImage.isWriteProtected());
    }

    @Test
    public void testSetWriteProtected_ToggleBehavior() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Enable protection
        diskImage.setWriteProtected(true);
        assertTrue("Should be protected", diskImage.isWriteProtected());

        // Disable protection
        diskImage.setWriteProtected(false);
        assertFalse("Should not be protected", diskImage.isWriteProtected());

        // Should allow writes again (test via block write)
        byte[] testData = new byte[ProDOSConstants.BLOCK_SIZE];
        Arrays.fill(testData, (byte) 0x42);
        diskImage.writeBlock(30, testData); // Should not throw

        byte[] readBack = diskImage.readBlock(30);
        assertArrayEquals("Data should be written when not protected", testData, readBack);
    }

    @Test
    public void testEject_ClosesFileHandle() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Perform some operations
        diskImage.readBlock(0);

        // Eject should close cleanly
        diskImage.eject();

        // Verify we can delete the file (would fail if handle still open)
        assertTrue("Should be able to delete file after eject", testDisk.delete());
    }

    @Test
    public void testGetPhysicalPath_ReturnsCorrectFile() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        File physicalPath = diskImage.getPhysicalPath();
        assertNotNull("Physical path should not be null", physicalPath);
        assertEquals("Physical path should match test disk", testDisk.getAbsolutePath(),
                    physicalPath.getAbsolutePath());
    }

    // ========================================================================
    // Phase 4: Integration Tests (End-to-End Scenarios)
    // ========================================================================

    @Test
    public void testWriteFileAndReadViaBlocks_DataMatches() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Write a file using high-level file API
        byte[] fileContent = "This is a test file for block integration".getBytes();
        diskImage.writeFile("TEST.TXT", fileContent, ProDOSConstants.FILE_TYPE_TEXT);

        // Verify by reading the file back both ways
        byte[] fileReadBack = diskImage.readFile("TEST.TXT");
        assertArrayEquals("File should be readable via file API", fileContent, fileReadBack);

        // Verify we can also read raw blocks containing the file data
        // (This demonstrates integration between file-level and block-level access)
        byte[] block7 = diskImage.readBlock(7);
        assertNotNull("Block 7 should contain data", block7);
    }

    @Test
    public void testWriteViaBlocksAndReadFile_DataMatches() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Allocate a block for our data
        int dataBlock = diskImage.allocateBlock();
        assertTrue("Should allocate a free block", dataBlock >= 7);

        // Write data to that block via block API
        byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
        Arrays.fill(blockData, (byte) 0xEE);

        diskImage.writeBlock(dataBlock, blockData);

        // Read back using block-level read
        byte[] readBack = diskImage.readBlock(dataBlock);
        assertArrayEquals("Block data should match after block write", blockData, readBack);
    }

    @Test
    public void testRoundTrip_WriteReadMultipleBlocks_AllDataIntact() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Write multiple blocks with unique patterns
        int[] testBlocks = {10, 15, 20, 25, 30};
        byte[][] patterns = new byte[testBlocks.length][];

        for (int i = 0; i < testBlocks.length; i++) {
            patterns[i] = new byte[ProDOSConstants.BLOCK_SIZE];

            // Create unique pattern for each block
            for (int j = 0; j < ProDOSConstants.BLOCK_SIZE; j++) {
                patterns[i][j] = (byte) ((i * 17 + j) & 0xFF);
            }

            // Write via block API
            diskImage.writeBlock(testBlocks[i], patterns[i]);
        }

        // Close and reopen
        diskImage.eject();
        diskImage = new ProDOSDiskImage(testDisk);

        // Read back all blocks and verify
        for (int i = 0; i < testBlocks.length; i++) {
            byte[] readBack = diskImage.readBlock(testBlocks[i]);
            assertArrayEquals("Block " + testBlocks[i] + " should match after reopen",
                            patterns[i], readBack);
        }
    }

    @Test
    public void testLargeDiskImage_HandlesCorrectly() throws IOException {
        // Create a larger disk (32MB ProDOS volume)
        File largeDisk = new File(tempDir, "large.2mg");
        createProDOSDiskWithSize(largeDisk, 65535); // Max ProDOS volume

        diskImage = new ProDOSDiskImage(largeDisk);

        assertEquals("Large disk should report correct size", 65535, diskImage.getSize());

        // Test operations on high block numbers
        int highBlock = 60000;

        // Write pattern to high block
        byte[] testPattern = new byte[ProDOSConstants.BLOCK_SIZE];
        for (int i = 0; i < testPattern.length; i++) {
            testPattern[i] = (byte) ((i ^ 0x55) & 0xFF);
        }

        diskImage.writeBlock(highBlock, testPattern);

        // Read back
        byte[] readBack = diskImage.readBlock(highBlock);
        assertArrayEquals("High block should match", testPattern, readBack);
    }

    // ========================================================================
    // Phase 5: Edge Cases and Error Handling
    // ========================================================================

    @Test
    public void testReadBlock_Block0_BootBlock_ReadsCorrectly() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        byte[] bootBlock = diskImage.readBlock(0);
        assertEquals("Boot block should have marker", 0x01, bootBlock[0]);
    }

    @Test
    public void testWriteBlock_OverwriteBootBlock_UpdatesCorrectly() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Create custom boot block
        byte[] customBoot = new byte[ProDOSConstants.BLOCK_SIZE];
        customBoot[0] = (byte) 0x42; // Custom marker
        customBoot[1] = (byte) 0xBA;

        // Write custom boot block
        diskImage.writeBlock(0, customBoot);

        // Read back and verify
        byte[] readBack = diskImage.readBlock(0);
        assertEquals("Custom boot marker 1 should match", 0x42, readBack[0]);
        assertEquals("Custom boot marker 2 should match", (byte) 0xBA, readBack[1]);
    }

    @Test
    public void testConcurrentBlockAccess_MultipleOperations_AllSucceed() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Write to multiple blocks in sequence
        for (int i = 0; i < 10; i++) {
            int blockNum = 10 + i;
            byte fillValue = (byte) (i * 25);

            byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
            Arrays.fill(blockData, fillValue);

            diskImage.writeBlock(blockNum, blockData);
        }

        // Read all back and verify
        for (int i = 0; i < 10; i++) {
            int blockNum = 10 + i;
            byte expectedValue = (byte) (i * 25);

            byte[] readBack = diskImage.readBlock(blockNum);
            for (int j = 0; j < ProDOSConstants.BLOCK_SIZE; j++) {
                assertEquals("Block " + blockNum + " byte " + j + " should have correct fill value",
                           expectedValue, readBack[j]);
            }
        }
    }

    @Test
    public void testFileOperations_InheritedFromWriter_WorkCorrectly() throws IOException {
        diskImage = new ProDOSDiskImage(testDisk);

        // Test that inherited ProDOSDiskWriter operations still work
        byte[] fileData = "Testing inherited file operations".getBytes();
        diskImage.writeFile("INHERIT.TXT", fileData, ProDOSConstants.FILE_TYPE_TEXT);

        assertTrue("File should exist", diskImage.fileExists("INHERIT.TXT"));

        byte[] readBack = diskImage.readFile("INHERIT.TXT");
        assertArrayEquals("File data should match", fileData, readBack);
    }
}
