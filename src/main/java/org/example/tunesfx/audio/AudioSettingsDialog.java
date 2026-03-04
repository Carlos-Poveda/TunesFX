package org.example.tunesfx.audio;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AudioSettingsDialog {
    public static String showDialog(Stage owner) {
        List<String> devices = AudioDeviceManager.enumerateOutputDevices();
        ComboBox<String> combo = new ComboBox<>();
        combo.setItems(FXCollections.observableArrayList(devices));
        String current = AudioDeviceManager.getCurrentDeviceName();
        if (current != null && devices.contains(current)) combo.setValue(current);
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().addAll(new Label("Select an audio output device:"), combo);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Output device");
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().setContent(root);
        ButtonType applyBtn = new ButtonType("Aplicar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(applyBtn, cancelBtn);
        dialog.setResultConverter(bt -> {
            if (bt == applyBtn) return combo.getValue();
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}
