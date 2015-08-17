package org.badvision.outlaweditor;

import javafx.scene.image.WritableImage;

/**
 *
 * @author brobert
 */
public abstract class TileRenderer {

    public abstract WritableImage redrawSprite(byte[] spriteData, WritableImage image, boolean useBleedOver);

    public abstract int getWidth();

    public abstract int getHeight();
}
