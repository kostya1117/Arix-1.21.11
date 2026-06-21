package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import dev.tr7zw.entityculling.EntityCullingModBase;
import dev.tr7zw.entityculling.versionless.access.Cullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.render.RenderState;
import org.jspecify.annotations.Nullable;

public class BlockEntityRenderDispatcher implements ResourceManagerReloadListener {
    private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers = ImmutableMap.of();
    private final Font font;
    private final Supplier<EntityModelSet> entityModelSet;
    private Vec3 cameraPos;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemModelResolver itemModelResolver;
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderer;
    private final MaterialSet materials;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private BlockEntityRendererProvider.Context context;

    public BlockEntityRenderDispatcher(
        Font p_234432_,
        Supplier<EntityModelSet> p_234434_,
        BlockRenderDispatcher p_377332_,
        ItemModelResolver p_376400_,
        ItemRenderer p_378208_,
        EntityRenderDispatcher p_375551_,
        MaterialSet p_423710_,
        PlayerSkinRenderCache p_424668_
    ) {
        this.itemRenderer = p_378208_;
        this.itemModelResolver = p_376400_;
        this.entityRenderer = p_375551_;
        this.font = p_234432_;
        this.entityModelSet = p_234434_;
        this.blockRenderDispatcher = p_377332_;
        this.materials = p_423710_;
        this.playerSkinRenderCache = p_424668_;
    }

    public <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable BlockEntityRenderer<E, S> getRenderer(E p_112266_) {
        return (BlockEntityRenderer<E, S>)this.renderers.get(p_112266_.getType());
    }

    public <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable BlockEntityRenderer<E, S> getRenderer(S p_429174_) {
        return (BlockEntityRenderer<E, S>)this.renderers.get(p_429174_.blockEntityType);
    }

    public void prepare(Camera p_173566_) {
        this.cameraPos = p_173566_.position();
    }

    public <E extends BlockEntity, S extends BlockEntityRenderState> @Nullable S tryExtractRenderState(
            E blockEntity, float partialTick, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay
    ) {
        BlockEntityRenderer<E, S> renderer = this.getRenderer(blockEntity);

        if (!EntityCullingModBase.instance.config.skipBlockEntityCulling && renderer != null) {
            if (renderer.shouldRenderOffScreen()) {
                EntityCullingModBase.instance.renderedBlockEntities++;
            } else {
                var frustum = EntityCullingModBase.instance.frustum;
                if (EntityCullingModBase.instance.config.blockEntityFrustumCulling
                        && frustum != null
                        && !frustum.isVisible(EntityCullingModBase.instance.setupAABB(blockEntity, blockEntity.getBlockPos()))) {
                    EntityCullingModBase.instance.skippedBlockEntities++;
                    return null;
                }

                if (blockEntity instanceof Cullable cullable) {
                    if (!cullable.isForcedVisible() && cullable.isCulled()) {
                        EntityCullingModBase.instance.skippedBlockEntities++;
                        return null;
                    }

                    EntityCullingModBase.instance.renderedBlockEntities++;
                    cullable.setOutOfCamera(false);
                }
            }
        }

        if (renderer == null) {
            return null;
        } else if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return null;
        } else if (!renderer.shouldRender(blockEntity, this.cameraPos)) {
            return null;
        } else {
            Vec3 vec3 = this.cameraPos;
            S state = renderer.createRenderState();
            renderer.extractRenderState(blockEntity, state, partialTick, vec3, overlay);
            return state;
        }
    }

    public <S extends BlockEntityRenderState> void submit(S p_425460_, PoseStack p_427977_, SubmitNodeCollector p_429959_, CameraRenderState p_430199_) {
        BlockEntityRenderer<?, S> blockentityrenderer = this.getRenderer(p_425460_);
        if (blockentityrenderer != null) {
            try {
                blockentityrenderer = CustomEntityModels.getBlockEntityRenderer(p_425460_.blockEntity, blockentityrenderer);
                p_425460_.blockEntityRenderer = blockentityrenderer;
                BlockEntityRenderState blockentityrenderstate = RenderState.setBlockEntityRenderState(p_425460_);
                blockentityrenderer.submit(p_425460_, p_427977_, p_429959_, p_430199_);
                RenderState.setBlockEntityRenderState(blockentityrenderstate);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Block Entity");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block Entity Details");
                p_425460_.fillCrashReportCategory(crashreportcategory);
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_173563_) {
        BlockEntityRendererProvider.Context blockentityrendererprovider$context = new BlockEntityRendererProvider.Context(
            this, this.blockRenderDispatcher, this.itemModelResolver, this.itemRenderer, this.entityRenderer, this.entityModelSet.get(), this.font, this.materials, this.playerSkinRenderCache
        );
        this.context = blockentityrendererprovider$context;
        this.renderers = BlockEntityRenderers.createEntityRenderers(blockentityrendererprovider$context);
    }

    public BlockEntityRenderer getRenderer(BlockEntityType type) {
        return this.renderers.get(type);
    }

    public BlockEntityRendererProvider.Context getContext() {
        return this.context;
    }

    public Map<BlockEntityType, BlockEntityRenderer> getBlockEntityRenderMap() {
        if (this.renderers instanceof ImmutableMap) {
            this.renderers = new HashMap<>(this.renderers);
        }

        return (Map<BlockEntityType, BlockEntityRenderer>) (Map) this.renderers;
    }
}
