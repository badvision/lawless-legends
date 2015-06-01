package org.badvision.outlaweditor.ui.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javax.xml.bind.JAXBException;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.TransferHelper;
import org.badvision.outlaweditor.data.DataUtilities;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.ui.GlobalEditorTabController;
import org.badvision.outlaweditor.ui.UIAction;
import static org.badvision.outlaweditor.ui.UIAction.editScript;

public class GlobalEditorTabControllerImpl extends GlobalEditorTabController {

    @Override
    protected void onScriptAddPressed(ActionEvent event) {
        UIAction.createAndEditScript();
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
                editScript(script);
            } catch (JAXBException ex) {
                Logger.getLogger(MapEditorTabControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                UIAction.alert("Error occured when attempting clone operation:\n" + ex.getMessage());
            }
        }
    }

    @Override
    protected void onDataTypeAddPressed(ActionEvent event) {
    }

    @Override
    protected void onDataTypeDeletePressed(ActionEvent event) {
    }

    @Override
    protected void onDeleteClonePressed(ActionEvent event) {
    }

    @Override
    protected void onVariableAddPressed(ActionEvent event) {
    }

    @Override
    protected void onVariableDeletePressed(ActionEvent event) {
    }

    @Override
    protected void onVariableClonePressed(ActionEvent event) {
    }

    @Override
    public void redrawGlobalScripts() {
        DataUtilities.ensureGlobalExists();
        globalScriptList.setOnEditStart((ListView.EditEvent<Script> event) -> {
            UIAction.editScript(event.getSource().getItems().get(event.getIndex()));
        });
        globalScriptList.setCellFactory(new Callback<ListView<Script>, ListCell<Script>>() {
            @Override
            public ListCell<Script> call(ListView<Script> param) {
                final ListCell<Script> cell = new ListCell<Script>() {

                    @Override
                    protected void updateItem(Script item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
//                            ImageView visibleIcon = getVisibleIcon(item);
//                            visibleIcon.setOnMouseClicked((e) -> {
//                                toggleVisibility(visibleIcon, item);
//                                mapScriptsList.getSelectionModel().clearSelection();
//                            });
//                            setGraphic(visibleIcon);
//                            getCurrentEditor().getCurrentMap().getScriptColor(item).ifPresent(this::setTextFill);
                            setText(item.getName());
                            setFont(Font.font(null, FontWeight.BOLD, 12.0));
//                            scriptDragDrop.registerDragSupport(this, item);
//                            visibleIcon.setMouseTransparent(false);
                        }
                    }
                };
                return cell;
            }
        });
        if (globalScriptList.getItems() != null && Application.gameData.getGlobal().getScripts() != null) {
            DataUtilities.sortScripts(Application.gameData.getGlobal().getScripts());
            globalScriptList.getItems().setAll(Application.gameData.getGlobal().getScripts().getScript());
        } else {
            globalScriptList.getItems().clear();
        }
    }

}
