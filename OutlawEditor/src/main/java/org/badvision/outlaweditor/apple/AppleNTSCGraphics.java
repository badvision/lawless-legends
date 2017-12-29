/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor.apple;

import org.badvision.outlaweditor.data.xml.PlatformData;

/**
 *
 * @author brobert
 */
public class AppleNTSCGraphics {

    // i Range [-0.5957, 0.5957]
    public static final double MAX_I = 0.5957;

    // q Range [-0.5226, 0.5226]
    public static final double MAX_Q = 0.5226;
    public static final double MAX_Y = 1;
    // y Range [0,1]
    public static final double MIN_Y = 0;

    public static int byteDoubler(byte b) {
        int num = ((b & 64) << 6) | ((b & 32) << 5) | ((b & 16) << 4) | ((b & 8) << 3) | ((b & 4) << 2) | ((b & 2) << 1) | (b & 1);
        return num | (num << 1);
    }

    public static double normalize(double x, double minX, double maxX) {
        if (x < minX) {
            return minX;
        }
        if (x > maxX) {
            return maxX;
        }
        return x;
    }

    public static int yiqToRgb(double y, double i, double q) {
        int r = (int) (normalize(y + 0.956 * i + 0.621 * q, 0, 1) * 255);
        int g = (int) (normalize(y - 0.272 * i - 0.647 * q, 0, 1) * 255);
        int b = (int) (normalize(y - 1.105 * i + 1.702 * q, 0, 1) * 255);
        return (r << 16) | (g << 8) | b;
    }

    public static int getRed(int col) {
        return ((col >> 16) & 255);
    }

    public static int getGreen(int col) {
        return ((col >> 8) & 255);
    }

    public static int getBlue(int col) {
        return (col & 255);
    }
    public static int[][] hgrToDhgr;
    public static int[][] textPalette = new int[4][128];
    public static int[][] hgrToDhgrBW;
    public static int[][] solidPalette = new int[4][128];

    public static void initPalettes() {
        double[][] yiq = {
            {0.0, 0.0, 0.0},
            {0.25, 0.5, 0.5},
            {0.25, -0.5, 0.5},
            {0.5, 0.0, 1.0},
            {0.25, -0.5, -0.5},
            {0.5, 0.0, 0.0},
            {0.5, -1.0, 0.0},
            {0.75, -0.5, 0.5},
            {0.25, 0.5, -0.5},
            {0.5, 1.0, 0.0},
            {0.5, 0.0, 0.0},
            {0.75, 0.5, 0.5},
            {0.5, 0.0, -1.0},
            {0.75, 0.5, -0.5},
            {0.75, -0.5, -0.5},
            {1.0, 0.0, 0.0}};

        int maxLevel = 10;
        for (int offset = 0; offset < 4; offset++) {
            for (int pattern = 0; pattern < 128; pattern++) {
                int level = (pattern & 1) + ((pattern >> 1) & 1) * 1 + ((pattern >> 2) & 1) * 2 + ((pattern >> 3) & 1) * 4 + ((pattern >> 4) & 1) * 2 + ((pattern >> 5) & 1) * 1;
                int col = (pattern >> 2) & 15;
                for (int rot = 0; rot < offset; rot++) {
                    col = ((col & 8) >> 3) | ((col << 1) & 15);
                }
                double y1 = yiq[col][0];
                double y2 = (double) level / (double) maxLevel;
                solidPalette[offset][pattern] = (255 << 24) | yiqToRgb(y1, yiq[col][1] * MAX_I, yiq[col][2] * MAX_Q);
                textPalette[offset][pattern] = (255 << 24) | yiqToRgb(y2, yiq[col][1] * MAX_I, yiq[col][2] * MAX_Q);
            }
        }
        hgrToDhgr = new int[512][256];
        hgrToDhgrBW = new int[256][256];
        for (int bb1 = 0; bb1 < 512; bb1++) {
            for (int bb2 = 0; bb2 < 256; bb2++) {
                int value = ((bb1 & 385) >= 257) ? 1 : 0;
                int b1 = byteDoubler((byte) (bb1 & 127));
                if ((bb1 & 128) != 0) {
                    b1 <<= 1;
                }
                int b2 = byteDoubler((byte) (bb2 & 127));
                if ((bb2 & 128) != 0) {
                    b2 <<= 1;
                }
                if ((bb1 & 64) == 64 && (bb2 & 1) != 0) {
                    b2 |= 1;
                }
                value |= b1 | (b2 << 14);
                if ((bb2 & 64) != 0) {
                    value |= 268435456;
                }
                hgrToDhgr[bb1][bb2] = value;
                hgrToDhgrBW[bb1 & 255][bb2] = byteDoubler((byte) bb1) | (byteDoubler((byte) bb2) << 14);
            }
        }
    }

    static public int calculateHiresOffset(int y) {
        return calculateTextOffset(y >> 3) + ((y & 7) << 10);
    }

    static public int calculateTextOffset(int y) {
        return ((y & 7) << 7) + 40 * (y >> 3);
    }

    static {
        initPalettes();
    }

    public static String generateHgrMonitorListing(PlatformData data) {
        StringBuilder listing = new StringBuilder();
        for (int y = 0; y < data.getHeight(); y++) {
            int pos = calculateHiresOffset(y) + 0x02000;
            listing.append("\n").append(Integer.toHexString(pos)).append(":");
            for (int x = 0; x < data.getWidth(); x++) {
                int val = data.getValue()[data.getWidth() * y + x] & 0x0ff;
                listing.append(Integer.toHexString(val)).append(" ");
            }
        }
        listing.append("\n");
        return listing.toString();
    }

    static byte[] getAppleHGRBinary(PlatformData platformData) {
        boolean dhgr = platformData.getWidth() > 40;
        byte[] output = new byte[dhgr ? 0x04000 : 0x02000];
        int counter = 0;
        for (int y = 0; y < platformData.getHeight(); y++) {
            int offset = calculateHiresOffset(y);
            if (dhgr) {
                for (int x = 0; x < 40; x++) {
                    output[0x02000 + offset + x] = platformData.getValue()[counter++];
                    output[offset + x] = platformData.getValue()[counter++];
                }                
            } else {
                for (int x = 0; x < platformData.getWidth(); x++) {
                    output[offset + x] = platformData.getValue()[counter++];
                }
            }
        }
        return output;
    }

    public static Object generateDhgrMonitorListing(PlatformData data) {
        StringBuilder listing = new StringBuilder();
        for (int y = 0; y < data.getHeight(); y++) {
            int pos = calculateHiresOffset(y) + 0x02000;
            listing.append("\n").append(Integer.toHexString(pos)).append(":");
            for (int x = 0; x < data.getWidth(); x+=2) {
                int val = data.getValue()[data.getWidth() * y + x] & 0x0ff;
                listing.append(Integer.toHexString(val)).append(" ");
            }
        }
        for (int y = 0; y < data.getHeight(); y++) {
            int pos = calculateHiresOffset(y) + 0x04000;
            listing.append("\n").append(Integer.toHexString(pos)).append(":");
            for (int x = 1; x < data.getWidth(); x+=2) {
                int val = data.getValue()[data.getWidth() * y + x] & 0x0ff;
                listing.append(Integer.toHexString(val)).append(" ");
            }
        }
        listing.append("\n");
        return listing.toString();
    }
}
