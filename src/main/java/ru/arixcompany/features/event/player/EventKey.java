package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;
import ru.arixcompany.features.module.setting.implement.BindSetting;

@AllArgsConstructor
@Getter
public final class EventKey extends Event {
    private final int key;
    private final int action;

    public int getKey() {
        if (key >= 0 && key <= 7) {
            return -100 - key;
        }
        return key;
    }

    public boolean isKeyDown(int key) {
        return getKey() == key;
    }

    public boolean isKeyDown(BindSetting b) {
        return getKey() == b.getKey();
    }
}