package astryxion.chunkanimator;

import astryxion.chunkanimator.config.ChunkAnimatorConfig;
import astryxion.chunkanimator.handler.AnimationHandler;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.world.EventWorldChange;

/**
 * Client-only Fabric entrypoint.
 *
 * @author lumien231
 */
public final class ChunkAnimator {

	public ChunkAnimator(){
		EventRepo.register(this);
	}
	public static final String MOD_ID = "chunkanimator";

	public static ChunkAnimator instance;

	public AnimationHandler animationHandler;

	public void init() {
		instance = this;

		ChunkAnimatorConfig.load();
		this.animationHandler = new AnimationHandler();
	}
	@EventHandler
	public void onWorldChange(EventWorldChange eventWorldChange){
		this.animationHandler.clear();
	}
}

