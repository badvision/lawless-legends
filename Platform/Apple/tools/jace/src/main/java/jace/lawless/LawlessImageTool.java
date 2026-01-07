package jace.lawless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.Emulator;
import jace.LawlessLegends;
import jace.apple2e.RAM128k;
import jace.core.Computer;
import jace.core.Keyboard;
import jace.core.Utility;
import jace.hardware.massStorage.CardMassStorage;
import jace.library.DiskType;
import jace.library.MediaConsumer;
import jace.library.MediaEntry;
import jace.library.MediaEntry.MediaFile;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

/**
 *
 * @author brobert
 */
public class LawlessImageTool implements MediaConsumer {

    Optional<Label> icon = Optional.empty();
    MediaEntry gameMediaEntry;
    MediaFile gameMediaFile;

    public LawlessImageTool() {
        icon = Utility.loadIconLabel("game_icon.png");
        icon.ifPresent(i -> {
            i.setText("Upgrade Game");
        });
    }

    @Override
    public Optional<Label> getIcon() {
        return icon;
    }

    @Override
    public void setIcon(Optional<Label> i) {
        icon = i;
    }

    @Override
    public void insertMedia(MediaEntry e, MediaEntry.MediaFile f) throws IOException {
        Utility.decision("Upgrade Game", "Do you want to attempt to preserve your save game?", "Yes", "No", () -> performGameUpgradeConfirmation(e, f), () -> replaceGameImageConfirmation(e, f));
    }

    public void performGameUpgradeConfirmation(MediaEntry e, MediaEntry.MediaFile f) {
        Utility.confirm("Upgrade Game",
                "This will upgrade your game and attempt to copy your progress.  "
                + "If this is unsuccessful you will lose your progress and have to start over.  Proceed?",
                () -> performGameUpgrade(e, f));
    }

    public void replaceGameImageConfirmation(MediaEntry e, MediaEntry.MediaFile f) {
        Utility.confirm("Upgrade Game",
                "You are about to replace your game and lose any saved progress.  Proceed?",
                () -> performGameReplace(e, f));
    }

    @Override
    public MediaEntry getMediaEntry() {
        return gameMediaEntry;
    }

    @Override
    public MediaFile getMediaFile() {
        return gameMediaFile;
    }

    @Override
    public boolean isAccepted(MediaEntry e, MediaEntry.MediaFile f) {
        return e.type == DiskType.FLOPPY800 || e.type == DiskType.LARGE;
    }

    @Override
    public void eject() {
        // Do nothing
    }

