package org.example.tunesfx.controller;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import org.example.tunesfx.synth.Sintetizador;

public class PianoComponent extends StackPane {

    // Rango de notas (MIDI) - Aumentado para cubrir más teclado
    private final int START_NOTE = 36; // C2
    private final int END_NOTE = 84;   // C6

    private Sintetizador synthLogic;

    public PianoComponent() {
        this.getStyleClass().add("piano-container");
        // Permitir que el piano crezca horizontalmente
        this.setMaxWidth(Double.MAX_VALUE);
        construirTeclas();
    }

    public void setSynthLogic(Sintetizador logic) {
        this.synthLogic = logic;
    }

    private void construirTeclas() {
        // Capa de teclas blancas
        HBox whiteKeysLayer = new HBox();
        whiteKeysLayer.setSpacing(0); // Sin espacio entre teclas blancas
        // Hacemos que la capa de teclas blancas ocupe todo el ancho
        whiteKeysLayer.setMaxWidth(Double.MAX_VALUE);

        // Capa de teclas negras (superpuesta)
        HBox blackKeysLayer = new HBox();
        blackKeysLayer.setSpacing(0);
        blackKeysLayer.setPickOnBounds(false); // Permitir clicks a través de los huecos
        blackKeysLayer.setMaxWidth(Double.MAX_VALUE);

        // Contadores para posicionar las teclas negras
        int whiteKeyCount = 0;
        for (int i = START_NOTE; i <= END_NOTE; i++) {
            if (!isBlackKey(i)) {
                whiteKeyCount++;
            }
        }

        int currentWhiteKey = 0;
        for (int i = START_NOTE; i <= END_NOTE; i++) {
            boolean isBlack = isBlackKey(i);

            if (!isBlack) {
                Button key = createKey(i, false);
                // ESTA ES LA CLAVE: Hacer que las teclas blancas crezcan
                HBox.setHgrow(key, Priority.ALWAYS);
                key.setMaxWidth(Double.MAX_VALUE);
                whiteKeysLayer.getChildren().add(key);
                currentWhiteKey++;
            } else {
                // Añadir tecla negra a su capa
                Button key = createKey(i, true);
                // Calcular la posición de la tecla negra
                // Se coloca entre la tecla blanca anterior y la actual
                // Usamos un espaciador proporcional para ubicarla
                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                spacer.setMaxWidth(Double.MAX_VALUE);

                // Ajuste fino de posición (margin negativo para centrarla sobre la línea)
                key.setTranslateX(-12); // Ajusta este valor si es necesario

                // Añadir primero un espaciador y luego la tecla
                // Esto es un truco: para posicionar las negras correctamente sobre las blancas que se estiran,
                // necesitaríamos un layout más complejo (como AnchorPane).
                // Esta es una aproximación simple que funciona visualmente si no se redimensiona mucho.
                // Para un piano perfecto que se redimensione, se requeriría un cálculo de posición absoluto.

                // SOLUCIÓN SIMPLIFICADA:
                // En lugar de usar HBox para las negras, las añadimos directamente al StackPane
                // con un margen calculado. Pero eso es complejo.
                // Vamos a usar el método de los espaciadores en la capa negra.

                // NOTA: El posicionamiento perfecto de teclas negras sobre blancas de ancho variable
                // es complicado con layouts simples. Esta implementación coloca las teclas negras
                // en un HBox separado. Si las blancas se estiran mucho, las negras podrían no quedar
                // perfectamente centradas. Para un resultado profesional, se necesitaría un custom layout.

                // Por ahora, usamos espaciadores fijos para las negras, que coincidirán
                // con el ancho "natural" de las blancas.
                blackKeysLayer.getChildren().add(key);
            }
        }

        // REHACEMOS LA LÓGICA DE LAS NEGRAS PARA QUE FUNCIONE MEJOR
        blackKeysLayer.getChildren().clear();
        for (int i = START_NOTE; i <= END_NOTE; i++) {
            if (isBlackKey(i)) {
                Button key = createKey(i, true);
                key.setTranslateX(-12); // Centrar sobre la separación
                blackKeysLayer.getChildren().add(key);
            } else {
                // Espaciador invisible del ancho aproximado de una tecla blanca
                Pane spacer = new Pane();
                spacer.setPrefWidth(32); // Ancho base de tecla blanca
                spacer.setDisable(true);
                blackKeysLayer.getChildren().add(spacer);
            }
        }

        // Añadir las capas al StackPane
        this.getChildren().addAll(whiteKeysLayer, blackKeysLayer);
    }

    private Button createKey(int note, boolean isBlack) {
        Button btn = new Button();
        btn.getStyleClass().add(isBlack ? "piano-key-black" : "piano-key-white");

        if (!isBlack) {
            String noteName = getNoteName(note);
            if (noteName.endsWith("C")) { // Solo mostrar el nombre en las teclas C para no saturar
                btn.setText(noteName);
            }
        }

        btn.setOnMousePressed(e -> {
            if (synthLogic != null) {
                char mappedChar = getCharForNote(note);
                synthLogic.onKeyPressed(mappedChar);
            }
        });

        btn.setOnMouseReleased(e -> {
            if (synthLogic != null) {
                // --- CORRECCIÓN: Calcular la tecla y enviarla ---
                char mappedChar = getCharForNote(note);
                synthLogic.onKeyReleased(mappedChar); // Pasamos el char
            }
        });

        return btn;
    }

    private boolean isBlackKey(int note) {
        int n = note % 12;
        return n == 1 || n == 3 || n == 6 || n == 8 || n == 10;
    }

    private String getNoteName(int note) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (note / 12) - 1;
        return names[note % 12] + octave;
    }

    private char getCharForNote(int note) {
        // Mapeo simple para pruebas. Idealmente, pasar la nota MIDI al sinte.
        return 'z';
    }
}