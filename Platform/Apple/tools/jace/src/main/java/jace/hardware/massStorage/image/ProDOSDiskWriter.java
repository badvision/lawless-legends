package jace.hardware.massStorage.image;

import jace.hardware.massStorage.core.BlockWriter;
import jace.hardware.massStorage.core.ProDOSConstants;
import jace.hardware.massStorage.core.StorageType;
import jace.hardware.massStorage.core.TreeStrategy;
import jace.hardware.massStorage.core.SaplingStrategy;
import jace.hardware.massStorage.core.SeedlingStrategy;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.BitSet;

/**
 * ProDOS disk writer for .2mg disk images.
 * Provides low-level block I/O and bitmap management.
 *
 * Thread-safety: This class is NOT thread-safe. Callers must synchronize access.
 */
public class ProDOSDiskWriter implements BlockWriter, Closeable {

    private final File diskFile;
    private final RandomAccessFile raf;
    private final int dataOffset;
    private final int totalBlocks;
    private final BitSet bitmap;
    private boolean bitmapModified;

    /**
     * Opens a ProDOS disk image for writing.
     *
     * @param diskFile The .2mg disk image file
     * @throws IOException if the file doesn't exist or is not a valid ProDOS disk
     */
    public ProDOSDiskWriter(File diskFile) throws IOException {
        if (!diskFile.exists()) {
            throw new IOException("Disk file does not exist: " + diskFile);
        }

        this.diskFile = diskFile;
        this.raf = new RandomAccessFile(diskFile, "rw");

        // Read and validate 2MG header
        byte[] magic = new byte[4];
        raf.readFully(magic);
        if (!Arrays.equals(magic, ProDOSConstants.MG2_MAGIC)) {
            raf.close();
            throw new IOException("Not a valid 2MG disk image");
        }

        // Skip to number of blocks field (offset 0x14)
        raf.seek(0x14);
        int numBlocks = readInt32LE();  // Number of blocks
        this.dataOffset = readInt32LE(); // Data offset (0x18)
        int dataLength = readInt32LE();  // Data length (0x1C)

        // Calculate total blocks
        this.totalBlocks = dataLength / ProDOSConstants.BLOCK_SIZE;

        // Read bitmap from disk
        this.bitmap = new BitSet(totalBlocks);
        this.bitmapModified = false;
        loadBitmap();
    }

    /**
     * Reads a block from the disk.
     *
     * @param blockNum The block number to read
     * @return The 512-byte block data
     * @throws IOException if read fails or block number is invalid
     */
    public byte[] readBlock(int blockNum) throws IOException {
        validateBlockNumber(blockNum);

        byte[] block = new byte[ProDOSConstants.BLOCK_SIZE];
        raf.seek(dataOffset + (blockNum * ProDOSConstants.BLOCK_SIZE));
        raf.readFully(block);
        return block;
    }

    /**
     * Writes a block to the disk.
     *
     * @param blockNum The block number to write
     * @param data The 512-byte block data
     * @throws IOException if write fails, block number is invalid, or data size is wrong
     */
    public void writeBlock(int blockNum, byte[] data) throws IOException {
        validateBlockNumber(blockNum);

        if (data.length != ProDOSConstants.BLOCK_SIZE) {
            throw new IOException("Block data must be exactly " + ProDOSConstants.BLOCK_SIZE + " bytes");
        }

        raf.seek(dataOffset + (blockNum * ProDOSConstants.BLOCK_SIZE));
        raf.write(data);
    }

    /**
     * Checks if a block is allocated in the bitmap.
     *
     * @param blockNum The block number to check
     * @return true if the block is allocated (in use)
     */
    public boolean isBlockAllocated(int blockNum) {
        if (blockNum < 0 || blockNum >= totalBlocks) {
            return true; // Out of range blocks are considered "allocated"
        }
        return bitmap.get(blockNum);
    }

    /**
     * Allocates a single free block.
     *
     * @return The allocated block number
     * @throws IOException if no free blocks are available
     */
    public int allocateBlock() throws IOException {
        for (int i = 0; i < totalBlocks; i++) {
            if (!bitmap.get(i)) {
                bitmap.set(i);
                bitmapModified = true;
                return i;
            }
        }
        throw new IOException("Disk full - no free blocks available");
    }

