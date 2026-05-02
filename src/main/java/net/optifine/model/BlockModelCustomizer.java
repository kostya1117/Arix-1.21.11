package net.optifine.model;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.BetterGrass;
import net.optifine.Config;
import net.optifine.ConnectedTextures;
import net.optifine.NaturalTextures;
import net.optifine.SmartLeaves;
import net.optifine.render.RenderEnv;

public class BlockModelCustomizer {
    private static final List<BakedQuad> NO_QUADS = ImmutableList.of();

    public static List<BlockModelPart> getRenderModel(List<BlockModelPart> modelIn, BlockState stateIn, RenderEnv renderEnv) {
        if (renderEnv.isSmartLeaves()) {
            modelIn = SmartLeaves.getLeavesModel(modelIn, stateIn);
        }

        return modelIn;
    }

    public static List<BakedQuad> getRenderQuads(
        List<BakedQuad> quads,
        BlockAndTintGetter worldIn,
        BlockState stateIn,
        BlockPos posIn,
        Direction enumfacing,
        ChunkSectionLayer layer,
        RenderEnv renderEnv
    ) {
        if (enumfacing != null) {
            if (renderEnv.isSmartLeaves() && SmartLeaves.isSameLeaves(worldIn.getBlockState(posIn.relative(enumfacing)), stateIn)) {
                return NO_QUADS;
            }

            if (!renderEnv.isBreakingAnimation(quads) && Config.isBetterGrass()) {
                quads = BetterGrass.getFaceQuads(worldIn, stateIn, posIn, enumfacing, quads);
            }
        }

        List<BakedQuad> list = renderEnv.getListQuadsCustomizer();
        list.clear();

        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedquad = quads.get(i);
            BakedQuad[] abakedquad = getRenderQuads(bakedquad, worldIn, stateIn, posIn, enumfacing, renderEnv);
            if (i == 0 && quads.size() == 1 && abakedquad.length == 1 && abakedquad[0] == bakedquad && bakedquad.getQuadEmissive() == null) {
                return quads;
            }

            for (int j = 0; j < abakedquad.length; j++) {
                BakedQuad bakedquad1 = abakedquad[j];
                list.add(bakedquad1);
                if (bakedquad1.getQuadEmissive() != null) {
                    renderEnv.getListQuadsOverlay(getEmissiveLayer(layer)).addQuad(bakedquad1.getQuadEmissive(), stateIn);
                    renderEnv.setOverlaysRendered(true);
                }
            }
        }

        return list;
    }

    private static ChunkSectionLayer getEmissiveLayer(ChunkSectionLayer layer) {
        return layer != null && layer != ChunkSectionLayer.SOLID ? layer : ChunkSectionLayer.CUTOUT;
    }

    private static BakedQuad[] getRenderQuads(
        BakedQuad quad, BlockAndTintGetter worldIn, BlockState stateIn, BlockPos posIn, Direction facingIn, RenderEnv renderEnv
    ) {
        if (renderEnv.isBreakingAnimation(quad)) {
            return renderEnv.getArrayQuadsCtm(quad);
        }

        BakedQuad bakedquad = quad;
        if (Config.isConnectedTextures()) {
            Direction direction = facingIn != null ? facingIn : quad.direction();
            BakedQuad[] abakedquad = ConnectedTextures.getConnectedTexture(worldIn, stateIn, posIn, quad, direction, renderEnv);
            if (abakedquad.length != 1 || abakedquad[0] != quad) {
                return abakedquad;
            }
        }

        if (Config.isNaturalTextures()) {
            quad = NaturalTextures.getNaturalTexture(stateIn, posIn, quad);
            if (quad != bakedquad) {
                return renderEnv.getArrayQuadsCtm(quad);
            }
        }

        return renderEnv.getArrayQuadsCtm(quad);
    }
}
