package org.badvision.outlaweditor;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.badvision.outlaweditor.data.xml.Block;
import org.badvision.outlaweditor.data.xml.Script;

/**
 * Mythos Scripting Editor
 * @author blurry
 */
public class MythosEditor {
    Script script;
    Stage primaryStage;
    MythosScriptEditorController controller;
    public MythosEditor(Script theScript) {
        script = theScript;
    }
    
    public void show() {
        primaryStage = new Stage();
        javafx.application.Platform.setImplicitExit(true);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MythosScriptEditor.fxml"));
        fxmlLoader.setResources(null);
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
        loadScript();
    }
    
    public void close() {
        primaryStage.close();
    }

    public void applyChanges() {
        try {
            String xml = String.valueOf(controller.editorView.getEngine().executeScript("Blockly.Xml.workspaceToDom(Blockly.mainWorkspace).outerHTML"));
            JAXBContext context = JAXBContext.newInstance(Block.class);
            Block scriptBlock = (Block) context.createUnmarshaller().unmarshal(new StringReader(xml));
            script.setBlock(scriptBlock);
        } catch (JAXBException ex) {
            Logger.getLogger(MythosEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void loadScript() {
        if (script == null || script.getBlock() == null) {
            loadScript(createDefaultScript());
            return;
        }
        try {
            JAXBContext context = JAXBContext.newInstance(Block.class);
            StringWriter buffer = new StringWriter();
            context.createMarshaller().marshal(script.getBlock(), buffer);
            String xml = buffer.toString();
            loadScript(xml);
        } catch (JAXBException ex) {
            Logger.getLogger(MythosEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void loadScript(String xml) {
        xml = xml.replaceAll("\"","\\\"");
        String loadScript = "var xml = Blockly.Xml.textToDom("+xml+");";
        loadScript += "Blockly.Xml.domToWorkspace(Blockly.mainWorkspace, xml);";
        controller.editorView.getEngine().executeScript(loadScript);        
    }

    private String createDefaultScript() {
        return "<block/>";
    }
}
