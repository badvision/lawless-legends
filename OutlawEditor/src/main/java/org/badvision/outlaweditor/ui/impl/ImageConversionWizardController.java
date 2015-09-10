/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.ui.impl;

import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.badvision.outlaweditor.apple.ImageDitherEngine;
import org.badvision.outlaweditor.ui.ImageConversionPostAction;

/**
 * FXML Controller class
 *
 * @author blurry
 */
public class ImageConversionWizardController implements Initializable {

    @FXML
    private TextField brightnessValue;
    @FXML
    private Slider brightnessSlider;
    @FXML
    private TextField contrastValue;
    @FXML
    private Slider contrastSlider;
    @FXML
    private TextField hueValue;
    @FXML
    private Slider hueSlider;
    @FXML
    private TextField saturationValue;
    @FXML
    private Slider saturationSlider;
    @FXML
    private TextField outputWidthValue;
    @FXML
    private TextField outputHeightValue;
    @FXML
    private TextField cropTopValue;
    @FXML
    private TextField cropLeftValue;
    @FXML
    private TextField cropBottomValue;
    @FXML
    private TextField cropRightValue;
    @FXML
    private TextField coefficientValue30;
    @FXML
    private TextField coefficientValue40;
    @FXML
    private TextField coefficientValue01;
    @FXML
    private TextField coefficientValue11;
    @FXML
    private TextField coefficientValue21;
    @FXML
    private TextField coefficientValue31;
    @FXML
    private TextField coefficientValue41;
    @FXML
    private TextField coefficientValue02;
    @FXML
    private TextField coefficientValue12;
    @FXML
    private TextField coefficientValue22;
    @FXML
    private TextField coefficientValue32;
    @FXML
    private TextField coefficientValue42;
    private final int[][] diffusionCoeffficients = new int[5][3];
    @FXML
    private TextField divisorValue;
    @FXML
    private ImageView sourceImageView;
    @FXML
    private ImageView convertedImageView;
  
    private ColorAdjust imageAdjustments = new ColorAdjust();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        for (TextField field : new TextField[]{
            brightnessValue, contrastValue, hueValue, saturationValue,
            cropBottomValue, cropLeftValue, cropRightValue, cropTopValue,
            coefficientValue01, coefficientValue02, coefficientValue11, coefficientValue12,
            coefficientValue21, coefficientValue22, coefficientValue30, coefficientValue31,
            coefficientValue32, coefficientValue40, coefficientValue41, coefficientValue41,
            coefficientValue42, divisorValue, outputHeightValue, outputWidthValue
        }) {
            configureNumberValidation(field, "0");
        }

        brightnessValue.textProperty().bindBidirectional(brightnessSlider.valueProperty(), NumberFormat.getNumberInstance());
        contrastValue.textProperty().bindBidirectional(contrastSlider.valueProperty(), NumberFormat.getNumberInstance());
        hueValue.textProperty().bindBidirectional(hueSlider.valueProperty(), NumberFormat.getNumberInstance());
        saturationValue.textProperty().bindBidirectional(saturationSlider.valueProperty(), NumberFormat.getNumberInstance());

