/*
 * Copyright 2016 org.badvision.
 *
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
package org.badvision.outlaweditor.ui.impl;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.SheetEditor;
import static org.badvision.outlaweditor.data.DataUtilities.getValue;
import static org.badvision.outlaweditor.data.DataUtilities.setValue;
import org.badvision.outlaweditor.data.xml.Columns;
import org.badvision.outlaweditor.data.xml.Rows;
import org.badvision.outlaweditor.data.xml.Rows.Row;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.SheetEditorController;
import org.badvision.outlaweditor.ui.UIAction;

public class SheetEditorControllerImpl extends SheetEditorController {

    private SheetEditor editor;
    private ObservableList<Row> tableData;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize();
        tableData = FXCollections.observableArrayList();
        table.setItems(tableData);
        table.setEditable(true);
    }

    public void setEditor(SheetEditor editor) {
        tableData.clear();
        this.editor = editor;
        if (editor.getSheet().getColumns() != null) {
            editor.getSheet().getColumns().getColumn().stream().forEach(this::insertViewColumn);
        }
        if (editor.getSheet().getRows() != null) {
            tableData.setAll(editor.getSheet().getRows().getRow());
        }
        sheetNameField.textProperty().set(editor.getSheet().getName());
        sheetNameField.textProperty().addListener((value, oldValue, newValue) -> {
            editor.getSheet().setName(newValue);
            ApplicationUIController.getController().updateSelectors();
        });
        table.getColumns().addListener((ListChangeListener.Change<? extends TableColumn<Row, ?>> c) -> syncData());
    }

    @Override
    public void addColumnAction(ActionEvent event) {
        String newColName = UIAction.getText("Enter new column name", "new");
        if (newColName != null && !newColName.isEmpty()) {
            UserType col = new UserType();
            col.setName(newColName);
            if (editor.getSheet().getColumns() == null) {
                editor.getSheet().setColumns(new Columns());
            }
            editor.getSheet().getColumns().getColumn().add(col);
            insertViewColumn(col);
        }
    }

    @Override
    public void addRowAction(ActionEvent event) {
        insertViewRow(new Row());
    }

    private void insertViewColumn(UserType col) {
        insertViewColumn(col, -1);
    }
    
    private void insertViewColumn(UserType col, int pos) {
        if (pos < 0) {
            pos = table.getColumns().size();
        }
        TableColumn<Row, String> tableCol = new TableColumn<>(col.getName());
        tableCol.setCellValueFactory((features) -> {
            String val = getValue(features.getValue().getOtherAttributes(), col.getName());
            if (val == null) {
                val = "";
            }
            return new SimpleObjectProperty(val);
        });
        tableCol.setCellFactory(TextFieldTableCell.forTableColumn());
        tableCol.setOnEditCommit((event)
                -> setValue(event.getRowValue().getOtherAttributes(), col.getName(), event.getNewValue()));
        tableCol.setEditable(true);
        tableCol.setContextMenu(new ContextMenu(
                createMenuItem("Rename Column", () -> renameColumn(col)),
                createMenuItem("Delete Column", () -> deleteColumnWithConfirmation(col))
        ));
        table.getColumns().add(pos, tableCol);
    }

    private void insertViewRow(Row row) {
        tableData.add(row);
        syncData();
    }

    private MenuItem createMenuItem(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction((evt) -> action.run());
        return menuItem;
    }

    private void deleteColumnWithConfirmation(UserType col) {
        UIAction.confirm("Are you sure you want to delete column " + col.getName() + "?",
                () -> deleteColumn(col),
                null);
    }

    private void renameColumn(UserType col) {
        String newColName = UIAction.getText("Enter new column name", col.getName());
        if (newColName != null && !newColName.isEmpty() && !col.getName().equals(newColName)) {
            UserType newCol = new UserType();
            newCol.setName(newColName);
            editor.getSheet().getColumns().getColumn().add(newCol);
            tableData.forEach(row -> setValue(row.getOtherAttributes(), newColName, getValue(row.getOtherAttributes(), col.getName())));
            int oldPos = deleteColumn(col);
            insertViewColumn(newCol, oldPos);
        }
    }

    private int deleteColumn(UserType col) {
        editor.getSheet().getColumns().getColumn().remove(col);
        tableData.stream()
                .map(Row::getOtherAttributes)
                .forEach(
                        m -> m.keySet().removeIf(n -> n.getLocalPart().equals(col.getName()))
                );
        int colNumber = findColumn(col);
        if (colNumber >=0 ) {
            table.getColumns().remove(colNumber);
        }
        return colNumber;
    }
    
    private void syncData() {
        if (editor.getSheet().getRows() == null) {
            editor.getSheet().setRows(new Rows());
        }
        editor.getSheet().getRows().getRow().clear();
        editor.getSheet().getRows().getRow().addAll(tableData);
        editor.getSheet().getColumns().getColumn().sort((t1, t2) -> {
            return Integer.compare(findColumn(t1), findColumn(t2));
        });
    }

    private int findColumn(UserType col) {
        for (int i=0; i < table.getColumns().size(); i++) {
            if (table.getColumns().get(i).getText().equals(col.getName())) {
                return i;
            }
        }
        return -1;        
    }    
}
