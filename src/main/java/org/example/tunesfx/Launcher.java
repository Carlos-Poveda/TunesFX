package org.example.tunesfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane; // O VBox, BorderPane...
import javafx.stage.Stage;

public class Launcher extends Application {

    private Sintetizador sintetizadorLogic;

    @Override
    public void start(Stage primaryStage) throws Exception {

        sintetizadorLogic = new Sintetizador();

        Pane root = new Pane();
        root.setPrefSize(800, 350);
        root.setStyle("-fx-background-color: black;");

        Oscilator[] oscillatorsFX = sintetizadorLogic.getOscillatorsFX();
        for (int i = 0; i < oscillatorsFX.length; i++) {
            Oscilator oscilatorFX = oscillatorsFX[i];
            oscilatorFX.setLayoutX(15);
            oscilatorFX.setLayoutY(10 + (i * 65));
            root.getChildren().add(oscilatorFX);
        }

        WaveViewer waveViewerFX = sintetizadorLogic.getWaveViewerFX();
        waveViewerFX.setLayoutX(440); // Equivalente a setBounds
        waveViewerFX.setLayoutY(10); // Equivalente a setBounds

        root.getChildren().add(waveViewerFX);

        Scene scene = new Scene(root);

        scene.setOnKeyPressed(e -> {

            String keyString = e.getText();

            if (keyString.isEmpty()) {
                if (e.getCode().isLetterKey() || e.getCode().isDigitKey()) {
                    keyString = e.getCode().toString().toLowerCase();
                }
            }

            if (!keyString.isEmpty() && keyString.length() == 1) {
                sintetizadorLogic.onKeyPressed(keyString.charAt(0));
            }

            e.consume();
        });

        scene.setOnKeyReleased(e -> {
            sintetizadorLogic.onKeyReleased();
        });

        primaryStage.setTitle("Sintetizador");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        primaryStage.setOnCloseRequest(e -> {
            sintetizadorLogic.shutdownAudio();
        });

        primaryStage.show();

        sintetizadorLogic.setUpdateCallback(() -> waveViewerFX.draw());
    }

    public static void main(String[] args) {
        launch(args);
    }
}