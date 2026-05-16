/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura;

import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.SprintServerRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.PlayerIntersectionUtil;

import java.util.Arrays;

@UtilityClass
public final class AttackHandler implements IMinecraft {

    public final Timer cooldownTimer = new Timer();

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

    public boolean hasMovementRestrictions() {
        return mc.player.hasEffect(MobEffects.BLINDNESS)
            || mc.player.hasEffect(MobEffects.LEVITATION)
            || PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)
            || mc.player.isUnderWater()
            || mc.player.isInLava()
            || mc.player.onClimbable()
            || !PlayerIntersectionUtil.canChangeIntoPose(Pose.STANDING) && mc.player.isCrouching()
            || mc.player.getAbilities().flying;
    }

    public double getYCapacityOnPlayerPos(int rangeY) {
        if (mc.level == null) return 1.0;
        Vec3 eyePos = mc.player.getEyePosition();
        double minDst = rangeY * 2.0, dst;
        double maxY = 320, minY = -64;
        final float selfWD2 = mc.player.getBbWidth() / 2.0f - 1E-2f;
        HitResult first, second;
        for (final Vec3 vec : Arrays.asList(
            eyePos.add(-selfWD2, 0, -selfWD2),
            eyePos.add(selfWD2,  0,  selfWD2),
            eyePos.add(selfWD2,  0, -selfWD2),
            eyePos.add(-selfWD2, 0,  selfWD2)
        )) {
            first  = mc.level.clip(new ClipContext(vec, vec.add(0, -rangeY, 0), ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));
            second = mc.level.clip(new ClipContext(vec, vec.add(0,  rangeY, 0), ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));
            if (maxY > second.getLocation().y) maxY = second.getLocation().y;
            if (minY < first.getLocation().y)  minY = first.getLocation().y;
            dst = maxY - minY;
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
                && mc.player.ticksOnGround > 6) {
                fallOffset = -mc.player.getDeltaMovement().y;
            }
        }
        return fallOffset;
    }

    public boolean isBestMomentToHit() {
        if (mc.player == null) return false;

        float adaptiveFallValue = 0.0f;
        double yCapacity = getYCapacityOnPlayerPos(2);
        float maxFallOff = MathUtils.randomValue(0.1f, 0.8f);
        if (yCapacity > 0.2) {
            adaptiveFallValue = maxFallOff;
        }

        final boolean hasFall = convenientFallOffset() > adaptiveFallValue || getYCapacityOnPlayerPos(2) < 0.1;
        if (hasFall) return true;

        boolean isInWeb   = mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB);
        boolean badLiquid = !mc.player.isJumping()
            && (mc.player.isInWater() || mc.player.isInLava())
            || mc.player.isEyeInFluid(FluidTags.WATER)
            || mc.player.isEyeInFluid(FluidTags.LAVA)
            || isInWeb;

        return badLiquid
            || (!mc.player.isJumping() && mc.player.ticksOnGround > 6
                && HitAura.extraSettings.isSelected("Умные криты"))
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
        ).getType() == HitResult.Type.ENTITY;
    }

    public boolean shouldAttack(LivingEntity target, boolean rayCast, boolean distanceCheck, float ranges) {
        if (distanceCheck && target != null && !AuraUtil.validDistance(target, ranges)) return false;
        if (!msCooldownReached()) return false;

        boolean valid = isBestMomentToHit();
        if (valid && rayCast) {
            if (!anyEntityOnRay(target, ranges)) {
                valid = false;
            }
        }
        return valid;
    }
}
