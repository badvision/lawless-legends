/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.outlaweditor.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.adapter.JavaBeanBooleanProperty;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanIntegerProperty;
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import org.badvision.outlaweditor.ApplicationUIController;

/**
 *
 * @author brobert
 */
public class PropertyHelper {

    public static JavaBeanIntegerProperty intProp(Object t, String fieldName) throws NoSuchMethodException {
        return new JavaBeanIntegerPropertyBuilder().bean(t).name(fieldName).build();
    }

    public static JavaBeanBooleanProperty boolProp(Object t, String fieldName) throws NoSuchMethodException {
        return new JavaBeanBooleanPropertyBuilder().bean(t).name(fieldName).build();
    }

    public static Property categoryProp(final Object t, String fieldName) throws NoSuchMethodException {
        final String camel = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        SimpleStringProperty prop = new SimpleStringProperty() {
            @Override
            public String get() {
                try {
                    Method getter = t.getClass().getMethod("get" + camel);
                    List<String> list = (List<String>) getter.invoke(t);
                    String out = "";
                    for (String s : list) {
                        if (out.length() > 0) {
                            out += ",";
                        }
                        out += s;
                    }
                    return out;
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }

            @Override
            public void set(String string) {
                try {
                    Method getter = t.getClass().getMethod("get" + camel);
                    List<String> list = (List<String>) getter.invoke(t);
                    list.clear();
                    Collections.addAll(list, string.split(","));
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ApplicationUIController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        return prop;
    }

    public static JavaBeanStringProperty stringProp(Object t, String fieldName) throws NoSuchMethodException {
        return new JavaBeanStringPropertyBuilder().bean(t).name(fieldName).build();
    }
    
    static private Map<Property, Property> boundProperties = new HashMap<>();
   
    static public void bind(Property formProp, Property sourceProp) {
        if (boundProperties.containsKey(formProp)) {
            formProp.unbindBidirectional(boundProperties.get(formProp));
            boundProperties.get(formProp).unbindBidirectional(formProp);
            boundProperties.get(formProp).unbind();
            boundProperties.remove(formProp);
        }
        formProp.unbind();
        if (sourceProp != null) {
            formProp.bindBidirectional(sourceProp);
            boundProperties.put(formProp, sourceProp);
        }
    }    
}
