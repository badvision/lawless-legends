package org.badvision.outlaweditor.data;

import org.badvision.outlaweditor.Platform;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.image.WritableImage;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBElement;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.ui.UIAction;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Map.Chunk;
import org.badvision.outlaweditor.data.xml.ObjectFactory;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class TileMap extends ArrayList<ArrayList<Tile>> implements Serializable {

    public static final long serialVersionUID = 6486309334559843742L;
    Map backingMap;
    boolean backingMapStale;
    int width;
    int height;

    public TileMap(Map m) {
        backingMapStale = false;
        width = 0;
        height = 0;
        loadFromMap(m);
    }

    public Tile get(int x, int y) {
        if (size() <= y || get(y) == null) {
            return null;
        }
        if (get(y).size() <= x) {
            return null;
        }
        return get(y).get(x);
    }

    public void put(int x, int y, Tile t) {
        width = Math.max(x + 1, width);
        height = Math.max(y + 1, height);
        for (int i = size(); i <= y; i++) {
            add(null);
        }
        if (get(y) == null) {
            set(y, new ArrayList<Tile>());
        }
        List<Tile> row = get(y);
        for (int i = row.size(); i <= x; i++) {
            row.add(null);
        }
        row.set(x, t);
        backingMapStale = true;
    }

    public Map getBackingMap() {
        return backingMap;
    }

    public void updateBackingMap() {
        ObjectFactory f = new ObjectFactory();
        backingMap.getChunk().clear();
        Chunk c = new Map.Chunk();
        c.setX(0);
        c.setY(0);
        for (int y = 0; y < height; y++) {
            List<String> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                Tile t = get(x, y);
                if (t == null) {
                    row.add(NULL_TILE_ID);
                } else {
                    row.add(TileUtils.getId(t));
                }
            }
            c.getRow().add(f.createMapChunkRow(row));
        }
        backingMap.getChunk().add(c);
        backingMapStale = false;
    }

    private void loadFromMap(Map m) {
        clear();
        width = 0;
        height = 0;
        Set<Tile> unknownTiles = new HashSet<>();
        for (Chunk c : m.getChunk()) {
            int y = c.getY();
            for (JAXBElement<List<String>> row : c.getRow()) {
                int x = c.getX();
                for (String tileId : row.getValue()) {
                    Tile t = null;
                    if (!isNullTile(tileId)) {
                        t = TilesetUtils.getTileById(tileId);
                        if (t == null) {
                            t = new Tile();
                            unknownTiles.add(t);
                            Platform p = Application.currentPlatform;
                            WritableImage img = UIAction.getBadImage(p.tileRenderer.getWidth(), p.tileRenderer.getHeight());
                            TileUtils.setImage(t, p, img);
                        }
                    }
                    put(x, y, t);
                    x++;
                }
                y++;
            }
        }
        if (!unknownTiles.isEmpty()) {
            int numMissing = unknownTiles.size();
            JOptionPane.showMessageDialog(null, (numMissing > 1
                    ? "There were " + numMissing + " missing tiles." : "There was a missing tile.")
                    + "Blank placeholders have been added.");
        }
        backingMap = m;
        backingMapStale = false;
    }
    public static String NULL_TILE_ID = "_";

    public static boolean isNullTile(String tileId) {
        return tileId.equalsIgnoreCase(NULL_TILE_ID);
    }
}