    private void insertHardDisk(int drive, MediaEntry entry, MediaFile file) {
        Emulator.withMemory(m->m.getCard(7).ifPresent(card -> {
            try {
                ((CardMassStorage) card).getConsumers()[drive].insertMedia(entry, file);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));

        if (drive == 0) {
            gameMediaEntry = entry;
            gameMediaFile = file;
        }

    }

    private void readCurrentDisk(int drive) {
        Emulator.withMemory(m->m.getCard(7).ifPresent(card -> {
            gameMediaEntry = ((CardMassStorage) card).getConsumers()[drive].getMediaEntry();
            gameMediaFile = ((CardMassStorage) card).getConsumers()[drive].getMediaFile();
        }));
    }

    private void ejectHardDisk(int drive) {
        Emulator.withMemory(m->m.getCard(7).ifPresent(card -> {
            ((CardMassStorage) card).getConsumers()[drive].eject();
        }));

        if (drive == 0) {
            gameMediaEntry = null;
            gameMediaFile = null;
        }
    }

    public void loadGame() {
        loadGame(null);
    }

    public void loadGame(UpgradeHandler upgradeHandler) {
        // Insert game disk image
        MediaEntry e = new MediaEntry();
        e.author = "8 Bit Bunch";
        e.name = "Lawless Legends";
        e.type = DiskType.LARGE;
        MediaFile f = new MediaEntry.MediaFile();
        f.path = getGamePath("game.2mg");

        if (f.path != null && f.path.exists()) {
            // Check for upgrades before loading if handler is provided
            if (upgradeHandler != null) {
                boolean shouldContinue = upgradeHandler.checkAndHandleUpgrade(f.path);
                if (!shouldContinue) {
                    // User cancelled - exit the application
                    System.exit(0);
                    return;
                }
            }

            insertHardDisk(0, e, f);
        }
    }

    private File getGamePath(String filename) {
        File base = getApplicationStoragePath();
        File target = new File(base, filename);
        if (!target.exists()) {
            copyResource(filename, target);
        }
        return target;
    }

    public File getApplicationStoragePath() {
        String path = System.getenv("APPDATA");
        if (path == null) {
            path = System.getProperty("user.home");
        }
        if (path == null) {
            path = System.getProperty("user.dir");
        }
        File base = new File(path);
        File appPath = new File(base, "lawless-legends");
        appPath.mkdirs();
        return appPath;
    }
    
    private File getUserGameFile() {
        return new File(getApplicationStoragePath(), "game.2mg");
    }

    private void copyResource(String filename, File target) {
        File localResource = getUserGameFile();
        InputStream in = null;
        if (localResource.exists()) {
            try {
                in = new FileInputStream(localResource);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            in = getClass().getResourceAsStream("/jace/data/" + filename);
        }
        if (in != null) {
            try {
                java.nio.file.Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, "Unable to find resource {0}", filename);
            Utility.decision("Unable to find game", "Sorry partner, we can't find yer game disk.  What're ya' gonna do about it?", "I have it", "Tuck tail and leave", this::selectGameFile, ()->System.exit(1));
        }
    }
    
    private void selectGameFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName("game.2mg");
            fileChooser.setTitle("Please locate your Lawless Legends game.2mg file to continue");
            File gameFile = fileChooser.showOpenDialog(null);
            if (gameFile == null || !gameFile.exists()) {
                Utility.gripe("Sorry pardner, can't help ya' this time.", true, ()->{System.exit(1);});
            } else {
                java.nio.file.Files.copy(gameFile.toPath(), getUserGameFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                loadGame();
                Emulator.withComputer(Computer::coldStart);
            }
        } catch (IOException ex) {
            Logger.getLogger(LawlessImageTool.class.getName()).log(Level.SEVERE, null, ex);
            Utility.gripe("Couldn't load yer game, friend.  Heard some fellow mumbling something about " + ex.getMessage(), true, ()->{System.exit(1);});
        }        
    }

    private void performGameReplace(MediaEntry e, MediaFile f) {
        try {
            File target = getMediaFile().path;
            ejectHardDisk(0);
            java.nio.file.Files.copy(f.path.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            f.path = target;
            insertHardDisk(0, e, f);
            Emulator.withComputer(Computer::coldStart);
            System.out.println("Upgrade completed");
        } catch (IOException ex) {
            Logger.getLogger(LawlessImageTool.class.getName()).log(Level.SEVERE, null, ex);
            Utility.gripe(ex.getMessage());
        }
    }

    void performGameUpgrade(MediaEntry e, MediaFile f) {
        try {
            System.out.println("Game upgrade starting");
            readCurrentDisk(0);
            MediaEntry originalEntry = gameMediaEntry;
            MediaFile originalFile = gameMediaFile;

            // Put in new disk and boot it -- we want to use its importer in case that importer works better!
            ejectHardDisk(0);
            insertHardDisk(0, e, f);
            Emulator.withComputer(Computer::coldStart);
            if (!waitForText("I)mport", 1)) {
                Emulator.withComputer(Computer::coldStart);
                if (!waitForText("I)mport", 2000)) {
                    throw new Exception("Unable to detect upgrade prompt - Upgrade aborted.");
                }
            }
            System.out.println("Menu Propmt detected");

            Keyboard.pasteFromString("i");
            if (!waitForText("Insert disk for import", 1500)) {
                throw new Exception("Unable to detect first insert prompt - Upgrade aborted.");
            }
            System.out.println("First Propmt detected");

            // Now put in the original disk to load its saved game (hopefully!)
            ejectHardDisk(0);
            insertHardDisk(0, originalEntry, originalFile);

            Keyboard.pasteFromString(" ");
            if (!waitForText("Game imported", 2000)) {
                throw new Exception("Unable to detect second insert prompt - Upgrade aborted.");
            }
            System.out.println("Completing upgrade");
            // Now we copy the new game disk over the old and insert it to write the save game and complete the upgrade.
            File target = getMediaFile().path;
            ejectHardDisk(0);
            java.nio.file.Files.copy(f.path.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            f.path = target;
            insertHardDisk(0, e, f);
            Keyboard.pasteFromString(" ");
            System.out.println("Upgrade completed");
        } catch (Exception ex) {
            Logger.getLogger(LawlessImageTool.class.getName()).log(Level.SEVERE, null, ex);
            Utility.gripe(ex.getMessage());
        }
    }

    private boolean waitForText(String message, int timeout) throws InterruptedException {
        while (timeout-- > 0) {
            StringBuilder allText = new StringBuilder();
            Emulator.withMemory(mem -> {
                for (int i = 0x0400; i < 0x07ff; i++) {
                    allText.append((char) ((RAM128k) mem).getMainMemory().readByte(i) & 0x07f);
                }
            });
            if (allText.toString().contains(message)) {
                return true;
            } else {
                Emulator.withComputer(c->{
                    try {
                        ((LawlessComputer)c).waitForVBL();
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                });
            }
        }
        return false;
    }
}
