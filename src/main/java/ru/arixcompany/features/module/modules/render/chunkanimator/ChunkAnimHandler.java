package ru.arixcompany.features.module.modules.render.chunkanimator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import ru.arixcompany.features.module.modules.render.ChunkAnimator;
import ru.arixcompany.utils.animation.Interpolation;

import java.util.HashMap;
import java.util.Map;

public class ChunkAnimHandler {

    private static final Map<Long, ChunkAnimData> animations = new HashMap<>();
    private static int horizonY = -1;

    public static void register(BlockPos origin) {
        if (ChunkAnimator.isDisableAroundPlayer() && isNearPlayer(origin)) return;
        long key = origin.asLong();
        if (animations.containsKey(key)) return;
        animations.put(key, new ChunkAnimData(
            Util.getMillis(),
            ChunkAnimator.getMode(),
            ChunkAnimator.getDuration(),
            ChunkAnimator.getEasing()
        ));
    }

    public static Offset getOffset(BlockPos origin, double camX, double camY, double camZ) {
        long key = origin.asLong();
        ChunkAnimData data = animations.get(key);
        if (data == null) return Offset.ZERO;

        long elapsed = Util.getMillis() - data.startTime;
        if (elapsed >= data.duration) {
            animations.remove(key);
            return Offset.ZERO;
        }

        float progress = (float) elapsed / data.duration;
        double eased = Interpolation.getRawCoefficient(progress, data.easing, Interpolation.Mode.OUT);
        float multiplier = 1.0f - (float) eased;

        return computeOffset(origin, data.animMode, multiplier, camX, camY, camZ);
    }

    private static Offset computeOffset(BlockPos origin, AnimationMode mode, float multiplier, double camX, double camY, double camZ) {
        final float startHeight = 20f;

        return switch (mode) {
            case BELOW -> new Offset(0, (int) (startHeight * multiplier), 0);
            case ABOVE -> new Offset(0, (int) (-startHeight * multiplier), 0);
            case HYBRID -> {
                if (horizonY < 0) updateHorizon();
                boolean below = origin.getY() + 8 < horizonY;
                yield below
                    ? new Offset(0, (int) (startHeight * multiplier), 0)
                    : new Offset(0, (int) (-startHeight * multiplier), 0);
            }
            case HORIZONTAL_SLIDE, HORIZONTAL_SLIDE_ALTERNATE -> {
                Direction dir = getChunkFacing(origin, camX, camZ, mode == AnimationMode.HORIZONTAL_SLIDE_ALTERNATE);
                int ox = 0, oz = 0;
                if (dir.getAxis() == Direction.Axis.X) {
                    ox = dir.getStepX() * (int) (startHeight * multiplier);
                } else if (dir.getAxis() == Direction.Axis.Z) {
                    oz = dir.getStepZ() * (int) (startHeight * multiplier);
                }
                yield new Offset(ox, 0, oz);
            }
        };
    }

    private static Direction getChunkFacing(BlockPos origin, double camX, double camZ, boolean alternate) {
        int dx = origin.getX() + 8 - (int) camX;
        int dz = origin.getZ() + 8 - (int) camZ;
        if (alternate) {
            dx = -dx;
            dz = -dz;
        }
        return Direction.getNearest(dx, 0, dz, Direction.NORTH);
    }

    private static boolean isNearPlayer(BlockPos origin) { 
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        int dx = Math.abs(origin.getX() - (int) player.getX());
        int dz = Math.abs(origin.getZ() - (int) player.getZ());
        return dx <= 32 && dz <= 32;
    }

    private static void updateHorizon() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            horizonY = (level.getMinY() + level.getMaxY()) / 2;
        } else {
            horizonY = 0;
        }
    }

    public static void clear() {
        animations.clear();
        horizonY = -1;
    }

    public static class Offset {
        public static final Offset ZERO = new Offset(0, 0, 0);

        public final int x;
        public final int y;
        public final int z;

        public Offset(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private record ChunkAnimData(long startTime, AnimationMode animMode, int duration, Interpolation.Curve easing) {}
}
