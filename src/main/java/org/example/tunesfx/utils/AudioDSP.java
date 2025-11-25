package org.example.tunesfx.utils; // O tu paquete principal

public class AudioDSP {

    /**
     * Aplica Attack, Release y Volumen a un array de audio crudo.
     * @param originalData Los datos originales (short[])
     * @param attackFactor 0.0 a 1.0 (Porcentaje del sample dedicado al ataque)
     * @param releaseFactor 0.0 a 1.0 (Porcentaje del sample dedicado al release)
     * @param volume 0.0 a 1.0
     * @return Un NUEVO array procesado.
     */
    public static short[] applyEnvelope(short[] originalData, double attackFactor, double releaseFactor, double volume) {
        int length = originalData.length;
        short[] processed = new short[length];

        // Definir longitudes en muestras
        // Limitamos el ataque y release para que no se solapen extrañamente si suman > 100%
        int attackSamples = (int) (length * attackFactor);
        int releaseSamples = (int) (length * releaseFactor);

        // Evitar que attack + release sea mayor que la longitud total
        if (attackSamples + releaseSamples > length) {
            // Ajuste simple: comprimirlos proporcionalmente
            double factor = (double) length / (attackSamples + releaseSamples);
            attackSamples *= factor;
            releaseSamples *= factor;
        }

        int releaseStartIndex = length - releaseSamples;

        for (int i = 0; i < length; i++) {
            double envelope = 1.0;

            // 1. Lógica de Attack (Fade In)
            if (i < attackSamples) {
                envelope = (double) i / attackSamples;
            }
            // 2. Lógica de Release (Fade Out)
            else if (i >= releaseStartIndex) {
                int samplesIntoRelease = i - releaseStartIndex;
                envelope = 1.0 - ((double) samplesIntoRelease / releaseSamples);
            }

            // 3. Aplicar Volumen y Envolvente
            // Multiplicamos: Sample * Volumen * Envolvente
            double val = originalData[i] * volume * envelope;

            // Clamping (asegurar que no nos pasamos del límite de short)
            if (val > Short.MAX_VALUE) val = Short.MAX_VALUE;
            if (val < Short.MIN_VALUE) val = Short.MIN_VALUE;

            processed[i] = (short) val;
        }

        return processed;
    }
}