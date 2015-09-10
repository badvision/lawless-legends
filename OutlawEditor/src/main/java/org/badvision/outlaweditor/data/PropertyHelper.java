/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.data;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.adapter.JavaBeanBooleanProperty;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanIntegerProperty;
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

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

    public static JavaBeanStringProperty stringProp(Object t, String fieldName) throws NoSuchMethodException {
        return new JavaBeanStringPropertyBuilder().bean(t).name(fieldName).build();
    }

    private static final Map<Property, Property> boundProperties = new HashMap<>();

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
