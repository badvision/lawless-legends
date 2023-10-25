package jace.lawless;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jace.core.SoundMixer;
import jace.core.SoundMixer.SoundBuffer;

public class MediaPlayer {

    double vol = 1.0;
    int repeats = 0;
    int maxRepetitions = 1;
    Status status = Status.STOPPED;
    Media song;
    SoundBuffer playbackBuffer;
    Executor executor = Executors.newSingleThreadExecutor();

    public static enum Status {
        PLAYING, PAUSED, STOPPED
    }

    public static final int INDEFINITE = -1;

    public MediaPlayer(Media song) {
        this.song = song;
    }

    public Status getStatus() {
        return status;
    }

    public Duration getCurrentTime() {
        return song.getCurrentTime();
    }

    public double getVolume() {
        return vol;
    }

    public void stop() {
        status = Status.STOPPED;
        try {
            playbackBuffer.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            // Ignore exception on shutdown
        }
    }

    public void setCycleCount(int i) {
        maxRepetitions = i;
    }

    public void setVolume(double d) {
        vol = Math.max(0.0, Math.min(1.0, d));
    }

    public void setStartTime(javafx.util.Duration millis) {
        song.seekToTime(millis);
    }

    public void play() {
        repeats = 0;
        status = Status.PLAYING;
        if (playbackBuffer == null || !playbackBuffer.isAlive()) {
            playbackBuffer = SoundMixer.createBuffer(true);
        }
        executor.execute(() -> {
            while (status == Status.PLAYING && (maxRepetitions == INDEFINITE || repeats < maxRepetitions)) {
                if (song.isEnded()) {
                    if (maxRepetitions == INDEFINITE) {
                        song.restart();
                    } else {
                        repeats++;
                        if (repeats < maxRepetitions) {
                            song.restart();
                        } else {
                            this.stop();
                            break;
                        }
                    }
                }
                short leftSample = song.getNextLeftSample();
                short rightSample = song.getNextRightSample();
                try {
                    playbackBuffer.playSample((short) (leftSample * vol));
                    playbackBuffer.playSample((short) (rightSample * vol));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    this.stop();
                }
            }
        });
    }
}