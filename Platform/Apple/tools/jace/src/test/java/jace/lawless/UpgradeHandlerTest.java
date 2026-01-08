package jace.lawless;

import jace.core.Utility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class UpgradeHandlerTest {

    private File tempDir;
    private File testGameFile;
    private GameVersionTracker tracker;
    private LawlessImageTool imageTool;
    private UpgradeHandler upgradeHandler;

    @Before
    public void setUp() throws IOException {
        Utility.setHeadlessMode(true);
        tempDir = Files.createTempDirectory("lawless-upgrade-test").toFile();
        testGameFile = new File(tempDir, "game.2mg");
        Files.writeString(testGameFile.toPath(), "test game content");
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
    public void testCreateBackup_Success() {
        File backup = upgradeHandler.createBackup(testGameFile);

        assertNotNull("Backup should be created", backup);
        assertTrue("Backup file should exist", backup.exists());
        assertEquals("Backup should have same size as original",
            testGameFile.length(), backup.length());
        assertTrue("Backup should have .backup extension",
            backup.getName().endsWith(".backup"));
    }

    @Test
    public void testCreateBackup_NonexistentFile() {
        File nonexistent = new File(tempDir, "nonexistent.2mg");
        File backup = upgradeHandler.createBackup(nonexistent);

        assertNull("Backup of nonexistent file should return null", backup);
    }

    @Test
    public void testCreateBackup_NullFile() {
        File backup = upgradeHandler.createBackup(null);

        assertNull("Backup of null file should return null", backup);
    }

    @Test
    public void testRestoreFromBackup_Success() throws IOException {
        // Create backup
        File backup = upgradeHandler.createBackup(testGameFile);
        assertNotNull(backup);

        // Modify original
        Files.writeString(testGameFile.toPath(), "modified content");
        assertNotEquals("File should be modified",
            backup.length(), testGameFile.length());

        // Restore
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, backup);

        assertTrue("Restore should succeed", restored);
        assertEquals("File should match backup after restore",
            backup.length(), testGameFile.length());
    }

    @Test
    public void testRestoreFromBackup_NonexistentBackup() {
        File nonexistent = new File(tempDir, "nonexistent.backup");
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, nonexistent);

        assertFalse("Restore should fail with nonexistent backup", restored);
    }

    @Test
    public void testRestoreFromBackup_NullBackup() {
        boolean restored = upgradeHandler.restoreFromBackup(testGameFile, null);

        assertFalse("Restore should fail with null backup", restored);
    }

    @Test
    public void testCheckAndHandleUpgrade_FirstRun_RecordsTimestamp() {
        // First run - no properties file exists
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile);

        assertTrue("Should continue on first run", shouldContinue);
        long recorded = tracker.getLastKnownModificationTime();
        assertTrue("Timestamp should be recorded", recorded > 0);
        assertEquals("Recorded timestamp should match file",
            testGameFile.lastModified(), recorded);
    }

    @Test
    public void testCheckAndHandleUpgrade_CurrentVersion_NoPrompt() throws IOException {
        // Record current version
        tracker.saveModificationTime(testGameFile.lastModified());

        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(testGameFile);

        assertTrue("Should continue with current version", shouldContinue);
    }

    @Test
    public void testCheckAndHandleUpgrade_NonexistentFile() {
        File nonexistent = new File(tempDir, "nonexistent.2mg");
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(nonexistent);

        assertTrue("Should continue even with nonexistent file", shouldContinue);
    }

    @Test
    public void testCheckAndHandleUpgrade_NullFile() {
        boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(null);

        assertTrue("Should continue even with null file", shouldContinue);
    }

    @Test
    public void testShowUpgradeConfirmation_HeadlessMode_ReturnsSkip() {
        Utility.setHeadlessMode(true);
        UpgradeHandler.UpgradeDecision decision = upgradeHandler.showUpgradeConfirmation();

        assertEquals("Headless mode should return SKIP",
            UpgradeHandler.UpgradeDecision.SKIP, decision);
    }
}
