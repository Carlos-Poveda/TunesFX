package org.example.tunesfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Cargar el FXML de la vista principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PrincipalView.fxml"));
        Pane root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());


        // 2. Configurar el Stage
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /**
     * NUEVO MÉTODO:
     * Sobrescribimos el método stop() de Application
     * para apagar OpenAL de forma segura.
     */
    @Override
    public void stop() throws Exception {
        Audio.shutdownOpenAL(); // <-- Llamar al apagado
        // System.exit(0) no es necesario aquí,
        // pero asegúrate de que todos los hilos 'daemon' se detengan.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
