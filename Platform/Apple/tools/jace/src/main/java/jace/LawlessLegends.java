package jace;

import jace.apple2e.SoftSwitches;
import jace.apple2e.VideoNTSC;
import jace.config.Configuration;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.CardDiskII;
import jace.hardware.CardRamFactor;
import jace.hardware.CardRamworks;
import jace.hardware.massStorage.CardMassStorage;
import jace.lawless.LawlessHacks;
import jace.lawless.LawlessImageTool;
import jace.lawless.LawlessVideo;
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  
 * @author blurry
 */
public class LawlessLegends extends Application {

    static LawlessLegends singleton;

    public Stage primaryStage;
    public JaceUIController controller;

    static boolean romStarted = false;

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
            primaryStage.setTitle("Lawless Legends");
            Utility.loadIcon("game_icon.png").ifPresent(primaryStage.getIcons()::add);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.show();
        Platform.runLater(() -> new Thread(() -> {
            Emulator.getInstance(getParameters().getRaw());
            configureEmulatorForGame();
            reconnectUIHooks();
            EmulatorUILogic.scaleIntegerRatio();
            while (Emulator.getComputer().getVideo() == null || Emulator.getComputer().getVideo().getFrameBuffer() == null) {
                Thread.yield();
            }
            bootWatchdog();
        }).start());
        primaryStage.setOnCloseRequest(event -> {
            Emulator.getComputer().deactivate();
            Platform.exit();
            System.exit(0);
        });
    }

    public void reconnectUIHooks() {
        controller.connectComputer(Emulator.getComputer(), primaryStage);
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
        romStarted = false;
        if (Emulator.getComputer().PRODUCTION_MODE) {
            RAMListener startListener = Emulator.getComputer().getMemory().
                    observe(RAMEvent.TYPE.EXECUTE, 0x0c700, (e) -> romStarted = true);
            Emulator.getComputer().invokeColdStart();
            try {
                Thread.sleep(7500);
                if (!romStarted) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot not detected, performing a cold start");
                    Emulator.getComputer().invokeColdStart();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
            Emulator.getComputer().getMemory().removeListener(startListener);
        } else {
            romStarted = true;
            Emulator.getComputer().invokeColdStart();
        }
    }

    private void configureEmulatorForGame() {
        Emulator.getComputer().enableHints = false;
        Emulator.getComputer().clockEnabled = true;
        Emulator.getComputer().joy1enabled = false;
        Emulator.getComputer().joy2enabled = false;
        Emulator.getComputer().enableStateManager = false;
        Emulator.getComputer().ramCard.setValue(CardRamworks.class);
        Emulator.getComputer().videoRenderer.setValue(LawlessVideo.class);
        if (Emulator.getComputer().PRODUCTION_MODE) {
            Emulator.getComputer().card7.setValue(CardMassStorage.class);
            Emulator.getComputer().card6.setValue(CardDiskII.class);
            Emulator.getComputer().card5.setValue(CardRamFactor.class);
            Emulator.getComputer().card4.setValue(null);
            Emulator.getComputer().card2.setValue(null);
        }
        Emulator.getComputer().cheatEngine.setValue(LawlessHacks.class);
        Configuration.buildTree();
        Emulator.getComputer().reconfigure();
        VideoNTSC.setVideoMode(VideoNTSC.VideoMode.TextFriendly, false);
        if (Emulator.getComputer().PRODUCTION_MODE) {
            ((LawlessImageTool) Emulator.getComputer().getUpgradeHandler()).loadGame();
        } else {
            for (SoftSwitches s : SoftSwitches.values()) {
                s.getSwitch().reset();
            }
        }
    }
}
