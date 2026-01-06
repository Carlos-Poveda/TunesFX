package org.example.tunesfx.utils;
import org.example.tunesfx.PrincipalController;

public class GlobalState {
    private static PrincipalController principal;

    public static void setPrincipalController(PrincipalController pc) {
        principal = pc;
    }

    public static PrincipalController getPrincipalController() {
        return principal;
    }
}