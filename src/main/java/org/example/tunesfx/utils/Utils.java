package org.example.tunesfx.utils;

import static java.lang.Math.*;

public class Utils {
    public static void handleProcedure(Procedure procedure, Boolean printStackTrace) {
        try {
            procedure.invoke();
        } catch (Exception e) {
            if (printStackTrace) {
                e.printStackTrace();
            }
        }
    }

    public static double getKeyFrequency(int keyNum){
        return pow(root(2,12),keyNum -49) * 440;
    }
    public static double root(double num, double root){
        return pow(E,log(num)/root);
    }

    public static class Math {
        public static double frequencyToAngularFrecuency(double freq) {
            return 2 * PI * freq;
        }
    }
}