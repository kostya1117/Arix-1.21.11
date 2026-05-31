package net.minecraft.client.player;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventSprint;

public class ClientInput {
    public Input keyPresses = Input.EMPTY;
    public float leftImpulse;
    public float forwardImpulse;
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;
    public boolean jumping;
    public boolean shiftKeyDown;
    public void tick() {
    }

    public Vec2 getMoveVector() {
        return new Vec2(this.leftImpulse, this.forwardImpulse);
    }

    public boolean hasForwardImpulse() {
        return this.forwardImpulse > 1.0E-5F;
    }

    public void makeJump() {
        final EventSprint eventSprint = new EventSprint(this.keyPresses.sprint(), EventSprint.Source.INPUT);
        EventRepo.call(eventSprint);
        this.keyPresses = new Input(
                this.keyPresses.forward(),
                this.keyPresses.backward(),
                this.keyPresses.left(),
                this.keyPresses.right(),
                true,
                this.keyPresses.shift(),
                eventSprint.isSprinting()
        );
    }
}