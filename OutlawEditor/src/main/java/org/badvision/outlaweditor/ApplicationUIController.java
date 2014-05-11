package org.badvision.outlaweditor;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.data.xml.Image;

public abstract class ApplicationUIController {

    public static ApplicationUIController getController() {
        return Application.instance.controller;
    }

    abstract void rebuildTileSelectors();

    abstract void rebuildImageSelector();

    abstract Editor getVisibleEditor();

    @FXML // ResourceBundle that was given to the FXMLLoader
    protected ResourceBundle resources;
    @FXML
    ApplicationMenuController menuController;
    @FXML
    TileEditorTabController tileController;
    @FXML
    MapEditorTabController mapController;

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

    // Handler for MenuItem[javafx.scene.control.MenuItem@547638c0] onAction
    @FXML
    abstract public void imageTogglePanZoom(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomInButton]] onAction
    @FXML
    abstract public void imageZoomIn(ActionEvent event);

    // Handler for Button[Button[id=null, styleClass=button zoomOutButton]] onAction
    @FXML
    abstract public void imageZoomOut(ActionEvent event);

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

    abstract public void platformChange();

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

    @FXML
    abstract public void imageDraw5BitMode(ActionEvent event);

    @FXML
    abstract public void tileTabActivated(Event event);

    @FXML
    abstract public void mapTabActivated(Event event);

    @FXML
    abstract public void imageTabActivated(Event event);

    @FXML // This method is called by the FXMLLoader when initialization is complete
    public void initialize() {
        assert imageCategoryField != null : "fx:id=\"imageCategoryField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageEditorAnchorPane != null : "fx:id=\"imageEditorAnchorPane\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageHeightField != null : "fx:id=\"imageHeightField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageNameField != null : "fx:id=\"imageNameField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imagePatternMenu != null : "fx:id=\"imagePatternMenu\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageSelector != null : "fx:id=\"imageSelector\" was not injected: check your FXML file 'ApplicationUI.fxml'.";
        assert imageWidthField != null : "fx:id=\"imageWidthField\" was not injected: check your FXML file 'ApplicationUI.fxml'.";

        // Initialize your logic here: all @FXML variables will have been injected
    }

    abstract void completeInflightOperations();
}
