package ru.arixcompany.features.module.modules.misc;

import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;

public final class ClickGui extends Module {

    public BindSetting bind = new BindSetting("Кнопка")
            .setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);

    public ClickGui() {
        super("ClickGui", Category.Misc);
        setup(bind);
    }
}