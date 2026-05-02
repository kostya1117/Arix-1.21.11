package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.entity.model.ModelAdapter;
import net.optifine.util.ArrayUtils;

public class WolfCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private static final Identifier WOLF_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");
    public WolfModel adultModel = new WolfModel(ModelAdapter.bakeModelLayer(ModelLayers.WOLF));
    public WolfModel babyModel = new WolfModel(ModelAdapter.bakeModelLayer(ModelLayers.WOLF_BABY));

    public WolfCollarLayer(RenderLayerParent<WolfRenderState, WolfModel> p_117707_) {
        super(p_117707_);
    }

    public void submit(PoseStack p_430602_, SubmitNodeCollector p_430277_, int p_431658_, WolfRenderState p_430543_, float p_429085_, float p_425979_) {
        DyeColor dyecolor = p_430543_.collarColor;
        if (dyecolor != null && !p_430543_.isInvisible) {
            int i = dyecolor.getTextureDiffuseColor();
            if (Config.isCustomColors()) {
                i = CustomColors.getWolfCollarColors(dyecolor, i);
            }

            WolfModel wolfmodel = this.getEntityModel(p_430543_);
            Identifier identifier = ArrayUtils.firstNonNull(wolfmodel.locationTextureCustom, WOLF_COLLAR_LOCATION);
            p_430277_.order(1)
                .submitModel(
                    wolfmodel, p_430543_, p_430602_, RenderTypes.entityCutoutNoCull(identifier), p_431658_, OverlayTexture.NO_OVERLAY, i, null, p_430543_.outlineColor, null
                );
        }
    }

    public WolfModel getEntityModel(WolfRenderState stateIn) {
        WolfModel wolfmodel = stateIn.isBaby ? this.babyModel : this.adultModel;
        if (wolfmodel != null) {
            wolfmodel.setupAnim(stateIn);
            return wolfmodel;
        } else {
            return (WolfModel)super.getParentModel();
        }
    }
}
