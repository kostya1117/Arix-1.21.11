package ru.arixcompany.module.setting.implement;

import ru.arixcompany.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class SliderSettings extends Setting {
    private float value, min, max, step = 0.01f;
    private boolean integer;

    public SliderSettings(String name, String description) {
        super(name, description);
    }

    public SliderSettings range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public SliderSettings range(int min, int max) {
        this.min = min;
        this.max = max;
        this.integer = true;
        return this;
    }

    public int getInt() {
        return (int) value;
    }

    public SliderSettings visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public SliderSettings step(float step) {
        this.step = step;
        return this;
    }
}