package org.example.tunesfx.audio; // O el paquete que prefieras

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class AudioEngine {

    private static long device;
    private static long context;
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;

        // 1. Abrir el dispositivo de audio por defecto (Tarjeta de sonido)
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDeviceName);
        if (device == NULL) {
            throw new IllegalStateException("Fallo al abrir el dispositivo de audio OpenAL.");
        }

        // 2. Crear las capacidades del dispositivo
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

        // 3. Crear el contexto de OpenAL
        context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            throw new IllegalStateException("Fallo al crear el contexto OpenAL.");
        }

        // 4. Hacer que el contexto sea el actual para todo el programa
        alcMakeContextCurrent(context);

        // 5. Crear las capacidades de AL (Esto soluciona tu error)
        AL.createCapabilities(deviceCaps);

        isInitialized = true;
        System.out.println("Motor de audio OpenAL inicializado correctamente.");
    }

    public static void destroy() {
        if (!isInitialized) return;

        // Limpiar memoria y soltar la tarjeta de sonido
        alcMakeContextCurrent(NULL);
        if (context != NULL) {
            alcDestroyContext(context);
        }
        if (device != NULL) {
            alcCloseDevice(device);
        }
        isInitialized = false;
        System.out.println("Motor de audio OpenAL apagado.");
    }

    public static long getDevice() {
        return device;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }
}