/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor.ui;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.FileUtils;
import org.badvision.outlaweditor.MythosEditor;
import org.badvision.outlaweditor.SheetEditor;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.apple.ImageDitherEngine;
import org.badvision.outlaweditor.data.DataUtilities;
import org.badvision.outlaweditor.data.TilesetUtils;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Scope;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Sheet;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.data.xml.Variable;
import org.badvision.outlaweditor.data.xml.Variables;
import org.badvision.outlaweditor.ui.impl.ImageConversionWizardController;

import jakarta.xml.bind.JAXB;
import javafx.animation.FadeTransition;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;

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
                ApplicationState.getInstance().setGameData(newData);
                DataUtilities.ensureGlobalExists();
                DataUtilities.cleanupAllScriptNames();
                ApplicationUIController.getController().updateSelectors();
                DataUtilities.logDataStructure(newData);
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
                    JAXB.marshal(ApplicationState.getInstance().getGameData(), currentSaveFile);
                }
                break;
            default:
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
        Application.shutdown();
        Platform.runLater(Platform::exit);
    }

    static Image badImage;

    public static WritableImage getBadImage(int width, int height) {
        if (badImage == null) {
            badImage = new Image(UIAction.class.getResourceAsStream("/images/icon_brokenLink.png"));
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

    public static void alert(String message) {
        choose(message, new Choice("Ok", null));
    }

    public static void choose(String message, Choice... choices) {
        final Stage dialogStage = new Stage();

        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.setSpacing(10.0);
        hbox.setPadding(new Insets(5));
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
        hbox.getChildren().addAll(buttons);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        VBox vbox = new VBox();
        vbox.getChildren().addAll(new Text(message), hbox);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(5));
        dialogStage.setScene(new Scene(vbox));
        dialogStage.show();
    }

    static public String getText(String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("MythosScript Editor");
        dialog.setHeaderText("Respond and press OK, or Cancel to abort");
        ImageView graphic = new ImageView(new Image(UIAction.class.getResourceAsStream("/images/revolver_icon.png")));
        graphic.setFitHeight(50.0);
        graphic.setFitWidth(50.0);
        graphic.setSmooth(true);
        dialog.setGraphic(graphic);
        dialog.setContentText(message);
        return dialog.showAndWait().orElse("");
    }

    public static Script createAndEditScript(Scope scope) {
        Script script = new Script();
        script.setName("New Script");
        ApplicationUIController.getController().getVisibleEditor().addScript(script);
        return editScript(script, scope);
    }

    public static Script editScript(Script script, Scope scope) {
        if (script == null) {
            System.err.println("Requested to edit a null script object, ignoring!");
            return null;
        }
        MythosEditor editor = new MythosEditor(script, scope);
        editor.show();
        return script;
    }

    public static void createAndEditVariable(Scope scope) throws IntrospectionException {
        Variable newVariable = new Variable();
        newVariable.setName("changeme");
        newVariable.setType("String");
        newVariable.setComment("");
        Optional<Variable> var = editAndGetVariable(newVariable);
        if (var.isPresent()) {
            if (scope.getVariables() == null) {
                scope.setVariables(new Variables());
            }
            scope.getVariables().getVariable().add(var.get());
        }
    }

    public static void editVariable(Variable var, Global global) {
        try {
            editAndGetVariable(var);
        } catch (IntrospectionException ex) {
            Logger.getLogger(UIAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Track open editors
    static Map<Variable, ModalEditor> editors = new LinkedHashMap<>();

    public static Optional<Variable> editAndGetVariable(Variable v) throws IntrospectionException {
        // Check if editor is already open
        if (editors.containsKey(v)) {
            editors.get(v);
            if (editors.get(v).isOpen()) {
                return Optional.empty();
            }
        }
        ModalEditor editor = new ModalEditor();
        editors.put(v, editor);
        Map<String, ModalEditor.EditControl> controls = new LinkedHashMap<>();

        controls.put("name", new ModalEditor.TextControl());
        controls.put("type", new ModalEditor.TextControl());
        controls.put("comment", new ModalEditor.TextControl());

        return editor.editObject(v, controls, Variable.class, "Variable", "Edit and press OK, or Cancel to abort");
    }

    public static void createAndEditUserType() throws IntrospectionException {
        UserType type = new UserType();
        if (editAndGetUserType(type).isPresent()) {
            if (ApplicationState.getInstance().getGameData().getGlobal().getUserTypes() == null) {
                ApplicationState.getInstance().getGameData().getGlobal().setUserTypes(new Global.UserTypes());
            }
            ApplicationState.getInstance().getGameData().getGlobal().getUserTypes().getUserType().add(type);
        }
    }

    public static void editUserType(UserType type) {
        try {
            editAndGetUserType(type);
        } catch (IntrospectionException ex) {
            Logger.getLogger(UIAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Optional<UserType> editAndGetUserType(UserType type) throws IntrospectionException {
        ModalEditor editor = new ModalEditor();
        Map<String, ModalEditor.EditControl> controls = new LinkedHashMap<>();

        Map<String, Callback<TableColumn<
                Variable, String>, TableCell<Variable, String>>> attributeControls = new LinkedHashMap<>();

        attributeControls.put("name", TextFieldTableCell.<Variable, String>forTableColumn(new DefaultStringConverter() {
            @Override
            public String toString(String value) {
                return value == null ? "Change Me" : value;
            }
        }));
        attributeControls.put("type", ComboBoxTableCell.<Variable, String>forTableColumn(new DefaultStringConverter() {
            @Override
            public String toString(String value) {
                return value == null ? "String" : value;
            }
        }, "String", "Boolean", "Number"));
        attributeControls.put("comment", TextFieldTableCell.<Variable, String>forTableColumn(new DefaultStringConverter()));

        controls.put("name", new ModalEditor.TextControl());
        controls.put("attribute", new ModalEditor.TableControl(attributeControls, Variable.class));
        controls.put("comment", new ModalEditor.TextControl());

        return editor.editObject(type, controls, UserType.class, "User Type", "Edit and press OK, or Cancel to abort");
    }

    public static Sheet createAndEditSheet() throws IntrospectionException {
        Sheet sheet = new Sheet();
        sheet.setName("New Sheet");
        if (ApplicationState.getInstance().getGameData().getGlobal().getSheets() == null) {
            ApplicationState.getInstance().getGameData().getGlobal().setSheets(new Global.Sheets());
        }
        ApplicationState.getInstance().getGameData().getGlobal().getSheets().getSheet().add(sheet);
        return editSheet(sheet);
    }

    static Map<Sheet, SheetEditor> sheetEditors = new LinkedHashMap<>();
    public static Sheet editSheet(Sheet item) {
        if (item == null) {
            System.err.println("Requested to edit a null sheet object, ignoring!");
            return null;
        }
        // Check if we don't already have an open editor first
        if (sheetEditors.containsKey(item) && sheetEditors.get(item).isShowing()) {
            sheetEditors.get(item).toFront();
            return item;
        }
        SheetEditor editor = new SheetEditor(item);
        sheetEditors.put(item, editor);
        editor.show();
        return item;
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

    public static void fadeOut(Node node, EventHandler<ActionEvent> callback) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        ft.setOnFinished(callback);
        ft.play();
    }
    
    public static File getCurrentSaveFile() {
        return currentSaveFile;
    }

    private UIAction() {
    }
}
