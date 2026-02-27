package org.example.tunesfx.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.SampleBank;
import org.example.tunesfx.synth.*;
import java.io.IOException;

public class SintetizadorController {
    @FXML private Pane rootPane;
    @FXML private Spinner<Double> sampleLengthSpinner;
    @FXML public Button guardarSample;
// --- OSCILADORES ---
    @FXML private Oscilator oscilador1;
    @FXML private Oscilator oscilador2;
    @FXML private Oscilator oscilador3;
    @FXML private Oscilator oscilador4;
    @FXML private Oscilator oscilador5;
    @FXML private WaveViewer waveViewer;
// --- NUEVO: ADSR ENVELOPE ---
    @FXML private Slider adsrAttackSlider;
    @FXML private Slider adsrDecaySlider;
    @FXML private Slider adsrSustainSlider;
    @FXML private Slider adsrReleaseSlider;
// --- FILTRO (MOOG) ---
    @FXML private ToggleButton filterToggleButton;
    @FXML private ComboBox<String> filterPresetsCombo;
    @FXML private ComboBox<Filter.Tipo> filterTypeCombo;
    @FXML private Slider filterCutoffSlider;
    @FXML private Slider filterResonanceSlider;
    @FXML private Label filterCutoffLabel;
    @FXML private Label filterResonanceLabel;
// --- LFO ---
    @FXML private ToggleButton lfoToggleButton;
    @FXML private ComboBox<String> lfoPresetsCombo;
    @FXML private ComboBox<LFO.Waveform> lfoWaveformCombo;
    @FXML private ComboBox<LFO.Target> lfoTargetCombo;
    @FXML private Slider lfoRateSlider;
    @FXML private Slider lfoAmountSlider;
    @FXML private Label lfoRateLabel;
    @FXML private Label lfoAmountLabel;
// --- DELAY FX ---
    @FXML private ToggleButton delayToggleButton;
    @FXML private Slider delayTimeSlider;
    @FXML private Slider delayFeedbackSlider;
    @FXML private Slider delayMixSlider;
    @FXML private Label delayTimeLabel;
    @FXML private Label delayFeedbackLabel;
    @FXML private Label delayMixLabel;
    @FXML private PianoComponent pianoComponent;
    private Oscilator[] oscillators;
    private Sintetizador logic;

    @FXML
    public void initialize() {
        guardarSample.setText("Save sample");
        guardarSample.setOnAction(e -> handleSaveSample());
        oscillators = new Oscilator[] {

                oscilador1, oscilador2, oscilador3, oscilador4, oscilador5

        };
        logic = new Sintetizador(oscillators, waveViewer);
        logic.setUpdateCallback(waveViewer::draw);
        for (Oscilator osc : oscillators) {
            osc.setUpdateCallback(logic::updateWaveviewer);
        }
        SpinnerValueFactory<Double> valueFactory =
                new DoubleSpinnerValueFactory(0.1, 10.0, 1.0, 0.1);
        sampleLengthSpinner.setValueFactory(valueFactory);
        rootPane.setFocusTraversable(true);
        rootPane.setOnMouseClicked(e -> {
            rootPane.requestFocus();
            e.consume();
        });
        if (pianoComponent != null) {
            pianoComponent.setSynthLogic(logic);
        }
// Configurar TODOS los módulos
        configurarADSR();
        configurarFiltro();
        configurarLFO();
        configurarDelay();
    }

// --- CONFIGURACIÓN ADSR ---
    private void configurarADSR() {
// Attack
        adsrAttackSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                logic.setAttack(newVal.doubleValue()));
// Decay
        adsrDecaySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                logic.setDecay(newVal.doubleValue()));
// Sustain
        adsrSustainSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                logic.setSustain(newVal.doubleValue()));
// Release
        adsrReleaseSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                logic.setRelease(newVal.doubleValue()));


// Valores iniciales (Coinciden con los del FXML)
        logic.setAttack(adsrAttackSlider.getValue());
        logic.setDecay(adsrDecaySlider.getValue());
        logic.setSustain(adsrSustainSlider.getValue());
        logic.setRelease(adsrReleaseSlider.getValue());
    }

