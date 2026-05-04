package ru.arixcompany.features.module.setting;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
public class Setting {
    private final String name;

    @Setter
    private Supplier<Boolean> visible;

    public Setting(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible == null || visible.get();
    }
}
