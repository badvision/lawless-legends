package org.badvision.outlaweditor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/**
 * Simplify management of drag/drop operations
 *
 * @author blurry
 * @param <T> Type of object being passed
 */
public class TransferHelper<T> {

    Class type;
    DataFormat format;
    static Random random = new Random();
    static Map<String, Object> registry = new HashMap<>();
    static Map<String, DataFormat> dataFormats = new HashMap<>();

    public interface DropEventHandler<T> {
        public void handle(T object, double x, double y);
    }

    private TransferHelper() {
    }

    public TransferHelper(Class<T> clazz) {
        type = clazz;
        format = getDataFormat(clazz);
    }

    public static DataFormat getDataFormat(Class clazz) {
        if (!dataFormats.containsKey(clazz.getName())) {
            dataFormats.put(clazz.getName(), new DataFormat(clazz.getName()));
        }
        return dataFormats.get(clazz.getName());
    }

    public void registerDragSupport(final Node source, final T object) {
        final String id = type.getName() + "_" + random.nextInt(999999999);
        source.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                registry.put(id, object);
                Dragboard db = source.startDragAndDrop(TransferMode.LINK);
                ClipboardContent content = new ClipboardContent();
                content.put(format, id);
                db.setContent(content);
                event.consume();
            }
        });
        source.setOnDragDone(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                registry.remove(id);
            }
        });
    }
    public void registerDropSupport(final Node target, final DropEventHandler<T> handler) {
        target.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.getContentTypes().contains(format)) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
                event.consume();
            }
        });
        target.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.getContentTypes().contains(format)) {
                    event.setDropCompleted(true);
                    String id = (String) db.getContent(format);
                    T object = (T) registry.get(id);
                    handler.handle(object, event.getX(), event.getY());
                } else {
                    event.setDropCompleted(false);
                }
                event.consume();
            }
        });
    }
}
