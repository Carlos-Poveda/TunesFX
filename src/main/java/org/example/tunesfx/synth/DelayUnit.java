package org.example.tunesfx.synth;

public class DelayUnit {
    private double[] buffer;
    private int writeIndex = 0;
    private int bufferSize;

    // --- CAMBIO 1: Delay activo por defecto para evitar cortes si la UI no carga ---
    private boolean active = true;

    private double time = 0.5;
    private double feedback = 0.5;
    private double mix = 0.3;

    // Detección de silencio
    private int silenceSamples = 0;

    public DelayUnit() {
        this.bufferSize = Sintetizador.AudioInfo.SAMPLE_RATE * 2; // 2 segundos
        this.buffer = new double[bufferSize];
        this.silenceSamples = bufferSize;
    }

    public double process(double inputSample) {
        // Si está desactivado, pasamos el audio limpio pero NO cortamos la cadena
        if (!active) return inputSample;

        // 1. Leer del pasado
        int delaySamples = (int) (time * Sintetizador.AudioInfo.SAMPLE_RATE);
        int readIndex = writeIndex - delaySamples;
        if (readIndex < 0) readIndex += bufferSize;

        double delayedSample = buffer[readIndex];

        // 2. Feedback
        double feedbackSignal = delayedSample * Math.min(0.95, feedback);
        double signalToWrite = inputSample + feedbackSignal;

        // 3. Grabar en buffer
        buffer[writeIndex] = signalToWrite;

        // --- DETECCIÓN DE ACTIVIDAD ---
        // Umbral de 0.0001 para detectar colas muy suaves
        if (Math.abs(signalToWrite) > 0.0001 || Math.abs(delayedSample) > 0.0001) {
            silenceSamples = 0; // Hay sonido
        } else {
            silenceSamples++;   // Hay silencio
        }

        // Avanzar índice
        writeIndex++;
        if (writeIndex >= bufferSize) writeIndex = 0;

        // 4. Mezcla Wet/Dry
        return (inputSample * (1.0 - mix)) + (delayedSample * mix);
    }

    public boolean isAudioActive() {
        // Si está desactivado, decimos que "no hay eco", permitiendo al synth apagarse si quiere
        if (!active) return false;

        // Si el contador de silencio es menor que el buffer, es que aún hay eco sonando
        return silenceSamples < bufferSize;
    }

    public void reset() {
        for (int i = 0; i < bufferSize; i++) buffer[i] = 0;
        writeIndex = 0;
        silenceSamples = bufferSize;
    }

    // Setters
    public void setActive(boolean active) {
        this.active = active;
        if (!active) reset();
    }
    public boolean isActive() { return active; }
    public void setTime(double time) { this.time = Math.max(0.01, Math.min(2.0, time)); }
    public void setFeedback(double feedback) { this.feedback = Math.max(0.0, Math.min(0.95, feedback)); }
    public void setMix(double mix) { this.mix = Math.max(0.0, Math.min(1.0, mix)); }
}