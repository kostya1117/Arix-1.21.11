package ru.arixcompany.features.module.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventPreTick;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.player.MoveUtils;
import ru.arixcompany.utils.player.inv.InventoryUtility;

public class TargetStrafe extends Module {

    public static final BooleanSetting jump =
            new BooleanSetting("Прыжок")
                    .setValue(true);

    public static final ValueSetting distance =
            new ValueSetting("Дистанция")
                    .range(0.2f, 7.0f)
                    .setValue(1.3f)
                    .setStep(0.1f);

    public static final SelectSetting boost =
            new SelectSetting("Буст")
                    .value("Нет", "Элитра", "Урон");

    public static final ValueSetting setSpeed =
            new ValueSetting("Скорость элитры")
                    .range(0.0f, 2.0f)
                    .setValue(1.3f)
                    .setStep(0.05f)
                    .visible(() -> boost.isSelected("Элитра"));

    public static final ValueSetting velReduction =
            new ValueSetting("Снижение скорости")
                    .range(0.1f, 10.0f)
                    .setValue(6.0f)
                    .setStep(0.1f)
                    .visible(() -> boost.isSelected("Урон"));

    public static final ValueSetting maxVelocitySpeed =
            new ValueSetting("Макс. скорость")
                    .range(0.1f, 2.0f)
                    .setValue(0.8f)
                    .setStep(0.05f)
                    .visible(() -> boost.isSelected("Урон"));

    public static double oldSpeed;
    public static double contextFriction;
    public static boolean switchDir;
    public static boolean disabled;
    public static int noSlowTicks;
    public static int jumpTicks;
    public static int waterTicks;

    private static long disableTime;
    private double savedFov;

    public TargetStrafe() {
        super("TargetStrafe", Category.Combat);
        setup(jump, distance, boost, setSpeed, velReduction, maxVelocitySpeed);
    }

    @Override
    public void activate() {
        super.activate();
        oldSpeed = 0;
        savedFov = mc.options.fovEffectScale().get();
        mc.options.fovEffectScale().set(0.0);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        mc.options.fovEffectScale().set(savedFov);
    }

    public boolean canStrafe() {
        if (mc.player == null) return false;
        if (mc.player.isCrouching()) return false;
        if (mc.player.isInLava()) return false;
        if (mc.player.isInWater() || waterTicks > 0) return false;
        if (mc.player.getAbilities().flying) return false;
        return true;
    }

