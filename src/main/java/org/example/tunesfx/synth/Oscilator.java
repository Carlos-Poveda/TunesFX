package org.example.tunesfx.synth;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class Oscilator extends HBox {

    public enum WaveTable {
        Sine(new float[2048]), Square(new float[2048]), Saw(new float[2048]), Triangle(new float[2048]), Noise(new float[2048]);
        private final float[] samples;
        public static final int SIZE = 2048;

        WaveTable(float[] samples) {
            this.samples = samples;
            generate();
        }
        private void generate() {
            // Generación básica de tablas de ondas para visualización y uso legacy
            for (int i = 0; i < SIZE; i++) {
                double t = (double) i / SIZE;
                double val = 0;
                switch (this.name()) {
                    case "Sine": val = Math.sin(2 * Math.PI * t); break;
                    case "Square": val = Math.signum(Math.sin(2 * Math.PI * t)); break;
                    case "Saw": val = 1.0 - 2.0 * t; break;
                    case "Triangle": val = 2.0 * Math.abs(2.0 * t - 1.0) - 1.0; break;
                    case "Noise": val = (Math.random() * 2.0) - 1.0; break;
                }
                samples[i] = (float) val;
            }
        }
        public float[] getSamples() { return samples; }
    }

    private WaveTable waveTable = WaveTable.Sine;

    // --- SERUM ENGINE: UNISON ---
    private static final int MAX_VOICES = 7;
    private int activeVoices = 1;
    private double detuneAmount = 0.0;

    // Arrays para manejar múltiples voces simultáneas (Legacy Mono)
    private final double[] voiceIndices = new double[MAX_VOICES];
    private final double[] voiceStepSizes = new double[MAX_VOICES];

    private double keyFrequency = 440.0;
    private int toneOffset = 0; // Semitonos
    private int volume = 100;

    // Control UI
    private double lastMouseY = -1;
    private double lastMouseYVolume = -1;

    private Runnable updateCallback;

    private static final int TONE_OFFSET_LIMIT = 48; // +/- 4 Octavas

    @FXML private ComboBox<WaveTable> waveFormComboBox;
    @FXML private Label toneValueLabel;
    @FXML private Label volumeParameter;
    @FXML private Slider unisonSlider;
    @FXML private Slider detuneSlider;
    @FXML private Label unisonLabel;
    @FXML private Label detuneLabel;

    public Oscilator() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/tunesfx/oscilador.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        inicializarOscilador();
    }

    @FXML
    private void initialize() {
        // 1. Configurar Combo
        waveFormComboBox.setItems(FXCollections.observableArrayList(WaveTable.values()));
        waveFormComboBox.setValue(WaveTable.Sine);
        waveFormComboBox.setOnAction(e -> {
            waveTable = waveFormComboBox.getValue();
            triggerUpdate();
        });

        // 2. Configurar Unison
        unisonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            activeVoices = newVal.intValue();
            unisonLabel.setText(activeVoices + "x");
            recalculateVoices();
            triggerUpdate();
        });

        // 3. Configurar Detune
        detuneSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            detuneAmount = newVal.doubleValue();
            detuneLabel.setText((int)(detuneAmount * 100) + "%");
            recalculateVoices();
        });

        setupToneDragControl();
        setupVolumeDragControl();
    }

    public void setUpdateCallback(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }

    private void triggerUpdate() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    private void inicializarOscilador() {
        keyFrequency = 440.0;
        toneOffset = 0;
        activeVoices = 1;
        resetPhase();
    }

    // --- MÉTODOS PÚBLICOS NECESARIOS PARA SINTETIZADOR ---

    /**
     * Devuelve el offset de semitonos (ej: -12, 0, +7).
     * ESTE ES EL MÉTODO QUE TE DABA ERROR.
     */
    public int getToneOffsetInt() {
        return this.toneOffset;
    }

    /**
     * Calcula una muestra de audio basada en una fase externa.
     * Vital para que el acorde suene limpio (Polifonía).
     */
    public double computeSample(double externalPhase) {
        String waveType = (waveFormComboBox.getValue() != null) ? waveFormComboBox.getValue().toString() : "Sine";
        double value = 0;

        switch (waveType) {
            case "Sine": value = Math.sin(externalPhase); break;
            case "Square": value = Math.signum(Math.sin(externalPhase)); break;
            case "Saw":
                // Aproximación de diente de sierra sin aliasing severo
                value = 1.0 - (2.0 * (externalPhase / (2.0 * Math.PI) - Math.floor(externalPhase / (2.0 * Math.PI))));
                break;
            case "Triangle":
                value = 2.0 * Math.abs(2.0 * (externalPhase / (2.0 * Math.PI) - Math.floor(externalPhase / (2.0 * Math.PI) + 0.5))) - 1.0;
                break;
            case "Noise":
                value = (Math.random() * 2.0) - 1.0;
                break;
            default: value = Math.sin(externalPhase);
        }

        // Aplicar volumen del oscilador
        return value * (volume / 100.0);
    }

    // --- LÓGICA LEGACY (MONO) ---
    public double getNextSample() {
        // Mantenemos esto por si alguna parte antigua del código lo llama,
        // pero el Sintetizador nuevo usará computeSample.
        float[] table = waveTable.getSamples();
        double totalSample = 0;

        for (int i = 0; i < activeVoices; i++) {
            int indexA = (int) voiceIndices[i];
            int indexB = (indexA + 1) % WaveTable.SIZE;
            double fraction = voiceIndices[i] - indexA;
            double valA = table[indexA];
            double valB = table[indexB];
            double sample = valA + (valB - valA) * fraction;
            totalSample += sample;
            voiceIndices[i] += voiceStepSizes[i];
            if (voiceIndices[i] >= WaveTable.SIZE) voiceIndices[i] -= WaveTable.SIZE;
        }
        return (totalSample / Math.sqrt(activeVoices)) * (volume / 100.0);
    }

    public void setKeyFrequency(double frequency) {
        this.keyFrequency = frequency;
        recalculateVoices();
    }

    public void resetPhase() {
        for (int i = 0; i < MAX_VOICES; i++) {
            voiceIndices[i] = (activeVoices > 1) ? Math.random() * WaveTable.SIZE : 0;
        }
    }

    private void recalculateVoices() {
        double baseStep = (WaveTable.SIZE * (keyFrequency * Math.pow(2, toneOffset / 12.0)) / Sintetizador.AudioInfo.SAMPLE_RATE);
        if (activeVoices == 1) {
            voiceStepSizes[0] = baseStep;
            return;
        }
        for (int i = 0; i < activeVoices; i++) {
            double spread = (double)i / (activeVoices - 1) * 2.0 - 1.0;
            double detuneFactor = Math.pow(2, spread * detuneAmount * 0.1);
            voiceStepSizes[i] = baseStep * detuneFactor;
        }
    }

    public double[] getSampleWaveform(int numSamples) {
        double[] samples = new double[numSamples];
        double index = 0;
        double step = (WaveTable.SIZE * (1.0 / (numSamples / 44100.0) * 3.0) / 44100.0);
        for (int i = 0; i < numSamples; i++) {
            int idx = (int) index % WaveTable.SIZE;
            samples[i] = waveTable.getSamples()[idx] * (volume / 100.0);
            index += step;
        }
        return samples;
    }

    private void setupToneDragControl() {
        toneValueLabel.setOnMousePressed(e -> { lastMouseY = e.getScreenY(); setCursor(Cursor.NONE); });
        toneValueLabel.setOnMouseReleased(e -> { setCursor(Cursor.DEFAULT); lastMouseY = -1; });
        toneValueLabel.setOnMouseDragged(e -> {
            if (lastMouseY == -1) { lastMouseY = e.getScreenY(); return; }
            double deltaY = e.getScreenY() - lastMouseY;
            int change = (int)(-deltaY / 10);
            if (change != 0) {
                toneOffset += change;
                if (toneOffset > TONE_OFFSET_LIMIT) toneOffset = TONE_OFFSET_LIMIT;
                else if (toneOffset < -TONE_OFFSET_LIMIT) toneOffset = -TONE_OFFSET_LIMIT;
                toneValueLabel.setText(toneOffset + " semi");
                recalculateVoices();
                lastMouseY = e.getScreenY();
                triggerUpdate();
            }
        });
    }

    private void setupVolumeDragControl() {
        volumeParameter.setOnMousePressed(e -> { lastMouseYVolume = e.getScreenY(); setCursor(Cursor.NONE); });
        volumeParameter.setOnMouseReleased(e -> { setCursor(Cursor.DEFAULT); lastMouseYVolume = -1; });
        volumeParameter.setOnMouseDragged(e -> {
            if (lastMouseYVolume == -1) { lastMouseYVolume = e.getScreenY(); return; }
            double deltaY = e.getScreenY() - lastMouseYVolume;
            int change = (int)(-deltaY / 3);
            if (change != 0) {
                volume = Math.max(0, Math.min(100, volume + change));
                volumeParameter.setText(volume + "%");
                lastMouseYVolume = e.getScreenY();
                triggerUpdate();
            }
        });
    }
}