    /**
     * Allocates multiple free blocks.
     *
     * @param count The number of blocks to allocate
     * @return Array of allocated block numbers
     * @throws IOException if not enough free blocks are available
     */
    public int[] allocateBlocks(int count) throws IOException {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        // Check if enough blocks are available
        int freeCount = getFreeBlockCount();
        if (freeCount < count) {
            throw new IOException("Disk full - requested " + count +
                                " blocks but only " + freeCount + " available");
        }

        int[] blocks = new int[count];
        int allocated = 0;

        for (int i = 0; i < totalBlocks && allocated < count; i++) {
            if (!bitmap.get(i)) {
                bitmap.set(i);
                blocks[allocated++] = i;
            }
        }

        if (allocated != count) {
            // Rollback allocation if we couldn't get all blocks
            for (int i = 0; i < allocated; i++) {
                bitmap.clear(blocks[i]);
            }
            throw new IOException("Failed to allocate " + count + " blocks");
        }

        bitmapModified = true;
        return blocks;
    }

    /**
     * Deallocates a block, marking it as free.
     *
     * @param blockNum The block number to deallocate
     */
    public void deallocateBlock(int blockNum) {
        if (blockNum >= 0 && blockNum < totalBlocks) {
            boolean wasSet = bitmap.get(blockNum);
            bitmap.clear(blockNum);
            bitmapModified = true;
            if (!wasSet) {
                java.util.logging.Logger.getLogger(getClass().getName()).fine(
                    "Attempted to deallocate already-free block " + blockNum);
            }
        }
    }

    /**
     * Frees a block (BlockWriter interface implementation).
     * Alias for deallocateBlock().
     *
     * @param blockNumber The block number to free
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if blockNumber is invalid
     */
    @Override
    public void freeBlock(int blockNumber) throws IOException {
        if (blockNumber < 0 || blockNumber >= totalBlocks) {
            throw new IllegalArgumentException("Block number out of range: " + blockNumber);
        }
        deallocateBlock(blockNumber);
    }

    /**
     * Gets the number of free blocks available.
     *
     * @return The count of free blocks
     */
    public int getFreeBlockCount() {
        int count = 0;
        for (int i = 0; i < totalBlocks; i++) {
            if (!bitmap.get(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Flushes the bitmap to disk if it has been modified.
     *
     * @throws IOException if write fails
     */
    public void flushBitmap() throws IOException {
        if (!bitmapModified) {
            return;
        }

        // Read volume directory to get bitmap pointer
        byte[] volDir = readBlock(ProDOSConstants.VOLUME_DIR_BLOCK);
        int bitmapPointer = ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_BITMAP_POINTER);

        // Debug: log the bitmap pointer
        java.util.logging.Logger.getLogger(getClass().getName()).info(
            String.format("Flush bitmap: pointer=%d (0x%04X), total blocks=%d",
                        bitmapPointer, bitmapPointer, totalBlocks));

        // Calculate number of bitmap blocks needed
        int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) /
                          (ProDOSConstants.BLOCK_SIZE * 8);

        // Write bitmap blocks
        for (int blockIndex = 0; blockIndex < bitmapBlocks; blockIndex++) {
            byte[] bitmapBlock = new byte[ProDOSConstants.BLOCK_SIZE];
            int startBit = blockIndex * ProDOSConstants.BLOCK_SIZE * 8;
            int endBit = Math.min(startBit + (ProDOSConstants.BLOCK_SIZE * 8), totalBlocks);

            // Convert BitSet to ProDOS bitmap format
            // ProDOS bitmap: bit SET = FREE, bit CLEAR = USED (allocated)
            // Our BitSet: bit SET = USED (allocated), bit CLEAR = FREE
            // Therefore: Must INVERT the bits when writing to disk
            for (int bit = startBit; bit < endBit; bit++) {
                int byteIndex = (bit - startBit) / 8;
                int bitIndex = (bit - startBit) % 8;

                // Set bit if block is FREE (invert BitSet convention for disk format)
                if (!bitmap.get(bit)) {
                    bitmapBlock[byteIndex] |= (1 << bitIndex);
                }
            }

            writeBlock(bitmapPointer + blockIndex, bitmapBlock);
        }

        bitmapModified = false;
    }

    /**
     * Loads the bitmap from disk into memory.
     *
     * @throws IOException if read fails
     */
    private void loadBitmap() throws IOException {
        // Read volume directory to get bitmap pointer
        byte[] volDir = readBlock(ProDOSConstants.VOLUME_DIR_BLOCK);
        int bitmapPointer = ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_BITMAP_POINTER);

        // Debug: log the bitmap pointer
        java.util.logging.Logger.getLogger(getClass().getName()).info(
            String.format("Load bitmap: pointer=%d (0x%04X), total blocks=%d",
                        bitmapPointer, bitmapPointer, totalBlocks));

        // Calculate number of bitmap blocks
        int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) /
                          (ProDOSConstants.BLOCK_SIZE * 8);

        // Read bitmap blocks
        for (int blockIndex = 0; blockIndex < bitmapBlocks; blockIndex++) {
            byte[] bitmapBlock = readBlock(bitmapPointer + blockIndex);
            int startBit = blockIndex * ProDOSConstants.BLOCK_SIZE * 8;
            int endBit = Math.min(startBit + (ProDOSConstants.BLOCK_SIZE * 8), totalBlocks);

            // Convert ProDOS bitmap to BitSet
            // ProDOS bitmap: bit SET = FREE, bit CLEAR = USED (allocated)
            // Our BitSet: bit SET = USED (allocated), bit CLEAR = FREE
            // Therefore: Must INVERT the bits when reading from disk
            for (int bit = startBit; bit < endBit; bit++) {
                int byteIndex = (bit - startBit) / 8;
                int bitIndex = (bit - startBit) % 8;

                boolean isFree = (bitmapBlock[byteIndex] & (1 << bitIndex)) != 0;
                bitmap.set(bit, !isFree); // Invert: disk bit SET = FREE, BitSet bit SET = USED
            }
        }
    }

