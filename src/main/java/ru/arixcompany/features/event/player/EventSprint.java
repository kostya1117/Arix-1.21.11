package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventSprint extends Event {
    private boolean sprinting;
}
