package astryxion.chunkanimator;

import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

/**
 * Client-only Fabric entrypoint.
 *
 * @author lumien231
 */
public final class ChunkAnimator implements ClientModInitializer {

	public static final String MOD_ID = "chunkanimator";

	public static ChunkAnimator instance;

	public AnimationHandler animationHandler;

	@Override
	public void onInitializeClient() {
		instance = this;

		ChunkAnimatorConfig.load();
		this.animationHandler = new AnimationHandler();

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> this.animationHandler.clear());
	}
}

