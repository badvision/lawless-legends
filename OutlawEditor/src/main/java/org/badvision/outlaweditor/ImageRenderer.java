package org.badvision.outlaweditor;

import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.data.TileMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author brobert
 */
public abstract class ImageRenderer {

    public abstract WritableImage renderImage(WritableImage img, byte[] rawImage);

    public abstract byte[] createImageBuffer();

    public abstract WritableImage renderPreview(TileMap map, int startX, int startY);

    public abstract WritableImage renderScanline(WritableImage currentImage, int y, byte[] imageData);
    
}
