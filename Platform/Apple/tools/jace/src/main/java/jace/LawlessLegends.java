/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jace;

import jace.core.RAM;
import jace.core.RAMEvent;
import jace.core.RAMListener;
import jace.core.Utility;
import jace.hardware.CardDiskII;
import jace.hardware.CardMockingboard;
import jace.hardware.CardRamFactor;
import jace.hardware.CardRamworks;
import jace.hardware.CardThunderclock;
import jace.hardware.PassportMidiInterface;
import jace.hardware.massStorage.CardMassStorage;
import jace.library.MediaEntry;
import jace.library.MediaEntry.MediaFile;
import jace.ui.MetacheatUI;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author blurry
 */
public class LawlessLegends extends Application {

    static LawlessLegends singleton;

    public Stage primaryStage;
    JaceUIController controller;

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
            primaryStage.setScene(s);
            primaryStage.setTitle("Jace");
            EmulatorUILogic.scaleIntegerRatio();
            Utility.loadIcon("revolver_icon.png").ifPresent(primaryStage.getIcons()::add);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.show();
        new Emulator(getParameters().getRaw());
        configureEmulatorForGame();
        javafx.application.Platform.runLater(() -> {
            while (Emulator.computer.getVideo() == null || Emulator.computer.getVideo().getFrameBuffer() == null) {
                Thread.yield();
            }
            controller.connectComputer(Emulator.computer, primaryStage);
            bootWatchdog();
        });
        primaryStage.setOnCloseRequest(event -> {
            Emulator.computer.deactivate();
            Platform.exit();
            System.exit(0);
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
                observe(RAMEvent.TYPE.EXECUTE, 0x0FA62, (e) -> {
                    romStarted = true;
                });
        Emulator.computer.coldStart();
        try {
            Thread.sleep(250);
            if (!romStarted) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Boot not detected, performing a cold start");
                Emulator.computer.coldStart();
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
        Emulator.computer.card7.setValue(CardMassStorage.class);
        Emulator.computer.card6.setValue(CardDiskII.class);
        Emulator.computer.card5.setValue(CardRamFactor.class);
        Emulator.computer.card4.setValue(CardMockingboard.class);
        Emulator.computer.card2.setValue(PassportMidiInterface.class);
        Emulator.computer.reconfigure();
        RAM memory = Emulator.computer.memory;

        // Insert game disk image
        MediaEntry e = new MediaEntry();
        e.author = "8 Bit Bunch";
        e.name = "Lawless Legends";
        MediaFile f = new MediaEntry.MediaFile();
        f.path = getGamePath("game.2mg");
        memory.getCard(7).ifPresent(card -> {
            try {
                ((CardMassStorage) card).currentDrive.insertMedia(e, f);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        // Insert utility disk image
        MediaEntry e2 = new MediaEntry();
        e.author = "8 Bit Bunch";
        e.name = "Lawless Legends Utilities";
        MediaFile f2 = new MediaEntry.MediaFile();
        f.path = getGamePath("utilities.dsk");
        memory.getCard(6).ifPresent(card -> {
            try {
                ((CardDiskII) card).getConsumers()[0].insertMedia(e2, f2);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        });        
    }
    
    private File getGamePath(String filename) {
        File base = getApplicationStoragePath();
        File target = new File(base, filename);
        if (!target.exists()) {
            copyResource(filename, target);
        }
        return target;
    }

    private File getApplicationStoragePath() {
        String path = System.getenv("APPDATA");
        if (path == null) {
            path = System.getProperty("user.home");
        }
        if (path == null) {
            path = ".";
        }
        File base = new File(path);
        File appPath = new File(base, "lawless-legends");
        appPath.mkdirs();
        return appPath;
    }

    private void copyResource(String filename, File target) {
        File localResource = new File(".", filename);
        InputStream in = null;
        if (localResource.exists()) {
            try {
                in = new FileInputStream(localResource);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            in = getClass().getClassLoader().getResourceAsStream("jace/data/" + filename);
        }
        if (in != null) {
            try {
                try (FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[4096];
                    int size;
                    while (in.available() > 0 && (size = in.read()) > 0) {
                        out.write(buf, 0, size);
                    }
                    out.flush();
                }
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }            
        } else {
            Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, "Unable to find resource {0}", filename);
        }
    }
}
