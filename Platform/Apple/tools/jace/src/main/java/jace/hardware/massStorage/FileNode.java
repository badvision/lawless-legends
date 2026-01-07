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

package jace.hardware.massStorage;

import jace.hardware.massStorage.core.BlockReader;
import jace.hardware.massStorage.core.ProDOSConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Representation of a prodos file with a known file type and having a known
 * size (either seedling, sapling or tree)
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class FileNode extends DiskNode implements BlockReader {

    @Override
    public int getLength() {
        return (int) getPhysicalFile().length();
    }

    public enum FileType {

        UNKNOWN(0x00, 0x0000),
        ADB(0x019, 0x0000),
        AWP(0x01a, 0x0000),
        ASP(0x01b, 0x0000),
        BAD(0x01, 0x0000),
        BIN(0x06, 0x0300),
        CLASS(0xED, 0x0000),
        BAS(0xfc, 0x0801),
        CMD(0x0f0, 0x0000),
        INT(0xfa, 0x0801),
        IVR(0xfb, 0x0000),
        PAS(0xef, 0x0000),
        REL(0x0Fe, 0x0000),
        SHK(0x0e0, 0x08002),
        SDK(0x0e0, 0x08002),
        SYS(0x0ff, 0x02000),
        SYSTEM(0x0ff, 0x02000),
        TXT(0x04, 0x0000),
        U01(0x0f1, 0x0000),
        U02(0x0f2, 0x0000),
        U03(0x0f3, 0x0000),
        U04(0x0f4, 0x0000),
        U05(0x0f5, 0x0000),
        U06(0x0f6, 0x0000),
        U07(0x0f7, 0x0000),
        U08(0x0f8, 0x0000),
        VAR(0x0FD, 0x0000);
        public int code = 0;
        public int defaultLoadAddress = 0;

        FileType(int code, int addr) {
            this.code = code;
            this.defaultLoadAddress = addr;
        }

        public static FileType findByCode(int code) {
            for (FileType t : FileType.values()) {
                if (t.code == code) {
                    return t;
                }
            }
            return UNKNOWN;
        }
    }
    public int fileType = 0x00;
    public int loadAddress = 0x00;
    public static int SEEDLING_MAX_SIZE = ProDOSConstants.BLOCK_SIZE;
    public static int SAPLING_MAX_SIZE = ProDOSConstants.BLOCK_SIZE * 128;

    @Override
    public EntryType getType() {
        long fileSize = getPhysicalFile().length();
        if (fileSize <= SEEDLING_MAX_SIZE) {
            setType(EntryType.SEEDLING);
            return EntryType.SEEDLING;
        } else if (fileSize <= SAPLING_MAX_SIZE) {
            setType(EntryType.SAPLING);
            return EntryType.SAPLING;
        } else {
            setType(EntryType.TREE);
            return EntryType.TREE;
        }
    }

    @Override
    public void setName(String name) {
        FileType t = FileType.UNKNOWN;
        int offset = 0;
        String prodosName = name;
        if (name.matches("^.*?#[0-9A-Fa-f]{6}$")) {
            int type = Integer.parseInt(name.substring(name.length() - 6, name.length() - 4), 16);
            offset = Integer.parseInt(name.substring(name.length() - 4), 16);
            t = FileType.findByCode(type);
            prodosName = name.substring(0, name.length()-7).replaceAll("[^A-Za-z0-9#]", ".").toUpperCase();
        } else {
            String[] parts = name.replaceAll("[^A-Za-z0-9#]", ".").split("\\.");
            if (parts.length > 1) {
                String extension = parts[parts.length - 1].toUpperCase();
                String[] extParts = extension.split("\\#");
                if (extParts.length == 2) {
                    offset = Integer.parseInt(extParts[1], 16);
                    extension = extParts[0];
                }
                try {
                    t = FileType.valueOf(extension);
                } catch (IllegalArgumentException ex) {
                    System.out.println("Not sure what extension " + extension + " is!");
                }
                prodosName = "";
                for (int i = 0; i < parts.length - 1; i++) {
                    prodosName += (i > 0 ? "." + parts[i] : parts[i]);
                }
                if (extParts[extParts.length - 1].equals("SYSTEM")) {
                    prodosName += ".SYSTEM";
                }
            }
        }
        if (offset == 0) {
            offset = t.defaultLoadAddress;
        }
        fileType = t.code;
        loadAddress = offset;

        // Pass usable name (stripped of file extension and other type info) as name
        super.setName(prodosName);
    }

    public FileNode(ProdosVirtualDisk ownerFilesystem, File file) throws IOException {
        super(ownerFilesystem);
        setPhysicalFile(file);
        setName(file.getName());
        allocate();
    }

    @Override
    public void doDeallocate() {
    }

    @Override
    public void doAllocate() throws IOException {
        int dataBlocks = (int) ((getPhysicalFile().length() + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);
        int treeBlocks = (((dataBlocks * 2) + (ProDOSConstants.BLOCK_SIZE - 2)) / ProDOSConstants.BLOCK_SIZE);
        if (treeBlocks > 1) {
            treeBlocks++;
        }
        for (int i = 1; i < dataBlocks + treeBlocks; i++) {
            new SubNode(i, this);
        }
    }

    @Override
    public void doRefresh() {
    }

    @Override
    public void readBlock(int block, byte[] buffer) throws IOException {
        allocate();
        int dataBlocks = (int) ((getPhysicalFile().length() + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);
        int treeBlocks = (((dataBlocks * 2) + (ProDOSConstants.BLOCK_SIZE - 2)) / ProDOSConstants.BLOCK_SIZE);
        if (treeBlocks > 1) {
            treeBlocks++;
        }
        switch (this.getType()) {
            case SEEDLING:
                readFile(buffer, 0);
                break;
            case SAPLING:
                if (block > 0) {
                    readFile(buffer, (block - 1));
                } else {
                    // Generate seedling index block
                    generateIndex(buffer, 1, dataBlocks + 1);
                }
                break;
            case TREE:
                if (block == 0) {
                    generateIndex(buffer, 1, treeBlocks);
                } else if (block <= treeBlocks) {
                    int start = treeBlocks + ((block - 1) * 256);
                    int end = treeBlocks + dataBlocks;
                    generateIndex(buffer, start, end);
                } else {
                    readFile(buffer, (block - treeBlocks - 1));
                }
                break;
            default:
                // ignore
        }
    }

    // ========================================================================
    // BlockReader interface implementation
    // ========================================================================

    /**
     * Implements BlockReader.readBlock() by adapting to DiskNode.readBlock().
     * This allows FileNode to be used with storage strategies from the core layer.
     *
     * @param blockNumber the block number to read (0-based)
     * @return a 512-byte array containing the block data
     * @throws IOException if an I/O error occurs
     */
    @Override
    public byte[] readBlock(int blockNumber) throws IOException {
        byte[] buffer = new byte[ProDOSConstants.BLOCK_SIZE];
        readBlock(blockNumber, buffer);
        return buffer;
    }

    /**
     * Implements BlockReader.getTotalBlocks().
     * Returns the total number of blocks needed for this file including
     * data blocks and any index blocks (for SAPLING/TREE files).
     *
     * @return the total block count
     */
    @Override
    public int getTotalBlocks() {
        long fileSize = getPhysicalFile().length();
        int dataBlocks = (int) ((fileSize + ProDOSConstants.BLOCK_SIZE - 1) / ProDOSConstants.BLOCK_SIZE);

        EntryType type = getType();
        if (type == EntryType.SEEDLING) {
            return 1;  // Just the data block
        } else if (type == EntryType.SAPLING) {
            return 1 + dataBlocks;  // Index block + data blocks
        } else if (type == EntryType.TREE) {
            int treeBlocks = (((dataBlocks * 2) + (ProDOSConstants.BLOCK_SIZE - 2)) / ProDOSConstants.BLOCK_SIZE);
            if (treeBlocks > 1) {
                treeBlocks++;  // Master index block
            }
            return treeBlocks + dataBlocks;  // Index blocks + data blocks
        }
        return dataBlocks;
    }

    // ========================================================================
    // Private helper methods
    // ========================================================================

    private void readFile(byte[] buffer, int start) throws IOException {
        try (FileInputStream f = new FileInputStream(physicalFile)) {
            f.skip(start * ProDOSConstants.BLOCK_SIZE);
            f.read(buffer, 0, ProDOSConstants.BLOCK_SIZE);
        }
    }

    private void generateIndex(byte[] buffer, int indexStart, int indexLimit) {
        for (int i = indexStart, count = 0; count < 256 && i < indexLimit && i <= additionalNodes.size(); i++, count++) {
            int base = getNodeSequence(i).getBaseBlock();
            buffer[count] = (byte) (base & 0x0ff);
            buffer[count + 256] = (byte) (base >> 8);
        }
    }
}
