package ru.arixcompany.module.setting;

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingAdder {
    List<Setting> settings = Lists.newArrayList();

    public final void setup(Setting... setting) {
        settings.addAll(Arrays.asList(setting));
    }

    public Setting get(String name) {
        return settings.stream()
                .filter(setting -> setting.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Setting> settings() {
        return settings;
    }

    public List<Setting> getSettingsForGUI() {
        return settings.stream()
                .filter(s -> s.getVisible() == null || s.getVisible().get())
                .collect(Collectors.toList());
    }
}
