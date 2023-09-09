module outlaweditor {
    requires org.controlsfx.controls;
    requires javafx.controls;
    requires javafx.graphics;
    requires jdk.jsobject;
    requires javafx.web;
    requires javafx.media;
    requires javafx.fxml;
    requires java.logging;
    requires java.desktop;
    requires java.scripting;
    requires java.xml;
    requires jakarta.xml.bind;
    requires org.apache.poi.ooxml;

    opens org.badvision.outlaweditor to javafx.graphics, javafx.fxml, javafx.web, org.apache.poi.ooxml;
    opens org.badvision.outlaweditor.ui to javafx.fxml;
    opens org.badvision.outlaweditor.ui.impl to javafx.fxml;
    opens org.badvision.outlaweditor.data to jakarta.xml.bind;
    opens org.badvision.outlaweditor.data.xml to javafx.base, jakarta.xml.bind, javafx.web;
}