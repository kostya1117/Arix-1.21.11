package ru.arixcompany.features.module.modules.combat.aura;

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
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class AttackHandler implements IMinecraft {

    // ─── CPS bypass ──────────────────────────────────────────────────────────

    public static long hitCounterCPSBypass;
    private static final Timer cooldownTimer = new Timer();
    private static boolean missDetected;
    private static int counterTo0PostMissHits;

    public static void hitCounterCPSBypassNext()  { hitCounterCPSBypass++; }
    public static void hitCounterCPSBypassReset() { hitCounterCPSBypass = 0L; }
    public static boolean cpsBypassTrigger()      { return hitCounterCPSBypass % 7L == 3L; }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public static Player getSelf()  { return mc.player; }
    public static Level  getWorld() { return mc.level;  }

    // ─── Main attack ─────────────────────────────────────────────────────────

    public static void performAttack(
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

        antiMissesHittingUpdate(target, true, rayCast, false);

        boolean canAttack = shouldAttack(target, rayCast, true, true, 0L, ranges);
        if (!canAttack) return;

        Runnable[] skipSilentSprint = skipSilentSprintingTaskForUse(canPacket);

        mc.player.setSprinting(false);
        mc.options.keySprint.setDown(false);

        useEntity(target, skipSilentSprint[0], skipSilentSprint[1], InteractionHand.MAIN_HAND, true);
    }

    // ─── Y-capacity (для крит-детекции) ──────────────────────────────────────

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

    // ─── Крит-момент ─────────────────────────────────────────────────────────

    public static boolean isBestMomentToHit(boolean fallCheck) {
        if (!fallCheck || mc.player == null) return true;

        float adaptiveFallValue = 0.0F;
        if (cpsBypassTrigger() && getYCapacityOnPlayerPos(2) > 0.20000005F) {
            adaptiveFallValue = 0.2F;
        }

        boolean hasFall = convenientFallOffset() > adaptiveFallValue
                || getYCapacityOnPlayerPos(2) < 0.1F;
        if (hasFall) return true;

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

    // ─── Sprint-пакеты ────────────────────────────────────────────────────────

    public static Runnable[] skipSilentSprintingTaskForUse(boolean enabled) {
        Runnable[] prePost = new Runnable[]{() -> {}, () -> {}};
        if (enabled && mc.player != null
                && mc.player.isSprinting()
                && !mc.player.onGround()
                && !mc.player.isEyeInFluid(FluidTags.WATER)) {
            prePost[0] = () -> {
                mc.player.setSprinting(false);
                mc.getConnection().send(new ServerboundPlayerCommandPacket(
                        mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            };
            prePost[1] = () -> {
                mc.player.setSprinting(true);
                mc.getConnection().send(new ServerboundPlayerCommandPacket(
                        mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            };
        }
        return prePost;
    }

    // ─── Удар ────────────────────────────────────────────────────────────────

    public static boolean useEntity(
            LivingEntity target,
            Runnable preHit,
            Runnable postHit,
            InteractionHand hand,
            boolean cpsBypass
    ) {
        if (preHit != null) preHit.run();

        if (target != null && mc.gameMode != null && mc.player != null) {
            mc.gameMode.attack(mc.player, target);
            if (hand != null) mc.player.swing(hand);

            if (cpsBypass) hitCounterCPSBypassNext();
            else           hitCounterCPSBypassReset();

            cooldownTimer.reset();
        }

        if (postHit != null) postHit.run();

        return target != null;
    }

    // ─── Cooldown ────────────────────────────────────────────────────────────

    public static long getMsCooldown() {
        if (mc.player == null) return 500L;

        double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
        long base = (long) (1.0 / attackSpeed * 1000.0 * 0.8);   // 0.8 = (1 - maxDeviation 0.2)

        return HitAura.attackDelay.isSelected("Динамичный")
                ? base + ThreadLocalRandom.current().nextLong(10L, 50L)
                : base + 40L;
    }

    public static boolean msCooldownReached(long msOffset) {
        return cooldownTimer.finished(getMsCooldown() + msOffset);
    }

    public static boolean msCooldownHasMs(long ms) {
        return cooldownTimer.finished(ms);
    }

    // ─── Raytrace-проверка ────────────────────────────────────────────────────

    /**
     * ИСПРАВЛЕНО: раньше передавался getYRot() дважды вместо yaw и pitch.
     * Теперь передаём реальные yaw/pitch игрока.
     */
    public static boolean anyEntityOnRay(LivingEntity target, double range) {
        if (target == null || mc.player == null) return false;
        return RayTraceUtil.rayTraceEntity(
                mc.player.getYRot(),
                mc.player.getXRot(),
                range,
                target
        );
    }

    // ─── shouldAttack ────────────────────────────────────────────────────────

    public static boolean shouldAttack(
            LivingEntity target,
            boolean rayCast,
            boolean distanceCheck,
            boolean fallCheck,
            long cooldownMSOffset,
            float[] ranges
    ) {
        if (distanceCheck && target != null && !AuraUtil.validDistance(target, ranges[0], true))
            return false;
        if (!msCooldownReached(cooldownMSOffset))
            return false;

        boolean valid = isBestMomentToHit(fallCheck);
        if (valid && rayCast && !anyEntityOnRay(target, ranges[0]))
            valid = false;

        return valid;
    }

    public static boolean shouldAttack(
            LivingEntity target,
            boolean rayCast,
            boolean fallCheck,
            long cooldownMSOffset,
            float[] ranges
    ) {
        return shouldAttack(target, rayCast, true, fallCheck, cooldownMSOffset, ranges);
    }

    public static boolean resetSprintTick(LivingEntity target, float[] ranges) {
        return target != null
                && shouldAttack(target, false, false, -60L, ranges)
                && !mc.player.onGround()
                && !mc.player.isEyeInFluid(FluidTags.WATER)
                && mc.player.getDeltaMovement().y <= 0.16477328182606651;
    }

    // ─── Anti-miss ───────────────────────────────────────────────────────────

    private static int maxHitsCountOnMiss() { return 3; }

    public static void antiMissesHittingReset() {
        missDetected = false;
        counterTo0PostMissHits = 0;
    }

    public static void antiMissesHittingUpdate(
            LivingEntity target,
            boolean cpsBypass,
            boolean rayCastCheck,
            boolean enabled
    ) {
        if (target == null || counterTo0PostMissHits == 0 || !enabled || target.hurtTime != 0) {
            antiMissesHittingReset();
        }

        if (enabled && target != null
                && msCooldownHasMs(cpsBypassTrigger() ? 250L : 150L)
                && mc.player.swinging) {
            if (!missDetected && counterTo0PostMissHits == 0 && target.hurtTime == 0) {
                missDetected = true;
                counterTo0PostMissHits = maxHitsCountOnMiss();
            }

            if (missDetected && counterTo0PostMissHits > 0
                    && (!rayCastCheck || anyEntityOnRay(target, 6.0))
                    && useEntity(target, () -> {}, () -> {}, InteractionHand.MAIN_HAND, cpsBypass)) {
                counterTo0PostMissHits--;
            }
        }
    }
}
