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
        return (int[])colors.get(col);
    }
    
    public int getColorInt(int c) {
        int col[] = getColor(c);
        return toRGBInt(col);
    }
    
    public void addColor(int col[]) {
        /*  45*/        colors.add(col);
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
        return new int[] {getR(color), getG(color), getB(color)};        
    }
    public static int toRGBInt(int[] col) {        
        return 0x10000 * col[0] + 256 * col[1] + col[2];
    }
    
    public int findColor(int color[]) {
        int lastDiff = COLOR_DISTANCE_MAX;
        int bestFit = 0;
        for(int i = 0; i < colors.size(); i++) {
            int test[] = (int[])colors.get(i);
            int diff = (int)distance(color, test);
            if(diff < lastDiff) {
                lastDiff = diff;
                bestFit = i;
            }
        }
        
        return bestFit;
    }
    
    public static double distance(int color[], int test[]) {
        return Math.pow(Math.abs(color[0] - test[0]), 3D) + Math.pow(Math.abs(color[1] - test[1]), 3D) + Math.pow(Math.abs(color[2] - test[2]), 3D);
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
            case 0: return getR(color);
            case 1: return getG(color);
            case 2: return getB(color);
            default: return 0;
        }
    }

    public static int addError(int color, int component, int error) {
        int[] sourceColor = parseIntColor(color);
        sourceColor[component] = Math.max(0, Math.min(255, sourceColor[component] + error));
        return toRGBInt(sourceColor);
    }
}