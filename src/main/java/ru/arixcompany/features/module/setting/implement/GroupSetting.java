package ru.arixcompany.features.module.setting.implement;

import lombok.Getter;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.quad.EaseInOutQuad;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class GroupSetting extends Setting {

    private final List<Setting> children;
    private boolean open = false;
    public final Animation openAnim;

    public GroupSetting(String name, Setting... children) {
        super(name);
        this.children = Arrays.asList(children);
        this.openAnim = new EaseInOutQuad(200, 1.0);
        this.openAnim.setDirection(Direction.BACKWARDS);
        this.openAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    public void toggle() {
        open = !open;
        openAnim.setDirection(open ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public List<Setting> getVisibleChildren() {
        return children.stream()
                .filter(s -> s.getVisible() == null || s.getVisible().get())
                .collect(Collectors.toList());
    }

    public GroupSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}