    /**
     * Validates a block number is in range.
     */
    private void validateBlockNumber(int blockNum) throws IOException {
        if (blockNum < 0 || blockNum >= totalBlocks) {
            throw new IOException("Block number out of range: " + blockNum +
                                " (valid range: 0-" + (totalBlocks - 1) + ")");
        }
    }

    /**
     * Reads a 32-bit little-endian integer from the current position.
     */
    private int readInt32LE() throws IOException {
        int b0 = raf.read() & 0xFF;
        int b1 = raf.read() & 0xFF;
        int b2 = raf.read() & 0xFF;
        int b3 = raf.read() & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    /**
     * Closes the disk writer and flushes any pending changes.
     */
    @Override
    public void close() throws IOException {
        try {
            if (bitmapModified) {
                flushBitmap();
            }
            // Ensure all data is written to disk before closing
            raf.getFD().sync();
        } finally {
            raf.close();
        }
    }

    /**
     * Gets the data offset (for testing/debugging).
     */
    int getDataOffset() {
        return dataOffset;
    }

    /**
     * Gets the total number of blocks (BlockReader interface implementation).
     */
    @Override
    public int getTotalBlocks() {
        return totalBlocks;
    }

    // ========== FILE OPERATIONS ==========

    /**
     * Writes a file to the disk, creating or overwriting as needed.
     *
     * @param filename The ProDOS filename (max 15 chars, uppercase recommended)
     * @param data The file data
     * @param fileType The ProDOS file type
     * @throws IOException if write fails or disk is full
     */
    public void writeFile(String filename, byte[] data, int fileType) throws IOException {
        validateFilename(filename);

        // Check if file already exists
        DirectoryEntry existingEntry = findFileEntry(filename);
        if (existingEntry != null) {
            // Overwrite existing file
            overwriteFile(existingEntry, data, fileType);
        } else {
            // Create new file
            createNewFile(filename, data, fileType);
        }
    }

    /**
     * Reads a file from the disk.
     *
     * @param filename The ProDOS filename
     * @return The file data, or null if file doesn't exist
     * @throws IOException if read fails
     */
    public byte[] readFile(String filename) throws IOException {
        DirectoryEntry entry = findFileEntry(filename);
        if (entry == null) {
            return null;
        }

        int storageType = entry.storageType;
        int keyPointer = entry.keyPointer;
        int eof = entry.eof;

        if (eof == 0) {
            return new byte[0];
        }

        if (storageType == ProDOSConstants.STORAGE_SEEDLING) {
            // Single block file
            SeedlingStrategy seedlingStrategy = new SeedlingStrategy();
            return seedlingStrategy.readFile(this, keyPointer, eof);
        } else if (storageType == ProDOSConstants.STORAGE_SAPLING) {
            // Multi-block file with index block
            SaplingStrategy saplingStrategy = new SaplingStrategy();
            return saplingStrategy.readFile(this, keyPointer, eof);
        } else if (storageType == ProDOSConstants.STORAGE_TREE) {
            // TREE file with master index + sub-indexes
            TreeStrategy treeStrategy = new TreeStrategy();
            return treeStrategy.readFile(this, keyPointer, eof);
        } else {
            throw new IOException("Unsupported storage type: " + storageType);
        }
    }

    /**
     * Checks if a file exists on the disk.
     *
     * @param filename The ProDOS filename
     * @return true if file exists
     */
    public boolean fileExists(String filename) throws IOException {
        return findFileEntry(filename) != null;
    }

    /**
     * Deletes a file from the disk.
     *
     * @param filename The ProDOS filename
     * @throws IOException if file doesn't exist or delete fails
     */
    public void deleteFile(String filename) throws IOException {
        DirectoryEntry entry = findFileEntry(filename);
        if (entry == null) {
            throw new IOException("File not found: " + filename);
        }

        // Deallocate file blocks
        deallocateFileBlocks(entry);

        // Mark directory entry as deleted
        markEntryDeleted(entry);

        // Update file count
        updateFileCount(-1);
    }

    /**
     * Gets the number of files in the volume directory.
     *
     * @return The file count
     */
    public int getFileCount() throws IOException {
        byte[] volDir = readBlock(ProDOSConstants.VOLUME_DIR_BLOCK);
        return ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_FILE_COUNT);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Directory entry structure.
     */
    private static class DirectoryEntry {
        int blockNum;
        int entryIndex;
        int storageType;
        String name;
        int fileType;
        int keyPointer;
        int blocksUsed;
        int eof;
        int auxType;
    }

    /**
     * Validates a ProDOS filename.
     */
    private void validateFilename(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("Filename cannot be empty");
        }
        if (filename.length() > 15) {
            throw new IOException("Filename too long (max 15 characters): " + filename);
        }
        // ProDOS filenames should start with a letter
        char first = filename.charAt(0);
        if (!Character.isLetter(first)) {
            throw new IOException("Filename must start with a letter: " + filename);
        }
    }