// --- CONFIGURACIÓN DEL FILTRO ---
    private void configurarFiltro() {
        filterTypeCombo.setItems(FXCollections.observableArrayList(Filter.Tipo.values()));
        filterTypeCombo.setValue(Filter.Tipo.OFF);
        filterCutoffSlider.setMin(20); filterCutoffSlider.setMax(10000); filterCutoffSlider.setValue(2000);
        filterResonanceSlider.setMin(0.0); filterResonanceSlider.setMax(1.0); filterResonanceSlider.setValue(0.0);
        actualizarLabelsFiltro();
        filterTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setTipoFiltro(newVal);
            if (newVal != Filter.Tipo.OFF) {
                filterCutoffSlider.setValue(logic.getFrecuenciaCorteFiltro());
                filterResonanceSlider.setValue(logic.getResonanciaFiltro());
                filterToggleButton.setSelected(true);
            } else {
                filterToggleButton.setSelected(false);
            }
            logic.setFiltroActivado(newVal != Filter.Tipo.OFF);
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
            logic.setFiltroActivado(newVal);
            if (newVal && filterTypeCombo.getValue() == Filter.Tipo.OFF) {
                filterTypeCombo.setValue(Filter.Tipo.LOW_PASS);
            } else if (!newVal) {
                filterTypeCombo.setValue(Filter.Tipo.OFF);
            }
        });
        filterPresetsCombo.setItems(FXCollections.observableArrayList(
                "Init (Clean)", "Warm Pad", "Acid Bass", "Bright Lead", "Submarine", "Screaming Lead"
        ));
        filterPresetsCombo.setOnAction(e -> aplicarPresetFiltro(filterPresetsCombo.getValue()));
    }

    private void aplicarPresetFiltro(String presetName) {
        if (presetName == null) return;
        switch (presetName) {
            case "Init (Clean)": filterTypeCombo.setValue(Filter.Tipo.OFF); filterCutoffSlider.setValue(10000); filterResonanceSlider.setValue(0.0); break;
            case "Warm Pad": filterTypeCombo.setValue(Filter.Tipo.LOW_PASS); filterCutoffSlider.setValue(800); filterResonanceSlider.setValue(0.2); break;
            case "Acid Bass": filterTypeCombo.setValue(Filter.Tipo.LOW_PASS); filterCutoffSlider.setValue(450); filterResonanceSlider.setValue(0.85); break;
            case "Bright Lead": filterTypeCombo.setValue(Filter.Tipo.HIGH_PASS); filterCutoffSlider.setValue(300); filterResonanceSlider.setValue(0.1); break;
            case "Submarine": filterTypeCombo.setValue(Filter.Tipo.LOW_PASS); filterCutoffSlider.setValue(200); filterResonanceSlider.setValue(0.6); break;
            case "Screaming Lead": filterTypeCombo.setValue(Filter.Tipo.LOW_PASS); filterCutoffSlider.setValue(2500); filterResonanceSlider.setValue(0.95); break;
        }
    }

    private void actualizarLabelsFiltro() {
        filterCutoffLabel.setText(String.format("%.0f Hz", filterCutoffSlider.getValue()));
        filterResonanceLabel.setText(String.format("%.2f", filterResonanceSlider.getValue()));
    }

