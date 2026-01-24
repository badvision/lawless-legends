package jace;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.apple2e.MOS65C02;
import jace.apple2e.RAM128k;
import jace.apple2e.SoftSwitches;
import jace.apple2e.VideoNTSC;
import jace.config.Configuration;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.Cards;
import jace.lawless.LawlessComputer;
import jace.lawless.LawlessImageTool;
import jace.ui.MetacheatUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *  
 * @author blurry
 */
public class LawlessLegends extends Application {

    static LawlessLegends singleton;

    public Stage primaryStage;
    public JaceUIController controller;

    static AtomicBoolean romStarted = new AtomicBoolean(false);
    private final AtomicBoolean watchdogRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService watchdogScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger retryDelayMs = new AtomicInteger(500);
    private static final int MAX_RETRY_DELAY = 10000;  // 10 seconds cap
    private static final int MAX_RETRIES = 5;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    int watchdogDelay = 500;

    @Override
    public void start(Stage stage) throws Exception {
        singleton = this;
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/JaceUI.fxml"));
        fxmlLoader.setResources(null);
        try {
            AnchorPane node = fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.initialize();
            Scene s = new Scene(node);
            s.setFill(Color.BLACK);

            primaryStage.setScene(s);
            primaryStage.titleProperty().set("Lawless Legends");
            Utility.loadIcon("game_icon.png").ifPresent(icon -> {
                primaryStage.getIcons().add(icon);
            });
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // Set up Shift+J detection before showing the stage
        // This allows users to press Shift+J before the window fully initializes
        AtomicBoolean bypassChecked = new AtomicBoolean(false);
        primaryStage.getScene().setOnKeyPressed(event -> {
            if (!bypassChecked.get() && event.isShiftDown() && event.getCode() == KeyCode.J) {
                bypassChecked.set(true);
                if (System.getProperty("jace.developerBypass") == null) {
                    System.setProperty("jace.developerBypass", "true");
                    Logger.getLogger(LawlessLegends.class.getName()).log(Level.INFO,
                        "Developer bypass mode enabled via Shift+J");
                }
            }
        });

        primaryStage.show();

        // Give user a brief moment to press Shift+J before creating the Emulator
        new Thread(() -> {
            try {
                Thread.sleep(100); // 100ms window to press Shift+J
            } catch (InterruptedException e) {
                // Continue anyway
            }
            Emulator.getInstance(getParameters().getRaw());
            Emulator.withComputer(c-> {
                ((LawlessComputer)c).initLawlessLegendsConfiguration();
                if (c.PRODUCTION_MODE) {
                    watchdogDelay = 7000;
                }
            });
            // Initialize base configuration structure without reading current field values
            // Configuration load message removed for cleaner logs
            
            configureEmulatorForGame();
            reconnectUIHooks();
            // Delay UI settings loading to ensure configuration system is fully ready
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    if (controller != null) {
                        controller.loadUISettings();
                    }
                    // Restore UI settings AFTER configureEmulatorForGame to avoid being overridden
                    restoreUISettings();
                });
            });
            // Don't call scaleIntegerRatio() here - it will override our restored window size
            AtomicBoolean waitingForVideo = new AtomicBoolean(true);
            while (waitingForVideo.get()) {
                Emulator.withVideo(v -> {
                    if (v.getFrameBuffer() != null) {
                        waitingForVideo.set(false);
                    }
                });
                Thread.onSpinWait();
            }

            // Wait for upgrade to complete before starting boot watchdog
            // This prevents RC-1: Boot watchdog starting before upgrade completes
            Emulator.withComputer(c-> {
                LawlessComputer computer = (LawlessComputer) c;
                if (computer.getAutoUpgradeHandler() != null) {
                    computer.getAutoUpgradeHandler().awaitUpgradeCompletion(10000);
                }
            });

