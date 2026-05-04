package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class ButtonSetting extends Setting {
    private Runnable runnable;
    private String buttonName;

    public ButtonSetting(String name, String description) {
        super(name);
    }

    public ButtonSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}