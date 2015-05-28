package org.badvision.outlaweditor;

import javafx.scene.layout.Pane;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Scripts;

public class GlobalEditor extends Editor<Global, Void>{

    @Override
    public void addScript(Script script) {
        Scripts scripts = Application.gameData.getGlobal().getScripts();
        if (scripts == null) {
            Application.gameData.getGlobal().setScripts(new Scripts());
            scripts = Application.gameData.getGlobal().getScripts();
        }
        scripts.getScript().add(script);
    }
    

    public void removeScript(Script script) {
        Scripts scripts = Application.gameData.getGlobal().getScripts();
        scripts.getScript().remove(script);
    }    

    @Override
    public void setDrawMode(Void drawMode) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showShiftUI() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void buildEditorUI(Pane targetPane) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregister() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void copy() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void paste() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void select() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectNone() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void redraw() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void observedObjectChanged(Global object) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
