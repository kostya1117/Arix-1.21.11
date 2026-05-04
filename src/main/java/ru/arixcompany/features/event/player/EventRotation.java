package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Setter
@Getter
public class EventRotation extends Event {
    public float yaw;
    public float pitch;
    public float partialTicks;
}
