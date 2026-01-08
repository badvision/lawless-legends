package jace.lawless;

import jace.core.Utility;
import jace.hardware.massStorage.core.ProDOSConstants;
import jace.hardware.massStorage.image.ProDOSDiskImage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Integration tests for UpgradeHandler silent upgrade functionality.
 * Tests the complete flow: backup, extract save from old disk, write to new disk.
 */
public class UpgradeHandlerIntegrationTest {

    private File tempDir;
    private GameVersionTracker tracker;
    private LawlessImageTool imageTool;
    private UpgradeHandler upgradeHandler;
    private Random random;

    @Before
    public void setUp() throws IOException {
        Utility.setHeadlessMode(true);
        tempDir = Files.createTempDirectory("lawless-upgrade-integration-test").toFile();
        tracker = new GameVersionTracker(tempDir);
        imageTool = new LawlessImageTool();
        upgradeHandler = new UpgradeHandler(imageTool, tracker);
        random = new Random(12345); // Deterministic for testing
    }

    @After
    public void tearDown() {
        deleteDirectory(tempDir);
        Utility.setHeadlessMode(false);
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
     * This is the same format used by the game.
     */
    private File createMinimalProDOSDisk(String filename) throws IOException {
        File diskFile = new File(tempDir, filename);
        int totalBlocks = 1600; // 800KB disk
        int diskSize = ProDOSConstants.MG2_HEADER_SIZE + (totalBlocks * ProDOSConstants.BLOCK_SIZE);

        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            // Write 2MG header (all values in little-endian)
            raf.write(ProDOSConstants.MG2_MAGIC);
            writeInt32LE(raf, 0); // Creator
            writeInt16LE(raf, 1); // Header size in bytes / 64
            writeInt16LE(raf, 1); // Version
            writeInt32LE(raf, 1); // Image format (1 = ProDOS order)
            writeInt32LE(raf, 0x80000001); // Flags: bit 31 = volume valid, bit 0 = locked
            writeInt32LE(raf, totalBlocks); // Number of blocks
            writeInt32LE(raf, ProDOSConstants.MG2_HEADER_SIZE); // Data offset
            writeInt32LE(raf, totalBlocks * ProDOSConstants.BLOCK_SIZE); // Data length
            writeInt32LE(raf, 0); // Comment offset
            writeInt32LE(raf, 0); // Comment length
            writeInt32LE(raf, 0); // Creator-specific data offset
            writeInt32LE(raf, 0); // Creator-specific data length

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

            // Prev/Next block: 0
            volDir[0] = 0;
            volDir[1] = 0;
            volDir[2] = 0;
            volDir[3] = 0;

            // Storage type (0xF) and name length
            volDir[0x04] = (byte) 0xF8; // Volume header, 8 char name

            // Volume name "TESTDISK"
            String volName = "TESTDISK";
            for (int i = 0; i < volName.length(); i++) {
                volDir[0x05 + i] = (byte) volName.charAt(i);
            }

            // Creation date/time (zeros for test)
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
            int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) / (ProDOSConstants.BLOCK_SIZE * 8);
            raf.seek(ProDOSConstants.MG2_HEADER_SIZE + (6 * ProDOSConstants.BLOCK_SIZE));

            for (int bitmapBlockIndex = 0; bitmapBlockIndex < bitmapBlocks; bitmapBlockIndex++) {
                byte[] bitmap = new byte[ProDOSConstants.BLOCK_SIZE];
                Arrays.fill(bitmap, (byte) 0xFF); // All free

                if (bitmapBlockIndex == 0) {
                    // Mark blocks 0-6 as used
                    bitmap[0] = 0x00; // Blocks 0-7 (boot, vol dir, bitmap)
                }

                raf.write(bitmap);
            }
        }