            bootWatchdog();
        }).start();
        primaryStage.setOnCloseRequest(event -> {
            // Save UI settings before closing
            if (controller != null) {
                controller.saveUISettings();
                controller.shutdown();
            }
            // Shutdown watchdog scheduler
            watchdogScheduler.shutdown();
            try {
                if (!watchdogScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    watchdogScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                watchdogScheduler.shutdownNow();
            }
            Emulator.withComputer(Computer::deactivate);
            Platform.exit();
            System.exit(0);
        });

        // Ensure UI settings are persisted even if the window is closed via SIGTERM or pkill
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (controller != null) {
                controller.saveUISettings();
                controller.shutdown();
            }
            // Shutdown watchdog scheduler
            watchdogScheduler.shutdown();
            try {
                if (!watchdogScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    watchdogScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                watchdogScheduler.shutdownNow();
            }
        }));
    }

    public void reconnectUIHooks() {
        controller.connectComputer(primaryStage);
    }
    
    private void restoreUISettings() {
        Platform.runLater(() -> {
            if (controller != null && controller.getUIConfiguration() != null) {
                controller.beginProgrammaticUpdate();
                EmulatorUILogic uiConfig = controller.getUIConfiguration();
                
                // Debug output removed
                
                // Restore video mode
                Emulator.withVideo(v -> {
                    if (v instanceof VideoNTSC) {
                        VideoNTSC.setVideoMode(uiConfig.getVideoModeEnum(), false);
                        // Debug output removed
                    }
                });
                
                // Restore window size index for the resize function
                // Note: scaleIntegerRatio() increments size first, so we set it to one less
                EmulatorUILogic.size = uiConfig.windowSizeIndex - 1;
                // Debug output removed
                
                // Apply the window size by calling scaleIntegerRatio
                EmulatorUILogic.scaleIntegerRatio();
                Platform.runLater(() -> {
                    controller.endProgrammaticUpdate();
                    JaceUIController.startupComplete = true;
                });
            } else {
                // Apply some defaults
                EmulatorUILogic.size = 1;
                EmulatorUILogic.scaleIntegerRatio();
                VideoNTSC.setVideoMode(VideoNTSC.VideoMode.Color, false);
            }
        });
    }

    public static LawlessLegends getApplication() {
        return singleton;
    }

    Stage cheatStage;
    private MetacheatUI cheatController;

    public MetacheatUI showMetacheat() {
        if (cheatController == null) {
            cheatStage = new Stage(StageStyle.DECORATED);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Metacheat.fxml"));
            fxmlLoader.setResources(null);
            try {
                VBox node = fxmlLoader.load();
                cheatController = fxmlLoader.getController();
                Scene s = new Scene(node);
                cheatStage.setScene(s);
                cheatStage.setTitle("Jace: MetaCheat");
                Utility.loadIcon("woz_figure.gif").ifPresent(cheatStage.getIcons()::add);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

        }
        cheatStage.show();
        return cheatController;
    }

    public void closeMetacheat() {
        if (cheatStage != null) {
            cheatStage.close();
        }
        if (cheatController != null) {
            cheatController.detach();
            cheatController = null;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Install custom exception handler to suppress noisy MacAccessible errors
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new MacAccessibleExceptionHandler(defaultHandler));

        // Check for developer bypass mode via Shift+J at startup
        checkForDeveloperBypass();
        launch(args);
    }

    private static void checkForDeveloperBypass() {
        // Developer bypass mode can be enabled via:
        // 1. Holding Shift+J during startup (detected in start() method)
        // 2. Command line: -Djace.developerBypass=true
        // Note: Keyboard state cannot be detected before JavaFX starts,
        // so actual Shift+J detection happens in the start() method.
        if (System.getProperty("jace.developerBypass") == null) {
            Logger.getLogger(LawlessLegends.class.getName()).log(Level.INFO,
                "To enable developer bypass mode, hold Shift+J during startup or use -Djace.developerBypass=true");
        }
    }

    /**
     * Detects if the system has dropped to the BASIC prompt, indicating a boot failure.
     * Looks for the "]" character in text mode page 1 (first 2 lines).
     *
     * @return true if at BASIC prompt, false otherwise
     */
    private boolean isAtBasicPrompt() {
        return Emulator.withComputer(c -> {
            // Check text mode and page 1 active
            boolean textMode = SoftSwitches.TEXT.isOn() && SoftSwitches.HIRES.isOff();
            boolean page1 = SoftSwitches.PAGE2.isOff();

            if (!textMode || !page1) return false;

            // Look for "]" BASIC prompt character in first few lines
            for (int addr = 0x0400; addr < 0x0480; addr++) {  // First 2 lines
                byte b = c.getMemory().readRaw(addr);
                if ((b & 0x7F) == ']') return true;
            }
            return false;
        }, false);
    }

    /**
     * Start the computer and make sure it runs through the expected rom routine
     * for cold boot.
     *
     * Public to allow access from upgrade handlers and other subsystems.
     */
    public void bootWatchdog() {
        // Atomic guard to prevent concurrent watchdog instances - debounce with exponential backoff
        if (!watchdogRunning.compareAndSet(false, true)) {
            // Already running - schedule retry with backoff
            int currentRetries = retryCount.incrementAndGet();
            if (currentRetries > MAX_RETRIES) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                    "Boot watchdog max retries exceeded, giving up");
                retryCount.set(0);
                return;
            }

            int delay = retryDelayMs.get();
            Logger.getLogger(getClass().getName()).log(Level.INFO,
                "Boot watchdog busy, scheduling retry in " + delay + "ms (attempt " + currentRetries + ")");
            watchdogScheduler.schedule(this::bootWatchdog, delay, TimeUnit.MILLISECONDS);
            retryDelayMs.set(Math.min(delay * 2, MAX_RETRY_DELAY));  // Exponential backoff
            return;
        }

        // Successfully acquired watchdog lock
        retryCount.set(0);  // Reset retry counter
        retryDelayMs.set(2000);  // Reset delay

        Emulator.withComputer(c -> {
            // We know the game started properly when it runs the decompressor the first time
            int watchAddress = c.PRODUCTION_MODE ? 0x0DF00 : 0x0ff3a;
            new Thread(()->{
                try {
                    // Logger.getLogger(getClass().getName()).log(Level.WARNING, "Booting with watchdog");
                    final RAMListener startListener = c.getMemory().observeOnce("Lawless Legends watchdog", RAMEvent.TYPE.EXECUTE, watchAddress, (e) -> {
                        // Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot was detected, watchdog terminated.");
                        romStarted.set(true);
                    });
                    romStarted.set(false);
                    c.coldStart();
                    try {
                        // Logger.getLogger(getClass().getName()).log(Level.WARNING, "Watchdog: waiting " + watchdogDelay + "ms for boot to start.");
                        Thread.sleep(watchdogDelay);
                        watchdogDelay = 500;
                        boolean invalidPC = c.getCpu().getProgramCounter() == MOS65C02.FASTBOOT ||
                                            c.getCpu().getProgramCounter() == 0;
                        boolean atBasic = isAtBasicPrompt();

                        if (!romStarted.get() || !c.isRunning() || invalidPC || atBasic) {
                            if (atBasic) {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING,
                                    "Boot failed: System dropped to BASIC prompt, retrying...");
                            } else {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING,
                                    "Boot not detected, performing a cold start");
                                Logger.getLogger(getClass().getName()).log(Level.WARNING,
                                    "Old PC: {0}", Integer.toHexString(c.getCpu().getProgramCounter()));
                            }
                            resetEmulator();
                            configureEmulatorForGame();
                            bootWatchdog();
                        } else {
                            startListener.unregister();
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } finally {
                    // Clear guard to allow future watchdog instances
                    watchdogRunning.set(false);
                }
            }).start();
        });
    }

    public void resetEmulator() {
        // Reset the emulator memory and restart
        Emulator.withComputer(c -> {
            c.getMemory().resetState();
            c.warmStart();
        });
    }

    private void configureEmulatorForGame() {
        Emulator.withComputer(c -> {
            LawlessComputer computer = (LawlessComputer) c;
            c.enableHints = false;
            c.clockEnabled = true;
            c.joy1enabled = false;
            c.joy2enabled = false;
            c.enableStateManager = false;
            c.ramCard.setValue(RAM128k.RamCards.CardRamworks);
            // Only configure production hardware and load game if NOT in developer bypass mode
            if (c.PRODUCTION_MODE && !computer.isDeveloperBypassMode()) {
                c.card7.setValue(Cards.MassStorage);
                c.card6.setValue(Cards.DiskIIDrive);
                c.card5.setValue(Cards.RamFactor);
                c.card4.setValue(null);
                c.card2.setValue(null);
            }
            c.reconfigure();
            restoreUISettings();
            if (c.PRODUCTION_MODE && !computer.isDeveloperBypassMode()) {
                ((LawlessImageTool) c.getUpgradeHandler()).loadGame(c.getAutoUpgradeHandler());
            }
        });
    }
}
