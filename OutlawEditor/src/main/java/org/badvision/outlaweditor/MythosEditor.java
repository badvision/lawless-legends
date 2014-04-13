package org.badvision.outlaweditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.badvision.outlaweditor.data.xml.Block;
import org.badvision.outlaweditor.data.xml.Script;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Mythos Scripting Editor
 *
 * @author blurry
 */
public class MythosEditor {

    Script script;
    Stage primaryStage;
    MythosScriptEditorController controller;
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    public MythosEditor(Script theScript) {
        script = theScript;
    }

    public void show() {
        primaryStage = new Stage();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MythosScriptEditor.fxml"));
        Map<String,String> properties = new HashMap<>();
        properties.put(MythosScriptEditorController.ONLOAD_SCRIPT, generateLoadScript());
        fxmlLoader.setResources(MythosScriptEditorController.createResourceBundle(properties));
        try {
            AnchorPane node = (AnchorPane) fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.setEditor(this);
            Scene s = new Scene(node);
            primaryStage.setScene(s);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent t) {
                t.consume();
            }
        });
        primaryStage.show();
    }

    public void close() {
        javafx.application.Platform.runLater(new Runnable() {
            @Override
            public void run() {
                primaryStage.getScene().getRoot().setDisable(true);
                primaryStage.close();
            }
        });
    }

    public void applyChanges() {
        try {
            String xml = controller.getScriptXml();
            JAXBContext context = JAXBContext.newInstance("org.badvision.outlaweditor.data.xml");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            JAXBElement<Block> b = unmarshaller.unmarshal(doc, Block.class);
            script.setBlock(b.getValue());
        } catch (JAXBException | ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(MythosEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String generateLoadScript() {
        if (script == null || script.getBlock() == null) {
            return generateLoadScript(getDefaultBlockMarkup());
        } else {
            try {
                JAXBContext context = JAXBContext.newInstance(Block.class);
                StringWriter buffer = new StringWriter();
                context.createMarshaller().marshal(script.getBlock(), buffer);
                String xml = buffer.toString();
                return generateLoadScript(xml);
            } catch (JAXBException ex) {
                Logger.getLogger(MythosEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public String generateLoadScript(String xml) {
        xml = XML_HEADER + xml;
        xml = xml.replaceAll("'", "\\'");
        xml = xml.replaceAll("\n", "");
        String loadScript = "Mythos.setScriptXml('"+xml+"');";
        return loadScript;
    }

    private String getDefaultBlockMarkup() {
        return "<xml><block type=\"procedures_defreturn\" id=\"1\" inline=\"false\" x=\"5\" y=\"5\"><mutation></mutation><field name=\"NAME\">NewScript</field></block></xml>";
    }
    
    // Called when the name of the root block is changed in the JS editor
    public void setFunctionName(String name) {
        System.out.println("Function title changed! >> "+name);
    }
}
