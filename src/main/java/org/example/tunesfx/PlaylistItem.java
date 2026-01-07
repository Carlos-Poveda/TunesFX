package org.example.tunesfx;

public class PlaylistItem {
    private String patternName; // Qué suena
    private int startBar;       // Cuándo empieza (compás)
    private int trackIndex;     // En qué pista está

    public PlaylistItem(String patternName, int startBar, int trackIndex) {
        this.patternName = patternName;
        this.startBar = startBar;
        this.trackIndex = trackIndex;
    }

    // Getters
    public String getPatternName() { return patternName; }
    public int getStartBar() { return startBar; }
    public int getTrackIndex() { return trackIndex; }
}