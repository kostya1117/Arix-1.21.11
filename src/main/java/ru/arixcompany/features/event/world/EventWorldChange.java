package ru.arixcompany.features.event.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
@Setter
public class EventWorldChange extends Event {
    Minecraft minecraft;
    ClientLevel clientLevel;
}
