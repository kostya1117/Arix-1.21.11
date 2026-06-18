package ru.arixcompany.features.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
@Setter
public class EventOnMovePost extends Event {

    float speed;
    Vec3 movementInput;

}
