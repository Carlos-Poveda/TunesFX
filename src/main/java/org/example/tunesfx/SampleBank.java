package org.example.tunesfx;

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

    public Sample getCurrentSample() {
        return currentSample;
    }

    public void setCurrentSample(Sample currentSample) {
        this.currentSample = currentSample;
        System.out.println("SampleBank: Nuevo sample almacenado.");
    }
}
