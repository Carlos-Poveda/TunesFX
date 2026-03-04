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

    public static boolean switchDevice(String deviceName) {
        try {
            // Si no está inicializado, inicializamos con el nuevo dispositivo
            if (!isInitialized) {
                String target = (deviceName != null && !deviceName.isEmpty()) ? deviceName
                        : alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
                long newDevice = alcOpenDevice(target);
                if (newDevice == NULL) {
                    return false;
                }
                ALCCapabilities deviceCaps = ALC.createCapabilities(newDevice);
                long newContext = alcCreateContext(newDevice, (IntBuffer) null);
                if (newContext == NULL) {
                    alcCloseDevice(newDevice);
                    return false;
                }
                alcMakeContextCurrent(newContext);
                AL.createCapabilities(deviceCaps);
                device = newDevice;
                context = newContext;
                isInitialized = true;
                System.out.println("AudioEngine: switched to device " + deviceName + " (initializing)");
                return true;
            }
            // Ya está inicializado: cerramos el contexto/dispositivo actual y abrimos el nuevo
            long oldDevice = device;
            long oldContext = context;
            // Desvincular contexto actual
            alcMakeContextCurrent(NULL);
            if (oldContext != NULL) alcDestroyContext(oldContext);
            if (oldDevice != NULL) alcCloseDevice(oldDevice);
            long newDevice = alcOpenDevice(deviceName);
            if (newDevice == NULL) {
                // Intentar restaurar el contexto anterior si es posible
                if (oldContext != NULL) {
                    alcMakeContextCurrent(oldContext);
                }
                return false;
            }
            ALCCapabilities newDeviceCaps = ALC.createCapabilities(newDevice);
            long newContext = alcCreateContext(newDevice, (IntBuffer) null);
            if (newContext == NULL) {
                alcCloseDevice(newDevice);
                if (oldContext != NULL) {
                    alcMakeContextCurrent(oldContext);
                }
                return false;
            }
            alcMakeContextCurrent(newContext);
            AL.createCapabilities(newDeviceCaps);
            device = newDevice;
            context = newContext;
            System.out.println("AudioEngine: switched device to " + deviceName);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static long getDevice() {
        return device;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }
}