    public boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision
                || ((mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) && jumpTicks <= 0)) {
            jumpTicks = 10;
            return true;
        }
        for (int i = (int) (mc.player.getY() + 4); i >= 0; --i) {
            BlockPos bp = new BlockPos((int) Math.floor(x), i, (int) Math.floor(z));
            var block = mc.level.getBlockState(bp).getBlock();
            if (block == Blocks.LAVA || block == Blocks.FIRE) return true;
            if (!mc.level.isEmptyBlock(bp)) return false;
        }
        return false;
    }

    public double calculateSpeed(double motionY) {
        jumpTicks--;

        float speedAttr = getAIMoveSpeed();

        BlockPos groundBp = new BlockPos(
                (int) mc.player.getX(),
                (int) (mc.player.getY() - 0.01),
                (int) mc.player.getZ());
        float slipperiness = mc.level.getBlockState(groundBp).getBlock().getFriction() * 0.91f;

        float n6 = mc.player.hasEffect(MobEffects.JUMP_BOOST) && mc.player.isUsingItem()
                ? 0.88f
                : (float) (oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91f);
        if (mc.player.onGround()) n6 = slipperiness;

        float n7 = (float) (0.1631f / Math.pow(n6, 3.0f));
        float n8;

        if (mc.player.onGround()) {
            n8 = speedAttr * n7;
            if (motionY > 0) {
                n8 += boost.isSelected("Элитра") && InventoryUtility.getElytra() != -1 && disabled
                        ? 0.65f : 0.2f;
            }
            disabled = false;
        } else {
            n8 = 0.0255f;
        }

        boolean noslow = false;
        double max2 = oldSpeed + n8;
        double max = 0.0;

        if (mc.player.isUsingItem() && motionY <= 0) {
            double n10 = oldSpeed + n8 * 0.25;
            if (motionY != 0.0 && Math.abs(motionY) < 0.08) n10 += 0.055;
            if (max2 > (max = Math.max(0.043, n10))) {
                noslow = true;
                ++noSlowTicks;
            } else noSlowTicks = Math.max(noSlowTicks - 1, 0);
        } else noSlowTicks = 0;

        if (noSlowTicks > 3) max2 = max - 0.019;
        else max2 = Math.max(noslow ? 0 : 0.25, max2) - (mc.player.tickCount % 2 == 0 ? 0.001 : 0.002);

        contextFriction = n6;
        return max2;
    }

    public float getAIMoveSpeed() {
        boolean prev = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getSpeed() * 1.3f;
        mc.player.setSprinting(prev);
        return speed;
    }

    public static void doElytraBoost(int elytraSlot) {
        if (elytraSlot == -1) return;
        if (System.currentTimeMillis() - disableTime <= 190L) return;

        if (elytraSlot != -2) {
            mc.gameMode.handleInventoryMouseClick(0, elytraSlot, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
        }

        mc.player.connection.send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.connection.send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

        if (elytraSlot != -2) {
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, elytraSlot, 1, ClickType.PICKUP, mc.player);
        }

        disableTime = System.currentTimeMillis();
        disabled = true;
    }

    private double angleToPoint(double x, double z) {
        double dx = x - mc.player.getX();
        double dz = z - mc.player.getZ();
        return Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
    }

    @EventHandler
    public void onPreTick(EventPreTick e) {
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = HitAura.target;

        if (boost.isSelected("Элитра")) {
            int elytraSlot = InventoryUtility.getElytra();
            if (elytraSlot != -1 && !mc.player.onGround()
                    && mc.player.fallDistance > 0 && !disabled) {
                doElytraBoost(elytraSlot);
            }
        }

        if (!canStrafe() || target == null) {
            oldSpeed = 0;
            return;
        }

        double motionY = mc.player.getDeltaMovement().y;

        if (boost.isSelected("Элитра") && InventoryUtility.getElytra() != -1) {
            if (MoveUtils.isMoving() && !mc.player.onGround()
                    && mc.level.getBlockCollisions(mc.player,
                    mc.player.getBoundingBox().move(0, motionY, 0)).iterator().hasNext()
                    && disabled) {
                oldSpeed = setSpeed.getValue();
            }
        }

        double speed = calculateSpeed(motionY);

        double wrap = Math.atan2(
                mc.player.getZ() - target.getZ(),
                mc.player.getX() - target.getX());
        double dist = mc.player.distanceTo(target);

        wrap += switchDir
                ? speed / Math.sqrt(dist)
                : -(speed / Math.sqrt(dist));

        double tx = target.getX() + distance.getValue() * Math.cos(wrap);
        double tz = target.getZ() + distance.getValue() * Math.sin(wrap);

        if (needToSwitch(tx, tz)) {
            switchDir = !switchDir;
            double delta = switchDir
                    ? speed / Math.sqrt(dist)
                    : -(speed / Math.sqrt(dist));
            wrap += 2 * delta;
            tx = target.getX() + distance.getValue() * Math.cos(wrap);
            tz = target.getZ() + distance.getValue() * Math.sin(wrap);
        }

        double angle = Math.toRadians(angleToPoint(tx, tz));
        double vx = speed * -Math.sin(angle);
        double vz = speed * Math.cos(angle);

        mc.player.setDeltaMovement(vx, motionY, vz);
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.level == null) return;

        oldSpeed = Math.hypot(
                mc.player.getX() - mc.player.xOld,
                mc.player.getZ() - mc.player.zOld) * contextFriction;

        if (mc.player.onGround() && jump.isValue() && HitAura.target != null) {
            mc.player.jumpFromGround();
        }

        if (mc.player.isInWater()) {
            waterTicks = 10;
        } else if (waterTicks > 0) {
            waterTicks--;
        }
    }

    @EventHandler
    public void onSprint(EventSprint e) {
        if (mc.player == null) return;
        if (canStrafe() && HitAura.target != null) {
            e.setSprinting(true);
        }
    }

    @EventHandler
    public void onPacket(EventPacket e) {
        if (mc.player == null || !e.isReceive()) return;
        if (!boost.isSelected("Урон")) return;

        if (e.getPacket() instanceof ClientboundSetEntityMotionPacket pkt) {
            if (pkt.getId() != mc.player.getId()) return;
            if (mc.player.onGround()) return;

            Vec3 movement = pkt.getMovement();
            double vx = Math.abs(movement.x);
            double vz = Math.abs(movement.z);

            double speed = (vx + vz) / (velReduction.getValue() * 1000.0);
            speed = Math.min(speed, maxVelocitySpeed.getValue());
            oldSpeed = speed;

            e.cancel();
        }
    }
}
