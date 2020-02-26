/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jace;

import jace.config.Configuration;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.CardDiskII;
import jace.hardware.CardMockingboard;
import jace.hardware.CardRamFactor;
import jace.hardware.CardRamworks;
import jace.hardware.PassportMidiInterface;
import jace.hardware.massStorage.CardMassStorage;
import jace.lawless.LawlessHacks;
import jace.lawless.LawlessImageTool;
import jace.lawless.LawlessVideo;
import jace.ui.MetacheatUI;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    static boolean romStarted = false;

    @Override
    public void start(Stage stage) throws Exception {
        singleton = this;
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/JaceUI.fxml"));
        fxmlLoader.setResources(null);
        try {
            AnchorPane node = (AnchorPane) fxmlLoader.load();
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
        new Thread(() -> {
            new Emulator(getParameters().getRaw());
            configureEmulatorForGame();
            reconnectUIHooks();
            EmulatorUILogic.scaleIntegerRatio();
            while (Emulator.computer.getVideo() == null || Emulator.computer.getVideo().getFrameBuffer() == null) {
                Thread.yield();
            }
            bootWatchdog();
        }).start();
        primaryStage.setOnCloseRequest(event -> {
            Emulator.computer.deactivate();
            Platform.exit();
            System.exit(0);
        });
    }

    public void reconnectUIHooks() {
        controller.connectComputer(Emulator.computer, primaryStage);
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
        RAMListener startListener = Emulator.computer.getMemory().
                observe(RAMEvent.TYPE.EXECUTE, 0x0c700, (e) -> {
                    romStarted = true;
                });
        Emulator.computer.invokeColdStart();
        try {
            Thread.sleep(7500);
            if (!romStarted) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot not detected, performing a cold start");
                Emulator.computer.invokeColdStart();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
        }
        Emulator.computer.getMemory().removeListener(startListener);
    }

    private void configureEmulatorForGame() {
        Emulator.computer.enableHints = false;
        Emulator.computer.clockEnabled = true;
        Emulator.computer.joy1enabled = false;
        Emulator.computer.joy2enabled = false;
        Emulator.computer.enableStateManager = false;
        Emulator.computer.ramCard.setValue(CardRamworks.class);
        Emulator.computer.videoRenderer.setValue(LawlessVideo.class);
        Emulator.computer.card7.setValue(CardMassStorage.class);
        Emulator.computer.card6.setValue(CardDiskII.class);
        Emulator.computer.card5.setValue(CardRamFactor.class);
        Emulator.computer.card4.setValue(CardMockingboard.class);
        Emulator.computer.card2.setValue(PassportMidiInterface.class);
        Emulator.computer.cheatEngine.setValue(LawlessHacks.class);
        Configuration.buildTree();
        Emulator.computer.reconfigure();
        ((LawlessImageTool) Emulator.computer.getUpgradeHandler()).loadGame();
    }
}
