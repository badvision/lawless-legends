package org.badvision.outlaweditor.ui.impl;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

/**
 * FXML Controller class
 *
 * @author blurry
 */
public class GlobalScriptTabControllerImpl implements Initializable {
    @FXML
    private ListView<?> globalScriptList;
    @FXML
    private ListView<?> dataTypeList;
    @FXML
    private ListView<?> variableList;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void onScriptAddPressed(ActionEvent event) {
    }

    @FXML
    private void onScriptDeletePressed(ActionEvent event) {
    }

    @FXML
    private void onScriptClonePressed(ActionEvent event) {
    }

    @FXML
    private void onDataTypeAddPressed(ActionEvent event) {
    }

    @FXML
    private void onDataTypeDeletePressed(ActionEvent event) {
    }

    @FXML
    private void onDeleteClonePressed(ActionEvent event) {
    }

    @FXML
    private void onVariableAddPressed(ActionEvent event) {
    }

    @FXML
    private void onVariableDeletePressed(ActionEvent event) {
    }

    @FXML
    private void onVariableClonePressed(ActionEvent event) {
    }
    
}
