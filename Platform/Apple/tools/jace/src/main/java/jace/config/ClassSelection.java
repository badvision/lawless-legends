/*
 * Copyright (C) 2012 Brendan Robert (BLuRry) brendan.robert@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jace.config;

import jace.core.Utility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */

public class ClassSelection<C> extends DynamicSelection<Class<? extends C>> {

    Class<C> template;

    public ClassSelection(Class<C> supertype, Class<? extends C> defaultValue) {
        super(defaultValue);
        template = supertype;
    }

    @Override
    public LinkedHashMap<Class<C>, String> getSelections() {
        LinkedHashMap<Class<C>, String> selections = new LinkedHashMap<>();
        Set<Class<? extends C>> allClasses = Utility.findAllSubclasses(template);
        allClasses.add(null);
        List<Entry<Class<? extends C>, String>> values = new ArrayList<>();
        if (allowNull()) {
            values.add(new Entry<Class<? extends C>, String>() {

                @Override
                public Class<? extends C> getKey() {
                    return null;
                }

                @Override
                public String getValue() {
                    return "***Empty***";
                }

                @Override
                public String setValue(String v) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        }
        for (final Class<? extends C> c : allClasses) {
            Entry<Class<? extends C>, String> entry = new Map.Entry<Class<? extends C>, String>() {

                @Override
                public Class<? extends C> getKey() {
                    return c;
                }

                @Override
                public String getValue() {
                    if (c == null) {
                        return "**Empty**";
                    }
                    if (c.isAnnotationPresent(Name.class)) {
                        return c.getAnnotation(Name.class).value();
                    }
                    return c.getSimpleName();
                }

                @Override
                public String setValue(String value) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String toString() {
                    return getValue();
                }

                @Override
                public boolean equals(Object obj) {
                    return super.equals(obj) || obj == getKey() || getKey() != null && getKey().equals(obj);
                }
            };
            values.add(entry);
        }
        Collections.sort(values, (Entry<? extends Class, String> o1, Entry<? extends Class, String> o2) -> {
            if (o1.getKey() == null) {
                return -1;
            }
            if (o2.getKey() == null) {
                return 1;
            } else {
                return (o1.getValue().compareTo(o2.getValue()));
            }
        });
        values.forEach((entry) -> {
            Class key = entry.getKey();
            selections.put(key, entry.getValue());
        });
        return selections;
    }

    @Override
    public boolean allowNull() {
        return false;
    }

    @Override
    public void setValue(Class value) {
        Object v = value;
        if (v != null && v instanceof String) {
            super.setValueByMatch((String) v);
            return;
        }
        super.setValue(value);
    }
}