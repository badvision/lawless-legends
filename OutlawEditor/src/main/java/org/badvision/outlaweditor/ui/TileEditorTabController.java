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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.TileEditor;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author blurry
 */
public abstract class TileEditorTabController {

    private Tile currentTile;

    public Tile getCurrentTile() {
        return currentTile;
    }

    public void setCurrentTile(Tile tile) {
        currentTile = tile;
    }

    private TileEditor currentEditor;

    public TileEditor getCurrentTileEditor() {
        return currentEditor;
    }

    public void setCurrentTileEditor(TileEditor editor) {
        currentEditor = editor;
    }

    @FXML // fx:id="tileCategoryField"
    protected TextField tileCategoryField; // Value injected by FXMLLoader
    @FXML // fx:id="tileEditorAnchorPane"
    protected AnchorPane tileEditorAnchorPane; // Value injected by FXMLLoader
    @FXML // fx:id="tileIdField"
    protected TextField tileIdField; // Value injected by FXMLLoader
    @FXML // fx:id="tileNameField"
    protected TextField tileNameField; // Value injected by FXMLLoader
    @FXML // fx:id="tileObstructionField"
    protected CheckBox tileObstructionField; // Value injected by FXMLLoader
    @FXML // fx:id="tilePatternMenu"
    protected Menu tilePatternMenu; // Value injected by FXMLLoader
    @FXML // fx:id="tileSelector"
    protected ComboBox<Tile> tileSelector; // Value injected by FXMLLoader
    @FXML
    protected CheckBox tileBlockerField;

    @FXML
    protected CheckBox tileSpriteField;

    @FXML
    abstract public void onCurrentTileSelected(ActionEvent event);

    @FXML
    abstract public void onTileCreatePressed(ActionEvent event);

    @FXML
    abstract public void onTileExportPressed(ActionEvent event);

    @FXML
    abstract public void onTileClonePressed(ActionEvent event);

    @FXML
    abstract public void onTileDeletePressed(ActionEvent event);

    @FXML
    abstract public void tileBitMode(ActionEvent event);

    @FXML
    abstract public void tileDraw1BitMode(ActionEvent event);

    @FXML
    abstract public void tileDraw3BitMode(ActionEvent event);

    @FXML
    abstract public void tileShift(ActionEvent event);

    abstract public void rebuildTileSelectors();

    public void initalize() {
        assert tileSpriteField != null : "fx:id=\"tileSpriteField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileCategoryField != null : "fx:id=\"tileCategoryField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileSelector != null : "fx:id=\"tileSelector\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tilePatternMenu != null : "fx:id=\"tilePatternMenu\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileBlockerField != null : "fx:id=\"tileBlockerField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileNameField != null : "fx:id=\"tileNameField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileEditorAnchorPane != null : "fx:id=\"tileEditorAnchorPane\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileIdField != null : "fx:id=\"tileIdField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
        assert tileObstructionField != null : "fx:id=\"tileObstructionField\" was not injected: check your FXML file 'tileEditorTab.fxml'.";
       
    }
}
