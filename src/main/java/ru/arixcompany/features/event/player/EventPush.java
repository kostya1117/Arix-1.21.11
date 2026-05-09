package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventPush extends Event {
    PushEnum pushEnum;

    public enum PushEnum {
        Players,
        Blocks,
        Fluids
    }
}
