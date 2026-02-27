package org.example.tunesfx.synth;

/**
 * Emulación de Filtro Moog Ladder (4-pole, 24dB/oct).
 * Corregido para usar nombres de variables consistentes.
 */
public class Filter {
    public enum Tipo {
        LOW_PASS, HIGH_PASS, OFF
    }

    private Tipo tipo = Tipo.OFF;
    private double cutoff;

    // --- CORRECCIÓN: Nombre de variable unificado a español ---
    private double resonancia;

    // Variables de estado para los 4 polos del filtro Moog
    private double stage1, stage2, stage3, stage4;
    private double oldStage1, oldStage2, oldStage3;

    public Filter() {
        setTipo(Tipo.OFF);
        reset();
    }

    public void reset() {
        stage1 = stage2 = stage3 = stage4 = 0.0;
        oldStage1 = oldStage2 = oldStage3 = 0.0;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
        if (this.cutoff == 0) this.cutoff = 20000.0; // Abierto por defecto
    }

    public Tipo getTipo() { return tipo; }

    public void setFrecuenciaCorte(double frecuencia) {
        // Limitamos para evitar explosiones matemáticas
        this.cutoff = Math.max(20.0, Math.min(20000.0, frecuencia));
    }

    public void setResonancia(double resonancia) {
        // En el filtro Moog, la resonancia interna va de 0 a 4.0 (auto-oscilación)
        // Recibimos 0.0-1.0 desde el slider y lo mapeamos a 0.0-3.5
        this.resonancia = resonancia * 3.5;
    }

    // Getter para la UI (devuelve el valor normalizado 0-1 aproximado para el slider)
    public double getResonancia() {
        return this.resonancia / 3.5;
    }

    public double getFrecuenciaCorte() {
        return this.cutoff;
    }

    /**
     * Procesamiento de audio estilo Moog.
     */
    public double procesar(double input) {
        if (tipo == Tipo.OFF) return input;

        // 1. Calcular coeficientes basados en Cutoff y Sample Rate
        double f = (2.0 * cutoff) / Sintetizador.AudioInfo.SAMPLE_RATE;

        // Coeficientes de aproximación (k y p)
        double k = 3.6 * f - 1.6 * f * f - 1;
        double p = (k + 1) * 0.5;

        // Escalar la resonancia (feedback)
        double scale = Math.exp((1.0 - p) * 1.386249);

        // --- AQUÍ DABA EL ERROR ANTES, AHORA ESTÁ CORREGIDO ---
        double r = this.resonancia * scale;

        // 2. Loop de Feedback (La magia del Moog)
        double x = input - r * stage4;

        // 3. Procesar los 4 polos en cascada (Ladder)
        double t1 = x;  // Entrada etapa 1

        stage1 += f * (Math.tanh(t1) - Math.tanh(stage1));
        stage2 += f * (Math.tanh(stage1) - Math.tanh(stage2));
        stage3 += f * (Math.tanh(stage2) - Math.tanh(stage3));
        stage4 += f * (Math.tanh(stage3) - Math.tanh(stage4));

        // 4. Salida
        double output = stage4;

        // Si queremos High Pass, restamos la salida Low Pass de la entrada original
        if (tipo == Tipo.HIGH_PASS) {
            output = input - stage4;
        }

        return output;
    }
}