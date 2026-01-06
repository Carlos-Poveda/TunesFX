package org.example.tunesfx;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.tunesfx.utils.GlobalState;

import java.io.IOException;

public class PrincipalController {
    @FXML private GridPane playlistGrid;
    @FXML private ListView patternListView;
    @FXML private ScrollPane playlistScrollPane;
    @FXML private Button openChannelRackButton;
    @FXML private Button btnSalir;
    @FXML private Button openSynthButton;
    @FXML private HBox timelineHeader;
    @FXML private VBox trackHeadersContainer;
    @FXML private Pane playlistGridContent;

    private Stage rackStage;
    private final int SNAP_X = 40; // Ancho de cada celda (puedes ajustarlo)
    private final int TRACK_HEIGHT = 60; // Alto de cada pista

    @FXML
    public void initialize() {
        GlobalState.setPrincipalController(this);

        // Shortcuts del teclado
        Platform.runLater(() -> {
            Scene scene = openSynthButton.getScene();

            // Atajo: CTRL + S para el Sintetizador
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_ANY),
                    () -> handleOpenSynth(null)
            );

            // Atajo: CTRL + R para el Channel Rack
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY),
                    () -> handleOpenChannelRack(null)
            );
        });
    setupPlaylist();
    }

    // Abrir Sintetizador
    @FXML
    private void handleOpenSynth(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SintetizadorView.fxml"));
            Pane synthRoot = loader.load();
            SintetizadorController synthController = loader.getController();

            Stage synthStage = new Stage();
            setWindowIcon(synthStage);
            Scene synthScene = new Scene(synthRoot);

            synthScene.setOnKeyPressed(synthController::handleKeyPressed);
            synthScene.setOnKeyReleased(synthController::handleKeyReleased);
            synthStage.setOnCloseRequest(e -> synthController.shutdown());

            synthStage.setTitle("Synth");
//            synthStage.initStyle(StageStyle.UNDECORATED);
            synthStage.setScene(synthScene);
            synthStage.setResizable(false);
            synthStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Abrir Channel Rack
    @FXML
    public void handleOpenChannelRack(ActionEvent actionEvent) {
        if (rackStage == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ChannelRackView.fxml"));
                Pane rackRoot = loader.load();
                ChannelRackController rackController = loader.getController();

                rackStage = new Stage();
                setWindowIcon(rackStage);
                Scene rackScene = new Scene(rackRoot);

                String css = this.getClass().getResource("styles.css").toExternalForm();
                rackScene.getStylesheets().add(css);

                rackStage.setOnCloseRequest(e -> rackController.shutdown());

                rackStage.setTitle("Channel Rack");
//                rackStage.initStyle(StageStyle.UNDECORATED);
                rackStage.setScene(rackScene);
                rackStage.setResizable(false);

                rackStage.setOnCloseRequest(e -> {
                    rackController.shutdown();
                    rackStage = null;
                });
                rackStage.show();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.err.println("No se pudo encontrar el archivo CSS. Revisa el nombre.");
                e.printStackTrace();
            }
        } else {
            rackStage.toFront();
            rackStage.requestFocus();
        }
    }

    public void addPatternFromSample(String patternName) {
        if (!patternListView.getItems().contains(patternName)) {
            patternListView.getItems().add(patternName);
        }
    }

    private void setupPlaylist() {
        // 1. Crear los Números de Compás (Timeline)
        for (int i = 1; i <= 50; i++) { // Por ejemplo, 50 compases
            Label measureLabel = new Label(String.valueOf(i));
            measureLabel.setMinWidth(SNAP_X);
            measureLabel.setStyle("-fx-text-fill: #888888; -fx-alignment: center; -fx-border-color: #121212; -fx-border-width: 0 1 0 0;");
            timelineHeader.getChildren().add(measureLabel);
        }

        // 2. Crear los nombres de las pistas (Track Headers)
        for (int i = 1; i <= 20; i++) { // 20 pistas iniciales
            Label trackLabel = new Label(" Track " + i);
            trackLabel.setMinHeight(TRACK_HEIGHT);
            trackLabel.setPrefWidth(120);
            trackLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-border-color: #121212; -fx-border-width: 0 0 1 0; -fx-background-color: #2D2D2D;");
            trackHeadersContainer.getChildren().add(trackLabel);

            // 3. Dibujar líneas horizontales en el Grid para separar pistas
            Line line = new Line(0, i * TRACK_HEIGHT, 2000, i * TRACK_HEIGHT);
            line.setStroke(Color.web("#121212"));
            playlistGridContent.getChildren().add(line);
        }

        // 4. Dibujar líneas verticales (Rejilla de tiempo)
        for (int i = 0; i <= 2000; i += SNAP_X) {
            Line vLine = new Line(i, 0, i, 2000);
            vLine.setStroke(Color.web("#222222", 0.5)); // Color suave para la rejilla
            playlistGridContent.getChildren().add(vLine);
        }
    }

    public void salir(ActionEvent actionEvent) {
        System.exit(0);
    }

    private void setWindowIcon(Stage stage) {
        try {
            // Sustituye "icono.png" por el nombre real de tu archivo en resources
            // Si está en una carpeta sería "/images/logo.png"
            Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la ventana.");
        }
    }
}