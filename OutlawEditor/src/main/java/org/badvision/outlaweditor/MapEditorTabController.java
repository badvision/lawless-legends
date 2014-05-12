package org.badvision.outlaweditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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
    @FXML // fx:id="mapHeightField"
    protected TextField mapHeightField; // Value injected by FXMLLoader
    @FXML // fx:id="mapNameField"
    protected TextField mapNameField; // Value injected by FXMLLoader
    @FXML // fx:id="mapScriptsList"
    protected ListView<Script> mapScriptsList; // Value injected by FXMLLoader
    @FXML // fx:id="mapSelect"
    protected ComboBox<Map> mapSelect; // Value injected by FXMLLoader
    @FXML
    protected Menu mapSelectTile;
    @FXML // fx:id="mapWidthField"
    protected TextField mapWidthField; // Value injected by FXMLLoader
    @FXML // fx:id="mapWrapAround"
    protected CheckBox mapWrapAround; // Value injected by FXMLLoader

    @FXML
    abstract public void mapDraw1(ActionEvent event);

    @FXML
    abstract public void mapDraw3(ActionEvent event);

    @FXML
    abstract public void mapDraw5(ActionEvent event);

    @FXML
    abstract public void mapDrawFilledRectMode(ActionEvent event);

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

    protected void initalize() {
        assert mapEditorAnchorPane != null : "fx:id=\"mapEditorAnchorPane\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapHeightField != null : "fx:id=\"mapHeightField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapNameField != null : "fx:id=\"mapNameField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapScriptsList != null : "fx:id=\"mapScriptsList\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapSelect != null : "fx:id=\"mapSelect\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapSelectTile != null : "fx:id=\"mapSelectTile\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapWidthField != null : "fx:id=\"mapWidthField\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
        assert mapWrapAround != null : "fx:id=\"mapWrapAround\" was not injected: check your FXML file 'mapEditorTab.fxml'.";
    }

    abstract void rebuildTileSelectors();
}
