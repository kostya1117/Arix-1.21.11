package ru.arixcompany.utils.math;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.utils.IMinecraft;

import java.util.Set;

public class PredictUtils {

    public static Vec3 predict(LivingEntity target, int ticks) {
        if (ticks <= 0) {
            return target.position();
        }

        if (!target.isFallFlying()) {
            return target.getEyePosition(1.0F);
        }

        Vec3 pos = target.position();
        Vec3 vel = target.getDeltaMovement();

        for (int i = 0; i < ticks; i++) {
            vel = target.updateFallFlyingMovement(vel);
            pos = pos.add(vel);
        }

        return pos;
    }
    public static Vec3 predict2(LivingEntity target, int ticks) {
        if (ticks <= 0) {
            return target.position();
        }

        if (!target.isFallFlying()) {
            return target.getEyePosition(1.0F);
        }

        Vec3 pos = target.position();
        Vec3 vel = target.getDeltaMovement();

        pos = pos.add(vel.scale(ticks));
        vel = target.updateFallFlyingMovement(pos);
        pos = pos.add(vel);

        return pos;
    }
}
//public final class PredictUtils implements IMinecraft {
//
//    public PredictUtils() {
//        EventRepo.register(this);
//    }
//
//    private static final Int2ObjectMap<State> states = new Int2ObjectOpenHashMap<>();
//
//    @EventHandler
//    public void onPacket(EventPacket e) {
//        if (!e.isReceive()) return;
//
//        Packet<?> p = e.getPacket();
//
//        if (p instanceof ClientboundMoveEntityPacket move) {
//            int id = move.entityId;
//            Entity ent = mc.level.getEntity(id);
//            if (!(ent instanceof LivingEntity)) return;
//
//            Vec3 pos = ent.position().add(
//                    move.getXa() / 4096.0,
//                    move.getYa() / 4096.0,
//                    move.getZa() / 4096.0
//            );
//
//            states.computeIfAbsent(id, k -> new State()).push(pos);
//        }
//    }
//
//    public static Vec3 predict(LivingEntity target, int ticks) {
//        if (ticks <= 0) return target.position();
//        if (!target.isFallFlying()) return target.getEyePosition(1.0F);
//
//        State st = states.get(target.getId());
//
//        Vec3 pos = st != null && st.hasSample ? st.serverPos : target.position();
//        Vec3 vel = st != null && st.hasSample ? st.serverVelTick : target.getDeltaMovement();
//
//        for (int i = 0; i < ticks; i++) {
//            vel = target.updateFallFlyingMovement(vel);
//            pos = pos.add(vel);
//        }
//
//        return pos;
//    }
//
//    private static final class State {
//        Vec3 serverPos = Vec3.ZERO;
//        Vec3 serverVelTick = Vec3.ZERO;
//        boolean hasSample;
//        long lastMs;
//
//        void push(Vec3 newPos) {
//            long now = System.currentTimeMillis();
//
//            if (hasSample) {
//                double dtSec = Math.max(0.001, (now - lastMs) / 1000.0);
//                Vec3 velSec = newPos.subtract(serverPos).scale(1.0 / dtSec);
//                serverVelTick = velSec.scale(1.0 / 20.0);
//            } else {
//                hasSample = true;
//                serverVelTick = Vec3.ZERO;
//            }
//
//            serverPos = newPos;
//            lastMs = now;
//        }
//    }
//}

