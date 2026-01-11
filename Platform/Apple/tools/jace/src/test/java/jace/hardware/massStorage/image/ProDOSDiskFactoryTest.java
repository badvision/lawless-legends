package jace.hardware.massStorage.image;

import jace.hardware.massStorage.core.ProDOSConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for ProDOSDiskFactory.
 * Tests cover all factory methods, validation, and disk structure correctness.
 */
public class ProDOSDiskFactoryTest {

    private File tempDir;
    private File testDiskFile;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("factory-test").toFile();
        testDiskFile = new File(tempDir, "test.2mg");
    }

    @After
    public void tearDown() {
        if (testDiskFile != null && testDiskFile.exists()) {
            testDiskFile.delete();
        }
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File dir) {
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

    // ========== Volume Name Validation Tests ==========

    @Test
    public void testCreateDisk_WithValidVolumeName_Succeeds() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");
        assertNotNull(disk);
        assertEquals(1600, disk.getSize());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithNullVolumeName_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, null);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Volume name cannot be empty"));
        }
    }

    @Test
    public void testCreateDisk_WithEmptyVolumeName_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "");
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Volume name cannot be empty"));
        }
    }

    @Test
    public void testCreateDisk_WithTooLongVolumeName_ThrowsException() {
        String tooLong = "ABCDEFGHIJKLMNOP"; // 16 characters
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, tooLong);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Volume name too long"));
        }
    }

    @Test
    public void testCreateDisk_WithMaxLengthVolumeName_Succeeds() throws IOException {
        String maxLength = "ABCDEFGHIJKLMNO"; // 15 characters
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, maxLength);
        assertNotNull(disk);
        disk.close();
    }

    @Test
    public void testCreateDisk_WithVolumeNameStartingWithDigit_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "1INVALID");
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("must start with a letter"));
        }
    }

    @Test
    public void testCreateDisk_WithVolumeNameContainingPeriod_Succeeds() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST.VOL");
        assertNotNull(disk);
        disk.close();
    }

    @Test
    public void testCreateDisk_WithVolumeNameContainingInvalidCharacters_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST-VOL");
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("invalid character"));
        }
    }

    @Test
    public void testCreateDisk_WithMixedCaseVolumeName_NormalizesToUpperCase() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TestVol");
        assertNotNull(disk);

        // Verify volume name was stored as uppercase
        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (2 * ProDOSConstants.BLOCK_SIZE) + ProDOSConstants.VOL_VOLUME_NAME);
            byte[] nameBytes = new byte[7];
            raf.read(nameBytes);
            String volumeName = new String(nameBytes, 0, 7);
            assertEquals("TESTVOL", volumeName);
        }
        disk.close();
    }

    // ========== Disk Size Tests ==========

    @Test
    public void testCreateDisk_WithStandardFloppy140KB_CreatesCorrectSize() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_140KB, "FLOPPY");
        assertEquals(280, disk.getSize());
        assertEquals(DiskSize.FLOPPY_140KB.getTotalBytes(), testDiskFile.length());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithStandardFloppy800KB_CreatesCorrectSize() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "FLOPPY");
        assertEquals(1600, disk.getSize());
        assertEquals(DiskSize.FLOPPY_800KB.getTotalBytes(), testDiskFile.length());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithStandardHard5MB_CreatesCorrectSize() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.HARD_5MB, "HARD5");
        assertEquals(10240, disk.getSize());
        assertEquals(DiskSize.HARD_5MB.getTotalBytes(), testDiskFile.length());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithStandardHard32MB_CreatesCorrectSize() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.HARD_32MB, "HARD32");
        assertEquals(65535, disk.getSize());
        assertEquals(DiskSize.HARD_32MB.getTotalBytes(), testDiskFile.length());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithCustomBlockCount_CreatesCorrectSize() throws IOException {
        int customBlocks = 5000;
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, customBlocks, "CUSTOM");
        assertEquals(customBlocks, disk.getSize());
        disk.close();
    }

    @Test
    public void testCreateDisk_WithBlockCountTooSmall_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, 0, "INVALID");
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Block count must be between"));
        }
    }

    @Test
    public void testCreateDisk_WithBlockCountTooLarge_ThrowsException() {
        try {
            ProDOSDiskFactory.createDisk(testDiskFile, 65536, "INVALID");
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Block count must be between"));
        }
    }

    // ========== Disk Structure Validation Tests ==========

    @Test
    public void testCreateDisk_Has2MGMagicNumber() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            byte[] magic = new byte[4];
            raf.readFully(magic);
            assertArrayEquals(ProDOSConstants.MG2_MAGIC, magic);
        }
    }

    @Test
    public void testCreateDisk_HasCorrectHeaderSize() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            raf.seek(0x08);
            int headerSize = readInt16LE(raf);
            assertEquals(1, headerSize); // 1 * 64 = 64 bytes
        }
    }

    @Test
    public void testCreateDisk_HasCorrectDataOffset() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            raf.seek(0x18);
            int dataOffset = readInt32LE(raf);
            assertEquals(ProDOSConstants.MG2_HEADER_SIZE, dataOffset);
        }
    }

    @Test
    public void testCreateDisk_HasCorrectDataLength() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            raf.seek(0x1C);
            int dataLength = readInt32LE(raf);
            assertEquals(1600 * 512, dataLength);
        }
    }

    @Test
    public void testCreateDisk_HasCorrectVolumeDirectoryHeader() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TESTVOL");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            // Seek to volume directory block (block 2)
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (2 * ProDOSConstants.BLOCK_SIZE));

            byte[] volDir = new byte[ProDOSConstants.BLOCK_SIZE];
            raf.readFully(volDir);

            // Check storage type (0xF for volume header)
            int storageTypeAndLength = volDir[ProDOSConstants.VOL_STORAGE_TYPE_NAME_LENGTH] & 0xFF;
            int storageType = (storageTypeAndLength >> 4) & 0x0F;
            assertEquals(0xF, storageType);

            // Check volume name length
            int nameLength = storageTypeAndLength & 0x0F;
            assertEquals(7, nameLength);

            // Check volume name
            String volumeName = new String(volDir, ProDOSConstants.VOL_VOLUME_NAME, nameLength);
            assertEquals("TESTVOL", volumeName);

            // Check entry length
            assertEquals(ProDOSConstants.DIR_ENTRY_LENGTH, volDir[ProDOSConstants.VOL_ENTRY_LENGTH] & 0xFF);

            // Check entries per block
            assertEquals(ProDOSConstants.DIR_ENTRIES_PER_BLOCK, volDir[ProDOSConstants.VOL_ENTRIES_PER_BLOCK] & 0xFF);

            // Check bitmap pointer
            int bitmapPointer = ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_BITMAP_POINTER);
            assertEquals(ProDOSConstants.BITMAP_START_BLOCK, bitmapPointer);

            // Check total blocks
            int totalBlocks = ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_TOTAL_BLOCKS);
            assertEquals(1600, totalBlocks);
        }
    }

    @Test
    public void testCreateDisk_HasCorrectBitmapInitialization() throws IOException {
        ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");

        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            // Seek to bitmap block (block 6)
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (6 * ProDOSConstants.BLOCK_SIZE));

            byte[] bitmap = new byte[ProDOSConstants.BLOCK_SIZE];
            raf.readFully(bitmap);

            // Check that blocks 0-6 are marked as used
            // ProDOS bitmap: bit SET = FREE, bit CLEAR = USED
            for (int i = 0; i < 7; i++) {
                int byteIndex = i / 8;
                int bitIndex = i % 8;
                boolean isFree = (bitmap[byteIndex] & (1 << bitIndex)) != 0;
                assertFalse("Block " + i + " should be marked as USED (bit clear)", isFree);
            }

            // Check that block 7 is marked as free
            int byteIndex = 7 / 8;
            int bitIndex = 7 % 8;
            boolean isFree = (bitmap[byteIndex] & (1 << bitIndex)) != 0;
            assertTrue("Block 7 should be marked as FREE (bit set)", isFree);
        }
    }

    @Test
    public void testCreateDisk_CanBeOpenedAndUsed() throws IOException {
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "TEST");
        assertNotNull(disk);

        // Verify we can perform basic operations
        assertEquals(1600, disk.getSize());
        assertFalse(disk.isWriteProtected());

        // Verify we can read the volume directory
        byte[] volDir = disk.readBlock(ProDOSConstants.VOLUME_DIR_BLOCK);
        assertNotNull(volDir);
        assertEquals(ProDOSConstants.BLOCK_SIZE, volDir.length);

        disk.close();
    }

    @Test
    public void testCreateDisk_OverwritesExistingFile() throws IOException {
        // Create initial disk
        ProDOSDiskImage disk1 = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.FLOPPY_800KB, "FIRST");
        disk1.close();

        long firstSize = testDiskFile.length();

        // Create new disk with different size (should overwrite)
        ProDOSDiskImage disk2 = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.HARD_5MB, "SECOND");
        disk2.close();

        long secondSize = testDiskFile.length();

        // Verify file was overwritten with new size
        assertNotEquals(firstSize, secondSize);
        assertEquals(DiskSize.HARD_5MB.getTotalBytes(), secondSize);

        // Verify volume name is from second disk
        try (RandomAccessFile raf = new RandomAccessFile(testDiskFile, "r")) {
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (2 * ProDOSConstants.BLOCK_SIZE) + ProDOSConstants.VOL_VOLUME_NAME);
            byte[] nameBytes = new byte[6];
            raf.read(nameBytes);
            String volumeName = new String(nameBytes, 0, 6);
            assertEquals("SECOND", volumeName);
        }
    }

    @Test
    public void testCreateDisk_WithLargeDisk_HasCorrectBitmapAllocation() throws IOException {
        // Create a disk that requires multiple bitmap blocks
        ProDOSDiskImage disk = ProDOSDiskFactory.createDisk(testDiskFile, DiskSize.HARD_32MB, "LARGE");

        // Calculate expected number of bitmap blocks
        int totalBlocks = 65535;
        int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) /
                          (ProDOSConstants.BLOCK_SIZE * 8);

        // Verify that blocks 0-6 and additional bitmap blocks are marked as used
        // ProDOS bitmap: bit SET = FREE, bit CLEAR = USED
        int expectedUsedBlocks = 7 + (bitmapBlocks - 1); // 7 base blocks + additional bitmap blocks

        byte[] bitmap = disk.readBlock(ProDOSConstants.BITMAP_START_BLOCK);

        for (int i = 0; i < expectedUsedBlocks; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            boolean isFree = (bitmap[byteIndex] & (1 << bitIndex)) != 0;
            assertFalse("Block " + i + " should be marked as USED (bit clear)", isFree);
        }

        disk.close();
    }

    // ========== Helper Methods ==========

    private int readInt32LE(RandomAccessFile raf) throws IOException {
        int b0 = raf.read() & 0xFF;
        int b1 = raf.read() & 0xFF;
        int b2 = raf.read() & 0xFF;
        int b3 = raf.read() & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private int readInt16LE(RandomAccessFile raf) throws IOException {
        int b0 = raf.read() & 0xFF;
        int b1 = raf.read() & 0xFF;
        return b0 | (b1 << 8);
    }
}
