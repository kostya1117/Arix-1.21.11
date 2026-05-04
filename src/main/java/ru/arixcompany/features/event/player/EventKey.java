package ru.arixcompany.features.event.player;

import lombok.Getter;
import ru.arixcompany.features.event.Event;

@Getter
public final class EventKey extends Event {

    private final int key;
    private final int action;
    private final boolean mouse;

    public EventKey(int key, int action) {
        this.key = key;
        this.action = action;
        this.mouse = false;
    }

    public EventKey(int key, int action, boolean mouse) {
        this.key = key;
        this.action = action;
        this.mouse = mouse;
    }


    public boolean isKeyDown(int bindKey) {
        int eventKey = this.key;
        if (this.mouse && this.key >= 0 && this.key <= 16) {
            eventKey = -100 - this.key;
        }
        return eventKey == bindKey && this.action == 1;
    }
    public boolean isKeyReleased(int key, boolean screen) {
        return this.key == key && action == 0 && screen;
    }
}