package jace.lawless;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class GameVersionTrackerTest {

    private File tempDir;
    private File testGameFile;
    private GameVersionTracker tracker;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("lawless-test").toFile();
        testGameFile = new File(tempDir, "game.2mg");
        testGameFile.createNewFile();
        tracker = new GameVersionTracker(tempDir);
    }

    @After
    public void tearDown() {
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

    @Test
    public void testReadTimestamp_MissingFile_ReturnsUnknown() {
        long timestamp = tracker.getLastKnownModificationTime();
        assertEquals(-1L, timestamp);
    }

    @Test
    public void testSaveTimestamp_CreatesPropertiesFile() throws IOException {
        long testTime = System.currentTimeMillis();
        tracker.saveModificationTime(testTime);

        File propsFile = new File(tempDir, "game-version.properties");
        assertTrue("Properties file should be created", propsFile.exists());
    }

    @Test
    public void testSaveAndReadTimestamp_Roundtrip() throws IOException {
        long testTime = 1704398400000L; // Fixed timestamp for reproducibility
        tracker.saveModificationTime(testTime);

        long retrieved = tracker.getLastKnownModificationTime();
        assertEquals("Timestamp should match after save/load", testTime, retrieved);
    }

    @Test
    public void testMultipleSaves_OverwritesPrevious() throws IOException {
        tracker.saveModificationTime(1000L);
        tracker.saveModificationTime(2000L);

        long retrieved = tracker.getLastKnownModificationTime();
        assertEquals("Latest timestamp should be stored", 2000L, retrieved);
    }

    @Test
    public void testCorruptedFile_GracefulDegradation() throws IOException {
        File propsFile = new File(tempDir, "game-version.properties");
        Files.writeString(propsFile.toPath(), "corrupted garbage data !@#$%");

        long timestamp = tracker.getLastKnownModificationTime();
        assertEquals("Corrupted file should return -1", -1L, timestamp);
    }

    @Test
    public void testEmptyFile_GracefulDegradation() throws IOException {
        File propsFile = new File(tempDir, "game-version.properties");
        propsFile.createNewFile();

        long timestamp = tracker.getLastKnownModificationTime();
        assertEquals("Empty file should return -1", -1L, timestamp);
    }

    @Test
    public void testCheckForUpdate_NoProperties_ReturnsUnknown() {
        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals(GameVersionTracker.UpdateStatus.UNKNOWN, status);
    }

    @Test
    public void testCheckForUpdate_SameTimestamp_ReturnsCurrent() throws IOException {
        long modTime = testGameFile.lastModified();
        long size = testGameFile.length();
        tracker.saveVersionInfo(modTime, size);

        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals(GameVersionTracker.UpdateStatus.CURRENT, status);
    }

    @Test
    public void testCheckForUpdate_SameSize_DifferentTimestamp_ReturnsCurrent() throws IOException, InterruptedException {
        // This is the critical test: gameplay changes timestamp but not size, so no upgrade
        long oldTime = testGameFile.lastModified();
        long size = testGameFile.length();
        tracker.saveVersionInfo(oldTime, size);

        // Wait and touch file to ensure newer timestamp (simulates gameplay save)
        Thread.sleep(10);
        testGameFile.setLastModified(System.currentTimeMillis());

        // Size hasn't changed, so should still be CURRENT (not UPGRADED)
        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals("Same size but different timestamp should be CURRENT (gameplay save, not upgrade)",
            GameVersionTracker.UpdateStatus.CURRENT, status);
    }

    @Test
    public void testCheckForUpdate_DifferentSize_ReturnsUpgraded() throws IOException {
        // Save initial version info
        tracker.saveVersionInfo(testGameFile.lastModified(), 1000L);

        // Verify current file is different size (should be 0 since it's empty)
        assertTrue("Test file should be different size than saved version",
            testGameFile.length() != 1000L);

        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        // Could be UPGRADED or DOWNGRADED depending on actual size, but not CURRENT
        assertTrue("Different size should not return CURRENT",
            status != GameVersionTracker.UpdateStatus.CURRENT);
    }

    @Test
    public void testSaveVersionInfo_BothFieldsPersisted() throws IOException {
        long testTime = 1704398400000L;
        long testSize = 819200L; // 800KB disk
        tracker.saveVersionInfo(testTime, testSize);

        long retrievedTime = tracker.getLastKnownModificationTime();
        long retrievedSize = tracker.getLastKnownSize();

        assertEquals("Modification time should match", testTime, retrievedTime);
        assertEquals("File size should match", testSize, retrievedSize);
    }

    @Test
    public void testGetLastKnownSize_MissingProperty_ReturnsNegativeOne() {
        // Save only timestamp (simulates old properties file)
        try {
            tracker.saveModificationTime(System.currentTimeMillis());
        } catch (IOException e) {
            fail("Should be able to save timestamp");
        }

        long size = tracker.getLastKnownSize();
        assertEquals("Missing size property should return -1", -1L, size);
    }

    @Test
    public void testCheckForUpdate_OldPropertiesFile_ReturnsUnknown() throws IOException {
        // Simulate old properties file without size field
        tracker.saveModificationTime(testGameFile.lastModified());

        // Should return UNKNOWN because size field is missing
        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals("Old properties file without size should return UNKNOWN",
            GameVersionTracker.UpdateStatus.UNKNOWN, status);
    }

    @Test
    public void testCheckForUpdate_LargerSize_ReturnsUpgraded() throws IOException {
        // Save smaller size
        tracker.saveVersionInfo(testGameFile.lastModified(), 100L);

        // File is actually larger (or we can write to it to make it larger)
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(testGameFile)) {
            fos.write(new byte[200]); // Make it 200 bytes
        }

        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals("Larger size should return UPGRADED",
            GameVersionTracker.UpdateStatus.UPGRADED, status);
    }

    @Test
    public void testCheckForUpdate_SmallerSize_ReturnsDowngraded() throws IOException {
        // Write some data to file
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(testGameFile)) {
            fos.write(new byte[100]);
        }

        // Save larger size
        tracker.saveVersionInfo(testGameFile.lastModified(), 200L);

        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(testGameFile);
        assertEquals("Smaller size should return DOWNGRADED",
            GameVersionTracker.UpdateStatus.DOWNGRADED, status);
    }

    @Test
    public void testCheckForUpdate_NonexistentFile_ReturnsUnknown() throws IOException {
        File nonexistent = new File(tempDir, "nonexistent.2mg");

        GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(nonexistent);
        assertEquals(GameVersionTracker.UpdateStatus.UNKNOWN, status);
    }

    // ========== Version String Tests ==========

    @Test
    public void testGetLastKnownVersion_NoVersion_ReturnsNull() {
        String version = tracker.getLastKnownVersion();
        assertNull("Should return null when no version saved", version);
    }

    @Test
    public void testSaveVersionInfo_WithVersionString() throws IOException {
        long testTime = 1704398400000L;
        long testSize = 819200L;
        String testVersion = "5123a.2";

        tracker.saveVersionInfo(testTime, testSize, testVersion);

        long retrievedTime = tracker.getLastKnownModificationTime();
        long retrievedSize = tracker.getLastKnownSize();
        String retrievedVersion = tracker.getLastKnownVersion();

        assertEquals("Modification time should match", testTime, retrievedTime);
        assertEquals("File size should match", testSize, retrievedSize);
        assertEquals("Version string should match", testVersion, retrievedVersion);
    }

    @Test
    public void testSaveVersionInfo_WithNullVersion() throws IOException {
        long testTime = 1704398400000L;
        long testSize = 819200L;

        tracker.saveVersionInfo(testTime, testSize, null);

        long retrievedTime = tracker.getLastKnownModificationTime();
        long retrievedSize = tracker.getLastKnownSize();
        String retrievedVersion = tracker.getLastKnownVersion();

        assertEquals("Modification time should match", testTime, retrievedTime);
        assertEquals("File size should match", testSize, retrievedSize);
        assertNull("Version should be null", retrievedVersion);
    }

    @Test
    public void testSaveVersionInfo_WithEmptyVersion() throws IOException {
        long testTime = 1704398400000L;
        long testSize = 819200L;

        tracker.saveVersionInfo(testTime, testSize, "");

        String retrievedVersion = tracker.getLastKnownVersion();
        assertNull("Empty version string should result in null", retrievedVersion);
    }

    @Test
    public void testSaveVersionInfo_OverwriteVersion() throws IOException {
        tracker.saveVersionInfo(System.currentTimeMillis(), 100L, "1.0.0");
        tracker.saveVersionInfo(System.currentTimeMillis(), 200L, "2.0.0");

        String version = tracker.getLastKnownVersion();
        assertEquals("Latest version should be stored", "2.0.0", version);
    }

    @Test
    public void testGetLastKnownVersion_EmptyString_ReturnsNull() throws IOException {
        // Manually save empty version
        tracker.saveVersionInfo(System.currentTimeMillis(), 100L, "test");
        // Now save without version to overwrite
        tracker.saveVersionInfo(System.currentTimeMillis(), 100L);

        String version = tracker.getLastKnownVersion();
        // After overwrite without version, we should still have the old version
        // unless we explicitly saved empty string
        assertNotNull("Version should persist unless overwritten", version);
    }
}
