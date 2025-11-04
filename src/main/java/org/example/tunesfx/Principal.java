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

        // 2. Configurar el Stage
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
