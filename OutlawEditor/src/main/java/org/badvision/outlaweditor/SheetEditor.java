/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.badvision.outlaweditor;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.badvision.outlaweditor.data.xml.Sheet;
import org.badvision.outlaweditor.ui.impl.SheetEditorControllerImpl;

/**
 * Edit a spreadsheet of information
 */
public class SheetEditor {
    private final Sheet sheet;
    private Stage primaryStage;
    private SheetEditorControllerImpl controller;
    
    public SheetEditor(Sheet sheet) {
        this.sheet=sheet;
    }

    public Sheet getSheet() {
        return sheet;
    }
    
    public void show() {
        primaryStage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/SheetEditor.fxml"));
//        Map<String, String> properties = new HashMap<>();
//        fxmlLoader.setResources(SheetEditorControllerImpl.createResourceBundle(properties));
        try {
            AnchorPane node = (AnchorPane) fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.setEditor(this);
            Scene s = new Scene(node);
            primaryStage.setScene(s);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.setOnCloseRequest((final WindowEvent t) -> {
            t.consume();
        });
        primaryStage.show();
    }
    
}
