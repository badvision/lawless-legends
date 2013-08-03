package org.badvision.outlaweditor;

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
 * @author brobert
 */
public enum Platform {
    AppleII(AppleTileEditor.class, AppleImageEditor.class, new AppleTileRenderer(), new AppleImageRenderer(),2, 16), 
    AppleII_DHGR(AppleDHGRTileEditor.class, AppleDHGRImageEditor.class, new AppleDHGRTileRenderer(), new AppleDHGRImageRenderer(),4, 16), 
    C64(null, null, null, null, 16, 16);
    
    public Class<? extends TileEditor> tileEditor;
    public Class<? extends ImageEditor> imageEditor;
    public TileRenderer tileRenderer;
    public ImageRenderer imageRenderer;
    public int dataWidth;
    public int dataHeight;
    
    Platform(Class ed, Class imged, TileRenderer ren, ImageRenderer img, int w, int h) {
        tileEditor = ed;
        imageEditor = imged;
        tileRenderer = ren;
        imageRenderer = img;
        dataWidth = w;
        dataHeight = h;
    }
}
