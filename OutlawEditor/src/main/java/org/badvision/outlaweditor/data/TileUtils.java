package org.badvision.outlaweditor.data;

import org.badvision.outlaweditor.Platform;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.WritableImage;
import org.badvision.outlaweditor.data.xml.*;

/**
 *
 * @author brobert
 */
public class TileUtils {

    static Map<String, Map<Platform, WritableImage>> display;

    static {
        clear();
    }

    public static void clear() {
        display = new ConcurrentHashMap<>();
    }

    public static Tile newTile() {
        Tile t = new Tile();
        t.setObstruction(false);
        updateId(t);
        return t;
    }

    public static void updateId(Tile t) {
        if (t.getId() == null) {
            t.setId("TILE" + Integer.toHexString((int) (Math.random() * 1000000.0)));
        }
    }

    public static String getId(Tile t) {
        if (t.getId() == null) {
            updateId(t);
        }
        return t.getId();
    }

    public static Map<Platform, WritableImage> getDisplay(Tile t) {
        if (display.get(getId(t)) == null) {
            display.put(getId(t), new EnumMap<>(Platform.class));
        }
        return display.get(getId(t));
    }

    public static void redrawTile(Tile t) {
        Map<Platform, WritableImage> displays = getDisplay(t);
        for (PlatformData d : t.getDisplayData()) {
            Platform p;
            try {
                p = Platform.valueOf(d.getPlatform());
            } catch (IllegalArgumentException e) {
                System.err.println("Unable to find any platform support for '" + d.getPlatform() + "'");
                continue;
            }
            displays.put(p, p.tileRenderer.redrawSprite(d.getValue(), displays.get(p)));
        }
        DataProducer.notifyObservers(t);
    }

    public static byte[] getPlatformData(Tile t, Platform p) {
        for (PlatformData d : t.getDisplayData()) {
            if (d.getPlatform().equalsIgnoreCase(p.name())) {
                return d.getValue();
            }
        }
        byte[] out = new byte[p.dataHeight * p.dataWidth];
        setPlatformData(t, p, out);
        return out;
    }

    public static WritableImage getImage(Tile t, Platform p) {
        Map<Platform, WritableImage> displays = getDisplay(t);
        byte[] data = getPlatformData(t, p);
        return displays.put(p, p.tileRenderer.redrawSprite(data, displays.get(p)));
    }

    public static void setImage(Tile t, Platform p, WritableImage img) {
        Map<Platform, WritableImage> displays = getDisplay(t);
        displays.put(p, img);
    }

    public static void setPlatformData(Tile t, Platform p, byte[] b) {
        for (PlatformData d : t.getDisplayData()) {
            if (d.getPlatform().equalsIgnoreCase(p.name())) {
                d.setValue(b);
                return;
            }
        }
        PlatformData d = new PlatformData();
        d.setPlatform(p.name());
        d.setValue(b);
        t.getDisplayData().add(d);
    }
}
