/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.badvision.outlaweditor;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.DataProducer;
import org.badvision.outlaweditor.data.xml.Script;

import jakarta.xml.bind.JAXBException;
import javafx.scene.layout.Pane;

/**
 * Extremely generic editor abstraction -- useful for uniform edit features
 * across application
 *
 * @author brobert
 */
public abstract class Editor<T, D> implements DataObserver<T> {
    public static final int UNDO_HISTORY_LENGTH = 50;
    LinkedList<T> undoStates = new LinkedList<>();

    T editEntity;

    public void setEntity(T t) {
        editEntity = t;
        DataProducer.addObserver(t, this);
        onEntityUpdated();
    }

    public T getEntity() {
        return editEntity;
    }

    abstract public void setDrawMode(D drawMode);

    abstract public void showShiftUI();

    abstract public void buildEditorUI(Pane targetPane);

    abstract public void unregister();

    abstract public void copy();

    abstract public void copyData();

    abstract public void paste();

    abstract public void select();

    abstract public void selectNone();
    int startX = 0;
    int startY = 0;
    int endX = 0;
    int endY = 0;

    public void setSelectionArea(int x1, int y1, int x2, int y2) {
        startX = Math.min(x1, x2);
        startY = Math.min(y1, y2);
        endX = Math.max(x1, x2);
        endY = Math.max(y1, y2);
        if (startX + startY + endX + endY <= 0) {
            selectInfo = null;
        } else {
            selectInfo = "x1/" + startX + "/y1/" + startY + "/x2/" + endX + "/y2/" + endY;
        }
    }

    public String getSelectedAllInfo() {
        return "all";
    }

    String selectInfo;
    public String getSelectionInfo() {
        if (selectInfo == null) {
            return getSelectedAllInfo();
        }
        return selectInfo;
    }

    public void addScript(Script script) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    abstract public void redraw();

    protected void trackState() {
        if (undoStates.size() >= UNDO_HISTORY_LENGTH) {
            undoStates.removeLast();
        }
        try {
            undoStates.push(TransferHelper.cloneObject(getEntity(), (Class<T>) getEntity().getClass(), getEntity().getClass().getName()));
        } catch (JAXBException ex) {
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void undo() {
        if (!undoStates.isEmpty()) {
            T undoState = undoStates.removeFirst();
            setEntity(undoState);            
            onEntityUpdated();
            redraw();
        }
    }

    protected void onEntityUpdated() {

    }

    public abstract void showSelectorModal();
}
