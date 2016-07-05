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
package org.badvision.outlaweditor.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.badvision.outlaweditor.data.xml.Rows.Row;
import org.badvision.outlaweditor.data.xml.UserType;

/**
 *
 */
public abstract class SheetEditorController implements Initializable {
    @FXML
    protected ResourceBundle resources;

    @FXML
    protected URL location;

    @FXML
    protected TableColumn<UserType, String> addColumn;
    
    @FXML
    protected TableView<Row> table;

    @FXML
    protected TextField sheetNameField;
    
    @FXML
    abstract public void addColumnAction(ActionEvent event);

    @FXML
    abstract public void addRowAction(ActionEvent event);
    
    @FXML
    abstract public void doImport(ActionEvent event);
    
    @FXML
    protected void initialize() {
        assert addColumn != null : "fx:id=\"addColumn\" was not injected: check your FXML file 'SheetEditor.fxml'.";
    }
}
