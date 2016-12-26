
package org.badvision;

/**
 *
 * @author mhaye
 */

public class Lx47Algorithm
{
    static final int MAX_OFFSET = 2176;  /* range 1..2176 */
    static final int MAX_LEN = 65536;  /* range 2..65536 */

    class Match {
        int index;
        Match next;
    }

    class Optimal {
        int bits;
        int offset;
        int len;
    }

    int elias_gamma_bits(int value) {
        int bits;

        bits = 1;
        while (value > 1) {
            bits += 2;
            value >>= 1;
        }
        return bits;
    }

    int count_bits(int offset, int len) {
        return 1 + (offset > 128 ? 12 : 8) + elias_gamma_bits(len-1);
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
        optimal[0].bits = 8;

        /* process remaining bytes */
        for (i = 1; i < input_data.length; i++) {

            optimal[i].bits = optimal[i-1].bits + 9;
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
                        bits = optimal[i-len].bits + count_bits(offset, len);
                        if (optimal[i].bits > bits) {
                            optimal[i].bits = bits;
                            optimal[i].offset = offset;
                            optimal[i].len = len;
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

    static int nBigLits = 0;
    static int nBigMatches = 0;
    static int nOffsets = 0;
    static int nPrevOffsets = 0;
    static int nPrev2Offsets = 0;

    public class Lx47Writer
    {
        public byte[] buf;
        public int outPos;
        private int mask;
        private int bitIndex;

        Lx47Writer(int uncompLen) {
            buf = new byte[uncompLen*2];
            mask = 0;
            outPos = 0;
        }

        void writeByte(int value) {
            buf[outPos++] = (byte)(value & 0xFF);
        }

        void writeBit(int value) {
            if (mask == 0) {
                mask = 128;
                bitIndex = outPos;
                writeByte(0);
            }
            if (value > 0)
                buf[bitIndex] |= mask;
            mask >>= 1;
        }

        void writeEliasGamma(int value) {
            assert value > 0 && value <= 65535;
            int i;
            for (i = 2; i <= value; i <<= 1)
                writeBit(0);
            while ((i >>= 1) > 0)
                writeBit(value & i);
        }

        void writeEliasExpGamma(int value, int exp) {
            assert value > 0;
            writeEliasGamma(((value-1) >> exp) + 1);
            for (int i=exp-1; i>=0; i--)
                writeBit(value & (1<<i));
        }

        void writeEliasDelta(int num) {
            int len = 0;
            int lengthOfLen = 0;
            for (int temp = num; temp > 0; temp >>= 1)  // calculate 1+floor(log2(num))
                len++;
            for (int temp = len; temp > 1; temp >>= 1)  // calculate floor(log2(len))
                lengthOfLen++;
            for (int i = lengthOfLen; i > 0; --i)
                writeBit(0);
            for (int i = lengthOfLen; i >= 0; --i)
                writeBit((len >> i) & 1);
            for (int i = len-2; i >= 0; i--)
                writeBit((num >> i) & 1);
        }

        void writeEliasOmega(int num) {
            int[] bits = new int[32];
            int nBits = 0;
            while (num > 1) {
                int len = 0;
                for (int temp = num; temp > 0; temp >>= 1)  // calculate 1+floor(log2(num))
                    len++;
                for (int i = 0; i < len; i++)
                    bits[nBits++] = (num >> i) & 1;
                num = len - 1;
            }
            while (nBits > 0)
                writeBit(bits[--nBits]);
            writeBit(0);
        }

        void write4bit(int num) {
            if (num < 15) {
                writeBit(num & 8); writeBit(num & 4); writeBit(num & 2); writeBit(num & 1);
            }
            else {
                writeBit(1); writeBit(1); writeBit(1); writeBit(1);
                num -= 15;
                while (num >= 128) {
                    writeByte((num & 127) | 128);
                    num -= 128;
                }
                writeByte(num);
            }
        }

        int[] fib = new int[30];

        // Stores values in fib and returns index of the largest
        // fibonacci number smaller than n. 
        int largestFiboLessOrEqual(int n)
        {
            fib[0] = 1;  // Fib[0] stores 2nd Fibonacci No.
            fib[1] = 2;  // Fib[1] stores 3rd Fibonacci No.
         
            // Keep Generating remaining numbers while previously
            // generated number is smaller
            int i;
            for (i=2; fib[i-1]<=n; i++)
                fib[i] = fib[i-1] + fib[i-2];
         
            // Return index of the largest fibonacci number
            // smaller than or equal to n. Note that the above
            // loop stopped when fib[i-1] became larger.
            return (i-2);
        }
 
        /* Returns pointer to the char string which corresponds to
           code for n */
        void writeFib(int n)
        {
            int index = largestFiboLessOrEqual(n);
         
            int[] codeword = new int[index+2];
         
            // index of the largest Fibonacci f <= n
            int i = index;
         
            while (n != 0)
            {
                // Mark usage of Fibonacci f (1 bit)
                codeword[i] = 1;
         
                // Subtract f from n
                n = n - fib[i];
         
                // Move to Fibonacci just smaller than f
                i = i - 1;
         
                // Mark all Fibonacci > n as not used (0 bit), 
                // progress backwards
                while (i>=0 && fib[i]>n)
                {
                    codeword[i] = 0;
                    i = i - 1;
                }
            }
         
            //additional '1' bit
            codeword[index+1] = 1;
         
            for (i=0; i<=index+1; i++)
                writeBit(codeword[i]);
        }        

        void writeLiteralLen(int value) {
            writeEliasExpGamma(value, 1);
            while (value > 255) {
                nBigLits++;
                value -= 255;
            }
        }

        void writeMatchLen(int value) {
            writeEliasExpGamma(value, 1);
            while (value > 255) {
                nBigMatches++;
                value -= 255;
            }
        }

        void write2byte(int offset) {
            assert offset >= 0 && offset <= 65535;
            if (offset < 128)
                writeByte(offset);
            else {
                offset -= 128;
                writeByte((offset & 127) | 128);
                writeEliasExpGamma((offset >> 7) + 1, 1);
            }
        }

        int prevOff = -1;
        int prevOff2 = -1;

        void writeOffset(int offset) {
            write2byte(offset-1);
            if (offset >= 126) {
                ++nOffsets;
                if (offset == prevOff)
                    ++nPrevOffsets;
                else if (offset == prevOff2)
                    ++nPrev2Offsets;
                prevOff2 = prevOff;
                prevOff = offset;
            }
            //writeEliasExpGamma(offset, 7) // same; other values worse
        }
    }
    
    byte[] compressOptimal(Optimal[] optimal, byte[] input_data) {
        int input_index;
        int input_prev;
        int offset1;
        int mask;
        int i;

        /* calculate and allocate output buffer */
        input_index = input_data.length-1;
        int output_size = (optimal[input_index].bits+18+7)/8;
        byte[] output_data = new byte[output_size];

        /* un-reverse optimal sequence */
        optimal[input_index].bits = 0;
        while (input_index > 0) {
            input_prev = input_index - (optimal[input_index].len > 0 ? optimal[input_index].len : 1);
            optimal[input_prev].bits = input_index;
            input_index = input_prev;
        }

        Lx47Writer w = new Lx47Writer(input_data.length);

        /* first byte is always literal */
        w.writeByte(input_data[0]);

        /* process remaining bytes */
        while ((input_index = optimal[input_index].bits) > 0) {
            if (optimal[input_index].len == 0) {

                /* literal indicator */
                w.writeBit(0);

                /* literal value */
                w.writeByte(input_data[input_index]);

            } else {

                /* sequence indicator */
                w.writeBit(1);

                /* sequence length */
                w.writeEliasGamma(optimal[input_index].len-1);

                /* sequence offset */
                offset1 = optimal[input_index].offset-1;
                if (offset1 < 128) {
                    w.writeByte(offset1);
                } else {
                    offset1 -= 128;
                    w.writeByte((offset1 & 127) | 128);
                    for (mask = 1024; mask > 127; mask >>= 1) {
                        w.writeBit(offset1 & mask);
                    }
                }
            }
        }

        /* sequence indicator */
        w.writeBit(1);

        /* end marker > MAX_LEN */
        for (i = 0; i < 16; i++)
            w.writeBit(0);
        w.writeBit(1);

        assert w.outPos == output_size : String.format("size miscalc: got %d, want %d", w.outPos, output_size);
        System.out.println(String.format("bufLen=%d outlen=%d outSize=%d outPos=%d", w.buf.length, output_data.length, output_size, w.outPos));
        System.arraycopy(w.buf, 0, output_data, 0, w.outPos);
                
        return output_data;
    }
    
    public byte[] compress(byte[] input_data) {
        return compressOptimal(optimize(input_data), input_data);
    }
}
