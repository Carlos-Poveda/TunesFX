package org.example.tunesfx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import javafx.util.Duration;
import org.example.tunesfx.utils.GlobalState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrincipalController {
    @FXML private Spinner bpmSpinner;
    @FXML private Button btnStopSong;
    @FXML private Button btnPlaySong;
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
    private List<PlaylistItem> songData = new ArrayList<>();
    private Line playheadLine; // La línea visual del cursor
    private Timeline songTimeline; // El "reloj" maestro de la Playlist
    private double currentPlayheadX = 0; // Posición X actual del cursor

    // Variables para el diseño de la rejilla
    private final int CELL_WIDTH = 40;   // Ancho de cada celda de tiempo (1 compás o beat)
    private final int TRACK_HEIGHT = 40; // Alto de cada pista
    private final int NUM_TRACKS = 20;   // Pistas iniciales
    private final int NUM_BARS = 100;    // Compases iniciales


    @FXML
    public void initialize() {
        GlobalState.setPrincipalController(this);

        // Configurar el Spinner de BPM (30 a 300, inicial 120)
        SpinnerValueFactory<Double> bpmFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(30.0, 300.0, 120.0, 1.0);
        bpmSpinner.setValueFactory(bpmFactory);

        // Cuando el spinner cambie, actualizamos el GlobalState
        bpmSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            GlobalState.setBpm((Double) newVal);
        });

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

        // LLamar al método para pintar la playlist
        setupPlaylist();
        enablePatternPainting();
        initializeSongTimeline();
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
        // Limpiamos por si acaso
        timelineHeader.getChildren().clear();
        trackHeadersContainer.getChildren().clear();
        playlistGridContent.getChildren().clear();

        // Ajustar el tamaño total del lienzo de la rejilla
        double totalWidth = NUM_BARS * CELL_WIDTH;
        double totalHeight = NUM_TRACKS * TRACK_HEIGHT;
        playlistGridContent.setPrefSize(totalWidth, totalHeight);

        // --- 1. GENERAR TIMELINE (Eje X) ---
        for (int i = 1; i <= NUM_BARS; i++) {
            Label barLabel = new Label(String.valueOf(i));
            barLabel.setPrefWidth(CELL_WIDTH);
            barLabel.setMinWidth(CELL_WIDTH);
            barLabel.setAlignment(Pos.CENTER);
            // Estilo: Texto gris y borde derecho suave
            barLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px; -fx-border-color: #333333; -fx-border-width: 0 1 0 0;");

            timelineHeader.getChildren().add(barLabel);

            // Línea vertical en la rejilla (para cada compás)
            Line vLine = new Line(i * CELL_WIDTH, 0, i * CELL_WIDTH, totalHeight);
            vLine.setStroke(Color.web("#2A2A2A")); // Color de la rejilla vertical
            vLine.getStrokeDashArray().addAll(2d, 2d); // Opcional: línea punteada
            playlistGridContent.getChildren().add(vLine);
        }

        // --- 2. GENERAR TRACK HEADERS (Eje Y) ---
        for (int j = 0; j < NUM_TRACKS; j++) {
            // Etiqueta del Track
            Label trackLabel = new Label("Track " + (j + 1));
            trackLabel.setPrefHeight(TRACK_HEIGHT);
            trackLabel.setMinHeight(TRACK_HEIGHT);
            trackLabel.setMaxWidth(Double.MAX_VALUE);
            trackLabel.setPadding(new Insets(0, 0, 0, 10)); // Margen izquierdo
            trackLabel.setAlignment(Pos.CENTER_LEFT);

            // Estilo alterno para facilitar la lectura (como en Excel)
            String bg = (j % 2 == 0) ? "#262626" : "#232323";
            trackLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-background-color: " + bg + "; -fx-border-color: #1A1A1A; -fx-border-width: 0 0 1 0;");

            // Menú contextual para el track (Renombrar, Color, etc.)
            ContextMenu trackMenu = new ContextMenu();
            MenuItem renameItem = new MenuItem("Rename Track");
            trackMenu.getItems().add(renameItem);
            trackLabel.setContextMenu(trackMenu);

            trackHeadersContainer.getChildren().add(trackLabel);

            // Línea horizontal en la rejilla (separando pistas)
            Line hLine = new Line(0, (j + 1) * TRACK_HEIGHT, totalWidth, (j + 1) * TRACK_HEIGHT);
            hLine.setStroke(Color.web("#1A1A1A")); // Color de separación de pistas
            playlistGridContent.getChildren().add(hLine);
        }

        playheadLine = new Line(0, 0, 0, totalHeight);
        playheadLine.setStroke(Color.RED); // Color clásico de playhead
        playheadLine.setStrokeWidth(2);
        playheadLine.setOpacity(0.8);
        playheadLine.setMouseTransparent(true); // Para que no interfiera con los clics en la rejilla

        playlistGridContent.getChildren().add(playheadLine);
    }

    private void enablePatternPainting() {
        playlistGridContent.setOnMouseClicked(event -> {
            // Solo pintamos con clic izquierdo
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {

                // 1. Obtener el patrón seleccionado de la lista
                String selectedPattern = (String) patternListView.getSelectionModel().getSelectedItem();

                // Si no hay nada seleccionado en la lista, no hacemos nada
                if (selectedPattern == null) {
                    // Mostrar algo
                    System.out.println("Selecciona primero un patrón de la lista de la izquierda.");
                    return;
                }

                // 2. Calcular coordenadas ajustadas a la rejilla (Snap to Grid)
                // Dividimos la coordenada del ratón por el ancho/alto de celda, convertimos a entero (truca decimales)
                // y multiplicamos de nuevo.
                int colIndex = (int) (event.getX() / CELL_WIDTH);
                int rowIndex = (int) (event.getY() / TRACK_HEIGHT);

                double snapX = colIndex * CELL_WIDTH;
                double snapY = rowIndex * TRACK_HEIGHT;

                // 3. Crear y añadir el Clip visual
                createClip(selectedPattern, snapX, snapY);
            }
        });
    }

    private void createClip(String patternName, double x, double y) {
        int startBar = (int) (x / CELL_WIDTH) + 1; // +1 porque los compases empiezan en 1
        int trackIndex = (int) (y / TRACK_HEIGHT);

        PlaylistItem item = new PlaylistItem(patternName, startBar, trackIndex);
        songData.add(item);

        // Usamos un StackPane para poder poner texto encima de un rectángulo
        javafx.scene.layout.StackPane clipContainer = new javafx.scene.layout.StackPane();

        // Dimensiones del clip (por defecto 1 compás de ancho)
        clipContainer.setPrefSize(CELL_WIDTH, TRACK_HEIGHT);
        clipContainer.setLayoutX(x);
        clipContainer.setLayoutY(y);

        // Estilo visual del Clip (parecido a un bloque de audio)
        // Fondo coloreado, bordes redondeados, borde suave
        clipContainer.setStyle(
                "-fx-background-color: #4a4a4a;" +
                        "-fx-background-radius: 3;" +
                        "-fx-border-color: #666666;" +
                        "-fx-border-radius: 3;" +
                        "-fx-border-width: 1;"
        );

        // Etiqueta con el nombre (solo si cabe, o truncado)
        Label nameLabel = new Label(patternName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px;");
        nameLabel.setMouseTransparent(true); // Para que el clic pase a través del texto
        clipContainer.setUserData(item);
        clipContainer.getChildren().add(nameLabel);
        // --- LÓGICA DE BORRADO ---
        // Si hacemos clic derecho sobre el clip, se elimina
        clipContainer.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                // Borrar de la vista
                playlistGridContent.getChildren().remove(clipContainer);
                // Borrar de la memoria (usando el objeto guardado en UserData)
                songData.remove((PlaylistItem) clipContainer.getUserData());
                e.consume();
            }
        });

        // Añadir al lienzo
        playlistGridContent.getChildren().add(clipContainer);
    }

    @FXML
    private void handlePlaySong() {
        if (songTimeline == null) initializeSongTimeline();

        if (songTimeline.getStatus() == Animation.Status.RUNNING) {
            songTimeline.pause();
//            btnPlaySong.setStyle("-fx-background-color: #424242;"); // Color normal
        } else {
            songTimeline.play();
//            btnPlaySong.setStyle("-fx-background-color: #2ecc71;"); // Color verde activo
        }
    }

    @FXML
    private void handleStopSong() {
        if (songTimeline != null) {
            songTimeline.stop();
            currentPlayheadX = 0;
            playheadLine.setStartX(0);
            playheadLine.setEndX(0);
            btnPlaySong.setStyle("-fx-background-color: #424242;");
        }
    }

    private void initializeSongTimeline() {
        // Definimos la resolución: Queremos que el cursor se actualice suavemente.
        // Una actualización cada 20ms (50 FPS) es suficiente.
        songTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            updatePlayhead();
        }));
        songTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private void updatePlayhead() {
        double bpm = GlobalState.getBpm();

        // CÁLCULO DE VELOCIDAD:
        // 1 compás = CELL_WIDTH (40px)
        // Velocidad (px/seg) = (BPM / 60) * CELL_WIDTH
        // Como el Timeline corre cada 20ms (1/50 seg), dividimos entre 50:
        double pixelsPerTick = ((bpm / 60.0) * CELL_WIDTH) / 50.0;

        currentPlayheadX += pixelsPerTick;

        // Si llegamos al final de la rejilla, volvemos al principio (Loop)
        if (currentPlayheadX >= NUM_BARS * CELL_WIDTH) {
            currentPlayheadX = 0;
        }

        // Actualizar posición visual de la línea
        playheadLine.setStartX(currentPlayheadX);
        playheadLine.setEndX(currentPlayheadX);

        // OPCIONAL: Auto-scroll. Si el cursor sale de la vista, mover el ScrollPane
        ensurePlayheadVisible();
    }

    private void ensurePlayheadVisible() {
        double viewportWidth = playlistScrollPane.getViewportBounds().getWidth();
        double scrollHValue = playlistScrollPane.getHvalue();
        double contentWidth = playlistGridContent.getWidth();

        // Si quieres que la pantalla siga al cursor automáticamente,
        // aquí calcularíamos el scrollHValue. Por ahora lo dejamos manual para no marear.
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