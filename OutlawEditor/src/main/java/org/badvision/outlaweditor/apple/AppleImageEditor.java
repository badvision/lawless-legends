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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.badvision.outlaweditor.FileUtils;
import org.badvision.outlaweditor.ImageEditor;
import org.badvision.outlaweditor.TransferHelper;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.data.TileMap;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.PlatformData;
import org.badvision.outlaweditor.ui.UIAction;

/**
 *
 * @author brobert
 */
public class AppleImageEditor extends ImageEditor implements EventHandler<MouseEvent> {

    public int[] currentFillPattern = FillPattern.White_PC.bytePattern;
    public boolean hiBitMatters = true;
    protected DrawMode currentDrawMode = DrawMode.Pencil1px;
    protected WritableImage currentImage;
    protected Pane anchorPane;
    protected ImageView screen;
    protected int posX = 0;
    protected int posY = 0;
    protected double zoom = 1.0;
    protected int xScale = 2;
    protected int yScale = 2;

    public static enum StateVars {
        PATTERN, DRAW_MODE
    };

    public Platform getPlatform() {
        return Platform.AppleII;
    }

    @Override
    protected void onEntityUpdated() {
        super.onEntityUpdated();
        data = null;
    }

    @Override
    public void buildEditorUI(Pane editorAnchorPane) {
        anchorPane = editorAnchorPane;
        redraw();
        screen = new ImageView(currentImage);
        anchorPane.getChildren().add(0, screen);
        screen.setOnMouseMoved(this);
        screen.setOnMousePressed(this);
        screen.setOnMouseClicked(this);
        screen.setOnMouseReleased(this);
        screen.setOnMouseDragged(this);
        screen.setOnMouseDragReleased(this);
    }

    @Override
    public void buildPatternSelector(Menu tilePatternMenu) {
        FillPattern.buildMenu(tilePatternMenu, (FillPattern object) -> {
            changeCurrentPattern(object);
            state.put(StateVars.PATTERN, object);
        });
    }

    public void changeCurrentPattern(FillPattern pattern) {
        if (pattern == null) {
            return;
        }
        currentFillPattern = pattern.getBytePattern();
        hiBitMatters = pattern.hiBitMatters;
        lastActionX = -1;
        lastActionY = -1;
    }

    EnumMap<StateVars, Object> state = new EnumMap<>(StateVars.class);

    @Override
    public EnumMap getState() {
        return state;
    }

    @Override
    public void setState(EnumMap oldState) {
        state.putAll(oldState);
        changeCurrentPattern((FillPattern) state.get(StateVars.PATTERN));
        DrawMode oldDrawMode = (DrawMode) state.get(StateVars.DRAW_MODE);
        if (oldDrawMode != null) {
            _setDrawMode(oldDrawMode);
        } else {
            state.put(StateVars.DRAW_MODE, currentDrawMode);
        }
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        _setDrawMode(drawMode);
        state.put(StateVars.DRAW_MODE, drawMode);
    }

