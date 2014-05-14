/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.badvision.outlaweditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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
    
}
