package org.example.tunesfx.audio;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;

public class AudioDeviceManager {
    /**
     * Enumera los dispositivos de salida disponibles (ALC_ALL_DEVICES_SPECIFIER).
     */
    public static List enumerateOutputDevices() {
        List<String> devices = new ArrayList<>();
        try {
            String all = alcGetString(0, ALC_ALL_DEVICES_SPECIFIER);
            if (all != null && !all.isEmpty()) {
                int start = 0;
                for (int i = 0; i < all.length(); i++) {
                    if (all.charAt(i) == '\0') {
                        if (i > start) {
                            String dev = all.substring(start, i);
                            if (!dev.isEmpty()) devices.add(dev);
                        }
                        start = i + 1;
                    }
                }
                if (start < all.length()) {
                    String dev = all.substring(start);
                    if (!dev.isEmpty()) devices.add(dev);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return devices;
    }

    /**
     * Nombre del dispositivo por defecto actual.
     */
    public static String getCurrentDeviceName() {
        try {
            String current = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
            return current != null ? current : "";
        } catch (Throwable t) {
            return "";
        }
    }
}