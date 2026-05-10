package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventMotion
        extends Event {
    private float yaw;
    private float pitch;
    private double X;
    private double Y;
    private double Z;
    private boolean onGround;
    private boolean sprint;
}
