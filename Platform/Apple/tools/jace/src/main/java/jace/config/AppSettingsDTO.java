package jace.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Plain data holder mirrored in JSON file. Add new fields carefully and set defaults.
 */
public class AppSettingsDTO {
    @JsonProperty
    public int version = 1;

    @JsonProperty
    public UiSettings ui = new UiSettings();

    public static class UiSettings {
        @JsonProperty
        public int windowWidth;

        @JsonProperty
        public int windowHeight;

        @JsonProperty
        public int windowSizeIndex;

        @JsonProperty
        public boolean fullscreen;

        @JsonProperty
        public String videoMode;

        @JsonProperty
        public double musicVolume;

        @JsonProperty
        public double sfxVolume;

        @JsonProperty
        public String soundtrackSelection;

        @JsonProperty
        public boolean aspectRatio;
    }
}