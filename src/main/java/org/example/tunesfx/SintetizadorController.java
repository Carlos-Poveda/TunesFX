package org.example.tunesfx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;

public class SintetizadorController {

    public Button guardarSample;

    // Osciladores y WaveViewer
    @FXML private Oscilator oscilador1;
    @FXML private Oscilator oscilador2;
    @FXML private Oscilator oscilador3;
    @FXML private Oscilator oscilador4;
    @FXML private Oscilator oscilador5;
    @FXML private WaveViewer waveViewer;

    // Filtro
    @FXML private CheckBox filterEnableCheckbox;
    @FXML private ComboBox<String> filterTypeComboBox;
    @FXML private Slider filterCutoffSlider;
    @FXML private Slider filterResonanceSlider;
    @FXML private Label filterCutoffLabel;
    @FXML private Label filterResonanceLabel;

    // LFO
    @FXML private ComboBox<String> lfo1WaveformComboBox;
    @FXML private Slider lfo1FrequencySlider;
    @FXML private Slider lfo1AmplitudeSlider;
    @FXML private Slider lfo1ToFilterSlider;
    @FXML private Slider lfo1ToPitchSlider;
    @FXML private Label lfo1FrequencyLabel;
    @FXML private Label lfo1AmplitudeLabel;
    @FXML private Label lfo1ToFilterLabel;
    @FXML private Label lfo1ToPitchLabel;

    // MOD ENV
    @FXML private Slider modAttackSlider;
    @FXML private Slider modDecaySlider;
    @FXML private Slider modSustainSlider;
    @FXML private Slider modReleaseSlider;
    @FXML private Slider modEnvToFilterSlider;
    @FXML private Label modAttackLabel;
    @FXML private Label modDecayLabel;
    @FXML private Label modSustainLabel;
    @FXML private Label modReleaseLabel;
    @FXML private Label modEnvToFilterLabel;

    // Efectos
    @FXML private Slider reverbLevelSlider;
    @FXML private Slider delayLevelSlider;
    @FXML private Slider delayFeedbackSlider;
    @FXML private Slider delayTimeSlider;
    @FXML private Slider effectsMixSlider;
    @FXML private Label reverbLevelLabel;
    @FXML private Label delayLevelLabel;
    @FXML private Label delayFeedbackLabel;
    @FXML private Label delayTimeLabel;
    @FXML private Label effectsMixLabel;

    // Global
    @FXML private Slider glideTimeSlider;
    @FXML private Label glideTimeLabel;

    // Unison
    @FXML private Slider unisonVoicesSlider;
    @FXML private Slider unisonDetuneSlider;
    @FXML private Slider unisonSpreadSlider;
    @FXML private Label unisonVoicesLabel;
    @FXML private Label unisonDetuneLabel;
    @FXML private Label unisonSpreadLabel;

    private Oscilator[] oscillators;
    private Sintetizador logic;

    @FXML
    public void initialize() {
        guardarSample.setText("Save Sample");

        // Agrupar los osciladores
        oscillators = new Oscilator[] {
                oscilador1, oscilador2, oscilador3, oscilador4, oscilador5
        };

        // Crear la instancia de la lógica del sintetizador
        logic = new Sintetizador(oscillators, waveViewer);
        logic.setUpdateCallback(waveViewer::draw);

        // Configurar callbacks de los osciladores
        for (Oscilator osc : oscillators) {
            osc.setUpdateCallback(logic::updateWaveviewer);
        }

        guardarSample.setOnAction(e -> handleSaveSample());

        // Inicializar todos los controles
        initializeFilterControls();
        initializeLFOControls();
        initializeModEnvControls();
        initializeEffectsControls();
        initializeGlobalControls();
        initializeUnisonControls();
    }

