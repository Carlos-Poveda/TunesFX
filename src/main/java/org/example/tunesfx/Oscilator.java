package org.example.tunesfx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox; // Reemplaza a JPanel
import javafx.scene.paint.Color; // Reemplaza a java.awt.Color

// java.awt.image.BufferedImage y java.awt.Toolkit se reemplazan por Cursor.NONE
// java.awt.event.* se reemplazan por javafx.scene.input.MouseEvent

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
    private final Runnable updateCallback;

    // --- Constantes (sin cambios) ---
    private static final int TONE_OFFSET_LIMIT = 400;

    // --- Componentes de la interfaz (JavaFX) ---
    private ComboBox<WaveTable> waveFormComboBox;
    private Label toneValueLabel;
    private Label volumeParameter;
    // (Los otros JLabels eran solo texto estático, los creamos en el método)


    /**
     * Constructor adaptado para JavaFX.
     * Recibe un 'Runnable' (callback) en lugar de la instancia de 'Sintetizador'.
     */
    public Oscilator(Runnable updateCallback) {
        this.updateCallback = updateCallback;

        inicializarOscilador(); // Lógica de audio (sin cambios)
        crearInterfaz(); // Lógica de UI (adaptada a JavaFX)
    }

    private void inicializarOscilador() {
        // Inicializar valores por defecto (sin cambios)
        keyFrequency = 440.0;
        toneOffset = 0;

        // --- Configuración del Layout (JavaFX) ---
        // Reemplaza setBorder, setLayout, setBackground
        this.setStyle(
                "-fx-background-color: black; " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1;"
        );
        // Equivalente a FlowLayout.LEFT con algo de espacio
        this.setSpacing(5);
        this.setPadding(new Insets(10)); // Espacio interno
    }

    private void crearInterfaz() {

        // --- Label waveform ---
        Label labelWf = new Label("Wave form:");
        labelWf.setTextFill(Color.WHITE); // Reemplaza setForeground
        HBox.setMargin(labelWf, new Insets(0, 0, 0, 15)); // Reemplaza setBorder(EmptyBorder)

        // --- ComboBox para formas de onda ---
        waveFormComboBox = new ComboBox<>(FXCollections.observableArrayList(WaveTable.values()));
        waveFormComboBox.setValue(WaveTable.Sine); // Reemplaza setSelectedItem
        waveFormComboBox.setFocusTraversable(false); // Reemplaza setFocusable

        // Reemplaza addItemListener
        waveFormComboBox.setOnAction(e -> {
            waveTable = waveFormComboBox.getValue();
            updateCallback.run(); // Llama al método updateWaveviewer() del Sintetizador
        });

        // --- Label de texto para el tono ---
        Label toneText = new Label("Tone:");
        toneText.setTextFill(Color.WHITE);
        HBox.setMargin(toneText, new Insets(0, 0, 0, 15));

        // --- Label para mostrar el valor actual ---
        toneValueLabel = new Label("x0.00");
        toneValueLabel.setTextFill(Color.WHITE);

        // --- Control offset (Reemplaza MouseAdapter) ---

        // Reemplaza mousePressed
        toneValueLabel.setOnMousePressed(e -> {
            lastMouseY = e.getScreenY(); // Reemplaza e.getYOnScreen()
            // Reemplaza la creación del Cursor BLANK_CURSOR
            setCursor(Cursor.NONE);
        });

        // Reemplaza mouseReleased
        toneValueLabel.setOnMouseReleased(e -> {
            setCursor(Cursor.DEFAULT);
            lastMouseY = -1;
        });

        // Reemplaza mouseDragged
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
                updateCallback.run(); // Notifica al Sintetizador
            }
        });


        // --- Label volumen ---
        Label labelVolumen = new Label("Volume: ");
        labelVolumen.setTextFill(Color.WHITE);
        HBox.setMargin(labelVolumen, new Insets(0, 0, 0, 50));

        // --- Label nivel volumen ---
        volumeParameter = new Label(" 100%");
        volumeParameter.setTextFill(Color.WHITE);

        // --- Listener volumen (Reemplaza MouseAdapter) ---

        // (Se combinan mousePressed y mouseReleased en un solo listener si se quiere)
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
                updateCallback.run(); // Notifica al Sintetizador
            }
        });


        // --- Agregar componentes ---
        // Reemplaza add(...)
        this.getChildren().addAll(
                labelWf,
                waveFormComboBox,
                toneText,
                toneValueLabel,
                labelVolumen,
                volumeParameter
        );
    }

    // =================================================================
    // MÉTODOS PÚBLICOS Y LÓGICA DE AUDIO (SIN NINGÚN CAMBIO)
    // =================================================================

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