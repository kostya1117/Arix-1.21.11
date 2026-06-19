package astryxion.chunkanimator.config;

import astryxion.chunkanimator.handler.AnimationContext;
import astryxion.chunkanimator.handler.AnimationHandler;
import astryxion.chunkanimator.handler.PreRenderContext;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static astryxion.chunkanimator.handler.AnimationHandler.*;

/**
 * @author Harley O'Connor
 */
public enum AnimationMode {
	BELOW(context -> new Offset(
			0,
			-Math.abs(context.origin().getY()) + getFunctionValue(
					(float) context.timeDif(),
					0,
					Math.abs(context.origin().getY()),
					ChunkAnimatorConfig.ANIMATION_DURATION.get()
			),
			0
	)),
	ABOVE(context -> new Offset(
			0,
			context.levelContext().maxY() - Math.abs(context.origin().getY()) - getFunctionValue(
					(float) context.timeDif(),
					0,
					context.levelContext().maxY() - Math.abs(context.origin().getY()),
					ChunkAnimatorConfig.ANIMATION_DURATION.get()
			),
			0
	)),
	HYBRID(context -> {
		if (context.origin().getY() < context.levelContext().horizonHeight()) {
			return BELOW.contextConsumer.apply(context);
		} else {
			return ABOVE.contextConsumer.apply(context);
		}
	}),
	HORIZONTAL_SLIDE(context -> {
		final var chunkFacing = context.animationData().chunkFacing;
		if (chunkFacing == null) {
			return Offset.ZERO;
		}

		final var vec = chunkFacing.getUnitVec3i();
		final var mod = -(200F - getFunctionValue((float) context.timeDif(), 0, 200, ChunkAnimatorConfig.ANIMATION_DURATION.get()));

		return new Offset(vec.getX() * mod, 0, vec.getZ() * mod);
	}),
	HORIZONTAL_SLIDE_ALTERNATE(
			(context, data) ->
					data.chunkFacing = getChunkFacing(getZeroedPlayerPos(Objects.requireNonNull(Minecraft.getInstance().player))
							.subtract(getZeroedCenteredChunkPos(context.origin()))
					),
			HORIZONTAL_SLIDE.contextConsumer
	);

	private final BiConsumer<PreRenderContext, AnimationHandler.AnimationData> prepareConsumer;
	private final Function<AnimationContext, Offset> contextConsumer;

	AnimationMode(Function<AnimationContext, Offset> contextConsumer) {
		this((context, data) -> {}, contextConsumer);
	}

	AnimationMode(BiConsumer<PreRenderContext, AnimationHandler.AnimationData> prepareConsumer, Function<AnimationContext, Offset> contextConsumer) {
		this.prepareConsumer = prepareConsumer;
		this.contextConsumer = contextConsumer;
	}

	public BiConsumer<PreRenderContext, AnimationHandler.AnimationData> prepareConsumer() {
		return prepareConsumer;
	}

	public Function<AnimationContext, Offset> contextConsumer() {
		return contextConsumer;
	}

	private static float getFunctionValue(final float t, @SuppressWarnings("SameParameterValue") final float b, final float c, final float d) {
		return ChunkAnimatorConfig.EASING_FUNCTION.get().easeOutFunc().apply(t, b, c, d);
	}
}

