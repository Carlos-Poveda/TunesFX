package org.example.tunesfx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.Cursor;
import java.io.IOException;

public class Oscilator extends HBox {

    // === NUEVO: La instancia lógica ===
    private final OscillatorDSP dsp = new OscillatorDSP();

    // Variables puramente visuales (interacción del ratón)
    private double lastMouseY = -1;
    private double lastMouseYVolume = -1;
    private Runnable updateCallback;

    @FXML private ComboBox<WaveTable> waveFormComboBox;
    @FXML private Label toneValueLabel;
    @FXML private Label volumeParameter;

    public Oscilator() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("oscilador.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // Actualizamos las etiquetas visuales con los valores por defecto del DSP
        updateUI();
    }

    // Método para permitir acceso al DSP desde fuera (para Sintetizador.java más tarde)
    public OscillatorDSP getDsp() {
        return dsp;
    }

    // === MÉTODOS PUENTE (Delegación) ===
    // Estos métodos conectan el mundo antiguo con el nuevo DSP
    // Así Sintetizador.java sigue funcionando por ahora.

    public void setKeyFrequency(double frequency) {
        dsp.setKeyFrequency(frequency);
    }

    public double getNextSample() {
        return dsp.getNextSample();
    }

    public void resetPhase() {
        dsp.resetPhase();
    }

    public double[] getSampleWaveform(int numSamples) {
        return dsp.getSampleWaveform(numSamples);
    }


    @FXML
    private void initialize() {
        waveFormComboBox.setItems(FXCollections.observableArrayList(WaveTable.values()));
        waveFormComboBox.setValue(WaveTable.Sine);

        // Evento cambio de Onda
        waveFormComboBox.setOnAction(e -> {
            dsp.setWaveTable(waveFormComboBox.getValue()); // Actualizamos DSP
            if (updateCallback != null) updateCallback.run();
        });

        setupMouseControls();
    }

    private void updateUI() {
        toneValueLabel.setText("x" + String.format("%.2f", dsp.getToneOffsetInt() / 100d));
        volumeParameter.setText(" " + dsp.getVolume() + "%");
    }

    private void setupMouseControls() {
        // --- CONTROL DE TONO (MOUSE) ---
        toneValueLabel.setOnMousePressed(e -> {
            lastMouseY = e.getScreenY();
            setCursor(Cursor.NONE);
        });

        toneValueLabel.setOnMouseReleased(e -> {
            setCursor(Cursor.DEFAULT);
            lastMouseY = -1;
        });

        toneValueLabel.setOnMouseDragged(e -> {
            if (lastMouseY == -1) {
                lastMouseY = e.getScreenY();
                return;
            }
            double currentMouseY = e.getScreenY();
            double deltaY = currentMouseY - lastMouseY;
            final int SENSITIVITY_FACTOR = 4;
            int change = (int)(-deltaY / SENSITIVITY_FACTOR);

            if (change != 0) {
                // Actualizamos el DSP
                int newTone = dsp.getToneOffsetInt() + change;
                dsp.setToneOffset(newTone);

                // Actualizamos la Vista
                toneValueLabel.setText("x" + String.format("%.2f", dsp.getToneOffsetInt() / 100d));
                lastMouseY = currentMouseY;
                if (updateCallback != null) updateCallback.run();
            }
        });

        // --- CONTROL DE VOLUMEN (MOUSE) ---
        volumeParameter.setOnMousePressed(e -> {
            lastMouseYVolume = e.getScreenY();
            setCursor(Cursor.NONE);
        });

        volumeParameter.setOnMouseReleased(e -> {
            setCursor(Cursor.DEFAULT);
            lastMouseYVolume = -1;
        });

        volumeParameter.setOnMouseDragged(e -> {
            if (lastMouseYVolume == -1) {
                lastMouseYVolume = e.getScreenY();
                return;
            }
            double currentMouseY = e.getScreenY();
            double deltaY = currentMouseY - lastMouseYVolume;
            final int SENSITIVITY_FACTOR = 3;
            int change = (int)(-deltaY / SENSITIVITY_FACTOR);

            if (change != 0) {
                // Actualizamos el DSP
                int newVol = dsp.getVolume() + change;
                dsp.setVolume(newVol);

                // Actualizamos la Vista
                volumeParameter.setText(" " + dsp.getVolume() + "%");
                lastMouseYVolume = currentMouseY;
                if (updateCallback != null) updateCallback.run();
            }
        });
    }

    public void setUpdateCallback(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }
}