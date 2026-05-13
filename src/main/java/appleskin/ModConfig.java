package appleskin;

public class ModConfig {
    public static final ModConfig INSTANCE = new ModConfig();

    public boolean showFoodValuesInTooltip         = true;
    public boolean showFoodValuesInTooltipAlways   = true;
    public boolean showSaturationHudOverlay        = true;
    public boolean showFoodValuesHudOverlay        = true;
    public boolean showFoodValuesHudOverlayWhenOffhand = true;
    public boolean showFoodExhaustionHudUnderlay   = true;
    public boolean showFoodHealthHudOverlay        = true;
    public boolean showVanillaAnimationsOverlay    = true;
    public float   maxHudOverlayFlashAlpha         = 0.65f;
}
