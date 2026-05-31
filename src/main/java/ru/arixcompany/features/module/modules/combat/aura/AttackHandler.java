package ru.arixcompany.features.module.modules.combat.aura;

import lombok.experimental.UtilityClass;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.jsonrpc.methods.Message;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.SprintServerRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.TimerUtils;
import ru.arixcompany.utils.player.FallingPlayer;

@UtilityClass
public final class AttackHandler implements IMinecraft {

    public final TimerUtils cooldownTimer = new TimerUtils();

    public void performAttack(LivingEntity target, boolean rayCast, float ranges) {
        if (target == null || mc.player == null) return;
        if (AuraUtil.getStrictDistance(target) >= ranges) return;

        boolean canAttack = shouldAttack(target, rayCast, true, ranges);
        if (!canAttack) return;

        HitAura hitAura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);

        if (target.hurtTime > 7 && hitAura.extraSettings.isSelected("Фикс удара при HurtTime")) {
            return;
        }
        if (hitAura.sprintReset.isSelected("Пакет") && hitAura.extraSettings.isSelected("Сброс спринта") && SprintServerRepo.serverSprint) {
            disableSprint();
        }

        useEntity(target, InteractionHand.MAIN_HAND);
    }

    public boolean shouldAttack() {
        if (HitAura.target == null) return false;
        if (!(mc.player.getAttackStrengthScale(0.5f) > 0.9f)) return false;
        return isBestMomentToHit();
    }
    public boolean shouldAttackS() {
        if (HitAura.target == null) return false;
        return mc.player.getAttackStrengthScale(0.5f) > 0.9f;
    }
    public boolean hasMovementRestrictions() {
        if (mc.player == null) return false;

        return mc.player.hasEffect(MobEffects.BLINDNESS)
                || mc.player.hasEffect(MobEffects.LEVITATION)
                || (mc.level != null && mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB))
                || mc.player.isUnderWater()
                || mc.player.isInLava()
                || mc.player.onClimbable()
                || mc.player.getAbilities().flying;
    }

    public boolean isBestMomentToHit() {
        if (mc.player == null) return false;

//        if (!canCrit()) {
//            return canAttackNow();
//        }
        HitAura hitAura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        boolean onGroundLong = mc.player.ticksOnGround > 6 && !hitAura.extraSettings.isSelected("Умные криты");

        if (onGroundLong) {
            return false;
        } else {
            return !shouldWaitForCrit();
        }
    }
    public boolean shouldWaitForCrit() {
        if (mc.player == null) {
            return false;
        }

        LocalPlayer player = mc.player;

        if (player.isFallFlying()) {
            return false;
        }

        if (!canCrit() || player.getDeltaMovement().y < -0.08) {
            return false;
        }

        float nextPossibleCrit = calculateTicksUntilNextCrit();
        double gravity = 0.08;
        float ticksTillFall = (float) (player.getDeltaMovement().y / gravity);
        float ticksTillCrit = Math.max(nextPossibleCrit, ticksTillFall);
        float hitProbability = 0.75f;
        float damageOnCrit = 0.5f * hitProbability;
        float damageLostWaiting = getCooldownDamageFactor(player, ticksTillCrit);

        if (damageOnCrit <= damageLostWaiting) {
            return false;
        }

        float targetFall = MathUtils.randomValue(0.1f, 0.3f);
        double fallPerTick = Math.abs(player.getDeltaMovement().y);
        double ticksToReachFall = fallPerTick > 0
                ? Math.max(0, (targetFall - player.fallDistance) / fallPerTick)
                : Float.MAX_VALUE;

        if (player.fallDistance < targetFall && ticksToReachFall < ticksTillCrit) {
            return true;
        }

        return FallingPlayer.fromPlayer(player).findCollision((int) (ticksTillCrit * 1.3f)) == null;
    }


    public boolean shouldWaitForJump() {
        return shouldWaitForJump(0.42f);
    }

    public boolean shouldWaitForJump(float initialMotion) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }

        float ticksTillFall = initialMotion / 0.08f;
        float nextPossibleCrit = calculateTicksUntilNextCrit();

        FallingPlayer.CollisionResult collision = new FallingPlayer(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getDeltaMovement().x,
                player.getDeltaMovement().y + initialMotion,
                player.getDeltaMovement().z,
                player.getYRot()
        ).findCollision((int) (ticksTillFall * 3.0f));

        Integer ticksTillNextOnGround = collision != null ? collision.getTick() : null;

        if (ticksTillNextOnGround == null) {
            ticksTillNextOnGround = (int) ticksTillFall * 2;
        }

        if (ticksTillNextOnGround + ticksTillFall < nextPossibleCrit) {
            return false;
        }

        return ticksTillFall + 1.0f < nextPossibleCrit;
    }

    public boolean canCrit() {
        if (mc.player == null) return false;

        return !mc.player.onGround()
                && !mc.player.onClimbable()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.hasEffect(MobEffects.BLINDNESS)
                && !mc.player.hasEffect(MobEffects.SLOW_FALLING)
                && !mc.player.isPassenger()
                && !mc.player.getAbilities().flying
                && !mc.player.isFallFlying();
    }
    public float calculateTicksUntilNextCrit() {
        Player player = mc.player;
        if (player == null) {
            return 0.0f;
        }

        float durationToWait = player.getCurrentItemAttackStrengthDelay() * 0.9F - 0.5F;
        float waitedDuration = (float) player.attackStrengthTicker;

        return Math.max(durationToWait - waitedDuration, 0.0f);
    }

    public float getCooldownDamageFactor(Player player, float tickDelta) {
        float base = (tickDelta + 0.5f) / player.getCurrentItemAttackStrengthDelay();
        return Math.min(0.2f + base * base * 0.8f, 1.0f);
    }

    private boolean canAttackNow() {
        if (mc.player == null) return false;

        boolean inWeb = mc.level != null && mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB);
        boolean inLiquid = mc.player.isInWater() || mc.player.isInLava()
                || mc.player.isEyeInFluid(FluidTags.WATER)
                || mc.player.isEyeInFluid(FluidTags.LAVA);

        return inWeb
                || inLiquid
                || mc.player.onClimbable()
                || mc.player.isPassenger()
                || mc.player.hasEffect(MobEffects.BLINDNESS)
                || mc.player.hasEffect(MobEffects.LEVITATION)
                || mc.player.hasEffect(MobEffects.SLOW_FALLING)
                || mc.player.getAbilities().flying;
    }

    public boolean useEntity(LivingEntity target, InteractionHand hand) {
        if (target == null || mc.gameMode == null || mc.player == null) return false;

        HitAura hitAura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(hand);
        MessageSender.sendOverlayMessage(Component.literal(mc.player.fallDistance + "")); //debug
        hitAura.count = (hitAura.count + 1) % 2;
        cooldownTimer.reset();

        return true;
    }



    private void disableSprint() {
        mc.player.setSprinting(false);
        mc.options.keySprint.setDown(false);
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
    }

    public long getMsCooldown() {
        if (mc.player == null) return 500L;
        double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        return (long) (1.0 / attackSpeed * 1000.0 * 0.8);
    }

    public boolean msCooldownReached() {
        return cooldownTimer.finished(getMsCooldown());
    }

    public boolean anyEntityOnRay(LivingEntity target, float range) {
        if (target == null || mc.player == null) return false;
        return RayTraceUtil.getServerHitResult(
                mc.player,
                RotationManager.currentRotation.yaw(),
                RotationManager.currentRotation.pitch(),
                e -> e == target,
                range
        ).getType() == net.minecraft.world.phys.HitResult.Type.ENTITY;
    }

    public boolean anyEntityOnRay(Rotation rotation, LivingEntity target, float range) {
        if (target == null || mc.player == null) return false;
        return RayTraceUtil.getServerHitResult(
                mc.player,
                rotation.yaw(),
                rotation.pitch(),
                e -> e == target,
                range
        ).getType() == net.minecraft.world.phys.HitResult.Type.ENTITY;
    }

    public boolean shouldAttack(LivingEntity target, boolean rayCast, boolean distanceCheck, float ranges) {
        if (distanceCheck && target != null && !AuraUtil.validDistance(target, ranges)) return false;
        if (!(mc.player.getAttackStrengthScale(0.5f) > 0.9f)) return false;

        boolean valid = isBestMomentToHit();
        if (valid && rayCast) {
            if (!anyEntityOnRay(new Rotation(mc.player), target, ranges)) {
                valid = false;
            }
        }
        return valid;
    }
}
