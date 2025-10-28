package org.example.tunesfx;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.function.Function;

public class WaveViewer extends Canvas {

    private Oscilator[] oscillators;

    WaveViewer() {
        // El tamaño se define en el layout (Pane) o con setWidth/setHeight
        super(320, 320); // Ancho y alto
//         setStyle("-fx-border-color: white;"); // El borde se puede poner aquí o en el Pane
    }

    public void setOscillators(Oscilator[] oscillators) {
        this.oscillators = oscillators;
        draw(); // Dibuja al establecer
    }

    public void draw() {
        if (oscillators == null || oscillators.length == 0) {
            return;
        }

        GraphicsContext g = getGraphicsContext2D();

        // 1. Limpiar el canvas
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        // 2. Tu lógica de dibujo (casi idéntica)
        final int PAD = 25;
        int numSamples = (int) (getWidth() - PAD * 2);
        double[] mixedSamples = new double[numSamples];
        for (Oscilator oscillator : oscillators) {
            double[] samples = oscillator.getSampleWaveform(numSamples);
            for (int i = 0; i < samples.length; i++) {
                mixedSamples[i] += samples[i] / oscillators.length;
            }
        }

        int midY = (int) (getHeight() / 2);
        Function<Double, Integer> sampleToYCoord = sample -> (int) (midY + sample * (midY - PAD));

        g.setStroke(Color.WHITE);
        g.setLineWidth(1); // Grosor de línea

        g.strokeLine(PAD, midY, getWidth() - PAD, midY); // Eje X
        g.strokeLine(PAD, PAD, PAD, getHeight() - PAD); // Eje Y

        for (int i = 0; i < numSamples - 1; i++) { // Ajuste para evitar OutOfBounds
            int y1 = sampleToYCoord.apply(mixedSamples[i]);
            int y2 = sampleToYCoord.apply(mixedSamples[i + 1]);
            g.strokeLine(PAD + i, y1, PAD + i + 1, y2);
        }
    }
}