        return diskFile;
    }

    /**
     * Creates a disk with a save game file written to it.
     */
    private File createDiskWithSave(String filename, byte[] saveData) throws IOException {
        File disk = createMinimalProDOSDisk(filename);

        try (ProDOSDiskImage writer = new ProDOSDiskImage(disk)) {
            writer.writeFile("GAME.1.SAVE", saveData, 0x00);
        }

        return disk;
    }

    @Test
    public void testSilentUpgrade_WithSaveGame_Success() throws Exception {
        // Create test save data (typical save game is 4608 bytes)
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot with old disk (creates last known good backup)
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Now simulate user replacing file with new version (DIFFERENT SIZE)
        File newDisk = createMinimalProDOSDisk("game.2mg"); // Overwrite with new version
        // Add some data to make it a different size
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(newDisk, "rw")) {
            raf.setLength(oldSize + 512); // Make it larger
        }
        Thread.sleep(10); // Ensure timestamp is different
        newDisk.setLastModified(System.currentTimeMillis());

        // Perform upgrade
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(newDisk);

        // Verify upgrade succeeded
        assertTrue("Upgrade should succeed", shouldContinue);

        // Verify save game was transferred
        byte[] transferred;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(newDisk)) {
            transferred = reader.readFile("GAME.1.SAVE");
        }

        assertNotNull("Save game should be present on new disk", transferred);
        assertArrayEquals("Save game content should match", testSaveData, transferred);

        // Verify version info was updated
        assertEquals("Size should be updated",
            newDisk.length(), tracker.getLastKnownSize());
    }

    @Test
    public void testNoFalseUpgrade_OnGameplaySave() throws Exception {
        // This is the critical test for the bug fix
        // Boot 1: Initial version recorded
        File gameDisk = createMinimalProDOSDisk("game.2mg");
        long initialSize = gameDisk.length();
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Get .lkg backup modification time after first boot
        File lkgBackup = new File(gameDisk.getParentFile(), "game.2mg.lkg");
        assertTrue("LKG backup should exist after first boot", lkgBackup.exists());
        long lkgModTimeAfterBoot1 = lkgBackup.lastModified();

        // Boot 2: Simulate gameplay - user saves the game
        byte[] userSaveData = new byte[4608];
        random.nextBytes(userSaveData); // User's actual save data
        try (ProDOSDiskImage writer = new ProDOSDiskImage(gameDisk)) {
            writer.writeFile("GAME.1.SAVE", userSaveData, 0x00);
        }
        Thread.sleep(100); // Ensure timestamp difference
        gameDisk.setLastModified(System.currentTimeMillis());
        assertEquals("Size should not change during gameplay", initialSize, gameDisk.length());

        // This should NOT trigger upgrade but SHOULD update .lkg with user's save
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);
        assertTrue("Should continue without upgrade", shouldContinue);

        // Verify .lkg backup WAS updated on boot 2 (CRITICAL FIX: preserve user's save)
        long lkgModTimeAfterBoot2 = lkgBackup.lastModified();
        assertTrue("LKG backup SHOULD be updated on CURRENT status to preserve saves",
            lkgModTimeAfterBoot2 > lkgModTimeAfterBoot1);

        // Verify .lkg backup now contains the user's save
        byte[] lkgSaveData;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(lkgBackup)) {
            lkgSaveData = reader.readFile("GAME.1.SAVE");
        }
        assertNotNull("LKG backup should contain user's save", lkgSaveData);
        assertArrayEquals("LKG backup should have user's exact save data", userSaveData, lkgSaveData);

        // Boot 3: Replace with actual new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(initialSize + 1024); // Different size = real upgrade
        }
        Thread.sleep(100);
        gameDisk.setLastModified(System.currentTimeMillis());

        // This SHOULD trigger upgrade and preserve user's save from .lkg
        shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);
        assertTrue("Should perform upgrade", shouldContinue);

        // CRITICAL: Verify user's save was preserved in the new version
        byte[] upgradedSaveData;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(gameDisk)) {
            upgradedSaveData = reader.readFile("GAME.1.SAVE");
        }
        assertNotNull("Upgraded game should contain user's save", upgradedSaveData);
        assertArrayEquals("Upgraded game should have user's exact save data", userSaveData, upgradedSaveData);

        // Verify .lkg backup WAS updated after upgrade
        long lkgModTimeAfterBoot3 = lkgBackup.lastModified();
        assertTrue("LKG backup should be updated after upgrade",
            lkgModTimeAfterBoot3 > lkgModTimeAfterBoot2);
    }

    @Test
    public void testSilentUpgrade_NoSaveGame_Success() throws Exception {
        // Create old disk without save
        File gameDisk = createMinimalProDOSDisk("game.2mg");
        long oldSize = gameDisk.length();

        // Simulate first boot (creates last known good backup)
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 256); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // Perform upgrade
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Verify upgrade succeeded even without save
        assertTrue("Upgrade should succeed even with no save", shouldContinue);

        // Verify no save game on new disk
        byte[] noSave;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(gameDisk)) {
            noSave = reader.readFile("GAME.1.SAVE");
        }

        assertNull("No save game should be present", noSave);

        // Verify version info was updated
        assertEquals("Size should be updated",
            gameDisk.length(), tracker.getLastKnownSize());
    }

    @Test
    public void testSilentUpgrade_BackupCreated() throws Exception {
        // Create test save data
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 768); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // Perform upgrade
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Verify last known good backup exists
        File lkgBackup = new File(gameDisk.getParentFile(), gameDisk.getName() + ".lkg");
        assertTrue("Last known good backup should exist", lkgBackup.exists());
        assertTrue("Backup should be readable", lkgBackup.canRead());

        // Verify safety backup was created
        File safetyBackup = new File(gameDisk.getParentFile(), gameDisk.getName() + ".backup");
        assertTrue("Safety backup should be created", safetyBackup.exists());
    }

    @Test
    public void testSilentUpgrade_ReadOnlyDisk_RestoresBackup() throws Exception {
        // Create test save data
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 384); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // Make disk read-only to simulate write failure
        gameDisk.setReadOnly();

        // Perform upgrade (should fail and restore)
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Should still continue (with backup restored)
        assertTrue("Should continue even after failure", shouldContinue);

        // Verify safety backup exists
        File safetyBackup = new File(gameDisk.getParentFile(), gameDisk.getName() + ".backup");
        assertTrue("Backup should exist", safetyBackup.exists());

        // Restore write permissions for cleanup
        gameDisk.setWritable(true);
    }

    @Test
    public void testSilentUpgrade_Performance_UnderTwoSeconds() throws Exception {
        // Create test save data (full size)
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 128); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // Measure performance
        long startTime = System.currentTimeMillis();
        upgradeHandler.checkAndHandleUpgrade(gameDisk);
        long duration = System.currentTimeMillis() - startTime;

        // Verify performance
        assertTrue("Silent upgrade should complete in under 2 seconds (was " + duration + "ms)",
            duration < 2000);

        // Verify correctness
        byte[] transferred;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(gameDisk)) {
            transferred = reader.readFile("GAME.1.SAVE");
        }
        assertArrayEquals("Save game content should match", testSaveData, transferred);
    }

    @Test
    public void testSilentUpgrade_SkipPrompt_InHeadlessMode() throws Exception {
        // Create test save data
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 512); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // In headless mode, silent upgrade should still work
        Utility.setHeadlessMode(true);
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Should continue and perform silent upgrade
        assertTrue("Should continue in headless mode", shouldContinue);

        // Verify save was transferred
        byte[] transferred;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(gameDisk)) {
            transferred = reader.readFile("GAME.1.SAVE");
        }
        assertArrayEquals("Silent upgrade should work in headless mode", testSaveData, transferred);
    }

    @Test
    public void testBackupIntegrity_SizeMatch() throws IOException {
        File testDisk = createMinimalProDOSDisk("test.2mg");

        File backup = upgradeHandler.createBackup(testDisk);

        assertNotNull("Backup should be created", backup);
        assertEquals("Backup size should match original",
            testDisk.length(), backup.length());
    }

    @Test
    public void testBackupRestore_PreservesContent() throws IOException {
        // Create disk with save
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);
        File testDisk = createDiskWithSave("test.2mg", testSaveData);

        // Create backup
        File backup = upgradeHandler.createBackup(testDisk);

        // Corrupt the disk
        try (RandomAccessFile raf = new RandomAccessFile(testDisk, "rw")) {
            raf.seek(1000);
            raf.write(new byte[1000]); // Overwrite with zeros
        }

        // Restore from backup
        boolean restored = upgradeHandler.restoreFromBackup(testDisk, backup);

        assertTrue("Restore should succeed", restored);

        // Verify save is intact
        byte[] restoredSave;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(testDisk)) {
            restoredSave = reader.readFile("GAME.1.SAVE");
        }
        assertArrayEquals("Restored save should match original", testSaveData, restoredSave);
    }

    @Test
    public void testSilentUpgrade_VerificationDetectsSave() throws Exception {
        // Create test save data
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot with old disk (creates last known good backup)
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing file with new version (DIFFERENT SIZE)
        File newDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(newDisk, "rw")) {
            raf.setLength(oldSize + 256); // Different size
        }
        Thread.sleep(10);
        newDisk.setLastModified(System.currentTimeMillis());

        // Perform upgrade
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(newDisk);

        // Verify upgrade succeeded
        assertTrue("Upgrade should succeed", shouldContinue);

        // Verify save game was transferred and is verifiable
        byte[] transferred;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(newDisk)) {
            transferred = reader.readFile("GAME.1.SAVE");
        }

        assertNotNull("Save game should be present", transferred);
        assertEquals("Save game should have correct size", testSaveData.length, transferred.length);
        assertArrayEquals("Save game content should match", testSaveData, transferred);
    }

    @Test
    public void testSilentUpgrade_VerificationDetectsNoSave() throws Exception {
        // Create old disk without save
        File gameDisk = createMinimalProDOSDisk("game.2mg");
        long oldSize = gameDisk.length();

        // Simulate first boot (creates last known good backup)
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        gameDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(gameDisk, "rw")) {
            raf.setLength(oldSize + 1024); // Different size
        }
        Thread.sleep(10);
        gameDisk.setLastModified(System.currentTimeMillis());

        // Perform upgrade
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Verify upgrade succeeded even without save
        assertTrue("Upgrade should succeed even with no save", shouldContinue);

        // Verify no save game on new disk
        byte[] noSave;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(gameDisk)) {
            noSave = reader.readFile("GAME.1.SAVE");
        }

        assertNull("No save game should be present", noSave);
    }

    @Test
    public void testSilentUpgrade_VerificationWithDelay() throws Exception {
        // Create test save data
        byte[] testSaveData = new byte[4608];
        random.nextBytes(testSaveData);

        // Create old disk with save
        File gameDisk = createDiskWithSave("game.2mg", testSaveData);
        long oldSize = gameDisk.length();

        // Simulate first boot
        upgradeHandler.checkAndHandleUpgrade(gameDisk);

        // Simulate user replacing with new version (DIFFERENT SIZE)
        File newDisk = createMinimalProDOSDisk("game.2mg");
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(newDisk, "rw")) {
            raf.setLength(oldSize + 896); // Different size
        }
        Thread.sleep(10);
        newDisk.setLastModified(System.currentTimeMillis());

        // Measure time including verification delay
        long startTime = System.currentTimeMillis();
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(newDisk);
        long duration = System.currentTimeMillis() - startTime;

        // Verify upgrade succeeded
        assertTrue("Upgrade should succeed", shouldContinue);

        // Verify the delay was applied (should be at least 100ms for the verification delay)
        assertTrue("Upgrade should take at least 100ms due to verification delay (was " + duration + "ms)",
            duration >= 100);

        // Verify save was transferred
        byte[] transferred;
        try (ProDOSDiskImage reader = new ProDOSDiskImage(newDisk)) {
            transferred = reader.readFile("GAME.1.SAVE");
        }

        assertNotNull("Save game should be present after verification", transferred);
        assertArrayEquals("Save game content should match", testSaveData, transferred);
    }
}
