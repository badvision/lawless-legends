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

/**
 * Standard ProDOS disk sizes.
 * Defines common disk configurations used in Apple II emulation and hardware.
 */
public enum DiskSize {
    /** 140KB - Standard 5.25" floppy disk (35 tracks, 16 sectors) */
    FLOPPY_140KB(280),

    /** 800KB - Standard 3.5" floppy disk */
    FLOPPY_800KB(1600),

    /** 5MB - Small hard disk */
    HARD_5MB(10240),

    /** 10MB - Medium hard disk */
    HARD_10MB(20480),

    /** 20MB - Large hard disk */
    HARD_20MB(40960),

    /** 32MB - Maximum ProDOS volume size (65535 blocks) */
    HARD_32MB(65535);

    private final int blocks;

    DiskSize(int blocks) {
        this.blocks = blocks;
    }

    /**
     * Gets the number of 512-byte blocks for this disk size.
     *
     * @return the block count
     */
    public int getBlocks() {
        return blocks;
    }

    /**
     * Gets the total disk size in bytes (including 2MG header).
     *
     * @return the total file size in bytes
     */
    public int getTotalBytes() {
        return ProDOSConstants.MG2_HEADER_SIZE + (blocks * ProDOSConstants.BLOCK_SIZE);
    }

    /**
     * Gets the data section size in bytes (excluding 2MG header).
     *
     * @return the data size in bytes
     */
    public int getDataBytes() {
        return blocks * ProDOSConstants.BLOCK_SIZE;
    }
}
