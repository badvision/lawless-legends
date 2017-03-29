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

import java.util.Arrays;
import java.util.HashMap;
import javafx.scene.Group;
import javafx.scene.control.Menu;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.data.DataUtilities;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class AppleTileEditor extends TileEditor {

    FillPattern currentPattern = FillPattern.DarkViolet1;
    DrawMode drawMode = DrawMode.Toggle;
    public static final long SAFE_WAIT_TIME = 100;

    @Override
    public void setEntity(Tile t) {
        super.setEntity(t);
        if (TileUtils.getPlatformData(t, Platform.AppleII) == null) {
            TileUtils.setPlatformData(t, Platform.AppleII, new byte[32]);
        }
    }

    @Override
    public void buildEditorUI(Pane tileEditorAnchorPane) {
        grid = new Rectangle[14][16];
        gridGroup = new Group();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 14; x++) {
                final int xx = x;
                final int yy = y;
                Rectangle rect = new Rectangle(zoom * x + 5, zoom * y + 5, zoom - 2, zoom - 2);
                rect.setOnMouseDragged((MouseEvent t) -> {
                    performDragAction((int) (t.getX() / zoom), (int) (t.getY() / zoom));
                });
                rect.setOnMousePressed((MouseEvent t) -> {
                    handleMouse(t, xx, yy);
                });
                grid[x][y] = rect;
                gridGroup.getChildren().add(rect);
                rect.setStrokeWidth(2);
            }
        }

        Group mainGroup = new Group();
        Rectangle background = new Rectangle(0, 0, zoom * 14 + 10, zoom * 16 + 10);
        background.setFill(Color.BLACK);
        mainGroup.getChildren().add(background);
        mainGroup.getChildren().add(gridGroup);

        tileEditorAnchorPane.getChildren().add(mainGroup);
        TileUtils.redrawTile(getEntity());
        observedObjectChanged(getEntity());
    }

    private void handleMouse(MouseEvent t, int x, int y) {
        t.consume();
        if (t.getButton() == null || t.getButton() == MouseButton.NONE) {
            return;
        }
        performAction(t.isShiftDown() || t.isSecondaryButtonDown(), x, y);
    }
    int lastActionX = -1;
    int lastActionY = -1;
    long debounceTime = 0;

    public void performDragAction(int x, int y) {
        performAction(false, x, y);
    }

    private void performAction(boolean alt, int x, int y) {
        y = Math.min(Math.max(y, 0), 15);
        x = Math.min(Math.max(x, 0), 13);
        if ((lastActionX == x && lastActionY == y) && (debounceTime > System.currentTimeMillis())) {
            return;
        }
        debounceTime = System.currentTimeMillis() + SAFE_WAIT_TIME;
        lastActionX = x;
        lastActionY = y;
        trackState();
        switch (drawMode) {
            case Toggle:
                if (alt) {
                    toggleHiBit(x, y);
                } else {
                    toggle(x, y);
                }
                break;
            case Pencil1px:
                int pat = currentPattern.getBytePattern()[y * 4 + (x / 7)];
                set((pat & (1 << (x % 7))) != 0, x, y);
                if (currentPattern.hiBitMatters) {
                    setHiBit(pat >= 128, x, y);
                }
                break;
            case Pencil3px:
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    if (xx < 0 || xx >= 14) {
                        continue;
                    }
                    pat = currentPattern.getBytePattern()[y * 4 + (xx / 7)];
                    set((pat & (1 << (xx % 7))) != 0, xx, y);
                    if (currentPattern.hiBitMatters) {
                        setHiBit(pat >= 128, xx, y);
                    }
                }
                break;
        }
        observedObjectChanged(getEntity());
    }

    @Override
    public void observedObjectChanged(Tile tile) {
        recolorGrid(
                TileUtils.getPlatformData(tile, Platform.AppleII),
                grid,
                TileUtils.getImage(tile, Platform.AppleII));
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
        lastActionX = -1;
        lastActionY = -1;
    }

    @Override
    public void buildPatternSelector(Menu tilePatternMenu) {
        FillPattern.buildMenu(tilePatternMenu, (FillPattern object) -> {
            changeCurrentPattern(object);
        });
    }

    public void changeCurrentPattern(FillPattern pat) {
        currentPattern = pat;
        lastActionX = -1;
        lastActionY = -1;
    }

    @Override
    public void unregister() {
    }
    int zoom = 25;
    Group gridGroup;
    Rectangle[][] grid;

    public void toggleHiBit(int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII);
        data[y * 2 + (x / 7)] ^= 128;
        TileUtils.setPlatformData(getEntity(), Platform.AppleII, data);
        redraw();
    }

    public void setHiBit(boolean on, int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII);
        if (on) {
            data[y * 2 + (x / 7)] |= 128;
        } else {
            data[y * 2 + (x / 7)] &= 127;
        }
        TileUtils.setPlatformData(getEntity(), Platform.AppleII, data);
        redraw();
    }

    public void toggle(int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII);
        data[y * 2 + (x / 7)] ^= (1 << (x % 7));
        TileUtils.setPlatformData(getEntity(), Platform.AppleII, data);
        redraw();
    }

    public void set(boolean on, int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII);
        data[y * 2 + (x / 7)] |= (1 << (x % 7));
        if (!on) {
            data[y * 2 + (x / 7)] ^= (1 << (x % 7));
        }
        TileUtils.setPlatformData(getEntity(), Platform.AppleII, data);
        redraw();
    }

    public void recolorGrid(byte[] spriteData, Shape[][] grid, WritableImage img) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 14; x++) {
                boolean isHiBit = ((spriteData[y * 2 + x / 7] & 128) != 0);
                grid[x][y].setFill(img.getPixelReader().getColor(x * 2 + (isHiBit ? 1 : 0), y * 2));
                if ((spriteData[y * 2 + x / 7] & (1 << (x % 7))) != 0) {
                    grid[x][y].setStroke(Color.ANTIQUEWHITE);
                } else {
                    grid[x][y].setStroke(
                            isHiBit
                                    ? (x % 2 == 1) ? Color.CHOCOLATE : Color.CORNFLOWERBLUE
                                    : (x % 2 == 1) ? Color.GREEN : Color.VIOLET);
                }
            }
        }
    }

    @Override
    public void copy() {
        java.util.Map<DataFormat, Object> clip = new HashMap<>();
        clip.put(DataFormat.IMAGE, TileUtils.getImage(getEntity(), Platform.AppleII));
        clip.put(DataFormat.PLAIN_TEXT, DataUtilities.hexDump(TileUtils.getPlatformData(getEntity(), Platform.AppleII)));
        clip.put(DataFormat.HTML, buildHtmlExport());
        Clipboard.getSystemClipboard().setContent(clip);
    }

    @Override
    public void paste() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void select() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectNone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void redraw() {
        if (getEntity() != null) {
            TileUtils.redrawTile(getEntity());
        }
    }

    private String buildHtmlExport() {
        StringBuilder export = new StringBuilder("<table>");
        export.append("<tr><th>H</th><th>0</th><th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th>")
                .append("<th>H</th><th>0</th><th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th>")
                .append("<th>B1</th><th>B2</th></tr>");
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII);
        for (int row = 0; row < 16; row++) {
            export.append("<tr>");
            for (int col = 0; col < 2; col++) {
                int b = data[row * 2 + col] & 0x0ff;
                export.append("<td>")
                        .append(b>>7)
                        .append("</td>");
                for (int bit = 0; bit < 7; bit++) {
                    export.append("<td style='background:#")
                            .append(getHexColor((Color) grid[bit + col * 7][row].getFill()))
                            .append("'>")
                            .append(b&1)
                            .append("</td>");
                    b >>=1;
                }
            }
            export.append("<td>")
                    .append(DataUtilities.getHexValueFromByte(data[row*2]))
                    .append("</td><td>")
                    .append(DataUtilities.getHexValueFromByte(data[row*2+1]))
                    .append("</td>");
            export.append("</tr>");
        }
        export.append("</table>");
        return export.toString();
    }

    private String getHexColor(Color color) {
        return DataUtilities.getHexValue((int) (color.getRed() * 255))
                + DataUtilities.getHexValue((int) (color.getGreen() * 255))
                + DataUtilities.getHexValue((int) (color.getBlue() * 255));
    }

    @Override
    public void copyData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
