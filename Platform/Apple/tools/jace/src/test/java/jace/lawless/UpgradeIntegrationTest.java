package jace.lawless;

import jace.core.Utility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Integration tests for the complete upgrade detection and handling workflow.
 */
public class UpgradeIntegrationTest {

    private File tempDir;
    private File testGameFile;
    private File propertiesFile;
    private GameVersionTracker tracker;
    private LawlessImageTool imageTool;
    private UpgradeHandler upgradeHandler;

    @Before
    public void setUp() throws IOException {
        Utility.setHeadlessMode(true);
        tempDir = Files.createTempDirectory("lawless-integration-test").toFile();
        testGameFile = new File(tempDir, "game.2mg");
        propertiesFile = new File(tempDir, "game-version.properties");
        Files.writeString(testGameFile.toPath(), "initial game version content");
        tracker = new GameVersionTracker(tempDir);
        imageTool = new LawlessImageTool();
        upgradeHandler = new UpgradeHandler(imageTool, tracker);
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

    @Test
    public void testFullWorkflow_FirstRun_RecordsVersionAndContinues() {
        assertFalse("Properties file should not exist initially", propertiesFile.exists());

        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile);

        assertTrue("Should continue on first run", shouldContinue);
        assertTrue("Properties file should be created", propertiesFile.exists());
        assertEquals("Timestamp should match file",
            testGameFile.lastModified(),
            tracker.getLastKnownModificationTime());
    }

    @Test
    public void testFullWorkflow_NoChange_ContinuesWithoutPrompt() throws IOException {
        // Simulate first run
        tracker.saveVersionInfo(testGameFile.lastModified(), testGameFile.length());
        long initialTimestamp = tracker.getLastKnownModificationTime();
        long initialSize = tracker.getLastKnownSize();

        // Check again with same file
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile);

