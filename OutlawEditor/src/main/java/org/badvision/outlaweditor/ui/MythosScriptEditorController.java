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
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.badvision.outlaweditor.MythosEditor;

public class MythosScriptEditorController
        implements Initializable {

    public static final String MYTHOS_EDITOR = "/mythos/mythos-editor/html/editor.html";
    public static final String ONLOAD_SCRIPT = "onloadScript";
    // This is tied to the Mythos object defined in mythos_uncompressed
    JSObject mythos;

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
        assert editorView != null : "fx:id=\"editorView\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAbortChanges != null : "fx:id=\"menuItemAbortChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAboutBlockly != null : "fx:id=\"menuItemAboutBlockly\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemApplyChanges != null : "fx:id=\"menuItemApplyChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemMythosHelp != null : "fx:id=\"menuItemMythosHelp\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemRedo != null : "fx:id=\"menuItemRedo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemUndo != null : "fx:id=\"menuItemUndo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";

        final String loadScript = resources.getString(ONLOAD_SCRIPT);
        if (loadScript != null) {
            editorView.getEngine().getLoadWorker().stateProperty().addListener(
                    (value, old, newState) -> {
                        if (newState == State.SUCCEEDED) {
                            mythos = (JSObject) editorView.getEngine().executeScript("Mythos");
                            mythos.setMember("editor", editor);
                            editorView.getEngine().executeScript(loadScript);
                        }
                    });
            
            editorView.getEngine().setPromptHandler((PromptData prompt) -> {
                return UIAction.getText(prompt.getMessage(), prompt.getDefaultValue());
            });
        }

        //TODO: Verify the path conversion works in Win7 with a jar file
        // Affected by https://bugs.openjdk.java.net/browse/JDK-8136466
        editorView.getEngine().load(getClass().getResource(MYTHOS_EDITOR).toExternalForm());
    }

    public static ResourceBundle createResourceBundle(final Map<String, String> input) {
        return new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                Object[][] output = new Object[input.size()][2];
                Set<String> keys = input.keySet();
                int i = 0;
                for (String key : keys) {
                    output[i] = new Object[]{key, input.get(key)};
                    i++;
                }
                return output;
            }
        };
    }

    public String getScriptXml() {
        return String.valueOf(editorView.getEngine().executeScript("Mythos.getScriptXml();"));
    }
}