    private void _setDrawMode(DrawMode drawMode) {
        currentDrawMode = drawMode;
        lastActionX = -1;
        lastActionY = -1;
        selectionFinished = false;
        if (drawMode != DrawMode.Stamp) {
            selectNone();
        }
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregister() {
        anchorPane.getChildren().remove(screen);
    }

    @Override
    public void observedObjectChanged(Image object) {
        redraw();
    }

    public void redrawScanline(int y) {
        currentImage = getPlatform().imageRenderer.renderScanline(currentImage, y, getWidth(), getImageData());
    }

    @Override
    public void redraw() {
        System.out.println("Redraw " + getPlatform().name());
        currentImage = getPlatform().imageRenderer.renderImage(currentImage, getImageData(), getWidth(), getHeight());
    }

    PlatformData data = null;

    public PlatformData getPlatformData() {
        if (data == null) {
            data = getPlatformData(getPlatform());
            if (data == null) {
                createNewPlatformData(getPlatform().maxImageWidth, getPlatform().maxImageHeight);
                data = getPlatformData(getPlatform());
            }
        }
        return data;
    }

    public byte[] getImageData() {
        return getPlatformData().getValue();
    }

    public void setData(byte[] data) {
        getPlatformData().setValue(data);
    }

    public void setDataAndRedraw(byte[] data) {
        setData(data);
        redraw();
    }

    @Override
    public void togglePanZoom() {
        anchorPane.getChildren().stream().filter((n) -> !(n == screen)).forEach((n) -> {
            n.setVisible(!n.isVisible());
        });
    }

    @Override
    public void zoomOut() {
        if (zoom <= 1) {
            zoom(-0.1);
        } else {
            zoom(-0.25);
        }
    }

    @Override
    public void zoomIn() {
        if (zoom >= 1) {
            zoom(0.25);
        } else {
            zoom(0.1);
        }
    }

    @Override
    public double getZoomScale() {
        return zoom;
    }

    private void zoom(double delta) {
        zoom += delta;
        zoom = Math.min(Math.max(0.15, zoom), 4.0);
    }

    @Override
    public void handle(MouseEvent t) {
        int x = (int) t.getX() / xScale;
        int y = (int) t.getY() / yScale;
        cursorInfoProperty().set("X="+x+"("+(x/7)+","+(x%7)+") Y="+y);
        if (t.getEventType().equals(MouseEvent.MOUSE_MOVED)) {
            return;
        }
        if (performAction(t.isShiftDown() || t.isSecondaryButtonDown(), t.getEventType().equals(MouseEvent.MOUSE_RELEASED), x, y)) {
            t.consume();
        }

    }
    protected int lastActionX = -1;
    protected int lastActionY = -1;
    protected long debounce = -1;
    public static long DEBOUNCE_THRESHOLD = 50;

    public boolean performAction(boolean alt, boolean released, int x, int y) {
        if (debounce != -1) {
            long ellapsed = System.currentTimeMillis() - debounce;
            if (ellapsed <= DEBOUNCE_THRESHOLD) {
                return false;
            }
            debounce = -1;
        }
        y = Math.min(Math.max(y, 0), getHeight() - 1);
        x = Math.min(Math.max(x, 0), (getWidth() * 7) - 1);
        boolean canSkip = false;
        if (lastActionX == x && lastActionY == y) {
            canSkip = true;
        }
        lastActionX = x;
        lastActionY = y;
        switch (currentDrawMode) {
            case Toggle:
                if (canSkip) {
                    return false;
                }
                trackState();
                if (alt) {
                    toggleHiBit(x, y);
                } else {
                    toggle(x, y);
                }
                redrawScanline(y);
                break;
            case Pencil1px:
                if (canSkip) {
                    return false;
                }
                trackState();
                plot(x, y, currentFillPattern, hiBitMatters); // [ref BigBlue1_30]
                redrawScanline(y);
                break;
            case Pencil3px:
                if (canSkip) {
                    return false;
                }
                trackState();
                drawBrush(x, y, 3, currentFillPattern, hiBitMatters);
                break;
            case Pencil5px:
                if (canSkip) {
                    return false;
                }
                trackState();
                drawBrush(x, y, 5, currentFillPattern, hiBitMatters);
                break;
            case Rectangle:
                debounce = System.currentTimeMillis()-10;
                if (released) {
                    trackState();
                    fillSelection(x, y);
                    redraw();
                    debounce = System.currentTimeMillis();
                    return true;
                } else {
                    updateSelection(x, y);
                    return false;
                }
            case Select:
                debounce = System.currentTimeMillis();
                if (selectionFinished && !released) {
                    startSelection(x, y);
                } else {
                    updateSelection(x, y);
                }
                selectionFinished = released;
                return false;
        }
        return true;
//        observedObjectChanged(getEntity());
    }
    public boolean selectionFinished = false;
    public static Rectangle selectRect = null;
    public int selectStartX = -1;
    public int selectStartY = -1;
    public int selectEndX = -1;
    public int selectEndY = -1;

    private void startSelection(int x, int y) {
        selectNone();
        selectRect = new Rectangle(1, 1, Color.NAVY);
        selectRect.setTranslateX(x * xScale);
        selectRect.setTranslateY(y * yScale);
        selectRect.setOpacity(0.5);
        selectStartX = x;
        selectStartY = y;
        anchorPane.getChildren().add(selectRect);
    }

    private void updateSelection(int x, int y) {
        if (selectRect == null) {
            startSelection(x, y);
        }

        int startX = Math.min(selectStartX, x);
        int endX = Math.max(selectStartX, x);
        int startY = Math.min(selectStartY, y);
        int endY = Math.max(selectStartY, y);

        selectRect.setTranslateX(startX * xScale);
        selectRect.setTranslateY(startY * yScale);
        selectRect.setWidth((endX - startX) * xScale);
        selectRect.setHeight((endY - startY) * yScale);

        setSelectionArea(startX, startY, endX, endY);
    }

    private void fillSelection(int x, int y) {
        if (selectRect == null) {
            return;
        }
        anchorPane.getChildren().remove(selectRect);
        selectRect = null;

        for (int yy = Math.min(selectStartY, y); yy <= Math.max(selectStartY, y); yy++) {
            for (int xx = Math.min(selectStartX, x); xx <= Math.max(selectStartX, x); xx++) {
                plot(xx, yy, currentFillPattern, hiBitMatters);
            }
        }
    }

    public void drawBrush(int x, int y, int size, int[] pattern, boolean hiBitMatters) {
        for (int yy = y - size; yy <= y + size; yy++) {
            for (int xx = x - size; xx <= x + size; xx++) {
                if (Math.sqrt((xx - x) * (xx - x) + (yy - y) * (yy - y)) > size) {
                    continue;
                }
                plot(xx, yy, pattern, hiBitMatters);
            }
            redrawScanline(yy);
        }
    }

    public void plot(int x, int y, int[] pattern, boolean hiBitMatters) {
        if (x < 0 || y < 0 || x >= getWidth() * 7 || y >= getHeight()) {
            return;
        }
        int pat = pattern[(y % 16) * 4 + ((x / 7) % 4)];  // [ref BigBlue1_20]
        set((pat & (1 << (x % 7))) != 0, x, y);
        if (hiBitMatters) {
            setHiBit(pat >= 128, x, y);
        }
    }

    public void toggleHiBit(int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] ^= 128;
        setData(data);
    }

