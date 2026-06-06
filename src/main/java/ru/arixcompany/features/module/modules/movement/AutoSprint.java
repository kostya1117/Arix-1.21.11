package ru.arixcompany.features.module.modules.movement;

import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventPriority;
import ru.arixcompany.features.event.player.EventMovementTick;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;

import javax.annotation.processing.SupportedSourceVersion;

public class AutoSprint extends Module {
    public boolean sprint = true;
    public AutoSprint() {
        super("AutoSprint", Category.Movement);
    }
    @EventHandler
    public void sprintHandler(EventSprint event) {
        if ((event.getSource() == EventSprint.Source.INPUT) && canStartSprinting() && sprint) {
            event.setSprinting(true);
        }
    }
    public boolean canStartSprinting() {
        return mc.player.input.hasForwardImpulse()
                && mc.player.isSprintingPossible(mc.player.getAbilities().flying)
                && !mc.player.isSlowDueToUsingItem()
                && (!mc.player.isFallFlying() || mc.player.isUnderWater())
                && (!mc.player.isMovingSlowly() || mc.player.isUnderWater());
    }
}
