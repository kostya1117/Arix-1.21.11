package astryxion.chunkanimator.handler;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

/**
 * Holds context gathered from the {@link LevelRenderer} required for pre-rendering
 * the chunk.
 *
 * @author Harley O'Connor
 */
public record PreRenderContext(
		BlockPos origin
) { }

