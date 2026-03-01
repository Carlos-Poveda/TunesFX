package org.example.tunesfx.synth;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.Cursor;

import java.io.IOException;
import java.util.Arrays;

public class Oscilator extends HBox {

    private WaveTable waveTable = WaveTable.Sine;

    // --- LÓGICA UNISON ---
    private static final int MAX_VOICES = 7;
    private int activeVoices = 1;
    private double detuneAmount = 0.0; // 0.0 a 0.5 (aprox medio semitono)

    // Usamos double para mayor precisión en la afinación (microtonalidad para unison)
    private double[] voiceIndices = new double[MAX_VOICES];
    private double[] voiceStepSizes = new double[MAX_VOICES];

    private double keyFrequency;
    private int toneOffset;
    private int volume = 100;

    // UI Interactiva
    private double lastMouseY = -1;
    private double lastMouseYVolume = -1;
    private Runnable updateCallback;
    private static final int TONE_OFFSET_LIMIT = 400;

    @FXML private ComboBox<WaveTable> waveFormComboBox;
    @FXML private Label toneValueLabel;
    @FXML private Label volumeParameter;

    // Nuevos controles FXML
    @FXML private Slider unisonVoicesSlider;
    @FXML private Slider unisonDetuneSlider;

    public Oscilator() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/tunesfx/oscilador.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try { fxmlLoader.load(); } catch (IOException exception) { throw new RuntimeException(exception); }

        inicializarOscilador();
    }

    @FXML
    private void initialize() {
        waveFormComboBox.setItems(FXCollections.observableArrayList(WaveTable.values()));
        waveFormComboBox.setValue(WaveTable.Sine);

        waveFormComboBox.setOnAction(e -> {
            waveTable = waveFormComboBox.getValue();
            if (updateCallback != null) updateCallback.run();
        });

        // --- Configuración Unison UI ---
        unisonVoicesSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            this.activeVoices = newVal.intValue();
            recalculateVoices(); // Recalcular frecuencias al cambiar voces
        });

        unisonDetuneSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            this.detuneAmount = newVal.doubleValue();
            recalculateVoices(); // Recalcular frecuencias al cambiar detune
        });

        // --- Configuración Tone y Volumen (Tu código original intacto) ---
        setupMouseControl(toneValueLabel, (change) -> {
            toneOffset += change;
            if (toneOffset > TONE_OFFSET_LIMIT) toneOffset = TONE_OFFSET_LIMIT;
            else if (toneOffset < -TONE_OFFSET_LIMIT) toneOffset = -TONE_OFFSET_LIMIT;
            recalculateVoices(); // Importante recalcular al cambiar tono
            toneValueLabel.setText("x" + String.format("%.2f", getToneOffset()));
        }, 4);

        setupMouseControl(volumeParameter, (change) -> {
            volume += change;
            if (volume > 100) volume = 100; else if (volume < 0) volume = 0;
            volumeParameter.setText(" " + volume + "%");
        }, 3);
    }

    // Helper para no repetir código de mouse
    private void setupMouseControl(Label label, java.util.function.Consumer<Integer> action, int sensitivity) {
        label.setOnMousePressed(e -> { lastMouseY = e.getScreenY(); setCursor(Cursor.NONE); });
        label.setOnMouseReleased(e -> { setCursor(Cursor.DEFAULT); lastMouseY = -1; });
        label.setOnMouseDragged(e -> {
            if (lastMouseY == -1) { lastMouseY = e.getScreenY(); return; }
            double deltaY = e.getScreenY() - lastMouseY;
            int change = (int)(-deltaY / sensitivity);
            if (change != 0) {
                action.accept(change);
                lastMouseY = e.getScreenY();
                if (updateCallback != null) updateCallback.run();
            }
        });
    }

    private void inicializarOscilador() {
        keyFrequency = 440.0;
        toneOffset = 0;
        Arrays.fill(voiceIndices, 0.0);
        recalculateVoices();
    }

    public void resetPhase() {
        // Reiniciamos todas las voces a fase 0
        Arrays.fill(voiceIndices, 0.0);
    }

    /**
     * EL CORAZÓN DEL UNISON: Suma todas las voces activas
     */
    public double getNextSample() {
        double totalSample = 0;
        float[] table = waveTable.getSamples();
        int tableSize = WaveTable.SIZE;

        for (int i = 0; i < activeVoices; i++) {
            // Obtenemos el índice actual (casteamos a int para leer el array)
            int index = (int) voiceIndices[i];

            // Sumamos la muestra
            totalSample += table[index];

            // Avanzamos el índice de esta voz específica
            voiceIndices[i] += voiceStepSizes[i];

            // Loop de la tabla (Wrap around)
            if (voiceIndices[i] >= tableSize) {
                voiceIndices[i] -= tableSize;
            }
        }

        // Normalización: Dividimos por la raíz cuadrada de las voces para mantener la energía
        // constante pero ganando potencia (o simplemente por activeVoices para ser conservadores)
        // Usaremos activeVoices para evitar saturación (clipping).
        double normalizedSample = totalSample / activeVoices;

        return normalizedSample * getVolumenMultiplier();
    }

    public void setKeyFrequency(double frequency) {
        this.keyFrequency = frequency;
        recalculateVoices();
    }

    /**
     * Calcula los pasos (velocidad) de cada voz del Unison
     */
    private void recalculateVoices() {
        double baseRate = (double) WaveTable.SIZE / Sintetizador.AudioInfo.SAMPLE_RATE;
        double baseFreqWithTone = keyFrequency * Math.pow(2, getToneOffset());

        // Si solo hay 1 voz, es la frecuencia pura
        if (activeVoices == 1) {
            voiceStepSizes[0] = baseFreqWithTone * baseRate;
            return;
        }

        // Si hay Unison, distribuimos las voces alrededor de la frecuencia central
        for (int i = 0; i < activeVoices; i++) {
            // Calculamos un factor de "spread" entre -1.0 y 1.0
            // Ej con 3 voces: -1.0, 0.0, 1.0
            double spread = -1.0 + (2.0 * i / (activeVoices - 1));

            // Aplicamos el detune.
            // spread * detuneAmount es cuántos semitonos desafinamos
            double detuneMultiplier = Math.pow(2, (spread * detuneAmount) / 12.0); // Dividido por 12 para que sea sutil

            double detunedFreq = baseFreqWithTone * detuneMultiplier;
            voiceStepSizes[i] = detunedFreq * baseRate;
        }
    }

    // --- Getters y Setters ---

    public void setUpdateCallback(Runnable updateCallback) { this.updateCallback = updateCallback; }
    private double getVolumenMultiplier() { return volume / 100.0; }
    private double getToneOffset() { return toneOffset / 100d; }

    // Para la visualización gráfica (simplificada, solo muestra la voz central o suma)
    public double[] getSampleWaveform(int numSamples) {
        // Para dibujar, usamos una lógica simplificada para no sobrecargar la UI
        double[] samples = new double[numSamples];
        double frequency = 1.0 / (numSamples / (double) Sintetizador.AudioInfo.SAMPLE_RATE) * 3.0;
        double idx = 0;
        double step = (WaveTable.SIZE * (frequency * Math.pow(2, getToneOffset())) / Sintetizador.AudioInfo.SAMPLE_RATE);

        for (int i = 0; i < numSamples; i++) {
            samples[i] = waveTable.getSamples()[(int)idx % WaveTable.SIZE] * getVolumenMultiplier();
            idx += step;
        }
        return samples;
    }
}