package org.example.tunesfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.SampleBank;
import org.example.tunesfx.audio.SamplePlayer;
import org.example.tunesfx.audio.StepData;
import org.example.tunesfx.utils.AudioFileLoader;
import org.example.tunesfx.utils.GlobalState;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChannelRackController {

    @FXML private Button btnEncenderRitmo;
    @FXML private Button btnNuevaPista;
    @FXML private VBox rackContainer;
    @FXML private Spinner<Integer> spinnerBPM;

    // Lógica del secuenciador
    private Timeline sequencerTimeline;
    private boolean isPlaying = false;
    private static double referenciaBPM = 120.0; // Beats Por Minuto

    private static final int NUM_STEPS = ChannelRackRowController.NUM_STEPS;
    private int currentStep = -1;
    private List<ChannelRackRowController> allRows = new ArrayList<>();

    @FXML
    public void initialize() {
        // Ya no configuramos un ValueFactory local.
        // En su lugar, escuchamos al GlobalState:
        GlobalState.bpmProperty().addListener((obs, oldVal, newVal) -> {
            updateSequencerRate(newVal.doubleValue());
        });

        initializeSequencer();
        SampleBank.getInstance().setOnSampleSaved(this::addNewRow);
    }

    @FXML
    private void addSample(ActionEvent event) {
        cargarSonidoExterno();
    }

    private void cargarSonidoExterno() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load sample (WAV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Audio WAV", "*.wav"));

        Stage stage = (Stage) btnNuevaPista.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                short[] data = AudioFileLoader.loadSample(file);
                Sample externalSample = new Sample(data);
                addNewRow(externalSample);
            } catch (IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeSequencer() {
        // Usamos el valor inicial de GlobalState
        double currentBpm = GlobalState.getBpm();
        double beatDurationMillis = 60000.0 / 120.0; // Mantenemos 120 como base de escala
        double stepDurationMillis = beatDurationMillis / 4.0;

        sequencerTimeline = new Timeline();
        sequencerTimeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame keyFrame = new KeyFrame(Duration.millis(stepDurationMillis), e -> runSequencerStep());
        sequencerTimeline.getKeyFrames().add(keyFrame);

        // Sincronizar velocidad inicial
        updateSequencerRate(currentBpm);
    }

    private void updateSequencerRate(double newBPM) {
        if (sequencerTimeline == null) return;
        // El rate es relativo a la base de 120 BPM
        double newRate = newBPM / 120.0;
        sequencerTimeline.setRate(newRate);
    }

    public void addNewRow(Sample sample) {
        Platform.runLater(() -> {
            if (sample == null) return;
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ChannelRackRow.fxml"));
                HBox newRowNode = loader.load();
                ChannelRackRowController rowController = loader.getController();
                rowController.setSample(sample);
                rowController.setOnDelete(() -> {
                    allRows.remove(rowController);
                    rackContainer.getChildren().remove(newRowNode);
                });
                allRows.add(rowController);
                rackContainer.getChildren().add(newRowNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void runSequencerStep() {
        int maxActiveStep = -1;
        for (ChannelRackRowController row : allRows) {
            int rowLastStep = row.getLastActiveStepIndex();
            if (rowLastStep > maxActiveStep) maxActiveStep = rowLastStep;
        }

        int loopLimit = (maxActiveStep == -1) ? 15 : maxActiveStep;
        if (maxActiveStep != -1) {
            int remainder = (loopLimit + 1) % 4;
            if (remainder != 0) loopLimit += (4 - remainder);
        }

        int prevStep = currentStep;
        if (currentStep >= loopLimit) currentStep = 0;
        else currentStep++;

        double currentBPM = GlobalState.getBpm();
        double stepDuration = (60000.0 / currentBPM) / 4.0;

        for (ChannelRackRowController row : allRows) {
            if (prevStep != -1) row.clearPlayhead(prevStep);
            if (currentStep == 0 && prevStep > 0) row.clearPlayhead(prevStep);

            row.setPlayhead(currentStep);

            StepData stepData = row.getStepData(currentStep);
            if (stepData != null && stepData.isActive()) {
                Sample sampleToPlay = row.getSample();
                if (sampleToPlay != null) {
                    SamplePlayer.playStep(sampleToPlay, stepData, stepDuration);
                }
            }
        }
    }

    @FXML
    private void handlePlayRitmo(ActionEvent event) {
        if (isPlaying) stopSequencer();
        else {
            if (allRows.isEmpty()) return;
            sequencerTimeline.play();
            isPlaying = true;
        }
    }

    private void stopSequencer() {
        sequencerTimeline.stop();
        isPlaying = false;
        for (ChannelRackRowController row : allRows) {
            if (currentStep != -1) row.clearPlayhead(currentStep);
        }
        currentStep = -1;
    }

    // Método para apagar todo si cierran la ventana
    public void shutdown() {
        stopSequencer();
    }
}