        brightnessValue.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
                -> javafx.application.Platform.runLater(this::updateImageAdjustments));
        contrastValue.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
                -> javafx.application.Platform.runLater(this::updateImageAdjustments));
        hueValue.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
                -> javafx.application.Platform.runLater(this::updateImageAdjustments));
        saturationValue.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue)
                -> javafx.application.Platform.runLater(this::updateImageAdjustments));

        configureAtkinsonPreset(null);
    }

    private Stage stage;
    private ImageConversionPostAction postAction;
    private Image sourceImage;
    private WritableImage preprocessedImage;
    private WritableImage outputPreviewImage;
    private ImageDitherEngine ditherEngine;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPostAction(ImageConversionPostAction postAction) {
        this.postAction = postAction;
    }

    public void setDitherEngine(ImageDitherEngine engine) {
        this.ditherEngine = engine;
    }

    public void setSourceImage(Image image) {
        sourceImage = image;
        preprocessImage();
    }

    private void updateImageAdjustments() {
        double hue = Double.parseDouble(hueValue.getText());
        double saturation = Double.parseDouble(saturationValue.getText());
        double brightness = Double.parseDouble(brightnessValue.getText());
        double contrast = Double.parseDouble(contrastValue.getText());

        imageAdjustments = new ColorAdjust();
        imageAdjustments.setContrast(contrast);
        imageAdjustments.setBrightness(brightness);
        imageAdjustments.setHue(hue);
        imageAdjustments.setSaturation(saturation);
        sourceImageView.setEffect(imageAdjustments);
    }

    private void preprocessImage() {
        PixelReader pixelReader = sourceImage.getPixelReader();
        preprocessedImage = new WritableImage(pixelReader, (int) sourceImage.getWidth(), (int) sourceImage.getHeight());
        updateSourceView(preprocessedImage);
    }

    public void setOutputDimensions(int targetWidth, int targetHeight) {
        ditherEngine.setOutputDimensions(targetWidth, targetHeight);
        outputWidthValue.setText(String.valueOf(targetWidth));
        outputHeightValue.setText(String.valueOf(targetHeight));
        outputPreviewImage = ditherEngine.getPreviewImage();
    }

    public int getOutputWidth() {
        return Integer.parseInt(outputWidthValue.getText());
    }

    public int getOutputHeight() {
        return Integer.parseInt(outputHeightValue.getText());
    }

    private void updateSourceView(Image image) {
        sourceImageView.setImage(image);
        sourceImageView.setFitWidth(0);
        sourceImageView.setFitHeight(0);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        defaultTextFieldValues.put(cropRightValue, String.valueOf(width));
        defaultTextFieldValues.put(cropBottomValue, String.valueOf(height));
        cropRightValue.setText(String.valueOf(width));
        cropBottomValue.setText(String.valueOf(height));
    }

    @FXML
    private void performQuantizePass(ActionEvent event) {
        prepareForConversion();
        byte[] out = ditherEngine.dither(false);
        updateConvertedImageWithData(out);
    }
    
    @FXML
    private void performDiffusionPass(ActionEvent event) {
        prepareForConversion();
        byte[] out = ditherEngine.dither(true);
        updateConvertedImageWithData(out);
    }

    private void prepareForConversion() {
        ditherEngine.setCoefficients(getCoefficients());
        ditherEngine.setDivisor(getDivisor());
        ditherEngine.setSourceImage(sourceImageView.snapshot(null, null));
    }    
    
    byte[] lastOutput;
    private void updateConvertedImageWithData(byte[] data) {
        lastOutput = data;
        convertedImageView.setImage(ditherEngine.getPreviewImage());
//        convertedImageView.setImage(ditherEngine.getScratchBuffer());
    }

    @FXML
    private void performOK(ActionEvent event) {
        postAction.storeConvertedImage(lastOutput);
        stage.close();
    }

    @FXML
    private void performCancel(ActionEvent event) {
        stage.close();
    }

    private final Map<TextField, String> defaultTextFieldValues = new HashMap<>();
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private void configureNumberValidation(TextField field, String defaultValue) {
        defaultTextFieldValues.put(field, defaultValue);
        field.textProperty().addListener((ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            if (newValue == null || "".equals(newValue)) {
                scheduler.schedule(() -> {
                    if (null == field.textProperty().getValue() || field.textProperty().getValue().isEmpty()) {
                        field.textProperty().setValue(defaultTextFieldValues.get(field));
                    }
                }, 250, TimeUnit.MILLISECONDS);
            } else {
                try {
                    Double.parseDouble(newValue.toString());
                } catch (Exception ex) {
                    field.textProperty().setValue(oldValue.toString());
                }
            }
        });
    }

    private void setCoefficients(int... coeff) {
        coefficientValue30.setText(String.valueOf(coeff[3]));
        coefficientValue40.setText(String.valueOf(coeff[4]));
        coefficientValue01.setText(String.valueOf(coeff[5]));
        coefficientValue11.setText(String.valueOf(coeff[6]));
        coefficientValue21.setText(String.valueOf(coeff[7]));
        coefficientValue31.setText(String.valueOf(coeff[8]));
        coefficientValue41.setText(String.valueOf(coeff[9]));
        coefficientValue02.setText(String.valueOf(coeff[10]));
        coefficientValue12.setText(String.valueOf(coeff[11]));
        coefficientValue22.setText(String.valueOf(coeff[12]));
        coefficientValue32.setText(String.valueOf(coeff[13]));
        coefficientValue42.setText(String.valueOf(coeff[14]));
    }

    private int[][] getCoefficients() {
        diffusionCoeffficients[0][0] = 0;
        diffusionCoeffficients[1][0] = 0;
        diffusionCoeffficients[2][0] = 0;
        diffusionCoeffficients[3][0] = Integer.parseInt(coefficientValue30.getText());
        diffusionCoeffficients[4][0] = Integer.parseInt(coefficientValue40.getText());
        diffusionCoeffficients[0][1] = Integer.parseInt(coefficientValue01.getText());
        diffusionCoeffficients[1][1] = Integer.parseInt(coefficientValue11.getText());
        diffusionCoeffficients[2][1] = Integer.parseInt(coefficientValue21.getText());
        diffusionCoeffficients[3][1] = Integer.parseInt(coefficientValue31.getText());
        diffusionCoeffficients[4][1] = Integer.parseInt(coefficientValue41.getText());
        diffusionCoeffficients[0][2] = Integer.parseInt(coefficientValue02.getText());
        diffusionCoeffficients[1][2] = Integer.parseInt(coefficientValue12.getText());
        diffusionCoeffficients[2][2] = Integer.parseInt(coefficientValue22.getText());
        diffusionCoeffficients[3][2] = Integer.parseInt(coefficientValue32.getText());
        diffusionCoeffficients[4][2] = Integer.parseInt(coefficientValue42.getText());
        return diffusionCoeffficients;
    }

    private void setDivisor(int div) {
        divisorValue.setText(String.valueOf(div));
    }

    private int getDivisor() {
        return Integer.valueOf(divisorValue.getText());
    }

    // http://www.tannerhelland.com/4660/dithering-eleven-algorithms-source-code/
    @FXML
    private void configureFloydSteinbergPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 7, 0,
                0, 3, 5, 1, 0,
                0, 0, 0, 0, 0
        );
