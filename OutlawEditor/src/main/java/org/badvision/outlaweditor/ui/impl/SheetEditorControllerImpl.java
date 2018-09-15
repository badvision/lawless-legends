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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.stage.FileChooser;
import javax.xml.bind.JAXBException;
import org.badvision.outlaweditor.SheetEditor;
import org.badvision.outlaweditor.TransferHelper;
import org.badvision.outlaweditor.data.DataUtilities;
import static org.badvision.outlaweditor.data.DataUtilities.getValue;
import static org.badvision.outlaweditor.data.DataUtilities.setValue;
import org.badvision.outlaweditor.data.xml.Columns;
import org.badvision.outlaweditor.data.xml.Rows;
import org.badvision.outlaweditor.data.xml.Rows.Row;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.SheetEditorController;
import org.badvision.outlaweditor.ui.UIAction;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellBase;

public class SheetEditorControllerImpl extends SheetEditorController {

    private SheetEditor editor;
    private ObservableList<ObservableList<SpreadsheetCell>> tableData;
    private int lastEditRow = 0;
    private int lastEditCol = 0;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize();
        tableData = table.getGrid().getRows();
        table.setEditable(true);
        /*
        table.setRowFactory(tableView -> {
            final TableRow<Row> row = new TableRow<>();

            final ContextMenu contextMenu = new ContextMenu(
                    createMenuItem("Insert Row", () -> tableData.add(row.getIndex(), new Row())),
                    createMenuItem("Clone Row", () -> cloneRow(row.getItem())),
                    createMenuItem("Delete Row", () -> deleteRowWithConfirmation(row.getItem()))
            );
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });
         */
    }

    @Override
    public void doImport(ActionEvent event) {
        FileChooser openFileDialog = new FileChooser();
        openFileDialog.setTitle("Select either a text or an Excel file");
        File selected = openFileDialog.showOpenDialog(null);
        if (selected != null && selected.exists() && selected.isFile()) {
            List<List<String>> data = DataUtilities.readFromFile(selected);
            if (data != null && data.size() > 1) {
                tableData.clear();
                editor.getSheet().setColumns(new Columns());
                data.get(0).stream().map(s -> s != null && !s.isEmpty() ? s : "---").map(name -> {
                    UserType type = new UserType();
                    type.setName(name);
                    return type;
                }).collect(Collectors.toCollection(editor.getSheet().getColumns()::getColumn));

                editor.getSheet().setRows(new Rows());
                data.stream().skip(1)
                        .map(cols -> {
                            Row r = new Row();
                            for (int i = 0; i < cols.size(); i++) {
                                if (cols.get(i) != null) {
                                    setValue(r.getOtherAttributes(), data.get(0).get(i), cols.get(i));
                                }
                            }
                            return r;
                        })
                        .filter(r -> !r.getOtherAttributes().isEmpty())
                        .collect(Collectors.toCollection(editor.getSheet().getRows()::getRow));

                buildTableFromSheet();
            }
        }
    }

    public void setEditor(SheetEditor editor) {
        tableData.clear();
        this.editor = editor;
        buildTableFromSheet();
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
            insertViewColumn(col);
            rebuildGridUI();
        }
    }

    @Override
    public void addRowAction(ActionEvent event) {
        insertViewRow(new Row());
    }

    //--------
    private void buildTableFromSheet() {
        if (editor.getSheet().getRows() != null) {
            rebuildGridUI();
        }
        sheetNameField.textProperty().set(editor.getSheet().getName());
        sheetNameField.textProperty().addListener((value, oldValue, newValue) -> {
            editor.getSheet().setName(newValue);
            ApplicationUIController.getController().updateSelectors();
        });
    }

    private void insertViewColumn(UserType col) {
        insertViewColumn(col, -1);
    }

    private void insertViewColumn(UserType col, int pos) {
        table.getGrid().getColumnHeaders().clear();
        if (pos < 0 || pos >= editor.getSheet().getColumns().getColumn().size()) {
            editor.getSheet().getColumns().getColumn().add(col);
        } else {
            editor.getSheet().getColumns().getColumn().add(pos, col);
        }
        rebuildColumnHeaders();
    }

    private void insertViewRow(Row row) {
        editor.getSheet().getRows().getRow().add(new Row());
        rebuildGridUI();
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
//            tableData.forEach(row -> setValue(row.getOtherAttributes(), newColName, getValue(row.getOtherAttributes(), col.getName())));
            int oldPos = deleteColumn(col);
            insertViewColumn(newCol, oldPos);
        }
    }

    private int deleteColumn(UserType col) {
//        editor.getSheet().getColumns().getColumn().remove(col);
//        tableData.stream()
//                .map(Row::getOtherAttributes)
//                .forEach(
//                        m -> m.keySet().removeIf(n -> n.getLocalPart().equals(col.getName()))
//                );
//        int colNumber = findColumn(col);
//        if (colNumber >= 0) {
//            table.getColumns().remove(colNumber);
//        }
//        return colNumber;
        return 0;
    }

    private void deleteRowWithConfirmation(Row row) {
        UIAction.confirm("Delete row, are you sure?", () -> {
            editor.getSheet().getRows().getRow().remove(row);
            rebuildGridUI();
        }, () -> {
        });
    }

    private void cloneRow(Row row) {
        try {
            Row newRow = TransferHelper.cloneObject(row, Row.class, "row");
            editor.getSheet().getRows().getRow().add(newRow);
            rebuildGridUI();
        } catch (JAXBException ex) {
            Logger.getLogger(SheetEditorControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void rebuildGridUI() {
        int numCols = editor.getSheet().getColumns().getColumn().size();
        int numRows = editor.getSheet().getRows().getRow().size();
        table.setGrid(new GridBase(numRows, numCols));

        rebuildColumnHeaders();

        tableData = FXCollections.observableList(new ArrayList(numRows));
        int rowNum = 0;
        for (Row row : editor.getSheet().getRows().getRow()) {
            int colNum = 0;
            ObservableList<SpreadsheetCell> rowUi = FXCollections.observableList(new ArrayList<>(numCols));
            tableData.add(rowUi);
            for (UserType col : editor.getSheet().getColumns().getColumn()) {
                String value = getValue(row.getOtherAttributes(), col.getName());
                SpreadsheetCellBase cell = new SpreadsheetCellBase(rowNum, colNum, 1, 1);
                cell.setItem(value);
                cell.itemProperty().addListener((ObservableValue<? extends Object> val, Object oldVal, Object newVal) -> {
                    setValue(row.getOtherAttributes(), col.getName(), String.valueOf(newVal));
                });
                rowUi.add(cell);
                colNum++;
            }
            rowNum++;
        }
        table.getGrid().setRows(tableData);
    }

    private void rebuildColumnHeaders() {
        int numCols = editor.getSheet().getColumns().getColumn().size();
        table.getGrid().getColumnHeaders().addAll(
                editor.getSheet().getColumns().getColumn().stream()
                        .map(UserType::getName)
                        .collect(Collectors.toCollection(
                                () -> FXCollections.observableList(new ArrayList<String>(numCols))
                        ))
        );
    }
}
