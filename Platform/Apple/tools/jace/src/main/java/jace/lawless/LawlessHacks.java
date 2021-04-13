package jace.lawless;

import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.lawless.LawlessVideo.RenderEngine;
import javafx.beans.property.DoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * Hacks that affect lawless legends gameplay
 */
public class LawlessHacks extends Cheats {

    // Modes specified by the game engine
    int MODE_SOFTSWITCH_MIN = 0x0C049;
    int MODE_SOFTSWITCH_MAX = 0x0C04F;
    int SFX_TRIGGER = 0x0C069;

    public LawlessHacks(Computer computer) {
        super(computer);
    }

    @Override
    public void toggleCheats() {
        // Do nothing -- you cannot toggle this once it's active.
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat(RAMEvent.TYPE.ANY, false, (e) -> {
            int addr = e.getAddress();
            if (addr >= MODE_SOFTSWITCH_MIN && e.getAddress() <= MODE_SOFTSWITCH_MAX) {
                System.out.println("Trapped " + e.getType().toString() + " to $" + Integer.toHexString(e.getAddress()));
                setEngineByOrdinal(e.getAddress() - MODE_SOFTSWITCH_MIN);
            }
        }, MODE_SOFTSWITCH_MIN, MODE_SOFTSWITCH_MAX);
        addCheat(RAMEvent.TYPE.WRITE, false, (e) -> {
            playSound(e.getNewValue());
        }, SFX_TRIGGER, SFX_TRIGGER);
    }

    @Override
    public String getDeviceName() {
        return "Lawless Legends optimizations";
    }

    @Override
    public void tick() {
    }

    private void setEngineByOrdinal(int mode) {
        LawlessVideo video = (LawlessVideo) computer.getVideo();
        if (mode >= 0 && mode < RenderEngine.values().length) {
            video.setEngine(RenderEngine.values()[mode]);
        } else {
            video.setEngine(RenderEngine.UNKNOWN);
        }
    }

    int currentSong;
    Thread playbackEffect;
    MediaPlayer currentSongPlayer;
    MediaPlayer currentSfxPlayer;

    private void playSound(int soundNumber) {
        boolean isMusic = soundNumber >= 0;
        int track = soundNumber & 0x07f;
        if (track == 0) {
            if (isMusic) {
//                System.out.println("Stop music");
                stopMusic();
            } else {
//                System.out.println("Stop sfx");
                stopSfx();
            }
        } else if (isMusic) {
//            System.out.println("Play music "+track);
            playMusic(track);
        } else {
//            System.out.println("Play sfx "+track);
            playSfx(track);
        }
    }

    private Media getAudioTrack(String file) {
        String pathStr = "jace/data/sound/" + file;
//        System.out.println("looking in "+pathStr);
        URL path = getClass().getClassLoader().getResource(pathStr);
        if (path == null) {
            return null;
        }
        try {
            return new Media(path.toURI().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void playMusic(int track) {
        if (currentSong != track) {
            fadeOutSong(() -> startNewSong(track));
        } else {
            new Thread(() -> startNewSong(track)).start();
        }
    }

    private void stopSongEffect() {
        if (playbackEffect != null && playbackEffect.isAlive()) {
            playbackEffect.interrupt();
            Thread.yield();
        }
        playbackEffect = null;
    }

    private void fadeOutSong(Runnable nextAction) {
        stopSongEffect();
        if (currentSongPlayer != null) {
            playbackEffect = new Thread(() -> {
                DoubleProperty volume = currentSongPlayer.volumeProperty();
                while (playbackEffect == Thread.currentThread() && currentSongPlayer != null && volume.get() > 0.0) {
//                    System.out.println("Fading down: " + volume.get());
                    volume.set(volume.get() - FADE_AMT);
                    try {
                        Thread.sleep(FADE_SPEED);
                    } catch (InterruptedException e) {
                        playbackEffect = null;
                        return;
                    }
                }
                currentSongPlayer.stop();
                currentSongPlayer = null;
                if (nextAction != null) {
                    nextAction.run();
                }
            });
            playbackEffect.start();
        } else if (nextAction != null) {
            new Thread(nextAction).start();
        }
    }

    double FADE_AMT = 0.05; // 5% per interval, or 20 stops between 0% and 100%
    int FADE_SPEED = 100; // 100ms per 5%, or 2 second duration

    private void startNewSong(int track) {
        if (track != currentSong || currentSongPlayer == null) {
            // If the same song is already playing don't restart it
            Media song = getAudioTrack("BGM-" + track + ".mp3");
            if (song == null) {
                System.out.println("Unable to start song " + track + "; File not found");
                return;
            }
            currentSongPlayer = new MediaPlayer(song);
            currentSongPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            currentSongPlayer.setVolume(0.0);
            currentSongPlayer.play();
            currentSong = track;
        }
        // But if the same song was already playing but possibly fading out
        // then this will fade it back in neatly.
        stopSongEffect();
//        System.out.println("Starting "+track+" NOW");
        playbackEffect = new Thread(() -> {
            DoubleProperty volume = currentSongPlayer.volumeProperty();
            while (playbackEffect == Thread.currentThread() && currentSongPlayer != null && volume.get() < 1.0) {
                volume.set(volume.get() + FADE_AMT);
//                System.out.println("Fading up: " + volume.get());
                try {
                    Thread.sleep(FADE_SPEED);
                } catch (InterruptedException e) {
                    playbackEffect = null;
                    return;
                }
            }
        });
        playbackEffect.start();
    }

    private void stopMusic() {
        stopSongEffect();
        fadeOutSong(null);
    }

    private void playSfx(int track) {
        new Thread(() -> {
            Media sfx = getAudioTrack("SFX-" + track + ".mp3");
            if (sfx == null) {
                System.out.println("Unable to start SFX " + track + "; File not found");
                return;
            }
            currentSfxPlayer = new MediaPlayer(sfx);
            currentSfxPlayer.setCycleCount(1);
            currentSfxPlayer.play();
        }).start();
    }

    private void stopSfx() {
        if (currentSfxPlayer != null) {
            currentSfxPlayer.stop();
            currentSfxPlayer = null;
        }
    }

}
