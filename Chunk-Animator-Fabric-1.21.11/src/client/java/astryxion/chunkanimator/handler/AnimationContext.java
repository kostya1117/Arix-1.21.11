package astryxion.chunkanimator.handler;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * Holds context parameters required for animating chunks.
 *
 * @author Harley O'Connor
 */
public record AnimationContext(
		AnimationHandler.AnimationData animationData,
		BlockPos origin,
		long timeDif,
		LevelContext levelContext
) {

	public record LevelContext(
			double horizonHeight,
			int minY,
			int maxY
	) {

		public static LevelContext from(ClientLevel level) {
			return new LevelContext(level.getLevelData().getHorizonHeight(level), level.dimensionType().minY(),
					level.dimensionType().minY() + level.dimensionType().height());
		}
	}
}

