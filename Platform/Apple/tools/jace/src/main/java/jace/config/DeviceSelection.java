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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 * @param <C> Enum class which implements DeviceEnum
 */
// C is an enum class which implements DeviceEnum
@SuppressWarnings("all")
public class DeviceSelection<C extends Enum & DeviceEnum> extends DynamicSelection<C> {

    Class<C> enumClass;
    boolean nullAllowed = false;

    public DeviceSelection(Class<C> enumClass, C defaultValue) {
        super(defaultValue);
        if (defaultValue == null) {
            nullAllowed = true;
        }
        this.enumClass = enumClass;
    }

    public DeviceSelection(Class<C> enumClass, C defaultValue, boolean nullAllowed) {
        this(enumClass, defaultValue);
        this.nullAllowed = nullAllowed;
    }

    @Override
    public LinkedHashMap<C, String> getSelections() {
        LinkedHashMap<C, String> selections = new LinkedHashMap<>();
        if (allowNull()) {
            selections.put(null, "***Empty***");
        }
        // Sort enum constants by getName
        List<C> sorted = new ArrayList<>();
        sorted.addAll(Arrays.asList(enumClass.getEnumConstants()));
        Collections.sort(sorted, (C o1, C o2) -> o1.getName().compareTo(o2.getName()));
        for (C c : enumClass.getEnumConstants()) {
            selections.put(c, c.getName());
        }
        return selections;
    }

    @Override
    public boolean allowNull() {
        return nullAllowed;
    }

    @Override
    public void setValue(C value) {
        Object v = value;
        if (v != null && v instanceof String) {
            super.setValueByMatch((String) v);
            return;
        }
        super.setValue(value);
    }
}