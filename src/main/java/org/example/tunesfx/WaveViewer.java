package org.example.tunesfx;

import javafx.fxml.FXML; // Importar
import javafx.fxml.FXMLLoader; // Importar
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.IOException; // Importar
import java.util.function.Function;

public class WaveViewer extends StackPane {

    @FXML
    private Canvas canvas;
    private Oscilator[] oscillators;

    public WaveViewer() {
        // Cargar el FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("waveViewer.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setOscillators(Oscilator[] oscillators) {
        this.oscillators = oscillators;
        draw();
    }

    public void draw() {
        if (oscillators == null || oscillators.length == 0) {
            return;
        }

        // Usamos el GraphicsContext del Canvas interno
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Obtener dimensiones del Canvas
        final double width = canvas.getWidth();
        final double height = canvas.getHeight();

        // 1. Limpiar el canvas
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // 2. Lógica de dibujo (sin cambios de lógica)
        final int PAD = 25;
        int numSamples = (int) (width - PAD * 2);
        double[] mixedSamples = new double[numSamples];
        for (Oscilator oscillator : oscillators) {
            // Asegúrate de que el método getSampleWaveform exista y sea accesible
            double[] samples = oscillator.getSampleWaveform(numSamples);
            for (int i = 0; i < samples.length; i++) {
                mixedSamples[i] += samples[i] / oscillators.length;
            }
        }

        int midY = (int) (height / 2);
        Function<Double, Integer> sampleToYCoord = sample -> (int) (midY + sample * (midY - PAD));

        g.setStroke(Color.WHITE);
        g.setLineWidth(1);

        g.strokeLine(PAD, midY, width - PAD, midY); // Eje X
        g.strokeLine(PAD, PAD, PAD, height - PAD); // Eje Y

        for (int i = 0; i < numSamples - 1; i++) {
            int y1 = sampleToYCoord.apply(mixedSamples[i]);
            int y2 = sampleToYCoord.apply(mixedSamples[i + 1]);
            g.strokeLine(PAD + i, y1, PAD + i + 1, y2);
        }
    }
}
