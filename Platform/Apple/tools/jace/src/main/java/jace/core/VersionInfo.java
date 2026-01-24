package jace.core;

import jace.lawless.GameVersionReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for providing version information
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class VersionInfo {
    private static final Logger LOG = Logger.getLogger(VersionInfo.class.getName());
    private static String appVersion = "1.0";
    private static String gameDataVersion = null;
    private static String buildDate = "";

    static {
        loadVersionInfo();
    }

    /**
     * Load version information from properties file and game disk
     */
    private static void loadVersionInfo() {
        // Load app version from properties file
        Properties properties = new Properties();
        try (InputStream input = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                properties.load(input);
                appVersion = properties.getProperty("version", "1.0");
                buildDate = properties.getProperty("build.date", "");
            } else {
                LOG.log(Level.WARNING, "version.properties file not found in classpath");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load version information", e);
        }

        // Load game data version from packaged game disk
        try (InputStream gameStream = VersionInfo.class.getResourceAsStream("/jace/data/game.2mg")) {
            if (gameStream != null) {
                gameDataVersion = GameVersionReader.extractVersion(gameStream);
                if (gameDataVersion != null) {
                    LOG.log(Level.INFO, "Loaded game data version: " + gameDataVersion);
                } else {
                    LOG.log(Level.WARNING, "Failed to extract game data version from packaged game disk");
                }
            } else {
                LOG.log(Level.WARNING, "Packaged game disk not found in resources");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load game data version", e);
        }
    }
    
    /**
     * Get the current application version (from pom.xml)
     *
     * @return The app version string
     */
    public static String getAppVersion() {
        return appVersion;
    }

    /**
     * Get the game data version (from packaged game disk)
     *
     * @return The game data version string, or null if not available
     */
    public static String getGameDataVersion() {
        return gameDataVersion;
    }

    /**
     * Get the combined version string (app-version-gamedata-version)
     *
     * @return The combined version string
     */
    public static String getVersion() {
        if (gameDataVersion != null && !gameDataVersion.isEmpty()) {
            return appVersion + "-" + gameDataVersion;
        }
        return appVersion;
    }

    /**
     * Get the build date
     *
     * @return The build date string
     */
    public static String getBuildDate() {
        return buildDate;
    }

    /**
     * Get the version display string
     *
     * @return The formatted version display string
     */
    public static String getVersionDisplay() {
        String version = getVersion();
        if (buildDate.isEmpty()) {
            return "Version " + version;
        } else {
            return "Version " + version.trim() + " (" + buildDate.trim() + ")";
        }
    }
} 