        assertTrue("Should continue when file hasn't changed", shouldContinue);
        assertEquals("Timestamp should remain unchanged",
            initialTimestamp,
            tracker.getLastKnownModificationTime());
        assertEquals("Size should remain unchanged",
            initialSize,
            tracker.getLastKnownSize());
    }

    @Test
    public void testFullWorkflow_FileModified_DetectsChange() throws IOException, InterruptedException {
        // Record initial version
        long initialSize = testGameFile.length();
        tracker.saveVersionInfo(testGameFile.lastModified(), initialSize);

        // Wait to ensure different timestamp
        Thread.sleep(10);

        // Modify the file - CHANGE THE SIZE to trigger upgrade
        Files.writeString(testGameFile.toPath(), "new upgraded game version content with extra data to change size");
        assertTrue("File size should be different",
            testGameFile.length() != initialSize);

        // Check for update (size changed = upgrade detected)
        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals("Should detect upgrade when size changes",
            GameVersionTracker.UpdateStatus.UPGRADED, status);
    }

    @Test
    public void testBackupCreation_VerifyIntegrity() throws IOException {
        String originalContent = "test game data for backup verification";
        Files.writeString(testGameFile.toPath(), originalContent);

        File backup = upgradeHandler.createBackup(testGameFile);

        assertNotNull("Backup should be created", backup);
        assertTrue("Backup should exist", backup.exists());
        assertEquals("Backup should have same size",
            testGameFile.length(), backup.length());

        // Verify content matches
        String backupContent = Files.readString(backup.toPath());
        assertEquals("Backup content should match original",
            originalContent, backupContent);
    }

    @Test
    public void testBackupRestore_RecoveryFromFailure() throws IOException {
        String originalContent = "original game version";
        Files.writeString(testGameFile.toPath(), originalContent);

        File backup = upgradeHandler.createBackup(testGameFile);
        assertNotNull(backup);

        // Simulate corruption/modification
        Files.writeString(testGameFile.toPath(), "corrupted data");

        // Restore from backup
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, backup);

        assertTrue("Restore should succeed", restored);
        String restoredContent = Files.readString(testGameFile.toPath());
        assertEquals("Content should match original after restore",
            originalContent, restoredContent);
    }

    @Test
    public void testPropertiesFilePersistence_SurvivesMultipleSaves() throws IOException {
        long timestamp1 = 1000000L;
        long size1 = 100000L;
        long timestamp2 = 2000000L;
        long size2 = 200000L;
        long timestamp3 = 3000000L;
        long size3 = 300000L;

        tracker.saveVersionInfo(timestamp1, size1);
        assertEquals(timestamp1, tracker.getLastKnownModificationTime());
        assertEquals(size1, tracker.getLastKnownSize());

        tracker.saveVersionInfo(timestamp2, size2);
        assertEquals(timestamp2, tracker.getLastKnownModificationTime());
        assertEquals(size2, tracker.getLastKnownSize());

        tracker.saveVersionInfo(timestamp3, size3);
        assertEquals(timestamp3, tracker.getLastKnownModificationTime());
        assertEquals(size3, tracker.getLastKnownSize());

        // Create new tracker to verify persistence
        GameVersionTracker newTracker = new GameVersionTracker(tempDir);
        assertEquals("Should read latest timestamp from file",
            timestamp3, newTracker.getLastKnownModificationTime());
        assertEquals("Should read latest size from file",
            size3, newTracker.getLastKnownSize());
    }

    @Test
    public void testUpdateDetection_DistinguishesUpgradeFromDowngrade() throws IOException {
        long currentTime = System.currentTimeMillis();
        long currentSize = testGameFile.length();

        // Test upgrade detection (larger size)
        tracker.saveVersionInfo(currentTime, currentSize - 100);
        assertEquals("Should detect upgrade when size increases",
            GameVersionTracker.UpdateStatus.UPGRADED,
            tracker.checkForUpdate(testGameFile));

        // Test downgrade detection (smaller size)
        tracker.saveVersionInfo(currentTime, currentSize + 100);
        assertEquals("Should detect downgrade when size decreases",
            GameVersionTracker.UpdateStatus.DOWNGRADED,
            tracker.checkForUpdate(testGameFile));

        // Test current state (same size, timestamp can differ)
        tracker.saveVersionInfo(currentTime - 10000, currentSize);
        testGameFile.setLastModified(currentTime); // Timestamp changes but size same
        assertEquals("Should detect no change when size unchanged",
            GameVersionTracker.UpdateStatus.CURRENT,
            tracker.checkForUpdate(testGameFile));
    }

    @Test
    public void testBackupNaming_UsesConsistentConvention() {
        File backup = upgradeHandler.createBackup(testGameFile);

        assertNotNull(backup);
        assertEquals("Backup should be in same directory",
            testGameFile.getParentFile(), backup.getParentFile());
        assertTrue("Backup should have .backup extension",
            backup.getName().endsWith(".backup"));
        assertEquals("Backup name should be original name + .backup",
            testGameFile.getName() + ".backup", backup.getName());
    }

    @Test
    public void testErrorHandling_MissingFile_GracefulDegradation() {
        File nonexistent = new File(tempDir, "missing.2mg");

        // Should not crash
        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(nonexistent);
        assertEquals("Should return UNKNOWN for missing file",
            GameVersionTracker.UpdateStatus.UNKNOWN, status);

        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(nonexistent);
        assertTrue("Should continue despite missing file", shouldContinue);

        File backup = upgradeHandler.createBackup(nonexistent);
        assertNull("Backup of missing file should return null", backup);
    }

    @Test
    public void testErrorHandling_CorruptedProperties_Recovers() throws IOException {
        // Create corrupted properties file
        Files.writeString(propertiesFile.toPath(), "garbage!@#$%^&*()");

        // Should not crash and should treat as unknown
        long timestamp = tracker.getLastKnownModificationTime();
        assertEquals("Should return -1 for corrupted file", -1L, timestamp);

        // Should be able to save new version info and recover
        long newTimestamp = testGameFile.lastModified();
        long newSize = testGameFile.length();
        tracker.saveVersionInfo(newTimestamp, newSize);

        assertEquals("Should successfully save timestamp after corruption",
            newTimestamp, tracker.getLastKnownModificationTime());
        assertEquals("Should successfully save size after corruption",
            newSize, tracker.getLastKnownSize());
    }
}
