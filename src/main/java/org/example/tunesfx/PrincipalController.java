package org.example.tunesfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class PrincipalController {

    // @FXML private Button openSynthButton; // No es necesario si solo usas onAction
    // @FXML private Button salirButton;     // No es necesario si solo usas onAction

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

    @FXML
    private void handleSalir(ActionEvent event) {
        System.exit(0);
    }
}