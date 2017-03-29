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

import javafx.scene.Group;
import javafx.scene.control.Menu;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.badvision.outlaweditor.api.Platform;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.data.TileUtils;

/**
 *
 * @author brobert
 */
public class AppleDHGRTileEditor extends TileEditor {

    FillPattern currentPattern = FillPattern.Magenta;
    DrawMode drawMode = DrawMode.Toggle;

    @Override
    public void setEntity(Tile t) {
        super.setEntity(t);
        if (TileUtils.getPlatformData(t, Platform.AppleII_DHGR) == null) {
            TileUtils.setPlatformData(t, Platform.AppleII_DHGR, new byte[64]);
        }
    }

    @Override
    public void buildEditorUI(Pane tileEditorAnchorPane) {
        grid = new Rectangle[28][16];
        gridGroup = new Group();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 28; x++) {
                final int xx = x;
                final int yy = y;
                Rectangle rect = new Rectangle((zoom/2) * x + 5, zoom * y + 5, zoom/2 - 2, zoom - 2);
                rect.setOnMouseDragged((MouseEvent t) -> {
                    performDragAction((int) (t.getX() / (zoom/2)), (int) (t.getY() / zoom));
                });
                rect.setOnMousePressed((MouseEvent t) -> {
                    handleMouse(t, xx, yy);
                    lastActionX=-1;
                    lastActionY=-1;
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
    public void performDragAction(int x, int y) {
        performAction(false, x, y);
    }

    private void performAction(boolean alt, int x, int y) {
        y = Math.min(Math.max(y,0), 15);
        x = Math.min(Math.max(x,0), 27);
        if (lastActionX == x && lastActionY == y) {
            return;
        }
        lastActionX = x;
        lastActionY = y;
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
                for (int xx = x-1; xx <= x+1; xx++) {
                    if (xx < 0 || xx >= 28) continue;
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
                TileUtils.getPlatformData(tile, Platform.AppleII_DHGR),
                grid,
                TileUtils.getImage(tile, Platform.AppleII_DHGR));
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
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII_DHGR);
        data[y * 4 + (x / 7)] ^= 128;
        TileUtils.setPlatformData(getEntity(), Platform.AppleII_DHGR, data);
        redraw();
    }

    public void setHiBit(boolean on, int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII_DHGR);
        if (on) {
            data[y * 4 + (x / 7)] |= 128;
        } else {
            data[y * 4 + (x / 7)] &= 127;
        }
        TileUtils.setPlatformData(getEntity(), Platform.AppleII_DHGR, data);
        redraw();
    }

    public void toggle(int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII_DHGR);
        data[y * 4 + (x / 7)] ^= (1 << (x % 7));
        TileUtils.setPlatformData(getEntity(), Platform.AppleII_DHGR, data);
        redraw();
    }

    public void set(boolean on, int x, int y) {
        byte[] data = TileUtils.getPlatformData(getEntity(), Platform.AppleII_DHGR);
        data[y * 4 + (x / 7)] |= (1 << (x % 7));
        if (!on) {
            data[y * 4 + (x / 7)] ^= (1 << (x % 7));
        }
        TileUtils.setPlatformData(getEntity(), Platform.AppleII_DHGR, data);
        redraw();
    }

    public void recolorGrid(byte[] spriteData, Shape[][] grid, WritableImage img) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 28; x++) {
                grid[x][y].setFill(img.getPixelReader().getColor(x, y * 2));
                if ((spriteData[y * 4 + x / 7] & (1 << (x % 7))) != 0) {
                    grid[x][y].setStroke(Color.ANTIQUEWHITE);
                } else {
                    Color stroke = Color.ANTIQUEWHITE;
                    switch (x%4) {
                        case 0: stroke=Color.BROWN;
                            break;
                        case 1: stroke=Color.BLUE;
                            break;
                        case 2: stroke=Color.GREEN;
                            break;
                        case 3: stroke=Color.CHOCOLATE;
                            break;
                    }
                    grid[x][y].setStroke(stroke);
                }
            }
        }
    }

    @Override
    public void copy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    @Override
    public void copyData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
