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
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javax.xml.namespace.QName;
import org.badvision.outlaweditor.SheetEditor;
import org.badvision.outlaweditor.data.xml.Columns;
import org.badvision.outlaweditor.data.xml.Rows.Row;
import org.badvision.outlaweditor.data.xml.UserType;
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
            editor.getSheet().getRows().getRow().forEach(this::insertViewRow);
        }
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
            String val = getValue(features.getValue(), col.getName());
            if (val == null) {
                val = "";
            }
            return new SimpleObjectProperty(val);
        });
        tableCol.setCellFactory(TextFieldTableCell.forTableColumn());
        tableCol.setOnEditCommit((event)
                -> setValue(event.getRowValue(), col.getName(), event.getNewValue()));
        tableCol.setEditable(true);
        tableCol.setContextMenu(new ContextMenu(
                createMenuItem("Rename Column", () -> renameColumn(col)),
                createMenuItem("Delete Column", () -> deleteColumnWithConfirmation(col))
        ));
        table.getColumns().add(pos, tableCol);
    }

    private void insertViewRow(Row row) {
        tableData.add(row);
    }

    private String getValue(Row row, String name) {
        return row.getOtherAttributes().entrySet().stream()
                .filter((e) -> e.getKey().getLocalPart().equals(name))
                .map(e -> e.getValue())
                .findFirst().orElse(null);

    }

    private void setValue(Row row, String name, String newValue) {
        Optional<Map.Entry<QName, String>> attr = row.getOtherAttributes().entrySet().stream()
                .filter((e) -> e.getKey().getLocalPart().equals(name)).findFirst();
        if (attr.isPresent()) {
            attr.get().setValue(newValue);
        } else {
            row.getOtherAttributes().put(new QName(name), newValue);
        }
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
            tableData.forEach(row -> setValue(row, newColName, getValue(row, col.getName())));
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
        for (int i=0; i < table.getColumns().size(); i++) {
            if (table.getColumns().get(i).getText().equals(col.getName())) {
                table.getColumns().remove(i);
                return i;
            }
        }
        return -1;
    }
}
