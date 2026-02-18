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

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.badvision.outlaweditor.data.FlagAuditor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Controller for the Flag Audit Dialog
 *
 * @author brobert
 */
public class FlagAuditDialogController {

    @FXML private Label totalFlagsLabel;
    @FXML private Label trackedFlagsLabel;
    @FXML private Label untrackedFlagsLabel;
    @FXML private Label unusedFlagsLabel;

    // All Flags Tab
    @FXML private TableView<FlagAuditor.FlagInfo> allFlagsTable;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> allFlagsNameColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> allFlagsStatusColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, Integer> allFlagsNumberColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, Integer> allFlagsCountColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> allFlagsLocationsColumn;

    // Missing Flags Tab
    @FXML private TableView<FlagAuditor.FlagInfo> missingFlagsTable;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> missingFlagsNameColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, Integer> missingFlagsCountColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> missingFlagsLocationsColumn;
    @FXML private Button addMissingButton;

    // Unused Flags Tab
    @FXML private TableView<FlagAuditor.FlagInfo> unusedFlagsTable;
    @FXML private TableColumn<FlagAuditor.FlagInfo, String> unusedFlagsNameColumn;
    @FXML private TableColumn<FlagAuditor.FlagInfo, Integer> unusedFlagsNumberColumn;

    // Details Tab
    @FXML private ComboBox<String> flagSelector;
    @FXML private VBox flagDetailsContainer;
    @FXML private Label flagDetailsTitle;
    @FXML private Label flagDetailsStatus;
    @FXML private Label flagDetailsRefCount;
    @FXML private TableView<FlagAuditor.FlagReference> flagDetailsTable;
    @FXML private TableColumn<FlagAuditor.FlagReference, String> detailsLocationColumn;
    @FXML private TableColumn<FlagAuditor.FlagReference, String> detailsScriptColumn;
    @FXML private TableColumn<FlagAuditor.FlagReference, String> detailsOperationColumn;

