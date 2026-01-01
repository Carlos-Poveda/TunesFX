package org.example.tunesfx.audio;

import static org.lwjgl.openal.AL10.AL_NONE;

public class Sample {
    private final short[] data;
    // Guardamos el ID del buffer de OpenAL para no subirlo 100 veces
    private int openALBufferId = AL_NONE;

    public Sample(short[] data) {
        this.data = data;
    }

    public short[] getData() {
        return data;
    }

    public int getLength() {
        return (data != null) ? data.length : 0;
    }

    public int getOpenALBufferId() {
        return openALBufferId;
    }

    public void setOpenALBufferId(int openALBufferId) {
        this.openALBufferId = openALBufferId;
    }
}