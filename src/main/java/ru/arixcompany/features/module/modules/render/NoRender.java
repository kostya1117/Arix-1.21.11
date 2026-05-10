package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class NoRender extends Module {
    public ListSetting cancel = new ListSetting("Отменить")
            .value("Плохие эффекты","Огонь","Оверлеи","Свечение");

    public NoRender() {
        super("NoRender", Category.Render);
        setup(
                cancel
        );
    }

    public boolean noFireOverlay() { return state && cancel.isSelected("Огонь"); }
    public boolean noBadEffects() { return state && cancel.isSelected("Плохие эффекты"); }
    public boolean noOverlays() { return state && cancel.isSelected("Оверлеи"); }
    public boolean noGlowing() { return state && cancel.isSelected("Свечение"); }
}