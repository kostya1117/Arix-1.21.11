package ru.arixcompany.features.module.modules.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ClickType;
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

public class Strafe extends Module {

    private final SelectSetting mode = new SelectSetting("Режим")
            .value("Обычный", "Matrix");

    private final SelectSetting boost = new SelectSetting("Буст")
            .value("Нет", "Элитра", "Урон");

    private final ValueSetting setSpeed = new ValueSetting("Скорость элитры")
            .range(0.0f, 2.0f).setValue(1.3f).setStep(0.05f)
            .visible(() -> boost.isSelected("Элитра"));

    private final ValueSetting velReduction = new ValueSetting("Снижение скорости")
            .range(0.1f, 10.0f).setValue(6.0f).setStep(0.1f)
            .visible(() -> boost.isSelected("Урон"));

    private final ValueSetting maxVelocitySpeed = new ValueSetting("Макс. скорость")
            .range(0.1f, 2.0f).setValue(0.8f).setStep(0.05f)
            .visible(() -> boost.isSelected("Урон"));

    public static double oldSpeed;
    public static double contextFriction;
    public static boolean disabled;

    private static long disableTime;
    private static int noSlowTicks;
    private double savedFov;

    public Strafe() {
        super("Strafe", Category.Movement);
        setup(mode, boost, setSpeed, velReduction, maxVelocitySpeed);
    }

    @Override
    public void activate() {
        super.activate();
        oldSpeed = 0.0;
        savedFov = mc.options.fovEffectScale().get();
        mc.options.fovEffectScale().set(0.0);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        mc.options.fovEffectScale().set(savedFov);
    }

    private boolean canStrafe() {
        if (mc.player == null) return false;
        if (mc.player.isCrouching() || mc.player.isInLava() || mc.player.isInWater() || mc.player.getAbilities().flying) return false;
        return true;
    }

    private double calculateSpeed(double motionY) {
        if (mode.isSelected("Matrix")) {
            return 0.25 - Math.random() * 0.001;
        }

        float speedAttr = getAIMoveSpeed();

        BlockPos groundBp = new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() - 0.01), (int) mc.player.getZ());
        float slipperiness = mc.level.getBlockState(groundBp).getBlock().getFriction() * 0.91f;

        float n6 = (mc.player.hasEffect(MobEffects.JUMP_BOOST) && mc.player.isUsingItem())
                ? 0.88f
                : (float) (oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91f);
        if (mc.player.onGround()) n6 = slipperiness;

        float n7 = (float) (0.1631f / Math.pow(n6, 3.0f));
        float n8;

        if (mc.player.onGround()) {
            n8 = speedAttr * n7;
            if (motionY > 0) {
                boolean elytraBoost = boost.isSelected("Элитра")
                        && InventoryUtility.getElytra() != -1
                        && disabled
                        && System.currentTimeMillis() - disableTime < 300;
                n8 += elytraBoost ? 0.65f : 0.2f;
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

    private float getAIMoveSpeed() {
        boolean prev = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getSpeed() * 1.3f;
        mc.player.setSprinting(prev);
        return speed;
    }

    @EventHandler
    public void onPreTick(EventPreTick e) {
        if (mc.player == null || mc.level == null) return;

        int elytraSlot = InventoryUtility.getElytra();

        if (boost.isSelected("Элитра") && elytraSlot != -1) {
            if (!mc.player.onGround() && mc.player.fallDistance > 0 && !disabled) {
                doElytraBoost(elytraSlot);
            }
        }

        if (!canStrafe()) {
            oldSpeed = 0;
            return;
        }

        double motionY = mc.player.getDeltaMovement().y;

        if (boost.isSelected("Элитра") && elytraSlot != -1) {
            if (MoveUtils.isMoving() && !mc.player.onGround()
                    && mc.level.getBlockCollisions(mc.player,
                    mc.player.getBoundingBox().move(0, motionY, 0)).iterator().hasNext()
                    && disabled) {
                oldSpeed = setSpeed.getValue();
            }
        }

        if (MoveUtils.isMoving()) {
            double speed = calculateSpeed(motionY);
            double[] motions = forward(speed);
            mc.player.setDeltaMovement(motions[0], motionY, motions[1]);
        } else {
            oldSpeed = 0;
            mc.player.setDeltaMovement(0, motionY, 0);
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.level == null) return;
        oldSpeed = Math.hypot(
                mc.player.getX() - mc.player.xOld,
                mc.player.getZ() - mc.player.zOld) * contextFriction;
    }

    @EventHandler
    public void onSprint(EventSprint e) {
        if (mc.player == null) return;
        if (canStrafe() && MoveUtils.isMoving()) {
            e.setSprinting(true);
        }
    }

    @EventHandler
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.isReceive()) {
            if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
                oldSpeed = 0;
            }

            if (boost.isSelected("Урон")
                    && e.getPacket() instanceof ClientboundSetEntityMotionPacket pkt) {
                if (pkt.getId() != mc.player.getId()) return;
                if (mc.player.onGround()) return;

                Vec3 movement = pkt.getMovement();
                double speed = (Math.abs(movement.x) + Math.abs(movement.z)) / (velReduction.getValue() * 1000.0);
                oldSpeed = Math.min(speed, maxVelocitySpeed.getValue());

                e.cancel();
            }
        }
    }

    private static void doElytraBoost(int elytraSlot) {
        if (elytraSlot == -1) return;
        if (System.currentTimeMillis() - disableTime <= 190L) return;

        if (elytraSlot != -2) {
            mc.gameMode.handleInventoryMouseClick(0, elytraSlot, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
        }

        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

        if (elytraSlot != -2) {
            mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(0, elytraSlot, 1, ClickType.PICKUP, mc.player);
        }

        disableTime = System.currentTimeMillis();
        disabled = true;
    }

    private static double[] forward(double speed) {
        float yaw = (float) Math.toRadians(MoveUtils.getPlayerDirection());
        return new double[]{
                -Math.sin(yaw) * speed,
                Math.cos(yaw) * speed
        };
    }
}