    public void setHiBit(boolean on, int x, int y) {
        byte[] data = getImageData();
        if (on) {
            data[y * getWidth() + (x / 7)] |= 128;
        } else {
            data[y * getWidth() + (x / 7)] &= 127;
        }
        setData(data);
    }

    public void toggle(int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] ^= (1 << (x % 7));
        setData(data);
    }

    public void set(boolean on, int x, int y) {
        byte[] data = getImageData();
        data[y * getWidth() + (x / 7)] |= (1 << (x % 7));  // [ref BigBlue1_10]
        if (!on) {
            data[y * getWidth() + (x / 7)] ^= (1 << (x % 7));
        }
        setData(data);
    }

    public int getWidth() {
        return getPlatformData(getPlatform()).getWidth();
    }

    public int getHeight() {
        return getPlatformData(getPlatform()).getHeight();
    }

    byte[] copyData = null;

    @Override
    public void copy() {
        java.util.Map<DataFormat, Object> clip = new HashMap<>();
        clip.put(DataFormat.IMAGE, currentImage);
        clip.put(DataFormat.PLAIN_TEXT, "selection/image/" + ApplicationState.getInstance().getGameData().getImage().indexOf(getEntity()) + "/" + getSelectionInfo());
        Clipboard.getSystemClipboard().setContent(clip);
        copyData = Arrays.copyOf(getImageData(), getImageData().length);
    }

    @Override
    public void paste() {
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.PLAIN_TEXT)) {
            if (pasteAppContent((String) Clipboard.getSystemClipboard().getContent(DataFormat.PLAIN_TEXT))) {
                return;
            }
        }
        if (Clipboard.getSystemClipboard().hasContent(DataFormat.IMAGE)) {
            javafx.scene.image.Image image = Clipboard.getSystemClipboard().getImage();

            importImage(image);
        }
    }
    //selection/map/2/x1/0/y1/0/x2/19/y2/11

    public boolean pasteAppContent(String contentPath) {
        trackState();
        System.out.println("Clipboard >> " + contentPath);
        Map<String, Integer> selection = TransferHelper.getSelectionDetails(contentPath);
        if (selection.containsKey("map")) {
            TileMap map = new TileMap(ApplicationState.getInstance().getGameData().getMap().get(selection.get("map")));
            byte[] buf = getPlatform().imageRenderer.renderPreview(
                    map,
                    selection.get("x1"),
                    selection.get("y1"),
                    getWidth(),
                    getHeight());
            setData(buf);
            redraw();
            return true;
        } else if (selection.containsKey("image")) {
            Image sourceImage = ApplicationState.getInstance().getGameData().getImage().get(selection.get("image"));
            byte[] sourceData;
            if (sourceImage.equals(getEntity())) {
                sourceData = copyData;
            } else {
                PlatformData platformData = sourceImage.getDisplayData().stream().filter(d -> d.getPlatform().equals(getPlatform().name())).findFirst().orElse(null);
                if (platformData == null) {
                    throw new NullPointerException("Unable to paste from source image, no matching platform data.");
                }
                sourceData = platformData.getValue();
            }

            if (selection.containsKey("all")) {
                setData(Arrays.copyOf(sourceData, sourceData.length));
                redraw();
                return true;
            } else {
                int xStart = selection.get("x1");
                int yStart = selection.get("y1");
                int xEnd = selection.get("x2");
                int yEnd = selection.get("y2");
                byte[] targetData = getImageData();
                int pasteX = lastActionX;
                int pasteY = lastActionY;
                // fix odd/even: Try to nudge left or right where it might work best.
                if ((xStart % 2) != pasteX % 2) {
                    if (pasteX == 0 || pasteX % 7 > 3 || (pasteX + (xEnd - xStart) / 7) < getWidth()) {
                        pasteX++;
                    } else {
                        pasteX--;
                    }
                }
                System.out.println("Paste to " + pasteX + "," + pasteY);
                for (int sourceY = yStart, targetY = pasteY; sourceY <= yEnd; sourceY++, targetY++) {
                    if (targetY < 0 || targetY >= getHeight()) {
                        continue;
                    }
                    int sourceRow = sourceY * getWidth();
                    int targetRow = targetY * getWidth();
                    for (int sourceX = xStart, targetX = pasteX; sourceX <= xEnd; sourceX++, targetX++) {
                        if (targetX < 0 || targetX / 7 >= getWidth()) {
                            continue;
                        }
                        int targetLoc = targetRow + targetX / 7;
                        byte sourceByte = sourceData[sourceRow + sourceX / 7];
                        byte targetByte = targetData[targetLoc];
                        int targetBit = targetX % 7;
                        int sourceBit = sourceX % 7;
                        // Remove hi-bit and image bit
                        targetByte &= 0x07f ^ (1 << targetBit);
                        // Copy hi-bit
                        targetByte |= sourceByte & 0x080;
                        // Copy x bit
                        targetByte |= ((sourceByte >> sourceBit) & 1) << targetBit;
                        targetData[targetLoc] = targetByte;
                    }
                }
                setDataAndRedraw(targetData);
                return true;
            }
        }
        return false;
    }

    @Override
    public void select() {
        _setDrawMode(DrawMode.Select);
    }

    @Override
    public void selectNone() {
        selectStartX = selectStartY = selectEndX = selectEndY = -1;
        setSelectionArea(selectStartX, selectStartY, selectEndX, selectEndY);
        if (selectRect != null) {
            anchorPane.getChildren().remove(selectRect);
        }
    }

    private void importImage(javafx.scene.image.Image image) {
        ImageDitherEngine ditherEngine = new ImageDitherEngine(getPlatform());
        ditherEngine.setTargetCoordinates(0, 0);
        UIAction.openImageConversionModal(image, ditherEngine, getWidth(), getHeight(), this::setDataAndRedraw);
    }

    private int calculateHiresOffset(int y) {
        return calculateTextOffset(y >> 3) + ((y & 7) << 10);
    }

    private int calculateTextOffset(int y) {
        return ((y & 7) << 7) + 40 * (y >> 3);
    }

    @Override
    public void exportImage() {
        byte[] output = new byte[0x02000];
        int counter = 0;
        for (int y = 0; y < getHeight(); y++) {
            int offset = calculateHiresOffset(y);
            for (int x = 0; x < getWidth(); x++) {
                output[offset + x] = getImageData()[counter++];
            }
        }
        File out = FileUtils.getFile(null, "Export image", true, FileUtils.Extension.BINARY, FileUtils.Extension.ALL);
        if (out == null) {
            return;
        }
        try (FileOutputStream outStream = new FileOutputStream(out)) {
            outStream.write(output);
            outStream.flush();
        } catch (IOException ex) {
            Logger.getLogger(AppleImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {
        UIAction.confirm("Do you want to scale the image?  If you select no, the image will be cropped as needed.", () -> {
            rescale(newWidth, newHeight);
        }, () -> {
            crop(newWidth, newHeight);
        });
    }

    /**
     * This takes the current image and dithers it to match the new image
     * dimensions Most likely it will result in a really bad looking resized
     * copy but in some cases might look okay
     *
     * @param newWidth
     * @param newHeight
     */
    public void rescale(int newWidth, int newHeight) {
        createNewPlatformData(newWidth, newHeight);
        importImage(currentImage);
    }

    /**
     * Crops the image (if necessary) or resizes the image leaving the extra
     * space blank (black)
     *
     * @param newWidth
     * @param newHeight
     */
    public void crop(int newWidth, int newHeight) {
    }

    private void createNewPlatformData(int width, int height) {
        PlatformData platformData = new PlatformData();
        platformData.setWidth(width);
        platformData.setHeight(height);
        platformData.setPlatform(getPlatform().name());
        platformData.setValue(getPlatform().imageRenderer.createImageBuffer(width, height));
        getEntity().getDisplayData().add(platformData);
    }
}
