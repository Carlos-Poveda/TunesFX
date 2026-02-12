package org.example.tunesfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.tunesfx.audio.Audio;

import java.io.InputStream;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // Cargar fuente principal
        InputStream fontStream = getClass().getResourceAsStream("/fonts/RobotoMono-VariableFont_wght.ttf");
        if (fontStream != null) {
            Font myFont = Font.loadFont(fontStream, 14);
            if (myFont != null) {
                System.out.println("Main Font loaded: " + myFont.getFamily());
            } else {
                System.err.println("Failed to create main font from stream.");
            }
        } else {
            System.err.println("Failed to find main font file.");
        }

        // Cargar fuente para el título
        // Es necesario cargarla aquí para que el CSS pueda usarla
        InputStream titleFontStream = getClass().getResourceAsStream("/fonts/Sarina-Regular.ttf");
        if (titleFontStream != null) {
            Font titleFont = Font.loadFont(titleFontStream, 33); 
            if (titleFont != null) {
                System.out.println("Title Font loaded: " + titleFont.getFamily());
            } else {
                System.err.println("Failed to create title font from stream.");
            }
        } else {
            System.err.println("Failed to find title font file.");
        }


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
            System.err.println("Error al cargar el ícono: images/logo.png. Usando ícono por defecto.");
            e.printStackTrace();
        }
        primaryStage.setTitle("Hydra");
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
