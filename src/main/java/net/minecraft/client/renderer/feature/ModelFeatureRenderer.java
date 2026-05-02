package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.render.RenderState;
import net.optifine.render.RenderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.util.TextureUtils;
import org.joml.Vector3f;

public class ModelFeatureRenderer {
    private final PoseStack poseStack = new PoseStack();
    private boolean isShaders;
    private Map<RenderType, List<SubmitNodeStorage.ModelSubmit>> emissiveSubmits = new HashMap<>();

    public void render(
        SubmitNodeCollection p_424122_, MultiBufferSource.BufferSource p_425574_, OutlineBufferSource p_424002_, MultiBufferSource.BufferSource p_428766_
    ) {
        this.isShaders = Config.isShaders();
        ModelFeatureRenderer.Storage modelfeaturerenderer$storage = p_424122_.getModelSubmits();
        this.renderBatch(p_425574_, p_424002_, modelfeaturerenderer$storage.opaqueModelSubmits, p_428766_);
        modelfeaturerenderer$storage.translucentModelSubmits.sort(Comparator.comparingDouble(submit2In -> -submit2In.position().lengthSquared()));
        this.renderTranslucents(p_425574_, p_424002_, modelfeaturerenderer$storage.translucentModelSubmits, p_428766_);
    }

    private void renderTranslucents(
        MultiBufferSource.BufferSource p_425035_,
        OutlineBufferSource p_424488_,
        List<SubmitNodeStorage.TranslucentModelSubmit<?>> p_423495_,
        MultiBufferSource.BufferSource p_430495_
    ) {
        for (SubmitNodeStorage.TranslucentModelSubmit<?> translucentmodelsubmit : p_423495_) {
            RenderType rendertype = translucentmodelsubmit.renderType();
            if (EmissiveTextures.isActive()) {
                EmissiveTextures.beginRender();
                Identifier identifier = rendertype.getTextureLocation();
                if (identifier != null) {
                    EmissiveTextures.getEmissiveTexture(identifier);
                }
            }

            this.renderModel(
                translucentmodelsubmit.modelSubmit(),
                translucentmodelsubmit.renderType(),
                p_425035_.getBuffer(translucentmodelsubmit.renderType()),
                p_424488_,
                p_430495_
            );
            if (EmissiveTextures.isActive()) {
                if (EmissiveTextures.hasEmissive()) {
                    if (rendertype.isEntitySolid()) {
                        RenderUtils.flushRenderBuffers();
                        rendertype = RenderTypes.entityCutout(rendertype.getTextureLocation());
                    }

                    EmissiveTextures.beginRenderEmissive();
                    Identifier identifier2 = rendertype.getTextureLocation();
                    if (identifier2 != null) {
                        Identifier identifier1 = EmissiveTextures.getEmissiveTexture(identifier2);
                        if (identifier1 != identifier2) {
                            rendertype = rendertype.getTextured(identifier1);
                        }
                    }

                    VertexConsumer vertexconsumer = p_425035_.getBuffer(rendertype);
                    this.renderModel(translucentmodelsubmit.modelSubmit(), rendertype, vertexconsumer, p_424488_, p_430495_);
                    EmissiveTextures.endRenderEmissive();
                }

                EmissiveTextures.endRender();
            }
        }
    }

    private void renderBatch(
        MultiBufferSource.BufferSource p_429497_,
        OutlineBufferSource p_425494_,
        Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> p_425382_,
        MultiBufferSource.BufferSource p_430884_
    ) {
        Iterable<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> iterable;
        if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
            List<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> list = new ArrayList<>(p_425382_.entrySet());
            Collections.shuffle(list);
            iterable = list;
        } else {
            iterable = p_425382_.entrySet();
        }

