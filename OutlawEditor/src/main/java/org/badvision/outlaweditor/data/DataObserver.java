package org.badvision.outlaweditor.data;

/**
 *
 * @author brobert
 */
public interface DataObserver<T> {
    public void observedObjectChanged(T object);
}
