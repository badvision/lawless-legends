package org.badvision.outlaweditor.apple;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Menu;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.data.TileUtils;

/**
 *
 * @author brobert
 */
public class AppleTileEditor extends TileEditor {

    FillPattern currentPattern = FillPattern.DarkViolet1;
    DrawMode drawMode = DrawMode.Toggle;

    @Override
    public void setEntity(Tile t) {
        super.setEntity(t);
        if (TileUtils.getPlatformData(t, Platform.AppleII) == null) {
            TileUtils.setPlatformData(t, Platform.AppleII, new byte[32]);
        }
    }

    @Override
    public void buildEditorUI(AnchorPane tileEditorAnchorPane) {
        grid = new Rectangle[14][16];
        gridGroup = new Group();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 14; x++) {
                final int xx = x;
                final int yy = y;
                Rectangle rect = new Rectangle(zoom * x + 5, zoom * y + 5, zoom - 2, zoom - 2);
                rect.setOnMouseDragged(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        performDragAction((int) (t.getX() / zoom), (int) (t.getY() / zoom));
                    }
                });
                rect.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        handleMouse(t, xx, yy);
                    }
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
        y = Math.min(Math.max(y, 0), 15);
        x = Math.min(Math.max(x, 0), 13);
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
        FillPattern.buildMenu(tilePatternMenu, new DataObserver<FillPattern>() {
            @Override
            public void observedObjectChanged(FillPattern object) {
                changeCurrentPattern(object);
            }
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
}
