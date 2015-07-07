package org.badvision.outlaweditor.ui;

import com.sun.glass.ui.Application;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import static org.badvision.outlaweditor.data.DataUtilities.uppercaseFirst;

/**
 *
 * @author blurry
 */
public class ModalEditor {

    public static interface EditControl<V> {

        public Node getControl();

        public V getValue();

        public void setValue(V value);
    }

    public static class TextControl implements EditControl<String> {

        TextField control = new TextField();

        @Override
        public Control getControl() {
            return control;
        }

        @Override
        public String getValue() {
            return control.getText();
        }

        @Override
        public void setValue(String value) {
            control.setText(value);
        }
    }

    static class TableControl<T, S> implements EditControl<List<T>> {

        Map<String, Callback<TableColumn<T, S>, TableCell<T, S>>> cols;
        Class<T> rowType;
        ObservableList<T> rows;
        TableView<T> table;
        VBox control;

        public TableControl(Map<String, Callback<TableColumn<T, S>, TableCell<T, S>>> cols, Class<T> rowType) {
            this.cols = cols;
            this.rowType = rowType;
            rows = FXCollections.observableArrayList();

            table = new TableView(rows);
            cols.forEach((String colName, Callback<TableColumn<T, S>, TableCell<T, S>> factory) -> {
                TableColumn<T, S> col = new TableColumn<>(uppercaseFirst(colName));
                col.setCellValueFactory((TableColumn.CellDataFeatures<T, S> param) -> {
                    try {
                        return (ObservableValue<S>) new JavaBeanStringPropertyBuilder().bean(param.getValue()).name(colName).build();
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                });
                col.setCellFactory(factory);
                col.setPrefWidth(150);
                table.getColumns().add(col);
            });
            table.setPlaceholder(new Label("Click + to add one or more attributes"));
            table.setEditable(true);
            table.setPrefWidth(cols.size() * 150 + 5);

            Button addButton = new Button("+");
            addButton.setOnAction((event) -> {
                try {
                    rows.add(rowType.newInstance());
                    Application.invokeLater(() -> {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        table.edit(rows.size() - 1, table.getColumns().get(0));                        
                    });
                } catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            Button removeButton = new Button("-");
            removeButton.setOnAction((event) -> {
                rows.removeAll(table.getSelectionModel().getSelectedItems());
            });
            addButton.setPrefWidth(25);
            removeButton.setPrefWidth(25);
            control = new VBox(
                    table,
                    new HBox(
                            10,
                            addButton,
                            removeButton
                    )
            );
        }

        @Override
        public Node getControl() {
            return control;
        }

        @Override
        public List<T> getValue() {
            return rows;
        }

        @Override
        public void setValue(List<T> value) {
            rows.setAll(value);
        }
    }

    public PropertyDescriptor getPropertyDescriptor(PropertyDescriptor[] descriptors, String propertyName) {
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getName().equalsIgnoreCase(propertyName)) {
                return descriptor;
            }
        }
        return null;
    }

    public <T> Optional<T> editObject(T sourceObject, Map<String, EditControl> obj, Class<T> clazz, String title, String header) throws IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(clazz);

        Dialog dialog = new Dialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

//        dialog.setGraphic(...);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        final AtomicInteger row = new AtomicInteger(0);
        obj.forEach((String property, EditControl control) -> {
            PropertyDescriptor descriptor = getPropertyDescriptor(info.getPropertyDescriptors(), property);
            if (row.get() == 0) {
                Platform.runLater(() -> control.getControl().requestFocus());
            }
            grid.add(new Label(uppercaseFirst(descriptor.getDisplayName())), 0, row.get());
            grid.add(control.getControl(), 1, row.getAndAdd(1));
            try {
                control.setValue(descriptor.getReadMethod().invoke(sourceObject));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                obj.forEach((String property, EditControl control) -> {
                    PropertyDescriptor descriptor = getPropertyDescriptor(info.getPropertyDescriptors(), property);
                    try {
                        if (descriptor.getWriteMethod() != null) {
                            descriptor.getWriteMethod().invoke(sourceObject, control.getValue());
                        } else {
                            Object val = descriptor.getReadMethod().invoke(sourceObject);
                            if (val instanceof List) {
                                List sourceList = (List) val;
                                sourceList.clear();
                                sourceList.addAll((Collection) control.getValue());
                            }
                        }
                    } catch (NullPointerException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, "Error updating property " + property, ex);
                    }
                });
                return sourceObject;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
