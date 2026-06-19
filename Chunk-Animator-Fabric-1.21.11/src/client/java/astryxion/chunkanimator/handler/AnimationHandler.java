package astryxion.chunkanimator.handler;

import astryxion.chunkanimator.config.AnimationMode;
import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles setting up and rendering the animations.
 *
 * @author lumien231
 */
public final class AnimationHandler {

	private final Minecraft mc = Minecraft.getInstance();
	private final Map<Long, AnimationData> timeStamps = new ConcurrentHashMap<>();

	public Offset preRender(PreRenderContext context) {
		final long key = context.origin().asLong();
		final var animationData = timeStamps.get(key);

		if (animationData == null) {
			return Offset.ZERO;
		}

		final var mode = ChunkAnimatorConfig.MODE.get();
		final int animationDuration = ChunkAnimatorConfig.ANIMATION_DURATION.get();

		long time = animationData.timeStamp;

		// If preRender hasn't been called on this chunk yet, prepare to start the animation.
		if (time == -1L) {
			time = System.currentTimeMillis();
			animationData.timeStamp = time;
			mode.prepareConsumer().accept(context, animationData);
		}

		final long timeDif = System.currentTimeMillis() - time;

		if (timeDif < animationDuration) {
			return ChunkAnimatorConfig.MODE.get().contextConsumer().apply(new AnimationContext(
					animationData,
					context.origin(),
					timeDif,
					AnimationContext.LevelContext.from(Objects.requireNonNull(this.mc.level))
			));
		} else {
			// Keep entry so recompiles don't restart the animation.
			return Offset.ZERO;
		}
	}

	public void setOrigin(final BlockPos pos) {
		if (this.mc.player == null) {
			return;
		}

		final BlockPos zeroedPlayerPos = getZeroedPlayerPos(this.mc.player);
		final BlockPos zeroedCenteredChunkPos = getZeroedCenteredChunkPos(pos);
		final long key = pos.asLong();

		if (!ChunkAnimatorConfig.DISABLE_AROUND_PLAYER.get() || zeroedPlayerPos.distSqr(zeroedCenteredChunkPos) > (64 * 64)) {
			final Direction facing = ChunkAnimatorConfig.MODE.get() == AnimationMode.HORIZONTAL_SLIDE
					? getChunkFacing(zeroedPlayerPos.subtract(zeroedCenteredChunkPos))
					: null;
			timeStamps.putIfAbsent(key, new AnimationData(-1L, facing));
		} else {
			timeStamps.remove(key);
		}
	}

	/**
	 * Gets the given player's position, setting their {@code y-coordinate} to {@code 0}.
	 *
	 * @param player The {@link LocalPlayer} instance.
	 * @return The zeroed {@link BlockPos}.
	 */
	public static BlockPos getZeroedPlayerPos(final LocalPlayer player) {
		final BlockPos playerPos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
		return playerPos.offset(0, -player.getBlockY(), 0);
	}

	/**
	 * Gets the given {@link BlockPos} for the chunk, setting its {@code y-coordinate} to
	 * {@code 0} and offsetting its {@code x} and {@code y-coordinate} to by {@code 8}.
	 *
	 * @param position The {@link BlockPos} of the chunk.
	 * @return The zeroed, centered {@link BlockPos}.
	 */
	public static BlockPos getZeroedCenteredChunkPos(final BlockPos position) {
		return position.offset(8, -position.getY(), 8);
	}

	/**
	 * Gets the direction the chunk is facing based on the given {@link Vec3i}
	 * from the relevant position to the chunk.
	 *
	 * @param dif The {@link Vec3i} distance from the relevant position to the chunk.
	 * @return The {@link Direction} of the chunk relative to the {@code dif}.
	 */
	public static Direction getChunkFacing(final Vec3i dif) {
		final int difX = Math.abs(dif.getX());
		final int difZ = Math.abs(dif.getZ());

		return difX > difZ ? dif.getX() > 0 ? Direction.EAST : Direction.WEST : dif.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	public void clear() {
		// These should be cleared by GC, but just in case.
		this.timeStamps.clear();
	}

	public record Offset(float x, float y, float z) {
		public static final Offset ZERO = new Offset(0, 0, 0);
	}

	public static class AnimationData {
		public long timeStamp;
		public Direction chunkFacing;

		public AnimationData(final long timeStamp, final Direction chunkFacing) {
			this.timeStamp = timeStamp;
			this.chunkFacing = chunkFacing;
		}
	}
}

