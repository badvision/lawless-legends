package jace.lawless;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser for Lawless Legends partition file format.
 * Extracts chunk information from GAME.PART.* files.
 *
 * Partition format (from A2PackPartitions.groovy):
 * [header_size:2bytes (little-endian)]
 * [chunk_entries...]
 *   Each entry: [type:byte][num:byte][length:2bytes][compressedLen:2bytes if compressed]
 * [chunk_data...]
 */
public class PartitionParser {

    private static final Logger LOGGER = Logger.getLogger(PartitionParser.class.getName());

    /**
     * Chunk types from the partition format.
     */
    public static final int TYPE_CODE = 0x01;  // From A2PackPartitions.groovy: TYPE_CODE = 1

    /**
     * Information about a chunk in the partition.
     */
    public static class ChunkInfo {
        public final int type;
        public final int num;
        public final int length;
        public final int uncompressedLength;
        public final boolean compressed;
        public final int dataOffset;

        public ChunkInfo(int type, int num, int length, int uncompressedLength, boolean compressed, int dataOffset) {
            this.type = type;
            this.num = num;
            this.length = length;
            this.uncompressedLength = uncompressedLength;
            this.compressed = compressed;
            this.dataOffset = dataOffset;
        }

        @Override
        public String toString() {
            return String.format("Chunk[type=0x%02x, num=%d, len=%d, uclen=%d, compressed=%s, offset=%d]",
                    type, num, length, uncompressedLength, compressed, dataOffset);
        }
    }

    /**
     * Parses chunk information from partition data.
     *
     * @param partitionData The raw partition file data
     * @return List of chunks found in the partition
     * @throws IllegalArgumentException if the partition format is invalid
     */
    public static List<ChunkInfo> parseChunks(byte[] partitionData) {
        if (partitionData == null || partitionData.length < 2) {
            throw new IllegalArgumentException("Partition data too small");
        }

        List<ChunkInfo> chunks = new ArrayList<>();

        // Read header size (little-endian)
        int headerSize = (partitionData[0] & 0xFF) | ((partitionData[1] & 0xFF) << 8);
        LOGGER.fine("Header size: " + headerSize + " bytes");

        if (headerSize < 3 || headerSize > partitionData.length) {
            throw new IllegalArgumentException("Invalid header size: " + headerSize);
        }

        // Parse chunk entries in header
        int pos = 2;
        int currentDataOffset = headerSize;

        while (pos < headerSize - 1) {
            int type = partitionData[pos++] & 0xFF;

            // Type 0 marks end of header
            if (type == 0) {
                break;
            }

            int num = partitionData[pos++] & 0xFF;

            // Read length (little-endian)
            int lengthLow = partitionData[pos++] & 0xFF;
            int lengthHigh = partitionData[pos++] & 0xFF;

            // Check if compressed (high bit of length's high byte)
            boolean compressed = (lengthHigh & 0x80) != 0;
            lengthHigh &= 0x7F; // Clear compression flag
            int length = lengthLow | (lengthHigh << 8);

            int uncompressedLength = length;
            if (compressed) {
                // Read uncompressed length (little-endian)
                int ucLow = partitionData[pos++] & 0xFF;
                int ucHigh = partitionData[pos++] & 0xFF;
                uncompressedLength = ucLow | (ucHigh << 8);
            }

            ChunkInfo chunk = new ChunkInfo(type, num, length, uncompressedLength, compressed, currentDataOffset);
            chunks.add(chunk);
            LOGGER.fine("Found: " + chunk);

            currentDataOffset += length;
        }

        LOGGER.info("Parsed " + chunks.size() + " chunks from partition");
        return chunks;
    }

    /**
     * Finds the resourceIndex chunk in the partition.
     * The resourceIndex is a CODE chunk (type 0x01) that contains the game version.
     * It's assigned num=code.size()+1, making it the LAST CODE chunk.
     *
     * @param partitionData The raw partition file data
     * @return The resourceIndex chunk, or null if not found
     */
    public static ChunkInfo findResourceIndexChunk(byte[] partitionData) {
        List<ChunkInfo> chunks = parseChunks(partitionData);

        // The resourceIndex is the LAST CODE chunk (num = code.size() + 1)
        // Find the CODE chunk with the highest num value
        ChunkInfo resourceIndex = null;
        int maxNum = -1;

        for (ChunkInfo chunk : chunks) {
            if (chunk.type == TYPE_CODE && chunk.num > maxNum) {
                maxNum = chunk.num;
                resourceIndex = chunk;
                LOGGER.fine("Found CODE chunk candidate: " + chunk);
            }
        }

        if (resourceIndex != null) {
            LOGGER.info("Found resourceIndex chunk (last CODE): " + resourceIndex);
            return resourceIndex;
        }

        LOGGER.warning("No resourceIndex chunk found in partition");
        return null;
    }

    /**
     * Extracts the raw chunk data from the partition.
     *
     * @param partitionData The raw partition file data
     * @param chunk The chunk to extract
     * @return The chunk data
     * @throws IllegalArgumentException if the chunk offset/length is invalid
     */
    public static byte[] extractChunkData(byte[] partitionData, ChunkInfo chunk) {
        if (chunk.dataOffset + chunk.length > partitionData.length) {
            throw new IllegalArgumentException(
                    "Chunk extends beyond partition data: offset=" + chunk.dataOffset +
                    ", length=" + chunk.length + ", dataLen=" + partitionData.length);
        }

        byte[] chunkData = new byte[chunk.length];
        System.arraycopy(partitionData, chunk.dataOffset, chunkData, 0, chunk.length);
        return chunkData;
    }
}
