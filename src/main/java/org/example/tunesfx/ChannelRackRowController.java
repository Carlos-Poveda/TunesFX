package org.example.tunesfx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.Arrays;
import java.util.List;

public class ChannelRackRowController {

    @FXML private Button deleteRowButton;
    @FXML private Label trackNameLabel;
    @FXML private Button stepBtn1, stepBtn2, stepBtn3, stepBtn4;
    @FXML private Button stepBtn5, stepBtn6, stepBtn7, stepBtn8;
    @FXML private Button stepBtn9, stepBtn10, stepBtn11, stepBtn12;
    @FXML private Button stepBtn13, stepBtn14, stepBtn15, stepBtn16;
    @FXML private Button stepBtn17, stepBtn18, stepBtn19, stepBtn20;
    @FXML private Button stepBtn21, stepBtn22, stepBtn23, stepBtn24;
    @FXML private Button stepBtn25, stepBtn26, stepBtn27, stepBtn28;
    @FXML private Button stepBtn29, stepBtn30, stepBtn31, stepBtn32;
    @FXML private Button stepBtn33, stepBtn34, stepBtn35, stepBtn36;
    @FXML private Button stepBtn37, stepBtn38, stepBtn39, stepBtn40;
    @FXML private Button stepBtn41, stepBtn42, stepBtn43, stepBtn44;
    @FXML private Button stepBtn45, stepBtn46, stepBtn47, stepBtn48;
    @FXML private Button stepBtn49, stepBtn50, stepBtn51, stepBtn52;
    @FXML private Button stepBtn53, stepBtn54, stepBtn55, stepBtn56;
    @FXML private Button stepBtn57, stepBtn58, stepBtn59, stepBtn60;
    @FXML private Button stepBtn61, stepBtn62, stepBtn63, stepBtn64;



    // Lista para acceder a los botones por índice
    private List<Button> stepButtons;

    // El sample que esta fila debe reproducir
    private Sample mySample;

    private Runnable deleteCallback;

    public static final int NUM_STEPS = 64;

    @FXML
    public void initialize() {
        stepButtons = Arrays.asList(
                stepBtn1, stepBtn2, stepBtn3, stepBtn4, stepBtn5, stepBtn6, stepBtn7, stepBtn8,
                stepBtn9, stepBtn10, stepBtn11, stepBtn12, stepBtn13, stepBtn14, stepBtn15, stepBtn16,
                stepBtn17, stepBtn18, stepBtn19, stepBtn20, stepBtn21, stepBtn22, stepBtn23, stepBtn24,
                stepBtn25, stepBtn26, stepBtn27, stepBtn28, stepBtn29, stepBtn30, stepBtn31, stepBtn32,
                stepBtn33, stepBtn34, stepBtn35, stepBtn36, stepBtn37, stepBtn38, stepBtn39, stepBtn40,
                stepBtn41, stepBtn42, stepBtn43, stepBtn44, stepBtn45, stepBtn46, stepBtn47, stepBtn48,
                stepBtn49, stepBtn50, stepBtn51, stepBtn52, stepBtn53, stepBtn54, stepBtn55, stepBtn56,
                stepBtn57, stepBtn58, stepBtn59, stepBtn60, stepBtn61, stepBtn62, stepBtn63, stepBtn64
        );

        for (Button btn : stepButtons) {
            // Asignar datos vacíos iniciales
            btn.setUserData(new StepData());

            // IMPORTANTE: Eliminar el onAction del FXML si puedes,
            // o dejarlo y usar este manejador de Mouse que es más potente.
            // Vamos a usar un manejador de Ratón manual:
            btn.setOnMouseClicked(this::handleMouseClick);
        }
    }

    private void handleMouseClick(MouseEvent event) {
        Button clickedButton = (Button) event.getSource();
        StepData data = (StepData) clickedButton.getUserData();

        if (event.getButton() == MouseButton.PRIMARY) {
            // --- CLICK IZQUIERDO: Encender/Apagar ---
            boolean newState = !data.isActive();
            data.setActive(newState);

            if (newState) {
                clickedButton.getStyleClass().add("step-button-on");
            } else {
                clickedButton.getStyleClass().remove("step-button-on");
            }

        } else if (event.getButton() == MouseButton.SECONDARY) {
            // --- CLICK DERECHO: Menú de Tono ---
            showPitchMenu(clickedButton, data, event.getScreenX(), event.getScreenY());
        }
    }

    private void showPitchMenu(Button btn, StepData data, double x, double y) {
        ContextMenu menu = new ContextMenu();

        // Opción: Resetear (Nota original)
        MenuItem resetItem = new MenuItem("Original");
        resetItem.setOnAction(e -> {
            data.setSemitoneOffset(0);
            updateButtonText(btn, 0);
        });
        menu.getItems().add(resetItem);
        menu.getItems().add(new SeparatorMenuItem());

        // Opciones: Rango de -12 a +12 semitonos
        // Puedes ajustar este rango según necesites
        for (int i = 12; i >= -12; i--) {
            if (i == 0) continue; // Ya tenemos el reset

            final int offset = i;
            String label = (offset > 0 ? "+" : "") + offset + " semitones";
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> {
                data.setSemitoneOffset(offset);
                updateButtonText(btn, offset);

                // Auto-activar el botón si cambiamos el tono
                if (!data.isActive()) {
                    data.setActive(true);
                    btn.getStyleClass().add("step-button-on");
                }
            });
            menu.getItems().add(item);
        }

        menu.show(btn, x, y);
    }

    // Pequeña ayuda visual: poner un numerito en el botón si tiene pitch
    private void updateButtonText(Button btn, int offset) {
        if (offset == 0) {
            btn.setText("");
        } else {
            btn.setText(String.valueOf(offset));
        }
    }

    /**
     * Método público para el PrincipalController.
     * Ya no miramos el estilo CSS, miramos el objeto de datos.
     */
    public StepData getStepData(int step) {
        if (step < 0 || step >= stepButtons.size()) return null;
        return (StepData) stepButtons.get(step).getUserData();
    }

    /**
     * El PrincipalController usará esto para decirnos
     * QUÉ hacer cuando se pulse el botón de borrado.
     */
    public void setOnDelete(Runnable callback) {
        this.deleteCallback = callback;
    }

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
//    @FXML
//    private void handleStepButtonToggle(ActionEvent event) {
//        Button clickedButton = (Button) event.getSource();
//        ObservableList<String> styleClasses = clickedButton.getStyleClass();
//
//        if (styleClasses.contains("step-button-on")) {
//            styleClasses.remove("step-button-on");
//        } else {
//            styleClasses.add("step-button-on");
//        }
//    }
}