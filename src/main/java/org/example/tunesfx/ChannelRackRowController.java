package org.example.tunesfx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Arrays;
import java.util.List;

public class ChannelRackRowController {

    @FXML
    private Button deleteRowButton;
    @FXML
    private Label trackNameLabel;
    @FXML
    private Button stepBtn1, stepBtn2, stepBtn3, stepBtn4;
    @FXML
    private Button stepBtn5, stepBtn6, stepBtn7, stepBtn8;
    @FXML
    private Button stepBtn9, stepBtn10, stepBtn11, stepBtn12;
    @FXML
    private Button stepBtn13, stepBtn14, stepBtn15, stepBtn16;



    // Lista para acceder a los botones por índice
    private List<Button> stepButtons;

    // El sample que esta fila debe reproducir
    private Sample mySample;

    private Runnable deleteCallback;

    // Asumimos 16 pasos
    public static final int NUM_STEPS = 16;
    // (Cambia esto a 12 si usaste 12 botones)

    @FXML
    public void initialize() {
        // Agrupa los botones en una lista para fácil acceso
        stepButtons = Arrays.asList(
                stepBtn1, stepBtn2, stepBtn3, stepBtn4,
                stepBtn5, stepBtn6, stepBtn7, stepBtn8,
                stepBtn9, stepBtn10, stepBtn11, stepBtn12,
                stepBtn13, stepBtn14, stepBtn15, stepBtn16
        );
        // (Si usas 12, elimina los últimos 4 de esta lista)
    }

    /**
     * El PrincipalController usará esto para decirnos
     * QUÉ hacer cuando se pulse el botón de borrado.
     */
    public void setOnDelete(Runnable callback) {
        this.deleteCallback = callback;
    }
    // --- FIN ---

    // --- NUEVO MÉTODO HANDLER ---
    /**
     * Se llama cuando se pulsa el botón 'X' de esta fila.
     */
    @FXML
    private void handleDeleteRow() {
        if (deleteCallback != null) {
            deleteCallback.run();
        } else {
            System.err.println("Error: La función de borrado no fue configurada.");
        }
    }

    /**
     * El controlador principal llamará a esto para asignar un sample a esta fila.
     */
    public void setSample(Sample sample) {
        this.mySample = sample;
        // Opcional: poner un nombre
        // this.trackNameLabel.setText("Sample " + sample.hashCode());
    }

    /**
     * Devuelve el sample asignado a esta fila.
     */
    public Sample getSample() {
        return mySample;
    }

    /**
     * Comprueba si un paso específico está "encendido".
     */
    public boolean isStepOn(int step) {
        if (step < 0 || step >= stepButtons.size()) return false;
        return stepButtons.get(step).getStyleClass().contains("step-button-on");
    }

    /**
     * Resalta el botón del paso actual (playhead).
     */
    public void setPlayhead(int step) {
        if (step < 0 || step >= stepButtons.size()) return;
        stepButtons.get(step).getStyleClass().add("step-button-playhead");
    }

    /**
     * Limpia el resaltado del playhead del botón.
     */
    public void clearPlayhead(int step) {
        if (step < 0 || step >= stepButtons.size()) return;
        stepButtons.get(step).getStyleClass().remove("step-button-playhead");
    }

    /**
     * Se llama cuando se hace clic en CUALQUIER botón de paso de esta fila.
     */
    @FXML
    private void handleStepButtonToggle(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        ObservableList<String> styleClasses = clickedButton.getStyleClass();

        if (styleClasses.contains("step-button-on")) {
            styleClasses.remove("step-button-on");
        } else {
            styleClasses.add("step-button-on");
        }
    }
}