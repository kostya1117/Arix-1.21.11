package ru.arixcompany.features.event.player;

import lombok.Getter;
import net.minecraft.world.phys.Vec2;
import ru.arixcompany.features.event.Event;

@Getter
public class EventInput extends Event {
    private static final float INPUT_EPSILON = 1.0E-4F;
    private static final MovementState[] MOVEMENT_STATES = new MovementState[]{
            new MovementState(false, false, false, false),
            new MovementState(true, false, false, false),
            new MovementState(false, true, false, false),
            new MovementState(false, false, true, false),
            new MovementState(false, false, false, true),
            new MovementState(true, false, true, false),
            new MovementState(true, false, false, true),
            new MovementState(false, true, true, false),
            new MovementState(false, true, false, true)
    };

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean shift;
    private boolean sprint;
    private Vec2 moveVector;

    public EventInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint, Vec2 moveVector) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.shift = shift;
        this.sprint = sprint;

        if (moveVector == null
                || !Float.isFinite(moveVector.x)
                || !Float.isFinite(moveVector.y)) {
            syncMoveVectorFromButtons();
        } else {
            this.moveVector = moveVector;
        }
    }

    public void setForward(boolean forward) {
        this.forward = forward;
        syncMoveVectorFromButtons();
    }

    public void setBackward(boolean backward) {
        this.backward = backward;
        syncMoveVectorFromButtons();
    }

    public void setLeft(boolean left) {
        this.left = left;
        syncMoveVectorFromButtons();
    }

    public void setRight(boolean right) {
        this.right = right;
        syncMoveVectorFromButtons();
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void setShift(boolean shift) {
        this.shift = shift;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    public void setMoveVector(Vec2 moveVector) {
        if (moveVector == null
                || !Float.isFinite(moveVector.x)
                || !Float.isFinite(moveVector.y)
                || moveVector.lengthSquared() <= INPUT_EPSILON) {
            applyDigitalMovement(false, false, false, false);
            return;
        }

        Vec2 normalized = moveVector.normalized();
        MovementState bestState = MOVEMENT_STATES[0];
        float bestScore = Float.NEGATIVE_INFINITY;

        for (MovementState state : MOVEMENT_STATES) {
            float score = state.moveVector.x * normalized.x + state.moveVector.y * normalized.y;
            if (score > bestScore) {
                bestScore = score;
                bestState = state;
            }
        }

        applyDigitalMovement(bestState.forward, bestState.backward, bestState.left, bestState.right);
    }

    private void applyDigitalMovement(boolean forward, boolean backward, boolean left, boolean right) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        syncMoveVectorFromButtons();
    }

    private void syncMoveVectorFromButtons() {
        float forwardImpulse = calculateImpulse(this.forward, this.backward);
        float leftImpulse = calculateImpulse(this.left, this.right);
        this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();
    }

    private static float calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    private static final class MovementState {
        private final boolean forward;
        private final boolean backward;
        private final boolean left;
        private final boolean right;
        private final Vec2 moveVector;

        private MovementState(boolean forward, boolean backward, boolean left, boolean right) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.moveVector = new Vec2(calculateImpulse(left, right), calculateImpulse(forward, backward)).normalized();
        }
    }
}