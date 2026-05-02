package net.optifine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.model.BlockModelUtils;

public class SmartLeaves {
    private static BlockModelPart modelLeavesCullAcacia = null;
    private static BlockModelPart modelLeavesCullBirch = null;
    private static BlockModelPart modelLeavesCullDarkOak = null;
    private static BlockModelPart modelLeavesCullJungle = null;
    private static BlockModelPart modelLeavesCullOak = null;
    private static BlockModelPart modelLeavesCullSpruce = null;
    private static BlockModelPart modelLeavesDoubleAcacia = null;
    private static BlockModelPart modelLeavesDoubleBirch = null;
    private static BlockModelPart modelLeavesDoubleDarkOak = null;
    private static BlockModelPart modelLeavesDoubleJungle = null;
    private static BlockModelPart modelLeavesDoubleOak = null;
    private static BlockModelPart modelLeavesDoubleSpruce = null;

    public static List<BlockModelPart> getLeavesModel(List<BlockModelPart> partsIn, BlockState stateIn) {
        if (partsIn.size() != 1) {
            return partsIn;
        }

        BlockModelPart blockmodelpart = partsIn.get(0);
        BlockModelPart blockmodelpart1 = getLeavesModel(blockmodelpart, stateIn);
        if (blockmodelpart1 == blockmodelpart) {
            return partsIn;
        }

        partsIn.set(0, blockmodelpart1);
        return partsIn;
    }

    public static BlockModelPart getLeavesModel(BlockModelPart model, BlockState stateIn) {
        if (!Config.isTreesSmart()) {
            return model;
        } else if (model == modelLeavesCullAcacia) {
            return modelLeavesDoubleAcacia;
        } else if (model == modelLeavesCullBirch) {
            return modelLeavesDoubleBirch;
        } else if (model == modelLeavesCullDarkOak) {
            return modelLeavesDoubleDarkOak;
        } else if (model == modelLeavesCullJungle) {
            return modelLeavesDoubleJungle;
        } else if (model == modelLeavesCullOak) {
            return modelLeavesDoubleOak;
        } else {
            return model == modelLeavesCullSpruce ? modelLeavesDoubleSpruce : model;
        }
    }

    public static boolean isSameLeaves(BlockState state1, BlockState state2) {
        if (state1 == state2) {
            return true;
        }

        Block block = state1.getBlock();
        Block block1 = state2.getBlock();
        return block == block1;
    }

    public static void updateLeavesModels() {
        List<String> list = new ArrayList<>();
        modelLeavesCullAcacia = getModelCull(Blocks.ACACIA_LEAVES.defaultBlockState(), list);
        modelLeavesCullBirch = getModelCull(Blocks.BIRCH_LEAVES.defaultBlockState(), list);
        modelLeavesCullDarkOak = getModelCull(Blocks.DARK_OAK_LEAVES.defaultBlockState(), list);
        modelLeavesCullJungle = getModelCull(Blocks.JUNGLE_LEAVES.defaultBlockState(), list);
        modelLeavesCullOak = getModelCull(Blocks.OAK_LEAVES.defaultBlockState(), list);
        modelLeavesCullSpruce = getModelCull(Blocks.SPRUCE_LEAVES.defaultBlockState(), list);
        modelLeavesDoubleAcacia = getModelDoubleFace(modelLeavesCullAcacia);
        modelLeavesDoubleBirch = getModelDoubleFace(modelLeavesCullBirch);
        modelLeavesDoubleDarkOak = getModelDoubleFace(modelLeavesCullDarkOak);
        modelLeavesDoubleJungle = getModelDoubleFace(modelLeavesCullJungle);
        modelLeavesDoubleOak = getModelDoubleFace(modelLeavesCullOak);
        modelLeavesDoubleSpruce = getModelDoubleFace(modelLeavesCullSpruce);
        if (list.size() > 0) {
            Config.dbg("Enable face culling: " + Config.arrayToString(list.toArray()));
        }
    }

    static BlockModelPart getModelCull(BlockState blockState, List<String> updatedTypes) {
        String s = blockState.getBlockLocation().getPath();
        Identifier identifier = new Identifier("blockstates/" + s + ".json");
        if (!Config.isFromDefaultResourcePack(identifier)) {
            return null;
        }

        Identifier identifier1 = new Identifier("models/block/" + s + ".json");
        if (!Config.isFromDefaultResourcePack(identifier1)) {
            return null;
        }

        BlockStateModel blockstatemodel = BlockModelUtils.getBlockStateModel(blockState);
        if (BlockModelUtils.isMissingModel(blockstatemodel)) {
            Config.warn("Model not found for block state: " + blockState);
            return BlockModelUtils.getMissingBlockModel();
        }

        BlockModelPart blockmodelpart = BlockModelUtils.getModel(blockState);

        for (Direction direction : Direction.VALUES) {
            List<BakedQuad> list = blockmodelpart.getQuads(direction);
            if (list.isEmpty()) {
                return null;
            }
        }

        return blockmodelpart;
    }

    private static BlockModelPart getModelDoubleFace(BlockModelPart model) {
        if (model == null) {
            return null;
        }

        if (model.getQuads(null).size() > 0) {
            Config.warn("SmartLeaves: Model is not cube, general quads: " + model.getQuads(null).size() + ", model: " + model);
            return model;
        }

        Direction[] adirection = Direction.VALUES;

        for (int i = 0; i < adirection.length; i++) {
            Direction direction = adirection[i];
            List<BakedQuad> list = model.getQuads(direction);
            if (list.size() != 1) {
                Config.warn("SmartLeaves: Model is not cube, side: " + direction + ", quads: " + list.size() + ", model: " + model);
                return model;
            }
        }

        return BlockModelUtils.duplicateModel(model, SmartLeaves::addReverseQuads);
    }

    public static List<BakedQuad> addReverseQuads(List<BakedQuad> quads, Direction face) {
        if (face == null) {
            return quads;
        }

        List<BakedQuad> list = new ArrayList<>(quads);
        BakedQuad bakedquad = list.get(0);
        BakedQuad bakedquad1 = bakedquad.makeCopyReverse();
        list.add(bakedquad1);
        return list;
    }
}
