package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.arixcompany.features.event.Event;

@Data
@AllArgsConstructor
public class EventInput extends Event {
    private float forward, strafe;
    private boolean jump, sneak;
    private double sneakSlowDownMultiplier;
}