// --- CONFIGURACIÓN DEL LFO ---
    private void configurarLFO() {
        lfoWaveformCombo.setItems(FXCollections.observableArrayList(LFO.Waveform.values()));
        lfoWaveformCombo.setValue(LFO.Waveform.SINE);
        lfoTargetCombo.setItems(FXCollections.observableArrayList(LFO.Target.values()));
        lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF);
        lfoRateSlider.setMin(0.1); lfoRateSlider.setMax(20.0); lfoRateSlider.setValue(1.0);
        lfoAmountSlider.setMin(0.0); lfoAmountSlider.setMax(1.0); lfoAmountSlider.setValue(0.5);
        actualizarLabelsLFO();
        lfoWaveformCombo.valueProperty().addListener((obs, oldVal, newVal) -> logic.setLFWaveform(newVal));
        lfoTargetCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFOTarget(newVal);
            boolean active = newVal != LFO.Target.NONE;
            lfoToggleButton.setSelected(active);
            logic.setLFOActivado(active);
        });
        lfoRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFORate(newVal.doubleValue()); actualizarLabelsLFO();
        });
        lfoAmountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFOAmount(newVal.doubleValue()); actualizarLabelsLFO();
        });
        lfoToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFOActivado(newVal);
            if (newVal && lfoTargetCombo.getValue() == LFO.Target.NONE) lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF);
        });
        lfoPresetsCombo.setItems(FXCollections.observableArrayList(
                "Vibrato (Pitch)", "Dubstep Wobble", "Slow Sweep", "Tremolo (Vol)", "R2-D2 (Random)"
        ));
        lfoPresetsCombo.setOnAction(e -> aplicarPresetLFO(lfoPresetsCombo.getValue()));
    }

    private void aplicarPresetLFO(String presetName) {
        if (presetName == null) return;
        switch (presetName) {
            case "Vibrato (Pitch)": lfoTargetCombo.setValue(LFO.Target.PITCH); lfoWaveformCombo.setValue(LFO.Waveform.SINE); lfoRateSlider.setValue(5.0); lfoAmountSlider.setValue(0.05); break;
            case "Dubstep Wobble": lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF); lfoWaveformCombo.setValue(LFO.Waveform.SINE); lfoRateSlider.setValue(4.0); lfoAmountSlider.setValue(0.8); break;
            case "Slow Sweep": lfoTargetCombo.setValue(LFO.Target.FILTER_CUTOFF); lfoWaveformCombo.setValue(LFO.Waveform.TRIANGLE); lfoRateSlider.setValue(0.2); lfoAmountSlider.setValue(0.6); break;
            case "Tremolo (Vol)": lfoTargetCombo.setValue(LFO.Target.OSC_VOLUME); lfoWaveformCombo.setValue(LFO.Waveform.TRIANGLE); lfoRateSlider.setValue(6.0); lfoAmountSlider.setValue(0.6); break;
            case "R2-D2 (Random)": lfoTargetCombo.setValue(LFO.Target.PITCH); lfoWaveformCombo.setValue(LFO.Waveform.RANDOM); lfoRateSlider.setValue(12.0); lfoAmountSlider.setValue(1.0); break;
        }
        lfoToggleButton.setSelected(true);
        logic.setLFOActivado(true);
    }

    private void actualizarLabelsLFO() {
        lfoRateLabel.setText(String.format("%.1f Hz", lfoRateSlider.getValue()));
        lfoAmountLabel.setText(String.format("%.2f", lfoAmountSlider.getValue()));
    }

// --- CONFIGURACIÓN DEL DELAY ---
    private void configurarDelay() {
        delayTimeSlider.setValue(0.5); delayFeedbackSlider.setValue(0.5); delayMixSlider.setValue(0.3);
        delayToggleButton.setSelected(true);
        delayTimeLabel.setText("0.50s"); delayFeedbackLabel.setText("50%"); delayMixLabel.setText("30%");
        delayToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> logic.setDelayActive(newVal));
        delayTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> { logic.setDelayTime(newVal.doubleValue()); delayTimeLabel.setText(String.format("%.2fs", newVal)); });
        delayFeedbackSlider.valueProperty().addListener((obs, oldVal, newVal) -> { logic.setDelayFeedback(newVal.doubleValue()); delayFeedbackLabel.setText((int)(newVal.doubleValue() * 100) + "%"); });
        delayMixSlider.valueProperty().addListener((obs, oldVal, newVal) -> { logic.setDelayMix(newVal.doubleValue()); delayMixLabel.setText((int)(newVal.doubleValue() * 100) + "%"); });
    }

    private void handleSaveSample() {
        double durationSeconds = sampleLengthSpinner.getValue();
        int sampleLength = (int) (durationSeconds * Sintetizador.AudioInfo.SAMPLE_RATE);
        short[] sampleData = logic.generateSample(sampleLength);
        Sample newSample = new Sample(sampleData);
        SampleBank.getInstance().setCurrentSample(newSample);
    }



    public void handleKeyPressed(KeyEvent e) {
        String keyString = e.getText();
        if (keyString.isEmpty()) if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) keyString = e.getCode().toString().toLowerCase();
        if (keyString.length() == 1) logic.onKeyPressed(keyString.charAt(0));
        e.consume();
    }

    public void handleKeyReleased(KeyEvent e) {
        String keyString = e.getText();
        if (keyString.isEmpty()) {
            if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) {
                keyString = e.getCode().toString().toLowerCase();
            }
        }
        if (keyString.length() == 1) {
            // --- CORRECCIÓN: Pasamos el caracter al sintetizador ---
            logic.onKeyReleased(keyString.charAt(0));
        }
    }

    @FXML
    private void handleOpenPianoRoll() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tunesfx/PianoRollView.fxml"));
            Parent root = loader.load();
            // --- CONEXIÓN NUEVA ---
            // Obtenemos el controlador del Piano Roll y le pasamos la lógica de audio
            PianoRollController pianoRoll = loader.getController();
            pianoRoll.setSynth(this.logic); // 'logic' es tu instancia de Sintetizador
            // ----------------------
            Stage stage = new Stage();
            stage.setTitle("Piano Roll Editor");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al abrir el Piano Roll: " + e.getMessage());
        }
    }
}