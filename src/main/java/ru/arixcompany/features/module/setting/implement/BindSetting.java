package ru.arixcompany.features.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.module.setting.Setting;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class BindSetting extends Setting {
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private int type = 1;
    public boolean active;

    public BindSetting(String name) {
        super(name);
    }

    public BindSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}