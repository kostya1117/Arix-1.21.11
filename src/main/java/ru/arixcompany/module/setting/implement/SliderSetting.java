package ru.arixcompany.module.setting.implement;

import ru.arixcompany.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class SliderSetting extends Setting {
    private float value, min, max, step = 0.01f;
    private boolean integer;

    public SliderSetting(String name) {
        super(name);
    }

    public SliderSetting range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public SliderSetting range(int min, int max) {
        this.min = min;
        this.max = max;
        this.integer = true;
        return this;
    }

    public int getInt() {
        return (int) value;
    }

    public SliderSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public SliderSetting step(float step) {
        this.step = step;
        return this;
    }
}