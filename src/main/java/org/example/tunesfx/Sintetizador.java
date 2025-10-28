package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;
import java.util.HashMap;

/**
 * Clase Sintetizador refactorizada para JavaFX.
 * Esta clase ya NO es un componente de UI (no extiende JFrame).
 * Actúa como un controlador que gestiona la lógica de audio
 * y los componentes de UI (que serán de JavaFX).
 */
public class Sintetizador {

    // --- Lógica de audio y estado (sin cambios) ---
    private static final HashMap<Character,Double> KEY_FREQUENCIES = new HashMap<>();
    private boolean shouldGenerate;
    private static final int NUM_OSCILLATORS = 5;

    private final Audio hiloAudio = new Audio(() -> {
        if (!shouldGenerate) {
            return null;
        }
        short[] s = new short[Audio.BUFFER_SIZE];
        for (int i = 0; i < Audio.BUFFER_SIZE; i++) {
            double d = nextSample();
            s[i] = (short) (Short.MAX_VALUE * d);
        }
        return s;
    });

    static {
        final char[] PIANO_KEYS = "-<zsxdcvgbhnjmq2w3er5t6y7ui9o0p".toCharArray();
        final int STARTING_NOTE = 46;
        for (int i = 0; i < PIANO_KEYS.length; i++) {
            int currentNote = STARTING_NOTE + i;
            KEY_FREQUENCIES.put(PIANO_KEYS[i], Utils.getKeyFrequency(currentNote));
        }
    }

    // --- Referencias a los componentes de UI de JavaFX ---

    // Estas clases (Oscilator y WaveViewer) deben ser refactorizadas
    // para ser componentes de JavaFX (ej. extends Pane, extends Canvas).
    private Oscilator[] oscillators;
    private WaveViewer waveViewer;

    // Callback para actualizar la UI desde la lógica
    private Runnable updateCallback;

    /**
     * Constructor refactorizado.
     * Ya no crea la ventana (JFrame), solo inicializa la lógica
     * y los componentes.
     */
    public Sintetizador() {
        // Inicializa los osciladores (versión JavaFX)
        oscillators = new Oscilator[NUM_OSCILLATORS];
        for (int i = 0; i < NUM_OSCILLATORS; i++) {
            // Pasamos un "callback" para que el oscilador pueda
            // notificar al sintetizador que debe actualizar el WaveViewer.
            oscillators[i] = new Oscilator(this::updateWaveviewer);
        }

        // Inicializa el WaveViewer (versión JavaFX)
        waveViewer = new WaveViewer();
        waveViewer.setOscillators(oscillators);
    }

    /**
     * Llama al callback de actualización de UI (que será implementado
     * en la clase Main de JavaFX para llamar a waveViewer.draw()).
     */
    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    /**
     * Método para que la clase Main (JavaFX Application)
     * establezca el callback de actualización.
     */
    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // --- Métodos de lógica de audio (sin cambios) ---

    public void setFrequency(double frequency) {
        for (Oscilator osc : oscillators) {
            osc.setKeyFrequency(frequency);
        }
    }

    public double nextSample() {
        double totalSample = 0;
        for (Oscilator osc : oscillators) {
            totalSample += osc.getNextSample();
        }
        return totalSample / NUM_OSCILLATORS;
    }

    // --- Métodos de control (reemplazan KeyAdapter y WindowListener) ---

    /**
     * Reemplaza la lógica de KeyAdapter.keyPressed.
     * Será llamado desde el listener setOnKeyPressed de la Scene de JavaFX.
     * @param keyChar El carácter presionado.
     */
    public void onKeyPressed(char keyChar) {
        if (!KEY_FREQUENCIES.containsKey(keyChar)) {
            return;
        }
        if (hiloAudio.isInitialized() && !hiloAudio.isRunning()) {
            setFrequency(KEY_FREQUENCIES.get(keyChar));
            shouldGenerate = true;
            hiloAudio.triggerPlayBack();
        }
    }

    /**
     * Reemplaza la lógica de KeyAdapter.keyReleased.
     * Será llamado desde el listener setOnKeyReleased de la Scene de JavaFX.
     */
    public void onKeyReleased() {
        shouldGenerate = false;
    }

    /**
     * Reemplaza la lógica de WindowAdapter.windowClosing.
     * Será llamado desde el listener setOnCloseRequest del Stage de JavaFX.
     */
    public void shutdownAudio() {
        hiloAudio.close();
    }

    // --- Getters para que la clase Main obtenga los nodos de UI ---

    public Oscilator[] getOscillatorsFX() {
        return oscillators;
    }

    public WaveViewer getWaveViewerFX() {
        return waveViewer;
    }

    // --- Clase interna de info (sin cambios) ---
    public static class AudioInfo {
        public static final int SAMPLE_RATE = 44100;
    }
}