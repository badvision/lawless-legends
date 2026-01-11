package jace.lawless;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Integration test for version extraction from real game files.
 * This test requires a valid game.2mg file to be present.
 */
public class VersionExtractionIntegrationTest {

    /**
     * Finds a game file at standard test locations.
     * @return A game file if found, null otherwise
     */
    private File findGameFile() {
        String[] possiblePaths = {
            System.getProperty("user.home") + "/.jace/game.2mg",
            "/tmp/lawless-legends-test/game.2mg",
            "test-data/game.2mg",
            System.getProperty("user.home") + "/Documents/game.2mg"
        };

        for (String path : possiblePaths) {
            File candidate = new File(path);
            if (candidate.exists() && candidate.isFile() && candidate.length() > 0) {
                return candidate;
            }
        }

        return null;
    }

    @Test
    public void testVersionExtraction_WithRealGameFile() {
        File gameFile = findGameFile();

        if (gameFile == null) {
            System.out.println("SKIPPING: No game file found for integration test");
            System.out.println("To enable this test, place a game.2mg file at one of:");
            System.out.println("  - ~/.jace/game.2mg");
            System.out.println("  - /tmp/lawless-legends-test/game.2mg");
            System.out.println("  - test-data/game.2mg");
            return;
        }

        System.out.println("Running integration test with: " + gameFile.getAbsolutePath());
        System.out.println("File size: " + gameFile.length() + " bytes");

        // Extract version using GameVersionReader
        String version = GameVersionReader.extractVersion(gameFile);

        // Validate results
        assertNotNull("Version should not be null for valid game file", version);
        assertTrue("Version should not be empty", version.length() > 0);
        assertTrue("Version should be reasonable length (< 50 chars)", version.length() < 50);

        System.out.println("Successfully extracted version: " + version);
        System.out.println("Version length: " + version.length() + " characters");

        // Version string should contain alphanumeric characters and possibly dots
        assertTrue("Version should contain alphanumeric characters",
            version.matches(".*[a-zA-Z0-9].*"));
    }

    @Test
    public void testVersionTracking_WithRealGameFile() throws Exception {
        File gameFile = findGameFile();

        if (gameFile == null) {
            System.out.println("SKIPPING: No game file found for version tracking test");
            return;
        }

        System.out.println("Testing version tracking with: " + gameFile.getAbsolutePath());

        // Create a temporary directory for tracking
        File tempDir = java.nio.file.Files.createTempDirectory("version-tracking-test").toFile();
        try {
            GameVersionTracker tracker = new GameVersionTracker(tempDir);

            // First check - should return UNKNOWN
            GameVersionTracker.UpdateStatus status1 = tracker.checkForUpdate(gameFile);
            assertEquals("First check should return UNKNOWN",
                GameVersionTracker.UpdateStatus.UNKNOWN, status1);

            // Save version info
            String version = GameVersionReader.extractVersion(gameFile);
            tracker.saveVersionInfo(gameFile.lastModified(), gameFile.length(), version);

            System.out.println("Saved version: " + version);

            // Second check - should return CURRENT
            GameVersionTracker.UpdateStatus status2 = tracker.checkForUpdate(gameFile);
            assertEquals("Second check should return CURRENT",
                GameVersionTracker.UpdateStatus.CURRENT, status2);

            // Verify we can retrieve the saved version
            String savedVersion = tracker.getLastKnownVersion();
            assertEquals("Saved version should match", version, savedVersion);

            System.out.println("Version tracking test passed");

        } finally {
            // Cleanup
            deleteDirectory(tempDir);
        }
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
    public void testUpgradeDetection_VersionChange() throws Exception {
        File gameFile = findGameFile();

        if (gameFile == null) {
            System.out.println("SKIPPING: No game file found for upgrade detection test");
            return;
        }

        System.out.println("Testing upgrade detection with: " + gameFile.getAbsolutePath());

        File tempDir = java.nio.file.Files.createTempDirectory("upgrade-detection-test").toFile();
        try {
            GameVersionTracker tracker = new GameVersionTracker(tempDir);

            // Save with a different version string
            tracker.saveVersionInfo(gameFile.lastModified(), gameFile.length(), "1.0.0.fake");

            // Check for update - should detect version change
            GameVersionTracker.UpdateStatus status = tracker.checkForUpdate(gameFile);

            // Should detect as upgraded because version string changed
            assertEquals("Should detect version change as UPGRADED",
                GameVersionTracker.UpdateStatus.UPGRADED, status);

            System.out.println("Upgrade detection test passed");

        } finally {
            deleteDirectory(tempDir);
        }
    }
}
