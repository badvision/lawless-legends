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

import java.util.ArrayList;
import java.util.List;

public abstract class Palette {

    public int MATCH_TOLERANCE = 0;
    List colors = null;
    public static int COLOR_DISTANCE_MAX = 0x2f708fd;

    public Palette() {
        MATCH_TOLERANCE = 64;
        colors = new ArrayList();
        initPalette();
    }

    protected abstract void initPalette();

    public int[] getColor(int col) {
        return (int[]) colors.get(col);
    }

    public int getColorInt(int c) {
        int col[] = getColor(c);
        return toRGBInt(col);
    }

    public void addColor(int col[]) {
        /*  45*/ colors.add(col);
    }

    public void addColor(int r, int g, int b) {
        int col[] = new int[3];
        col[0] = r;
        col[1] = g;
        col[2] = b;
        addColor(col);
    }

    public int findColor(int color) {
        int col[] = parseIntColor(color);
        return findColor(col);
    }

    public static int[] parseIntColor(int color) {
        return new int[]{getR(color), getG(color), getB(color)};
    }

    public static int toRGBInt(int[] col) {
        return 0x10000 * col[0] + 256 * col[1] + col[2];
    }

    public int findColor(int color[]) {
        double lastDiff = COLOR_DISTANCE_MAX;
        int bestFit = 0;
        for (int i = 0; i < colors.size(); i++) {
            int test[] = (int[]) colors.get(i);
            double diff = distance(color, test);
            if (diff < lastDiff) {
                lastDiff = diff;
                bestFit = i;
            }
        }

        return bestFit;
    }

    public static double distance(int c1[], int c2[]) {
        double rmean = ( c1[0] + c2[0] ) / 512.0;
        double r = c1[0] - c2[0];
        double g = c1[1] - c2[1];
        double b = c1[2] - c2[2];
        double weightR = 2.0 + rmean;
        double weightG = 2.0;
        double weightB = 3.0 - rmean;
        return Math.sqrt(weightR*(r*r) + weightG*(g*g) + weightB*(b*b)) / 1.5275;
    }
    
    public static double distance_linear(int color[], int test[]) {
        return Math.sqrt(Math.pow(color[0] - test[0], 2D) + Math.pow(color[1] - test[1], 2D) + Math.pow(color[2] - test[2], 2D));
    }

    public static int getR(int color) {
        return ((color >> 16) & 255);
    }

    public static int getG(int color) {
        return ((color >> 8) & 255);
    }

    public static int getB(int color) {
        return color & 255;
    }

    public static int getComponent(int color, int component) {
        switch (component) {
            case 0:
                return getR(color);
            case 1:
                return getG(color);
            case 2:
                return getB(color);
            default:
                return 0;
        }
    }
}
