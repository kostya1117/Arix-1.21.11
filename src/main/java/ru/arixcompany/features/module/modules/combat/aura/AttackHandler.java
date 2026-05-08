package ru.arixcompany.features.module.modules.combat.aura;

import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.Timer;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public final class AttackHandler implements IMinecraft {
    private final Timer cooldownTimer = new Timer();

    public Player getSelf()  { return mc.player; }
    public Level  getWorld() { return mc.level;  }

    public void performAttack(
            LivingEntity target,
            boolean rayCast,
            boolean legitSprint,
            float[] ranges
    ) {
        if (target == null || mc.player == null) return;
        if (AuraUtil.getStrictDistance(target) >= ranges[0]) return;

        boolean canPacket = !mc.player.hasEffect(MobEffects.BLINDNESS)
                && !mc.player.isFlyingVehicle()
                && !legitSprint;

        boolean canAttack = shouldAttack(target, rayCast, true, true, 0L, ranges);
        if (!canAttack) return;

        useEntity(target, InteractionHand.MAIN_HAND);
    }

    public static double getYCapacityOnPlayerPos(int rangeY) {
        if (mc.level == null || mc.player == null) return 1.0;

        Vec3 eyePos = mc.player.getEyePosition();
        double minDst = rangeY * 2.0;
        double maxY = 320.0;
        double minY = -64.0;
        float halfW = mc.player.getBbWidth() / 2.0F - 0.01F;

        for (Vec3 corner : Arrays.asList(
                eyePos.add(-halfW, 0, -halfW),
                eyePos.add( halfW, 0,  halfW),
                eyePos.add( halfW, 0, -halfW),
                eyePos.add(-halfW, 0,  halfW)
        )) {
            HitResult down = mc.level.clip(new ClipContext(corner, corner.add(0, -rangeY, 0),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));
            HitResult up   = mc.level.clip(new ClipContext(corner, corner.add(0,  rangeY, 0),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));

            if (maxY > up.getLocation().y)   maxY = up.getLocation().y;
            if (minY < down.getLocation().y) minY = down.getLocation().y;

            double dst = maxY - minY;
            if (minDst > dst) minDst = dst;
        }

        return minDst - mc.player.getBbHeight();
    }

    public static double convenientFallOffset() {
        if (mc.player == null) return 0.0;

        double fallOffset = mc.player.fallDistance;
        if (mc.level != null
                && !mc.player.onGround()
                && mc.player.getDeltaMovement().y < -0.0784000015258789) {
            boolean posLiquid   = !mc.level.getFluidState(mc.player.blockPosition()).isEmpty();
            boolean posUpLiquid = !mc.level.getFluidState(mc.player.blockPosition().above()).isEmpty();
            if (!posLiquid && !posUpLiquid
                    && mc.player.fallDistance < -mc.player.getDeltaMovement().y
                    && mc.player.tickCount > 6) {
                fallOffset = -mc.player.getDeltaMovement().y;
            }
        }
        return fallOffset;
    }

    public boolean isBestMomentToHit(boolean fallCheck) {
        if (!fallCheck || mc.player == null) return true;

        float adaptiveFallValue = 0.F;
        //float maxFallOff = .2F;
        double yCapacity = getYCapacityOnPlayerPos(2);
        //float maxFallOff = (float) Math.min(0.9, 0.2 + yCapacity * 0.35);
        float maxFallOff = MathUtils.randomValue(0.2f,0.7f);
        if (yCapacity > .20000004768371582D) {
            adaptiveFallValue = maxFallOff;
        }
        final boolean hasFall = convenientFallOffset() > adaptiveFallValue || getYCapacityOnPlayerPos(2) < .1F;
        if (hasFall)
            return true;

        boolean isInWeb = mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB);
        boolean badLiquid = !mc.player.isJumping()
                && (mc.player.isInWater() || mc.player.isInLava())
                || mc.player.isEyeInFluid(FluidTags.WATER)
                || mc.player.isEyeInFluid(FluidTags.LAVA)
                || isInWeb;

        return badLiquid
                || (!mc.player.isJumping() && mc.player.tickCount > 6
                        && HitAura.extraSettings.isSelected("Умные криты"))
                || mc.player.onClimbable()
                || mc.player.isPassenger()
                || mc.player.hasEffect(MobEffects.BLINDNESS)
                || mc.player.hasEffect(MobEffects.LEVITATION)
                || mc.player.hasEffect(MobEffects.SLOW_FALLING)
                || mc.player.getAbilities().flying;
    }

    public boolean useEntity(
            LivingEntity target,
            InteractionHand hand
    ) {
        if (target != null && mc.gameMode != null && mc.player != null) {
            mc.gameMode.attack(mc.player, target);
            if (hand != null)
                mc.player.swing(hand);
            Arix.getInstance().getModuleRepo().getModule(HitAura.class).count = (Arix.getInstance().getModuleRepo().getModule(HitAura.class).count + 1) % 2; // Update count for FunTime rotation

            cooldownTimer.reset();
        }
        return target != null;
    }

    public long getMsCooldown() {
        if (mc.player == null) return 500L;

        double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        long base = (long) (1.0 / attackSpeed * 1000.0 * 0.8);   // 0.8 = (1 - maxDeviation 0.2)

        return HitAura.attackDelay.isSelected("Динамичный")
                ? base + ThreadLocalRandom.current().nextLong(10L, 50L)
                : base + 40L;
    }

    public boolean msCooldownReached(long msOffset) {
        return cooldownTimer.finished(getMsCooldown() + msOffset);
    }


    public boolean anyEntityOnRay(LivingEntity target, float range) {
        if (target == null || mc.player == null) return false;

        return RayTraceUtil.checkRtx(
                mc.player.getYRot(),
                mc.player.getXRot(),
                range,
                range,
                target
        );
    }

    public boolean shouldAttack(
            LivingEntity target,
            boolean rayCast,
            boolean distanceCheck,
            boolean fallCheck,
            long cooldownMSOffset,
            float[] ranges
    ) {
        if (distanceCheck && target != null && !AuraUtil.validDistance(target, ranges[0]))
            return false;
        if (!msCooldownReached(cooldownMSOffset))
            return false;

        boolean valid = isBestMomentToHit(fallCheck);
        if (valid && rayCast) {
            if (!anyEntityOnRay(target, ranges[0])) {
                valid = false;
            }
        }

        return valid;
    }

    public boolean shouldAttack(
            LivingEntity target,
            boolean rayCast,
            boolean fallCheck,
            long cooldownMSOffset,
            float[] ranges
    ) {
        return shouldAttack(target, rayCast, true, fallCheck, cooldownMSOffset, ranges);
    }

    public boolean resetSprintTick(LivingEntity target, float[] ranges) {
        return target != null
                && shouldAttack(target, false, false, -60L, ranges)
                && !mc.player.onGround()
                && !mc.player.isEyeInFluid(FluidTags.WATER)
                && mc.player.getDeltaMovement().y <= 0.16477328182606651;
    }
}
