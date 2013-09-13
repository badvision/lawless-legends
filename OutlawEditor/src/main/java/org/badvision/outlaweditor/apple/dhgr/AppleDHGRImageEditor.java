/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.apple.dhgr;

import org.badvision.outlaweditor.apple.*;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.input.MouseEvent;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.data.DataObserver;

/**
 *
 * @author brobert
 */
public class AppleDHGRImageEditor extends AppleImageEditor implements EventHandler<MouseEvent> {

    public AppleDHGRImageEditor() {
        xScale = 1;
        yScale = 2;
        changeCurrentPattern(FillPattern.Magenta);
    }
   
    @Override
    public Platform getPlatform() {
        return Platform.AppleII_DHGR;
    }


    @Override
    public void buildPatternSelector(Menu tilePatternMenu) {
        FillPattern.buildMenu(tilePatternMenu, new DataObserver<FillPattern>() {
            @Override
            public void observedObjectChanged(FillPattern object) {
                changeCurrentPattern(object);
            }
        });
    }

    public void changeCurrentPattern(FillPattern pattern) {
        currentFillPattern = pattern.getBytePattern();
        hiBitMatters = pattern.hiBitMatters;
        lastActionX = -1;
        lastActionY = -1;
    }

    @Override
    public void redrawScanline(int y) {
        currentImage = Platform.AppleII_DHGR.imageRenderer.renderScanline(currentImage, y, getWidth(), getImageData());
    }
    
    /**
     * This takes the current image and dithers it to match the new image dimensions
     * Most likely it will result in a really bad looking resized copy
     * but in some cases might look okay
     * @param newWidth
     * @param newHeight 
     */
    @Override
    public void rescale(int newWidth, int newHeight) {
        
    }
    
    /**
     * Crops the image (if necessary) or resizes the image leaving the extra space
     * blank (black)
     * @param newWidth
     * @param newHeight 
     */
    @Override
    public void crop(int newWidth, int newHeight) {
    }    
}