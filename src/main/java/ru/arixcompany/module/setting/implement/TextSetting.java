package ru.arixcompany.module.setting.implement;

import ru.arixcompany.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class TextSetting extends Setting {
    private String text;
    private int min, max;

    public TextSetting(String name, String description) {
        super(name, description);
    }

    public TextSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}