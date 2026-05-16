package ru.arixcompany.features.module.modules.misc;

import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;

public final class Core extends Module {

    public BindSetting bind = new BindSetting("Кнопка гуи")
            .setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

    public BooleanSetting customButtons = new BooleanSetting("Кастомные кнопки");
    public BooleanSetting buttonSounds = new BooleanSetting("Звуки кнопок")
            .setValue(true)
            .visible(customButtons::isValue);

    public Core() {
        super("Core", Category.Misc);
        setup(bind,customButtons,buttonSounds);
    }

    @Override
    public boolean isForced() {
        return true;
    }
}