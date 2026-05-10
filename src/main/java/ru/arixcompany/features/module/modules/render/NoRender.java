package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class NoRender extends Module {
    public BooleanSetting noBadEffects      = new BooleanSetting("Плохие эффекты");
    public BooleanSetting noFireOverlay        = new BooleanSetting("Огонь");
    public BooleanSetting overlays      = new BooleanSetting("Оверлеи");
    public BooleanSetting noGuiBackground      = new BooleanSetting("Фон GUI");
    public BooleanSetting noFog                = new BooleanSetting("Туман");
    public BooleanSetting noGlowing            = new BooleanSetting("Свечение");

    public NoRender() {
        super("NoRender", Category.Render);
        setup(
                noBadEffects,noFireOverlay,overlays,noGuiBackground,noFog,noGlowing
        );
    }

    public boolean noFireOverlay()         { return state && noFireOverlay.isValue(); }
    public boolean noBadEffects()         { return state && noBadEffects.isValue(); }
    public boolean noOverlays()       { return state && overlays.isValue(); }
    public boolean noGuiBackground()       { return state && noGuiBackground.isValue(); }
}