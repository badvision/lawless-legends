package jace;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.core.Card;
import jace.core.Motherboard;
import jace.core.Utility;
import jace.core.Video;
import jace.lawless.LawlessComputer;
import jace.lawless.LawlessHacks;
import jace.library.MediaCache;
import jace.library.MediaConsumer;
import jace.library.MediaConsumerParent;
import jace.library.MediaEntry;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 *
 * @author blurry
 */
public class JaceUIController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private StackPane stackPane;

    @FXML
    private HBox notificationBox;

    @FXML
    private ImageView appleScreen;

    @FXML
    private BorderPane controlOverlay;

    @FXML
    private Slider speedSlider;

    @FXML
    private AnchorPane menuButtonPane;

    @FXML
    private Button menuButton;

    @FXML
    private ComboBox<String> musicSelection;
    
    @FXML
    private Slider speakerToggle;

    private final BooleanProperty aspectRatioCorrectionEnabled = new SimpleBooleanProperty(false);

    public static final double MIN_SPEED = 0.5;
    public static final double MAX_SPEED = 5.0;

    @FXML
    void initialize() {
        assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'JaceUI.fxml'.";
        assert stackPane != null : "fx:id=\"stackPane\" was not injected: check your FXML file 'JaceUI.fxml'.";
        assert notificationBox != null : "fx:id=\"notificationBox\" was not injected: check your FXML file 'JaceUI.fxml'.";
        assert appleScreen != null : "fx:id=\"appleScreen\" was not injected: check your FXML file 'JaceUI.fxml'.";
        speedSlider.setValue(1.0);
        controlOverlay.setVisible(false);
        menuButtonPane.setVisible(false);
        controlOverlay.setFocusTraversable(false);
        menuButtonPane.setFocusTraversable(true);
        NumberBinding aspectCorrectedWidth = rootPane.heightProperty().multiply(3.0).divide(2.0);
        NumberBinding width = new When(
                aspectRatioCorrectionEnabled.and(aspectCorrectedWidth.lessThan(rootPane.widthProperty()))
        ).then(aspectCorrectedWidth).otherwise(rootPane.widthProperty());
        appleScreen.fitWidthProperty().bind(width);
        appleScreen.fitHeightProperty().bind(rootPane.heightProperty());
        appleScreen.setVisible(false);
        rootPane.setOnDragEntered(this::processDragEnteredEvent);
        rootPane.setOnDragExited(this::processDragExitedEvent);
        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        rootPane.setOnMouseMoved(this::showMenuButton);
        rootPane.setOnMouseExited(this::hideControlOverlay);
        rootPane.setOnMouseClicked((evt)->{
            rootPane.requestFocus();
        });
        menuButton.setOnMouseClicked(this::showControlOverlay);
        controlOverlay.setOnMouseClicked(this::hideControlOverlay);
        delayTimer.getKeyFrames().add(new KeyFrame(Duration.millis(3000), evt -> {
            hideControlOverlay(null);
            rootPane.requestFocus();
        }));
        rootPane.requestFocus();
        speakerToggle.setValue(1.0);
        speakerToggle.setOnMouseClicked(evt -> {
            speakerEnabled = !speakerEnabled;
            int desiredValue = speakerEnabled ? 1 : 0;
            speakerToggle.setValue(desiredValue);
            Emulator.withComputer(computer -> {
                Motherboard.enableSpeaker = speakerEnabled;
                computer.motherboard.reconfigure();
                if (!speakerEnabled) {
                    computer.motherboard.speaker.detach();
                } else {
                    computer.motherboard.speaker.attach();                    
                }
            });
        });
    }
    boolean speakerEnabled = true;

    private void showMenuButton(MouseEvent evt) {
        if (!evt.isPrimaryButtonDown() && !evt.isSecondaryButtonDown() && !controlOverlay.isVisible()) {
            resetMenuButtonTimer();
            if (!menuButtonPane.isVisible()) {
                menuButtonPane.setVisible(true);
                FadeTransition ft = new FadeTransition(Duration.millis(500), menuButtonPane);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();
            }
        }
        rootPane.requestFocus();
    }

    Timeline delayTimer = new Timeline();

    private void resetMenuButtonTimer() {
        delayTimer.playFromStart();
    }

    private void showControlOverlay(MouseEvent evt) {
        if (!evt.isPrimaryButtonDown() && !evt.isSecondaryButtonDown()) {
            delayTimer.stop();
            menuButtonPane.setVisible(false);
            controlOverlay.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(500), controlOverlay);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
            rootPane.requestFocus();
        }
    }

    private void hideControlOverlay(MouseEvent evt) {
        if (evt == null || evt.getSource() != null && (
                evt.getSource() == musicSelection ||
                (evt.getSource() == rootPane && musicSelection.isFocused())
        )) {
            return;
        }
        if (menuButtonPane.isVisible()) {
            FadeTransition ft1 = new FadeTransition(Duration.millis(500), menuButtonPane);
            ft1.setFromValue(1.0);
            ft1.setToValue(0.0);
            ft1.setOnFinished(evt1 -> menuButtonPane.setVisible(false));
            ft1.play();
        }
        if (controlOverlay.isVisible()) {
            FadeTransition ft2 = new FadeTransition(Duration.millis(500), controlOverlay);
            ft2.setFromValue(1.0);
            ft2.setToValue(0.0);
            ft2.setOnFinished(evt1 -> controlOverlay.setVisible(false));
            ft2.play();
        }
    }

    protected double convertSpeedToRatio(Double setting) {
        if (setting < 1.0) {
            return 0.5;
        } else if (setting == 1.0) {
            return 1.0;
        } else if (setting >= 5) {
            return Double.MAX_VALUE;
        } else {
            return setting;
        }
    }

    Stage primaryStage;

    public void reconnectKeyboard() {
        Emulator.withComputer(computer -> {
            if (computer.getKeyboard() != null) {
                EventHandler<KeyEvent> keyboardHandler = computer.getKeyboard().getListener();
                primaryStage.setOnShowing(evt -> computer.getKeyboard().resetState());
                rootPane.setOnKeyPressed(keyboardHandler);
                rootPane.setOnKeyReleased(keyboardHandler);
                rootPane.setFocusTraversable(true);
            }
        });
    }

    private void connectControls(Stage ps) {
        primaryStage = ps;

        connectButtons(controlOverlay);
        speedSlider.setMinorTickCount(3);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMax(MAX_SPEED);
        speedSlider.setMin(MIN_SPEED);
        speedSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double val) {
                if (val <= MIN_SPEED) {
                    return "Half";
                } else if (val >= MAX_SPEED) {
                    return "∞";
                }
                double v = convertSpeedToRatio(val);
                if (v != Math.floor(v)) {
                    return v + "x";
                } else {
                    return (int) v + "x";
                }
            }

            @Override
            public Double fromString(String string) {
                return 1.0;
            }
        });
        Platform.runLater(() -> {
            double currentSpeed = (double) Emulator.withComputer(c->c.getMotherboard().getSpeedRatio(), 100) / 100.0;
            speedSlider.valueProperty().set(currentSpeed);
            speedSlider.valueProperty().addListener((val, oldValue, newValue) -> setSpeed(newValue.doubleValue()));
        });
        musicSelection.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
            Emulator.withComputer(computer -> 
                ((LawlessHacks) ((LawlessComputer) computer).activeCheatEngine).changeMusicScore(String.valueOf(newValue))
            )
        );
        reconnectKeyboard();
    }

    private void connectButtons(Node n) {
        if (n instanceof Button button) {
            Function<Boolean, Boolean> action = Utility.getNamedInvokableAction(button.getText());
            button.setOnMouseClicked(evt -> action.apply(false));
        } else if (n instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> connectButtons(child));
        }
    }

    public void setSpeed(double speed) {
        double newSpeed = Math.max(speed, MIN_SPEED);
        if (speedSlider.getValue() != speed) {
            Platform.runLater(()->speedSlider.setValue(newSpeed));
        }
        if (newSpeed >= MAX_SPEED) {
            Emulator.withComputer(c -> {
                c.getMotherboard().setMaxSpeed(true);
            });
        } else {
            Emulator.withComputer(c -> {
                c.getMotherboard().setMaxSpeed(false);
                c.getMotherboard().setSpeedInPercentage((int) (newSpeed * 100));
            });
        }
    }

    public void toggleAspectRatio() {
        setAspectRatioEnabled(aspectRatioCorrectionEnabled.not().get());
    }

    public void setAspectRatioEnabled(boolean enabled) {
        aspectRatioCorrectionEnabled.set(enabled);
    }

    public void connectComputer(Stage primaryStage) {
        Platform.runLater(() -> {
            connectControls(primaryStage);
            Emulator.withVideo(this::connectVideo);
            appleScreen.setVisible(true);
            rootPane.requestFocus();
        });
    }

    public void connectVideo(Video video) {
        if (video != null) {
            appleScreen.setImage(video.getFrameBuffer());
        } else {
            appleScreen.setImage(null);
        }
    }

    private void processDragEnteredEvent(DragEvent evt) {
        MediaEntry media = null;
        if (evt.getDragboard().hasFiles()) {
            media = MediaCache.getMediaFromFile(getDraggedFile(evt.getDragboard().getFiles()));
        } else if (evt.getDragboard().hasUrl()) {
            media = MediaCache.getMediaFromUrl(evt.getDragboard().getUrl());
        } else if (evt.getDragboard().hasString()) {
            String path = evt.getDragboard().getString();
            try {
                URI.create(path);
                media = MediaCache.getMediaFromUrl(path);
            } catch (IllegalArgumentException ex) {
                File f = new File(path);
                if (f.exists()) {
                    media = MediaCache.getMediaFromFile(f);
                }
            }
        }
        if (media != null) {
            evt.acceptTransferModes(TransferMode.LINK, TransferMode.COPY);
            startDragEvent(media);
        }
    }

    private void processDragExitedEvent(DragEvent evt) {
        endDragEvent();
    }

    private File getDraggedFile(List<File> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        for (File f : files) {
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    HBox drivePanel;

    private void startDragEvent(MediaEntry media) {
        List<MediaConsumer> consumers = getMediaConsumers();
        drivePanel = new HBox();
        consumers.stream()
                .filter((consumer) -> (consumer.isAccepted(media, media.files.get(0))))
                .forEach((consumer) -> {
                    Label icon = consumer.getIcon().orElse(null);
                    if (icon == null) {
                        return;
                    }
                    icon.setTextFill(Color.WHITE);
                    icon.setPadding(new Insets(2.0));
                    drivePanel.getChildren().add(icon);
                    icon.setOnDragOver(event -> {
                        event.acceptTransferModes(TransferMode.ANY);
                        event.consume();
                    });
                    icon.setOnDragDropped(event -> {
                        System.out.println("Dropping media on " + icon.getText());
                        Emulator.whileSuspended(c-> {
                            try {
                                consumer.insertMedia(media, media.files.get(0));
                            } catch (IOException ex) {
                                Logger.getLogger(JaceUIController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                        event.setDropCompleted(true);
                        event.consume();
                        endDragEvent();
                    });
                });
        stackPane.getChildren().add(drivePanel);
        drivePanel.setLayoutX(10);
        drivePanel.setLayoutY(10);
    }

    private void endDragEvent() {
        stackPane.getChildren().remove(drivePanel);
        drivePanel.getChildren().forEach((n) -> n.setOnDragDropped(null));
    }

    private List<MediaConsumer> getMediaConsumers() {
        List<MediaConsumer> consumers = new ArrayList<>();
        Emulator.withComputer(c -> consumers.add(((LawlessComputer) c).getUpgradeHandler()));
        if (Emulator.getUILogic().showDrives) {
            Emulator.withMemory(m -> {
                for (Optional<Card> card : m.getAllCards()) {
                    card.filter(c -> c instanceof MediaConsumerParent).ifPresent(parent ->
                        consumers.addAll(Arrays.asList(((MediaConsumerParent) parent).getConsumers()))
                    );
                }
            });
        }
        return consumers;
    }

    Map<Label, Long> iconTTL = new ConcurrentHashMap<>();

    public void addIndicator(Label icon) {
        addIndicator(icon, 250);
    }

    public void addIndicator(Label icon, long TTL) {
        if (!iconTTL.containsKey(icon)) {
            Platform.runLater(() -> {
                if (!notificationBox.getChildren().contains(icon)) {
                    notificationBox.getChildren().add(0, icon);;
                }
            });
        }
        trackTTL(icon, TTL);
    }

    public void removeIndicator(Label icon) {
        Platform.runLater(() -> {
            notificationBox.getChildren().remove(icon);
            iconTTL.remove(icon);
        });
    }

    ScheduledExecutorService notificationExecutor = Executors.newSingleThreadScheduledExecutor();
    @SuppressWarnings("all")
    ScheduledFuture ttlCleanupTask = null;

    private void trackTTL(Label icon, long TTL) {
        iconTTL.put(icon, System.currentTimeMillis() + TTL);

        if (ttlCleanupTask == null || ttlCleanupTask.isCancelled()) {
            ttlCleanupTask = notificationExecutor.scheduleWithFixedDelay(this::processTTL, 1, 100, TimeUnit.MILLISECONDS);
        }
    }

    private void processTTL() {
        Long now = System.currentTimeMillis();
        iconTTL.keySet().stream()
                .filter((icon) -> (iconTTL.get(icon) <= now))
                .forEach(this::removeIndicator);
        if (iconTTL.isEmpty()) {
            ttlCleanupTask.cancel(true);
            ttlCleanupTask = null;
        }
    }

    public void addMouseListener(EventHandler<MouseEvent> handler) {
        appleScreen.addEventHandler(MouseEvent.ANY, handler);
        rootPane.addEventHandler(MouseEvent.ANY, handler);
    }

    public void removeMouseListener(EventHandler<MouseEvent> handler) {
        appleScreen.removeEventHandler(MouseEvent.ANY, handler);
        rootPane.removeEventHandler(MouseEvent.ANY, handler);
    }

    Label currentNotification = null;

    public void displayNotification(String message) {
        Label oldNotification = currentNotification;
        Label notification = new Label(message);
        currentNotification = notification;
        notification.setEffect(new DropShadow(2.0, Color.BLACK));
        notification.setTextFill(Color.WHITE);
        notification.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 80, 0.7), new CornerRadii(5.0), new Insets(-5.0))));
        Platform.runLater(() -> {
            stackPane.getChildren().remove(oldNotification);
            stackPane.getChildren().add(notification);
        });

        notificationExecutor.schedule(
                () -> Platform.runLater(() -> stackPane.getChildren().remove(notification)),
                4, TimeUnit.SECONDS);
    }
}
