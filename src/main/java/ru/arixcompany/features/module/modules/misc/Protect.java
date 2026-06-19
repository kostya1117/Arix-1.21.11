package ru.arixcompany.features.module.modules.misc;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.TextSetting;

public class Protect extends Module {

    public ListSetting mode = new ListSetting("Режим")
            .value("Игрок", "Друзья")
            .selected("Игрок");

    public TextSetting nickReplacement = new TextSetting("Замена ника")
            .setText("Protected");

    public BooleanSetting hideAnarchy = new BooleanSetting("Замена анки/грифа");

    public BooleanSetting hideSites = new BooleanSetting("Скрыть сайты");

    public Protect() {
        super("Protect", Category.Misc);
        setup(mode, nickReplacement, hideAnarchy, hideSites);
    }
}
