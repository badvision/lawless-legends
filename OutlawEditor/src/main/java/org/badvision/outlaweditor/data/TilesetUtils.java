/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.badvision.outlaweditor.api.ApplicationState;
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
        boolean output = ApplicationState.getInstance().getGameData().getTile().add(e);
        DataProducer.notifyObservers(TilesetUtils.class);
        return output;
    }

    public static void clear() {
        ApplicationState.getInstance().getGameData().getTile().clear();
        DataProducer.notifyObservers(TilesetUtils.class);
    }

    public static void remove(Tile t) {
        ApplicationState.getInstance().getGameData().getTile().remove(t);
        DataProducer.notifyObservers(TilesetUtils.class);
    }
    // The tileset should have been a map in retrospect but now it has to
    // stay this way to preserve compatibility with existing files.
    static Map<String, Tile> lookup;

    public static Tile getTileById(String tileId) {
        if (lookup == null || (lookup.get(tileId) == null && !lookup.containsKey(tileId))) {
            lookup = new HashMap<>();
            for (Tile t : ApplicationState.getInstance().getGameData().getTile()) {
                lookup.put(TileUtils.getId(t), t);
            }
            if (lookup.get(tileId) == null) {
                lookup.put(tileId, null);
            }
        }
        return lookup.get(tileId);
    }

    private TilesetUtils() {
    }
}
