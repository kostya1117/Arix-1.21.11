package ru.arixcompany.features.module.modules.combat.aura.attack;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;

public class AttackHandler implements IMinecraft {

    private final Timer attackTimer = new Timer();
    private final ListSetting options;

    public AttackHandler(ListSetting options) {
        this.options = options;
    }

    public void handle(LivingEntity target, boolean rotationValid) {
        if (target == null || mc.player == null) return;
        if (!canAttack()) return;
        if (!rotationValid) return;
        if (!canCrit()) return;

        performAttack(target);
        attackTimer.reset();
    }

    private boolean canAttack() {
        return attackTimer.hasReached(100)
                && mc.player.getAttackStrengthScale(1.0f) >= 1f;
    }

    private boolean canCrit() {
        if (!options.is("Только криты")) return true;

        return !mc.player.onGround()
                && mc.player.fallDistance > 0
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable()
                && mc.player.getDeltaMovement().y < 0;
    }

    private void performAttack(LivingEntity target) {

        if (target instanceof Player player
                && options.is("Ломать щит")
                && player.isBlocking()) {
            // TODO: Shield breaker (switch to axe)
        }

        mc.player.setSprinting(false);
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.setSprinting(true);
    }

    public void reset() {
        attackTimer.reset();
    }
}