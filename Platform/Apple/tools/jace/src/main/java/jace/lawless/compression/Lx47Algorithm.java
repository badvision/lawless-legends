package jace.lawless.compression;

/**
 * Lx47 decompression algorithm for Lawless Legends compressed data.
 * This is a simplified version containing only the decompression logic.
 *
 * Based on the original Lx47Algorithm.java by mhaye.
 */
public class Lx47Algorithm {

    /**
     * Reader for decompressing Lx47-compressed data.
     */
    public static class Lx47Reader {
        private final byte[] buf;
        private int inPos;
        private final int inStart;
        private int mask;
        private int indexByte;
        private int indexPos;

        public Lx47Reader(byte[] inBuf, int inStart) {
            this.inStart = inStart;
            this.buf = inBuf;
            this.mask = 0;
            this.inPos = inStart;
        }

        public int getInPos() {
            return inPos;
        }

        int readByte() {
            return buf[inPos++] & 0xFF;
        }

        int readBit() {
            if (mask == 0) {
                mask = 128;
                indexPos = inPos - inStart;
                indexByte = readByte();
            }
            int ret = ((indexByte & mask) != 0) ? 1 : 0;
            mask >>= 1;
            return ret;
        }

        int readGamma() {
            int out = 1;
            while (readBit() == 0) {
                out = (out << 1) | readBit();
            }
            return out;
        }

        int readLiteralLen() {
            if (readBit() == 0) {
                return 0;
            } else {
                return readGamma();
            }
        }

        int readCodePair() {
            int data = readByte();
            int offset = data & 63; // 6 bits
            int matchLen = 2;
            if ((data & 64) == 64) {
                offset |= readGamma() << 6;
            }
            offset++; // important
            if ((data & 128) == 128) {
                matchLen += readGamma();
            }
            return matchLen | (offset << 16); // pack both vals into a single int
        }
    }

    /**
     * Decompress Lx47-compressed data.
     *
     * @param input_data Input buffer containing compressed data
     * @param inStart Starting offset in input buffer
     * @param output_data Output buffer for decompressed data
     * @param outStart Starting offset in output buffer
     * @param outLen Expected length of decompressed data
     */
    public static void decompress(byte[] input_data, int inStart, byte[] output_data, int outStart, int outLen) {
        int len;
        Lx47Reader r = new Lx47Reader(input_data, inStart);
        int outPos = outStart;

        // Decompress until done.
        while (true) {
            // Check for literal string
            while (true) {
                len = r.readLiteralLen();
                for (int i = 0; i < len; i++) {
                    output_data[outPos++] = (byte) r.readByte();
                }
                if (len != 255) {
                    break;
                }
            }

            // Check for EOF at the end of each literal string
            if (outPos == outStart + outLen) {
                break;
            }

            // Not a literal, so it's a sequence. Get len, offset, and copy.
            int codePair = r.readCodePair();
            len = codePair & 0xFFFF;
            int off = codePair >> 16;
            while (len-- > 0) {
                output_data[outPos] = output_data[outPos - off];
                ++outPos;
            }
        }
    }
}
