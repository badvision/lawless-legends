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

import java.util.*;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import org.badvision.outlaweditor.api.Platform;
import static org.badvision.outlaweditor.apple.AppleNTSCGraphics.hgrToDhgr;
import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public enum FillPattern {

    Violet(true, 2, false,
            "+-+-+-+-+-+-+-"),
    DarkViolet1(true, 4, false,
            "+---+---+---+---+---+---+---",
            "--+---+---+---+---+---+---+-"
    ),
    DarkViolet2(true, 4, false,
            "--+---+---+---+---+---+---+-",
            "+---+---+---+---+---+---+---"
    ),
    LightViolet1(true, 4, false,
            "+++-+++-+++-+++-+++-+++-+++-",
            "+-+++-+++-+++-+++-+++-+++-++"
    ),
    LightViolet2(true, 4, false,
            "+-+++-+++-+++-+++-+++-+++-++",
            "+++-+++-+++-+++-+++-+++-+++-"
    ),
    Green(true, 2, false,
            "-+-+-+-+-+-+-+"),
    DarkGreen1(true, 4, false,
            "-+---+---+---+---+---+---+--",
            "---+---+---+---+---+---+---+"
    ),
    DarkGreen2(true, 4, false,
            "---+---+---+---+---+---+---+",
            "-+---+---+---+---+---+---+--"
    ),
    LightGreen1(true, 4, false,
            "++-+++-+++-+++-+++-+++-+++-+",
            "-+++-+++-+++-+++-+++-+++-+++"
    ),
    LightGreen2(true, 4, false,
            "-+++-+++-+++-+++-+++-+++-+++",
            "++-+++-+++-+++-+++-+++-+++-+"),
    Blue(true, 2, true,
            "+-+-+-+-+-+-+-"),
    DarkBlue(true, 4, true,
            "+---+---+---+---+---+---+---",
            "--+---+---+---+---+---+---+-"
    ),
    DarkBlue2(true, 4, true,
            "--+---+---+---+---+---+---+-",
            "+---+---+---+---+---+---+---"
    ),
    LightBlue1(true, 4, true,
            "+++-+++-+++-+++-+++-+++-+++-",
            "+-+++-+++-+++-+++-+++-+++-++"
    ),
    LightBlue2(true, 4, true,
            "+-+++-+++-+++-+++-+++-+++-++",
            "+++-+++-+++-+++-+++-+++-+++-"
    ),
    Orange(true, 2, true,
            "-+-+-+-+-+-+-+"),
    DarkOrange1(true, 4, true,
            "-+---+---+---+---+---+---+--",
            "---+---+---+---+---+---+---+"
    ),
    DarkOrange(true, 4, true,
            "---+---+---+---+---+---+---+",
            "-+---+---+---+---+---+---+--"
    ),
    LightOrange1(true, 4, true,
            "++-+++-+++-+++-+++-+++-+++-+",
            "-+++-+++-+++-+++-+++-+++-+++"
    ),
    LightOrange2(true, 4, true,
            "-+++-+++-+++-+++-+++-+++-+++",
            "++-+++-+++-+++-+++-+++-+++-+"),
    Black_PC(false, 1, false, "-------"),
    Black_Lo(true, 1, false, "-------"),
    Black_Hi(true, 1, true, "-------"),
    White_PC(false, 1, false, "+++++++"),
    White_Lo(true, 1, false, "+++++++"),
    White_Hi(true, 1, true, "+++++++"),
    BW1_PC(false, 4, false,
            "++--++--++--++--++--++--++--",
            "--++--++--++--++--++--++--++"
    ),
    BW1_Lo(true, 4, false,
            "++--++--++--++--++--++--++--",
            "--++--++--++--++--++--++--++"
    ),
    BW1_Hi(true, 4, true,
            "++--++--++--++--++--++--++--",
            "--++--++--++--++--++--++--++"
    ),
    BW2_PC(false, 4, false,
            "--++--++--++--++--++--++--++",
            "++--++--++--++--++--++--++--"
    ),
    BW2_Lo(true, 4, false,
            "--++--++--++--++--++--++--++",
            "++--++--++--++--++--++--++--"
    ),
    BW2_Hi(true, 4, true,
            "--++--++--++--++--++--++--++",
            "++--++--++--++--++--++--++--"
    );
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
        int[] out = new int[pattern.length() / 7];
        int place = 1;
        int pos = 0;
        int value = hiBit ? 128 : 0;
        for (int i = 0; i < pattern.length(); i++) {
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
            i.setOnAction(t -> dataObserver.observedObjectChanged(fill));
            target.getItems().add(i);
        }
    }

    public static Map<String,FillPattern> getMapOfValues() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(
                        v -> v.name(),
                        v -> v,
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }

    Integer[] pattern;
    int[] bytePattern;
    int width;
    boolean hiBitMatters;
    WritableImage preview;

    FillPattern(boolean hiBitMatters, int width, boolean hiBit, String... pattern) {
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
        byte[] raw = TileUtils.getPlatformData(t, Platform.AppleII);
        int[] out = new int[16 * 4];
        int pos = 0;
        for (int y = 0; y < 16; y++) {
            for (int c = 0; c < 2; c++) {
                for (int x = 0; x < 2; x++) {
                    out[pos++] = raw[y * 2 + x] & 0x0ff;
                }
            }
        }
        return out;
    }

    public static WritableImage renderPreview(WritableImage img, int[] pattern) {
        if (img == null) {
            img = new WritableImage(28, 32);
        }
        int[] scan = new int[2];
        for (int y = 0; y < 16; y++) {
            int b1 = pattern[y * 4] & 255;
            int b2 = pattern[y * 4 + 1] & 255;
            int i = hgrToDhgr[b1][b2];
            boolean extraHalfBit = (i & 0x10000000) != 0;
            scan[0] = i & 0xfffffff;
            b1 = pattern[y * 4 + 2] & 255;
            b2 = pattern[y * 4 + 3] & 255;
            i = hgrToDhgr[(extraHalfBit) ? b1 | 0x0100 : b1][b2];
            scan[1] = i & 0xfffffff;
            AppleImageRenderer.renderScanline(img.getPixelWriter(), y * 2, scan, true, false, 4);
            AppleImageRenderer.renderScanline(img.getPixelWriter(), y * 2 + 1, scan, true, false, 4);
        }
        return img;
    }
}
