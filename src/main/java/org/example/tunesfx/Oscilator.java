package org.example.tunesfx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML; // Importar
import javafx.fxml.FXMLLoader; // Importar
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color; // No se usa si el FXML lo define
import javafx.geometry.Insets; // No se usa si el FXML lo define
import javafx.scene.Cursor;

import java.io.IOException; // Importar

public class Oscilator extends HBox { // HBox es el equivalente a un JPanel con FlowLayout horizontal

    // --- Atributos lógicos (sin cambios) ---
    private WaveTable waveTable = WaveTable.Sine;
    private int waveTableStepSize;
    private int waveTableIndex;
    private double keyFrequency;
    private int toneOffset;
    private int volume = 100;

    // --- Variables de estado de UI (adaptadas) ---
    private double lastMouseY = -1; // Usamos double para e.getScreenY()
    private double lastMouseYVolume = -1; // Usamos double para e.getScreenY()

    // Callback para notificar al Sintetizador que actualice el WaveViewer
    private Runnable updateCallback;

    // --- Constantes (sin cambios) ---
    private static final int TONE_OFFSET_LIMIT = 400;

    // --- Componentes de la interfaz (JavaFX) ---
    @FXML
    private ComboBox<WaveTable> waveFormComboBox;
    @FXML
    private Label toneValueLabel;
    @FXML
    private Label volumeParameter;
    // (Los otros JLabels eran solo texto estático, los creamos en el método)


    /**
     * Constructor adaptado para JavaFX.
     * Recibe un 'Runnable' (callback) en lugar de la instancia de 'Sintetizador'.
     */
    public Oscilator() {
        // Cargar el FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("oscilador.fxml"));
        fxmlLoader.setRoot(this); // El HBox raíz es esta misma clase
        fxmlLoader.setController(this); // El controlador es esta misma clase

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        inicializarOscilador(); // Lógica de audio
    }

    @FXML
    private void initialize() {
        // --- Configuración de componentes ---
        waveFormComboBox.setItems(FXCollections.observableArrayList(WaveTable.values()));
        waveFormComboBox.setValue(WaveTable.Sine);

        // --- Listeners ---
        waveFormComboBox.setOnAction(e -> {
            waveTable = waveFormComboBox.getValue();
            if (updateCallback != null) updateCallback.run();
        });

        toneValueLabel.setOnMousePressed(e -> {
            lastMouseY = e.getScreenY(); // Reemplaza e.getYOnScreen()
            // Reemplaza la creación del Cursor BLANK_CURSOR
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
            double deltaY = currentMouseY - lastMouseY; // Usamos double
            final int SENSITIVITY_FACTOR = 4;

            int change = (int)(-deltaY / SENSITIVITY_FACTOR); // El cálculo es el mismo

            if (change != 0) {
                toneOffset += change;

                if (toneOffset > TONE_OFFSET_LIMIT) {
                    toneOffset = TONE_OFFSET_LIMIT;
                } else if (toneOffset < -TONE_OFFSET_LIMIT) {
                    toneOffset = -TONE_OFFSET_LIMIT;
                }

                applyToneOffset();
                toneValueLabel.setText("x" + String.format("%.2f", getToneOffset()));
                lastMouseY = currentMouseY;
                if (change != 0 && updateCallback != null) {
                    // ...
                    updateCallback.run(); // Probar a dejarlo sin if
                }
            }
        });

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
                volume += change;

                if (volume > 100) {
                    volume = 100;
                } else if (volume < 0) {
                    volume = 0;
                }

                volumeParameter.setText(" " + volume + "%");
                lastMouseYVolume = currentMouseY;
                if (change != 0 && updateCallback != null) {
                    // ...
                    updateCallback.run(); // Probar sin if
                }
            }
        });
    }

    public void setUpdateCallback(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }


    private void inicializarOscilador() {
        // Quita toda la configuración de UI (setStyle, setSpacing, setPadding).
        // Déjalo solo con la lógica de audio.
        keyFrequency = 440.0;
        toneOffset = 0;
    }



    public double getNextSample() {
        double sample = waveTable.getSamples()[waveTableIndex] * getVolumenMultiplier();
        waveTableIndex = (waveTableIndex + waveTableStepSize) % WaveTable.SIZE;
        return sample;
    }

    public void setKeyFrequency(double frequency) {
        keyFrequency = frequency;
        applyToneOffset();
    }

    public double[] getSampleWaveform(int numSamples) {
        double[] samples = new double[numSamples];
        // Asegúrate de que Sintetizador.AudioInfo.SAMPLE_RATE siga siendo accesible
        double frequency = 1.0 / (numSamples / (double)Sintetizador.AudioInfo.SAMPLE_RATE) * 3.0;
        int index = 0;
        int stepSize = (int)(WaveTable.SIZE * (frequency * Math.pow(2, getToneOffset())) / Sintetizador.AudioInfo.SAMPLE_RATE);
        for (int i = 0; i < numSamples; i++) {
            samples[i] = waveTable.getSamples()[index] * getVolumenMultiplier();
            index = (index + stepSize) % WaveTable.SIZE;
        }
        return samples;
    }

    private void applyToneOffset() {
        // Asegúrate de que Sintetizador.AudioInfo.SAMPLE_RATE siga siendo accesible
        waveTableStepSize = (int)(WaveTable.SIZE * (keyFrequency * Math.pow(2, getToneOffset())) / Sintetizador.AudioInfo.SAMPLE_RATE);
    }

    private double getVolumenMultiplier() {
        return volume / 100.0;
    }

    private double getToneOffset() {
        return toneOffset / 100d;
    }
}