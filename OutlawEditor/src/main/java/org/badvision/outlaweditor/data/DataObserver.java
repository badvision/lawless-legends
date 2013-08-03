package org.badvision.outlaweditor.data;

import org.badvision.outlaweditor.*;

/**
 *
 * @author brobert
 */
public interface DataObserver<T> {
    public void observedObjectChanged(T object);
}
