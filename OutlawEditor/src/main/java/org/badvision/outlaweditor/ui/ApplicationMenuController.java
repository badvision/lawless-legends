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
import javafx.scene.control.Menu;

/**
 *
 * @author blurry
 */
public abstract class ApplicationMenuController {

    @FXML
    protected Menu extraMenu;
    
    @FXML
    abstract public void onChangePlatformAppleDHGRSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleDHGRText(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleSolid(ActionEvent event);

    @FXML
    abstract public void onChangePlatformAppleText(ActionEvent event);

    @FXML
    abstract public void onChangePlatformC64(ActionEvent event);

    @FXML
    abstract public void onEditCopy(ActionEvent event);

    @FXML
    abstract public void onEditCopyData(ActionEvent event);

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

    @FXML
    abstract public void performUndo(ActionEvent event);

    @FXML
    abstract public void onToolsAuditFlags(ActionEvent event);

    abstract public void initalize();
}
