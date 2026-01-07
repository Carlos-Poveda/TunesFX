package org.example.tunesfx.utils;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.example.tunesfx.PrincipalController;

public class GlobalState {
    private static PrincipalController principalController;
    // Propiedad observable para el BPM
    private static final DoubleProperty bpm = new SimpleDoubleProperty(120.0);

    public static void setPrincipalController(PrincipalController pc) {
        principalController = pc;
    }

    public static PrincipalController getPrincipalController() {
        return principalController;
    }

    public static DoubleProperty bpmProperty() {
        return bpm;
    }

    public static double getBpm() {
        return bpm.get();
    }

    public static void setBpm(double value) {
        bpm.set(value);
    }
}