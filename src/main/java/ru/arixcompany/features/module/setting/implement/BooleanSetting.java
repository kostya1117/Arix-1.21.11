package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class BooleanSetting extends Setting {
    public boolean value;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private int type = 1;
    private boolean active = false;
    public Animation anim = new EaseInOutQuad(200, 1.0);
    public final Animation hotkeyAnim;

    public BooleanSetting(String name) {
        super(name);
        this.hotkeyAnim = new EaseInOutQuad(200, 1.0);
        this.hotkeyAnim.setDirection(Direction.BACKWARDS);
        this.hotkeyAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    public BooleanSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}