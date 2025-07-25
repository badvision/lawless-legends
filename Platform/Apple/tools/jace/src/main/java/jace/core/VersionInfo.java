package jace.core;

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
    private static String version = "1.0";
    private static String buildDate = "";
    
    static {
        loadVersionInfo();
    }
    
    /**
     * Load version information from properties file
     */
    private static void loadVersionInfo() {
        Properties properties = new Properties();
        try (InputStream input = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                properties.load(input);
                version = properties.getProperty("version", "1.0");
                buildDate = properties.getProperty("build.date", "");
            } else {
                LOG.log(Level.WARNING, "version.properties file not found in classpath");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load version information", e);
        }
    }
    
    /**
     * Get the current application version
     * 
     * @return The version string
     */
    public static String getVersion() {
        return version;
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
        if (buildDate.isEmpty()) {
            return "Version " + version;
        } else {
            return "Version " + version.trim() + " (" + buildDate.trim() + ")";
        }
    }
} 