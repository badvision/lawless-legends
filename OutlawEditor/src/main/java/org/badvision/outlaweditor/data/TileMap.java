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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javax.xml.bind.JAXBElement;
import org.badvision.outlaweditor.Application;
import org.badvision.outlaweditor.Platform;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Map.Chunk;
import org.badvision.outlaweditor.data.xml.ObjectFactory;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Script.LocationTrigger;
import org.badvision.outlaweditor.data.xml.Scripts;
import org.badvision.outlaweditor.data.xml.Tile;
import org.badvision.outlaweditor.ui.UIAction;

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

    public static final double SATURATION = 0.70;
    public static final double VALUE = 1.0;
    public static double HUE = 180;
    private final java.util.Map<Integer, List<Script>> locationScripts = new HashMap<>();
    private final java.util.Map<Script, Color> scriptColors = new HashMap<>();

    public Optional<Color> getScriptColor(Script s) {
        return Optional.ofNullable(scriptColors.get(s));
    }

    public List<Script> getLocationScripts(int x, int y) {
        List<Script> list = locationScripts.get(getMortonNumber(x, y));
        if (list != null) {
            return list;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public void putLocationScript(int x, int y, Script s) {
        LocationTrigger trigger = new Script.LocationTrigger();
        trigger.setX(x);
        trigger.setY(y);
        s.getLocationTrigger().add(trigger);
        registerLocationScript(x, y, s);
    }

    public void removeLocationScripts(int x, int y) {
        int loc = getMortonNumber(x, y);
        List<Script> scripts = locationScripts.get(loc);
        if (scripts != null) {
            scripts.forEach(s -> {
                s.getLocationTrigger().removeIf(t -> {
                    return t.getX() == x && t.getY() == y;
                });
            });
        }
        locationScripts.remove(loc);
        Application.getInstance().getController().redrawScripts();
    }

    private void registerLocationScript(int x, int y, Script s) {
        if (!scriptColors.containsKey(s)) {
            scriptColors.put(s, Color.hsb(HUE, SATURATION, 0.75 + Math.cos(HUE / Math.PI / 2.0) / 8.0));
            HUE = (HUE + 27) % 360;
        }
        int loc = getMortonNumber(x, y);
        List<Script> list = locationScripts.get(loc);
        if (list == null) {
            list = new ArrayList<>();
            locationScripts.put(loc, list);
        }
        list.add(s);
        Application.getInstance().getController().redrawScripts();
    }

    private int getMortonNumber(int x, int y) {
        int morton = 0;
        for (int i = 0; i < 16; i++) {
            int mask = 1 << (i);
            morton += (x & mask) << (i + 1);
            morton += (y & mask) << i;
        }
        return morton;
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
            set(y, new ArrayList<>());
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
        Scripts scripts = m.getScripts();
        if (scripts != null) {
            List<Script> allScripts = new ArrayList<>(scripts.getScript());
            allScripts.forEach(
                    s -> s.getLocationTrigger().forEach(
                            l -> registerLocationScript(l.getX(), l.getY(), s)
                    )
            );
        }
        m.getChunk().forEach(c -> {
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
        });
        if (!unknownTiles.isEmpty()) {
            int numMissing = unknownTiles.size();
            Alert missingTileAlert = new Alert(Alert.AlertType.WARNING);
            missingTileAlert.setContentText(
                    (numMissing > 1
                            ? "There were " + numMissing + " missing tiles." : "There was a missing tile.  ")
                    + "Blank placeholders have been added.");
            missingTileAlert.showAndWait();
        }
        backingMap = m;
        backingMapStale = false;
    }
    public static String NULL_TILE_ID = "_";

    public static boolean isNullTile(String tileId) {
        return tileId.equalsIgnoreCase(NULL_TILE_ID);
    }

    public void removeScriptFromMap(Script script) {
        script.getLocationTrigger().clear();
        locationScripts.values().stream().filter((scripts) -> !(scripts == null)).forEach((scripts) -> {
            scripts.remove(script);
        });
        backingMap.getScripts().getScript().remove(script);
    }
}
