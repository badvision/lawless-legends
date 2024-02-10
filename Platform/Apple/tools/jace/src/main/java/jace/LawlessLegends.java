package jace;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.apple2e.RAM128k;
import jace.apple2e.VideoNTSC;
import jace.cheat.Cheats.Cheat;
import jace.config.Configuration;
import jace.core.Computer;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.Cards;
import jace.hardware.VideoImpls;
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
        Platform.runLater(() -> new Thread(() -> {
            Emulator.getInstance(getParameters().getRaw());
            Emulator.withComputer(c->((LawlessComputer)c).initLawlessLegendsConfiguration());
            configureEmulatorForGame();
            reconnectUIHooks();
            EmulatorUILogic.scaleIntegerRatio();
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
        }).start());
        primaryStage.setOnCloseRequest(event -> {
            Emulator.withComputer(Computer::deactivate);
            Platform.exit();
            System.exit(0);
        });
    }

    public void reconnectUIHooks() {
        controller.connectComputer(primaryStage);
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
            int watchAddress = c.PRODUCTION_MODE ? 0x02000 : 0x0ff3a;
            int watchdogDelay = c.PRODUCTION_MODE ? 6500 : 500;
            new Thread(()->{
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Booting with watchdog");
                final RAMListener startListener = c.getMemory().observeOnce("Lawless Legends watchdog", RAMEvent.TYPE.EXECUTE, watchAddress, (e) -> {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot was detected, watchdog terminated.");
                    romStarted.set(true);
                });
                romStarted.set(false);
                c.invokeColdStart();
                try {
                    Thread.sleep(watchdogDelay);
                    if (!romStarted.get() || !c.isRunning() || c.getCpu().getProgramCounter() == 0xc700 || c.getCpu().getProgramCounter() == 0) {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot not detected, performing a cold start");
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Old PC: {0}", c.getCpu().getProgramCounter());
                        resetEmulator();
                        configureEmulatorForGame();
                        bootWatchdog();
                        // Emulator.getComputer().getCpu().trace=true;
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
        // Reset the emulator memory and reconfigure
        Emulator.withComputer(c -> {
            c.pause();
            c.getMemory().resetState();
            c.reconfigure();
            c.resume();
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
            c.videoRenderer.setValue(VideoImpls.Lawless);
            if (c.PRODUCTION_MODE) {
                c.card7.setValue(Cards.MassStorage);
                c.card6.setValue(Cards.DiskIIDrive);
                c.card5.setValue(Cards.RamFactor);
                c.card4.setValue(null);
                c.card2.setValue(null);
                c.getMemory().writeWord(0x03f0, 0x0c700, false, false);
            }
            c.cheatEngine.setValue(Cheat.LawlessHacks);
            Configuration.buildTree();
            c.reconfigure();
            VideoNTSC.setVideoMode(VideoNTSC.VideoMode.TextFriendly, false);
            if (c.PRODUCTION_MODE) {
                ((LawlessImageTool) c.getUpgradeHandler()).loadGame();
            }
        });
    }
}
