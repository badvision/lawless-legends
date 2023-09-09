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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.badvision.outlaweditor.api.ApplicationState;
import static org.badvision.outlaweditor.data.DataUtilities.extract;
import static org.badvision.outlaweditor.data.DataUtilities.extractFirst;
import org.badvision.outlaweditor.data.xml.Arg;
import org.badvision.outlaweditor.data.xml.Block;
import org.badvision.outlaweditor.data.xml.Global;
import org.badvision.outlaweditor.data.xml.Mutation;
import org.badvision.outlaweditor.data.xml.Scope;
import org.badvision.outlaweditor.data.xml.Script;
import org.badvision.outlaweditor.data.xml.Statement;
import org.badvision.outlaweditor.data.xml.UserType;
import org.badvision.outlaweditor.data.xml.Variable;
import org.badvision.outlaweditor.spelling.SpellChecker;
import org.badvision.outlaweditor.spelling.SpellResponse;
import org.badvision.outlaweditor.spelling.Suggestion;
import org.badvision.outlaweditor.ui.ApplicationUIController;
import org.badvision.outlaweditor.ui.MythosScriptEditorController;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Mythos Scripting Editor
 *
 * @author blurry
 */
public class MythosEditor {

    Scope scope;
    Script script;
    Stage primaryStage;
    MythosScriptEditorController controller;
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    SpellChecker spellChecker;

    public MythosEditor(Script theScript, Scope theScope) {
        script = theScript;
        scope = theScope;
        spellChecker = new SpellChecker();
        fixMutators(script.getBlock());
    }

