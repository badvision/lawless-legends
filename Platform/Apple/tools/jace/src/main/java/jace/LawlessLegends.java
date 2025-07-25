package jace;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.apple2e.MOS65C02;
import jace.apple2e.RAM128k;
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

        primaryStage.show();
        new Thread(() -> {
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
            bootWatchdog();
        }).start();
        primaryStage.setOnCloseRequest(event -> {
            // Save UI settings before closing
            if (controller != null) {
                controller.saveUISettings();
                controller.shutdown();
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
        launch(args);
    }

    /**
     * Start the computer and make sure it runs through the expected rom routine
     * for cold boot
     */
    private void bootWatchdog() {
        Emulator.withComputer(c -> {
            // We know the game started properly when it runs the decompressor the first time
            int watchAddress = c.PRODUCTION_MODE ? 0x0DF00 : 0x0ff3a;
            new Thread(()->{
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
                    if (!romStarted.get() || !c.isRunning() || c.getCpu().getProgramCounter() == MOS65C02.FASTBOOT || c.getCpu().getProgramCounter() == 0) {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot not detected, performing a cold start");
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Old PC: {0}", Integer.toHexString(c.getCpu().getProgramCounter()));
                        resetEmulator();
                        configureEmulatorForGame();
                        bootWatchdog();
                    } else {
                        startListener.unregister();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
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
            c.enableHints = false;
            c.clockEnabled = true;
            c.joy1enabled = false;
            c.joy2enabled = false;
            c.enableStateManager = false;
            c.ramCard.setValue(RAM128k.RamCards.CardRamworks);
            if (c.PRODUCTION_MODE) {
                c.card7.setValue(Cards.MassStorage);
                c.card6.setValue(Cards.DiskIIDrive);
                c.card5.setValue(Cards.RamFactor);
                c.card4.setValue(null);
                c.card2.setValue(null);
            }
            c.reconfigure();
            restoreUISettings();            
            if (c.PRODUCTION_MODE) {
                ((LawlessImageTool) c.getUpgradeHandler()).loadGame();
            }
        });
    }
}
