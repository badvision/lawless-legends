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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.badvision.outlaweditor.api.ApplicationState;

import static org.badvision.outlaweditor.ui.UIAction.fadeOut;

/**
 * Create and manage a pattern selection modal
 * @author blurry
 */
public class PatternSelectModal<P> {
    public static final int GRID_SPACING = 7;
    public static final int MAX_TILES_PER_ROW = 16;
    public static AnchorPane currentPatternSelector;
    Supplier<Map<String, P>> patternSupplier;
    Function<P, ImageView> imageViewGenerator;
    Consumer<P> selectionCallback;

    public PatternSelectModal(Supplier<Map<String, P>> patternSupplier,
                              Function<P, ImageView> imageViewGenerator,
                              Consumer<P> selectionCallback) {
        this.patternSupplier = patternSupplier;
        this.imageViewGenerator = imageViewGenerator;
        this.selectionCallback = selectionCallback;
    }

    public void showPatternSelectModal(Pane anchorPane) {
        if (currentPatternSelector != null) {
            return;
        }
        currentPatternSelector = new AnchorPane();
        Double zoom = 1 / anchorPane.getParent().getScaleX();
        currentPatternSelector.setScaleX(zoom);
        currentPatternSelector.setScaleY(zoom);
        Map<String, P> selections = patternSupplier.get();
        ImageView firstSelection = imageViewGenerator.apply(selections.values().iterator().next());

        int TILE_WIDTH = 100;
        int TILE_HEIGHT = (int) firstSelection.getImage().getHeight() + 18;

        int tilesPerRow = (int) Math.min(selections.size(), Math.min(MAX_TILES_PER_ROW, anchorPane.getWidth() / (TILE_WIDTH + GRID_SPACING)));
        int numRows = (selections.size() + tilesPerRow - 1) / tilesPerRow;
        int prefWidth = tilesPerRow * (TILE_WIDTH + GRID_SPACING) + GRID_SPACING;
        currentPatternSelector.setPrefWidth(prefWidth);
        currentPatternSelector.setPrefHeight(Math.min(numRows * (TILE_HEIGHT + GRID_SPACING) + GRID_SPACING, prefWidth));
        AtomicInteger i = new AtomicInteger(0);
        selections.forEach((name, selection) -> {
            Button tileIcon = new Button(name, imageViewGenerator.apply(selection));
            tileIcon.setPrefWidth(TILE_WIDTH);
            tileIcon.setBackground(Background.EMPTY);
            tileIcon.setTextAlignment(TextAlignment.CENTER);
            tileIcon.setContentDisplay(ContentDisplay.TOP);
            currentPatternSelector.getChildren().add(tileIcon);
            tileIcon.setOnMouseClicked((e) -> {
                e.consume();
                selectionCallback.accept(selection);
                closeCurrentPatternSelector();
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
            tileIcon.setLayoutX(GRID_SPACING + (i.get() % tilesPerRow) * (TILE_WIDTH + GRID_SPACING));
            tileIcon.setLayoutY(GRID_SPACING + (i.get() / tilesPerRow) * (TILE_HEIGHT + GRID_SPACING));
            i.incrementAndGet();
        });
        currentPatternSelector.setLayoutX((anchorPane.getWidth() - currentPatternSelector.getPrefWidth()) / 2 / zoom);
        currentPatternSelector.setLayoutY((anchorPane.getHeight() - currentPatternSelector.getPrefHeight()) / 2 / zoom);
        currentPatternSelector.setBackground(
                new Background(
                        new BackgroundFill(
                                new Color(0.7, 0.7, 0.9, 0.75),
                                new CornerRadii(10.0),
                                null)));
        currentPatternSelector.setEffect(new DropShadow(5.0, 1.0, 1.0, Color.BLACK));
        anchorPane.getChildren().add(currentPatternSelector);
        ApplicationState.getInstance().getPrimaryStage().getScene().addEventHandler(KeyEvent.KEY_PRESSED, cancelPatternSelectKeyHandler);
        ApplicationState.getInstance().getPrimaryStage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, cancelPatternSelectMouseHandler);
    }

    public void closeCurrentPatternSelector() {
        ApplicationState.getInstance().getPrimaryStage().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, cancelPatternSelectKeyHandler);
        ApplicationState.getInstance().getPrimaryStage().getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, cancelPatternSelectMouseHandler);

        fadeOut(currentPatternSelector, (ActionEvent ev) -> {
            if (currentPatternSelector != null) {
                Pane parent = (Pane) currentPatternSelector.getParent();
                parent.getChildren().remove(currentPatternSelector);
                currentPatternSelector = null;
            }
        });
    }

    private final EventHandler<MouseEvent> cancelPatternSelectMouseHandler = (MouseEvent e) -> {
        if (!(e.getSource() instanceof ImageView)) {
            e.consume();
        }
        closeCurrentPatternSelector();
    };

    private final EventHandler<KeyEvent> cancelPatternSelectKeyHandler = (KeyEvent e) -> {
        if (e.getCode() == KeyCode.ESCAPE) {
            closeCurrentPatternSelector();
        }
    };
}
