/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.api;

import org.badvision.outlaweditor.ImageEditor;
import org.badvision.outlaweditor.ImageRenderer;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.TileRenderer;
import org.badvision.outlaweditor.apple.AppleImageEditor;
import org.badvision.outlaweditor.apple.AppleTileEditor;
import org.badvision.outlaweditor.apple.AppleTileRenderer;
import org.badvision.outlaweditor.apple.AppleImageRenderer;
import org.badvision.outlaweditor.apple.dhgr.AppleDHGRImageEditor;
import org.badvision.outlaweditor.apple.dhgr.AppleDHGRImageRenderer;
import org.badvision.outlaweditor.apple.dhgr.AppleDHGRTileEditor;
import org.badvision.outlaweditor.apple.dhgr.AppleDHGRTileRenderer;

/**
 * Enumeration of platforms
 *
 * @author brobert
 */
public enum Platform {

    AppleII(AppleTileEditor.class, AppleImageEditor.class, new AppleTileRenderer(), new AppleImageRenderer(), 2, 16, 40, 192),
    AppleII_DHGR(AppleDHGRTileEditor.class, AppleDHGRImageEditor.class, new AppleDHGRTileRenderer(), new AppleDHGRImageRenderer(), 4, 16, 80, 192),
    C64(null, null, null, null, 16, 16, 40, 200);

    public Class<? extends TileEditor> tileEditor;
    public Class<? extends ImageEditor> imageEditor;
    public TileRenderer tileRenderer;
    public ImageRenderer imageRenderer;
    public int dataWidth;
    public int dataHeight;
    public int maxImageWidth;
    public int maxImageHeight;

    Platform(Class<? extends TileEditor> ed, Class<? extends ImageEditor> imged, TileRenderer ren, ImageRenderer img, int w, int h, int maxW, int maxH) {
        tileEditor = ed;
        imageEditor = imged;
        tileRenderer = ren;
        imageRenderer = img;
        dataWidth = w;
        dataHeight = h;
        maxImageWidth = maxW;
        maxImageHeight = maxH;
    }
}
