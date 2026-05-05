package net.minecraft.client.player;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventInput;

public class KeyboardInput extends ClientInput {
    private final Options options;

    public KeyboardInput(Options pOptions) {
        this.options = pOptions;
    }

    private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
        if (pInput == pOtherInput) {
            return 0.0F;
        } else {
            return pInput ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {
        this.keyPresses = new Input(
                this.options.keyUp.isDown(),
                this.options.keyDown.isDown(),
                this.options.keyLeft.isDown(),
                this.options.keyRight.isDown(),
                this.options.keyJump.isDown(),
                this.options.keyShift.isDown(),
                this.options.keySprint.isDown()
        );
        final EventInput moveInputEvent = new EventInput(forwardImpulse, leftImpulse, jumping, shiftKeyDown, 0.3D);
        EventRepo.call(moveInputEvent);

        this.forwardImpulse = moveInputEvent.getForward();
        this.leftImpulse = moveInputEvent.getStrafe();
        this.jumping = moveInputEvent.isJump();
        this.shiftKeyDown = moveInputEvent.isSneak();
        this.forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        this.leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
    }
}