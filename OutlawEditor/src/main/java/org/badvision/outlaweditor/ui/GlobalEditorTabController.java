package org.badvision.outlaweditor.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

public abstract class GlobalScriptTabController implements Initializable {
    @FXML
    protected ListView<?> globalScriptList;
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
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    
}
