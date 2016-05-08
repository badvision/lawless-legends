/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.ui.ToolType;

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
        source.setOnDragDetected((MouseEvent event) -> {
            registry.put(id, object);
            Dragboard db = source.startDragAndDrop(TransferMode.LINK);
            if (type.isAssignableFrom(ToolType.class)) {
                ToolType tool = (ToolType) object;
                tool.getIcon().ifPresent(db::setDragView);
            }
            ClipboardContent content = new ClipboardContent();
            content.put(format, id);
            db.setContent(content);
            event.consume();
            dropSupportRegisterHandler.run();
        });
        source.setOnDragDone((DragEvent event) -> {
            registry.remove(id);
            dropSupportRegisterHandler.run();
        });
    }

    Runnable dropSupportRegisterHandler;
    Runnable dropSupportUnregisterHandler;

    public void registerDropSupport(final Node target, final DropEventHandler<T> handler) {
        dropSupportUnregisterHandler = () -> {
            target.setOnDragOver(null);
            target.setOnDragDropped(null);
        };
        dropSupportRegisterHandler = () -> {
            target.setOnDragOver((DragEvent event) -> {
                Dragboard db = event.getDragboard();
                if (db.getContentTypes().contains(format)) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
                event.consume();
            });
            target.setOnDragDropped((DragEvent event) -> {
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
            });
        };
    }

    public static <U> U cloneObject(U source, Class<U> type, String nodeType) throws JAXBException {
        JAXBContext sourceJAXBContext = JAXBContext.newInstance(source.getClass());
        Marshaller jaxbMarshaller = sourceJAXBContext.createMarshaller();

        // format the XML output
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        JAXBContext targetJAXBContext = JAXBContext.newInstance(source.getClass());
        QName qName = new QName("info.source4code.jaxb.model", nodeType);
        JAXBElement<U> root = new JAXBElement<>(qName, type, source);
        JAXBElement<U> cloneRoot = targetJAXBContext.createUnmarshaller().unmarshal(new JAXBSource(sourceJAXBContext, root), type);
        return cloneRoot.getValue();
    }

    public static Map<String, Integer> getSelectionDetails(String contentPath) {
        String[] bufferDetails = contentPath.split("/");
        Map<String, Integer> details = new HashMap<>();
        for (int i = 1; i < bufferDetails.length; i += 2) {
            details.put(
                    bufferDetails[i], 
                    (i+1 < bufferDetails.length) ? Integer.parseInt(bufferDetails[i + 1]) : -1);
        }
        return details;
    }
}
