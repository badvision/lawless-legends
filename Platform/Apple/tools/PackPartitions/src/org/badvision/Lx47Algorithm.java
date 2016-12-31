
package org.badvision;

import java.util.LinkedList;
import java.util.Arrays;

/**
 *
 * @author mhaye
 */

public class Lx47Algorithm
{
    static final int MAX_OFFSET = 16384;  /* range 1..65536 */
    static final int MAX_LEN = 256;  /* range 2..65536 */
    static final int OFFSET_EXP_BITS = 6;
    
    LinkedList<String> debugs = new LinkedList<String>();
    
    void addDebug(String format, Object... arguments) {
        String str = String.format(format, arguments);
        System.out.println("Gen: " + str);
        debugs.add(str);
    }

    class Match {
        int index;
        Match next;
    }

    class Optimal {
        int bits;
        int offset;
        int len;
        int lits;
        int next;
    }

    int countEliasGammaBits(int value) {
        int bits;
        assert value >= 1 && value <= 255;

        bits = 1;
        while (value > 1) {
            bits += 2;
            value >>= 1;
        }
        return bits;
    }
    
    int countEliasExpGammaBits(int value, int exp) {
        return (exp==0) ? countEliasGammaBits(value) : (countEliasGammaBits((value >> exp) + 1) + exp);
    }
    
    int countCodePair(int prevLits, int matchLen, int offset) {
        int nBits = (prevLits>0 ? 0 : 1) 
                    + 8 // 8 for the byte that's always emitted
                    + (offset>=64 ? countEliasGammaBits(offset>>6) : 0)
                    + (matchLen>2 ? countEliasGammaBits(matchLen-2) : 0);
        return nBits;
    }
    
    int countLitBits(int lits) {
        if (lits == 0)
            return 0;
        int bits = lits * 8;
        while (lits > 0) {
            int n = Math.min(254, lits);
            bits += countEliasGammaBits(n+1);
            lits -= n;
        }
        return bits;
    }
    
    Optimal[] optimize(byte[] input_data) {
        int[] min = new int[MAX_OFFSET+1];
        int[] max = new int[MAX_OFFSET+1];
        Match[] matches = new Match[256*256];
        Match[] match_slots = new Match[input_data.length];
        Optimal[] optimal = new Optimal[input_data.length];
        Match match;
        int match_index;
        int offset;
        int len;
        int best_len;
        int bits;
        int i;
        
        for (i=0; i<256*256; i++)
            matches[i] = new Match();
        for (i=0; i<input_data.length; i++) {
            match_slots[i] = new Match();
            optimal[i] = new Optimal();
        }

        /* first byte is always literal */
        optimal[0].lits = 1;
        optimal[0].bits = countLitBits(1);

        /* process remaining bytes */
        for (i = 1; i < input_data.length; i++) {

            // Start by assuming that we'll extend the previous literal string
            // (or start a new one if prev was a match sequence).
            optimal[i].lits = optimal[i-1].lits + 1;
            optimal[i].bits = optimal[i-1].bits 
                            - countLitBits(optimal[i-1].lits) 
                            + countLitBits(optimal[i].lits);
            
            // Now search for a match that's better than just using a literal.
            match_index = (input_data[i-1] & 0xFF) << 8 | (input_data[i] & 0xFF);
            best_len = 1;
            for (match = matches[match_index]; match.next != null && best_len < MAX_LEN; match = match.next) {
                offset = i - match.next.index;
                if (offset > MAX_OFFSET) {
                    match.next = null;
                    break;
                }

                for (len = 2; len <= MAX_LEN; len++) {
                    if (len > best_len) {
                        best_len = len;
                        bits = optimal[i-len].bits + countCodePair(optimal[i-len].lits, len, offset);
                        if (optimal[i].bits > bits) {
                            optimal[i].bits = bits;
                            optimal[i].offset = offset;
                            optimal[i].len = len;
                            optimal[i].lits = 0;
                        }
                    } else if (i+1 == max[offset]+len && max[offset] != 0) {
                        len = i-min[offset];
                        if (len > best_len) {
                            len = best_len;
                        }
                    }
                    if (i < offset+len || input_data[i-len] != input_data[i-len-offset]) {
                        break;
                    }
                }
                min[offset] = i+1-len;
                max[offset] = i;
            }
            match_slots[i].index = i;
            match_slots[i].next = matches[match_index].next;
            matches[match_index].next = match_slots[i];
        }

        return optimal;
    }

