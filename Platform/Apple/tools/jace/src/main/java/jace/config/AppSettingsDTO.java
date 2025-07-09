package jace.config;

/**
 * Plain data holder mirrored in JSON file. Add new fields carefully and set defaults.
 */
public class AppSettingsDTO {
    public int version = 1;
    public UiSettings ui = new UiSettings();

    public static class UiSettings {
        public int windowWidth;
        public int windowHeight;
        public int windowSizeIndex;
        public boolean fullscreen;
        public String videoMode;
        public double musicVolume;
        public double sfxVolume;
        public String soundtrackSelection;
        public boolean aspectRatio;
    }
} 