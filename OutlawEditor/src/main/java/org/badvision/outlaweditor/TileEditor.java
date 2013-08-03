package org.badvision.outlaweditor;

/**
 *
 * @author brobert
 */
import javafx.scene.control.Menu;
import org.badvision.outlaweditor.data.xml.Tile;

public abstract class TileEditor extends Editor<Tile, TileEditor.DrawMode> {
    abstract public void buildPatternSelector(Menu tilePatternMenu);
    
    public static enum DrawMode {
        Pencil1px, Pencil3px, Toggle
    }
}
