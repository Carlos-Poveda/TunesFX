package org.example.tunesfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;


import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

public class PrincipalController {
    @FXML
    private Button openSynthButton;
    @FXML
    private HBox panelRitmo;
    @FXML
    private Button btnEncenderRitmo;
    @FXML
    private Button btnNuevaPista;
    @FXML
    private VBox rackContainer;
    @FXML
    private Spinner<Integer> spinnerBPM;

    // Lógica del secuenciador
    private Timeline sequencerTimeline;
    private boolean isPlaying = false;
    private static double referenciaBPM = 120.0; // Beats Por Minuto

    // Variables del secuenciador ---
    private List<Button> stepButtons; // Una lista para agrupar los botones
    private static final int NUM_STEPS = ChannelRackRowController.NUM_STEPS;
    private int currentStep = -1; // El paso actual (-1 para que el primero sea 0)

    private List<ChannelRackRowController> allRows = new ArrayList<>();

    /**
     * Este método es llamado automáticamente por el FXMLLoader después
     * de que todos los componentes @FXML han sido inyectados.
     */
    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 300, 120, 1);
        spinnerBPM.setValueFactory(valueFactory);
        spinnerBPM.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSequencerRate(newVal);
        });
        initializeSequencer();
        // --- ¡AQUÍ ESTÁ LA MAGIA! ---
        // 1. Registramos este controlador como "oyente" en el SampleBank.
        // 2. Le decimos: "Cuando recibas un sample, llama a mi método 'addNewRow'".
        // 3. (El "this::addNewRow" es un atajo para "sample -> addNewRow(sample)")
        SampleBank.getInstance().setOnSampleSaved(this::addNewRow);
        // --- FIN DE LA MAGIA ---
    }

    private void initializeSequencer() {
        // Calculamos la duración del paso basado en 120 BPM
        double beatDurationMillis = 60000.0 / referenciaBPM;
        double stepDurationMillis = beatDurationMillis / 4.0;

        sequencerTimeline = new Timeline();
        sequencerTimeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame keyFrame = new KeyFrame(Duration.millis(stepDurationMillis), e -> {
            runSequencerStep();
        });

        sequencerTimeline.getKeyFrames().add(keyFrame);

        // Establecer la tasa de reproducción inicial
        // basada en el valor por defecto del spinner
        updateSequencerRate(spinnerBPM.getValue());
    }

    private void updateSequencerRate(double newBPM) {
        if (sequencerTimeline == null) return;

        // Calcula la nueva tasa (rate)
        // ej. 180 BPM -> 180 / 120 = 1.5x (más rápido)
        // ej. 90 BPM  -> 90 / 120 = 0.75x (más lento)
        double newRate = newBPM / referenciaBPM;

        sequencerTimeline.setRate(newRate);
    }

    @FXML
    private void handleNuevaPista(ActionEvent event) {
        Sample sampleToAdd = SampleBank.getInstance().getCurrentSample();

        if (sampleToAdd == null) {
            System.err.println("¡No hay sample guardado! Abre el sinte y guarda uno primero.");
            return;
        }

        // Llama al método reutilizable
        addNewRow(sampleToAdd);
    }

    /**
     * NUEVO MÉTODO REUTILIZABLE:
     * Esta es la lógica central para añadir una fila.
     * Ahora puede ser llamado por el botón (handleNuevaPista)
     * O por el SampleBank (el listener que pusimos en initialize).
     */
    public void addNewRow(Sample sample) {
        Platform.runLater(() -> {
            if (sample == null) {
                System.err.println("Intento de añadir fila con sample nulo.");
                return;
            }

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
                System.err.println("Error al cargar ChannelRackRow.fxml");
            }
        });
    }

    /**
     * Lógica del secuenciador
     */
    private void runSequencerStep() {
        // 1. Calcular paso anterior y actual
        int prevStep = currentStep;
        currentStep = (currentStep + 1) % NUM_STEPS;

        // 2. Iterar por TODAS las filas
        for (ChannelRackRowController row : allRows) {

            // 3. Mover el playhead visual
            if (prevStep != -1) {
                row.clearPlayhead(prevStep);
            }
            row.setPlayhead(currentStep);

            // 4. Comprobar si esta fila debe sonar en este paso
            if (row.isStepOn(currentStep)) {

                // 5. Si sí, coger SU sample y reproducirlo
                Sample sampleToPlay = row.getSample();
                if (sampleToPlay != null) {
                    SamplePlayer.playSample(sampleToPlay);
                    }
                }
        }
    }

    @FXML
    private void handlePlayRitmo(ActionEvent event) {
        if (isPlaying) {
            stopSequencer();
        } else {
            // No iniciar si no hay pistas
            if (allRows.isEmpty()) {
                System.out.println("Añade una pista primero.");
                return;
            }
            // Iniciar
            sequencerTimeline.play();
            btnEncenderRitmo.setText("Stop");
            isPlaying = true;
            System.out.println("Sequencer Iniciado...");
        }
    }

    /**
     * Función de ayuda para detener y limpiar
     */
    private void stopSequencer() {
        sequencerTimeline.stop();
        btnEncenderRitmo.setText("Play");
        isPlaying = false;

        // Limpiar el playhead de todas las filas
        for (ChannelRackRowController row : allRows) {
            if (currentStep != -1) {
                row.clearPlayhead(currentStep);
            }
        }
        currentStep = -1; // Resetear el contador
        System.out.println("Sequencer Detenido.");
    }


    // Abrir sintetizador

    @FXML
    private void handleOpenSynth(ActionEvent event) {
        try {
            // 1. Cargar el FXML del Sintetizador
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SintetizadorView.fxml"));
            Pane synthRoot = loader.load();

            // 2. Obtener el controlador del Sintetizador
            org.example.tunesfx.SintetizadorController synthController = loader.getController();

            // 3. Crear el nuevo Stage (Ventana)
            Stage synthStage = new Stage();
            Scene synthScene = new Scene(synthRoot);

            // 4. Configurar Listeners de teclado y cierre
            //    Redirigir los eventos de la Scene al Controlador
            synthScene.setOnKeyPressed(synthController::handleKeyPressed);
            synthScene.setOnKeyReleased(synthController::handleKeyReleased);

            synthStage.setOnCloseRequest(e -> synthController.shutdown());

            // 5. Mostrar la ventana
            synthStage.setTitle("Sintetizador");
            synthStage.setScene(synthScene);
            synthStage.setResizable(false);
            synthStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}