    static int nOffsets = 0;
    static int nPrevOffsets = 0;
    static int nPrev2Offsets = 0;

    public class Lx47Writer
    {
        public byte[] buf;
        public int bitPos;
        public int outPos;
        private int mask;
        private int bitIndex;

        Lx47Writer(int uncompLen) {
            buf = new byte[uncompLen*2];
            mask = 0;
            outPos = 0;
            bitPos = 0;
        }

        void writeByte(int value) {
            buf[outPos++] = (byte)(value & 0xFF);
            bitPos += 8;
        }

        void writeBit(int value) {
            if (mask == 0) {
                mask = 128;
                bitIndex = outPos;
                writeByte(0);
                bitPos -= 8;
            }
            if (value > 0)
                buf[bitIndex] |= mask;
            mask >>= 1;
            bitPos++;
        }

        void writeEliasGamma(int value) {
            assert value > 0 && value <= 65535;
            int i;
            for (i = 2; i <= value; i <<= 1)
                writeBit(0);
            while ((i >>= 1) > 0)
                writeBit(value & i);
        }

        void writeLiteralLen(int value) {
            writeEliasGamma(value+1);
        }

        void writeCodePair(int matchLen, int offset) 
        {
            int data = offset & 63; // 6 bits

            if (offset >= 64)
                data |= 64;
            assert matchLen >= 2;
            if (matchLen > 2)
                data |= 128;

            writeByte(data);
            
            if (offset >= 64)
                writeEliasGamma(offset>>6);
            if (matchLen > 2)
                writeEliasGamma(matchLen-2);
        }
    }

    byte[] compressOptimal(Optimal[] optimal, byte[] input_data) 
    {
        int input_index;
        int input_prev;
        int i;
        
        //for (i=0; i<optimal.length; i++)
        //    System.out.format("opt[%d]: bits=%d off=%d len=%d lits=%d\n", i, 
        //            optimal[i].bits, optimal[i].offset, optimal[i].len, optimal[i].lits);

        /* calculate and allocate output buffer */
        input_index = input_data.length-1;
        int output_bits = optimal[input_index].bits;
        if (optimal[input_index].lits == 0)
            output_bits++; // zero-length lit str at end
        int output_size = (output_bits+7)/8;
        byte[] output_data = new byte[output_size];

        /* un-reverse optimal sequence */
        int first_index = -1;
        optimal[input_index].next = -1;
        while (input_index >= 0) {
            input_prev = input_index - (optimal[input_index].len > 0 ? optimal[input_index].len : optimal[input_index].lits);
            if (input_prev >= 0)
                optimal[input_prev].next = input_index;
            else
                first_index = input_index;
            input_index = input_prev;
        }

        Lx47Writer w = new Lx47Writer(input_data.length);

        /* process all bytes */
        boolean prevIsLit = false;
        addDebug("start");
        for (input_index = first_index; input_index >= 0; input_index = optimal[input_index].next)
        {
            if (optimal[input_index].len == 0) {

                // Literal string
                int pos = input_index - optimal[input_index].lits + 1;
                while (optimal[input_index].lits > 0) {
                    int n = Math.min(254, optimal[input_index].lits);
                    addDebug("lits l=%d", n);
                    w.writeLiteralLen(n);
                    for (i = 0; i < n; i++, pos++) {
                        addDebug("lit $%x", input_data[pos]);
                        w.writeByte(input_data[pos]);
                    }
                    optimal[input_index].lits -= n;
                }
                prevIsLit = true;

            } else {

                // Sequence. If two in a row, insert a zero-length lit str
                if (!prevIsLit) {
                    addDebug("lits l=0");
                    w.writeLiteralLen(0);
                }
                
                // Now write sequence info
                addDebug("seq l=%d o=%d", optimal[input_index].len, optimal[input_index].offset);
                w.writeCodePair(optimal[input_index].len, optimal[input_index].offset);
                prevIsLit = false;
            }
            
            assert optimal[input_index].bits == w.bitPos :
                   String.format("pos miscalc: calc'd %d, got %d", optimal[input_index].bits, w.bitPos);
        }

        // EOF marker
        if (!prevIsLit) {
            addDebug("lits l=0");
            w.writeLiteralLen(0);
        }
        addDebug("EOF");

        assert w.outPos == output_size : String.format("size miscalc: got %d, want %d", w.outPos, output_size);
        System.arraycopy(w.buf, 0, output_data, 0, w.outPos);
                
        return output_data;
    }
    
