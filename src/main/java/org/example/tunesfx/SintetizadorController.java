package org.example.tunesfx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;

public class SintetizadorController {

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
        
        guardarSample.setOnAction(e -> handleSaveSample());
    }

    /**
     * NUEVO MÉTODO: Se llama al pulsar el botón "Save sample".
     */
    private void handleSaveSample() {
        // Definimos la longitud del sample (ej. 1 segundo)
//        int sampleLength = Sintetizador.AudioInfo.SAMPLE_RATE;
        int sampleLength = 5000;

        // 1. Pedir a la lógica del sintetizador que genere el sample
        short[] sampleData = logic.generateSample(sampleLength);

        // 2. Crear nuestro objeto Sample
        Sample newSample = new Sample(sampleData);

        // 3. Guardarlo en el banco compartido
        SampleBank.getInstance().setCurrentSample(newSample);

        // Feedback visual/consola (opcional)
        guardarSample.setText("Sample saved");
        System.out.println("Sample guardado en el banco. Longitud: " + sampleData.length);

        // Opcional: resetear el texto después de un tiempo
        // new Thread(() -> { ... Thread.sleep(2000); Platform.runLater(() -> guardarSample.setText("Save sample")); ... }).start();
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