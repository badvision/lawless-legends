package jace.lawless;

import java.time.Duration;

public class MediaPlayer {

    double vol = 1.0;
    int repeats = 0;
    int maxRepetitions = 1;
    Status status = Status.STOPPED;

    public static enum Status {
        PLAYING, PAUSED, STOPPED
    }

    public static final int INDEFINITE = -1;

    public MediaPlayer(Media song) {
    }

    public Status getStatus() {
        return null;
    }

    public Duration getCurrentTime() {
        return null;
    }

    public double getVolume() {
        return vol;
    }

    public void stop() {
        status = Status.STOPPED;
    }

    public void setCycleCount(int i) {
        maxRepetitions = i;
    }

    public void setVolume(double d) {
        vol = Math.max(0.0, Math.min(1.0, d));
    }

    public void setStartTime(javafx.util.Duration millis) {
    }

    public void play() {
        repeats = 0;
        status = Status.PLAYING;
    }



}