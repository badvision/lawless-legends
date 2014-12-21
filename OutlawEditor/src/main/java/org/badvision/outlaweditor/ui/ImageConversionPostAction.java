package org.badvision.outlaweditor.ui;

/**
 *
 * @author blurry
 */
@FunctionalInterface
public interface ImageConversionPostAction {
    public void storeConvertedImage(byte[] image);
}
