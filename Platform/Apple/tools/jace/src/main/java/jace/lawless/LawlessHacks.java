package jace.lawless;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jace.Emulator;
import jace.apple2e.MOS65C02;
import jace.apple2e.SoftSwitches;
import jace.apple2e.VideoDHGR;
import jace.cheat.Cheats;
import jace.core.Computer;
import jace.core.Keyboard;
import jace.core.Motherboard;
import jace.core.RAMEvent;
import jace.core.TimedDevice;
import javafx.util.Duration;

/**
 * Hacks that affect lawless legends gameplay
 */
public class LawlessHacks extends Cheats {
    boolean DEBUG = false;
    // Modes specified by the game engine
    int MODE_SOFTSWITCH_MIN = 0x0C049;
    int MODE_SOFTSWITCH_MAX = 0x0C04F;
    int SFX_TRIGGER = 0x0C069;
    double PORTRAIT_SPEED = 1.0;

    public LawlessHacks() {
        super();
        readScores();
        currentScore = SCORE_ORCHESTRAL;
    }

    @Override
    public void toggleCheats() {
        // Do nothing -- you cannot toggle this once it's active.
    }

    @Override
    public void registerListeners() {
        // Observe graphics changes
        addCheat("Lawless Text Speedup", RAMEvent.TYPE.EXECUTE, this::fastText, 0x0ee00, 0x0ee00 + 0x0f00);
        addCheat("Lawless Text Enhancement", RAMEvent.TYPE.WRITE, this::enhanceText, 0x02000, 0x03fff);
        addCheat("Lawless Legends Music Commands", RAMEvent.TYPE.WRITE, (e) -> playSound(e.getNewValue()), SFX_TRIGGER);
        addCheat("Lawless Adjust Animation Speed", RAMEvent.TYPE.READ, this::adjustAnimationSpeed, 0x0c000, 0x0c010);
    }

    @Override
    public String getDeviceName() {
        return "Lawless Legends optimizations";
    }

    boolean isSlowedDown = false;
    @Override
    public void tick() {
        if (isSlowedDown && (Keyboard.readState() & 0x080) > 0) {
            endSlowdown();
        }
    }

    @Override
    public void detach() {
        super.detach();
        stopMusic();
        Emulator.withComputer((c->c.getMotherboard().cancelSpeedRequest(this)));
    }

    // Speed up text rendering
    private void fastText(RAMEvent e) {
        if (e.isMainMemory() && e.getOldValue() != 0x060) {
            Emulator.withComputer((c->c.getMotherboard().requestSpeed(this)));
        } else {
            Emulator.withComputer((c->c.getMotherboard().cancelSpeedRequest(this)));
        }
    }

    // Enhance text rendering by forcing the text to be pure B&W
    Map<Integer, Integer> detectedEntryPoints = new TreeMap<>();
    long lastStatus = 0;
    private void enhanceText(RAMEvent e) {
        if (!e.isMainMemory() || SoftSwitches.RAMWRT.isOn() || (SoftSwitches.PAGE2.isOn() && SoftSwitches._80STORE.isOn())) {
            return;
        }
        int pc = Emulator.withComputer(c->c.getCpu().getProgramCounter(), 0);
        boolean drawingText = (pc >= 0x0ee00 && pc <= 0x0f0c0 && pc != 0x0f005) || pc > 0x0f100;
        if (DEBUG) {
            if (drawingText) {
                detectedEntryPoints.put(pc, detectedEntryPoints.getOrDefault(pc, 0) + 1);
            }
            if ((System.currentTimeMillis() - lastStatus) >= 10000) {
                lastStatus = System.currentTimeMillis();
                System.out.println("---text entry points---");
                detectedEntryPoints.forEach((addr, count) -> {
                    System.out.println(Integer.toHexString(addr) + ": " + count);
                });
            }
        }

        int addr = e.getAddress();
        int y = VideoDHGR.identifyHiresRow(addr);
        if (y >= 0 && y <= 192) {
            int x = addr - VideoDHGR.hiresOffset[y] - 0x02000;
            if (x >= 0 && x < 40) {
                Emulator.withVideo(v -> {
                    if (v instanceof LawlessVideo) {
                        LawlessVideo video = (LawlessVideo) v;
                        video.activeMask[y][x*2] = !drawingText;
                        video.activeMask[y][x*2+1] = !drawingText;
                    }
                });
            }
        }
    }

