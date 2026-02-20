package org.example.tunesfx.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import org.example.tunesfx.synth.Sintetizador;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PianoRollController {

    @FXML private ScrollPane pianoScrollPane;
    @FXML private VBox pianoKeysContainer;
    @FXML private ScrollPane gridScrollPane;
    @FXML private Pane noteGrid;

    // Configuración
    private final int KEY_HEIGHT = 20;
    private final int BEAT_WIDTH = 40;
    private final int NUM_OCTAVES = 8;
    private final int TOTAL_KEYS = NUM_OCTAVES * 12;
    private final int NUM_BARS = 8;
    private final int BEATS_PER_BAR = 4;
    private double currentSnap = BEAT_WIDTH / 2.0;

    private final List<NoteBlock> activeNotes = new ArrayList<>();
    // Lista de notas que están sonando actualmente
    private final List<NoteBlock> playingNotes = new ArrayList<>();

    private Sintetizador synth;
    private Line playhead;
    private AnimationTimer playbackTimer;
    private boolean isPlaying = false;
    private double playheadPosition = 0;
    private double bpm = 120.0;

    @FXML
    public void initialize() {
        gridScrollPane.vvalueProperty().bindBidirectional(pianoScrollPane.vvalueProperty());
        construirInterfaz();
        inicializarPlayhead();
        noteGrid.setOnMousePressed(this::handleGridClick);
    }

    public void setSynth(Sintetizador synth) {
        this.synth = synth;
    }

    private void inicializarPlayhead() {
        playhead = new Line(0, 0, 0, TOTAL_KEYS * KEY_HEIGHT);
        playhead.setStroke(Color.ORANGE);
        playhead.setStrokeWidth(2);
        playhead.setMouseTransparent(true);
        playhead.setViewOrder(-100);
        noteGrid.getChildren().add(playhead);
    }

    private void construirInterfaz() {
        pianoKeysContainer.getChildren().clear();
        noteGrid.getChildren().clear();

        double totalHeight = TOTAL_KEYS * KEY_HEIGHT;
        double totalWidth = NUM_BARS * BEATS_PER_BAR * BEAT_WIDTH;

        pianoKeysContainer.setPrefHeight(totalHeight);
        noteGrid.setPrefWidth(totalWidth);
        noteGrid.setPrefHeight(totalHeight);

        String[] noteNames = {"B", "A#", "A", "G#", "G", "F#", "F", "E", "D#", "D", "C#", "C"};

        for (int i = 0; i < TOTAL_KEYS; i++) {
            int noteIndex = i % 12;
            int octave = NUM_OCTAVES - 1 - (i / 12);
            boolean isBlack = noteNames[noteIndex].contains("#");

            Pane key = new Pane();
            key.setPrefSize(60, KEY_HEIGHT);
            key.setStyle("-fx-background-color: " + (isBlack ? "#111" : "#eee") + ";" +
                    "-fx-border-color: #333; -fx-border-width: 0 0 1 0;");

            if (noteNames[noteIndex].equals("C")) {
                Label lbl = new Label("C" + octave);
                lbl.setTextFill(Color.GRAY);
                lbl.setStyle("-fx-font-size: 9px; -fx-padding: 0 0 0 30;");
                key.getChildren().add(lbl);
            }
            pianoKeysContainer.getChildren().add(key);

            if (isBlack) {
                Rectangle rowBg = new Rectangle(0, i * KEY_HEIGHT, totalWidth, KEY_HEIGHT);
                rowBg.setFill(Color.rgb(40, 40, 40));
                rowBg.setMouseTransparent(true);
                noteGrid.getChildren().add(rowBg);
            }
            Line hLine = new Line(0, i * KEY_HEIGHT, totalWidth, i * KEY_HEIGHT);
            hLine.setStroke(Color.rgb(60, 60, 60));
            hLine.setStrokeWidth(0.5);
            noteGrid.getChildren().add(hLine);
        }

        int totalBeats = NUM_BARS * BEATS_PER_BAR;
        for (int i = 0; i <= totalBeats; i++) {
            double x = i * BEAT_WIDTH;
            Line vLine = new Line(x, 0, x, totalHeight);
            if (i % 4 == 0) { vLine.setStroke(Color.rgb(100, 100, 100)); vLine.setStrokeWidth(1.0); }
            else { vLine.setStroke(Color.rgb(60, 60, 60)); vLine.setStrokeWidth(0.3); }
            noteGrid.getChildren().add(vLine);
        }
    }

    private void handleGridClick(MouseEvent e) {
        if (!e.isPrimaryButtonDown()) return;
        if (e.getTarget() instanceof Rectangle || (e.getTarget() instanceof Pane && ((Pane)e.getTarget()).getUserData() instanceof NoteBlock)) {
            return;
        }
        double snappedX = Math.floor(e.getX() / currentSnap) * currentSnap;
        double snappedY = Math.floor(e.getY() / KEY_HEIGHT) * KEY_HEIGHT;
        crearNota(snappedX, snappedY, currentSnap);
    }

    private void crearNota(double x, double y, double width) {
        NoteBlock block = new NoteBlock(x, y, width, KEY_HEIGHT);
        activeNotes.add(block);
        noteGrid.getChildren().add(block.root);
        if (synth != null) playNotePreview(y);
    }

    @FXML
    private void handlePlay() {
        if (isPlaying) return;
        double pixelsPerSecond = (bpm / 60.0) * BEAT_WIDTH;
        playbackTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) { lastUpdate = now; return; }
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                playheadPosition += (pixelsPerSecond * elapsedSeconds);
                playhead.setTranslateX(playheadPosition);
                checkNotesTrigger();
            }
        };
        playbackTimer.start();
        isPlaying = true;
    }

    @FXML
    private void handleStop() {
        if (playbackTimer != null) playbackTimer.stop();
        isPlaying = false;
        playheadPosition = 0;
        playhead.setTranslateX(0);
        if (synth != null) synth.allNotesOff();
        playingNotes.clear();
    }

    private void checkNotesTrigger() {
        if (synth == null) return;
        double currentX = playheadPosition;

        // Note ON
        for (NoteBlock note : activeNotes) {
            double start = note.root.getLayoutX();
            double end = start + note.rect.getWidth();
            if (currentX >= start && currentX < end) {
                if (!playingNotes.contains(note)) {
                    double freq = getFrequencyFromY(note.root.getLayoutY());
                    note.playingFrequency = freq; // GUARDAR FRECUENCIA
                    synth.noteOn(freq);
                    playingNotes.add(note);
                }
            }
        }

        // Note OFF
        Iterator<NoteBlock> it = playingNotes.iterator();
        while (it.hasNext()) {
            NoteBlock note = it.next();
            double end = note.root.getLayoutX() + note.rect.getWidth();
            if (currentX >= end || currentX < note.root.getLayoutX()) {
                if (note.playingFrequency > 0) {
                    synth.noteOff(note.playingFrequency); // USAR LA GUARDADA
                    note.playingFrequency = -1;
                }
                it.remove();
            }
        }
    }

    private double getFrequencyFromY(double yPos) {
        int keyIndex = (int) (yPos / KEY_HEIGHT);
        int invertedIndex = TOTAL_KEYS - 1 - keyIndex;
        int midiNote = 24 + invertedIndex;
        return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0);
    }

    private void playNotePreview(double yPos) {
        if (synth == null) return;

        double frequency = getFrequencyFromY(yPos);

        // 1. Encender nota
        synth.noteOn(frequency);

        // 2. Programar apagado seguro en 200ms
        // Usamos PauseTransition para mantenernos en el hilo de JavaFX y evitar desincronización
        PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(200));
        pause.setOnFinished(e -> {
            synth.noteOff(frequency);
        });
        pause.play();
    }

    // Clase interna para manejar las notas
    private class NoteBlock {
        Pane root;
        Rectangle rect;
        double startX, startY;
        boolean resizing = false;
        double initialMouseX, initialWidth, initialRectX;
        double playingFrequency = -1; // IMPORTANTE PARA EL BUG

        public NoteBlock(double x, double y, double w, double h) {
            this.startX = x; this.startY = y;
            rect = new Rectangle(w, h);
            rect.setFill(Color.rgb(0, 230, 118));
            rect.setStroke(Color.rgb(200, 255, 200));
            rect.setStrokeWidth(1);
            rect.setArcWidth(4); rect.setArcHeight(4);
            root = new Pane(rect);
            root.setLayoutX(x); root.setLayoutY(y);
            root.setUserData(this);

            root.setOnMouseMoved(e -> {
                if (e.getX() > rect.getWidth() - 10) root.setCursor(Cursor.E_RESIZE);
                else root.setCursor(Cursor.HAND);
            });

            root.setOnMousePressed(e -> {
                if (e.isSecondaryButtonDown()) {
                    noteGrid.getChildren().remove(root);
                    activeNotes.remove(this);
                    return;
                }
                initialMouseX = e.getSceneX();
                initialWidth = rect.getWidth();
                initialRectX = root.getLayoutX();
                resizing = (e.getX() > rect.getWidth() - 10);
                e.consume();
                if(synth != null) playNotePreview(root.getLayoutY());
            });

            root.setOnMouseDragged(e -> {
                double deltaX = e.getSceneX() - initialMouseX;
                if (resizing) {
                    double newWidth = initialWidth + deltaX;
                    newWidth = Math.round(newWidth / currentSnap) * currentSnap;
                    if (newWidth < currentSnap) newWidth = currentSnap;
                    rect.setWidth(newWidth);
                } else {
                    double newX = initialRectX + deltaX;
                    newX = Math.round(newX / currentSnap) * currentSnap;
                    if (newX < 0) newX = 0;
                    root.setLayoutX(newX);
                    startX = newX;
                }
            });
        }
    }
}