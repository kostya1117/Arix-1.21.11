package net.minecraft.client.player;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import ru.arixcompany.features.event.Event;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.player.EventSprint;

//public class KeyboardInput extends ClientInput {
//    private final Options options;
//
//    public KeyboardInput(Options pOptions) {
//        this.options = pOptions;
//    }
//
//    private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
//        if (pInput == pOtherInput) {
//            return 0.0F;
//        } else {
//            return pInput ? 1.0F : -1.0F;
//        }
//    }
//
//    @Override
//    public void tick() {
//        final EventSprint eventSprint = new EventSprint(this.options.keySprint.isDown(),EventSprint.Source.INPUT);
//        EventRepo.call(eventSprint);
//        this.keyPresses = new Input(
//                this.options.keyUp.isDown(),
//                this.options.keyDown.isDown(),
//                this.options.keyLeft.isDown(),
//                this.options.keyRight.isDown(),
//                this.options.keyJump.isDown(),
//                this.options.keyShift.isDown(),
//                eventSprint.isSprinting()
//        );
//        final EventInput moveInputEvent = new EventInput(forwardImpulse, leftImpulse, jumping, shiftKeyDown, 0.3D);
//        EventRepo.call(moveInputEvent);
//
//        this.forwardImpulse = moveInputEvent.getForward();
//        this.leftImpulse = moveInputEvent.getStrafe();
//        this.jumping = moveInputEvent.isJump();
//        this.shiftKeyDown = moveInputEvent.isSneak();
//        this.forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
//        this.leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
//    }
//}
public class KeyboardInput extends ClientInput {
    private final Options options;

    public KeyboardInput(Options p_108580_) {
        this.options = p_108580_;
    }

    private static float calculateImpulse(boolean p_205578_, boolean p_205579_) {
        if (p_205578_ == p_205579_) {
            return 0.0F;
        } else {
            return p_205578_ ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {
        final EventSprint eventSprint = new EventSprint(this.options.keySprint.isDown(), EventSprint.Source.INPUT);
        EventRepo.call(eventSprint);

        boolean forward = this.options.keyUp.isDown();
        boolean backward = this.options.keyDown.isDown();
        boolean left = this.options.keyLeft.isDown();
        boolean right = this.options.keyRight.isDown();
        boolean jump = this.options.keyJump.isDown();
        boolean shift = this.options.keyShift.isDown();
        boolean sprint = eventSprint.isSprinting();

        float forwardImpulse = calculateImpulse(forward, backward);
        float leftImpulse = calculateImpulse(left, right);

        Vec2 moveVector = new Vec2(leftImpulse, forwardImpulse);
        if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_21_4)) {
            moveVector = moveVector.normalized();
        }

        EventInput event = new EventInput(forward, backward, left, right, jump, shift, sprint, moveVector);
        EventRepo.call(event);

        if (event.isCancelled()) {
            this.keyPresses = new Input(false, false, false, false, false, false, false);
            this.moveVector = Vec2.ZERO;
            return;
        }

        this.keyPresses = new Input(
                event.isForward(),
                event.isBackward(),
                event.isLeft(),
                event.isRight(),
                event.isJump(),
                event.isShift(),
                event.isSprint()
        );

        this.moveVector = event.getMoveVector();
    }
}