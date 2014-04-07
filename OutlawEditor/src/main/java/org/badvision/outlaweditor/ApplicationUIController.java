package org.badvision.outlaweditor;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.data.xml.Image;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;

public abstract class ApplicationUIController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    protected ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    protected URL location;
    @FXML // fx:id="imageCategoryField"
    protected TextField imageCategoryField; // Value injected by FXMLLoader
    @FXML // fx:id="imageEditorAnchorPane"
    protected AnchorPane imageEditorAnchorPane; // Value injected by FXMLLoader
    @FXML // fx:id="imageHeightField"
    protected TextField imageHeightField; // Value injected by FXMLLoader
    @FXML // fx:id="imageNameField"
    protected TextField imageNameField; // Value injected by FXMLLoader
    @FXML // fx:id="imagePatternMenu"
    protected Menu imagePatternMenu; // Value injected by FXMLLoader
    @FXML // fx:id="imageSelector"
    protected ComboBox<Image> imageSelector; // Value injected by FXMLLoader
    @FXML // fx:id="imageWidthField"
    protected TextField imageWidthField; // Value injected by FXMLLoader
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

    // Handler for MenuItem[javafx.scene.control.MenuItem@3a4bc91a] onAction
    @FXML
    abstract public void imageBitMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@2fd6cf0] onAction
    @FXML
    abstract public void imageDraw1BitMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@2fd4f37f] onAction
    @FXML
    abstract public void imageDraw3BitMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@73091451] onAction
    @FXML
    abstract public void imageDrawFilledRectMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@5768f863] onAction
    @FXML
    abstract public void imageShift(ActionEvent event);

    @FXML
    abstract public void imageTabActivated(Event event);
    
    // Handler for MenuItem[javafx.scene.control.MenuItem@547638c0] onAction
    @FXML
    abstract public void imageTogglePanZoom(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomInButton]] onAction
    @FXML
    abstract public void imageZoomIn(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomOutButton]] onAction
    @FXML
    abstract public void imageZoomOut(ActionEvent event);

    @FXML
    abstract public void mapDraw1(ActionEvent event);

    @FXML
    abstract public void mapDraw3(ActionEvent event);

    @FXML
    abstract public void mapDraw5(ActionEvent event);

    @FXML
    abstract public void mapDrawFilledRectMode(ActionEvent event);

    @FXML
    abstract public void mapTabActivated(Event event);

    @FXML
    abstract public void mapTogglePanZoom(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomInButton]] onAction
    @FXML
    abstract public void mapZoomIn(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomOutButton]] onAction
    @FXML
    abstract public void mapZoomOut(ActionEvent event);

    // Handler for ComboBox[fx:id="tileSelector"] onAction
    @FXML
    abstract public void onCurrentTileSelected(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleText(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleDHGRSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleDHGRText(ActionEvent event);
    
    @FXML
    abstract public void onChangePlatformC64(ActionEvent event);

    @FXML
    abstract public void onEditCopy(ActionEvent event);

    @FXML
    abstract public void onEditPaste(ActionEvent event);

    @FXML
    abstract public void onEditSelect(ActionEvent event);

    @FXML
    abstract public void onFileOpen(ActionEvent event);

    @FXML
    abstract public void onFileQuit(ActionEvent event);

    @FXML
    abstract public void onFileSave(ActionEvent event);

    @FXML
    abstract public void onFileSaveAs(ActionEvent event);

    @FXML
    abstract public void onHelpAbout(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onImageClonePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onImageCreatePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onImageDeletePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onImageExportPressed(ActionEvent event);

    // Handler for ComboBox[fx:id="imageSelector"] onAction
    @FXML
    abstract public void onImageSelected(ActionEvent event);

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

    // Handler for onClick
    @FXML
    abstract public void onMapScriptClicked(MouseEvent event);
    
    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onMapScriptDeletePressed(ActionEvent event);

    // Handler for ComboBox[id="tileSelect"] onAction
    @FXML
    abstract public void onMapSelected(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onTileClonePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onTileCreatePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onTileDeletePressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button]] onAction
    @FXML
    abstract public void onTileExportPressed(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollImageDown(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollImageLeft(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollImageRight(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button moveButton]] onAction
    @FXML
    abstract public void scrollImageUp(ActionEvent event);

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

    // Handler for MenuItem[javafx.scene.control.MenuItem@3b007e44] onAction
    @FXML
    abstract public void tileBitMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@4771c0b8] onAction
    @FXML
    abstract public void tileDraw1BitMode(ActionEvent event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@766bd19d] onAction
    @FXML
    abstract public void tileDraw3BitMode(ActionEvent event);

    @FXML
    abstract public void imageDraw5BitMode(ActionEvent event);

    @FXML
    abstract public void tileTabActivated(Event event);

    // Handler for MenuItem[javafx.scene.control.MenuItem@622410f1] onAction
    @FXML
    abstract public void tileShift(ActionEvent event);

    @FXML // This method is called by the FXMLLoader when initialization is complete
    public void initialize() {
        assert imageCategoryField != null : "fx:id=\"imageCategoryField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageEditorAnchorPane != null : "fx:id=\"imageEditorAnchorPane\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageHeightField != null : "fx:id=\"imageHeightField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageNameField != null : "fx:id=\"imageNameField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imagePatternMenu != null : "fx:id=\"imagePatternMenu\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageSelector != null : "fx:id=\"imageSelector\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageWidthField != null : "fx:id=\"imageWidthField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapEditorAnchorPane != null : "fx:id=\"mapEditorAnchorPane\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapHeightField != null : "fx:id=\"mapHeightField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapNameField != null : "fx:id=\"mapNameField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapScriptsList != null : "fx:id=\"mapScriptsList\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapSelect != null : "fx:id=\"mapSelect\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapSelectTile != null : "fx:id=\"mapSelectTile\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapWidthField != null : "fx:id=\"mapWidthField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert mapWrapAround != null : "fx:id=\"mapWrapAround\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileCategoryField != null : "fx:id=\"tileCategoryField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileEditorAnchorPane != null : "fx:id=\"tileEditorAnchorPane\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileIdField != null : "fx:id=\"tileIdField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileNameField != null : "fx:id=\"tileNameField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileObstructionField != null : "fx:id=\"tileObstructionField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tilePatternMenu != null : "fx:id=\"tilePatternMenu\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert tileSelector != null : "fx:id=\"tileSelector\" was not injected: check your FXML file 'ApplicationUI.fxml'.";

        // Initialize your logic here: all @FXML variables will have been injected

    }
}
