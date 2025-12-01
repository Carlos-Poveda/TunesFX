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
    public static short[] applyEnvelope(short[] originalData, double attackFactor, double releaseFactor, double volume, double durationFactor) {
// 1. Calcular la longitud final del array
        int targetLength = (int) (originalData.length * durationFactor);
        // Aseguramos una duración mínima (ej. 10ms o 100 muestras) para evitar fallos
        if (targetLength < 100) targetLength = 100;
        if (targetLength > originalData.length) targetLength = originalData.length;

        short[] processed = new short[targetLength];

        // 2. Definir longitudes en muestras basadas en la LONGITUD OBJETIVO
        int attackSamples = (int) (targetLength * attackFactor);
        int releaseSamples = (int) (targetLength * releaseFactor);

        // Manejar superposición (reutilizando la lógica anterior)
        if (attackSamples + releaseSamples > targetLength) {
            double factor = (double) targetLength / (attackSamples + releaseSamples);
            attackSamples = (int)(attackSamples * factor);
            releaseSamples = (int)(releaseSamples * factor);
        }

        int releaseStartIndex = targetLength - releaseSamples;

        // 3. Aplicar envolvente y truncar
        for (int i = 0; i < targetLength; i++) { // Bucle hasta la longitud objetivo
            double envelope = 1.0;

            // Lógica de Attack (Fade In)
            if (i < attackSamples) {
                envelope = (double) i / attackSamples;
            }
            // Lógica de Release (Fade Out)
            else if (i >= releaseStartIndex) {
                int samplesIntoRelease = i - releaseStartIndex;
                envelope = 1.0 - ((double) samplesIntoRelease / releaseSamples);
            }

            // Aplicar Volumen, Envolvente y Truncamiento
            // Usamos originalData[i] pero solo hasta targetLength
            double val = originalData[i] * volume * envelope;

            // Clamping (asegurar que no nos pasamos del límite de short)
            if (val > Short.MAX_VALUE) val = Short.MAX_VALUE;
            if (val < Short.MIN_VALUE) val = Short.MIN_VALUE;

            processed[i] = (short) val;
        }

        return processed; // Devolvemos el array truncado
    }
}