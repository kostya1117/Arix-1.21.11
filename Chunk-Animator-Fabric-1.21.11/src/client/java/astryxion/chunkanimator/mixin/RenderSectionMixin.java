package astryxion.chunkanimator.mixin;

import astryxion.chunkanimator.ChunkAnimator;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Harley O'Connor
 */
@Mixin(LevelRenderer.class)
public final class RenderSectionMixin {

	@Inject(method = "addRecentlyCompiledSection", at = @At("TAIL"))
	private void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection renderSection, CallbackInfo ci) {
		ChunkAnimator.instance.animationHandler.setOrigin(renderSection.getRenderOrigin());
	}
}

