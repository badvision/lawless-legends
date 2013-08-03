package org.badvision.outlaweditor;

import javafx.scene.layout.AnchorPane;
import org.badvision.outlaweditor.data.DataObserver;
import org.badvision.outlaweditor.data.DataProducer;

/**
 * Extremely generic editor abstraction -- useful for uniform edit features across application
 * @author brobert
 */
public abstract class Editor<T,D> implements DataObserver<T> {
    T editEntity;
    public void setEntity(T t) {
        editEntity = t;
        DataProducer.addObserver(t, this);
    }
    public T getEntity() {
        return editEntity;
    }
    abstract public void setDrawMode(D drawMode);
    abstract public void showShiftUI();
    abstract public void buildEditorUI(AnchorPane tileEditorAnchorPane);
    abstract public void unregister();

    abstract public void copy();
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
        if (startX + startY + endX + endY == 0) {
            selectInfo = null;
        } else {
            selectInfo="x1/"+startX+"/y1/"+startY+"/x2/"+endX+"/y2/"+endY;
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
    };
    
}
