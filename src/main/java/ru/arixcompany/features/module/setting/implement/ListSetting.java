package ru.arixcompany.features.module.setting.implement;

import ru.arixcompany.features.module.setting.Setting;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
public class ListSetting extends Setting {
    private List<String> list, selected = new ArrayList<>();

    public ListSetting(String name) {
        super(name);
    }

    public ListSetting value(String... settings) {
        list = Arrays.asList(settings);
        return this;
    }

    public ListSetting selected(String... settings) {
        selected = new ArrayList<>(Arrays.asList(settings));
        return this;
    }

    public ListSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public boolean isSelected(String name) {
        return selected.contains(name);
    }

    public void toggleSelected(String name) {
        if (selected.contains(name)) {
            selected.remove(name);
        } else {
            selected.add(name);
        }
    }

    public void enable(String name) {
        if (!selected.contains(name)) {
            selected.add(name);
        }
    }

    public boolean is(String name) {
        return selected.contains(name);
    }
}