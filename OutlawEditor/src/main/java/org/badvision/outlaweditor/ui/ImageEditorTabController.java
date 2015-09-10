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
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.data.xml.Image;

/**
 *
 * @author blurry
 */
public abstract class ImageEditorTabController {

    @FXML // fx:id="imageCategoryField"
    protected TextField imageCategoryField; // Value injected by FXMLLoader
    @FXML // fx:id="imageEditorAnchorPane"
    protected AnchorPane imageEditorAnchorPane; // Value injected by FXMLLoader
    @FXML
    protected ScrollPane imageEditorScrollPane;
    @FXML
    protected Group imageEditorZoomGroup;
    @FXML
    protected AnchorPane imageEditorScrollAnchorPane;
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
    @FXML
    protected Label zoomLabel;
    
    @FXML
    public void initalize() {
        assert imageCategoryField != null : "fx:id=\"imageCategoryField\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageEditorAnchorPane != null : "fx:id=\"imageEditorAnchorPane\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageHeightField != null : "fx:id=\"imageHeightField\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageNameField != null : "fx:id=\"imageNameField\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imagePatternMenu != null : "fx:id=\"imagePatternMenu\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageSelector != null : "fx:id=\"imageSelector\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageWidthField != null : "fx:id=\"imageWidthField\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
        assert imageEditorScrollPane != null : "fx:id\"imageEditorScrollPane\" was not injected: check your FXML file 'imageEditorTab.fxml'";
        assert imageEditorZoomGroup != null : "fx:id\"imageEditorZoomGroup\" was not injected: check your FXML file 'imageEditorTab.fxml'";
        assert imageEditorScrollAnchorPane != null : "fx:id\"imageEditorScrollAnchorPane\" was not injected: check your FXML file 'imageEditorTab.fxml'";
        assert zoomLabel != null : "fx:id=\"zoomLabel\" was not injected: check your FXML file 'imageEditorTab.fxml'.";
    }

    abstract public void rebuildImageSelector();

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


    @FXML
    abstract public void imageDraw5BitMode(ActionEvent event);

    abstract public Editor getCurrentEditor();
}
