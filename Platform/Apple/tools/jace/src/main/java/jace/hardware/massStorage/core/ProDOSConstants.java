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

package jace.hardware.massStorage.core;

/**
 * ProDOS filesystem constants and structures.
 * Based on ProDOS Technical Reference Manual.
 */
public class ProDOSConstants {

    // Block and disk layout
    public static final int BLOCK_SIZE = 512;
    public static final int VOLUME_DIR_BLOCK = 2;
    public static final int BITMAP_START_BLOCK = 6;
    public static final int MAX_BLOCKS = 65535;

    // 2MG format
    public static final int MG2_HEADER_SIZE = 64;
    public static final byte[] MG2_MAGIC = {0x32, 0x49, 0x4D, 0x47}; // "2IMG"

    // Directory entry offsets
    public static final int DIR_ENTRY_LENGTH = 0x27; // 39 bytes
    public static final int DIR_ENTRIES_PER_BLOCK = 0x0D; // 13 entries per block
    public static final int DIR_HEADER_LENGTH = 0x04; // 4 bytes before first entry

    // Storage types
    public static final int STORAGE_DELETED = 0x0;
    public static final int STORAGE_SEEDLING = 0x1;  // 1 block (0-512 bytes)
    public static final int STORAGE_SAPLING = 0x2;   // 2-256 blocks (513-131072 bytes)
    public static final int STORAGE_TREE = 0x3;      // 257-32768 blocks
    public static final int STORAGE_SUBDIRECTORY = 0xD;
    public static final int STORAGE_VOLUME_HEADER = 0xF;

    // File types
    public static final int FILE_TYPE_UNKNOWN = 0x00;
    public static final int FILE_TYPE_BAD = 0x01;
    public static final int FILE_TYPE_TEXT = 0x04;
    public static final int FILE_TYPE_BINARY = 0x06;
    public static final int FILE_TYPE_DIRECTORY = 0x0F;
    public static final int FILE_TYPE_SYS = 0xFF;

    // Directory entry field offsets
    public static final int ENTRY_STORAGE_TYPE_NAME_LENGTH = 0x00;
    public static final int ENTRY_FILE_NAME = 0x01;
    public static final int ENTRY_FILE_TYPE = 0x10;
    public static final int ENTRY_KEY_POINTER = 0x11;  // 2 bytes, little-endian
    public static final int ENTRY_BLOCKS_USED = 0x13;  // 2 bytes, little-endian
    public static final int ENTRY_EOF = 0x15;          // 3 bytes, little-endian
    public static final int ENTRY_CREATION_DATE = 0x18; // 2 bytes
    public static final int ENTRY_CREATION_TIME = 0x1A; // 2 bytes
    public static final int ENTRY_VERSION = 0x1C;
    public static final int ENTRY_MIN_VERSION = 0x1D;
    public static final int ENTRY_ACCESS = 0x1E;
    public static final int ENTRY_AUX_TYPE = 0x1F;    // 2 bytes
    public static final int ENTRY_MOD_DATE = 0x21;    // 2 bytes
    public static final int ENTRY_MOD_TIME = 0x23;    // 2 bytes
    public static final int ENTRY_HEADER_POINTER = 0x25; // 2 bytes

    // Volume directory header offsets
    public static final int VOL_PREV_BLOCK = 0x00;  // Always 0 for volume dir
    public static final int VOL_NEXT_BLOCK = 0x02;  // Next volume dir block
    public static final int VOL_STORAGE_TYPE_NAME_LENGTH = 0x04;
    public static final int VOL_VOLUME_NAME = 0x05;
    public static final int VOL_CREATION_DATE = 0x18;
    public static final int VOL_CREATION_TIME = 0x1A;
    public static final int VOL_VERSION = 0x1C;
    public static final int VOL_MIN_VERSION = 0x1D;
    public static final int VOL_ACCESS = 0x1E;
    public static final int VOL_ENTRY_LENGTH = 0x1F;
    public static final int VOL_ENTRIES_PER_BLOCK = 0x20;
    public static final int VOL_FILE_COUNT = 0x21;    // 2 bytes
    public static final int VOL_BITMAP_POINTER = 0x23; // 2 bytes
    public static final int VOL_TOTAL_BLOCKS = 0x25;  // 2 bytes

    // Access bits
    public static final int ACCESS_DESTROY = 0x80;
    public static final int ACCESS_RENAME = 0x40;
    public static final int ACCESS_BACKUP = 0x20;
    public static final int ACCESS_WRITE = 0x02;
    public static final int ACCESS_READ = 0x01;
    public static final int ACCESS_DEFAULT = ACCESS_DESTROY | ACCESS_RENAME | ACCESS_WRITE | ACCESS_READ;

    private ProDOSConstants() {
        // Utility class - no instantiation
    }

    /**
     * Converts a 16-bit little-endian value to an integer.
     */
    public static int readLittleEndianWord(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8);
    }

    /**
     * Writes a 16-bit little-endian value to a buffer.
     */
    public static void writeLittleEndianWord(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * Reads a 24-bit little-endian value (used for EOF).
     */
    public static int readLittleEndian24(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) |
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16);
    }

    /**
     * Writes a 24-bit little-endian value (used for EOF).
     */
    public static void writeLittleEndian24(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
    }

    /**
     * Encodes a ProDOS date (year since 1900, month, day).
     */
    public static int encodeProDOSDate(int year, int month, int day) {
        int yearSince1900 = year - 1900;
        return ((yearSince1900 & 0x7F) << 9) | ((month & 0x0F) << 5) | (day & 0x1F);
    }

    /**
     * Encodes a ProDOS time (hour, minute).
     */
    public static int encodeProDOSTime(int hour, int minute) {
        return ((hour & 0x1F) << 8) | (minute & 0x3F);
    }
}
