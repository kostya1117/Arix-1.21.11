package ru.arixcompany.features.event.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventWorldTick extends Event {
    ClientLevel clientLevel;
}
