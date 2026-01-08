package jace.lawless;

import jace.hardware.massStorage.image.ProDOSDiskImage;
import java.io.File;

public class ExtractMyGameVersion {
    public static void main(String[] args) throws Exception {
        File gameFile = new File(System.getProperty("user.home"), "lawless-legends/game.2mg");

        System.out.println("Extracting version from: " + gameFile.getAbsolutePath());

        // Read partition and show chunk types
        try (ProDOSDiskImage disk = new ProDOSDiskImage(gameFile)) {
            byte[] partitionData = disk.readFile("GAME.PART.1");
            if (partitionData != null) {
                System.out.println("\nPartition size: " + partitionData.length + " bytes");

                // Parse header to see chunk types
                int headerSize = (partitionData[0] & 0xFF) | ((partitionData[1] & 0xFF) << 8);
                System.out.println("Header size: " + headerSize + " bytes");

                System.out.println("\nChunk type summary:");
                int pos = 2;
                int chunkIndex = 0;
                java.util.Map<Integer, Integer> typeCounts = new java.util.HashMap<>();
                while (pos + 4 <= headerSize) {
                    int type = partitionData[pos] & 0xFF;
                    int num = partitionData[pos + 1] & 0xFF;
                    int len = (partitionData[pos + 2] & 0xFF) | ((partitionData[pos + 3] & 0xFF) << 8);
                    boolean compressed = (len & 0x8000) != 0;
                    len = len & 0x7FFF;

                    typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);

                    // Show last 5 chunks (including the resourceIndex)
                    if (chunkIndex >= 134) {
                        System.out.printf("  Chunk %d: type=0x%02X num=%d len=%d compressed=%s\n",
                                          chunkIndex, type, num, len, compressed);
                    }

                    pos += 4;
                    if (compressed) {
                        pos += 2; // skip compressedLen
                    }
                    chunkIndex++;
                }
                System.out.println("\nType distribution:");
                for (int t : new java.util.TreeSet<>(typeCounts.keySet())) {
                    System.out.printf("  Type 0x%02X: %d chunks\n", t, typeCounts.get(t));
                }
            }
        }

        String version = GameVersionReader.extractVersion(gameFile);

        if (version != null) {
            System.out.println("\n✓ SUCCESS - Game version: " + version);
        } else {
            System.out.println("\n✗ FAILED - Could not extract version");
        }
    }
}
