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
import org.example.tunesfx.Sample;
import org.example.tunesfx.SampleBank;
import org.example.tunesfx.SamplePlayer;

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

    /**
     * NUEVO MÉTODO:
     * Se llama al pulsar el botón "Play" (btnEncenderRitmo).
     */
    @FXML
    private void handlePlayRitmo(ActionEvent event) {
        // 1. Preguntar al banco si tiene un sample guardado
        Sample sampleToPlay = SampleBank.getInstance().getCurrentSample();

        if (sampleToPlay != null) {
            // 2. Si lo tiene, pedir al SamplePlayer que lo reproduzca
            System.out.println("PrincipalController: Reproduciendo sample...");
            SamplePlayer.playSample(sampleToPlay);
        } else {
            // 3. Si no, informar al usuario
            System.out.println("PrincipalController: No hay sample. Abre el sinte y guarda uno.");
            // (Opcional: podrías hacer que el botón parpadee en rojo)
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
}