    /**
     * Finds a file entry in the volume directory.
     */
    private DirectoryEntry findFileEntry(String filename) throws IOException {
        String upperName = filename.toUpperCase();

        // Start with volume directory block
        int currentBlock = ProDOSConstants.VOLUME_DIR_BLOCK;

        while (currentBlock != 0) {
            byte[] dirBlock = readBlock(currentBlock);

            // Get next block pointer
            int nextBlock = ProDOSConstants.readLittleEndianWord(dirBlock, 0x02);

            // In volume directory block, skip the volume header (entry 0)
            int startEntry = (currentBlock == ProDOSConstants.VOLUME_DIR_BLOCK) ? 1 : 0;
            int offset = (currentBlock == ProDOSConstants.VOLUME_DIR_BLOCK) ?
                        ProDOSConstants.DIR_HEADER_LENGTH + ProDOSConstants.DIR_ENTRY_LENGTH :
                        ProDOSConstants.DIR_HEADER_LENGTH;

            for (int i = startEntry; i < ProDOSConstants.DIR_ENTRIES_PER_BLOCK; i++) {
                if (offset + ProDOSConstants.DIR_ENTRY_LENGTH > ProDOSConstants.BLOCK_SIZE) {
                    break;
                }

                int storageTypeAndLength = dirBlock[offset] & 0xFF;
                int storageType = (storageTypeAndLength >> 4) & 0x0F;
                int nameLength = storageTypeAndLength & 0x0F;

                if (storageType != ProDOSConstants.STORAGE_DELETED && nameLength > 0) {
                    // Read filename
                    StringBuilder name = new StringBuilder();
                    for (int j = 0; j < nameLength; j++) {
                        name.append((char) (dirBlock[offset + 1 + j] & 0x7F));
                    }

                    if (name.toString().equals(upperName)) {
                        // Found it!
                        DirectoryEntry entry = new DirectoryEntry();
                        entry.blockNum = currentBlock;
                        entry.entryIndex = i;
                        entry.storageType = storageType;
                        entry.name = name.toString();
                        entry.fileType = dirBlock[offset + ProDOSConstants.ENTRY_FILE_TYPE] & 0xFF;
                        entry.keyPointer = ProDOSConstants.readLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_KEY_POINTER);
                        entry.blocksUsed = ProDOSConstants.readLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_BLOCKS_USED);
                        entry.eof = ProDOSConstants.readLittleEndian24(dirBlock, offset + ProDOSConstants.ENTRY_EOF);
                        entry.auxType = ProDOSConstants.readLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_AUX_TYPE);
                        return entry;
                    }
                }

