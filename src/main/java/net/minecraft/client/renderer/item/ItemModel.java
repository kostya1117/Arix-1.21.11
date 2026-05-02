package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public interface ItemModel {
    void update(
        ItemStackRenderState p_377489_,
        ItemStack p_376390_,
        ItemModelResolver p_378232_,
        ItemDisplayContext p_376927_,
         ClientLevel p_377374_,
         ItemOwner p_428412_,
        int p_377873_
    );

    
    record BakingContext(
        ModelBaker blockModelBaker,
        EntityModelSet entityModelSet,
        MaterialSet materials,
        PlayerSkinRenderCache playerSkinRenderCache,
        ItemModel missingItemModel,
         RegistryContextSwapper contextSwapper
    ) implements SpecialModelRenderer.BakingContext {
        @Override
        public EntityModelSet entityModelSet() {
            return this.entityModelSet;
        }

        @Override
        public MaterialSet materials() {
            return this.materials;
        }

        @Override
        public PlayerSkinRenderCache playerSkinRenderCache() {
            return this.playerSkinRenderCache;
        }
    }

    
    interface Unbaked extends ResolvableModel {
        MapCodec<? extends ItemModel.Unbaked> type();

        ItemModel bake(ItemModel.BakingContext p_376062_);
    }
}
