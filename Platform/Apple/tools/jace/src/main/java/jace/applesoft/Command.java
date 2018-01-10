/*
 * Copyright (C) 2012 Brendan Robert (BLuRry) brendan.robert@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jace.applesoft;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A command is a list of parts, either raw bytes (ascii text) or tokens. When
 * put together they represent a single basic statement.
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com
 */
public class Command {

    public static enum TOKEN {

        END((byte) 0x080, "END"),
        FOR((byte) 0x081, "FOR"),
        NEXT((byte) 0x082, "NEXT"),
        DATA((byte) 0x083, "DATA"),
        INPUT((byte) 0x084, "INPUT"),
        DEL((byte) 0x085, "DEL"),
        DIM((byte) 0x086, "DIM"),
        READ((byte) 0x087, "READ"),
        GR((byte) 0x088, "GR"),
        TEXT((byte) 0x089, "TEXT"),
        PR((byte) 0x08A, "PR#"),
        IN((byte) 0x08B, "IN#"),
        CALL((byte) 0x08C, "CALL"),
        PLOT((byte) 0x08D, "PLOT"),
        HLIN((byte) 0x08E, "HLIN"),
        VLIN((byte) 0x08F, "VLIN"),
        HGR2((byte) 0x090, "HGR2"),
        HGR((byte) 0x091, "HGR"),
        HCOLOR((byte) 0x092, "HCOLOR="),
        HPLOT((byte) 0x093, "HPLOT"),
        DRAW((byte) 0x094, "DRAW"),
        XDRAW((byte) 0x095, "XDRAW"),
        HTAB((byte) 0x096, "HTAB"),
        HOME((byte) 0x097, "HOME"),
        ROT((byte) 0x098, "ROT="),
        SCALE((byte) 0x099, "SCALE="),
        SHLOAD((byte) 0x09A, "SHLOAD"),
        TRACE((byte) 0x09B, "TRACE"),
        NOTRACE((byte) 0x09C, "NOTRACE"),
        NORMAL((byte) 0x09D, "NORMAL"),
        INVERSE((byte) 0x09E, "INVERSE"),
        FLASH((byte) 0x09F, "FLASH"),
        COLOR((byte) 0x0A0, "COLOR="),
        POP((byte) 0x0A1, "POP"),
        VTAB((byte) 0x0A2, "VTAB"),
        HIMEM((byte) 0x0A3, "HIMEM:"),
        LOMEM((byte) 0x0A4, "LOMEM:"),
        ONERR((byte) 0x0A5, "ONERR"),
        RESUME((byte) 0x0A6, "RESUME"),
        RECALL((byte) 0x0A7, "RECALL"),
        STORE((byte) 0x0A8, "STORE"),
        SPEED((byte) 0x0A9, "SPEED="),
        LET((byte) 0x0AA, "LET"),
        GOTO((byte) 0x0AB, "GOTO"),
        RUN((byte) 0x0AC, "RUN"),
        IF((byte) 0x0AD, "IF"),
        RESTORE((byte) 0x0AE, "RESTORE"),
        AMPERSAND((byte) 0x0AF, "&"),
        GOSUB((byte) 0x0B0, "GOSUB"),
        RETURN((byte) 0x0B1, "RETURN"),
        REM((byte) 0x0B2, "REM"),
        STOP((byte) 0x0B3, "STOP"),
        ONGOTO((byte) 0x0B4, "ON"),
        WAIT((byte) 0x0B5, "WAIT"),
        LOAD((byte) 0x0B6, "LOAD"),
        SAVE((byte) 0x0B7, "SAVE"),
        DEF((byte) 0x0B8, "DEF"),
        POKE((byte) 0x0B9, "POKE"),
        PRINT((byte) 0x0BA, "PRINT"),
        CONT((byte) 0x0BB, "CONT"),
        LIST((byte) 0x0BC, "LIST"),
        CLEAR((byte) 0x0BD, "CLEAR"),
        GET((byte) 0x0BE, "GET"),
        NEW((byte) 0x0BF, "NEW"),
        TAB((byte) 0x0C0, "TAB("),
        TO((byte) 0x0C1, "TO"),
        FN((byte) 0x0C2, "FN"),
        SPC((byte) 0x0c3, "SPC"),
        THEN((byte) 0x0c4, "THEN"),
        AT((byte) 0x0c5, "AT"),
        NOT((byte) 0x0c6, "NOT"),
        STEP((byte) 0x0c7, "STEP"),
        PLUS((byte) 0x0c8, "+"),
        MINUS((byte) 0x0c9, "-"),
        MULTIPLY((byte) 0x0Ca, "*"),
        DIVIDE((byte) 0x0Cb, "/"),
        POWER((byte) 0x0Cc, "^"),
        AND((byte) 0x0Cd, "AND"),
        OR((byte) 0x0Ce, "OR"),
        GREATER((byte) 0x0CF, ">"),
        EQUAL((byte) 0x0d0, "="),
        LESS((byte) 0x0d1, "<"),
        SGN((byte) 0x0D2, "SGN"),
        INT((byte) 0x0D3, "INT"),
        ABS((byte) 0x0D4, "ABS"),
        USR((byte) 0x0D5, "USR"),
        FRE((byte) 0x0D6, "FRE"),
        SCREEN((byte) 0x0D7, "SCRN("),
        PDL((byte) 0x0D8, "PDL"),
        POS((byte) 0x0D9, "POS"),
        SQR((byte) 0x0DA, "SQR"),
        RND((byte) 0x0DB, "RND"),
        LOG((byte) 0x0DC, "LOG"),
        EXP((byte) 0x0DD, "EXP"),
        COS((byte) 0x0DE, "COS"),
        SIN((byte) 0x0DF, "SIN"),
        TAN((byte) 0x0E0, "TAN"),
        ATN((byte) 0x0E1, "ATN"),
        PEEK((byte) 0x0E2, "PEEK"),
        LEN((byte) 0x0E3, "LEN"),
        STR((byte) 0x0E4, "STR$"),
        VAL((byte) 0x0E5, "VAL"),
        ASC((byte) 0x0E6, "ASC"),
        CHR((byte) 0x0E7, "CHR$"),
        LEFT((byte) 0x0E8, "LEFT$"),
        RIGHT((byte) 0x0E9, "RIGHT$"),
        MID((byte) 0x0EA, "MID$");

