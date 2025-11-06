package org.example.tunesfx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.example.tunesfx.Sample;
import org.example.tunesfx.SampleBank;
import org.example.tunesfx.SamplePlayer;

import java.util.ArrayList;
import java.util.Arrays; //
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
    private VBox rackContainer; // <-- El nuevo contenedor de filas

    // Lógica del secuenciador
    private Timeline sequencerTimeline;
    private boolean isPlaying = false;
    private static final double BPM = 120.0; // Beats Por Minuto

    // Variables del secuenciador ---
    private List<Button> stepButtons; // Una lista para agrupar los botones
    private static final int NUM_STEPS = ChannelRackRowController.NUM_STEPS;
    private int currentStep = -1; // El paso actual (-1 para que el primero sea 0)

    private List<ChannelRackRowController> allRows = new ArrayList<>();

    /**
     * NUEVO MÉTODO:
     * Este método es llamado automáticamente por el FXMLLoader después
     * de que todos los componentes @FXML han sido inyectados.
     */
    @FXML
    public void initialize() {
        initializeSequencer();
    }

    private void initializeSequencer() {
        // beatDuration (negra) = 60000 / BPM
        double beatDurationMillis = 60000.0 / BPM;
        // stepDuration (semicorchea) = beatDuration / 4
        double stepDurationMillis = beatDurationMillis / 4.0; // Ej: 120 BPM -> 500ms / 4 = 125ms

        sequencerTimeline = new Timeline();
        sequencerTimeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame keyFrame = new KeyFrame(Duration.millis(stepDurationMillis), e -> {
            runSequencerStep();
        });

        sequencerTimeline.getKeyFrames().add(keyFrame);
    }

    /**
     * NUEVO: Se llama al pulsar "Añadir Pista".
     */
    @FXML
    private void handleNuevaPista(ActionEvent event) {
        Sample sampleToAdd = SampleBank.getInstance().getCurrentSample();

        if (sampleToAdd == null) {
            System.err.println("¡No hay sample guardado! Abre el sinte y guarda uno.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChannelRackRow.fxml"));

            // 1. Cargar el nodo raíz (el HBox)
            //    Esto también crea la instancia del ChannelRackRowController
            HBox newRowNode = loader.load();

            // 2. Obtener la instancia del controlador
            ChannelRackRowController rowController = loader.getController();

            // 3. Asignarle el sample
            rowController.setSample(sampleToAdd);

            // 4. Asignar la función de borrado (Callback/Lambda)
            //    Le decimos a la fila: "Cuando te borren, ejecuta ESTO:"
            rowController.setOnDelete(() -> {
                // Lógica que SÓLO el PrincipalController conoce:
                // a) Borrar el controlador de la lista lógica
                allRows.remove(rowController);
                // b) Borrar el nodo de la interfaz visual
                rackContainer.getChildren().remove(newRowNode);
            });
            // --- FIN DE LA MAGIA ---

            // 5. Guardar el controlador y añadir el nodo (como antes)
            allRows.add(rowController);
            rackContainer.getChildren().add(newRowNode);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar ChannelRackRow.fxml");
        }
    }

    /**
     * MODIFICADO: Esta es la nueva lógica del secuenciador
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

    /**
     * Se llama al pulsar el botón "Play" (btnEncenderRitmo).
     */
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