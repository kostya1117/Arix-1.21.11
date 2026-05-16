package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;

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
}