        static TOKEN findMatch(String search, int start) {
            for (TOKEN t : values()) {
                int i = start;
                boolean found = true;
                for (int j = 0; j < t.toString().length(); j++) {
                    while (i+j < search.length() && search.charAt(j + i) == ' ') {
                        i++;
                    }
                    if (i + j >= search.length()
                            || (search.charAt(i + j) != t.toString().charAt(j))) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return t;
                }
            }
            return null;
        }
        private String str;
        public byte code;

        TOKEN(byte b, String str) {
            this.code = b;
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        public static TOKEN fromByte(byte b) {
            for (TOKEN t : values()) {
                if (t.code == b) {
                    return t;
                }
            }
            return null;
        }
    }

    public static class ByteOrToken {

        byte b;
        TOKEN t;
        boolean isToken = false;

        public ByteOrToken(byte b) {
            TOKEN t = TOKEN.fromByte(b);
            if (t != null) {
                isToken = true;
                this.t = t;
            } else {
                isToken = false;
                this.b = b;
            }

        }

        public ByteOrToken(TOKEN token) {
            isToken = true;
            this.t = token;
        }

        @Override
        public String toString() {
            return isToken
                    ? " " + t.toString() + " "
                    : String.valueOf((char) b);
        }

        public byte getByte() {
            if (isToken) {
                return t.code;
            } else {
                return b;
            }
        }
    }
    List<ByteOrToken> parts = new ArrayList<>();

    @Override
    public String toString() {
        return parts.stream().map(ByteOrToken::toString).collect(Collectors.joining());
    }
}