    private FlagAuditor.AuditReport report;
    private FlagAuditor auditor;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Initialize All Flags table
        allFlagsNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().flagName));
        allFlagsStatusColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isInSheet() ? "Tracked" : "Missing"));
        allFlagsNumberColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().sheetNumber != null ?
                data.getValue().sheetNumber : 0).asObject());
        allFlagsCountColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getReferenceCount()).asObject());
        allFlagsLocationsColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getLocations().stream()
                .sorted()
                .collect(Collectors.joining("; "))));

        // Style the status column
        allFlagsStatusColumn.setCellFactory(column -> new TableCell<FlagAuditor.FlagInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Missing".equals(item)) {
                        setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #009900;");
                    }
                }
            }
        });

        // Initialize Missing Flags table
        missingFlagsNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().flagName));
        missingFlagsCountColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getReferenceCount()).asObject());
        missingFlagsLocationsColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getLocations().stream()
                .sorted()
                .collect(Collectors.joining("; "))));

        // Initialize Unused Flags table
        unusedFlagsNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().flagName));
        unusedFlagsNumberColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().sheetNumber != null ?
                data.getValue().sheetNumber : 0).asObject());

        // Initialize Details table
        detailsLocationColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().location));
        detailsScriptColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().scriptName));
        detailsOperationColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().operation));

        // Style operations
        detailsOperationColumn.setCellFactory(column -> new TableCell<FlagAuditor.FlagReference, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "get":
                            setStyle("-fx-text-fill: #0066cc;");
                            break;
                        case "set":
                            setStyle("-fx-text-fill: #009900;");
                            break;
                        case "clr":
                            setStyle("-fx-text-fill: #cc0000;");
                            break;
                    }
                }
            }
        });

        // Set up flag selector
        flagSelector.setOnAction(e -> onFlagSelected());
    }

    public void setReport(FlagAuditor.AuditReport report, FlagAuditor auditor) {
        this.report = report;
        this.auditor = auditor;
        populateData();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void populateData() {
        // Update summary labels
        int trackedCount = (int) report.allFlags.values().stream()
            .filter(FlagAuditor.FlagInfo::isInSheet).count();

        totalFlagsLabel.setText("Total Flags: " + report.allFlags.size());
        trackedFlagsLabel.setText("Tracked in Sheet: " + trackedCount);
        untrackedFlagsLabel.setText("Missing from Sheet: " + report.missingFromSheet.size());
        unusedFlagsLabel.setText("Unused in Sheet: " + report.unusedInSheet.size());

        // Populate All Flags table
        allFlagsTable.setItems(FXCollections.observableArrayList(report.allFlags.values()));

        // Populate Missing Flags table
        missingFlagsTable.setItems(FXCollections.observableArrayList(
            report.missingFromSheet.stream()
                .map(report.allFlags::get)
                .collect(Collectors.toList())
        ));

        // Populate Unused Flags table
        unusedFlagsTable.setItems(FXCollections.observableArrayList(
            report.unusedInSheet.stream()
                .map(report.allFlags::get)
                .collect(Collectors.toList())
        ));

        // Populate flag selector
        flagSelector.setItems(FXCollections.observableArrayList(
            report.allFlags.keySet().stream().sorted().collect(Collectors.toList())
        ));

        // Disable add button if nothing to add
        addMissingButton.setDisable(report.missingFromSheet.isEmpty());
    }

    private void onFlagSelected() {
        String selectedFlag = flagSelector.getValue();
        if (selectedFlag == null || selectedFlag.isEmpty()) {
            flagDetailsTitle.setText("No flag selected");
            flagDetailsStatus.setText("");
            flagDetailsRefCount.setText("");
            flagDetailsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        FlagAuditor.FlagInfo info = report.allFlags.get(selectedFlag);
        if (info == null) return;

        flagDetailsTitle.setText("Flag: " + info.flagName);
        flagDetailsStatus.setText("Status: " + (info.isInSheet() ?
            "Tracked (Sheet #" + info.sheetNumber + ")" : "Missing from sheet"));
        flagDetailsRefCount.setText("Total References: " + info.getReferenceCount());

        flagDetailsTable.setItems(FXCollections.observableArrayList(info.references));
    }

    @FXML
    private void onAddMissingFlags(ActionEvent event) {
        if (report.missingFromSheet.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Missing Flags",
                "There are no missing flags to add to the sheet.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Add Missing Flags");
        confirm.setHeaderText("Add " + report.missingFromSheet.size() + " flags to the flags sheet?");
        confirm.setContentText("This will automatically assign sequential numbers to the missing flags.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            auditor.appendMissingFlagsToSheet(report);
            showAlert(Alert.AlertType.INFORMATION, "Flags Added",
                report.missingFromSheet.size() + " flags have been added to the flags sheet.\n\n" +
                "Remember to save your changes!");

            // Close dialog since data has changed
            onClose(event);
        }
    }

    @FXML
    private void onExportReport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Flag Audit Report");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName("flag_audit_report.txt");

        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportReportToFile(file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Report exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Failed to export report: " + e.getMessage());
            }
        }
    }

    private void exportReportToFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("GAME FLAG AUDIT REPORT\n");
            writer.write("======================\n\n");

            writer.write("Summary:\n");
            writer.write("--------\n");
            writer.write("Total Flags: " + report.allFlags.size() + "\n");
            int trackedCount = (int) report.allFlags.values().stream()
                .filter(FlagAuditor.FlagInfo::isInSheet).count();
            writer.write("Tracked in Sheet: " + trackedCount + "\n");
            writer.write("Missing from Sheet: " + report.missingFromSheet.size() + "\n");
            writer.write("Unused in Sheet: " + report.unusedInSheet.size() + "\n\n");

            // All flags section
            writer.write("ALL FLAGS:\n");
            writer.write("----------\n");
            for (FlagAuditor.FlagInfo info : report.allFlags.values()) {
                writer.write(String.format("%-30s %s #%-4s Refs: %-4d\n",
                    info.flagName,
                    info.isInSheet() ? "Tracked" : "MISSING",
                    info.sheetNumber != null ? info.sheetNumber.toString() : "N/A",
                    info.getReferenceCount()));

                for (FlagAuditor.FlagReference ref : info.references) {
                    writer.write(String.format("    %s\n", ref.toString()));
                }
                writer.write("\n");
            }

            // Missing flags section
            if (!report.missingFromSheet.isEmpty()) {
                writer.write("\nMISSING FROM SHEET:\n");
                writer.write("-------------------\n");
                for (String flagName : report.missingFromSheet) {
                    FlagAuditor.FlagInfo info = report.allFlags.get(flagName);
                    writer.write(String.format("%-30s References: %d\n",
                        flagName, info.getReferenceCount()));
                    for (FlagAuditor.FlagReference ref : info.references) {
                        writer.write(String.format("    %s\n", ref.toString()));
                    }
                }
            }

            // Unused flags section
            if (!report.unusedInSheet.isEmpty()) {
                writer.write("\nUNUSED IN SCRIPTS:\n");
                writer.write("------------------\n");
                for (String flagName : report.unusedInSheet) {
                    FlagAuditor.FlagInfo info = report.allFlags.get(flagName);
                    writer.write(String.format("%-30s Sheet #%d\n",
                        flagName, info.sheetNumber));
                }
            }
        }
    }

    @FXML
    private void onClose(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
