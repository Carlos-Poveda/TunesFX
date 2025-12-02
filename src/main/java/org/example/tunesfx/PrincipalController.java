package org.example.tunesfx;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.SampleBank;
import org.example.tunesfx.audio.SamplePlayer;
import org.example.tunesfx.audio.StepData;
import javafx.stage.FileChooser;
import org.example.tunesfx.utils.AudioFileLoader;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
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

    @FXML
    private void handleNuevaPista(ActionEvent event) {
        cargarSonidoExterno();
    }

    private void cargarSonidoExterno() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load sample (WAV)");

        // Filtro para solo ver archivos WAV
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de Audio WAV", "*.wav")
        );

        // Obtener la ventana actual para mostrar el diálogo encima
        Stage stage = (Stage) btnNuevaPista.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                // 1. Usar nuestro cargador para obtener los datos raw
                short[] data = AudioFileLoader.loadSample(file);

                // 2. Crear un objeto Sample nativo de tu motor
                Sample externalSample = new Sample(data);

                // 3. Añadir la fila directamente
                addNewRow(externalSample);
//                System.out.println("Sample externo cargado: " + file.getName());
            } catch (IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
                System.err.println("Error al cargar el archivo de audio.");
            }
        }
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
        // 1. Calcular el punto de retorno dinámico (Loop Point)
        int maxActiveStep = -1;

        for (ChannelRackRowController row : allRows) {
            // Buscamos cuál es el paso activo más lejano de TODAS las filas
            int rowLastStep = row.getLastActiveStepIndex();
            if (rowLastStep > maxActiveStep) {
                maxActiveStep = rowLastStep;
            }
        }

        // 2. Definir el límite del bucle
        // Si no hay ninguna nota (maxActiveStep == -1), ponemos un mínimo (ej. 15 para 16 pasos)
        // para que el secuenciador no se vuelva loco o se quede en 0.
        int loopLimit = (maxActiveStep == -1) ? 15 : maxActiveStep;

           if (maxActiveStep != -1) {
               // Redondea hacia arriba al siguiente múltiplo de 4 (ej: si acaba en 2, sube a 3)
               // Esto hace que los bucles suenen más "cuadrados" musicalmente.
               int remainder = (loopLimit + 1) % 4;
               if (remainder != 0) {
                   loopLimit += (4 - remainder);
               }
           }

        // 3. Avanzar el paso
        int prevStep = currentStep;

        // Si el paso actual ya superó el límite (porque borraste notas mientras sonaba)
        // O si hemos llegado al final del bucle activo...
        if (currentStep >= loopLimit) {
            currentStep = 0; // ...reiniciamos al principio
        } else {
            currentStep++;   // ...avanzamos normal
        }

        // 4. Actualizar la UI y reproducir (El resto es igual que antes)
        for (ChannelRackRowController row : allRows) {
            if (prevStep != -1) {
                row.clearPlayhead(prevStep);
            }
            // Asegurarnos de limpiar también si saltamos bruscamente de un paso alto a 0
            if (currentStep == 0 && prevStep > 0) {
                row.clearPlayhead(prevStep);
            }

            row.setPlayhead(currentStep);

            // Reproducir sonido
            StepData stepData = row.getStepData(currentStep);
            if (stepData != null && stepData.isActive()) {
                Sample sampleToPlay = row.getSample();
                if (sampleToPlay != null) {
                    SamplePlayer.playStep(sampleToPlay, stepData);
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
                return;
            }
            // Iniciar
            sequencerTimeline.play();
            btnEncenderRitmo.setText("Stop");
            isPlaying = true;
//            System.out.println("Sequencer Iniciado...");
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
//        System.out.println("Sequencer Detenido.");
    }


    // Abrir sintetizador

    @FXML
    private void handleOpenSynth(ActionEvent event) {
        try {
            // 1. Cargar el FXML del Sintetizador
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SintetizadorView.fxml"));
            Pane synthRoot = loader.load();

            // 2. Obtener el controlador del Sintetizador
            SintetizadorController synthController = loader.getController();

            // 3. Crear el nuevo Stage (Ventana)
            Stage synthStage = new Stage();
            Scene synthScene = new Scene(synthRoot);

            // 4. Configurar Listeners de teclado y cierre
            //    Redirigir los eventos de la Scene al Controlador
            synthScene.setOnKeyPressed(synthController::handleKeyPressed);
            synthScene.setOnKeyReleased(synthController::handleKeyReleased);

            synthStage.setOnCloseRequest(e -> synthController.shutdown());

            // 5. Mostrar la ventana
            synthStage.setTitle("Synth");
            synthStage.setScene(synthScene);
            synthStage.setResizable(false);
            synthStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}