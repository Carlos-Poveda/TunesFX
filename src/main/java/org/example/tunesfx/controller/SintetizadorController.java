package org.example.tunesfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.collections.FXCollections;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.example.tunesfx.synth.Filter;
import org.example.tunesfx.synth.Oscilator;
import org.example.tunesfx.synth.Sintetizador;
import org.example.tunesfx.synth.WaveViewer;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.SampleBank;
import javafx.scene.control.*;
import org.example.tunesfx.synth.LFO;

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

    // --- NUEVOS CAMPOS FXML PARA FILTRO ---
    @FXML private ComboBox<Filter.Tipo> filterTypeCombo;
    @FXML private Slider filterCutoffSlider;
    @FXML private Slider filterResonanceSlider;
    @FXML private ToggleButton filterToggleButton;
    @FXML private Label filterCutoffLabel;
    @FXML private Label filterResonanceLabel;

    // --- NUEVOS COMPONENTES PARA LFO ---
    @FXML private ComboBox<LFO.Waveform> lfoWaveformCombo;
    @FXML private ComboBox<LFO.Target> lfoTargetCombo;
    @FXML private Slider lfoRateSlider;
    @FXML private Slider lfoAmountSlider;
    @FXML private ToggleButton lfoToggleButton;
    @FXML private Label lfoRateLabel;
    @FXML private Label lfoAmountLabel;

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

        configurarFiltro();

        configurarLFO();
    }

    private void configurarLFO() {
        // Configurar ComboBox de waveform y target
        lfoWaveformCombo.setItems(FXCollections.observableArrayList(LFO.Waveform.values()));
        lfoWaveformCombo.setValue(LFO.Waveform.SINE);

        lfoTargetCombo.setItems(FXCollections.observableArrayList(LFO.Target.values()));
        lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF);

        // Configurar sliders
        lfoRateSlider.setMin(0.1);
        lfoRateSlider.setMax(20.0);
        lfoRateSlider.setValue(1.0);
        lfoRateSlider.setShowTickLabels(true);
        lfoRateSlider.setShowTickMarks(true);

        lfoAmountSlider.setMin(0.0);
        lfoAmountSlider.setMax(1.0);
        lfoAmountSlider.setValue(0.5);
        lfoAmountSlider.setShowTickLabels(true);
        lfoAmountSlider.setShowTickMarks(true);

        // Actualizar labels
        actualizarLabelsLFO();

        // Listeners
        lfoWaveformCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFWaveform(newVal);
        });

        lfoTargetCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFOTarget(newVal);
            lfoToggleButton.setSelected(newVal != LFO.Target.NONE);
            logic.setLFOActivado(newVal != LFO.Target.NONE);
        });

        lfoRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFORate(newVal.doubleValue());
            actualizarLabelsLFO();
        });

        lfoAmountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFOAmount(newVal.doubleValue());
            actualizarLabelsLFO();
        });

        lfoToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && lfoTargetCombo.getValue() == LFO.Target.NONE) {
                lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF);
            } else if (!newVal) {
                lfoTargetCombo.setValue(LFO.Target.NONE);
            }
            logic.setLFOActivado(newVal);
        });
    }

    private void actualizarLabelsLFO() {
        lfoRateLabel.setText(String.format("%.1f Hz", lfoRateSlider.getValue()));
        lfoAmountLabel.setText(String.format("%.2f", lfoAmountSlider.getValue()));
    }

    private void configurarFiltro() {
        // Configurar ComboBox de tipos de filtro
        filterTypeCombo.setItems(FXCollections.observableArrayList(Filter.Tipo.values()));
        filterTypeCombo.setValue(Filter.Tipo.OFF);

        // Configurar sliders
        filterCutoffSlider.setMin(20);
        filterCutoffSlider.setMax(20000);
        filterCutoffSlider.setValue(1000);
        filterCutoffSlider.setShowTickLabels(true);
        filterCutoffSlider.setShowTickMarks(true);

        filterResonanceSlider.setMin(0.1);
        filterResonanceSlider.setMax(1.0);
        filterResonanceSlider.setValue(0.5);
        filterResonanceSlider.setShowTickLabels(true);
        filterResonanceSlider.setShowTickMarks(true);

        // Actualizar labels iniciales
        actualizarLabelsFiltro();

        // Listeners para los controles del filtro
        filterTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setTipoFiltro(newVal);

            // --- NUEVO: SINCRONIZAR LOS SLIDERS CON LOS VALORES POR DEFECTO DEL FILTRO ---
            if (newVal != Filter.Tipo.OFF) {
                // Obtener los valores actuales del filtro y actualizar los sliders
                filterCutoffSlider.setValue(logic.getFrecuenciaCorteFiltro());
                filterResonanceSlider.setValue(logic.getResonanciaFiltro());
            }

            // Sincronizar el toggle button con el tipo de filtro
            filterToggleButton.setSelected(newVal != Filter.Tipo.OFF);
            logic.setFiltroActivado(newVal != Filter.Tipo.OFF);

            // Actualizar los labels
            actualizarLabelsFiltro();
        });

        filterCutoffSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setFrecuenciaCorteFiltro(newVal.doubleValue());
            actualizarLabelsFiltro();
        });

        filterResonanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setResonanciaFiltro(newVal.doubleValue());
            actualizarLabelsFiltro();
        });

        filterToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // Si se activa el toggle y el filtro está en OFF, cambiar a LOW_PASS
            if (newVal && filterTypeCombo.getValue() == Filter.Tipo.OFF) {
                filterTypeCombo.setValue(Filter.Tipo.LOW_PASS);
            }
            // Si se desactiva el toggle, cambiar a OFF
            else if (!newVal) {
                filterTypeCombo.setValue(Filter.Tipo.OFF);
            }
            logic.setFiltroActivado(newVal);
        });
    }
    private void actualizarLabelsFiltro() {
        filterCutoffLabel.setText(String.format("%.0f Hz", filterCutoffSlider.getValue()));
        filterResonanceLabel.setText(String.format("%.2f", filterResonanceSlider.getValue()));
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

//        System.out.println("Sample guardado (" + durationSeconds + "s) y notificación enviada.");
//        guardarSample.setText("Sample Saved");
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