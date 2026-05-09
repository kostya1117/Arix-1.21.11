package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;

@Getter
@AllArgsConstructor
public class EventMouseScroll extends Event {
    private final double deltaX;
    private final double deltaY;

    private final double mouseX;
    private final double mouseY;
}