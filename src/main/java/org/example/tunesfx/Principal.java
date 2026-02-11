package org.example.tunesfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.tunesfx.audio.Audio;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
//        Font myFont = Font.loadFont(getClass().getResourceAsStream("/fonts/iceberg-regular.ttf"), 14);
//        Font myFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Handjet-VariableFont_ELGR,ELSH,wrght.ttf"), 14);

        // 1. Cargar el FXML de la vista principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PrincipalView.fxml"));
        Pane root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // 2. Configurar el Stage
        try {
            Image applicationIcon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            if (applicationIcon.isError()) {
                throw new Exception("La imagen del ícono no pudo ser cargada o es inválida.");
            }

            primaryStage.getIcons().add(applicationIcon);

        } catch (Exception e) {
            // Es buena práctica actualizar el mensaje de error para reflejar el nombre del archivo
            System.err.println("Error al cargar el ícono: images/logo.png. Usando ícono por defecto.");
            e.printStackTrace();
        }
        primaryStage.setTitle("TunesFX");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        Audio.shutdownOpenAL();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
