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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author brobert
 */
public class DataProducer {

    static Map<Object, List<WeakReference<DataObserver>>> observers;

    static {
        clear();
    }

    public static void clear() {
        observers = new ConcurrentHashMap<>();
    }

    public static List<WeakReference<DataObserver>> getObservers(Object o) {
        if (observers.get(o) == null) {
            observers.put(o, new ArrayList<WeakReference<DataObserver>>());
        }
        return observers.get(o);
    }

    public static void addObserver(Object o, DataObserver observer) {
        getObservers(o).add(new WeakReference<>(observer));
    }

    public static void notifyObservers(Object o) {
        for (WeakReference<DataObserver> ref : getObservers(o)) {
            DataObserver observer = ref.get();
            if (observer != null) {
                observer.observedObjectChanged(o);
            }
        }
    }
}
