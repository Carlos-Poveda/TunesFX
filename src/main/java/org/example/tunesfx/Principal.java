package org.example.tunesfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader; // Importar
import javafx.scene.Scene;
import javafx.scene.layout.Pane; // Importar
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception { // Añadir Exception
        // 1. Cargar el FXML de la vista principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PrincipalView.fxml"));
        Pane root = loader.load();

        // 2. Configurar el Stage
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(new Scene(root));

        // Quita setMaximized(true) si quieres usar el tamaño del FXML
        // primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

//    private void openSynthesizerWindow() {
//        try {
//            Sintetizador sintetizadorLogic = new Sintetizador();
//
//            Pane synthRoot = new Pane();
//            synthRoot.setPrefSize(800, 350);
//            synthRoot.setStyle("-fx-background-color: black;");
//
//            // Componentes
//            Oscilator[] oscillatorsFX = sintetizadorLogic.getOscillatorsFX();
//            for (int i = 0; i < oscillatorsFX.length; i++) {
//                Oscilator oscilatorFX = oscillatorsFX[i];
//                oscilatorFX.setLayoutX(15);
//                oscilatorFX.setLayoutY(10 + (i * 65));
//                synthRoot.getChildren().add(oscilatorFX);
//            }
//
//            WaveViewer waveViewerFX = sintetizadorLogic.getWaveViewerFX();
//            waveViewerFX.setLayoutX(440);
//            waveViewerFX.setLayoutY(10);
//            synthRoot.getChildren().add(waveViewerFX);
//
//            // --- 3. Configurar el nuevo Stage (la ventana del Sintetizador) ---
//            Stage synthStage = new Stage();
//            Scene synthScene = new Scene(synthRoot);
//
//            // --- 4. Configurar Listeners de teclado y cierre ---
//
//            // Teclado (usando la solución que ya implementamos)
//            synthScene.setOnKeyPressed(e -> {
//                String keyString = e.getText();
//                if (keyString.isEmpty()) {
//                    if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) {
//                        keyString = e.getCode().toString().toLowerCase();
//                    }
//                }
//                if (keyString.length() == 1) {
//                    sintetizadorLogic.onKeyPressed(keyString.charAt(0));
//                }
//                e.consume();
//            });
//
//            synthScene.setOnKeyReleased(e -> {
//                sintetizadorLogic.onKeyReleased();
//            });
//
//            // Cierre de ventana (importante para cerrar el hilo de audio)
//            synthStage.setOnCloseRequest(e -> {
//                sintetizadorLogic.shutdownAudio();
//            });
//
//            // --- 5. Mostrar la ventana ---
//            synthStage.setTitle("Sintetizador");
//            synthStage.setScene(synthScene);
//            synthStage.setResizable(false);
//            synthStage.show();
//
//            // Enlaza la función de actualización
//            sintetizadorLogic.setUpdateCallback(waveViewerFX::draw);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