        for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : iterable) {
            VertexConsumer vertexconsumer = p_429497_.getBuffer(entry.getKey());

            for (SubmitNodeStorage.ModelSubmit<?> modelsubmit : entry.getValue()) {
                RenderType rendertype = entry.getKey();
                if (EmissiveTextures.isActive()) {
                    EmissiveTextures.beginRender();
                    Identifier identifier = rendertype.getTextureLocation();
                    if (identifier != null) {
                        EmissiveTextures.getEmissiveTexture(identifier);
                    }
                }

                this.renderModel(modelsubmit, entry.getKey(), vertexconsumer, p_425494_, p_430884_);
                if (EmissiveTextures.isActive()) {
                    if (EmissiveTextures.hasEmissive()) {
                        List<SubmitNodeStorage.ModelSubmit> list2 = this.emissiveSubmits.computeIfAbsent(rendertype, x -> new ArrayList<>());
                        list2.add(modelsubmit);
                    }

                    EmissiveTextures.endRender();
                }
            }
        }

        if (!this.emissiveSubmits.isEmpty()) {
            for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit>> entry1 : this.emissiveSubmits.entrySet()) {
                RenderType rendertype1 = entry1.getKey();
                if (rendertype1.isEntitySolid()) {
                    RenderUtils.flushRenderBuffers();
                    rendertype1 = RenderTypes.entityCutout(rendertype1.getTextureLocation());
                }

                EmissiveTextures.beginRender();
                EmissiveTextures.beginRenderEmissive();
                Identifier identifier1 = rendertype1.getTextureLocation();
                if (identifier1 != null) {
                    Identifier identifier2 = EmissiveTextures.getEmissiveTexture(identifier1);
                    if (identifier2 != identifier1) {
                        rendertype1 = rendertype1.getTextured(identifier2);
                    }
                }

                VertexConsumer vertexconsumer1 = p_429497_.getBuffer(rendertype1);
                List<SubmitNodeStorage.ModelSubmit> list1 = entry1.getValue();

                for (int i = 0; i < list1.size(); i++) {
                    SubmitNodeStorage.ModelSubmit submitnodestorage$modelsubmit = list1.get(i);
                    this.renderModel(submitnodestorage$modelsubmit, rendertype1, vertexconsumer1, p_425494_, p_430884_);
                }

                EmissiveTextures.endRenderEmissive();
                EmissiveTextures.endRender();
            }

            this.emissiveSubmits.clear();
        }
    }

    private <S> void renderModel(
        SubmitNodeStorage.ModelSubmit<S> p_426187_,
        RenderType p_450242_,
        VertexConsumer p_428121_,
        OutlineBufferSource p_422455_,
        MultiBufferSource.BufferSource p_428832_
    ) {
        this.poseStack.pushPose();
        this.poseStack.last().set(p_426187_.pose());
        Model<? super S> model = p_426187_.model();
        Object object = p_426187_.renderState();
        if (object instanceof EntityRenderState entityrenderstate) {
            RenderState.setEntityRenderState(entityrenderstate);
            RenderState.setRenderOverlayEyes(this.isRenderOverlayEyes(entityrenderstate, p_450242_));
            if (this.isShaders) {
                Shaders.nextEntity(entityrenderstate.entity);
                if (RenderState.isRenderOverlayEyes()) {
                    Shaders.beginSpiderEyes();
                }

                RenderState.setRenderItemHead(this.isRenderItemHead(entityrenderstate));
                if (entityrenderstate instanceof LivingEntityRenderState livingentityrenderstate) {
                    if (livingentityrenderstate.hasRedOverlay) {
                        Shaders.setEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
                    }

                    if (livingentityrenderstate.overlayProgress > 0.0F) {
                        Shaders.setEntityColor(
                            livingentityrenderstate.overlayProgress, livingentityrenderstate.overlayProgress, livingentityrenderstate.overlayProgress, 0.5F
                        );
                    }
                }
            }
        } else if (object instanceof BlockEntityRenderState blockentityrenderstate) {
            RenderState.setBlockEntityRenderState(blockentityrenderstate);
            if (this.isShaders) {
                Shaders.nextBlockEntity(blockentityrenderstate.blockEntity);
            }
        }

        TextureAtlasSprite textureatlassprite = TextureUtils.getCustomSprite(p_426187_.sprite());
        VertexConsumer vertexconsumer = textureatlassprite == null ? p_428121_ : textureatlassprite.wrap(p_428121_);
        model.setupAnim(p_426187_.state());
        if (EmissiveTextures.isRenderEmissive()) {
            model.renderToBuffer(this.poseStack, vertexconsumer, LightTexture.MAX_BRIGHTNESS, p_426187_.overlayCoords(), p_426187_.tintedColor());
        } else {
            model.renderToBuffer(this.poseStack, vertexconsumer, p_426187_.lightCoords(), p_426187_.overlayCoords(), p_426187_.tintedColor());
        }

        if (this.isShaders) {
            if (RenderState.isRenderOverlayEyes()) {
                Shaders.endSpiderEyes();
            }

            Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        if (!EmissiveTextures.isRenderEmissive()) {
            if (p_426187_.outlineColor() != 0 && (p_450242_.outline().isPresent() || p_450242_.isOutline())) {
                p_422455_.setColor(p_426187_.outlineColor());
                VertexConsumer vertexconsumer1 = p_422455_.getBuffer(p_450242_);
                model.renderToBuffer(
                    this.poseStack,
                    textureatlassprite == null ? vertexconsumer1 : textureatlassprite.wrap(vertexconsumer1),
                    p_426187_.lightCoords(),
                    p_426187_.overlayCoords(),
                    p_426187_.tintedColor()
                );
            }

            if (p_426187_.crumblingOverlay() != null && p_450242_.affectsCrumbling()) {
                VertexConsumer vertexconsumer2 = new SheetedDecalTextureGenerator(
                    p_428832_.getBuffer(ModelBakery.DESTROY_TYPES.get(p_426187_.crumblingOverlay().progress())), p_426187_.crumblingOverlay().cameraPose(), 1.0F
                );
                model.renderToBuffer(
                    this.poseStack,
                    textureatlassprite == null ? vertexconsumer2 : textureatlassprite.wrap(vertexconsumer2),
                    p_426187_.lightCoords(),
                    p_426187_.overlayCoords(),
                    p_426187_.tintedColor()
                );
            }
        }

        RenderState.clear();
        this.poseStack.popPose();
    }

    private boolean isRenderOverlayEyes(EntityRenderState ers, RenderType renderTypeIn) {
        if (ers.renderLayer instanceof EyesLayer) {
            return true;
        } else {
            return ers.renderLayer instanceof LivingEntityEmissiveLayer ? true : renderTypeIn.isEyes();
        }
    }

    private boolean isRenderItemHead(EntityRenderState ers) {
        return ers.renderLayer instanceof CustomHeadLayer;
    }

    public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
    }

    public static class Storage {
        final Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueModelSubmits = new HashMap<>();
        final List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentModelSubmits = new ArrayList<>();
        private final Set<RenderType> usedModelSubmitBuckets = new ObjectOpenHashSet<>();

        public void add(RenderType p_457687_, SubmitNodeStorage.ModelSubmit<?> p_428367_) {
            if (p_457687_.pipeline().getBlendFunction().isEmpty()) {
                this.opaqueModelSubmits.computeIfAbsent(p_457687_, type2In -> new ArrayList<>()).add(p_428367_);
            } else {
                Vector3f vector3f = p_428367_.pose().pose().transformPosition(new Vector3f());
                this.translucentModelSubmits.add(new SubmitNodeStorage.TranslucentModelSubmit<>(p_428367_, p_457687_, vector3f));
            }
        }

        public void clear() {
            this.translucentModelSubmits.clear();

            for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : this.opaqueModelSubmits.entrySet()) {
                List<SubmitNodeStorage.ModelSubmit<?>> list = entry.getValue();
                if (!list.isEmpty()) {
                    this.usedModelSubmitBuckets.add(entry.getKey());
                    list.clear();
                }
            }
        }

        public void endFrame() {
            this.opaqueModelSubmits.keySet().removeIf(type2In -> !this.usedModelSubmitBuckets.contains(type2In));
            this.usedModelSubmitBuckets.clear();
        }

        public int getCountOpaque() {
            int i = 0;

            for (List list : this.opaqueModelSubmits.values()) {
                i += list.size();
            }

            return i;
        }

        public int getCountTranslucent() {
            return this.translucentModelSubmits.size();
        }
    }
}