    private Map<Integer, Integer> keyReadAddresses = new TreeMap<>();
    long lastKeyStatus = 0;
    long lastKnownSpeed = -1;
    boolean isCurrentlyMaxSpeed = false;
    long keyEventTraceDuration = 0;
    private void adjustAnimationSpeed(RAMEvent e) {
        int pc = Emulator.withComputer(c->c.getCpu().getProgramCounter(), 0);
        int eventAddress = e.getAddress();

        if (DEBUG) {
            keyReadAddresses.put(pc, keyReadAddresses.getOrDefault(pc, 0) + 1);
            if ((System.currentTimeMillis() - lastKeyStatus) >= 10000) {
                lastKeyStatus = System.currentTimeMillis();
                System.out.println("---keyin points---");
                keyReadAddresses.forEach((addr, count) -> {
                    System.out.println(Integer.toHexString(addr) + ": " + count);
                });
            }
        }

        if (eventAddress == 0x0c000 && pc == 0x0D5FE) {
            // We are waiting for a key in portait mode
            // Check where we were called from in the stack
            MOS65C02 cpu = (MOS65C02) Emulator.withComputer(c->c.getCpu(), null);
            int stackAddr1 = 0x0100 + cpu.STACK;
            int lastStackByte = Emulator.withMemory(ram-> ram.readRaw(stackAddr1), (byte) 0) & 0x0ff;
            if (lastStackByte == 0x09b) {
                // Turns out the last value on the stack is consistently
                // the same value whenever we also want to be running in a
                // slower speed for animation, but not in other key read
                // routines where we're needing more speed.  Convenient!
                beginSlowdown();
            }
        }
    }

    public void beginSlowdown() {
        Motherboard m = Emulator.withComputer(Computer::getMotherboard, null);
        long portraitSpeed = (long) (TimedDevice.NTSC_1MHZ * PORTRAIT_SPEED);
        long currentSpeed = m.getSpeedInHz();

        if (!isSlowedDown && (currentSpeed != portraitSpeed || m.isMaxSpeedEnabled())) {
            isSlowedDown = true;
            lastKnownSpeed = currentSpeed;
            isCurrentlyMaxSpeed = m.isMaxSpeedEnabled();
            m.cancelSpeedRequest(this);
            m.setSpeedInHz(portraitSpeed);
            m.setMaxSpeed(false);
        }
    }

