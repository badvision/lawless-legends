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
import javafx.scene.control.ListView;
import org.badvision.outlaweditor.GlobalEditor;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.data.xml.Variable;

public abstract class GlobalEditorTabController {
    private final GlobalEditor currentEditor = new GlobalEditor();

    public GlobalEditor getCurrentEditor() {
        return currentEditor;
    }
    @FXML
    protected ListView<Script> globalScriptList;
    @FXML
    protected ListView<UserType> dataTypeList;
    @FXML
    protected ListView<Variable> variableList;

    @FXML
    abstract protected void onScriptAddPressed(ActionEvent event);
    @FXML
    abstract protected void onScriptDeletePressed(ActionEvent event);
    @FXML
    abstract protected void onScriptClonePressed(ActionEvent event);
    @FXML
    abstract protected void onDataTypeAddPressed(ActionEvent event);
    @FXML
    abstract protected void onDataTypeDeletePressed(ActionEvent event);
    @FXML
    abstract protected void onDataTypeClonePressed(ActionEvent event);
    @FXML
    abstract protected void onVariableAddPressed(ActionEvent event);
    @FXML
    abstract protected void onVariableDeletePressed(ActionEvent event);
    @FXML
    abstract protected void onVariableClonePressed(ActionEvent event);
    
    abstract public void redrawGlobalScripts();

    abstract public void redrawGlobalVariables();

    abstract public void redrawGlobalDataTypes();
    
    public void initialize() {
        redrawGlobalScripts();
        redrawGlobalVariables();
        redrawGlobalDataTypes();
    }    

}
