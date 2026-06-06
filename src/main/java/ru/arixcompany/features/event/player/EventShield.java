package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventShield extends Event {

    private boolean shieldUse;
    private EventShield.Source source;

    public enum Source {
        PRE,
        POST
    }
}