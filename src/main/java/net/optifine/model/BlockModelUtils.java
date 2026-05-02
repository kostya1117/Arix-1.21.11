package net.optifine.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.optifine.Config;
import net.optifine.util.DummyRandomSource;
import org.joml.Vector3f;

public class BlockModelUtils {
    private static final float VERTEX_COORD_ACCURACY = 1.0E-6F;
    private static Minecraft mc = Minecraft.getInstance();

    public static BlockModelPart makeModelCube(TextureAtlasSprite sprite, int tintIndex) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();
        Direction[] adirection = Direction.VALUES;

        for (int i = 0; i < adirection.length; i++) {
            Direction direction = adirection[i];
            BakedQuad bakedquad = makeBakedQuad(direction, sprite, tintIndex);
            quadcollection$builder.addCulledFace(direction, bakedquad);
        }

        QuadCollection quadcollection = quadcollection$builder.build();
        return new SimpleModelWrapper(quadcollection, true, sprite);
    }

    public static BlockModelPart joinModelsCube(BlockModelPart modelBase, BlockModelPart modelAdd) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();
        List<BakedQuad> list = new ArrayList<>();
        list.addAll(modelBase.getQuads(null));
        list.addAll(modelAdd.getQuads(null));
        addAllUnculled(quadcollection$builder, list);
        Direction[] adirection = Direction.VALUES;

        for (int i = 0; i < adirection.length; i++) {
            Direction direction = adirection[i];
            addAllCulled(quadcollection$builder, direction, modelBase.getQuads(direction));
            addAllCulled(quadcollection$builder, direction, modelAdd.getQuads(direction));
        }

        addAllUnculled(quadcollection$builder, modelBase.getQuads(null));
        addAllUnculled(quadcollection$builder, modelAdd.getQuads(null));
        QuadCollection quadcollection = quadcollection$builder.build();
        boolean flag = modelBase.useAmbientOcclusion();
        TextureAtlasSprite textureatlassprite = modelBase.particleIcon();
        return new SimpleModelWrapper(quadcollection, flag, textureatlassprite);
    }

    public static void addAllCulled(QuadCollection.Builder qcb, Direction facing, List<BakedQuad> quads) {
        for (BakedQuad bakedquad : quads) {
            qcb.addCulledFace(facing, bakedquad);
        }
    }

    public static void addAllUnculled(QuadCollection.Builder qcb, List<BakedQuad> quads) {
        for (BakedQuad bakedquad : quads) {
            qcb.addUnculledFace(bakedquad);
        }
    }

    public static BakedQuad makeBakedQuad(Direction facing, TextureAtlasSprite sprite, int tintIndex) {
        Vector3f vector3f = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f vector3f1 = new Vector3f(16.0F, 16.0F, 16.0F);
        BlockElementFace.UVs blockelementface$uvs = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
        BlockElementFace blockelementface = new BlockElementFace(facing, tintIndex, "#" + facing.getSerializedName(), blockelementface$uvs, Quadrant.R0);
        BlockModelRotation blockmodelrotation = BlockModelRotation.IDENTITY;
        BlockElementRotation blockelementrotation = null;
        boolean flag = true;
        int i = 0;
        ModelBaker.PartCache modelbaker$partcache = new ModelBakery.PartCacheImpl();
        return FaceBakery.bakeQuad(
            modelbaker$partcache, vector3f, vector3f1, blockelementface, sprite, facing, blockmodelrotation, blockelementrotation, flag, i
        );
    }

    public static BlockModelPart makeModel(BlockState blockState, final TextureAtlasSprite spriteOld, final TextureAtlasSprite spriteNew) {
        if (spriteOld != null && spriteNew != null) {
            ModelManager modelmanager = Config.getModelManager();
            if (modelmanager == null) {
                return null;
            } else {
                BlockStateModel blockstatemodel = getBlockStateModel(blockState);
                if (blockstatemodel != null && blockstatemodel != modelmanager.getMissingBlockStateModel()) {
                    BlockModelPart blockmodelpart = getModel(blockstatemodel);
                    BiFunction<List<BakedQuad>, Direction, List<BakedQuad>> bifunction = new BiFunction<List<BakedQuad>, Direction, List<BakedQuad>>() {
                        public List<BakedQuad> apply(List<BakedQuad> quads, Direction face) {
                            return BlockModelUtils.replaceTexture(quads, spriteOld, spriteNew);
                        }
                    };
                    return duplicateModel(blockmodelpart, bifunction);
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static SimpleModelWrapper duplicateModel(BlockModelPart model, BiFunction<List<BakedQuad>, Direction, List<BakedQuad>> quadsTransformer) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();
        Direction[] adirection = Direction.VALUES;

        for (int i = 0; i < adirection.length; i++) {
            Direction direction = adirection[i];
            List<BakedQuad> list = model.getQuads(direction);
            List<BakedQuad> list1 = quadsTransformer.apply(list, direction);
            addAllCulled(quadcollection$builder, direction, list1);
        }

        List<BakedQuad> list2 = model.getQuads(null);
        List<BakedQuad> list3 = quadsTransformer.apply(list2, null);
        addAllUnculled(quadcollection$builder, list3);
        QuadCollection quadcollection = quadcollection$builder.build();
        return new SimpleModelWrapper(quadcollection, model.useAmbientOcclusion(), model.particleIcon());
    }

    private static List<BakedQuad> replaceTexture(List<BakedQuad> quads, TextureAtlasSprite spriteOld, TextureAtlasSprite spriteNew) {
        List<BakedQuad> list = new ArrayList<>();
        if (quads == null) {
            return list;
        }

        for (BakedQuad bakedquad : quads) {
            if (bakedquad.sprite() == spriteOld) {
                bakedquad = BakedQuadRetextured.make(bakedquad, spriteNew);
            }

            list.add(bakedquad);
        }

        return list;
    }

    public static void snapVertexPosition(Vector3f pos) {
        pos.set(snapVertexCoord(pos.x()), snapVertexCoord(pos.y()), snapVertexCoord(pos.z()));
    }

    private static float snapVertexCoord(float x) {
        if (x > -1.0E-6F && x < 1.0E-6F) {
            return 0.0F;
        } else {
            return x > 0.999999F && x < 1.000001F ? 1.0F : x;
        }
    }

    public static AABB getOffsetBoundingBox(AABB aabb, BlockBehaviour.OffsetType offsetType, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getZ();
        long k = i * 3129871 ^ j * 116129781L;
        k = k * k * 42317861L + k * 11L;
        double d0 = ((float)(k >> 16 & 15L) / 15.0F - 0.5) * 0.5;
        double d1 = ((float)(k >> 24 & 15L) / 15.0F - 0.5) * 0.5;
        double d2 = 0.0;
        if (offsetType == BlockBehaviour.OffsetType.XYZ) {
            d2 = ((float)(k >> 20 & 15L) / 15.0F - 1.0) * 0.2;
        }

        return aabb.move(d0, d2, d1);
    }

    public static BlockStateModel getBlockStateModel(BlockState state) {
        return mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
    }

    public static boolean isMissingModel(BlockStateModel model) {
        return model == null ? true : model == getMissingBlockStateModel();
    }

    public static BlockStateModel getMissingBlockStateModel() {
        return mc.getModelManager().getMissingBlockStateModel();
    }

    public static BlockModelPart getMissingBlockModel() {
        return getModel(getMissingBlockStateModel());
    }

    public static BlockModelPart getModel(BlockState state) {
        BlockStateModel blockstatemodel = mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        return getModel(blockstatemodel);
    }

    public static BlockModelPart getModel(BlockStateModel bsm) {
        if (bsm instanceof SingleVariant singlevariant1) {
            return singlevariant1.getModel();
        } else {
            List<BlockModelPart> list = bsm.collectParts(DummyRandomSource.INSTANCE);
            if (list.size() > 0) {
                return list.get(0);
            } else {
                return mc.getModelManager().getMissingBlockStateModel() instanceof SingleVariant singlevariant ? singlevariant.getModel() : null;
            }
        }
    }
}
