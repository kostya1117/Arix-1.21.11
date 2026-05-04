package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class BooleanSetting extends Setting {
    private boolean value;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private int type = 1;
    public Animation anim = new EaseInOutQuad(200, 1.0);

    public BooleanSetting(String name) {
        super(name);
    }

    public BooleanSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}