package org.example.tunesfx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.SampleBank;

public class SintetizadorController {
    @FXML
    private Pane rootPane; // El panel de fondo
    @FXML
    private Spinner<Double> sampleLengthSpinner;

    public Button guardarSample;
    // --- Componentes de UI inyectados por el FXML ---
    @FXML private Oscilator oscilador1;
    @FXML private Oscilator oscilador2;
    @FXML private Oscilator oscilador3;
    @FXML private Oscilator oscilador4;
    @FXML private Oscilator oscilador5;
    @FXML private WaveViewer waveViewer;

    private Oscilator[] oscillators;

    private Sintetizador logic;

    @FXML
    public void initialize() {
        guardarSample.setText("Save sample");
        guardarSample.setOnAction(e -> handleSaveSample());

        // Agrupar los osciladores
        oscillators = new Oscilator[] {
                oscilador1, oscilador2, oscilador3, oscilador4, oscilador5
        };
        // 1. Crear la instancia de la lógica del sintetizador
        //    Pasamos los componentes de UI que la lógica necesita controlar.
        logic = new Sintetizador(oscillators, waveViewer);
        // 2. Configurar los callbacks
        //    El sintetizador (lógica) necesita decirle al WaveViewer (UI) cuándo dibujar
        logic.setUpdateCallback(waveViewer::draw);
        //    Cada Oscilador (UI) necesita decirle al Sintetizador (lógica)
        //    que la forma de onda cambió (para que actualice el WaveViewer).
        for (Oscilator osc : oscillators) {
            osc.setUpdateCallback(logic::updateWaveviewer);
        }

        // Configura el Spinner para que maneje segundos (Double)
        // Mín: 0.1s, Máx: 10.0s, Valor inicial: 1.0s, Incremento: 0.1s
        SpinnerValueFactory<Double> valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 10.0, 1.0, 0.1);

        // Para pillar el foco del spinner
        rootPane.setFocusTraversable(true);

        rootPane.setOnMouseClicked(e -> {
            rootPane.requestFocus();
            e.consume(); // Opcional: evita que el clic se propague
        });

        sampleLengthSpinner.setValueFactory(valueFactory);
        
        guardarSample.setOnAction(e -> handleSaveSample());
    }

    private void handleSaveSample() {
        // 1. Obtener la duración en segundos del Spinner
        double durationSeconds = sampleLengthSpinner.getValue();

        // 2. Calcular el número de samples necesarios
        //    (ej. 1.5s * 44100 Hz = 66150 samples)
        int sampleLength = (int) (durationSeconds * Sintetizador.AudioInfo.SAMPLE_RATE);

        short[] sampleData = logic.generateSample(sampleLength);
        Sample newSample = new Sample(sampleData);
        SampleBank.getInstance().setCurrentSample(newSample);

        System.out.println("Sample guardado (" + durationSeconds + "s) y notificación enviada.");
        guardarSample.setText("Sample Saved");
    }

    /**
     * Métodos públicos para que el Stage (creado en Principal)
     * pueda reenviar los eventos a este controlador.
     */
    public void handleKeyPressed(KeyEvent e) {
        String keyString = e.getText();
        if (keyString.isEmpty()) {
            if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) {
                keyString = e.getCode().toString().toLowerCase();
            }
        }
        if (keyString.length() == 1) {
            logic.onKeyPressed(keyString.charAt(0));
        }
        e.consume();
    }

    public void handleKeyReleased(KeyEvent e) {
        logic.onKeyReleased();
    }

    public void shutdown() {
        logic.shutdownAudio();
    }
}