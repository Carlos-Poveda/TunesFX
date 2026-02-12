package org.example.tunesfx.controller;

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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tunesfx.audio.PlaylistItem;
import org.example.tunesfx.utils.AudioExporter;
import org.example.tunesfx.utils.GlobalState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrincipalController {
    @FXML private TreeView libraryTreeView;
    @FXML private Spinner bpmSpinner;
    @FXML private Button btnStopSong;
    @FXML private Button btnPlaySong;
    @FXML private GridPane playlistGrid;
    @FXML private ListView patternListView; // lista de patterns
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
    private double previousPlayheadX = 0; // Para recordar dónde estábamos antes

    // Variables para el diseño de la rejilla
    private final int CELL_WIDTH = 40;   // Ancho de cada celda de tiempo (1 compás o beat)
    private final int TRACK_HEIGHT = 40; // Alto de cada pista
    private final int NUM_TRACKS = 40;   // Pistas iniciales
    private final int NUM_BARS = 400;    // Compases iniciales

    // Variables para la gestión del ratón en la Playlist
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double initialLayoutX;
    private double initialLayoutY;
    private boolean isResizing = false;
    private final double RESIZE_MARGIN = 10.0; // Píxeles del borde derecho para redimensionar

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
        setupPlaylist();
        enablePatternPainting();
        initializeSongTimeline();
        setupPatternListViewContextMenu();
        setupLibrary();
    }

    // Abrir Sintetizador
    @FXML
    private void handleOpenSynth(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tunesfx/SintetizadorView.fxml"));
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tunesfx/ChannelRackView.fxml"));
                Pane rackRoot = loader.load();
                ChannelRackController rackController = loader.getController();

                rackStage = new Stage();
                setWindowIcon(rackStage);
                Scene rackScene = new Scene(rackRoot);

                String css = this.getClass().getResource("/org/example/tunesfx/styles.css").toExternalForm();
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

    @FXML
    private void handlePlaySong() {
        if (songTimeline == null) initializeSongTimeline();

        if (songTimeline.getStatus() == Animation.Status.RUNNING) {
            songTimeline.pause();
        } else {
            songTimeline.play();
        }
    }

    @FXML
    private void handleStopSong() {
        if (songTimeline != null) {
            songTimeline.stop();
            currentPlayheadX = 0;
            previousPlayheadX = 0;
            playheadLine.setStartX(0);
            playheadLine.setEndX(0);
            btnPlaySong.setStyle("-fx-background-color: #424242;");
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
        playlistGridContent.setMinWidth(totalWidth);
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
            // Estilo alterno para facilitar la lectura
            String bg = (j % 2 == 0) ? "#262626" : "#232323";
            trackLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-background-color: " + bg + "; -fx-border-color: #1A1A1A; -fx-border-width: 0 0 1 0;");
            // Menú contextual para el track
            ContextMenu trackMenu = new ContextMenu();
            MenuItem renameItem = new MenuItem("Renombrar Pista");

            renameItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(trackLabel.getText());
                dialog.setTitle("Rename");
                dialog.setHeaderText(null);
                dialog.setContentText("New name:");
                dialog.setGraphic(null);
                try {
                    DialogPane dialogPane = dialog.getDialogPane();
                    String css = this.getClass().getResource("styles.css").toExternalForm();
                    dialogPane.getStylesheets().add(css);
                    dialogPane.getStyleClass().add("my-dialog");
                } catch (Exception ex) {
                }
                dialog.showAndWait().ifPresent(newName -> {
                    if (!newName.trim().isEmpty()) {
                        trackLabel.setText(newName);
                    }
                });
            });

            trackMenu.getItems().add(renameItem);
            trackLabel.setContextMenu(trackMenu);
            // Permitir renombrar también con DOBLE CLIC
            trackLabel.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    renameItem.fire();
                }
            });
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
//                    System.out.println("Selecciona primero un patrón de la lista de la izquierda.");
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
        // 1. Crear datos lógicos
        int startBar = (int) (x / CELL_WIDTH) + 1;
        int trackIndex = (int) (y / TRACK_HEIGHT);
        PlaylistItem item = new PlaylistItem(patternName, startBar, trackIndex);
        songData.add(item);

        // 2. Crear contenedor visual
        javafx.scene.layout.StackPane clipContainer = new javafx.scene.layout.StackPane();
        clipContainer.setPrefSize(CELL_WIDTH, TRACK_HEIGHT);
        clipContainer.setLayoutX(x);
        clipContainer.setLayoutY(y);
        // Guardamos la referencia al objeto de datos
        clipContainer.setUserData(item);
        // Estilo del bloque de sonido
        clipContainer.setStyle(
                "-fx-background-color: #4a4a4a;" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-color: #666666;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" // Cursor de mano por defecto
        );
        // Etiqueta
        Label nameLabel = new Label(patternName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        nameLabel.setMouseTransparent(true);
        clipContainer.getChildren().add(nameLabel);

        // =============================================================
        // LÓGICA DE INTERACCIÓN (MOUSE EVENTS)
        // =============================================================

        // --- 1. MOUSE MOVED (Cambiar cursor) ---
        clipContainer.setOnMouseMoved(e -> {
            // Si estamos cerca del borde derecho, cambiamos cursor a redimensionar
            if (e.getX() > clipContainer.getWidth() - RESIZE_MARGIN) {
                clipContainer.setCursor(javafx.scene.Cursor.H_RESIZE);
            } else {
                clipContainer.setCursor(javafx.scene.Cursor.HAND);
            }
        });
        // --- 2. MOUSE PRESSED (Iniciar acción) ---
        clipContainer.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                // Guardamos posición inicial
                mouseAnchorX = e.getSceneX();
                mouseAnchorY = e.getSceneY();
                initialLayoutX = clipContainer.getLayoutX();
                initialLayoutY = clipContainer.getLayoutY();
                // Detectar si vamos a redimensionar o a mover
                isResizing = (e.getX() > clipContainer.getWidth() - RESIZE_MARGIN);
                // Traer al frente para que no quede detrás de otros bloques al mover
                clipContainer.toFront();
                e.consume();
            }
        });
        // --- 3. MOUSE DRAGGED (Arrastrar) ---
        clipContainer.setOnMouseDragged(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (isResizing) {
                    // MODO REDIMENSIONAR
                    // Calculamos nuevo ancho basado en la posición del ratón dentro del nodo
                    double newWidth = e.getX();
                    // Mínimo 1 celda de ancho
                    if (newWidth < CELL_WIDTH) newWidth = CELL_WIDTH;
                    clipContainer.setPrefWidth(newWidth);
                } else {
                    // MODO MOVER
                    double dragX = e.getSceneX() - mouseAnchorX;
                    double dragY = e.getSceneY() - mouseAnchorY;

                    clipContainer.setLayoutX(initialLayoutX + dragX);
                    clipContainer.setLayoutY(initialLayoutY + dragY);
                }
                e.consume();
            }
        });
        // --- 4. MOUSE RELEASED (Soltar y Ajustar a Rejilla - SNAP) ---
        clipContainer.setOnMouseReleased(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (isResizing) {
                    // Ajustar ANCHO a la rejilla
                    double currentWidth = clipContainer.getPrefWidth();
                    // Redondear al múltiplo de CELL_WIDTH más cercano
                    int cols = (int) Math.round(currentWidth / CELL_WIDTH);
                    if (cols < 1) cols = 1; // Mínimo 1 compás

                    double snappedWidth = cols * CELL_WIDTH;
                    clipContainer.setPrefWidth(snappedWidth);
                    // OPCIONAL: Aquí se podría guardar la duración en 'item' si tuvieras esa propiedad
                    // item.setDurationBars(cols);

                } else {
                    // Ajustar POSICIÓN a la rejilla
                    double rawX = clipContainer.getLayoutX();
                    double rawY = clipContainer.getLayoutY();
                    // Snap X (Tiempo)
                    int colIndex = (int) Math.round(rawX / CELL_WIDTH);
                    if (colIndex < 0) colIndex = 0;
                    double snappedX = colIndex * CELL_WIDTH;
                    // Snap Y (Track)
                    int rowIndex = (int) Math.round(rawY / TRACK_HEIGHT);
                    if (rowIndex < 0) rowIndex = 0;
                    if (rowIndex >= NUM_TRACKS) rowIndex = NUM_TRACKS - 1;
                    double snappedY = rowIndex * TRACK_HEIGHT;
                    // Aplicar posición ajustada
                    clipContainer.setLayoutX(snappedX);
                    clipContainer.setLayoutY(snappedY);
                    // ACTUALIZAR DATOS LÓGICOS
                    // Importante para que el cursor de reproducción sepa dónde está ahora
                    item.setStartBar(colIndex + 1);
                    item.setTrackIndex(rowIndex);
                    // Feedback visual de "encaje"
//                    System.out.println("Bloque movido a: Bar " + (colIndex + 1) + ", Track " + (rowIndex + 1));
                }
            }
        });
        // --- 5. CLICK DERECHO (Borrar) ---
        // Usamos Filter para capturarlo antes que otros eventos si fuera necesario
        clipContainer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                // Borrar de la vista
                playlistGridContent.getChildren().remove(clipContainer);
                // Borrar de la memoria
                songData.remove(item);
                e.consume();
            }
        });
        // Añadir al lienzo
        playlistGridContent.getChildren().add(clipContainer);
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
        // 1. Guardar posición ANTERIOR
        double oldX = currentPlayheadX;
        // 2. Calcular NUEVA posición
        // (BPM / 60) * CELL_WIDTH = Píxeles por segundo
        // Dividimos entre 50 porque corremos a 50Hz (20ms)
        double pixelsPerTick = ((bpm / 60.0) * CELL_WIDTH) / 50.0;

        currentPlayheadX += pixelsPerTick;
        // 3. DETECCIÓN DE COLISIÓN (Trigger)
        // Buscamos items que empiecen justo en el tramo que acabamos de recorrer [oldX, currentPlayheadX)
        checkCollisions(oldX, currentPlayheadX);
        // 4. Loop (Vuelta al principio)
        if (currentPlayheadX >= NUM_BARS * CELL_WIDTH) {
            currentPlayheadX = 0;
            previousPlayheadX = 0; // Resetear para evitar disparos falsos
        }
        // 5. Actualizar visual
        playheadLine.setStartX(currentPlayheadX);
        playheadLine.setEndX(currentPlayheadX);
    }

    private void checkCollisions(double oldX, double newX) {
        // Recorremos todos los bloques de la canción
        for (PlaylistItem item : songData) {
            // Calculamos el píxel exacto donde empieza este bloque
            // (startBar - 1) porque visualmente el compás 1 está en x=0
            double itemStartX = (item.getStartBar() - 1) * CELL_WIDTH;

            // ¿El inicio del bloque está dentro del tramo que hemos recorrido?
            // Es decir: oldX <= inicio < newX
            if (itemStartX >= oldX && itemStartX < newX) {
                triggerPattern(item);
            }
        }
    }

    private void triggerPattern(PlaylistItem item) {
        // Pedimos al Channel Rack que suene
        ChannelRackController rack = GlobalState.getChannelRackController();
        if (rack != null) {
            // Sonido
            rack.playSoundByName(item.getPatternName());
        }
    }
    // Menú contextual de la lista de patterns
    private void setupPatternListViewContextMenu() {
        patternListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Eliminar Patrón");
            deleteItem.setStyle("-fx-text-fill: #ff5555;");
            deleteItem.setOnAction(event -> {
                String itemToRemove = cell.getItem();
                if (itemToRemove != null) {
                    patternListView.getItems().remove(itemToRemove);
                }
            });

            contextMenu.getItems().add(deleteItem);
            cell.textProperty().bind(cell.itemProperty());
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });
    }
    // Mét0do para exportar a wav
    @FXML
    private void handleExportWav(ActionEvent event) {
        // 1. Abrir diálogo para elegir dónde guardar
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export song");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Archivo WAV", "*.wav")
        );
        // Nombre por defecto
        fileChooser.setInitialFileName("my_hydra_project.wav");

        File file = fileChooser.showSaveDialog(btnPlaySong.getScene().getWindow());

        if (file != null) {
            double bpm = GlobalState.getBpm();
            ChannelRackController rack = GlobalState.getChannelRackController(); // Obtenemos el rack
            new Thread(() -> {
                try {
                    AudioExporter.exportSong(file, songData, bpm, rack);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText("Exported song to " + file.getAbsolutePath());
                        alert.setHeaderText("Your song has been succesfully exported.");
                        alert.setTitle("Successful export");
                        alert.getDialogPane().setGraphic(null);
                        alert.showAndWait();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText("An error has occurred while exporting your song");
                        alert.setHeaderText("Exporting error.");
                        alert.setTitle("Error");
                        alert.getDialogPane().setGraphic(null);
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    // Librería de samples

    private void setupLibrary() {
        // 1. Ruta Raíz
        File rootDir = new File("src/main/resources/samples");

        // 2. Crear el nodo raíz del árbol (invisible si showRoot es false)
        TreeItem<File> rootItem = new TreeItem<>(rootDir);
        rootItem.setExpanded(true);

        // 3. Llenar el árbol recursivamente
        populateTree(rootDir, rootItem);

        // 4. Asignar al componente visual
        libraryTreeView.setRoot(rootItem);
        libraryTreeView.setShowRoot(false); // Ocultamos la carpeta madre para que se vean directo los contenidos

        // 5. PERSONALIZAR CÓMO SE VE CADA FILA (CellFactory)
        // Esto es vital: le decimos cómo dibujar un File (solo queremos el nombre)
        libraryTreeView.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName()); // Mostramos solo el nombre (ej: "kick.wav")

                    // Opcional: Añadir iconos simples si es carpeta o archivo
                    // Si tienes imágenes, puedes usar: setGraphic(new ImageView(...));
                    if (item.isDirectory()) {
                        setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;"); // Carpetas en blanco negrita
                    } else {
                        setStyle("-fx-text-fill: #aaaaaa;"); // Archivos en gris
                    }
                }
            }
        });

        // 6. Evento de Doble Clic
        libraryTreeView.setOnMouseClicked(this::handleLibraryClick);
    }

    // MÉTODO RECURSIVO: La clave para leer subcarpetas infinitas
    private void populateTree(File dir, TreeItem<File> parentItem) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                // Filtramos: Solo carpetas o archivos WAV
                boolean isWav = file.isFile() && file.getName().toLowerCase().endsWith(".wav");
                boolean isDir = file.isDirectory();

                if (isWav || isDir) {
                    TreeItem<File> item = new TreeItem<>(file);
                    parentItem.getChildren().add(item);

                    // SI ES UNA CARPETA, LLAMARSE A SÍ MISMO (RECURSIÓN)
                    if (isDir) {
                        populateTree(file, item);
                    }
                }
            }
        }
    }

    private void handleLibraryClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            // Obtenemos el item seleccionado
            TreeItem<File> selectedItem = (TreeItem<File>) libraryTreeView.getSelectionModel().getSelectedItem();

            // Verificamos que sea un archivo de audio válido (no una carpeta)
            if (selectedItem != null && selectedItem.getValue().isFile()) {
                File selectedFile = selectedItem.getValue();
                loadSampleToRack(selectedFile);
            }
        }
    }

    // Tu método loadSampleToRack modificado ligeramente para aceptar File directo
    private void loadSampleToRack(File sampleFile) {
        ChannelRackController rack = GlobalState.getChannelRackController();

        if (rack != null) {
            String fileName = sampleFile.getName();
            String trackName = fileName.replace(".wav", "");

            rack.addTrackFromLibrary(trackName, sampleFile);
            System.out.println("Sample añadido: " + trackName);
        }
    }

    private void setWindowIcon(Stage stage) {
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la ventana.");
        }
    }

    public void salir(ActionEvent actionEvent) {
        System.exit(0);
    }
}