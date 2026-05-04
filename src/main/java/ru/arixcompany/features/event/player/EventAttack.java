package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
@Setter
public class EventAttack extends Event {
   private Entity target;
}
