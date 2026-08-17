package org.badvision.outlaweditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Test to verify if JavaFX CSS properties bleed into WebView content.
 * 
 * The test creates a WebView and applies various -fx-* CSS properties directly
 * to the WebView element to see if they corrupt the HTML rendering.
 */
public class WebViewCSSTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Test 1: WebView with -fx-background-color set to a non-transparent value
        WebView webViewWithCSS = new WebView();
        webViewWithCSS.getEngine().loadContent(
            "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { background-color: #fff; margin: 0; padding: 20px; }" +
            ".test-box { background-color: #ff0000; color: white; padding: 20px; margin: 10px; }" +
            "h1 { color: #333; }" +
            "</style></head><body>" +
            "<h1>WebView CSS Bleed Test</h1>" +
            "<div class='test-box'>This red box should be #ff0000</div>" +
            "<p>If JavaFX CSS is bleeding, the background may appear tinted or the colors may be wrong.</p>" +
            "</body></html>"
        );

        // Apply CSS properties that could bleed into WebView
        webViewWithCSS.setStyle("-fx-background-color: #00ff00; -fx-padding: 10px; -fx-effect: null;");

        // Test 2: WebView with NO CSS applied (control)
        WebView webViewNoCSS = new WebView();
        webViewNoCSS.getEngine().loadContent(
            "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { background-color: #fff; margin: 0; padding: 20px; }" +
            ".test-box { background-color: #ff0000; color: white; padding: 20px; margin: 10px; }" +
            "h1 { color: #333; }" +
            "</style></head><body>" +
            "<h1>WebView No CSS Test (Control)</h1>" +
            "<div class='test-box'>This red box should be #ff0000</div>" +
            "<p>This WebView has no JavaFX CSS applied.</p>" +
            "</body></html>"
        );

        // Test 3: WebView with -fx-background-color: transparent (potential fix)
        WebView webViewTransparent = new WebView();
        webViewTransparent.getEngine().loadContent(
            "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { background-color: #fff; margin: 0; padding: 20px; }" +
            ".test-box { background-color: #ff0000; color: white; padding: 20px; margin: 10px; }" +
            "h1 { color: #333; }" +
            "</style></head><body>" +
            "<h1>WebView Transparent CSS Test</h1>" +
            "<div class='test-box'>This red box should be #ff0000</div>" +
            "<p>This WebView has -fx-background-color: transparent applied.</p>" +
            "</body></html>"
        );
        webViewTransparent.setStyle("-fx-background-color: transparent; -fx-padding: 10px;");

        // Test 4: WebView with explicit HTML background override
        WebView webViewHTMLOverride = new WebView();
        webViewHTMLOverride.getEngine().loadContent(
            "<!DOCTYPE html>" +
            "<html><head><style>" +
            "html, body { background-color: #ffffff !important; margin: 0; padding: 0; }" +
            ".test-box { background-color: #ff0000; color: white; padding: 20px; margin: 10px; }" +
            "h1 { color: #333; }" +
            "</style></head><body>" +
            "<h1>WebView HTML Override Test</h1>" +
            "<div class='test-box'>This red box should be #ff0000</div>" +
            "<p>This WebView uses !important in HTML CSS to override any JavaFX bleeding.</p>" +
            "</body></html>"
        );
        webViewHTMLOverride.setStyle("-fx-background-color: #00ff00; -fx-padding: 10px;");

        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 20; -fx-background-color: #cccccc;");

        root.getChildren().addAll(
            new Label("Test 1: WebView with -fx-background-color: #00ff00 (GREEN bleed expected)"),
            webViewWithCSS,
            new Label("Test 2: WebView with NO CSS (Control - should look correct)"),
            webViewNoCSS,
            new Label("Test 3: WebView with -fx-background-color: transparent"),
            webViewTransparent,
            new Label("Test 4: WebView with HTML !important override"),
            webViewHTMLOverride
        );

        // Set reasonable sizes for each test
        webViewWithCSS.setPrefSize(600, 200);
        webViewNoCSS.setPrefSize(600, 200);
        webViewTransparent.setPrefSize(600, 200);
        webViewHTMLOverride.setPrefSize(600, 200);

        Scene scene = new Scene(root, 650, 950);
        primaryStage.setTitle("WebView CSS Bleed Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
