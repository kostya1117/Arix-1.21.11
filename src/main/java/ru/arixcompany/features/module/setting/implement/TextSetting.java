package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class TextSetting extends Setting {
    public String text;
    public boolean active;

    public TextSetting(String name) {
        super(name);
    }

    public TextSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}