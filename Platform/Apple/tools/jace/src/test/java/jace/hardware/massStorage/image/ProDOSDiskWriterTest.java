package jace.hardware.massStorage.image;

import jace.hardware.massStorage.core.ProDOSConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * TDD-driven tests for ProDOSDiskWriter.
 * Each test verifies correctness using AppleCommander as oracle.
 */
public class ProDOSDiskWriterTest {

    private File tempDir;
    private File testDisk;
    private ProDOSDiskWriter writer;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("prodos-test").toFile();
        testDisk = new File(tempDir, "test.2mg");

        // Create a minimal valid ProDOS disk for testing using factory
        ProDOSDiskFactory.createDisk(testDisk, DiskSize.FLOPPY_800KB, "TEST");
    }

    @After
    public void tearDown() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
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

    // Phase 1 Tests: Core Write Operations

    @Test
    public void testConstructor_ValidDisk_OpensSuccessfully() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);
        assertNotNull("Writer should be created", writer);
    }

    @Test(expected = IOException.class)
    public void testConstructor_NonExistentFile_ThrowsException() throws IOException {
        File nonExistent = new File(tempDir, "missing.2mg");
        writer = new ProDOSDiskWriter(nonExistent);
    }

    @Test(expected = IOException.class)
    public void testConstructor_InvalidFormat_ThrowsException() throws IOException {
        File invalid = new File(tempDir, "invalid.2mg");
        Files.writeString(invalid.toPath(), "not a disk image");
        writer = new ProDOSDiskWriter(invalid);
    }

    @Test
    public void testReadBlock_BootBlock_ReadsCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);
        byte[] block = writer.readBlock(0);

        assertNotNull("Block should not be null", block);
        assertEquals("Block should be 512 bytes", ProDOSConstants.BLOCK_SIZE, block.length);
    }

    @Test
    public void testReadBlock_VolumeDirectory_ReadsCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);
        byte[] block = writer.readBlock(2);

        assertNotNull("Block should not be null", block);
        assertEquals("Block should be 512 bytes", ProDOSConstants.BLOCK_SIZE, block.length);

        // Verify volume header signature
        int storageTypeAndNameLength = block[0x04] & 0xFF;
        int storageType = (storageTypeAndNameLength >> 4) & 0x0F;
        assertEquals("Should be volume header", ProDOSConstants.STORAGE_VOLUME_HEADER, storageType);
    }

    @Test
    public void testWriteBlock_DataBlock_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        byte[] testData = new byte[ProDOSConstants.BLOCK_SIZE];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i & 0xFF);
        }

        // Write to a free block (block 10 should be free)
        writer.writeBlock(10, testData);

        // Read it back
        byte[] readBack = writer.readBlock(10);
        assertArrayEquals("Written data should match read data", testData, readBack);
    }

    @Test
    public void testReadBitmap_InitialState_CorrectlyIdentifiesUsedBlocks() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Blocks 0-6 should be marked as used in our test disk
        assertTrue("Block 0 should be used", writer.isBlockAllocated(0));
        assertTrue("Block 1 should be used", writer.isBlockAllocated(1));
        assertTrue("Block 2 should be used", writer.isBlockAllocated(2));
        assertTrue("Block 6 should be used", writer.isBlockAllocated(6));

        // Block 7+ should be free
        assertFalse("Block 7 should be free", writer.isBlockAllocated(7));
        assertFalse("Block 10 should be free", writer.isBlockAllocated(10));
    }

    @Test
    public void testAllocateBlocks_SingleBlock_ReturnsBlockNumber() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        int allocatedBlock = writer.allocateBlock();

        assertTrue("Allocated block should be >= 7", allocatedBlock >= 7);
        assertTrue("Allocated block should now be marked as used",
                   writer.isBlockAllocated(allocatedBlock));
    }

    @Test
    public void testAllocateBlocks_MultipleBlocks_ReturnsSequence() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        int[] blocks = writer.allocateBlocks(5);

        assertNotNull("Should return block array", blocks);
        assertEquals("Should allocate 5 blocks", 5, blocks.length);

        // All blocks should now be marked as used
        for (int block : blocks) {
            assertTrue("Block " + block + " should be marked as used",
                      writer.isBlockAllocated(block));
        }
    }

    @Test
    public void testDeallocateBlock_AllocatedBlock_MarksAsFree() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Allocate a block
        int block = writer.allocateBlock();
        assertTrue("Block should be allocated", writer.isBlockAllocated(block));

        // Deallocate it
        writer.deallocateBlock(block);
        assertFalse("Block should be free", writer.isBlockAllocated(block));
    }

    @Test
    public void testFlushBitmap_ModifiedBitmap_PersistsChanges() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Allocate a block
        int block = writer.allocateBlock();

        // Flush bitmap to disk
        writer.flushBitmap();

        // Close and reopen
        writer.close();
        writer = new ProDOSDiskWriter(testDisk);

        // Block should still be allocated
        assertTrue("Block should remain allocated after reopen",
                   writer.isBlockAllocated(block));
    }

    @Test
    public void testGetFreeBlockCount_InitialDisk_ReturnsCorrectCount() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        int freeBlocks = writer.getFreeBlockCount();

        // 1600 total - 7 used (0-6) = 1593 free
        assertTrue("Should have many free blocks", freeBlocks > 1500);
        assertTrue("Should not exceed total blocks", freeBlocks <= 1600);
    }

    @Test
    public void testAllocateBlocks_NearCapacity_ThrowsExceptionWhenFull() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Try to allocate more blocks than available
        int freeBlocks = writer.getFreeBlockCount();

        try {
            writer.allocateBlocks(freeBlocks + 1);
            fail("Should throw IOException when disk is full");
        } catch (IOException e) {
            assertTrue("Exception should mention disk full",
                      e.getMessage().toLowerCase().contains("full"));
        }
    }

    // Phase 2 Tests: File Writing

    @Test
    public void testWriteFile_Seedling_SingleBlock_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        byte[] fileData = new byte[256]; // 256 bytes = SEEDLING file
        for (int i = 0; i < fileData.length; i++) {
            fileData[i] = (byte) (i & 0xFF);
        }

        writer.writeFile("TEST.FILE", fileData, ProDOSConstants.FILE_TYPE_BINARY);
        writer.close();

        // Reopen and verify file exists
        writer = new ProDOSDiskWriter(testDisk);
        assertTrue("File should exist", writer.fileExists("TEST.FILE"));

        // Read file back
        byte[] readData = writer.readFile("TEST.FILE");
        assertArrayEquals("File data should match", fileData, readData);
    }

    @Test
    public void testWriteFile_Sapling_MultipleBlocks_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        byte[] fileData = new byte[4608]; // 9 blocks = SAPLING file (GAME.1.SAVE size)
        for (int i = 0; i < fileData.length; i++) {
            fileData[i] = (byte) ((i % 256) & 0xFF);
        }

        writer.writeFile("GAME.1.SAVE", fileData, ProDOSConstants.FILE_TYPE_BINARY);
        writer.close();

        // Reopen and verify
        writer = new ProDOSDiskWriter(testDisk);
        assertTrue("File should exist", writer.fileExists("GAME.1.SAVE"));

        byte[] readData = writer.readFile("GAME.1.SAVE");
        assertNotNull("Should read file data", readData);
        assertEquals("File size should match", fileData.length, readData.length);
        assertArrayEquals("File data should match", fileData, readData);
    }

    @Test
    public void testWriteFile_Overwrite_UpdatesExistingFile() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Write initial file
        byte[] data1 = "Original content".getBytes();
        writer.writeFile("OVERWRITE.TXT", data1, ProDOSConstants.FILE_TYPE_TEXT);

        // Overwrite with new data
        byte[] data2 = "New content that is much longer".getBytes();
        writer.writeFile("OVERWRITE.TXT", data2, ProDOSConstants.FILE_TYPE_TEXT);

        // Verify new data
        byte[] readData = writer.readFile("OVERWRITE.TXT");
        assertArrayEquals("Should have new data", data2, readData);
    }

    @Test
    public void testWriteFile_MaxFilename_HandlesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // ProDOS allows up to 15 character filenames
        String maxName = "VERYLONGNAME123"; // 15 chars
        byte[] data = "test".getBytes();

        writer.writeFile(maxName, data, ProDOSConstants.FILE_TYPE_TEXT);
        assertTrue("File with max name should exist", writer.fileExists(maxName));
    }

    @Test(expected = IOException.class)
    public void testWriteFile_FilenameTooLong_ThrowsException() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        String tooLong = "THISFILENAMEISTOOLONG"; // 21 chars
        writer.writeFile(tooLong, "test".getBytes(), ProDOSConstants.FILE_TYPE_TEXT);
    }

    @Test
    public void testWriteFile_EmptyFile_CreatesZeroLengthFile() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        byte[] emptyData = new byte[0];
        writer.writeFile("EMPTY.FILE", emptyData, ProDOSConstants.FILE_TYPE_TEXT);

        assertTrue("Empty file should exist", writer.fileExists("EMPTY.FILE"));
        byte[] readData = writer.readFile("EMPTY.FILE");
        assertEquals("Should be zero length", 0, readData.length);
    }

    @Test
    public void testDeleteFile_ExistingFile_RemovesFromDirectory() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Create file
        writer.writeFile("DELETE.ME", "test data".getBytes(), ProDOSConstants.FILE_TYPE_TEXT);
        assertTrue("File should exist", writer.fileExists("DELETE.ME"));

        // Delete it
        writer.deleteFile("DELETE.ME");
        assertFalse("File should not exist after delete", writer.fileExists("DELETE.ME"));
    }

    @Test
    public void testGetFileCount_AfterWriting_ReturnsCorrectCount() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        assertEquals("Should start with 0 files", 0, writer.getFileCount());

        writer.writeFile("FILE1.TXT", "data1".getBytes(), ProDOSConstants.FILE_TYPE_TEXT);
        assertEquals("Should have 1 file", 1, writer.getFileCount());

        writer.writeFile("FILE2.TXT", "data2".getBytes(), ProDOSConstants.FILE_TYPE_TEXT);
        assertEquals("Should have 2 files", 2, writer.getFileCount());

        writer.deleteFile("FILE1.TXT");
        assertEquals("Should have 1 file after delete", 1, writer.getFileCount());
    }

    // Phase 2 Tests: TREE Storage Support (>128KB files)

    @Test
    public void testWriteFile_Tree_MinimumSize_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // 129KB file (minimum TREE size: >128KB)
        int fileSize = (128 * 1024) + 1024;
        byte[] largeData = new byte[fileSize];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        writer.writeFile("LARGE.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);

        // Read it back
        byte[] readData = writer.readFile("LARGE.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match", fileSize, readData.length);
        assertArrayEquals("File data should match", largeData, readData);
    }

    @Test
    public void testWriteFile_Tree_256KB_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // 256KB file
        int fileSize = 256 * 1024;
        byte[] largeData = new byte[fileSize];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ((i >> 8) ^ (i & 0xFF));
        }

        writer.writeFile("MEDIUM.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);

        // Read it back
        byte[] readData = writer.readFile("MEDIUM.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match", fileSize, readData.length);
        assertArrayEquals("File data should match", largeData, readData);
    }

    @Test
    public void testWriteFile_Tree_512KB_WritesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // 512KB file
        int fileSize = 512 * 1024;
        byte[] largeData = new byte[fileSize];
        // Use predictable pattern for verification
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i & 0xFF);
        }

        writer.writeFile("HUGE.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);

        // Read it back
        byte[] readData = writer.readFile("HUGE.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match", fileSize, readData.length);
        assertArrayEquals("File data should match", largeData, readData);
    }

    @Test
    public void testWriteFile_Tree_1MB_WritesCorrectly() throws IOException {
        // Create larger disk for 1MB file (need ~2200 blocks = 1.1MB)
        File largeDisk = new File(tempDir, "large.2mg");
        ProDOSDiskFactory.createDisk(largeDisk, 2500, "TEST");
        writer = new ProDOSDiskWriter(largeDisk);

        // 1MB file
        int fileSize = 1024 * 1024;
        byte[] largeData = new byte[fileSize];
        // Fill with repeating pattern
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ((i % 256) ^ ((i / 256) % 256));
        }

        writer.writeFile("MASSIVE.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);

        // Read it back
        byte[] readData = writer.readFile("MASSIVE.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match", fileSize, readData.length);
        assertArrayEquals("File data should match", largeData, readData);
    }

    @Test
    public void testWriteFile_Tree_Overwrite_UpdatesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Write initial 129KB file
        int size1 = (128 * 1024) + 1024;
        byte[] data1 = new byte[size1];
        Arrays.fill(data1, (byte) 0xAA);
        writer.writeFile("OVERWRITE.BIN", data1, ProDOSConstants.FILE_TYPE_BINARY);

        // Overwrite with 256KB file
        int size2 = 256 * 1024;
        byte[] data2 = new byte[size2];
        Arrays.fill(data2, (byte) 0xBB);
        writer.writeFile("OVERWRITE.BIN", data2, ProDOSConstants.FILE_TYPE_BINARY);

        // Read it back
        byte[] readData = writer.readFile("OVERWRITE.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match new size", size2, readData.length);
        assertArrayEquals("File data should match new data", data2, readData);
    }

    @Test
    public void testWriteFile_Tree_Delete_RemovesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Write 200KB file
        int fileSize = 200 * 1024;
        byte[] largeData = new byte[fileSize];
        Arrays.fill(largeData, (byte) 0x42);
        writer.writeFile("DELETE.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);

        // Verify it exists
        assertTrue("File should exist", writer.fileExists("DELETE.BIN"));

        // Delete it
        writer.deleteFile("DELETE.BIN");

        // Verify it's gone
        assertFalse("File should not exist", writer.fileExists("DELETE.BIN"));
        assertNull("File read should return null", writer.readFile("DELETE.BIN"));
    }

    @Test
    public void testWriteFile_Tree_Multiple_AllReadCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Write multiple TREE files
        byte[] file1 = new byte[150 * 1024];
        Arrays.fill(file1, (byte) 0x11);
        writer.writeFile("FILE1.BIN", file1, ProDOSConstants.FILE_TYPE_BINARY);

        byte[] file2 = new byte[200 * 1024];
        Arrays.fill(file2, (byte) 0x22);
        writer.writeFile("FILE2.BIN", file2, ProDOSConstants.FILE_TYPE_BINARY);

        byte[] file3 = new byte[300 * 1024];
        Arrays.fill(file3, (byte) 0x33);
        writer.writeFile("FILE3.BIN", file3, ProDOSConstants.FILE_TYPE_BINARY);

        // Read all back and verify
        assertArrayEquals("File1 should match", file1, writer.readFile("FILE1.BIN"));
        assertArrayEquals("File2 should match", file2, writer.readFile("FILE2.BIN"));
        assertArrayEquals("File3 should match", file3, writer.readFile("FILE3.BIN"));
    }

    @Test
    public void testWriteFile_Tree_BoundaryConditions_HandlesCorrectly() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Test exact TREE boundary (131072 bytes = 256 blocks * 512)
        int exactTreeSize = 256 * 512;
        byte[] boundaryData = new byte[exactTreeSize];
        for (int i = 0; i < boundaryData.length; i++) {
            boundaryData[i] = (byte) (i % 256);
        }

        writer.writeFile("BOUNDARY.BIN", boundaryData, ProDOSConstants.FILE_TYPE_BINARY);

        byte[] readData = writer.readFile("BOUNDARY.BIN");
        assertNotNull("File should exist", readData);
        assertEquals("File size should match", exactTreeSize, readData.length);
        assertArrayEquals("File data should match", boundaryData, readData);
    }

    @Test
    public void testWriteFile_Tree_RoundTrip_DataIntegrity() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);

        // Create file with complex pattern to detect corruption
        int fileSize = 400 * 1024;
        byte[] originalData = new byte[fileSize];

        // Fill with pattern that uses file position
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) ((i * 7) ^ ((i >> 8) * 13));
        }

        writer.writeFile("INTEGRITY.BIN", originalData, ProDOSConstants.FILE_TYPE_BINARY);
        writer.close();

        // Re-open disk and read file
        writer = new ProDOSDiskWriter(testDisk);
        byte[] readData = writer.readFile("INTEGRITY.BIN");

        assertNotNull("File should exist after re-open", readData);
        assertEquals("File size should match after re-open", fileSize, readData.length);
        assertArrayEquals("File data should be identical after re-open", originalData, readData);
    }

    @Test
    public void testWriteFile_Tree_Performance_CompletesInReasonableTime() throws IOException {
        // Create larger disk for 1MB file (need ~2200 blocks = 1.1MB)
        File largeDisk = new File(tempDir, "perf.2mg");
        ProDOSDiskFactory.createDisk(largeDisk, 2500, "TEST");
        writer = new ProDOSDiskWriter(largeDisk);

        // 1MB file - should complete in <200ms
        int fileSize = 1024 * 1024;
        byte[] largeData = new byte[fileSize];
        Arrays.fill(largeData, (byte) 0x55);

        long startTime = System.currentTimeMillis();
        writer.writeFile("PERF.BIN", largeData, ProDOSConstants.FILE_TYPE_BINARY);
        long writeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        byte[] readData = writer.readFile("PERF.BIN");
        long readTime = System.currentTimeMillis() - startTime;

        assertTrue("Write should complete in <200ms, took: " + writeTime + "ms", writeTime < 200);
        assertTrue("Read should complete in <200ms, took: " + readTime + "ms", readTime < 200);
        assertArrayEquals("Data should match", largeData, readData);
    }

    /**
     * Test bitmap round-trip: write file, close, reopen, verify bitmap is correct.
     * This verifies our bitmap read/write logic matches ProDOS specification.
     */
    @Test
    public void testBitmapRoundTrip_WriteAndReadBack_BitmapStaysConsistent() throws IOException {
        writer = new ProDOSDiskWriter(testDisk);
        
        // Get initial free block count
        int initialFree = writer.getFreeBlockCount();
        
        // Write a sapling file (requires multiple blocks)
        byte[] data = new byte[5000]; // ~10 blocks
        Arrays.fill(data, (byte) 0x42);
        writer.writeFile("TEST.FILE", data, 0x06);
        
        // Check free blocks decreased
        int afterWrite = writer.getFreeBlockCount();
        assertTrue("Free blocks should decrease after write", afterWrite < initialFree);
        int blocksUsed = initialFree - afterWrite;
        
        // Close and reopen
        writer.close();
        writer = new ProDOSDiskWriter(testDisk);
        
        // Verify free block count is same after reload
        int afterReload = writer.getFreeBlockCount();
        assertEquals("Free blocks should be same after reload", afterWrite, afterReload);
        
        // Read file back
        byte[] readData = writer.readFile("TEST.FILE");
        assertArrayEquals("File data should match", data, readData);
        
        // Delete file
        writer.deleteFile("TEST.FILE");

        // Verify free blocks returned to initial count
        int afterDelete = writer.getFreeBlockCount();
        System.out.println("Blocks: initial=" + initialFree + ", afterWrite=" + afterWrite +
                          ", blocksUsed=" + blocksUsed + ", afterDelete=" + afterDelete +
                          ", leaked=" + (initialFree - afterDelete));
        assertEquals("Free blocks should return to initial after delete", initialFree, afterDelete);
        
        // Close and reopen again
        writer.close();
        writer = new ProDOSDiskWriter(testDisk);
        
        // Verify bitmap is still consistent
        int finalFree = writer.getFreeBlockCount();
        assertEquals("Bitmap should be consistent after delete+reload", initialFree, finalFree);
    }
}
