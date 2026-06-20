package dev.tr7zw.waveycapes.versionless.config;

import dev.tr7zw.waveycapes.versionless.CapeMovement;
import dev.tr7zw.waveycapes.versionless.CapeStyle;
import dev.tr7zw.waveycapes.versionless.WindMode;

public class Config {

    public WindMode windMode = WindMode.WAVES;
    public CapeStyle capeStyle = CapeStyle.SMOOTH;
    public CapeMovement capeMovement = CapeMovement.BASIC_SIMULATION;
    // public int capeParts = 16;
    public int gravity = 25;
    public int heightMultiplier = 6;
    public int straveMultiplier = 2;
    // public int maxBend = 5;
}
