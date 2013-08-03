package org.badvision.outlaweditor.data;

import java.io.Serializable;
import java.util.Collection;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class TileInstance implements Serializable {
    transient Tile tile;
    String tileId;
    public TileInstance(Tile t) {
        tile = t;
        if (t != null) {
            tileId = t.getId();
        }
    }
    
    public Tile getTile() {
        if (tile == null && tileId != null) {
            tile = TilesetUtils.getTileById(tileId);
        }
        return tile;
    }
}
