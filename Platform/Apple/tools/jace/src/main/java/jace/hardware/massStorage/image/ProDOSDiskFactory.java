/**
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.hardware.massStorage.image;

import jace.hardware.massStorage.core.ProDOSConstants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Factory for creating new ProDOS disk images.
 * Uses static factory pattern to create properly initialized .2mg disk images.
 *
 * Thread-safety: This class is thread-safe (all methods are static and stateless).
 */
public class ProDOSDiskFactory {

    private ProDOSDiskFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a new ProDOS disk image with the specified size and volume name.
     *
     * @param diskFile The file to create (will be overwritten if exists)
     * @param diskSize The standard disk size to create
     * @param volumeName The volume name (1-15 characters, must start with letter)
     * @return A ProDOSDiskImage ready for use
     * @throws IOException if creation fails or parameters are invalid
     */
    public static ProDOSDiskImage createDisk(File diskFile, DiskSize diskSize, String volumeName) throws IOException {
        validateVolumeName(volumeName);
        createDiskImage(diskFile, diskSize.getBlocks(), volumeName);
        return new ProDOSDiskImage(diskFile);
    }

    /**
     * Creates a new ProDOS disk image with custom block count.
     *
     * @param diskFile The file to create (will be overwritten if exists)
     * @param blocks The number of 512-byte blocks (1-65535)
     * @param volumeName The volume name (1-15 characters, must start with letter)
     * @return A ProDOSDiskImage ready for use
     * @throws IOException if creation fails or parameters are invalid
     */
    public static ProDOSDiskImage createDisk(File diskFile, int blocks, String volumeName) throws IOException {
        if (blocks < 1 || blocks > ProDOSConstants.MAX_BLOCKS) {
            throw new IOException("Block count must be between 1 and " + ProDOSConstants.MAX_BLOCKS + ": " + blocks);
        }
        validateVolumeName(volumeName);
        createDiskImage(diskFile, blocks, volumeName);
        return new ProDOSDiskImage(diskFile);
    }

