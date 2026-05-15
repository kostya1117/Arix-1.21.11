package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class ValueSetting extends Setting {
    private float value, min, max, step = 0.01f;
    private boolean integer;

    public ValueSetting(String name) {
        super(name);
    }

    public ValueSetting range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public ValueSetting range(int min, int max) {
        this.min = min;
        this.max = max;
        this.integer = true;
        return this;
    }

    public ValueSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public ValueSetting step(float step) {
        this.step = step;
        return this;
    }

    public int getInt() {
        return (int) value;
    }
}