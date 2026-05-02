package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.optifine.EmissiveTextures;

public abstract class Model<S> {
    protected final ModelPart root;
    protected final Function<Identifier, RenderType> renderType;
    private final List<ModelPart> allParts;
    public int textureWidth = 64;
    public int textureHeight = 32;
    public Identifier locationTextureCustom;

    public Model(ModelPart p_362439_, Function<Identifier, RenderType> p_103110_) {
        this.root = p_362439_;
        this.renderType = p_103110_;
        this.allParts = p_362439_.getAllParts();
    }

    public final RenderType renderType(Identifier p_452728_) {
        if (this.locationTextureCustom != null) {
            p_452728_ = this.locationTextureCustom;
        }

        RenderType rendertype = this.renderType.apply(p_452728_);
        if (EmissiveTextures.isRenderEmissive() && rendertype.isEntitySolid()) {
            rendertype = RenderTypes.entityCutout(p_452728_);
        }

        return rendertype;
    }

    public final void renderToBuffer(PoseStack p_103111_, VertexConsumer p_103112_, int p_103113_, int p_103114_, int p_345283_) {
        this.root().render(p_103111_, p_103112_, p_103113_, p_103114_, p_345283_);
    }

    public final void renderToBuffer(PoseStack p_345147_, VertexConsumer p_343104_, int p_342281_, int p_344413_) {
        this.renderToBuffer(p_345147_, p_343104_, p_342281_, p_344413_, -1);
    }

    public final ModelPart root() {
        return this.root;
    }

    public final List<ModelPart> allParts() {
        return this.allParts;
    }

    public void setupAnim(S p_429527_) {
        this.resetPose();
    }

    public final void resetPose() {
        for (ModelPart modelpart : this.allParts) {
            modelpart.resetPose();
        }
    }

    public boolean isRenderRoot() {
        return true;
    }

    public static class Simple extends Model<Unit> {
        public Simple(ModelPart p_368796_, Function<Identifier, RenderType> p_362226_) {
            super(p_368796_, p_362226_);
        }

        public void setupAnim(Unit p_429209_) {
        }
    }
}