    /**
     * Validates a ProDOS volume name.
     *
     * @param volumeName The volume name to validate
     * @throws IOException if the volume name is invalid
     */
    private static void validateVolumeName(String volumeName) throws IOException {
        if (volumeName == null || volumeName.isEmpty()) {
            throw new IOException("Volume name cannot be empty");
        }
        if (volumeName.length() > 15) {
            throw new IOException("Volume name too long (max 15 characters): " + volumeName);
        }
        char first = volumeName.charAt(0);
        if (!Character.isLetter(first)) {
            throw new IOException("Volume name must start with a letter: " + volumeName);
        }
        // ProDOS allows letters, digits, and period in volume names
        for (int i = 0; i < volumeName.length(); i++) {
            char c = volumeName.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.') {
                throw new IOException("Volume name contains invalid character '" + c + "': " + volumeName);
            }
        }
    }

    /**
     * Creates the physical disk image file with proper ProDOS structure.
     */
    private static void createDiskImage(File diskFile, int totalBlocks, String volumeName) throws IOException {
        int diskSize = ProDOSConstants.MG2_HEADER_SIZE + (totalBlocks * ProDOSConstants.BLOCK_SIZE);

        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            // Clear any existing file
            raf.setLength(0);

            // Write 2MG header
            write2MGHeader(raf, totalBlocks);

            // Write boot blocks (blocks 0-1: zeros for now)
            raf.write(new byte[ProDOSConstants.BLOCK_SIZE * 2]);

            // Write volume directory header (block 2)
            writeVolumeDirectory(raf, totalBlocks, volumeName);

            // Write additional directory blocks (blocks 3-5: zeros)
            raf.write(new byte[ProDOSConstants.BLOCK_SIZE * 3]);

            // Write bitmap (starting at block 6)
            writeBitmap(raf, totalBlocks);

            // Fill rest of disk with zeros
            while (raf.length() < diskSize) {
                raf.write(0);
            }

            // Ensure all data is written to disk
            raf.getFD().sync();
        }
    }

    /**
     * Writes the 2MG header.
     */
    private static void write2MGHeader(RandomAccessFile raf, int totalBlocks) throws IOException {
        // Magic number "2IMG"
        raf.write(ProDOSConstants.MG2_MAGIC);

        // Creator (offset 0x04) - 4 bytes
        writeInt32LE(raf, 0x4A414345); // "JACE" in ASCII

        // Header size in 64-byte units (offset 0x08) - 2 bytes
        writeInt16LE(raf, 1); // 1 * 64 = 64 bytes

        // Version (offset 0x0A) - 2 bytes
        writeInt16LE(raf, 1);

        // Image format (offset 0x0C) - 4 bytes (1 = ProDOS)
        writeInt32LE(raf, 1);

        // Flags (offset 0x10) - 4 bytes (0x01 = disk is not write-protected)
        writeInt32LE(raf, 0x01);

        // Number of blocks (offset 0x14) - 4 bytes
        writeInt32LE(raf, totalBlocks);

        // Data offset (offset 0x18) - 4 bytes
        writeInt32LE(raf, ProDOSConstants.MG2_HEADER_SIZE);

        // Data length (offset 0x1C) - 4 bytes
        writeInt32LE(raf, totalBlocks * ProDOSConstants.BLOCK_SIZE);

        // Pad rest of header to 64 bytes (we've written 32 bytes so far)
        while (raf.getFilePointer() < ProDOSConstants.MG2_HEADER_SIZE) {
            raf.write(0);
        }
    }

    /**
     * Writes the volume directory header.
     */
    private static void writeVolumeDirectory(RandomAccessFile raf, int totalBlocks, String volumeName) throws IOException {
        byte[] volDir = new byte[ProDOSConstants.BLOCK_SIZE];

        // Prev block (0x00-0x01): Always 0 for volume directory
        volDir[ProDOSConstants.VOL_PREV_BLOCK] = 0;
        volDir[ProDOSConstants.VOL_PREV_BLOCK + 1] = 0;

        // Next block (0x02-0x03): 0 (no continuation - single volume directory block)
        volDir[ProDOSConstants.VOL_NEXT_BLOCK] = 0;
        volDir[ProDOSConstants.VOL_NEXT_BLOCK + 1] = 0;

        // Storage type (0xF = volume header) and name length
        String upperName = volumeName.toUpperCase();
        volDir[ProDOSConstants.VOL_STORAGE_TYPE_NAME_LENGTH] = (byte) ((0xF << 4) | upperName.length());

        // Volume name
        for (int i = 0; i < upperName.length(); i++) {
            volDir[ProDOSConstants.VOL_VOLUME_NAME + i] = (byte) upperName.charAt(i);
        }

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

        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_CREATION_DATE, date);
        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_CREATION_TIME, time);

        // Version and min version
        volDir[ProDOSConstants.VOL_VERSION] = 0;
        volDir[ProDOSConstants.VOL_MIN_VERSION] = 0;

        // Access (destroy, rename, write, read)
        volDir[ProDOSConstants.VOL_ACCESS] = (byte) ProDOSConstants.ACCESS_DEFAULT;

        // Entry length (0x27 = 39 bytes)
        volDir[ProDOSConstants.VOL_ENTRY_LENGTH] = (byte) ProDOSConstants.DIR_ENTRY_LENGTH;

        // Entries per block (0x0D = 13)
        volDir[ProDOSConstants.VOL_ENTRIES_PER_BLOCK] = (byte) ProDOSConstants.DIR_ENTRIES_PER_BLOCK;

        // File count (starts at 0)
        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_FILE_COUNT, 0);

        // Bitmap pointer (block 6)
        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_BITMAP_POINTER, ProDOSConstants.BITMAP_START_BLOCK);

        // Total blocks
        ProDOSConstants.writeLittleEndianWord(volDir, ProDOSConstants.VOL_TOTAL_BLOCKS, totalBlocks);

        raf.write(volDir);
    }

    /**
     * Writes the allocation bitmap.
     */
    private static void writeBitmap(RandomAccessFile raf, int totalBlocks) throws IOException {
        // Calculate number of bitmap blocks needed
        int bitmapBlocks = (totalBlocks + (ProDOSConstants.BLOCK_SIZE * 8) - 1) /
                          (ProDOSConstants.BLOCK_SIZE * 8);

        // Write first bitmap block with boot blocks, volume dir, and bitmap marked as used
        byte[] bitmap = new byte[ProDOSConstants.BLOCK_SIZE];

        // Mark blocks 0-6 as used (boot blocks 0-1, volume dir 2-5, bitmap starts at 6)
        // ProDOS bitmap: bit SET = FREE, bit CLEAR = USED (allocated)
        // Initialize all bits to 1 (all free), then clear bits for used blocks
        java.util.Arrays.fill(bitmap, (byte) 0xFF);

        // We need to mark the first 7 blocks as used, plus any additional bitmap blocks
        int blocksToMark = 7 + (bitmapBlocks - 1); // Block 6 is first bitmap block, so -1

        for (int i = 0; i < blocksToMark && i < totalBlocks; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            bitmap[byteIndex] &= ~(1 << bitIndex);  // Clear bit = mark as USED
        }

        raf.write(bitmap);

        // Write remaining bitmap blocks (all 0xFF = all free)
        byte[] freeBitmap = new byte[ProDOSConstants.BLOCK_SIZE];
        java.util.Arrays.fill(freeBitmap, (byte) 0xFF);
        for (int i = 1; i < bitmapBlocks; i++) {
            raf.write(freeBitmap);
        }
    }

    /**
     * Writes a 32-bit little-endian integer.
     */
    private static void writeInt32LE(RandomAccessFile raf, int value) throws IOException {
        raf.writeByte(value & 0xFF);
        raf.writeByte((value >> 8) & 0xFF);
        raf.writeByte((value >> 16) & 0xFF);
        raf.writeByte((value >> 24) & 0xFF);
    }

    /**
     * Writes a 16-bit little-endian integer.
     */
    private static void writeInt16LE(RandomAccessFile raf, int value) throws IOException {
        raf.writeByte(value & 0xFF);
        raf.writeByte((value >> 8) & 0xFF);
    }
}
