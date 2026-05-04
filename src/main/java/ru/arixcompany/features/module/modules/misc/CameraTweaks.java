package ru.arixcompany.features.module.modules.misc;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

public class CameraTweaks extends Module {
    public CameraTweaks() {
        super("CameraTweaks", Category.Misc);
        setup(focus,through);
    }

    public static final ValueSetting focus = new ValueSetting("Приближение")
            .setValue(1).range(0.1F,2).setStep(0.1F);
    public static final BooleanSetting through = new BooleanSetting("Камера через стенки");
}
