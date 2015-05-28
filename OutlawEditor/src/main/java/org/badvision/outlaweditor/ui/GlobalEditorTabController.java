package org.badvision.outlaweditor.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import org.badvision.outlaweditor.GlobalEditor;
import org.badvision.outlaweditor.data.xml.Script;

public abstract class GlobalEditorTabController {
    private final GlobalEditor currentEditor = new GlobalEditor();

    public GlobalEditor getCurrentEditor() {
        return currentEditor;
    }
    @FXML
    protected ListView<Script> globalScriptList;
    @FXML
    protected ListView<?> dataTypeList;
    @FXML
    protected ListView<?> variableList;

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
    abstract protected void onDeleteClonePressed(ActionEvent event);
    @FXML
    abstract protected void onVariableAddPressed(ActionEvent event);
    @FXML
    abstract protected void onVariableDeletePressed(ActionEvent event);
    @FXML
    abstract protected void onVariableClonePressed(ActionEvent event);
    
    abstract public void redrawGlobalScripts();
    public void initialize() {
        redrawGlobalScripts();
    }    
}
