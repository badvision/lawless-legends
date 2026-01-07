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

import jace.Emulator;
import jace.apple2e.MOS65C02;
import jace.apple2e.SoftSwitches;
import jace.core.RAM;
import jace.hardware.massStorage.CardMassStorage;
import jace.hardware.massStorage.IDisk;
import static jace.hardware.ProdosDriver.MLI_COMMAND;
import static jace.hardware.ProdosDriver.MLI_UNITNUMBER;
import jace.hardware.ProdosDriver.MLI_COMMAND_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Universal ProDOS disk image handler that provides both:
 * 1. SmartPort-compatible operations (IDisk) for emulator integration
 * 2. Block and file-level operations (BlockWriter) for file manipulation
 *
 * Unifies the functionality of:
 * - LargeDisk (emulator disk mounting)
 * - ProDOSDiskWriter (file operations on disk images)
 *
 * This class extends ProDOSDiskWriter to inherit all block and file operations,
 * then adds SmartPort operations (mliRead/mliWrite) for emulator integration.
 *
 * Architecture:
 * - Backing store: Disk image file (.2mg, .dsk, .hdv, .po, .do)
 * - Writes: Update blocks in disk image atomically
 * - SmartPort operations: Map to block I/O with emulator memory
 * - File operations: High-level ProDOS file read/write
 *
 * Thread-safety: This class is NOT thread-safe. Callers must synchronize access.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class ProDOSDiskImage extends ProDOSDiskWriter implements IDisk {

    private static final Logger LOGGER = Logger.getLogger(ProDOSDiskImage.class.getName());

    private final File diskPath;
    private boolean writeProtected = false;

    /**
     * Opens a ProDOS disk image for both emulator and file operations.
     *
     * @param diskFile The disk image file (.2mg, .dsk, .hdv, .po, .do)
     * @throws IOException if the file doesn't exist or is not a valid ProDOS disk
     */
    public ProDOSDiskImage(File diskFile) throws IOException {
        super(diskFile);
        this.diskPath = diskFile;
    }

    // ========================================================================
    // IDisk interface implementation (SmartPort operations for emulator)
    // ========================================================================

    /**
     * SmartPort READ operation.
     * Reads a 512-byte block from the disk image into emulator memory.
     *
     * @param block the block number to read (0-based)
     * @param bufferAddress the emulator memory address to write to
     * @throws IOException if read fails
     */
    @Override
    public void mliRead(int block, int bufferAddress) throws IOException {
        AtomicReference<IOException> error = new AtomicReference<>();
        Emulator.withMemory(memory -> {
            try {
                byte[] blockData = readBlock(block);
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    memory.write(bufferAddress + i, blockData[i], true, false);
                }
            } catch (IOException ex) {
                error.set(ex);
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
    }

    /**
     * SmartPort WRITE operation.
     * Writes a 512-byte block from emulator memory to the disk image.
     *
     * @param block the block number to write (0-based)
     * @param bufferAddress the emulator memory address to read from
     * @throws IOException if write fails
     */
    @Override
    public void mliWrite(int block, int bufferAddress) throws IOException {
        if (writeProtected) {
            throw new IOException("Disk is write-protected");
        }

        AtomicReference<IOException> error = new AtomicReference<>();
        Emulator.withMemory(memory -> {
            try {
                byte[] blockData = new byte[BLOCK_SIZE];
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    blockData[i] = memory.readRaw(bufferAddress + i);
                }
                writeBlock(block, blockData);
            } catch (IOException ex) {
                error.set(ex);
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
    }

    /**
     * SmartPort FORMAT operation.
     * Currently not implemented.
     *
     * @throws IOException always (not supported yet)
     */
    @Override
    public void mliFormat() throws IOException {
        throw new UnsupportedOperationException("Format not supported yet");
    }

    /**
     * Boot0 operation for emulator.
     * Loads block 0 to $800 and jumps to it with proper register setup.
     *
     * @param slot the slot number (1-7)
     * @throws IOException if boot block cannot be read
     */
    @Override
    public void boot0(int slot) throws IOException {
        Emulator.withComputer(c ->
            c.getCpu().whilePaused(() -> {
                try {
                    mliRead(0, 0x0800);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to read boot block", ex);
                    return;
                }

                byte slot16 = (byte) (slot << 4);
                ((MOS65C02) c.getCpu()).X = slot16;
                RAM memory = c.getMemory();

                // Set up soft switches for boot
                SoftSwitches.AUXZP.getSwitch().setState(false);
                SoftSwitches.LCBANK1.getSwitch().setState(false);
                SoftSwitches.LCRAM.getSwitch().setState(false);
                SoftSwitches.LCWRITE.getSwitch().setState(true);
                SoftSwitches.RAMRD.getSwitch().setState(false);
                SoftSwitches.RAMWRT.getSwitch().setState(false);
                SoftSwitches.CXROM.getSwitch().setState(false);
                SoftSwitches.SLOTC3ROM.getSwitch().setState(false);
                SoftSwitches.INTC8ROM.getSwitch().setState(false);

                memory.write(CardMassStorage.SLT16, slot16, false, false);
                memory.write(MLI_COMMAND, (byte) MLI_COMMAND_TYPE.READ.intValue, false, false);
                memory.write(MLI_UNITNUMBER, slot16, false, false);

                // Write location to block read routine to zero page
                memory.writeWord(0x048, 0x0c000 + CardMassStorage.DEVICE_DRIVER_OFFSET +
                               (slot * 0x0100), false, false);

                c.getCpu().setProgramCounter(0x0800);
            })
        );
    }

    /**
     * Returns the size of the disk in 512-byte blocks.
     *
     * @return the number of blocks on this disk
     */
    @Override
    public int getSize() {
        return getTotalBlocks();
    }

    /**
     * Ejects the disk (closes the file).
     */
    @Override
    public void eject() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing disk image during eject", e);
        }
    }

    /**
     * Checks if the disk is write-protected.
     *
     * @return true if write-protected
     */
    @Override
    public boolean isWriteProtected() {
        return writeProtected;
    }

    /**
     * Sets the write-protect status.
     *
     * @param writeProtected true to write-protect the disk
     */
    public void setWriteProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    /**
     * Gets the physical file path of this disk image.
     *
     * @return the disk image file
     */
    public File getPhysicalPath() {
        return diskPath;
    }
}
