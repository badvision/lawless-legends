package jace.lawless;

import jace.hardware.massStorage.image.ProDOSDiskImage;
import java.io.File;

public class ListGameFiles {
    public static void main(String[] args) throws Exception {
        File gameFile = new File(System.getProperty("user.home"), "lawless-legends/game.2mg");

        System.out.println("Listing files in: " + gameFile.getAbsolutePath());
        System.out.println();

        try (ProDOSDiskImage disk = new ProDOSDiskImage(gameFile)) {
            // Try common partition file names
            String[] candidates = {
                "GAME.PART.1", "GAME.PART.1.BIN", "GAME.1",
                "GAME.PART.2", "GAME.PART.2.BIN", "GAME.2",
                "PRODOS", "GAME.1.SAVE"
            };

            System.out.println("File listing:");
            for (String filename : candidates) {
                try {
                    byte[] data = disk.readFile(filename);
                    if (data != null) {
                        System.out.println("✓ Found: " + filename + " (" + data.length + " bytes)");

                        // Try to read Pascal string from beginning
                        if (data.length > 1) {
                            int length = data[0] & 0xFF;
                            if (length > 0 && length < 100 && (1 + length) <= data.length) {
                                StringBuilder version = new StringBuilder();
                                boolean isPrintable = true;
                                for (int i = 0; i < length; i++) {
                                    char c = (char) (data[1 + i] & 0xFF);
                                    if (c < 32 || c > 126) {
                                        isPrintable = false;
                                        break;
                                    }
                                    version.append(c);
                                }
                                if (isPrintable) {
                                    System.out.println("  → Possible version string: \"" + version + "\"");
                                }
                            }
                        }

                        // Show first 32 bytes as hex for analysis
                        System.out.print("  → First bytes: ");
                        for (int i = 0; i < Math.min(32, data.length); i++) {
                            System.out.printf("%02X ", data[i] & 0xFF);
                        }
                        System.out.println();
                    }
                } catch (Exception e) {
                    // File doesn't exist, continue
                }
            }
        }
    }
}
