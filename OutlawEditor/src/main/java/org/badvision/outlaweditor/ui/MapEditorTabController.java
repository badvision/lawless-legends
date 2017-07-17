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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.MapEditor;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;

/**
 *
 * @author blurry
 */
public abstract class MapEditorTabController {

    private MapEditor currentEditor;

    public MapEditor getCurrentEditor() {
        return currentEditor;
    }

    public void setCurrentEditor(MapEditor editor) {
        currentEditor = editor;
    }

    public abstract Map getCurrentMap();

    public abstract void setCurrentMap(Map m);

    public abstract void rebuildMapSelectors();

    public abstract void redrawMapScripts();

    @FXML // fx:id="mapEditorAnchorPane"
    protected AnchorPane mapEditorAnchorPane; // Value injected by FXMLLoader
    @FXML // fx:id="mapNameField"
    protected TextField mapNameField; // Value injected by FXMLLoader
    @FXML // fx:id="mapScriptsList"
    protected ListView<Script> mapScriptsList; // Value injected by FXMLLoader
    @FXML // fx:id="mapSelect"
    protected ComboBox<Map> mapSelect; // Value injected by FXMLLoader
    @FXML
    protected Menu mapSelectTile;
    @FXML // fx:id="mapOrderField"
    protected TextField mapOrderField; // Value injected by FXMLLoader
    @FXML // fx:id="mapDisplay3dField"
    protected CheckBox mapDisplay3dField; // Value injected by FXMLLoader
    @FXML
    protected Button scriptEraseTool;
    @FXML
    protected Label cursorInfo;
    
    @FXML
    abstract public void mapEraser(ActionEvent event);
    
    @FXML
    abstract public void mapDraw1(ActionEvent event);

    @FXML
    abstract public void mapDraw3(ActionEvent event);

    @FXML
    abstract public void mapDraw5(ActionEvent event);

    @FXML
    abstract public void mapDrawFilledRectMode(ActionEvent event);

    @FXML
    abstract public void mapScriptPaint(ActionEvent event);
    
    @FXML
    abstract public void mapScriptErasor(ActionEvent event);
    
    @FXML
    abstract public void mapTogglePanZoom(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomInButton]] onAction
    @FXML
    abstract public void mapZoomIn(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomOutButton]] onAction
    @FXML
    abstract public void mapZoomOut(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapClonePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapCreatePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapDeletePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapExportPressed(ActionEvent event);

    @FXML
    abstract public void onMapPreviewPressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapScriptAddPressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapScriptClonePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapScriptDeletePressed(ActionEvent event);

    // Handler for ComboBox[id="tileSelect"] onAction
    @FXML
    abstract public void onMapSelected(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollMapDown(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollMapLeft(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollMapRight(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollMapUp(ActionEvent event);

    public void initalize() {
        assert mapEditorAnchorPane != null : "fx:id=\"mapEditorAnchorPane\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapNameField != null : "fx:id=\"mapNameField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapScriptsList != null : "fx:id=\"mapScriptsList\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapSelect != null : "fx:id=\"mapSelect\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapSelectTile != null : "fx:id=\"mapSelectTile\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapOrderField != null : "fx:id=\"mapOrderField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert cursorInfo != null : "fx:id=\"cursorInfo\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert mapDisplay3dField != null : "fx:id=\"mapDisplay3dField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
    }

    abstract public void rebuildTileSelectors();

    public void completeInflightOperations() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
