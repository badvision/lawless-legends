package jace.lawless;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jace.core.SoundMixer;
import jace.core.SoundMixer.SoundBuffer;
import jace.core.SoundMixer.SoundError;

public class MediaPlayer {

    double vol = 1.0;
    int repeats = 0;
    int maxRepetitions = 1;
    Status status = Status.NOT_STARTED;
    Media song;
    SoundBuffer playbackBuffer;
    Executor executor = Executors.newSingleThreadExecutor();

    public static enum Status {
        NOT_STARTED, PLAYING, PAUSED, STOPPED
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

    // NOTE: Once a song is stopped, it cannot be restarted.
    public void stop() {
        status = Status.STOPPED;
        try {
            if (playbackBuffer != null) {
                playbackBuffer.shutdown();
            }
        } catch (InterruptedException | ExecutionException | SoundError e) {
            // Ignore exception on shutdown
        } finally {
            song.close();
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

    public void pause() {
        status = Status.PAUSED;
    }

    public void play() {
        if (status == Status.STOPPED) {
            return;
        } else if (status == Status.NOT_STARTED) {
            repeats = 0;
            if (playbackBuffer == null || !playbackBuffer.isAlive()) {
                try {
                    playbackBuffer = SoundMixer.createBuffer(true);
                } catch (InterruptedException | ExecutionException | SoundError e) {
                    stop();
                    return;
                }
                if (playbackBuffer == null) {
                    stop();
                    return;
                }
            }
        }
        executor.execute(() -> {
            status = Status.PLAYING;
            System.out.println("Song playback thread started");
            while (status == Status.PLAYING && (maxRepetitions == INDEFINITE || repeats < maxRepetitions)) {
                if (song.isEnded()) {
                    if (maxRepetitions == INDEFINITE) {
                        song.restart();
                    } else {
                        repeats++;
                        if (repeats < maxRepetitions) {
                            song.restart();
                        } else {
                            System.out.println("Song ended");
                            this.stop();
                            break;
                        }
                    }
                }
                try {
                    playbackBuffer.playSample((short) (song.getNextLeftSample() * vol));
                    playbackBuffer.playSample((short) (song.getNextRightSample() * vol));
                } catch (InterruptedException | ExecutionException | SoundError e) {
                    e.printStackTrace();
                    this.stop();
                }
            }
        });
    }
}