    public void endSlowdown() {
        Motherboard m = Emulator.withComputer(Computer::getMotherboard, null);
        if (isSlowedDown) {
            isSlowedDown = false;
            m.setSpeedInHz(lastKnownSpeed);
            m.setMaxSpeed(isCurrentlyMaxSpeed);
            isCurrentlyMaxSpeed = false;
            lastKnownSpeed = -1;
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
    private static MediaPlayer previousSongPlayer;
    private static MediaPlayer currentSfxPlayer;
    private static String currentScore = SCORE_COMMON;
    
    // Volume control for music
    private static double musicVolume = 0.5;
    
    /**
     * Set the music volume
     * @param volume Volume between 0.0 and 1.0
     */
    public void setMusicVolume(double volume) {
        musicVolume = Math.max(0.0, Math.min(1.0, volume));
        if (currentSongPlayer != null) {
            currentSongPlayer.setVolume(musicVolume);
        }
    }
    
    public  void playSound(int soundNumber) {
        boolean isMusic = soundNumber >= 0;
        int track = soundNumber & 0x03f;
        repeatSong = (soundNumber & 0x040) > 0;
        if (DEBUG) {
            System.out.println("Play sound " + soundNumber + " (track " + track + "; repeat " + repeatSong + ") invoked on " + getName());
        }
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
    
    public String getSongName(int number) {
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

    public Media getAudioTrack(int number) {
        String filename = getSongName(number);
        String pathStr = "/jace/data/sound/" + filename;
        // URL path = getClass().getResource(pathStr);
        // if (path == null) {
        //     return null;
        // }
        // String resourcePath = path.toString();
        // System.out.println("Playing " + resourcePath);
        // if (resourcePath.startsWith("resource:")) {
        //     resourcePath = Paths.get(resourcePath).toFile().getAbsolutePath();
        //     System.out.println("Playing " + resourcePath);
        // }
        // Log path
        try {
            return new Media(pathStr);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to load audio track " + pathStr, e);
            return null;
        }
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
    
    private Optional<Long> getCurrentTime() {
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
            Thread effect = new Thread(() -> {
                while (playbackEffect == Thread.currentThread() && player.getVolume() > 0.0) {
                    player.setVolume(Math.max(player.getVolume() - FADE_AMT, 0.0));
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
            playbackEffect = effect;
            effect.start();
        } else if (nextAction != null) {
            new Thread(nextAction).start();
        }
    }

    private void fadeInSong(MediaPlayer player) {
        stopSongEffect();
        if (previousSongPlayer != null) {
            previousSongPlayer.stop();
        }
        previousSongPlayer = currentSongPlayer;
        currentSongPlayer = player;
        if (player.getVolume() >= 1.0) {
            return;
        }

        Thread effect = new Thread(() -> {
            while (playbackEffect == Thread.currentThread() && player.getVolume() < musicVolume) {
                player.setVolume(Math.min(player.getVolume() + FADE_AMT, musicVolume));
                try {
                    Thread.sleep(FADE_SPEED);
                } catch (InterruptedException e) {
                    playbackEffect = null;
                    return;
                }
            }
        });
        playbackEffect = effect;
        effect.start();
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
        // If the same song is already playing don't restart it
        if (track != currentSong || !isPlayingMusic() || switchScores) {
            if (DEBUG) {
                System.out.println("Start new song " + track + " (switch " + switchScores + ")");
            }

            Media song = getAudioTrack(track);
            if (song == null) {
                System.out.println("Unable to start song " + track + "; File " + getSongName(track) + " not found");
                return;
            }
            player = new MediaPlayer(song);
            player.setCycleCount(repeatSong ? MediaPlayer.INDEFINITE : 1);
            player.setVolume(0.0);
            if (playingFightSong || autoResume.contains(track) || switchScores) {
                long time = lastTime.getOrDefault(track, 0L);
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

    /**
     * Get the current music score selection
     * @return The current score name
     */
    public String getCurrentScore() {
        return currentScore;
    }

    Pattern COMMENT = Pattern.compile("\\s*[-#;']+.*");
    Pattern LABEL = Pattern.compile("(8-)?[A-Za-z\\s\\-_]+");
    Pattern ENTRY = Pattern.compile("([0-9]+)\\s+(.*)");
    public final Map<String, Map<Integer, String>> scores = new HashMap<>();
    private final Set<Integer> autoResume = new HashSet<>();
    private final Map<Integer, Long> lastTime = new HashMap<>();
    private void readScores() {
        InputStream data = getClass().getResourceAsStream("/jace/data/sound/scores.txt");
        readScores(data);
    }

    public void readScores(InputStream data) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(data));
        reader.lines().forEach(line -> {
            boolean useAutoResume = false;
            if (line.indexOf('*') > 0) {
                useAutoResume = true;
                line = line.replace("*", "");
            }
            if (COMMENT.matcher(line).matches() || line.trim().isEmpty()) {
                if (DEBUG)
                   System.out.println("Ignoring: "+line);
            } else if (LABEL.matcher(line).matches()) {
                currentScore = line.toLowerCase(Locale.ROOT);
                scores.put(currentScore, new HashMap<>());
                if (DEBUG)
                   System.out.println("Score: "+ currentScore);
            } else {
                Matcher m = ENTRY.matcher(line);
                if (m.matches()) {
                    int num = Integer.parseInt(m.group(1));
                    String file = m.group(2);
                    scores.get(currentScore).put(num, file);
                    if (useAutoResume) {
                        autoResume.add(num);
                    }
                    if (DEBUG)
                       System.out.println("Score: " + currentScore + "; Song: " + num + "; " + file);
                } else {
                    if (DEBUG)
                       System.out.println("Couldn't parse: " + line);
                }
            }
        });

    }
}
