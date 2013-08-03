package org.badvision.outlaweditor.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.data.xml.Tile;

/**
 *
 * @author brobert
 */
public class TilesetUtils implements Serializable {

    public static void addObserver(DataObserver o) {
        DataProducer.addObserver(TilesetUtils.class, o);
    }

    public static boolean add(Tile e) {
        boolean output = Application.gameData.getTile().add(e);
        DataProducer.notifyObservers(TilesetUtils.class);
        return output;
    }

    public static void clear() {
        Application.gameData.getTile().clear();
        DataProducer.notifyObservers(TilesetUtils.class);
    }

    public static void remove(Tile t) {
        Application.gameData.getTile().remove(t);
        DataProducer.notifyObservers(TilesetUtils.class);
    }
    // The tileset should have been a map in retrospect but now it has to
    // stay this way to preserve compatibility with existing files.
    static Map<String, Tile> lookup;

    public static Tile getTileById(String tileId) {
        if (lookup == null || (lookup.get(tileId) == null && !lookup.containsKey(tileId))) {
            lookup = new HashMap<>();
            for (Tile t : Application.gameData.getTile()) {
                lookup.put(TileUtils.getId(t), t);
            }
            if (lookup.get(tileId) == null) {
                lookup.put(tileId, null);
            }
        }
        return lookup.get(tileId);
    }
}
