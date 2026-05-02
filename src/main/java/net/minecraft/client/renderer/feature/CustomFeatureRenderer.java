package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.optifine.Config;
import net.optifine.render.RenderState;
import net.optifine.shaders.Shaders;

public class CustomFeatureRenderer {
    public void render(SubmitNodeCollection p_425143_, MultiBufferSource.BufferSource p_423778_) {
        boolean flag = Config.isShaders();
        CustomFeatureRenderer.Storage customfeaturerenderer$storage = p_425143_.getCustomGeometrySubmits();

        for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : customfeaturerenderer$storage.customGeometrySubmits.entrySet()) {
            VertexConsumer vertexconsumer = p_423778_.getBuffer(entry.getKey());

            for (SubmitNodeStorage.CustomGeometrySubmit submitnodestorage$customgeometrysubmit : entry.getValue()) {
                Object object = submitnodestorage$customgeometrysubmit.renderState();
                if (object instanceof EntityRenderState entityrenderstate) {
                    RenderState.setEntityRenderState(entityrenderstate);
                    if (flag) {
                        Shaders.nextEntity(entityrenderstate.entity);
                    }
                } else if (object instanceof BlockEntityRenderState blockentityrenderstate) {
                    RenderState.setBlockEntityRenderState(blockentityrenderstate);
                    if (flag) {
                        Shaders.nextBlockEntity(blockentityrenderstate.blockEntity);
                    }
                }

                submitnodestorage$customgeometrysubmit.customGeometryRenderer().render(submitnodestorage$customgeometrysubmit.pose(), vertexconsumer);
                RenderState.clear();
            }
        }
    }

    public static class Storage {
        final Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> customGeometrySubmits = new HashMap<>();
        private final Set<RenderType> customGeometrySubmitsUsage = new ObjectOpenHashSet<>();

        public void add(PoseStack p_424712_, RenderType p_459557_, SubmitNodeCollector.CustomGeometryRenderer p_428692_) {
            List<SubmitNodeStorage.CustomGeometrySubmit> list = this.customGeometrySubmits.computeIfAbsent(p_459557_, type2In -> new ArrayList<>());
            list.add(new SubmitNodeStorage.CustomGeometrySubmit(p_424712_.last().copy(), p_428692_));
        }

        public void clear() {
            for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : this.customGeometrySubmits.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    this.customGeometrySubmitsUsage.add(entry.getKey());
                    entry.getValue().clear();
                }
            }
        }

        public void endFrame() {
            this.customGeometrySubmits.keySet().removeIf(type2In -> !this.customGeometrySubmitsUsage.contains(type2In));
            this.customGeometrySubmitsUsage.clear();
        }

        public int getCount() {
            int i = 0;

            for (List list : this.customGeometrySubmits.values()) {
                i += list.size();
            }

            return i;
        }
    }
}
