package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public final class EventKey extends Event {
    private final int key;
    private final int action;
}