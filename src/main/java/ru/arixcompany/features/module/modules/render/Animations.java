package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class Animations extends Module {

    public static final ListSetting animations = new ListSetting("Анимации")
            .value("Чат", "Перспектива", "Зум", "Таб", "Хотбар");

    public Animations() {
        super("Animations", Category.Render);
        setup(animations);
    }

    public static boolean isEnabled(String name) {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Animations mod = Arix.getInstance().getModuleRepo().getModule(Animations.class);
        return mod != null && mod.isState() && animations.isSelected(name);
    }
}