    private void initializeFilterControls() {
        // Configurar ComboBox de tipos de filtro
        filterTypeComboBox.getItems().addAll("Low Pass", "High Pass", "Band Pass", "Notch");
        filterTypeComboBox.setValue("Low Pass");

        // Configurar sliders
        filterCutoffSlider.setMin(20);
        filterCutoffSlider.setMax(22000);
        filterCutoffSlider.setValue(12000);
        filterCutoffSlider.setBlockIncrement(100);

        filterResonanceSlider.setMin(0.1);
        filterResonanceSlider.setMax(12.0);
        filterResonanceSlider.setValue(0.7);
        filterResonanceSlider.setBlockIncrement(0.1);

        // Listeners
        filterEnableCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            logic.setFilterEnabled(newVal);
            filterTypeComboBox.setDisable(!newVal);
            filterCutoffSlider.setDisable(!newVal);
            filterResonanceSlider.setDisable(!newVal);

            if (newVal) {
                filterEnableCheckbox.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                filterEnableCheckbox.setStyle("-fx-text-fill: #ccc;");
            }
        });

        filterTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Filter.FilterType type = Filter.FilterType.LOW_PASS;
            switch (newVal) {
                case "High Pass": type = Filter.FilterType.HIGH_PASS; break;
                case "Band Pass": type = Filter.FilterType.BAND_PASS; break;
                case "Notch": type = Filter.FilterType.NOTCH; break;
                default: type = Filter.FilterType.LOW_PASS;
            }
            logic.setFilterType(type);
        });

        filterCutoffSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setFilterCutoff(newVal.doubleValue());
            if (newVal.doubleValue() >= 1000) {
                filterCutoffLabel.setText(String.format("%.1f kHz", newVal.doubleValue() / 1000));
            } else {
                filterCutoffLabel.setText(String.format("%.0f Hz", newVal.doubleValue()));
            }
        });

        filterResonanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setFilterResonance(newVal.doubleValue());
            filterResonanceLabel.setText(String.format("%.1f", newVal.doubleValue()));

            if (newVal.doubleValue() > 8.0) {
                filterResonanceLabel.setStyle("-fx-text-fill: #ff4444;");
            } else if (newVal.doubleValue() > 4.0) {
                filterResonanceLabel.setStyle("-fx-text-fill: #ffaa00;");
            } else {
                filterResonanceLabel.setStyle("-fx-text-fill: #ccc;");
            }
        });

        // Valores iniciales
        filterCutoffLabel.setText("12.0 kHz");
        filterResonanceLabel.setText("0.7");
        filterEnableCheckbox.setSelected(true);
        filterEnableCheckbox.setStyle("-fx-text-fill: #4CAF50;");
    }

    private void initializeLFOControls() {
        // Configurar ComboBox de formas de onda LFO
        lfo1WaveformComboBox.getItems().addAll("SINE", "TRIANGLE", "SQUARE", "SAW", "RAMP", "RANDOM");
        lfo1WaveformComboBox.setValue("SINE");

        // Configurar sliders LFO
        lfo1FrequencySlider.setMin(0.1);
        lfo1FrequencySlider.setMax(20.0);
        lfo1FrequencySlider.setValue(1.0);
        lfo1FrequencySlider.setBlockIncrement(0.1);

        lfo1AmplitudeSlider.setMin(0.0);
        lfo1AmplitudeSlider.setMax(1.0);
        lfo1AmplitudeSlider.setValue(1.0);
        lfo1AmplitudeSlider.setBlockIncrement(0.1);

        lfo1ToFilterSlider.setMin(0.0);
        lfo1ToFilterSlider.setMax(1.0);
        lfo1ToFilterSlider.setValue(0.0);
        lfo1ToFilterSlider.setBlockIncrement(0.1);

        lfo1ToPitchSlider.setMin(0.0);
        lfo1ToPitchSlider.setMax(1.0);
        lfo1ToPitchSlider.setValue(0.0);
        lfo1ToPitchSlider.setBlockIncrement(0.1);

        // Listeners LFO
        lfo1WaveformComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            LFO.Waveform wave = LFO.Waveform.SINE;
            switch (newVal) {
                case "TRIANGLE": wave = LFO.Waveform.TRIANGLE; break;
                case "SQUARE": wave = LFO.Waveform.SQUARE; break;
                case "SAW": wave = LFO.Waveform.SAW; break;
                case "RAMP": wave = LFO.Waveform.RAMP; break;
                case "RANDOM": wave = LFO.Waveform.RANDOM; break;
                default: wave = LFO.Waveform.SINE;
            }
            logic.setLFO1Waveform(wave);
        });

        lfo1FrequencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFO1Frequency(newVal.doubleValue());
            lfo1FrequencyLabel.setText(String.format("%.1f Hz", newVal.doubleValue()));
        });

        lfo1AmplitudeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFO1Amplitude(newVal.doubleValue());
            lfo1AmplitudeLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        lfo1ToFilterSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFO1ToFilter(newVal.doubleValue());
            lfo1ToFilterLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        lfo1ToPitchSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setLFO1ToPitch(newVal.doubleValue());
            lfo1ToPitchLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        // Valores iniciales LFO
        lfo1FrequencyLabel.setText("1.0 Hz");
        lfo1AmplitudeLabel.setText("1.0");
        lfo1ToFilterLabel.setText("0.0");
        lfo1ToPitchLabel.setText("0.0");
    }

    private void initializeModEnvControls() {
        // Configurar sliders de MOD ENV
        modAttackSlider.setMin(0.001);
        modAttackSlider.setMax(5.0);
        modAttackSlider.setValue(0.01);
        modAttackSlider.setBlockIncrement(0.01);

        modDecaySlider.setMin(0.001);
        modDecaySlider.setMax(5.0);
        modDecaySlider.setValue(0.1);
        modDecaySlider.setBlockIncrement(0.01);

        modSustainSlider.setMin(0.0);
        modSustainSlider.setMax(1.0);
        modSustainSlider.setValue(0.5);
        modSustainSlider.setBlockIncrement(0.1);

        modReleaseSlider.setMin(0.001);
        modReleaseSlider.setMax(5.0);
        modReleaseSlider.setValue(0.2);
        modReleaseSlider.setBlockIncrement(0.01);

        modEnvToFilterSlider.setMin(0.0);
        modEnvToFilterSlider.setMax(1.0);
        modEnvToFilterSlider.setValue(0.0);
        modEnvToFilterSlider.setBlockIncrement(0.1);

        // Listeners de MOD ENV
        modAttackSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setModEnvAttack(newVal.doubleValue());
            modAttackLabel.setText(String.format("%.2f s", newVal.doubleValue()));
        });

        modDecaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setModEnvDecay(newVal.doubleValue());
            modDecayLabel.setText(String.format("%.2f s", newVal.doubleValue()));
        });

        modSustainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setModEnvSustain(newVal.doubleValue());
            modSustainLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        modReleaseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setModEnvRelease(newVal.doubleValue());
            modReleaseLabel.setText(String.format("%.2f s", newVal.doubleValue()));
        });

        modEnvToFilterSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setModEnvToFilter(newVal.doubleValue());
            modEnvToFilterLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        // Valores iniciales
        modAttackLabel.setText("0.01 s");
        modDecayLabel.setText("0.10 s");
        modSustainLabel.setText("0.50");
        modReleaseLabel.setText("0.20 s");
        modEnvToFilterLabel.setText("0.00");
    }

    private void initializeEffectsControls() {
        // Configurar sliders de Efectos
        reverbLevelSlider.setMin(0.0);
        reverbLevelSlider.setMax(1.0);
        reverbLevelSlider.setValue(0.0);
        reverbLevelSlider.setBlockIncrement(0.1);

        delayLevelSlider.setMin(0.0);
        delayLevelSlider.setMax(1.0);
        delayLevelSlider.setValue(0.0);
        delayLevelSlider.setBlockIncrement(0.1);

        delayFeedbackSlider.setMin(0.0);
        delayFeedbackSlider.setMax(0.95);
        delayFeedbackSlider.setValue(0.5);
        delayFeedbackSlider.setBlockIncrement(0.05);

        delayTimeSlider.setMin(0.01);
        delayTimeSlider.setMax(2.0);
        delayTimeSlider.setValue(0.5);
        delayTimeSlider.setBlockIncrement(0.1);

        effectsMixSlider.setMin(0.0);
        effectsMixSlider.setMax(1.0);
        effectsMixSlider.setValue(0.0);
        effectsMixSlider.setBlockIncrement(0.1);

        // Listeners de Efectos
        reverbLevelSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setReverbLevel(newVal.doubleValue());
            reverbLevelLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        delayLevelSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setDelayLevel(newVal.doubleValue());
            delayLevelLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        delayFeedbackSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setDelayFeedback(newVal.doubleValue());
            delayFeedbackLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        delayTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setDelayTime(newVal.doubleValue());
            delayTimeLabel.setText(String.format("%.2f s", newVal.doubleValue()));
        });

        effectsMixSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setEffectsMix(newVal.doubleValue());
            effectsMixLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        // Valores iniciales
        reverbLevelLabel.setText("0.0");
        delayLevelLabel.setText("0.0");
        delayFeedbackLabel.setText("0.50");
        delayTimeLabel.setText("0.50 s");
        effectsMixLabel.setText("0.0");
    }

    private void initializeGlobalControls() {
        // Configurar slider de Glide
        glideTimeSlider.setMin(0.0);
        glideTimeSlider.setMax(5.0);
        glideTimeSlider.setValue(0.0);
        glideTimeSlider.setBlockIncrement(0.1);

        glideTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setGlideTime(newVal.doubleValue());
            glideTimeLabel.setText(String.format("%.2f s", newVal.doubleValue()));
        });

        glideTimeLabel.setText("0.00 s");
    }

    private void initializeUnisonControls() {
        // Configurar sliders de Unison
        unisonVoicesSlider.setMin(1);
        unisonVoicesSlider.setMax(8);
        unisonVoicesSlider.setValue(1);
        unisonVoicesSlider.setBlockIncrement(1);

        unisonDetuneSlider.setMin(0.0);
        unisonDetuneSlider.setMax(50.0);
        unisonDetuneSlider.setValue(0.0);
        unisonDetuneSlider.setBlockIncrement(1.0);

        unisonSpreadSlider.setMin(0.0);
        unisonSpreadSlider.setMax(1.0);
        unisonSpreadSlider.setValue(0.0);
        unisonSpreadSlider.setBlockIncrement(0.1);

        // Listeners de Unison
        unisonVoicesSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setUnisonVoices(newVal.intValue());
            unisonVoicesLabel.setText(String.format("%d", newVal.intValue()));
        });

        unisonDetuneSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setUnisonDetune(newVal.doubleValue());
            unisonDetuneLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        unisonSpreadSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            logic.setUnisonSpread(newVal.doubleValue());
            unisonSpreadLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        // Valores iniciales
        unisonVoicesLabel.setText("1");
        unisonDetuneLabel.setText("0.0");
        unisonSpreadLabel.setText("0.0");
    }

    private void handleSaveSample() {
        int sampleLength = Sintetizador.AudioInfo.SAMPLE_RATE;
        short[] sampleData = logic.generateSample(sampleLength);
        Sample newSample = new Sample(sampleData);
        SampleBank.getInstance().setCurrentSample(newSample);
        guardarSample.setText("Sample Saved!");
        System.out.println("Sample guardado en el banco. Longitud: " + sampleData.length);
    }

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