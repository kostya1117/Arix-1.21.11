package ru.arixcompany.features.module.modules.render.cape;

import net.minecraft.util.Mth;
import ru.arixcompany.features.module.modules.render.Cape;
import ru.arixcompany.features.module.modules.render.cape.sim.BasicSimulation;
import ru.arixcompany.features.module.modules.render.cape.sim.StickSimulation;
import ru.arixcompany.features.module.modules.render.cape.sim.StickSimulation.Vector2;
import ru.arixcompany.features.module.modules.render.cape.sim.StickSimulation3d;
import ru.arixcompany.features.module.modules.render.cape.sim.StickSimulationDungeons;
import ru.arixcompany.features.module.modules.render.cape.util.Vector3;

public class CapeSimulation {

    public static BasicSimulation getOrCreate(BasicSimulation existing, int partCount) {
        if (existing == null || incorrectSimulation(existing)) {
            existing = createSimulation();
        }
        if (existing == null) return null;
        boolean dirty = existing.init(partCount);
        if (dirty) {
            existing.applyMovement(new Vector3(1f, 1f, 0));
            for (int i = 0; i < 5; i++) {
                update(existing, dummyPlayer(existing));
            }
        }
        return existing;
    }

    private static boolean incorrectSimulation(BasicSimulation sim) {
        CapeMovement style = Cape.getMovement();
        if (style == CapeMovement.BASIC_SIMULATION && sim.getClass() != StickSimulation.class) return true;
        if (style == CapeMovement.BASIC_SIMULATION_3D && sim.getClass() != StickSimulation3d.class) return true;
        if (style == CapeMovement.DUNGEONS && sim.getClass() != StickSimulationDungeons.class) return true;
        return false;
    }

    private static BasicSimulation createSimulation() {
        CapeMovement style = Cape.getMovement();
        if (style == CapeMovement.BASIC_SIMULATION) return new StickSimulation();
        if (style == CapeMovement.BASIC_SIMULATION_3D) return new StickSimulation3d();
        if (style == CapeMovement.DUNGEONS) return new StickSimulationDungeons();
        return null;
    }

    public static void update(BasicSimulation simulation, MinecraftPlayer player) {
        if (simulation == null || simulation.empty()) return;

        double d = player.getXCloak() - player.getX();
        double m = player.getZCloak() - player.getZ();
        float n = player.getYBodyRotO() + player.getYBodyRot() - player.getYBodyRotO();
        double o = Mth.sin(n * 0.017453292F);
        double p = -Mth.cos(n * 0.017453292F);

        int heightMul = Cape.getHeightMultiplier();
        int straveMul = Cape.getStraveMultiplier();
        int gravityVal = Cape.getGravity();

        if (player.isUnderWater()) heightMul *= 2;

        double fallHack = Mth.clamp((player.getYo() - player.getY()) * 10, 0, 1);
        if (player.isUnderWater()) {
            simulation.setGravity(gravityVal / 10f);
        } else {
            simulation.setGravity(gravityVal);
        }

        Vector3 gravity = new Vector3(0, -1, 0);
        Vector2 strave = new Vector2((float) (player.getX() - player.getXo()),
                (float) (player.getZ() - player.getZo()));
        strave.rotateDegrees(-player.getYRot());

        double changeX = (d * o + m * p) + fallHack
                + (player.isCrouching() && !simulation.isSneaking() ? 3 : 0);
        double changeY = ((player.getY() - player.getYo()) * heightMul)
                + (player.isCrouching() && !simulation.isSneaking() ? 1 : 0);
        double changeZ = -strave.x * straveMul;

        simulation.setSneaking(player.isCrouching());
        Vector3 change = new Vector3((float) changeX, (float) changeY, (float) changeZ);

        if (player.isVisuallySwimming()) {
            float rotation = player.getXRot() + 90;
            gravity.rotateDegrees(rotation);
            change.rotateDegrees(rotation);
        }
        simulation.setGravityDirection(gravity);
        simulation.applyMovement(change);
        simulation.simulate();
    }

    private static MinecraftPlayer dummyPlayer(BasicSimulation sim) {
        return new MinecraftPlayer() {
            public boolean isVisuallySwimming() { return false; }
            public float getXRot() { return 0; }
            public boolean isCrouching() { return false; }
            public double getY() { return 0; }
            public float getYRot() { return 0; }
            public double getZ() { return 0; }
            public double getX() { return 0; }
            public boolean isUnderWater() { return false; }
            public double getXCloak() { return 0; }
            public double getZCloak() { return 0; }
            public float getYBodyRotO() { return 0; }
            public float getYBodyRot() { return 0; }
            public double getYo() { return 0; }
            public double getXo() { return 0; }
            public double getZo() { return 0; }
        };
    }
}
