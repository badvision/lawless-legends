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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.Event;
import javafx.fxml.FXML;
import org.badvision.outlaweditor.Editor;
import org.badvision.outlaweditor.api.ApplicationState;

public abstract class ApplicationUIController {

    public static ApplicationUIController getController() {
        return ApplicationState.getInstance().getController();
    }

    abstract public void rebuildTileSelectors();

    abstract public void rebuildMapSelectors();

    abstract public void rebuildImageSelectors();

    public abstract Editor getVisibleEditor();

    @FXML // ResourceBundle that was given to the FXMLLoader
    protected ResourceBundle resources;
    @FXML
    protected ApplicationMenuController menuController;
    @FXML
    protected TileEditorTabController tileController;
    @FXML
    protected MapEditorTabController mapController;
    @FXML
    protected ImageEditorTabController imageController;
    @FXML
    protected GlobalEditorTabController globalController;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    protected URL location;

    abstract public void platformChange();

    @FXML
    abstract public void tileTabActivated(Event event);

    @FXML
    abstract public void mapTabActivated(Event event);

    @FXML
    abstract public void imageTabActivated(Event event);

    @FXML
    abstract public void globalTabActivated(Event event);

    @FXML // This method is called by the FXMLLoader when initialization is complete
    public void initialize() {
    }

    abstract public void completeInflightOperations();

    abstract public void clearData();

    abstract public void updateSelectors();

    abstract public void redrawScripts();
}
