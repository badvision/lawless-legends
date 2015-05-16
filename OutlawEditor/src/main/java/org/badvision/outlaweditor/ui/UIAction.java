/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javax.xml.bind.JAXB;
import org.badvision.outlaweditor.Application;
import static org.badvision.outlaweditor.Application.currentPlatform;
import org.badvision.outlaweditor.FileUtils;
import org.badvision.outlaweditor.MythosEditor;
import org.badvision.outlaweditor.apple.ImageDitherEngine;
import org.badvision.outlaweditor.data.TileUtils;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.impl.ImageConversionWizardController;

/**
 *
 * @author brobert
 */
public class UIAction {

    private static File currentSaveFile;

    public static enum MAIN_ACTIONS {

        _General,
        Load, Save, Save_as, Apple_Mode, C64_Mode, About_Outlaw_Editor, Quit,
        _Map,
        Create_new_map, Export_map_to_image,
        _Tiles,
        Create_new_tile, Export_tile_to_image, Delete_tile,
        _Images,
        Create_new_image, Import_image, Export_image_as_raw, Delete_image;
    };

    public static void actionPerformed(MAIN_ACTIONS action) throws IOException {
        switch (action) {
            case About_Outlaw_Editor:
                break;
            case Apple_Mode:
                break;
            case C64_Mode:
                break;
            case Delete_tile:
//                if (currentSelectedTile != null) {
//                    Tile t = currentSelectedTile;
//                    selectTile(null, null);
//                    TilesetUtils.remove(t);
//                    Application.instance.redrawTileSelector();
//                    Application.instance.rebuildMapEditor();
//                }
                break;
            case Export_map_to_image:
                break;
            case Export_tile_to_image:
                break;
            case Load:
                File f = FileUtils.getFile(currentSaveFile, "Load game data", Boolean.FALSE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                if (f == null) {
                    return;
                }
                currentSaveFile = f;
                GameData newData = JAXB.unmarshal(currentSaveFile, GameData.class);
                ApplicationUIController.getController().clearData();
                TilesetUtils.clear();
                Application.gameData = newData;
                ApplicationUIController.getController().updateSelectors();
                break;
            case Quit:
                quit();
                break;
            case Save_as:
                f = FileUtils.getFile(currentSaveFile, "Save game data", Boolean.TRUE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                if (f == null) {
                    return;
                }
                currentSaveFile = f;
            case Save:
                if (currentSaveFile == null) {
                    currentSaveFile = FileUtils.getFile(currentSaveFile, "Save game data", Boolean.TRUE, FileUtils.Extension.XML, FileUtils.Extension.ALL);
                }
                if (currentSaveFile != null) {
                    currentSaveFile.delete();
                    JAXB.marshal(Application.gameData, currentSaveFile);
                }
                break;
        }
    }

    public static MenuBar buildMenu() {
        MenuBar menu = new MenuBar();
        Menu currentMenu = null;
        for (final MAIN_ACTIONS action : UIAction.MAIN_ACTIONS.values()) {
            if (action.name().startsWith("_")) {
                if (currentMenu != null) {
                    menu.getMenus().add(currentMenu);
                }
                currentMenu = new Menu(action.name().replace("_", ""));
            } else {
                MenuItem item = new MenuItem(action.name().replaceAll("_", " "));
                item.setOnAction((ActionEvent t) -> {
                    try {
                        actionPerformed(action);
                    } catch (IOException ex) {
                        Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                currentMenu.getItems().add(item);
            }
        }
        menu.getMenus().add(currentMenu);
        menu.setMinWidth(1.0);
        return menu;
    }

    public static void quit() {
        confirm("Quit?  Are you sure?", UIAction::quitWithoutConfirming, null);
    }

    public static void quitWithoutConfirming() {
        Platform.runLater(Platform::exit);
    }

    static Image badImage;

    public static WritableImage getBadImage(int width, int height) {
        if (badImage == null) {
            badImage = new Image("images/icon_brokenLink.png");
        }
        WritableImage img = new WritableImage(width, height);
        img.getPixelWriter().setPixels(0, 0, (int) badImage.getWidth(), (int) badImage.getHeight(), badImage.getPixelReader(), 0, 0);

        return img;
    }

    public static class Choice {

        String text;
        Runnable handler;

        public Choice(String text, Runnable handler) {
            this.text = text;
            this.handler = handler;
        }
    }

    public static void confirm(String message, Runnable yes, Runnable no) {
        choose(message, new Choice("Yes", yes), new Choice("No", no));
    }

    public static void choose(String message, Choice... choices) {
        final Stage dialogStage = new Stage();

        HBoxBuilder options = HBoxBuilder.create().alignment(Pos.CENTER).spacing(10.0).padding(new Insets(5));
        List<Button> buttons = new ArrayList<>();
        for (final Choice c : choices) {
            Button b = new Button(c.text);
            b.setOnAction((ActionEvent t) -> {
                if (c.handler != null) {
                    c.handler.run();
                }
                dialogStage.close();
            });
            buttons.add(b);
        }
        options.children(buttons);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(new Scene(VBoxBuilder.create().
                children(new Text(message), options.build()).
                alignment(Pos.CENTER).padding(new Insets(5)).build()));
        dialogStage.show();
    }

    public static Script createAndEditScript() {
        Script script = new Script();
        script.setName("New Script");
        ApplicationUIController.getController().getVisibleEditor().addScript(script);
        return editScript(script);
    }

    public static Script editScript(Script script) {
        if (script == null) {
            System.err.println("Requested to edit a null script object, ignoring!");
            return null;
        }
        MythosEditor editor = new MythosEditor(script);
        editor.show();
        return script;
    }

    public static ImageConversionWizardController openImageConversionModal(Image image, ImageDitherEngine ditherEngine, int targetWidth, int targetHeight, ImageConversionPostAction postAction) {
        FXMLLoader fxmlLoader = new FXMLLoader(UIAction.class.getResource("/imageConversionWizard.fxml"));
        try {
            Stage primaryStage = new Stage();
            AnchorPane node = (AnchorPane) fxmlLoader.load();
            ImageConversionWizardController controller = fxmlLoader.getController();
            controller.setDitherEngine(ditherEngine);
            controller.setOutputDimensions(targetWidth, targetHeight);
            controller.setPostAction(postAction);
            controller.setSourceImage(image);
            Scene s = new Scene(node);
            primaryStage.setScene(s);
            primaryStage.show();
            controller.setStage(primaryStage);
            return controller;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static final int GRID_SPACING = 7;
    public static final int MAX_TILES_PER_ROW = 16;
    public static AnchorPane currentTileSelector;

    public static void showTileSelectModal(Pane anchorPane, String category, Callback<Tile,?> callback) {
        if (currentTileSelector != null) {
            return;
        }
        currentTileSelector = new AnchorPane();

        int TILE_WIDTH = Application.currentPlatform.tileRenderer.getWidth();
        int TILE_HEIGHT = Application.currentPlatform.tileRenderer.getHeight();

        List<Tile> tiles = Application.gameData.getTile().stream().filter((Tile t) -> {
            return category == null || t.getCategory().equals(category);
        }).collect(Collectors.toList());

        int tilesPerRow = (int) Math.min(tiles.size(), Math.min(MAX_TILES_PER_ROW, anchorPane.getWidth() / (TILE_WIDTH + GRID_SPACING)));
        int numRows = (tiles.size() + tilesPerRow - 1) / tilesPerRow;
        int prefWidth = tilesPerRow * (TILE_WIDTH + GRID_SPACING) + GRID_SPACING;
        currentTileSelector.setPrefWidth(prefWidth);
        currentTileSelector.setPrefHeight(Math.min(numRows * (TILE_HEIGHT + GRID_SPACING) + GRID_SPACING, prefWidth));
        for (int i = 0; i < tiles.size(); i++) {
            final Tile tile = tiles.get(i);
            ImageView tileIcon = new ImageView(TileUtils.getImage(tile, currentPlatform));
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
        Application.getPrimaryStage().getScene().addEventHandler(KeyEvent.KEY_PRESSED, cancelTileSelectKeyHandler);
        Application.getPrimaryStage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, cancelTileSelectMouseHandler);
    }

    private static final EventHandler<MouseEvent> cancelTileSelectMouseHandler = (MouseEvent e) -> {
        if (! (e.getSource() instanceof ImageView)) {
            e.consume();
        }
        closeCurrentTileSelector();
    };
    
    private static final EventHandler<KeyEvent> cancelTileSelectKeyHandler = (KeyEvent e) -> {
        if (e.getCode() == KeyCode.ESCAPE) {
            closeCurrentTileSelector();
        }
    };

    public static void closeCurrentTileSelector() {
        Application.getPrimaryStage().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, cancelTileSelectKeyHandler);
        Application.getPrimaryStage().getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, cancelTileSelectMouseHandler);

        fadeOut(currentTileSelector, (ActionEvent ev) -> {
            if (currentTileSelector != null) {
                Pane parent = (Pane) currentTileSelector.getParent();
                parent.getChildren().remove(currentTileSelector);
                currentTileSelector = null;
            }
        });
    }

    public static void fadeOut(Node node, EventHandler<ActionEvent> callback) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.setOnFinished(callback);
        ft.play();
    }
}
