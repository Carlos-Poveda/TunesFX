package org.example.tunesfx;

public class Sample {
    private final short[] data;

    public Sample(short[] data) {
        this.data = data;
    }

    public short[] getData() {
        return data;
    }

    public int getLength() {
        return (data != null) ? data.length : 0;
    }
}
