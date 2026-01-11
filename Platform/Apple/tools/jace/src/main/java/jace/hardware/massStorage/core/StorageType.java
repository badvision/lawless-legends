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
 * ProDOS file storage types.
 * Based on ProDOS Technical Reference Manual.
 */
public enum StorageType {
    /**
     * SEEDLING: Single data block, 1-512 bytes.
     */
    SEEDLING(1, 512),

    /**
     * SAPLING: Index block + up to 256 data blocks, 513-131072 bytes (128KB).
     */
    SAPLING(2, 256 * 512),

    /**
     * TREE: Master index + up to 256 sub-indexes + up to 65536 data blocks.
     * Maximum size: 256 * 256 * 512 = 16,777,216 bytes (16MB).
     */
    TREE(3, 256 * 256 * 512);

    private final int value;
    private final long maxFileSize;

    StorageType(int value, long maxFileSize) {
        this.value = value;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Gets the ProDOS storage type value (1, 2, or 3).
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the maximum file size in bytes for this storage type.
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Converts a ProDOS storage type value to the enum.
     *
     * @param value the ProDOS storage type value (1, 2, or 3)
     * @return the corresponding StorageType
     * @throws IllegalArgumentException if the value is not valid
     */
    public static StorageType fromValue(int value) {
        for (StorageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid storage type value: " + value);
    }

    /**
     * Determines the appropriate storage type for a given file size.
     *
     * @param fileSize the file size in bytes
     * @return the appropriate StorageType
     * @throws IllegalArgumentException if fileSize is negative or exceeds TREE max
     */
    public static StorageType fromFileSize(long fileSize) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + fileSize);
        }
        if (fileSize > TREE.maxFileSize) {
            throw new IllegalArgumentException(
                "File size exceeds maximum supported size: " + fileSize + " > " + TREE.maxFileSize);
        }

        if (fileSize <= SEEDLING.maxFileSize) {
            return SEEDLING;
        } else if (fileSize <= SAPLING.maxFileSize) {
            return SAPLING;
        } else {
            return TREE;
        }
    }
}
