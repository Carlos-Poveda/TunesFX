package org.example.tunesfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;

public class PrincipalController {
    @FXML
    private Button openSynthButton;
    @FXML
    private HBox panelRitmo;
    @FXML
    private Button btnRitmo1;
    @FXML
    private Button salirButton;
    @FXML
    private Button btnEncenderRitmo;

    private Timeline sequencerTimeline;
    private boolean isPlaying = false;
    private static final double BPM = 120.0;

    /**
     * NUEVO MÉTODO:
     * Este método es llamado automáticamente por el FXMLLoader después
     * de que todos los componentes @FXML han sido inyectados.
     */
    @FXML
    public void initialize() {
        // Inicializamos nuestro secuenciador
        initializeSequencer();
    }

    /**
     * NUEVO MÉTODO:
     * Configura el Timeline pero no lo inicia.
     */
    private void initializeSequencer() {
        // Calculamos la duración de cada "beat" (golpe)
        // 120 BPM = 2 beats por segundo = 500ms por beat
        // Fórmula: 60,000 milisegundos / BPM
        double beatDurationMillis = 60000.0 / BPM;

        // 1. Crear la Timeline
        sequencerTimeline = new Timeline();
        sequencerTimeline.setCycleCount(Animation.INDEFINITE); // Bucle infinito

        // 2. Crear el KeyFrame (la acción que se repite)
        KeyFrame keyFrame = new KeyFrame(Duration.millis(beatDurationMillis), e -> {
            // Esta es la acción que se ejecuta en cada "beat"
            runSequencerStep();
        });

        // 3. Añadir el KeyFrame a la Timeline
        sequencerTimeline.getKeyFrames().add(keyFrame);
    }

    /**
     * NUEVO MÉTODO:
     * La lógica que se ejecuta en cada paso del bucle.
     */
    private void runSequencerStep() {
        // 1. Coger el sample del banco
        Sample sampleToPlay = SampleBank.getInstance().getCurrentSample();

        if (sampleToPlay != null) {
            // 2. Si existe, reproducirlo
            System.out.println("Sequencer Step! Reproduciendo sample.");
            Audio.getInstance().playSample(sampleToPlay);
        } else {
            // 3. Si no existe (ej. el usuario nunca guardó uno),
            // detenemos el bucle para evitar errores.
            System.out.println("Sequencer: No hay sample. Deteniendo bucle.");
            sequencerTimeline.stop();
            isPlaying = false;
            btnEncenderRitmo.setText("Play");
        }
    }

    /**
     * Se llama al pulsar el botón "Play" (btnEncenderRitmo).
     */
    @FXML
    private void handlePlayRitmo(ActionEvent event) {
        if (isPlaying) {
            // --- Estaba sonando -> PARAR ---
            sequencerTimeline.stop();
            btnEncenderRitmo.setText("Play");
            isPlaying = false;
            System.out.println("Sequencer Detenido.");

        } else {
            // --- Estaba parado -> INICIAR ---

            // Verificación previa: ¿Hay un sample?
            Sample currentSample = SampleBank.getInstance().getCurrentSample();
            if (currentSample == null) {
                System.out.println("PrincipalController: No hay sample. Abre el sinte y guarda uno.");
                // Opcional: podrías hacer que el botón parpadee en rojo un momento
                return; // No iniciar si no hay sample
            }

            // Hay sample, así que iniciamos el bucle
            sequencerTimeline.play();
            btnEncenderRitmo.setText("Stop");
            isPlaying = true;
            System.out.println("Sequencer Iniciado...");
        }
    }

    @FXML
    private void handleSalir(ActionEvent event) {
        // MODIFICADO: No llames a System.exit(0) directamente.
        // Cierra el Stage para permitir que el método stop() de Principal se ejecute.
        Stage stage = (Stage) salirButton.getScene().getWindow();
        stage.close();
        // System.exit(0); // <-- Eliminar esto
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

            //    Llamar al método shutdown del controlador al cerrar la ventana
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