package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@Setter
@Getter
@AllArgsConstructor
public class EventMouseLook
        extends Event {
    private double yaw;
    private double pitch;
}