    public void show() {
        primaryStage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MythosScriptEditor.fxml"));
        try {
            AnchorPane node = (AnchorPane) fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.setEditor(this);
            Scene s = new Scene(node);
            primaryStage.setScene(s);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        primaryStage.setOnCloseRequest((final WindowEvent t) -> {
            t.consume();
        });
        primaryStage.show();
    }

    public void close() {
        javafx.application.Platform.runLater(() -> {
            primaryStage.getScene().getRoot().setDisable(true);
            primaryStage.close();
        });
    }

    public void applyChanges() {
        try {
            String xml = controller.getScriptXml()
                    .replaceFirst(Pattern.quote("<block"), "<block xmlns=\"outlaw\"");
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
                QName qName = new QName("outlaw", "block");
                JAXBElement<Block> root = new JAXBElement<>(qName, Block.class, script.getBlock());
                context.createMarshaller().marshal(root, buffer);
                String xml = buffer.toString();
                xml = xml.replaceAll("'", "&apos;");
                xml = xml.replace("?>", "?><xml>");
                xml += "</xml>";
                return generateLoadScript(xml);
            } catch (JAXBException ex) {
                Logger.getLogger(MythosEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public String generateLoadScript(String xml) {
        xml = xml.replaceAll("'", "\\'");
        xml = xml.replaceAll("\n", "");
        String loadScript = "Mythos.initCustomDefinitions();";
        loadScript += "Mythos.setScriptXml('" + xml + "');";
        return loadScript;
    }

    private String getDefaultBlockMarkup() {
        return XML_HEADER + "<xml><block type=\"procedures_defreturn\" id=\"1\" inline=\"false\" x=\"5\" y=\"5\"><mutation></mutation><field name=\"NAME\">NewScript</field></block></xml>";
    }

    // Called when the name of the root block is changed in the JS editor
    public void setFunctionName(String name) {
        if (script == null) {
            return;
        }
        script.setName(name);
        ApplicationUIController.getController().redrawScripts();
    }

    public List<UserType> getUserTypes() {
        Global global = (Global) getGlobalScope();
        if (global.getUserTypes() == null) {
            return new ArrayList<>();
        } else {
            return global.getUserTypes().getUserType();
        }
    }

    public List<Script> getGlobalFunctions() {
        return getFunctions(getGlobalScope());
    }

    public List<Script> getLocalFunctions() {
        return getFunctions(scope);
    }

    private List<Script> getFunctions(Scope scriptScope) {
        if (scriptScope.getScripts() == null) {
            return new ArrayList<>();
        } else {
            List<Script> scripts = scriptScope.getScripts().getScript();
            List<Script> filteredList = scripts.stream()
                    .filter(s -> s.getName() != null)
                    .collect(Collectors.toList());
            return filteredList;
        }
    }

    public List<Variable> getGlobalVariables() {
        return getVariables(getGlobalScope());
    }

    public static Scope getGlobalScope() {
        return ApplicationState.getInstance().getGameData().getGlobal();
    }

    private boolean isGlobalScope() {
        return scope.equals(getGlobalScope());
    }

    public List<Variable> getLocalVariables() {
        return getVariables(scope);
    }

    private List<Variable> getVariables(Scope scriptScope) {
        if (scriptScope.getVariables() == null) {
            return new ArrayList<>();
        } else {
            return scriptScope.getVariables().getVariable();
        }
    }

    public List<Variable> getVariablesByType(String type) {
        Stream<Variable> allGlobals = Stream.empty();
        if (!isGlobalScope()) {
            allGlobals = getGlobalVariables().stream();
        }
        Stream<Variable> allLocals = getLocalVariables().stream();
        return Stream.concat(allGlobals, allLocals)
                .filter((v) -> v.getType().equals(type))
                .collect(Collectors.toList());
    }

    public List<String> getParametersForScript(Script script) {
        List<String> allArgs = new ArrayList();
        if (script.getBlock() != null) {
            extractFirst(script.getBlock(), Mutation.class)
                    .ifPresent((m) -> m.getArg().stream().map(Arg::getName).forEach(allArgs::add));
        }
        return allArgs;
    }

    public String checkSpelling(String value) {
        SpellResponse result = spellChecker.check(value);
        if (result.getErrors() == 0) {
            return null;
        } else {
            StringBuilder message = new StringBuilder();
            result.getCorrections().forEach((SpellResponse.Source source, Set<Suggestion> suggestions) -> {
                message
                        .append(source.word)
                        .append(": ")
                        .append(suggestions.stream().map(Suggestion::getWord).limit(5).collect(Collectors.joining(",")))
                        .append("\n");
            });
            return message.toString();
        }
    }

    public void log(String message) {
        Logger.getLogger(getClass().getName()).warning(message);
        System.out.println(message);
    }

    public static enum MutationType {
        controls_if(MythosEditor::fixIfStatement);

        Consumer<Block> rebuildMutation;

        MutationType(Consumer<Block> rebuilder) {
            rebuildMutation = rebuilder;
        }
    }

    private void fixMutators(Block block) {
        extractFirst(block, Mutation.class).ifPresent((mutation) -> {
            if (mutation.getOtherAttributes().isEmpty()) {
                try {
                    MutationType type = MutationType.valueOf(block.getType());
                    type.rebuildMutation.accept(block);
                } catch (IllegalArgumentException ex) {
                    // No big deal, it just doesn't have a mutation we know how to handle
                }
            }
        });

        extract(block, Statement.class).map(Statement::getBlock).flatMap(List::stream).forEach(this::fixMutators);
        if (block != null && block.getNext() != null && block.getNext().getBlock() != null) {
            fixMutators(block.getNext().getBlock());
        }
    }

    private static void fixIfStatement(Block block) {
        Mutation mutation = extractFirst(block, Mutation.class).get();
        long doCount = extract(block, Statement.class).filter((s) -> s.getName().startsWith("DO")).collect(Collectors.counting());
        long elseCount = extract(block, Statement.class).filter((s) -> s.getName().startsWith("ELSE")).collect(Collectors.counting());
        if (doCount > 1) {
            mutation.getOtherAttributes().put(new QName("elseif"), String.valueOf(doCount - 1));
        }
        if (elseCount > 0) {
            mutation.getOtherAttributes().put(new QName("else"), String.valueOf(elseCount));
        }
    }

}
