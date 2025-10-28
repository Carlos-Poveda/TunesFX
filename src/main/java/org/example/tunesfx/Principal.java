package org.example.tunesfx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) {
        // --- 1. Crear el layout principal (Toolbox) ---
        HBox root = new HBox(10);
        root.setAlignment(Pos.CENTER);
        primaryStage.setMaximized(true);
        root.setStyle("-fx-background-color: #252233;");

        // --- 2. Crear el botón para el Sintetizador ---
        Button openSynthButton = new Button("Abrir Sintetizador");
        openSynthButton.setStyle("-fx-background-color: #3f3f3f;-fx-text-fill: white;");

        Button salirButton = new Button("Salir");
        salirButton.setStyle("-fx-background-color: #3f3f3f;-fx-text-fill: white;");



        // Asignar la acción
        openSynthButton.setOnAction(e -> {
            // Llama al método que abre la ventana del sintetizador
            openSynthesizerWindow();
        });

        root.getChildren().add(openSynthButton);

        salirButton.setOnAction(e -> {
            System.exit(0);
        });

        root.getChildren().add(salirButton);

        // --- 3. Configurar el Stage (Ventana de la Caja de Herramientas) ---
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    // El Main ahora apunta a esta nueva clase
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Lógica para abrir la ventana del sintetizador.
     * Esta lógica estaba originalmente en el método start() de tu antiguo Launcher.
     */
    private void openSynthesizerWindow() {
        try {
            // Un SintetizadorLogic solo necesita ser creado una vez por ventana
            Sintetizador sintetizadorLogic = new Sintetizador();

            // --- 1. Configurar el layout del Sintetizador ---
            Pane synthRoot = new Pane();
            synthRoot.setPrefSize(800, 350);
            synthRoot.setStyle("-fx-background-color: black;");

            // --- 2. Añadir los componentes (Oscillators y WaveViewer) ---
            Oscilator[] oscillatorsFX = sintetizadorLogic.getOscillatorsFX();
            for (int i = 0; i < oscillatorsFX.length; i++) {
                Oscilator oscilatorFX = oscillatorsFX[i];
                oscilatorFX.setLayoutX(15);
                oscilatorFX.setLayoutY(10 + (i * 65));
                synthRoot.getChildren().add(oscilatorFX);
            }

            WaveViewer waveViewerFX = sintetizadorLogic.getWaveViewerFX();
            waveViewerFX.setLayoutX(440);
            waveViewerFX.setLayoutY(10);
            synthRoot.getChildren().add(waveViewerFX);

            // --- 3. Configurar el nuevo Stage (la ventana del Sintetizador) ---
            Stage synthStage = new Stage();
            Scene synthScene = new Scene(synthRoot);

            // --- 4. Configurar Listeners de teclado y cierre ---

            // Teclado (usando la solución que ya implementamos)
            synthScene.setOnKeyPressed(e -> {
                String keyString = e.getText();
                if (keyString.isEmpty()) {
                    if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) {
                        keyString = e.getCode().toString().toLowerCase();
                    }
                }
                if (keyString.length() == 1) {
                    sintetizadorLogic.onKeyPressed(keyString.charAt(0));
                }
                e.consume();
            });

            synthScene.setOnKeyReleased(e -> {
                sintetizadorLogic.onKeyReleased();
            });

            // Cierre de ventana (importante para cerrar el hilo de audio)
            synthStage.setOnCloseRequest(e -> {
                sintetizadorLogic.shutdownAudio();
            });

            // --- 5. Mostrar la ventana ---
            synthStage.setTitle("Sintetizador");
            synthStage.setScene(synthScene);
            synthStage.setResizable(false);
            synthStage.show();

            // Enlaza la función de actualización
            sintetizadorLogic.setUpdateCallback(() -> waveViewerFX.draw());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}