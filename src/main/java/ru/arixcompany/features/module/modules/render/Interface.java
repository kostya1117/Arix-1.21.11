package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class Interface extends Module {

    public static final ListSetting elements = new ListSetting("Элементы")
            .value("ArrayList","ArmorHUD","BossBar","Scoreboard","Crosshair","TargetHUD","Hotbar");

    public Interface() {
        super("Interface", Category.Render);
        setup(elements);
    }
}