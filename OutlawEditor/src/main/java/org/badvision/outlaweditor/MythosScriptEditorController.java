package org.badvision.outlaweditor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.web.WebView;


public class MythosScriptEditorController
    implements Initializable {
    
    public static final String MYTHOS_EDITOR = "/mythos/mythos-editor/html/editor.html";
    private final List<Runnable> onLoadEvents = new ArrayList<>();
    boolean loaded = false;
    public synchronized void onLoad(Runnable runnable) {
        if (loaded) {
            runnable.run();
        } else {
            onLoadEvents.add(runnable);
        }
    }

    @FXML //  fx:id="editorView"
    WebView editorView; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemAbortChanges"
    private MenuItem menuItemAbortChanges; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemAboutBlockly"
    private MenuItem menuItemAboutBlockly; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemApplyChanges"
    private MenuItem menuItemApplyChanges; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemMythosHelp"
    private MenuItem menuItemMythosHelp; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemRedo"
    private MenuItem menuItemRedo; // Value injected by FXMLLoader

    @FXML //  fx:id="menuItemUndo"
    private MenuItem menuItemUndo; // Value injected by FXMLLoader

    MythosEditor editor;
    public void setEditor(MythosEditor editor) {
        this.editor = editor;
    }    
    
    // Handler for MenuItem[fx:id="menuItemAbortChanges"] onAction
    public void onAbortChangesSelected(ActionEvent event) {
        editor.close();
    }

    // Handler for MenuItem[fx:id="menuItemAboutBlockly"] onAction
    public void onAboutBlocklySelected(ActionEvent event) {
        // handle the event here
    }

    // Handler for MenuItem[fx:id="menuItemApplyChanges"] onAction
    public void onApplyChangesSelected(ActionEvent event) {
        editor.applyChanges();
        editor.close();
    }

    // Handler for MenuItem[fx:id="menuItemMythosHelp"] onAction
    public void onMythosHelpSelected(ActionEvent event) {
        // handle the event here
    }

    // Handler for MenuItem[fx:id="menuItemRedo"] onAction
    public void onRedoSelected(ActionEvent event) {
        // handle the event here
    }

    // Handler for MenuItem[fx:id="menuItemUndo"] onAction
    public void onUndoSelected(ActionEvent event) {
        // handle the event here
    }    
    
    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        editorView.getEngine().getLoadWorker().stateProperty().addListener(
        new ChangeListener<State>() {
            public void changed(ObservableValue ov, State oldState, State newState) {
                if (newState == State.SUCCEEDED) {
                    loaded = true;
                    for (Runnable r : onLoadEvents) {
                        r.run();
                    }
                    onLoadEvents.clear();
                }
            }
        });

        assert editorView != null : "fx:id=\"editorView\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAbortChanges != null : "fx:id=\"menuItemAbortChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAboutBlockly != null : "fx:id=\"menuItemAboutBlockly\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemApplyChanges != null : "fx:id=\"menuItemApplyChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemMythosHelp != null : "fx:id=\"menuItemMythosHelp\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemRedo != null : "fx:id=\"menuItemRedo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemUndo != null : "fx:id=\"menuItemUndo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        
        editorView.getEngine().load(getClass().getResource(MYTHOS_EDITOR).toExternalForm());    
    }
}
