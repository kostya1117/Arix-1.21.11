package ru.arixcompany.features.module.modules.movement;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventPriority;
import ru.arixcompany.features.event.player.EventMotionPost;
import ru.arixcompany.features.event.player.EventMoveInput;
import ru.arixcompany.features.event.player.EventOnMovePost;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RequestHandler;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.MoveUtils;

public class Speed extends Module implements RequestHandler.RequestProvider {
    private final SelectSetting modespeed = new SelectSetting("Мод")
            .value("RW 06.06.2025");

    int ticks;
    int groundTicks;

    public Speed() {
        super("Speed", Category.Movement);
    }

    @EventHandler
    public void movePost(EventOnMovePost event) {
        if (mc.player == null || mc.level == null) return;

        //Timer.INSTANCE.requestTimerSpeed(1.7F, EventPriority.NORMAL.getValue(), this);
        mc.deltaTracker.deltaTicks = 1.7F;
       // mc.player.setSpeed(1.3F);

        if (ticks > 3) {
            double bst;
            if (ticks % 2 == 0) {
                mc.player.addDeltaMovement(new Vec3(0, 0.03F, 0));
                if (mc.player.onGround()) {
                    bst = 0.085;
                } else {
                    bst = 0.03;
                }
            } else {
                bst = 0.03;
            }

            double yaw = Math.toRadians(MoveUtils.getDirection());
            double xt = -Math.sin(yaw);
            double zt = Math.cos(yaw);

            if (MoveUtils.getDirection() == -1.0F) {
                xt = 0.0;
                zt = 0.0;
            }

            mc.player.addDeltaMovement(new Vec3(xt * bst, 0,zt *bst));
        }

        ticks++;
    }
//
    @EventHandler
    public void update(EventMoveInput event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.player.verticalCollision) groundTicks++;
        else groundTicks = 0;

        if (groundTicks >= 1) {
            mc.player.jumpFromGround();
        }
    }

    @EventHandler
    public void motionPost(EventMotionPost event) {
        if (mc.player == null || mc.level == null) return;

        if (ticks % 2 == 0) {
          // Timer.INSTANCE.requestTimerSpeed(0.3F, EventPriority.NORMAL.getValue(), this);
            mc.deltaTracker.deltaTicks = 0.3f;
          //  mc.player.setSpeed(0.5F);
            mc.getConnection().sendSilent(
                    new ServerboundPlayerCommandPacket(
                            mc.player,
                            ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
                    )
            );
        }
    }

    @EventHandler
    public void packet(EventPacket e) {
        if (mc.player == null || mc.level == null) return;

        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            if (ticks % 2 == 1) {
                ticks++;
            }
          // Timer.INSTANCE.requestTimerSpeed(1.0f, EventPriority.NORMAL.getValue(), this);
            mc.deltaTracker.deltaTicks = 1.0f;
           // mc.player.setSpeed(1F);
        }
    }

    @Override
    public void deactivate() {
        if (mc.player == null || mc.level == null) return;
        ticks = 0;
        groundTicks = 0;
        mc.deltaTracker.deltaTicks = 1;
     //   mc.player.setSpeed(1F);
        super.deactivate();
    }

    @Override
    public boolean isRunning() {
        return this.isState() && mc.player != null && mc.level != null;
    }
}