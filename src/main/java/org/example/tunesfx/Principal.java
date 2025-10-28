package org.example.tunesfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) {
        // --- 1. Crear el layout principal (Toolbox) ---
        // Usamos Pane o StackPane como root si queremos posicionar hijos libremente
        Pane root = new Pane();
        primaryStage.setMaximized(true);
        root.setStyle("-fx-background-color: #252525;");

        // El Pane no gestiona el tamaño de sus hijos automáticamente,
        // así que el VBox de control solo ocupará el tamaño que le demos.

        // Panel de control (VBox para apilar los botones verticalmente)
        VBox panelDeControl = new VBox(10);
        panelDeControl.setAlignment(Pos.TOP_LEFT); // Alineación del contenido interno
        panelDeControl.setPadding(new Insets(15)); // Margen interno

        // Establecer el tamaño exacto del panel de control
        panelDeControl.setPrefWidth(320);
        panelDeControl.setPrefHeight(200); // Altura más razonable

        // Posicionamiento en la esquina superior izquierda (Layout X e Y)
        panelDeControl.setLayoutX(50); // 50px de margen desde la izquierda
        panelDeControl.setLayoutY(50); // 50px de margen desde arriba

        panelDeControl.setStyle("-fx-background-color: #1f2025; -fx-border-color: #ffffff; -fx-border-width: 1;");

        // --- 2. Crear y configurar botones ---
        Button openSynthButton = new Button("Abrir Sintetizador");
        openSynthButton.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: white;");
        openSynthButton.setMaxWidth(Double.MAX_VALUE); // Para que ocupe todo el ancho del VBox

        Button salirButton = new Button("Salir");
        salirButton.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: white;");
        salirButton.setMaxWidth(Double.MAX_VALUE); // Para que ocupe todo el ancho del VBox

        // Asignar las acciones
        openSynthButton.setOnAction(e -> openSynthesizerWindow());
        salirButton.setOnAction(e -> System.exit(0));

        // Añadir los botones al panel de control
        panelDeControl.getChildren().addAll(openSynthButton, salirButton);

        // Añadir el panel de control al root
        root.getChildren().add(panelDeControl);

        // --- 3. Configurar el Stage ---
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    // El Main y openSynthesizerWindow() permanecen igual...
    public static void main(String[] args) {
        launch(args);
    }

    private void openSynthesizerWindow() {
        try {
            Sintetizador sintetizadorLogic = new Sintetizador();

            Pane synthRoot = new Pane();
            synthRoot.setPrefSize(800, 350);
            synthRoot.setStyle("-fx-background-color: black;");

            // Componentes
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
            sintetizadorLogic.setUpdateCallback(waveViewerFX::draw);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