//        setDivisor(16);
        setDivisor(18);
    }

    @FXML
    private void configureFastFloydSteinbergPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 3, 0,
                0, 0, 3, 2, 0,
                0, 0, 0, 0, 0
        );
//        setDivisor(8);
        setDivisor(10);
    }

    @FXML
    private void configureJarvisJudiceNinkePreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 7, 5,
                3, 5, 7, 5, 3,
                1, 3, 5, 3, 1
        );
//        setDivisor(48);
        setDivisor(57);
    }

    @FXML
    private void configureStuckiPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 8, 4,
                2, 4, 8, 4, 2,
                1, 2, 4, 2, 1
        );
//        setDivisor(42);
        setDivisor(52);
    }

    @FXML
    private void configureAtkinsonPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 1, 1,
                0, 1, 1, 1, 0,
                0, 0, 1, 0, 0
        );
        setDivisor(8);
    }

    @FXML
    private void configureBurkesPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 8, 4,
                2, 4, 8, 4, 2,
                0, 0, 0, 0, 0
        );
        setDivisor(37);
//        setDivisor(32);
    }

    @FXML
    private void configureSierraPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 5, 3,
                2, 4, 5, 4, 2,
                0, 2, 3, 2, 0
        );
//        setDivisor(32);
        setDivisor(41);
    }

    @FXML
    private void configureTwoRowSierraPreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 4, 3,
                1, 2, 3, 2, 1,
                0, 0, 0, 0, 0
        );
//        setDivisor(16);
        setDivisor(18);
    }

    @FXML
    private void configureSierraLitePreset(ActionEvent event) {
        setCoefficients(
                0, 0, 0, 2, 0,
                0, 1, 1, 0, 0,
                0, 0, 0, 0, 0
        );
//        setDivisor(4);
        setDivisor(5);
    }

}
