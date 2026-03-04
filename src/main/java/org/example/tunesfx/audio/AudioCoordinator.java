package org.example.tunesfx.audio;

public class AudioCoordinator {
    private static final Object LOCK = new Object();
    private static boolean reconnectionRequested = false;
    private static String lastReason = "";

    // Llamado por AudioDeviceMonitor para pedir reconexión automática
    public static void requestReconnection(String reason) {
        synchronized (LOCK) {
            if (!reconnectionRequested) {
                reconnectionRequested = true;
                lastReason = reason;
                System.out.println("[AudioCoordinator] Reconexión solicitada: " + reason);
            } else {
                System.out.println("[AudioCoordinator] Reconexión ya solicitada; ignorando: " + reason);
            }
        }
    }

    // Llamado por el hilo de audio para comprobar si hay que reconectar
    public static boolean pollReconnectionFlag() {
        synchronized (LOCK) {
            boolean r = reconnectionRequested;
            if (r) reconnectionRequested = false;
            return r;
        }
    }

    public static boolean switchToDevice(String deviceName) {
        boolean ok = AudioEngine.switchDevice(deviceName);
        if (ok) {
            synchronized (LOCK) {
                reconnectionRequested = false;
                lastReason = "Dispositivo cambiado a: " + deviceName;
            }
            System.out.println("[AudioCoordinator] Dispositivo cambiado a: " + deviceName);
        } else {
            System.err.println("[AudioCoordinator] Fallo al cambiar dispositivo: " + deviceName);
        }
        return ok;
    }

    public static String getLastReason() {
        synchronized (LOCK) {
            return lastReason;
        }
    }
}
