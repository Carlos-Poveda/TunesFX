package org.example.tunesfx.audio;

public class PlaylistItem {
    private String patternName; // Qué suena
    private int startBar;       // Cuándo empieza (compás)
    private int trackIndex;     // En qué pista está
    private int durationBars;

    public PlaylistItem(String patternName, int startBar, int trackIndex) {
        this.patternName = patternName;
        this.startBar = startBar;
        this.trackIndex = trackIndex;
    }

    // Getters
    public String getPatternName() { return patternName; }
    public int getStartBar() { return startBar; }
    public int getTrackIndex() { return trackIndex; }
    // Setters (necesarios para el Drag & Drop)
    public void setStartBar(int startBar) { this.startBar = startBar; }
    public void setTrackIndex(int trackIndex) { this.trackIndex = trackIndex; }
    public int getDurationBars() { return durationBars; }
    public void setDurationBars(int durationBars) {this.durationBars = durationBars; }
}