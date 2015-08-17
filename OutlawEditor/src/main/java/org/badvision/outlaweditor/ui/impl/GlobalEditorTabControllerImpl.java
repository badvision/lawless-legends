package org.badvision.outlaweditor.ui.impl;

import java.beans.IntrospectionException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javax.xml.bind.JAXBException;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.TransferHelper;
import org.badvision.outlaweditor.data.DataUtilities;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.data.xml.Variable;
import org.badvision.outlaweditor.ui.GlobalEditorTabController;
import org.badvision.outlaweditor.ui.UIAction;
import static org.badvision.outlaweditor.ui.UIAction.editScript;

public class GlobalEditorTabControllerImpl extends GlobalEditorTabController {

    @Override
    public void initialize() {
        super.initialize();
        variableList.setCellFactory((listView) -> new ListCell<Variable>() {
            @Override
            protected void updateItem(Variable item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getName());
                    if (item.getComment() != null && !(item.getComment().isEmpty())) {
                        setTooltip(new Tooltip(item.getComment()));
                    }
                    setFont(Font.font(null, FontWeight.BOLD, 12.0));
                }
            }

            @Override
            public void startEdit() {
                UIAction.editVariable(getItem(), Application.gameData.getGlobal());
                cancelEdit();
                updateItem(getItem(), false);
            }
        });
        globalScriptList.setCellFactory((listView) -> new ListCell<Script>() {
            @Override
            protected void updateItem(Script item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getName());
                    setFont(Font.font(null, FontWeight.BOLD, 12.0));
                }
            }

            @Override
            public void startEdit() {
                UIAction.editScript(getItem(), Application.gameData.getGlobal());
                cancelEdit();
                updateItem(getItem(), false);
            }
        });
        dataTypeList.setCellFactory((listView) -> new ListCell<UserType>() {
            @Override
            protected void updateItem(UserType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getName());
                    if (item.getComment() != null && !(item.getComment().isEmpty())) {
                        setTooltip(new Tooltip(item.getComment()));
                    }
                    setFont(Font.font(null, FontWeight.BOLD, 12.0));
                }
            }

            @Override
            public void startEdit() {
                UIAction.editUserType(getItem());
                cancelEdit();
                updateItem(getItem(), false);
            }
        });
    }

    @Override
    protected void onScriptAddPressed(ActionEvent event) {
        UIAction.createAndEditScript(Application.gameData.getGlobal());
    }

    @Override
    protected void onScriptDeletePressed(ActionEvent event) {
        Script script = globalScriptList.getSelectionModel().getSelectedItem();
        if (script != null) {
            UIAction.confirm(
                    "Are you sure you want to delete the script "
                    + script.getName()
                    + "?  There is no undo for this!",
                    () -> {
                        getCurrentEditor().removeScript(script);
                        redrawGlobalScripts();
                    }, null);
        }
    }

    @Override
    protected void onScriptClonePressed(ActionEvent event) {
        Script source = globalScriptList.getSelectionModel().getSelectedItem();
        if (source == null) {
            String message = "First select a script and then press Clone";
            UIAction.alert(message);
        } else {
            try {
                Script script = TransferHelper.cloneObject(source, Script.class, "script");
                script.setName(source.getName() + " CLONE");
                getCurrentEditor().addScript(script);
                editScript(script, Application.gameData.getGlobal());
            } catch (JAXBException ex) {
                Logger.getLogger(MapEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                UIAction.alert("Error occured when attempting clone operation:\n" + ex.getMessage());
            }
        }
    }

    @Override
    protected void onDataTypeAddPressed(ActionEvent event) {
        try {
            UIAction.createAndEditUserType();
            redrawGlobalDataTypes();
        } catch (IntrospectionException ex) {
            Logger.getLogger(GlobalEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void onDataTypeDeletePressed(ActionEvent event) {
        UserType type = dataTypeList.getSelectionModel().getSelectedItem();
        if (type != null) {
            UIAction.confirm(
                    "Are you sure you want to delete the user-defined type "
                    + type.getName()
                    + "?  There is no undo for this!",
                    () -> {
                        Application.gameData.getGlobal().getUserTypes().getUserType().remove(type);
                        redrawGlobalDataTypes();
                    }, null);
        }
    }

    @Override
    protected void onDataTypeClonePressed(ActionEvent event) {
        UserType source = dataTypeList.getSelectionModel().getSelectedItem();
        if (source == null) {
            String message = "First select a data type and then press Clone";
            UIAction.alert(message);
        } else {
            try {
                UserType newType = TransferHelper.cloneObject(source, UserType.class, "userType");
                newType.setName(source.getName() + " CLONE");
                if (UIAction.editAndGetUserType(newType) != null) {
                    Application.gameData.getGlobal().getUserTypes().getUserType().add(newType);
                    redrawGlobalDataTypes();
                }
            } catch (JAXBException ex) {
                Logger.getLogger(MapEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                UIAction.alert("Error occured when attempting clone operation:\n" + ex.getMessage());
            } catch (IntrospectionException ex) {
                Logger.getLogger(GlobalEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void onVariableAddPressed(ActionEvent event) {
        try {
            UIAction.createAndEditVariable(Application.gameData.getGlobal());
            redrawGlobalVariables();
        } catch (IntrospectionException ex) {
            Logger.getLogger(GlobalEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void onVariableDeletePressed(ActionEvent event) {
        Variable var = variableList.getSelectionModel().getSelectedItem();
        if (var != null) {
            UIAction.confirm(
                    "Are you sure you want to delete the variable "
                    + var.getName()
                    + "?  There is no undo for this!",
                    () -> {
                        Application.gameData.getGlobal().getVariables().getVariable().remove(var);
                        redrawGlobalVariables();
                    }, null);
        }
    }

    @Override
    protected void onVariableClonePressed(ActionEvent event) {
        Variable source = variableList.getSelectionModel().getSelectedItem();
        if (source == null) {
            String message = "First select a variable and then press Clone";
            UIAction.alert(message);
        } else {
            try {
                Variable variable = TransferHelper.cloneObject(source, Variable.class, "variable");
                variable.setName(source.getName() + " CLONE");
                Optional<Variable> newVar = UIAction.editAndGetVariable(variable);
                if (newVar.isPresent()) {
                    Application.gameData.getGlobal().getVariables().getVariable().add(newVar.get());
                    redrawGlobalVariables();
                }
            } catch (JAXBException ex) {
                Logger.getLogger(MapEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                UIAction.alert("Error occured when attempting clone operation:\n" + ex.getMessage());
            } catch (IntrospectionException ex) {
                Logger.getLogger(GlobalEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void redrawGlobalScripts() {
        DataUtilities.ensureGlobalExists();
        if (globalScriptList.getItems() != null && Application.gameData.getGlobal().getScripts() != null) {
            DataUtilities.sortNamedEntities(Application.gameData.getGlobal().getScripts().getScript());
            globalScriptList.getItems().setAll(Application.gameData.getGlobal().getScripts().getScript());
        } else {
            globalScriptList.getItems().clear();
        }
    }

    @Override
    public void redrawGlobalVariables() {
        DataUtilities.ensureGlobalExists();
        if (variableList.getItems() != null && Application.gameData.getGlobal().getVariables() != null) {
            DataUtilities.sortNamedEntities(Application.gameData.getGlobal().getVariables().getVariable());
            variableList.getItems().setAll(Application.gameData.getGlobal().getVariables().getVariable());
        } else {
            variableList.getItems().clear();
        }
    }

    @Override
    public void redrawGlobalDataTypes() {
        DataUtilities.ensureGlobalExists();
        if (dataTypeList.getItems() != null && Application.gameData.getGlobal().getUserTypes() != null) {
            DataUtilities.sortNamedEntities(Application.gameData.getGlobal().getUserTypes().getUserType());
            dataTypeList.getItems().setAll(Application.gameData.getGlobal().getUserTypes().getUserType());
        } else {
            dataTypeList.getItems().clear();
        }
    }
}
