/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.apple.dhgr;

import java.util.*;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public enum FillPattern {
    Magenta(true, 4, false, 
            "+---+---+---+---+---+---+---"),
    Pink(true, 4, false, 
            "++-+++-+++-+++-+++-+++-+++-+"),
    Purple(true, 4, false, 
            "++--++--++--++--++--++--++--"),
    LtBlue(true, 4, false, 
            "+++-+++-+++-+++-+++-+++-+++-"),
    MedBlue(true, 4, false, 
            "-++--++--++--++--++--++--++-"),
    Blue(true, 4, false,
            "-+---+---+---+---+---+---+--"),
    Green(true, 4, false,  
            "--+---+---+---+---+---+---+-"),
    LtGreen(true, 4, false, 
            "--++--++--++--++--++--++--++"),
    Aqua(true, 4, false, 
            "-+++-+++-+++-+++-+++-+++-+++"),
    Yellow(true, 4, false, 
            "+-+++-+++-+++-+++-+++-+++-++"),
    Orange(true, 4, false, 
            "+--++--++--++--++--++--++--+"),
    Brown(true, 4, false,  
            "---+---+---+---+---+---+---+"),
    Black(false, 1, false, "-------"),
    White(false, 1, false, "+++++++"),
    Grey1(false, 2, false, 
            "+-+-+-+-+-+-+-"),
    Grey2(false, 2, false, 
            "-+-+-+-+-+-+-+");
//    ,
//    BW3(false, 4, false, 
//            "+--++--++--++--++--++--++--+",
//            "-++--++--++--++--++--++--++-"
//    ),
//    BW4(false, 4, false, 
//            "-++--++--++--++--++--++--++-",
//            "+--++--++--++--++--++--++--+"
//    );

    public static int[] bitmask(boolean hiBit, String pattern) {
        int[] out = new int[pattern.length()/7];
        int place = 1;
        int pos = 0;
        int value = hiBit ? 128 : 0;
        for (int i=0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '1' || c == '+') {
                value |= place;
            }
            out[pos] = value;
            place <<= 1;
            if (place >= 128) {
                place = 1;
                value = hiBit ? 128 : 0;
                pos++;
            }
        }
        return out;
    }
    
    public static Integer[] buildPattern(boolean hiBit, String... pattern) {
        List<Integer> out = new ArrayList<>();
        for (String s : pattern) {
            for (int i : bitmask(hiBit, s)) {
                out.add(i);
            }
        }
        return out.toArray(new Integer[0]);
    }
    
    public static void buildMenu(Menu target, final DataObserver<FillPattern> dataObserver) {
        target.getItems().clear();
        for (final FillPattern fill : FillPattern.values()) {
            MenuItem i = new MenuItem(fill.name(), new ImageView(fill.getPreview()));            
            i.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    dataObserver.observedObjectChanged(fill);
                }
            });
            target.getItems().add(i);
        }
    }
    Integer[] pattern;
    int[] bytePattern;
    int width;
    boolean hiBitMatters;
    WritableImage preview;

    private FillPattern(boolean hiBitMatters, int width, boolean hiBit, String... pattern) {
        this.pattern = buildPattern(hiBit, pattern);        
        this.width = width;
        this.hiBitMatters = hiBitMatters;
        this.bytePattern = getBytePattern();
    }

    public WritableImage getPreview() {
        if (preview == null) {
            preview = renderPreview(null, bytePattern);
        }
        return preview;
    }
    
    public int[] getBytePattern() {
        int[] out = new int[16 * 4];
        int pos = 0;
        int patternHeight = pattern.length / width;
        for (int y = 0; y < 16; y++) {
            int yOffset = (y % patternHeight) * width;
            for (int x = 0; x < 4; x++) {
                out[pos++] = pattern[yOffset + (x % width)];
            }
        }
        return out;
    }

    public static int[] interleave(int[] pat1, int[] pat2) {
        int[] out = Arrays.copyOf(pat1, pat1.length);
        for (int y = 1; y < 16; y += 2) {
            int offset = y * 4;
            for (int x = 0; x < 4; x++) {
                out[offset + x] = pat2[offset + x];
            }
        }
        return out;
    }

    public static int[] fromTile(Tile t) {
        byte[] raw = TileUtils.getPlatformData(t, Platform.AppleII_DHGR);
        int[] out = new int[16 * 4];
        int pos = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 4; x++) {
                out[pos++] = raw[y * 4 + x] & 0x0ff;
            }
        }
        return out;
    }

    public static WritableImage renderPreview(WritableImage img, int[] pattern) {
        byte[] b = new byte[pattern.length];
        for (int i=0; i < pattern.length; i++) {
            b[i]=(byte) pattern[i];
        }
        return Platform.AppleII_DHGR.tileRenderer.redrawSprite(b, null, true);
    }

    public static Map<String, FillPattern> getMapOfValues() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(
                        v -> v.name(),
                        v -> v,
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }
}
