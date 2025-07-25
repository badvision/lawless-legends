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
import jace.config.Configuration;
import jace.lawless.LawlessComputer;
import jace.EmulatorUILogic;
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
    private Slider musicVolumeSlider;
    
    @FXML
    private Slider sfxVolumeSlider;

    private final BooleanProperty aspectRatioCorrectionEnabled = new SimpleBooleanProperty(false);
    
    // UI configuration is now part of EmulatorUILogic
    
    // Store listeners so we can remove them during loading
    private javafx.beans.value.ChangeListener<Number> musicVolumeListener;
    private javafx.beans.value.ChangeListener<Number> sfxVolumeListener;
    private javafx.beans.value.ChangeListener<String> musicSelectionListener;
    
    // Flag to prevent concurrent saves
    private volatile boolean isSaving = false;
    private volatile boolean loadingSettings = false;
    public static volatile boolean startupComplete = false;

    // Debounce machinery
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> pendingSave = null;
    private static final int SAVE_DEBOUNCE_MS = 2000;

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
        
        loadingSettings = true;
        
        // Initialize UI configuration
        initializeUIConfiguration();
        
        // UI settings will be loaded later when configuration system is ready
        
        // Set listeners for volume changes
        musicVolumeListener = (observable, oldValue, newValue) -> {
            Emulator.withComputer(computer -> {
                if (computer instanceof LawlessComputer) {
                    LawlessHacks lawlessHacks = (LawlessHacks) ((LawlessComputer) computer).activeCheatEngine;
                    if (lawlessHacks != null) {
                        lawlessHacks.setMusicVolume(newValue.doubleValue());
                    }
                }
            });
            // Save settings when volume changes
            saveUISettings();
        };
        musicVolumeSlider.valueProperty().addListener(musicVolumeListener);
        
        sfxVolumeListener = (observable, oldValue, newValue) -> {
            Emulator.withComputer(computer -> {
                // Update SFX volume
                Motherboard.enableSpeaker = newValue.doubleValue() > 0.0;
                computer.motherboard.reconfigure();
                
                // Set volume scale on speaker
                if (computer.motherboard.speaker != null) {
                    computer.motherboard.speaker.setVolumeScale(newValue.doubleValue());
                }
                
                // Only detach speaker if volume is zero
                if (newValue.doubleValue() == 0.0) {
                    computer.motherboard.speaker.detach();
                } else {
                    computer.motherboard.speaker.attach();
                }
            });
            // Save settings when volume changes
            saveUISettings();
        };
        sfxVolumeSlider.valueProperty().addListener(sfxVolumeListener);
    }
    
    private void initializeUIConfiguration() {
        // UI configuration is now part of EmulatorUILogic, no separate initialization needed
        // The configuration tree already includes EmulatorUILogic as part of the main Configuration
    }
    
    public void loadUISettings() {
        loadingSettings = true;
        // Apply loaded settings to UI components
        Platform.runLater(() -> {
            // Check if configuration system is ready
            if (Configuration.BASE == null || Configuration.BASE.subject == null) {
                System.out.println("Configuration not ready yet, skipping UI settings load");
                return;
            }
            
            System.out.println("Configuration is ready, loading UI settings");
            
            // Set volume sliders from saved values (avoid triggering listeners during load)
            if (musicVolumeListener != null) {
                musicVolumeSlider.valueProperty().removeListener(musicVolumeListener);
            }
            if (sfxVolumeListener != null) {
                sfxVolumeSlider.valueProperty().removeListener(sfxVolumeListener);
            }
            if (musicSelectionListener != null) {
                musicSelection.getSelectionModel().selectedItemProperty().removeListener(musicSelectionListener);
            }
            
            EmulatorUILogic ui = ((Configuration) Configuration.BASE.subject).ui;
            if (ui != null) {
                musicVolumeSlider.setValue(ui.musicVolume);
                sfxVolumeSlider.setValue(ui.sfxVolume);
                
                // Set soundtrack selection from saved value
                if (musicSelection.getItems().contains(ui.soundtrackSelection)) {
                    musicSelection.setValue(ui.soundtrackSelection);
                }
                
                // Set aspect ratio correction
                aspectRatioCorrectionEnabled.set(ui.aspectRatioCorrection);
            }
            
            // Re-add listeners if they exist
            if (musicVolumeListener != null) {
                musicVolumeSlider.valueProperty().addListener(musicVolumeListener);
            }
            if (sfxVolumeListener != null) {
                sfxVolumeSlider.valueProperty().addListener(sfxVolumeListener);
            }
            if (musicSelectionListener != null) {
                musicSelection.getSelectionModel().selectedItemProperty().addListener(musicSelectionListener);
            }
            
            // Update LawlessHacks with the loaded settings
            syncLawlessHacksWithUISettings();
            
            loadingSettings = false;
        });
    }
    
    public synchronized void saveUISettings() {
        if (Configuration.BASE != null && Configuration.BASE.subject != null && !isSaving && !loadingSettings && startupComplete) {
            isSaving = true;
            try {
                EmulatorUILogic ui = ((Configuration) Configuration.BASE.subject).ui;
                
                // Update configuration with current UI state
                if (musicVolumeSlider != null) {
                    ui.musicVolume = musicVolumeSlider.getValue();
                }
                if (sfxVolumeSlider != null) {
                    ui.sfxVolume = sfxVolumeSlider.getValue();
                }
                if (musicSelection != null && musicSelection.getValue() != null) {
                    ui.soundtrackSelection = musicSelection.getValue();
                }
                ui.aspectRatioCorrection = aspectRatioCorrectionEnabled.get();
                
                // Save window state if we have a primary stage
                if (primaryStage != null) {
                    ui.windowWidth = (int) primaryStage.getWidth();
                    ui.windowHeight = (int) primaryStage.getHeight();
                    ui.fullscreen = primaryStage.isFullScreen();
                }
                
                // Save the window size index from EmulatorUILogic
                ui.windowSizeIndex = EmulatorUILogic.size;
                
                // Debounced save
                scheduleDebouncedSave();
            } catch (Exception e) {
                // Log error but don't let it crash the application
                System.err.println("Error saving UI settings: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                e.printStackTrace();
            } finally {
                isSaving = false;
            }
        }
    }
    
    private void scheduleDebouncedSave() {
        // cancel previous pending save
        if (pendingSave != null && !pendingSave.isDone()) {
            pendingSave.cancel(false);
        }
        if (saveExecutor.isShutdown()) {
            // executor gone, save directly
            Configuration.saveSettingsImmediate();
            return;
        }
        pendingSave = saveExecutor.schedule(() -> {
            try {
                Configuration.saveSettingsImmediate();
            } catch (Exception e) {
                System.err.println("Error saving settings: " + e.getMessage());
            }
        }, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** Call this when application is shutting down to flush pending save and stop executor */
    public void shutdown() {
        try {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.get(); // wait for completion
            }
        } catch (Exception ignored) {}
        saveExecutor.shutdownNow();
    }
    
    public EmulatorUILogic getUIConfiguration() {
        return Configuration.BASE != null && Configuration.BASE.subject != null ? 
               ((Configuration) Configuration.BASE.subject).ui : null;
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
        musicSelectionListener = (observable, oldValue, newValue) -> {
            Emulator.withComputer(computer -> {
                if (((LawlessComputer) computer).activeCheatEngine != null) {
                    ((LawlessHacks) ((LawlessComputer) computer).activeCheatEngine).changeMusicScore(String.valueOf(newValue));
                }
            });
            // Save settings when soundtrack selection changes
            saveUISettings();
        };
        musicSelection.getSelectionModel().selectedItemProperty().addListener(musicSelectionListener);
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
        // Save settings when aspect ratio changes (only if config is initialized)
        if (getUIConfiguration() != null) {
            saveUISettings();
        }
    }

    public void connectComputer(Stage primaryStage) {
        Platform.runLater(() -> {
            connectControls(primaryStage);
            Emulator.withVideo(this::connectVideo);
            appleScreen.setVisible(true);
            rootPane.requestFocus();
            
            // Restore window size from saved settings
            restoreWindowSize(primaryStage);
            
            // Only sync with LawlessHacks if we don't have saved settings
            // (saved settings will be loaded later and will sync LawlessHacks with UI)
            // We check if the configuration has been loaded from a file by checking if startupComplete is false
            if (!startupComplete) {
                syncMusicSelectionWithLawlessHacks();
            }
        });
    }
    
    private void restoreWindowSize(Stage stage) {
        EmulatorUILogic ui = getUIConfiguration();
        if (ui != null && stage != null) {
            // Restore window size
            stage.setWidth(ui.windowWidth);
            stage.setHeight(ui.windowHeight);
            
            // Restore fullscreen state
            if (ui.fullscreen) {
                stage.setFullScreen(true);
            }
            
            // Restore window size index for integer scaling
            EmulatorUILogic.size = ui.windowSizeIndex;
            
            // Add listener to save window size changes
            stage.widthProperty().addListener((obs, oldVal, newVal) -> saveUISettings());
            stage.heightProperty().addListener((obs, oldVal, newVal) -> saveUISettings());
            stage.fullScreenProperty().addListener((obs, oldVal, newVal) -> saveUISettings());
        }
    }
    
    /**
     * Sync the music selection dropdown with the current score from LawlessHacks
     * This ensures the UI reflects the actual current state without creating callback loops
     */
    private void syncMusicSelectionWithLawlessHacks() {
        // Only sync if we're not currently loading settings to avoid interference
        if (loadingSettings) {
            return;
        }
        
        // Only sync if the musicSelection component is available
        if (musicSelection == null) {
            return;
        }
        
        Emulator.withComputer(computer -> {
            if (computer instanceof LawlessComputer) {
                LawlessHacks lawlessHacks = (LawlessHacks) ((LawlessComputer) computer).activeCheatEngine;
                if (lawlessHacks != null) {
                    String currentScore = lawlessHacks.getCurrentScore();
                    if (currentScore != null) {
                        // Check if the current selection is different from LawlessHacks
                        String currentSelection = musicSelection.getValue();
                        if (!currentScore.equals(currentSelection)) {
                            // Temporarily remove the listener to avoid triggering a callback loop
                            if (musicSelectionListener != null) {
                                musicSelection.getSelectionModel().selectedItemProperty().removeListener(musicSelectionListener);
                            }
                            
                            // Update the dropdown to match the current score
                            if (musicSelection.getItems().contains(currentScore)) {
                                musicSelection.setValue(currentScore);
                            }
                            
                            // Re-add the listener
                            if (musicSelectionListener != null) {
                                musicSelection.getSelectionModel().selectedItemProperty().addListener(musicSelectionListener);
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Update LawlessHacks with the current UI settings
     * This is called when loading saved settings to ensure LawlessHacks reflects the saved state
     */
    private void syncLawlessHacksWithUISettings() {
        Emulator.withComputer(computer -> {
            if (computer instanceof LawlessComputer) {
                LawlessHacks lawlessHacks = (LawlessHacks) ((LawlessComputer) computer).activeCheatEngine;
                if (lawlessHacks != null) {
                    // Update music volume
                    if (musicVolumeSlider != null) {
                        lawlessHacks.setMusicVolume(musicVolumeSlider.getValue());
                    }
                    
                    // Update soundtrack selection
                    if (musicSelection != null && musicSelection.getValue() != null) {
                        lawlessHacks.changeMusicScore(musicSelection.getValue());
                    }
                }
            }
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

    /** Begin a programmatic update block during which saves are suppressed */
    public void beginProgrammaticUpdate() {
        loadingSettings = true;
    }

    /** End a programmatic update block */
    public void endProgrammaticUpdate() {
        loadingSettings = false;
    }
}
