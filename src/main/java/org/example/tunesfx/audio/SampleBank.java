package org.example.tunesfx.audio;

import java.util.function.Consumer;

public class SampleBank {
    // 1. Instancia estática (Singleton)
    private static final SampleBank instance = new SampleBank();

    // 2. Constructor privado
    private SampleBank() {
    }

    // 3. Método de acceso público
    public static SampleBank getInstance() {
        return instance;
    }

    // 4. El sample guardado (tu "variable compartida")
    //    Empieza vacío (null), tal como sugeriste.
    private Sample currentSample = null;

    /**
     * Este es el "oyente". Será nuestro PrincipalController.
     * Consumer<Sample> es una función que "consume" un Sample.
     */
    private Consumer<Sample> onSampleSavedListener = null;

    /**
     * Método para que el PrincipalController se "registre"
     * y pueda escuchar.
     */
    public void setOnSampleSaved(Consumer<Sample> listener) {
        this.onSampleSavedListener = listener;
    }

    public Sample getCurrentSample() {
        return currentSample;
    }

    public void setCurrentSample(Sample sample) {
        this.currentSample = sample;
//        System.out.println("SampleBank: Nuevo sample almacenado.");

        // Si hay alguien escuchando (el PrincipalController)...
        if (onSampleSavedListener != null) {
            // ...avísale y envíale el nuevo sample
            onSampleSavedListener.accept(sample);
        }
    }
}
