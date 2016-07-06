/*
 * Copyright 2016 org.badvision.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.badvision.outlaweditor.ui;

import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.xml.Tile;
import static org.badvision.outlaweditor.ui.UIAction.fadeOut;

/**
 * Create and manage a tile selection modal
 * @author blurry
 */
public class TileSelectModal {
    public static final int GRID_SPACING = 7;
    public static final int MAX_TILES_PER_ROW = 16;
    public static AnchorPane currentTileSelector;

    public static void showTileSelectModal(Pane anchorPane, String category, Callback<Tile, ?> callback) {
        if (currentTileSelector != null) {
            return;
        }
        currentTileSelector = new AnchorPane();

        int TILE_WIDTH = ApplicationState.getInstance().getCurrentPlatform().tileRenderer.getWidth();
        int TILE_HEIGHT = ApplicationState.getInstance().getCurrentPlatform().tileRenderer.getHeight();

        List<Tile> tiles = ApplicationState.getInstance().getGameData().getTile().stream().filter((Tile t) -> {
            return category == null || t.getCategory().equals(category);
        }).collect(Collectors.toList());

        int tilesPerRow = (int) Math.min(tiles.size(), Math.min(MAX_TILES_PER_ROW, anchorPane.getWidth() / (TILE_WIDTH + GRID_SPACING)));
        int numRows = (tiles.size() + tilesPerRow - 1) / tilesPerRow;
        int prefWidth = tilesPerRow * (TILE_WIDTH + GRID_SPACING) + GRID_SPACING;
        currentTileSelector.setPrefWidth(prefWidth);
        currentTileSelector.setPrefHeight(Math.min(numRows * (TILE_HEIGHT + GRID_SPACING) + GRID_SPACING, prefWidth));
        for (int i = 0; i < tiles.size(); i++) {
            final Tile tile = tiles.get(i);
            ImageView tileIcon = new ImageView(TileUtils.getImage(tile, ApplicationState.getInstance().getCurrentPlatform()));
            currentTileSelector.getChildren().add(tileIcon);
            tileIcon.setOnMouseClicked((e) -> {
                e.consume();
                callback.call(tile);
                closeCurrentTileSelector();
            });
            tileIcon.setOnMouseEntered((e) -> {
                tileIcon.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.CORNSILK, 5.0, 0.5, 0, 0));
                ScaleTransition st = new ScaleTransition(Duration.millis(150), tileIcon);
                st.setAutoReverse(false);
                st.setToX(1.25);
                st.setToY(1.25);
                st.play();
            });
            tileIcon.setOnMouseExited((e) -> {
                tileIcon.setEffect(null);
                ScaleTransition st = new ScaleTransition(Duration.millis(150), tileIcon);
                st.setAutoReverse(false);
                st.setToX(1);
                st.setToY(1);
                st.play();
            });
            tileIcon.setLayoutX(GRID_SPACING + (i % tilesPerRow) * (TILE_WIDTH + GRID_SPACING));
            tileIcon.setLayoutY(GRID_SPACING + (i / tilesPerRow) * (TILE_HEIGHT + GRID_SPACING));
        }
        currentTileSelector.setLayoutX((anchorPane.getWidth() - currentTileSelector.getPrefWidth()) / 2);
        currentTileSelector.setLayoutY((anchorPane.getHeight() - currentTileSelector.getPrefHeight()) / 2);
        currentTileSelector.setBackground(
                new Background(
                        new BackgroundFill(
                                new Color(0.7, 0.7, 0.9, 0.75),
                                new CornerRadii(10.0),
                                null)));
        currentTileSelector.setEffect(new DropShadow(5.0, 1.0, 1.0, Color.BLACK));
        anchorPane.getChildren().add(currentTileSelector);
        ApplicationState.getInstance().getPrimaryStage().getScene().addEventHandler(KeyEvent.KEY_PRESSED, cancelTileSelectKeyHandler);
        ApplicationState.getInstance().getPrimaryStage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, cancelTileSelectMouseHandler);
    }

    public static void closeCurrentTileSelector() {
        ApplicationState.getInstance().getPrimaryStage().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, cancelTileSelectKeyHandler);
        ApplicationState.getInstance().getPrimaryStage().getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, cancelTileSelectMouseHandler);

        fadeOut(currentTileSelector, (ActionEvent ev) -> {
            if (currentTileSelector != null) {
                Pane parent = (Pane) currentTileSelector.getParent();
                parent.getChildren().remove(currentTileSelector);
                currentTileSelector = null;
            }
        });
    }
    
    private static final EventHandler<MouseEvent> cancelTileSelectMouseHandler = (MouseEvent e) -> {
        if (!(e.getSource() instanceof ImageView)) {
            e.consume();
        }
        closeCurrentTileSelector();
    };

    private static final EventHandler<KeyEvent> cancelTileSelectKeyHandler = (KeyEvent e) -> {
        if (e.getCode() == KeyCode.ESCAPE) {
            closeCurrentTileSelector();
        }
    };

    private TileSelectModal() {
    }
}
