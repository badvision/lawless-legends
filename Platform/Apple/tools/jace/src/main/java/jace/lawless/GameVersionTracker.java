package jace.lawless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks game disk version using FILE SIZE to detect when upgrades are available.
 * Uses a properties file to persist the last known size and modification time.
 *
 * SIZE is the primary version indicator because:
 * - ProDOS disk images are fixed size
 * - New versions from developers = different size = upgrade needed
 * - Normal gameplay saves = same size = no upgrade (even if timestamp changes)
 */
public class GameVersionTracker {

    private static final Logger LOGGER = Logger.getLogger(GameVersionTracker.class.getName());
    private static final String PROPERTIES_FILENAME = "game-version.properties";
    private static final String LAST_MODIFIED_KEY = "lastModified";
    private static final String LAST_SIZE_KEY = "lastSize";
    private static final String LAST_VERSION_KEY = "lastVersion";

    public enum UpdateStatus {
        CURRENT,      // No change detected
        UPGRADED,     // Newer version detected
        DOWNGRADED,   // Older version detected (unusual but possible)
        UNKNOWN       // No previous version information
    }

    private final File storageDirectory;
    private final Properties props;

    public GameVersionTracker(File storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.props = new Properties();
        loadProperties();
    }

    /**
     * Loads the properties file into memory, if it exists.
     */
    private void loadProperties() {
        File propsFile = new File(storageDirectory, PROPERTIES_FILENAME);
        if (propsFile.exists()) {
            try (FileInputStream in = new FileInputStream(propsFile)) {
                props.load(in);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to read game version properties", e);
            }
        }
    }

    /**
     * Saves the properties to the file.
     * @throws IOException if unable to write
     */
    private void saveProperties() throws IOException {
        File propsFile = new File(storageDirectory, PROPERTIES_FILENAME);
        try (FileOutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "Lawless Legends Game Version Tracking");
        }
    }

    /**
     * Gets the last known modification time from the properties file.
     *
     * @return The last known modification timestamp, or -1 if not available
     */
    public long getLastKnownModificationTime() {
        String value = props.getProperty(LAST_MODIFIED_KEY);
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Unable to parse modification time", e);
            return -1L;
        }
    }

    /**
     * Gets the last known file size from the properties file.
     *
     * @return The last known file size in bytes, or -1 if not available
     */
    public long getLastKnownSize() {
        String value = props.getProperty(LAST_SIZE_KEY);
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Unable to parse file size", e);
            return -1L;
        }
    }

    /**
     * Gets the last known version string from the properties file.
     *
     * @return The last known version string (e.g., "5123a.2"), or null if not available
     */
    public String getLastKnownVersion() {
        String value = props.getProperty(LAST_VERSION_KEY);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * Saves the modification time to the properties file.
     *
     * @deprecated Use {@link #saveVersionInfo(long, long)} instead to track both timestamp and size.
     * @param modificationTime The timestamp to save
     * @throws IOException if unable to write the properties file
     */
    @Deprecated
    public void saveModificationTime(long modificationTime) throws IOException {
        props.setProperty(LAST_MODIFIED_KEY, String.valueOf(modificationTime));
        saveProperties();
    }

    /**
     * Saves both modification time and file size to the properties file.
     * This is the preferred method for tracking version information.
     *
     * @param modificationTime The timestamp to save
     * @param fileSize The file size in bytes to save
     * @throws IOException if unable to write the properties file
     */
    public void saveVersionInfo(long modificationTime, long fileSize) throws IOException {
        props.setProperty(LAST_MODIFIED_KEY, String.valueOf(modificationTime));
        props.setProperty(LAST_SIZE_KEY, String.valueOf(fileSize));
        saveProperties();
        LOGGER.info("Saved version info: size=" + fileSize + " bytes, modified=" + modificationTime);
    }

    /**
     * Saves version information including the version string.
     * This is the most reliable method for tracking version information.
     *
     * @param modificationTime The timestamp to save
     * @param fileSize The file size in bytes to save
     * @param version The version string (e.g., "5123a.2"), or null to skip version tracking
     * @throws IOException if unable to write the properties file
     */
    public void saveVersionInfo(long modificationTime, long fileSize, String version) throws IOException {
        props.setProperty(LAST_MODIFIED_KEY, String.valueOf(modificationTime));
        props.setProperty(LAST_SIZE_KEY, String.valueOf(fileSize));
        if (version != null && !version.trim().isEmpty()) {
            props.setProperty(LAST_VERSION_KEY, version);
        }
        saveProperties();
        String versionInfo = (version != null) ? ", version=" + version : "";
        LOGGER.info("Saved version info: size=" + fileSize + " bytes, modified=" + modificationTime + versionInfo);
    }

    /**
     * Checks if the game file has been updated since the last known version.
     * Uses VERSION STRING comparison as the primary indicator (most reliable).
     * Falls back to FILE SIZE comparison if version extraction fails.
     * Normal gameplay saves change the timestamp but not the size/version, so they won't trigger upgrades.
     *
     * @param gameFile The game disk file to check
     * @return The update status
     */
    public UpdateStatus checkForUpdate(File gameFile) {
        if (gameFile == null || !gameFile.exists()) {
            return UpdateStatus.UNKNOWN;
        }

        long currentModTime = gameFile.lastModified();
        long currentSize = gameFile.length();
        long lastKnownModTime = getLastKnownModificationTime();
        long lastKnownSize = getLastKnownSize();
        String lastKnownVersion = getLastKnownVersion();

        // First run - no version info exists
        if (lastKnownModTime == -1L || lastKnownSize == -1L) {
            LOGGER.info("No version info found - first run");
            return UpdateStatus.UNKNOWN;
        }

        // METHOD 1: Try version string comparison (most reliable)
        String currentVersion = GameVersionReader.extractVersion(gameFile);
        if (currentVersion != null && lastKnownVersion != null) {
            if (!currentVersion.equals(lastKnownVersion)) {
                LOGGER.info("Version changed: " + lastKnownVersion + " -> " + currentVersion +
                           " (version string comparison)");
                // Since we can't reliably order version strings, treat any change as upgrade
                return UpdateStatus.UPGRADED;
            } else {
                LOGGER.fine("Version unchanged: " + currentVersion + " (version string comparison)");
                return UpdateStatus.CURRENT;
            }
        }

        // METHOD 2: Fall back to size comparison if version extraction failed
        LOGGER.fine("Using size-based version detection (version string not available)");
        if (currentSize != lastKnownSize) {
            LOGGER.info("Size changed: " + lastKnownSize + " -> " + currentSize + " bytes (size comparison fallback)");
            return currentSize > lastKnownSize ? UpdateStatus.UPGRADED : UpdateStatus.DOWNGRADED;
        }

        // Size unchanged = same version (ignore timestamp changes from gameplay)
        if (currentModTime != lastKnownModTime) {
            LOGGER.fine("Timestamp changed but size unchanged - gameplay activity, not upgrade");
        }
        return UpdateStatus.CURRENT;
    }
}
