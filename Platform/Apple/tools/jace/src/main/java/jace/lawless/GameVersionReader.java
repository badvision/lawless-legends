package jace.lawless;

import jace.hardware.massStorage.image.ProDOSDiskImage;
import jace.lawless.compression.Lx47Algorithm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the game version string from the game disk image.
 * The version is stored in the resourceIndex chunk of GAME.PART.1.
 *
 * The resourceIndex is:
 * - In GAME.PART.1
 * - Type CODE (0x02)
 * - Compressed using Lx47 algorithm
 * - Version string is at the beginning after decompression (Pascal string: [length][chars...])
 */
public class GameVersionReader {

    private static final Logger LOGGER = Logger.getLogger(GameVersionReader.class.getName());
    private static final String PARTITION_FILE = "GAME.PART.1";

    /**
     * Extracts the game version string from a disk image InputStream.
     * This is useful for reading from packaged resources.
     *
     * @param inputStream The input stream containing the game disk image
     * @return The version string (e.g., "5123a.2"), or null if extraction fails
     */
    public static String extractVersion(InputStream inputStream) {
        if (inputStream == null) {
            LOGGER.warning("InputStream is null");
            return null;
        }

        File tempFile = null;
        try {
            // Create temporary file
            tempFile = Files.createTempFile("game-version-", ".2mg").toFile();
            tempFile.deleteOnExit();

            // Copy stream to temp file
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Extract version from temp file
            return extractVersion(tempFile);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to extract version from InputStream", e);
            return null;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Extracts the game version string from a disk image.
     *
     * @param gameFile The game disk image file (.2mg)
     * @return The version string (e.g., "5123a.2"), or null if extraction fails
     */
    public static String extractVersion(File gameFile) {
        if (gameFile == null || !gameFile.exists()) {
            LOGGER.warning("Game file does not exist: " + gameFile);
            return null;
        }

        try (ProDOSDiskImage disk = new ProDOSDiskImage(gameFile)) {
            // Read the partition file
            byte[] partitionData = disk.readFile(PARTITION_FILE);
            if (partitionData == null) {
                LOGGER.warning("Could not read " + PARTITION_FILE + " from disk image");
                return null;
            }

            // Find the resourceIndex chunk
            PartitionParser.ChunkInfo resourceIndex = PartitionParser.findResourceIndexChunk(partitionData);
            if (resourceIndex == null) {
                LOGGER.warning("Could not find resourceIndex chunk in " + PARTITION_FILE);
                return null;
            }

            // Extract and decompress the chunk data
            byte[] compressedData = PartitionParser.extractChunkData(partitionData, resourceIndex);
            byte[] decompressedData = new byte[resourceIndex.uncompressedLength];
            Lx47Algorithm.decompress(compressedData, 0, decompressedData, 0, resourceIndex.uncompressedLength);

            // Read Pascal string at the beginning (length byte followed by characters)
            if (decompressedData.length < 1) {
                LOGGER.warning("Decompressed data too small for version string");
                return null;
            }

            int versionLength = decompressedData[0] & 0xFF;
            if (versionLength < 1 || versionLength + 1 > decompressedData.length) {
                LOGGER.warning("Invalid version string length: " + versionLength);
                return null;
            }

            // Extract version string
            byte[] versionBytes = new byte[versionLength];
            System.arraycopy(decompressedData, 1, versionBytes, 0, versionLength);
            return new String(versionBytes, "US-ASCII");

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to extract version from " + gameFile.getName(), e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error extracting version", e);
            return null;
        }
    }

    /**
     * Extracts version from partition data directly (for testing).
     *
     * @param partitionData Raw partition file data
     * @return The version string, or null if extraction fails
     */
    static String extractVersionFromPartition(byte[] partitionData) {
        try {
            PartitionParser.ChunkInfo resourceIndex = PartitionParser.findResourceIndexChunk(partitionData);
            if (resourceIndex == null) {
                return null;
            }

            byte[] compressedData = PartitionParser.extractChunkData(partitionData, resourceIndex);
            byte[] decompressedData = new byte[resourceIndex.uncompressedLength];
            Lx47Algorithm.decompress(compressedData, 0, decompressedData, 0, resourceIndex.uncompressedLength);

            int versionLength = decompressedData[0] & 0xFF;
            if (versionLength < 1 || versionLength + 1 > decompressedData.length) {
                return null;
            }

            byte[] versionBytes = new byte[versionLength];
            System.arraycopy(decompressedData, 1, versionBytes, 0, versionLength);
            return new String(versionBytes, "US-ASCII");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract version from partition data", e);
            return null;
        }
    }
}
