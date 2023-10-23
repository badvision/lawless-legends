package jace.lawless;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.lawless.LawlessVideo.RenderEngine;
import javafx.beans.property.DoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

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
        readScores();
        currentScore = SCORE_CHIPTUNE;
    }

    @Override
    public void toggleCheats() {
        // Do nothing -- you cannot toggle this once it's active.
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat("Lawless Legends Graphics Modes", RAMEvent.TYPE.ANY, (e) -> {
            int addr = e.getAddress();
            if (addr >= MODE_SOFTSWITCH_MIN && e.getAddress() <= MODE_SOFTSWITCH_MAX) {
//                System.out.println("Trapped " + e.getType().toString() + " to $" + Integer.toHexString(e.getAddress()));
                setEngineByOrdinal(e.getAddress() - MODE_SOFTSWITCH_MIN);
            }
        }, MODE_SOFTSWITCH_MIN, MODE_SOFTSWITCH_MAX);
        addCheat("Lawless Legends Music Commands", RAMEvent.TYPE.WRITE, (e) -> {
//            System.out.println(Integer.toHexString(e.getAddress()) + " => " + Integer.toHexString(e.getNewValue() & 0x0ff));
            playSound(e.getNewValue());
        }, SFX_TRIGGER);
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

    public static final String SCORE_NONE = "none";
    public static final String SCORE_COMMON = "common";
    public static final String SCORE_ORCHESTRAL = "8-bit orchestral samples";
    public static final String SCORE_CHIPTUNE = "8-bit chipmusic";

    private static int currentSong;
    private static boolean repeatSong = false;
    private static Thread playbackEffect;
    private static MediaPlayer currentSongPlayer;
    private static MediaPlayer currentSfxPlayer;
    private static String currentScore = SCORE_COMMON;

    private void playSound(int soundNumber) {
        boolean isMusic = soundNumber >= 0;
        int track = soundNumber & 0x03f;
        repeatSong = (soundNumber & 0x040) > 0;
//        System.out.println("(invoked sound on "+getName()+")");
        if (track == 0) {
            if (isMusic) {
                System.out.println("Stop music");
                stopMusic();
            } else {
                System.out.println("Stop sfx");
                stopSfx();
            }
        } else if (isMusic) {
            playMusic(track, false);
        } else {
            System.out.println("Play sfx "+track);
            playSfx(track);
        }
    }
    
    private String getSongName(int number) {
        Map<Integer, String> score = scores.get(currentScore);
        if (score == null) {
            return null;
        }
        String filename = score.get(number);
        if (filename == null) {
            score = scores.get("common");
            if (score == null || !score.containsKey(number)) {
                return null;
            }
            filename = score.get(number);
        }
        return filename;
    }

    private Media getAudioTrack(int number) {
        String filename = getSongName(number);
        String pathStr = "/jace/data/sound/" + filename;
        URL path = getClass().getResource(pathStr);
        if (path == null) {
            return null;
        }
        String resourcePath = path.toString();
        System.out.println("Playing " + resourcePath);
        if (resourcePath.startsWith("resource:")) {
            resourcePath = Paths.get(resourcePath).toFile().getAbsolutePath();
            System.out.println("Playing " + resourcePath);
        }
        // Log path
        return new Media(resourcePath);
    }

    private void playMusic(int track, boolean switchScores) {
        if (currentSong != track || switchScores) {
            fadeOutSong(() -> startNewSong(track, switchScores));
        } else {
            // new Thread(() -> startNewSong(track, false)).start();
        }
        currentSong = track;
    }

    private boolean isPlayingMusic() {
        return currentSongPlayer != null && currentSongPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    private void stopSongEffect() {
        if (playbackEffect != null && playbackEffect.isAlive()) {
            playbackEffect.interrupt();
            Thread.onSpinWait();
        }
        playbackEffect = null;
    }
    
    private Optional<Double> getCurrentTime() {
        if (currentSongPlayer == null) {
            return Optional.empty();
        } else if (currentSongPlayer.getCurrentTime() == null) {
            return Optional.empty();
        } else {
            return Optional.of(currentSongPlayer.getCurrentTime().toMillis());
        }
    }

    private void fadeOutSong(Runnable nextAction) {
        stopSongEffect();
        MediaPlayer player = currentSongPlayer;
        if (player != null) {
            getCurrentTime().ifPresent(val -> lastTime.put(currentSong, val + 1500));
            playbackEffect = new Thread(() -> {
                DoubleProperty volume = player.volumeProperty();
                while (playbackEffect == Thread.currentThread() && volume.get() > 0.0) {
                    volume.set(volume.get() - FADE_AMT);
                    try {
                        Thread.sleep(FADE_SPEED);
                    } catch (InterruptedException e) {
                        playbackEffect = null;
                        return;
                    }
                }
                player.stop();
                if (currentSongPlayer == player) {
                    currentSongPlayer = null;
                }
                if (nextAction != null) {
                    nextAction.run();
                }
            });
            playbackEffect.start();
        } else if (nextAction != null) {
            new Thread(nextAction).start();
        }
    }

    private void fadeInSong(MediaPlayer player) {
        stopSongEffect();
        currentSongPlayer = player;
        DoubleProperty volume = player.volumeProperty();
        if (volume.get() >= 1.0) {
            return;
        }

        playbackEffect = new Thread(() -> {
            while (playbackEffect == Thread.currentThread() && volume.get() < 1.0) {
                volume.set(volume.get() + FADE_AMT);
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

    double FADE_AMT = 0.05; // 5% per interval, or 20 stops between 0% and 100%
//    int FADE_SPEED = 100; // 100ms per 5%, or 2 second duration
    int FADE_SPEED = 75; // 75ms per 5%, or 1.5 second duration
    int FIGHT_SONG = 17;
    boolean playingFightSong = false;
    private void startNewSong(int track, boolean switchScores) {
        if (!isMusicEnabled()) {
            return;
        }
        MediaPlayer player;
        if (track != currentSong || !isPlayingMusic() || switchScores) {
            System.out.println("Play music "+track);
            
            // If the same song is already playing don't restart it
            Media song = getAudioTrack(track);
            if (song == null) {
                System.out.println("Unable to start song " + track + "; File " + getSongName(track) + " not found");
                return;
            }
            player = new MediaPlayer(song);
            player.setCycleCount(repeatSong ? MediaPlayer.INDEFINITE : 1);
            player.setVolume(0.0);
            if (playingFightSong || autoResume.contains(track) || switchScores) {
                double time = lastTime.getOrDefault(track, 0.0);
                System.out.println("Auto-resume from time " + time);
                player.setStartTime(Duration.millis(time));
            }                
            player.play();
        } else {
            // But if the same song was already playing but possibly fading out
            // then this will fade it back in neatly.
            player = currentSongPlayer;
        }
        fadeInSong(player);
        playingFightSong = track == FIGHT_SONG;
    }

    private void stopMusic() {
        stopSongEffect();
        fadeOutSong(()->{
            if (!repeatSong) {
                currentSong = 0;
            }
        });
    }

    private void playSfx(int track) {
        new Thread(() -> {
            Media sfx = getAudioTrack(track + 128);
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

    public void changeMusicScore(String score) {
        if (currentScore.equalsIgnoreCase(score)) {
            return;
        }
        boolean wasStoppedPreviously = !isMusicEnabled();
        currentScore = score.toLowerCase(Locale.ROOT);
        if (currentScore.equalsIgnoreCase(SCORE_NONE)) {
            stopMusic();
            currentSong = -1;
        } else if ((currentSongPlayer != null || wasStoppedPreviously) && currentSong > 0) {
            playMusic(currentSong, true);
        }
    }

    public boolean isMusicEnabled() {
        return currentScore != null && !currentScore.equalsIgnoreCase(SCORE_NONE);
    }

    Pattern COMMENT = Pattern.compile("\\s*[-#;']+.*");
    Pattern LABEL = Pattern.compile("(8-)?[A-Za-z\\s\\-_]+");
    Pattern ENTRY = Pattern.compile("([0-9]+)\\s+(.*)");
    private final Map<String, Map<Integer, String>> scores = new HashMap<>();
    private final Set<Integer> autoResume = new HashSet<>();
    private final Map<Integer, Double> lastTime = new HashMap<>();
    private void readScores() {
        InputStream data = getClass().getResourceAsStream("/jace/data/sound/scores.txt");
        readScores(data);
    }

    private void readScores(InputStream data) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(data));
        reader.lines().forEach(line -> {
            boolean useAutoResume = false;
            if (line.indexOf('*') > 0) {
                useAutoResume = true;
                line = line.replace("*", "");
            }
            if (COMMENT.matcher(line).matches() || line.trim().isEmpty()) {
//                System.out.println("Ignoring: "+line);
            } else if (LABEL.matcher(line).matches()) {
                currentScore = line.toLowerCase(Locale.ROOT);
                scores.put(currentScore, new HashMap<>());
//                System.out.println("Score: "+ currentScore);
            } else {
                Matcher m = ENTRY.matcher(line);
                if (m.matches()) {
                    int num = Integer.parseInt(m.group(1));
                    String file = m.group(2);
                    scores.get(currentScore).put(num, file);
                    if (useAutoResume) {
                        autoResume.add(num);
                    }
//                    System.out.println("Score: " + currentScore + "; Song: " + num + "; " + file);
                } else {
//                    System.out.println("Couldn't parse: " + line);
                }
            }
        });

    }
}
