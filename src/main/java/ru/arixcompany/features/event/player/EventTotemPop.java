package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventTotemPop extends Event {
    private final Player entity;
    private int pops;
}
