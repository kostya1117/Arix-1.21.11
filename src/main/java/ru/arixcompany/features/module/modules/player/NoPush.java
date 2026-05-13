package ru.arixcompany.features.module.modules.player;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class NoPush extends Module {
    public NoPush() {
        super("NoPush", Category.Player);
        setup(cancelPush);
    }

   public ListSetting cancelPush = new ListSetting("Отменить от")
            .value("Блоков","Игроков");
}
