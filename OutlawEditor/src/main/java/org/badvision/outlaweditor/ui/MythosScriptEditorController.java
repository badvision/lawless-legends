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

import org.badvision.outlaweditor.MythosEditor;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class MythosScriptEditorController
        implements Initializable {

    public static final String MYTHOS_EDITOR = "/mythos/mythos-editor/html/editor.html";
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
        final String loadScript = editor.generateLoadScript();
        if (loadScript != null) {
            editorView.getEngine().getLoadWorker().stateProperty().addListener(
                    (value, old, newState) -> {
                        if (newState == State.SUCCEEDED) {
                            mythos = (JSObject) editorView.getEngine().executeScript("Mythos");
                            mythos.setMember("editor", editor);
                            editorView.getEngine().executeScript(loadScript);
                            editorView.getEngine().executeScript("window.dispatchEvent(new Event('resize'));");
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
    /**
     *
     * @param fxmlFileLocation
     * @param resources
     */

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert editorView != null : "fx:id=\"editorView\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAbortChanges != null : "fx:id=\"menuItemAbortChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemAboutBlockly != null : "fx:id=\"menuItemAboutBlockly\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemApplyChanges != null : "fx:id=\"menuItemApplyChanges\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemMythosHelp != null : "fx:id=\"menuItemMythosHelp\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemRedo != null : "fx:id=\"menuItemRedo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";
        assert menuItemUndo != null : "fx:id=\"menuItemUndo\" was not injected: check your FXML file 'MythosScriptEditor.fxml'.";

        // JavaFX WebView does not forward scroll wheel events to embedded HTML content.
        // Intercept them here and drive Blockly's workspace scroll directly via JS.
        // ws.scroll(x, y) takes absolute workspace coords; divide pixel delta by scale
        // so that scrolling feels the same at all zoom levels.
        editorView.setOnScroll((ScrollEvent event) -> {
            double dx = event.getDeltaX();
            double dy = event.getDeltaY();
            editorView.getEngine().executeScript(
                "if(typeof Mythos!=='undefined'&&Mythos.workspace){" +
                "var ws=Mythos.workspace,s=ws.scale||1;" +
                "ws.scroll(ws.scrollX+(" + (-dx) + "/s),ws.scrollY+(" + dy + "/s));}"
            );
            event.consume();
        });

        // JavaFX8 has a bug where stage maximize events do not trigger resize events to webview components
        // Also fix general window resize not triggering WebView resize
        Platform.runLater(() -> {
            Stage stage = (Stage) editorView.getScene().getWindow();
            if (stage != null) {
                // Handle maximize events
                stage.maximizedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    Platform.runLater(()->editorView.getEngine().executeScript("window.dispatchEvent(new Event('resize'));"));
                });
                // Handle general window size changes
                stage.widthProperty().addListener((observable, oldValue, newValue) -> {
                    Platform.runLater(()->editorView.getEngine().executeScript("window.dispatchEvent(new Event('resize'));"));
                });
                stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                    Platform.runLater(()->editorView.getEngine().executeScript("window.dispatchEvent(new Event('resize'));"));
                });

                // Cross-window Cmd+C / Cmd+V clipboard bridge.
                //
                // The JS-to-Java bridge (Mythos.editor.setClipboard / getClipboard)
                // is unreliable in JavaFX WebView when the window calling into Java is
                // not the one that exposed the Java object.  Instead, drive both
                // directions from the Java side:
                //
                //   Cmd+C  – ask the WebView for Blockly's current copy-stash via
                //            executeScript (Java→JS) and store it in the shared static
                //            field.  This is a fallback in case the JS-side copy handler
                //            never fires.
                //
                //   Cmd+V  – read the shared static field (pure Java) and inject the
                //            JSON into this window's Blockly stash + trigger paste via
                //            executeScript (Java→JS).  We consume the event only when
                //            there is cross-window data to paste; otherwise the WebView
                //            handles Cmd+V normally so same-window paste keeps working.
                stage.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
                    boolean isMeta = event.isMetaDown() || event.isControlDown();
                    if (!isMeta) return;

                    if (event.getCode() == KeyCode.C) {
                        // Backup copy capture: read Blockly's stash from JS and
                        // store in shared field.  The JS-side handler does the same
                        // thing but may be silently failing.
                        Platform.runLater(() -> {
                            try {
                                Object result = editorView.getEngine().executeScript(
                                    "(function(){" +
                                    "  try{" +
                                    "    var d=Blockly.clipboard.getLastCopiedData ? Blockly.clipboard.getLastCopiedData() : null;" +
                                    "    return d ? JSON.stringify(d) : null;" +
                                    "  }catch(e){return null;}" +
                                    "})()"
                                );
                                if (result != null && !"null".equals(result.toString())) {
                                    if (editor != null) editor.setClipboard(result.toString());
                                }
                            } catch (Exception ignored) {}
                        });
                    }

                    if (event.getCode() == KeyCode.V && editor != null) {
                        String clipboard = editor.getClipboard();
                        if (clipboard != null && !clipboard.isEmpty()) {
                            // Consume the event so the WebView does not also fire its
                            // own Cmd+V handler and produce a double-paste.
                            event.consume();
                            final String json = clipboard;
                            Platform.runLater(() -> {
                                try {
                                    editorView.getEngine().executeScript(
                                        "(function(){" +
                                        "  try{" +
                                        "    var d=" + json + ";" +
                                        "    if(typeof Blockly!=='undefined'&&Mythos&&Mythos.workspace){" +
                                        "      Blockly.clipboard.setLastCopiedData(d);" +
                                        "      Blockly.clipboard.setLastCopiedWorkspace(Mythos.workspace);" +
                                        "      Blockly.clipboard.paste(Mythos.workspace);" +
                                        "    }" +
                                        "  }catch(e){}" +
                                        "})()"
                                    );
                                } catch (Exception ignored) {}
                            });
                        }
                    }
                });
            }
        });
    }

    public String getScriptXml() {
        return String.valueOf(editorView.getEngine().executeScript("Mythos.getScriptXml();"));
    }
}
