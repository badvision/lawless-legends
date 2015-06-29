/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.ui;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 *
 * @author blurry
 */
public class ModalEditor {

    public static interface EditControl<V> {

        public Control getControl();

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
        grid.setPadding(new Insets(20, 150, 10, 10));

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
                        descriptor.getWriteMethod().invoke(sourceObject, control.getValue());
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(ModalEditor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                return sourceObject;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private String uppercaseFirst(String str) {
        StringBuilder b = new StringBuilder(str);
        int i = 0;
        do {
            b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
            i = b.indexOf(" ", i) + 1;
        } while (i > 0 && i < b.length());
        return b.toString();
    }
}