    public class Lx47Reader
    {
        public byte[] buf;
        public int inPos;
        private int indexByte;
        private int mask;

        Lx47Reader(byte[] inBuf, int inStart) {
            buf = inBuf;
            mask = 0;
            inPos = inStart;
        }

        int readByte() {
            return buf[inPos++] & 0xFF;
        }

        int readBit() {
            if (mask == 0) {
                mask = 128;
                indexByte = readByte();
            }
            int ret = ((indexByte & mask) != 0) ? 1 : 0;
            mask >>= 1;
            return ret;
        }

        int readEliasGamma() {
            int nBits = 0;
            while (readBit() == 0)
                ++nBits;
            if (nBits >= 16)
                return -99; // EOF marker
            int out = 1;
            while (nBits-- > 0)
                out = (out << 1) | readBit();
            return out;
        }
        
        int readEliasExpGamma(int exp) {
            int val = readEliasGamma();
            if (exp == 0)
                return val;
            if (val < 0)
                return val;
            val = (val-1) << exp;
            for (int i=exp-1; i>=0; i--) {
                int bit = readBit();
                if (bit > 0)
                    val |= (1<<i);
            }
            return val;
        }
        
        int readLiteralLen() {
            return readEliasGamma() - 1;
        }

        int readCodePair()
        {
            int data = readByte();
            int offset = data & 63; // 6 bits
            int matchLen = 2;
            if ((data & 64) == 64)
                offset |= readEliasGamma() << 6;
            if ((data & 128) == 128)
                matchLen += readEliasGamma();
            return matchLen | (offset<<16);
        }
    }
    
    int debugPos = 0;
    void chkDebug(String format, Object... arguments) {
        String toCheck = String.format(format, arguments);
        String expect = debugs.removeFirst();
        assert expect.equals(toCheck) : 
            String.format("Expecting '%s', got '%s'", expect, toCheck);
        System.out.format("OK [%d]: %s\n", debugs.size(), expect);
    }

    public void decompress(byte[] input_data, int inStart, byte[] output_data, int outStart, int outLen)
    {
        int len;
        Lx47Reader r = new Lx47Reader(input_data, inStart);
        int outPos = outStart;
        
        // Now decompress until done.
        chkDebug("start");
        while (true) 
        {
            // Check for literal string
            while (true) {
                len = r.readLiteralLen();
                chkDebug("lits l=%d", len);
                for (int i=0; i<len; i++) {
                    output_data[outPos++] = (byte) r.readByte();
                    chkDebug("lit $%x", output_data[outPos-1]);
                }
                if (len != 254)
                    break;
            }
            
            // Check for EOF at the end of each literal string
            if (outPos == outStart+outLen)
                break;
            
            // Not a literal, so it's a sequence. Get len, offset, and copy.
            int codePair = r.readCodePair();
            len = codePair & 0xFFFF;
            int off = codePair >> 16;
            chkDebug("seq l=%d o=%d", len, off);
            while (len-- > 0) {
                output_data[outPos] = output_data[outPos - off];
                ++outPos;
            }
        }
        
        chkDebug("EOF");
    }
    
    public byte[] compress(byte[] input_data) {
        if (true) {
            input_data = "aaaaaaaaa".getBytes();
            byte[] testComp = compressOptimal(optimize(input_data), input_data);
            byte[] testDecomp = new byte[input_data.length];
            decompress(testComp, 0, testDecomp, 0, input_data.length);
            assert Arrays.equals(input_data, testDecomp);
            System.out.println("Good!");
            System.exit(1);
        }
        return compressOptimal(optimize(input_data), input_data);
    }
}
