package jace.lawless;

import jace.Emulator;
import jace.LawlessLegends;
import jace.apple2e.RAM128k;
import jace.core.Keyboard;
import jace.core.RAM;
import jace.core.Utility;
import jace.hardware.massStorage.CardMassStorage;
import jace.library.DiskType;
import jace.library.MediaConsumer;
import jace.library.MediaEntry;
import jace.library.MediaEntry.MediaFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;

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
        Utility.confirm("Upgrade Game",
                "This will upgrade your game and attempt to copy your progress.  "
                + "If this is unsuccessful you might have to start a new game.  Proceed?",
                () -> performGameUpgrade(e, f));
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
        return e.type == DiskType.FLOPPY800;
    }

    @Override
    public void eject() {
        // Do nothing
    }

    private void insertHardDisk(int drive, MediaEntry entry, MediaFile file) {
        RAM memory = Emulator.computer.memory;

        memory.getCard(7).ifPresent(card -> {
            try {
                ((CardMassStorage) card).getConsumers()[drive].insertMedia(entry, file);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        if (drive == 0) {
            gameMediaEntry = entry;
            gameMediaFile = file;
        }

    }
    
    private void ejectHardDisk(int drive) {
        RAM memory = Emulator.computer.memory;

        memory.getCard(7).ifPresent(card -> {
            ((CardMassStorage) card).getConsumers()[drive].eject();
        });

        if (drive == 0) {
            gameMediaEntry = null;
            gameMediaFile = null;
        }
    }
    

    public void loadGame() {
        // Insert game disk image
        MediaEntry e = new MediaEntry();
        e.author = "8 Bit Bunch";
        e.name = "Lawless Legends";
        e.type = DiskType.LARGE;
        MediaFile f = new MediaEntry.MediaFile();
        f.path = getGamePath("game.2mg");

        if (f.path != null && f.path.exists()) {
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
                java.nio.file.Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Logger.getLogger(LawlessLegends.class.getName()).log(Level.SEVERE, "Unable to find resource {0}", filename);
        }
    }

    private void performGameUpgrade(MediaEntry e, MediaFile f) {
        try {
            System.out.println("Game upgrade starting");
            if (!waitForText("I)mport", 1)) {
                Emulator.computer.coldStart();
                if (!waitForText("I)mport", 1000)) {
                    throw new Exception("Unable to detect upgrade prompt - Upgrade aborted.");
                }
            }
            System.out.println("Menu Propmt detected");
            Keyboard.pasteFromString("i");
            if (!waitForText("Insert disk for import", 100)) {
                throw new Exception("Unable to detect first insert prompt - Upgrade aborted.");                
            }
            System.out.println("First Propmt detected");
            Keyboard.pasteFromString(" ");
            if (!waitForText("Game imported", 100)) {
                throw new Exception("Unable to detect second insert prompt - Upgrade aborted.");                
            }
            System.out.println("Performing upgrade");
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
        LawlessComputer compy = Emulator.computer;
        RAM128k mem = (RAM128k) compy.getMemory();
        while (timeout-- > 0) {
            StringBuilder allText = new StringBuilder();
            for (int i=0x0400; i < 0x07ff; i++) {
                allText.append((char) (mem.getMainMemory().readByte(i) & 0x07f));
            }
            if (allText.toString().contains(message)) {
                return true;
            } else {
                compy.waitForVBL();
            }
        }
        return false;
    }
}
