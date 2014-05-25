/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
