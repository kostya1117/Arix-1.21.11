package net.optifine.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.optifine.Config;

public class WorldUtils {
    public static int getDimensionId(Level world) {
        return world == null ? 0 : getDimensionId(world.dimension());
    }

    public static int getDimensionId(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return -1;
        } else if (dimension == Level.OVERWORLD) {
            return 0;
        } else {
            return dimension == Level.END ? 1 : 0;
        }
    }

    public static boolean isNether(Level world) {
        return world.dimension() == Level.NETHER;
    }

    public static boolean isOverworld(Level world) {
        ResourceKey<Level> resourcekey = world.dimension();
        return getDimensionId(resourcekey) == 0;
    }

    public static boolean isEnd(Level world) {
        return world.dimension() == Level.END;
    }

    public static float getCelestialAngle(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        float f = environmentattributeprobe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks);
        return f / 360.0F;
    }

    public static float getCelestialAngleRadians(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        float f = environmentattributeprobe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks);
        return (float)Math.toRadians(f);
    }

    public static MoonPhase getMoonPhase() {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        return environmentattributeprobe.getValue(EnvironmentAttributes.MOON_PHASE, 0.0F);
    }

    public static int getMoonPhaseInt() {
        return getMoonPhase().index();
    }

    public static int getSkyColor(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        return environmentattributeprobe.getValue(EnvironmentAttributes.SKY_COLOR, partialTicks);
    }

    public static int getFogColor(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        return environmentattributeprobe.getValue(EnvironmentAttributes.FOG_COLOR, partialTicks);
    }

    public static int getSkyLightColor(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        return environmentattributeprobe.getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks);
    }

    public static float getSkyLightFactor(float partialTicks) {
        EnvironmentAttributeProbe environmentattributeprobe = Config.getGameRenderer().getMainCamera().attributeProbe();
        return environmentattributeprobe.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);
    }
}