                offset += ProDOSConstants.DIR_ENTRY_LENGTH;
            }

            currentBlock = nextBlock;
        }

        return null;
    }

    /**
     * Creates a new file on the disk using appropriate storage strategy.
     */
    private void createNewFile(String filename, byte[] data, int fileType) throws IOException {
        int dataLength = data.length;

        // Determine storage type based on file size (handles 0-byte files too)
        StorageType storageType = StorageType.fromFileSize(dataLength);

        int keyPointer;
        int blocksUsed;

        switch (storageType) {
            case SEEDLING:
                // Use SeedlingStrategy for 0-512 bytes
                SeedlingStrategy seedlingStrategy = new SeedlingStrategy();
                keyPointer = seedlingStrategy.writeFile(this, data);
                blocksUsed = seedlingStrategy.calculateBlocksNeeded(dataLength);
                break;

            case SAPLING:
                // Use SaplingStrategy for 513-131072 bytes
                SaplingStrategy saplingStrategy = new SaplingStrategy();
                keyPointer = saplingStrategy.writeFile(this, data);
                blocksUsed = saplingStrategy.calculateBlocksNeeded(dataLength);
                break;

            case TREE:
                // Use TreeStrategy for >131072 bytes (>128KB)
                TreeStrategy treeStrategy = new TreeStrategy();
                keyPointer = treeStrategy.writeFile(this, data);
                blocksUsed = treeStrategy.calculateBlocksNeeded(dataLength);
                break;

            default:
                throw new IOException("Unsupported storage type: " + storageType);
        }

        // Write directory entry with calculated values
        writeDirectoryEntry(filename, fileType, storageType.getValue(),
                          keyPointer, blocksUsed, dataLength, 0);

        updateFileCount(1);
    }

    /**
     * Writes a SEEDLING file (1 data block).
     */
    private void writeSeedlingFile(String filename, byte[] data, int fileType) throws IOException {
        int dataBlock = allocateBlock();

        // Write data (pad to full block)
        byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
        System.arraycopy(data, 0, blockData, 0, data.length);
        writeBlock(dataBlock, blockData);

        // Write directory entry
        writeDirectoryEntry(filename, fileType, ProDOSConstants.STORAGE_SEEDLING,
                          dataBlock, 1, data.length, 0);
    }

    /**
     * Writes a SAPLING file (2-128 blocks with index block).
     */
    private void writeSaplingFile(String filename, byte[] data, int fileType) throws IOException {
        int dataLength = data.length;
        int numDataBlocks = (dataLength + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;

        // Allocate index block + data blocks
        int indexBlock = allocateBlock();
        int[] dataBlocks = allocateBlocks(numDataBlocks);

        // Write data blocks
        for (int i = 0; i < numDataBlocks; i++) {
            byte[] blockData = new byte[ProDOSConstants.BLOCK_SIZE];
            int offset = i * ProDOSConstants.BLOCK_SIZE;
            int length = Math.min(ProDOSConstants.BLOCK_SIZE, dataLength - offset);
            System.arraycopy(data, offset, blockData, 0, length);
            writeBlock(dataBlocks[i], blockData);
        }

        // Create index block
        byte[] indexBlockData = new byte[ProDOSConstants.BLOCK_SIZE];
        for (int i = 0; i < numDataBlocks; i++) {
            ProDOSConstants.writeLittleEndianWord(indexBlockData, i * 2, dataBlocks[i]);
        }
        writeBlock(indexBlock, indexBlockData);

        // Write directory entry
        writeDirectoryEntry(filename, fileType, ProDOSConstants.STORAGE_SAPLING,
                          indexBlock, numDataBlocks + 1, dataLength, 0);
    }

    /**
     * Reads a SAPLING file using its index block.
     */
    private byte[] readSaplingFile(int indexBlock, int eof) throws IOException {
        byte[] indexData = readBlock(indexBlock);
        byte[] result = new byte[eof];

        int numDataBlocks = (eof + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;
        int offset = 0;

        for (int i = 0; i < numDataBlocks; i++) {
            // Read using ProDOS split format: low bytes at 0-255, high bytes at 256-511
            int dataBlockNum = (indexData[i] & 0xFF) | ((indexData[256 + i] & 0xFF) << 8);
            if (dataBlockNum == 0) {
                break;
            }

            byte[] blockData = readBlock(dataBlockNum);
            int length = Math.min(ProDOSConstants.BLOCK_SIZE, eof - offset);
            System.arraycopy(blockData, 0, result, offset, length);
            offset += length;
        }

        return result;
    }

    /**
     * Overwrites an existing file using appropriate storage strategy.
     */
    private void overwriteFile(DirectoryEntry entry, byte[] data, int fileType) throws IOException {
        // Log disk space before deallocation
        int freeBeforeDealloc = getFreeBlockCount();
        int blocksToFree = entry.blocksUsed;

        java.util.logging.Logger.getLogger(getClass().getName()).info(
            String.format("Before deallocation: %d free blocks (total=%d)",
                        freeBeforeDealloc, totalBlocks));

        // Deallocate old blocks
        deallocateFileBlocks(entry);

        // Log disk space after deallocation
        int freeAfterDealloc = getFreeBlockCount();

        java.util.logging.Logger.getLogger(getClass().getName()).info(
            String.format("After deallocation: %d free blocks (should have gained ~%d)",
                        freeAfterDealloc, blocksToFree));

        // Write new file data using appropriate strategy
        int dataLength = data.length;

        // Determine storage type based on file size (handles 0-byte files too)
        StorageType storageType = StorageType.fromFileSize(dataLength);

        // Calculate blocks needed for new file
        int blocksNeeded;
        switch (storageType) {
            case SEEDLING:
                blocksNeeded = new SeedlingStrategy().calculateBlocksNeeded(dataLength);
                break;
            case SAPLING:
                blocksNeeded = new SaplingStrategy().calculateBlocksNeeded(dataLength);
                break;
            case TREE:
                blocksNeeded = new TreeStrategy().calculateBlocksNeeded(dataLength);
                break;
            default:
                throw new IOException("Unsupported storage type: " + storageType);
        }

        // Enhanced error message if not enough space
        if (freeAfterDealloc < blocksNeeded) {
            throw new IOException(String.format(
                "Disk full - overwrite would need %d blocks but only %d free " +
                "(freed %d blocks from old file of %d blocks, had %d free before dealloc)",
                blocksNeeded, freeAfterDealloc, blocksToFree, entry.blocksUsed, freeBeforeDealloc));
        }

        int keyPointer;
        int blocksUsed;

        switch (storageType) {
            case SEEDLING:
                // SEEDLING (0-512 bytes)
                SeedlingStrategy seedlingStrategy = new SeedlingStrategy();
                keyPointer = seedlingStrategy.writeFile(this, data);
                blocksUsed = seedlingStrategy.calculateBlocksNeeded(dataLength);
                break;

            case SAPLING:
                // SAPLING (513-131072 bytes)
                SaplingStrategy saplingStrategy = new SaplingStrategy();
                keyPointer = saplingStrategy.writeFile(this, data);
                blocksUsed = saplingStrategy.calculateBlocksNeeded(dataLength);
                break;

            case TREE:
                // TREE (>131072 bytes, >128KB)
                TreeStrategy treeStrategy = new TreeStrategy();
                keyPointer = treeStrategy.writeFile(this, data);
                blocksUsed = treeStrategy.calculateBlocksNeeded(dataLength);
                break;

            default:
                throw new IOException("Unsupported storage type: " + storageType);
        }

        updateDirectoryEntry(entry, fileType, storageType.getValue(),
                           keyPointer, blocksUsed, dataLength, 0);
    }

    /**
     * Deallocates all blocks used by a file.
     */
    private void deallocateFileBlocks(DirectoryEntry entry) throws IOException {
        if (entry.storageType == ProDOSConstants.STORAGE_SEEDLING) {
            deallocateBlock(entry.keyPointer);
        } else if (entry.storageType == ProDOSConstants.STORAGE_SAPLING) {
            // Read index block
            byte[] indexData = readBlock(entry.keyPointer);

            // Deallocate all data blocks referenced in the index
            // Scan ALL 256 possible index entries to handle corrupted/sparse files
            // where blocksUsed doesn't match actual allocated blocks
            int freedCount = 0;
            int actuallyFreedCount = 0; // Blocks that were allocated before we freed them

            // Deallocate all data blocks using ProDOS split format
            // Low bytes at positions 0-255, high bytes at positions 256-511
            for (int i = 0; i < 256; i++) {
                int blockNum = (indexData[i] & 0xFF) | ((indexData[256 + i] & 0xFF) << 8);
                if (blockNum != 0) {
                    boolean wasAllocated = bitmap.get(blockNum);
                    deallocateBlock(blockNum);
                    freedCount++;
                    if (wasAllocated) {
                        actuallyFreedCount++;
                    }
                }
            }

            // Deallocate index block
            boolean indexWasAllocated = bitmap.get(entry.keyPointer);
            deallocateBlock(entry.keyPointer);
            freedCount++;
            if (indexWasAllocated) {
                actuallyFreedCount++;
            }

            // Log if deallocation doesn't match directory entry or if blocks were already free
            int dataBlocksNeeded = (entry.eof + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;
            if (freedCount != entry.blocksUsed || actuallyFreedCount != freedCount) {
                java.util.logging.Logger.getLogger(getClass().getName()).info(
                    String.format("Deallocated %d blocks (%d were actually allocated) from file " +
                                "(directory claimed %d, EOF=%d bytes needs %d)",
                                freedCount, actuallyFreedCount, entry.blocksUsed, entry.eof, dataBlocksNeeded + 1));
            }
        } else if (entry.storageType == ProDOSConstants.STORAGE_TREE) {
            // Read master index block
            byte[] masterIndex = readBlock(entry.keyPointer);

            // Calculate number of data blocks from EOF
            // blocksUsed = 1 (master) + N (sub-indexes) + M (data blocks)
            int totalDataBlocks = (entry.eof + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE;
            int numSubIndexes = (totalDataBlocks + 255) / 256; // Round up

            // Iterate through sub-indexes
            for (int subIndexNum = 0; subIndexNum < numSubIndexes; subIndexNum++) {
                // Get sub-index block number from master index (ProDOS split format)
                // Low byte at offset subIndexNum, high byte at offset 256+subIndexNum
                int subIndexBlock = (masterIndex[subIndexNum] & 0xFF) |
                                   ((masterIndex[256 + subIndexNum] & 0xFF) << 8);
                if (subIndexBlock == 0) {
                    break;
                }

                // Read sub-index block
                byte[] subIndex = readBlock(subIndexBlock);

                // Calculate how many data blocks this sub-index references
                int dataBlocksInSubIndex = Math.min(256, totalDataBlocks - (subIndexNum * 256));

                // Deallocate data blocks referenced by this sub-index (ProDOS split format)
                for (int i = 0; i < dataBlocksInSubIndex; i++) {
                    int dataBlockNum = (subIndex[i] & 0xFF) |
                                      ((subIndex[256 + i] & 0xFF) << 8);
                    if (dataBlockNum != 0) {
                        deallocateBlock(dataBlockNum);
                    }
                }

                // Deallocate sub-index block
                deallocateBlock(subIndexBlock);
            }

            // Deallocate master index block
            deallocateBlock(entry.keyPointer);
        }
    }

    /**
     * Writes a directory entry for a new file.
     */
    private void writeDirectoryEntry(String filename, int fileType, int storageType,
                                    int keyPointer, int blocksUsed, int eof, int auxType) throws IOException {
        // Find first free directory entry
        int currentBlock = ProDOSConstants.VOLUME_DIR_BLOCK;
        int entryIndex = -1;
        int targetBlock = -1;

        while (currentBlock != 0) {
            byte[] dirBlock = readBlock(currentBlock);
            int nextBlock = ProDOSConstants.readLittleEndianWord(dirBlock, 0x02);

            // In volume directory block, skip the volume header (entry 0)
            int startEntry = (currentBlock == ProDOSConstants.VOLUME_DIR_BLOCK) ? 1 : 0;
            int offset = (currentBlock == ProDOSConstants.VOLUME_DIR_BLOCK) ?
                        ProDOSConstants.DIR_HEADER_LENGTH + ProDOSConstants.DIR_ENTRY_LENGTH :
                        ProDOSConstants.DIR_HEADER_LENGTH;

            for (int i = startEntry; i < ProDOSConstants.DIR_ENTRIES_PER_BLOCK; i++) {
                if (offset + ProDOSConstants.DIR_ENTRY_LENGTH > ProDOSConstants.BLOCK_SIZE) {
                    break;
                }

                int storageTypeAndLength = dirBlock[offset] & 0xFF;
                int entryStorageType = (storageTypeAndLength >> 4) & 0x0F;

                if (entryStorageType == ProDOSConstants.STORAGE_DELETED) {
                    // Found free entry
                    targetBlock = currentBlock;
                    entryIndex = i;
                    break;
                }

                offset += ProDOSConstants.DIR_ENTRY_LENGTH;
            }

            if (targetBlock != -1) {
                break;
            }

            currentBlock = nextBlock;
        }

        if (targetBlock == -1) {
            throw new IOException("Directory full");
        }

        // Create directory entry
        DirectoryEntry entry = new DirectoryEntry();
        entry.blockNum = targetBlock;
        entry.entryIndex = entryIndex;
        writeEntryData(entry, filename, fileType, storageType, keyPointer, blocksUsed, eof, auxType);
    }

    /**
     * Updates an existing directory entry.
     */
    private void updateDirectoryEntry(DirectoryEntry entry, int fileType, int storageType,
                                     int keyPointer, int blocksUsed, int eof, int auxType) throws IOException {
        writeEntryData(entry, entry.name, fileType, storageType, keyPointer, blocksUsed, eof, auxType);
    }

    /**
     * Writes entry data to the directory block.
     */
    private void writeEntryData(DirectoryEntry entry, String filename, int fileType, int storageType,
                                int keyPointer, int blocksUsed, int eof, int auxType) throws IOException {
        // Sanity check: entry 0 in volume directory is the volume header, not a file entry
        if (entry.blockNum == ProDOSConstants.VOLUME_DIR_BLOCK && entry.entryIndex == 0) {
            throw new IOException("Cannot write file entry at index 0 - that's the volume header!");
        }

        byte[] dirBlock = readBlock(entry.blockNum);

        // Calculate offset based on entry index
        // In volume directory block, entry 0 is the volume header, so entries start at 1
        int offset;
        if (entry.blockNum == ProDOSConstants.VOLUME_DIR_BLOCK) {
            // Volume directory: header at offset 4, entry 1 starts at offset 4 + 39 = 43 (0x2B)
            offset = ProDOSConstants.DIR_HEADER_LENGTH + (entry.entryIndex * ProDOSConstants.DIR_ENTRY_LENGTH);
        } else {
            // Regular directory blocks: header (4 bytes), then entries starting at 0
            offset = ProDOSConstants.DIR_HEADER_LENGTH + (entry.entryIndex * ProDOSConstants.DIR_ENTRY_LENGTH);
        }

        // Additional sanity check on the calculated offset
        if (entry.blockNum == ProDOSConstants.VOLUME_DIR_BLOCK && offset < 0x2B) {
            throw new IOException(String.format(
                "Invalid directory offset %d (0x%02X) for volume directory entry %d - would corrupt volume header!",
                offset, offset, entry.entryIndex));
        }

        // Clear entry
        for (int i = 0; i < ProDOSConstants.DIR_ENTRY_LENGTH; i++) {
            dirBlock[offset + i] = 0;
        }

        // Storage type and name length
        String upperName = filename.toUpperCase();
        dirBlock[offset] = (byte) ((storageType << 4) | upperName.length());

        // Filename
        for (int i = 0; i < upperName.length(); i++) {
            dirBlock[offset + 1 + i] = (byte) upperName.charAt(i);
        }

        // File type
        dirBlock[offset + ProDOSConstants.ENTRY_FILE_TYPE] = (byte) fileType;

        // Key pointer
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_KEY_POINTER, keyPointer);

        // Blocks used
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_BLOCKS_USED, blocksUsed);

        // EOF
        ProDOSConstants.writeLittleEndian24(dirBlock, offset + ProDOSConstants.ENTRY_EOF, eof);

        // Creation date/time (use current time)
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);

        int date = ProDOSConstants.encodeProDOSDate(year, month, day);
        int time = ProDOSConstants.encodeProDOSTime(hour, minute);

        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_CREATION_DATE, date);
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_CREATION_TIME, time);
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_MOD_DATE, date);
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_MOD_TIME, time);

        // Access
        dirBlock[offset + ProDOSConstants.ENTRY_ACCESS] = (byte) ProDOSConstants.ACCESS_DEFAULT;

        // Aux type
        ProDOSConstants.writeLittleEndianWord(dirBlock, offset + ProDOSConstants.ENTRY_AUX_TYPE, auxType);

        // Write block back
        writeBlock(entry.blockNum, dirBlock);
    }

    /**
     * Marks a directory entry as deleted.
     */
    private void markEntryDeleted(DirectoryEntry entry) throws IOException {
        byte[] dirBlock = readBlock(entry.blockNum);

        // Calculate offset (same logic as writeEntryData)
        int offset = ProDOSConstants.DIR_HEADER_LENGTH + (entry.entryIndex * ProDOSConstants.DIR_ENTRY_LENGTH);

        // Set storage type to 0 (deleted)
        dirBlock[offset] = (byte) (dirBlock[offset] & 0x0F); // Keep name length, clear storage type

        writeBlock(entry.blockNum, dirBlock);
    }

    /**
     * Updates the file count in the volume directory.
     */
    private void updateFileCount(int delta) throws IOException {
        byte[] volDir = readBlock(ProDOSConstants.VOLUME_DIR_BLOCK);
        int fileCount = ProDOSConstants.readLittleEndianWord(volDir, ProDOSConstants.VOL_FILE_COUNT);
        fileCount += delta;
        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_FILE_COUNT, fileCount);
        writeBlock(ProDOSConstants.VOLUME_DIR_BLOCK, volDir);
    }
}
