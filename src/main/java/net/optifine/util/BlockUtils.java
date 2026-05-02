package net.optifine.util;

import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.optifine.Config;
import net.optifine.render.RenderEnv;

public class BlockUtils {
    private static final ThreadLocal<RenderSideCacheKey> threadLocalKey = ThreadLocal.withInitial(() -> {
        return new RenderSideCacheKey((BlockState)null, (BlockState)null, (Direction)null);
    });
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<RenderSideCacheKey>> threadLocalMap = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<RenderSideCacheKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<RenderSideCacheKey>(200) {
            protected void rehash(int p_rehash_1_) {
            }
        };
        object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
        return object2bytelinkedopenhashmap;
    });

    public BlockUtils() {
    }

    public static boolean shouldSideBeRendered(BlockState blockStateIn, BlockGetter blockReaderIn, BlockPos blockPosIn, Direction facingIn, RenderEnv renderEnv) {
        BlockPos posNeighbour = blockPosIn.relative(facingIn);
        BlockState stateNeighbour = blockReaderIn.getBlockState(posNeighbour);
        if (stateNeighbour.isSolidRender() && !(blockStateIn.getBlock() instanceof PowderSnowBlock)) {
            return false;
        } else if (blockStateIn.skipRendering(stateNeighbour, facingIn)) {
            return false;
        } else {
            return stateNeighbour.canOcclude() ? shouldSideBeRenderedCached(blockStateIn, blockReaderIn, blockPosIn, facingIn, renderEnv, stateNeighbour, posNeighbour) : true;
        }
    }

    public static boolean shouldSideBeRenderedCached(BlockState blockStateIn, BlockGetter blockReaderIn, BlockPos blockPosIn, Direction facingIn, RenderEnv renderEnv, BlockState stateNeighbourIn, BlockPos posNeighbourIn) {
        long key = (long)blockStateIn.getBlockStateId() << 36 | (long)stateNeighbourIn.getBlockStateId() << 4 | (long)facingIn.ordinal();
        Long2ByteLinkedOpenHashMap map = renderEnv.getRenderSideMap();
        byte b0 = map.getAndMoveToFirst(key);
        if (b0 != 0) {
            return b0 > 0;
        } else {
            VoxelShape voxelshape = blockStateIn.getFaceOcclusionShape(facingIn);
            if (voxelshape.isEmpty()) {
                return true;
            } else {
                VoxelShape voxelshape1 = stateNeighbourIn.getFaceOcclusionShape(facingIn.getOpposite());
                boolean flag = Shapes.joinIsNotEmpty(voxelshape, voxelshape1, BooleanOp.ONLY_FIRST);
                if (map.size() > 400) {
                    map.removeLastByte();
                }

                map.putAndMoveToFirst(key, (byte)(flag ? 1 : -1));
                return flag;
            }
        }
    }

    public static int getBlockId(Block block) {
        return BuiltInRegistries.BLOCK.getId(block);
    }

    public static Block getBlock(Identifier loc) {
        return !BuiltInRegistries.BLOCK.containsKey(loc) ? null : (Block)BuiltInRegistries.BLOCK.getValue(loc);
    }

    public static int getMetadata(BlockState blockState) {
        Block block = blockState.getBlock();
        StateDefinition<Block, BlockState> stateContainer = block.getStateDefinition();
        List<BlockState> validStates = stateContainer.getPossibleStates();
        int metadata = validStates.indexOf(blockState);
        return metadata;
    }

    public static int getMetadataCount(Block block) {
        StateDefinition<Block, BlockState> stateContainer = block.getStateDefinition();
        List<BlockState> validStates = stateContainer.getPossibleStates();
        return validStates.size();
    }

    public static BlockState getBlockState(Block block, int metadata) {
        StateDefinition<Block, BlockState> stateContainer = block.getStateDefinition();
        List<BlockState> validStates = stateContainer.getPossibleStates();
        if (metadata >= 0 && metadata < validStates.size()) {
            BlockState blockState = (BlockState)validStates.get(metadata);
            return blockState;
        } else {
            return null;
        }
    }

    public static List<BlockState> getBlockStates(Block block) {
        StateDefinition<Block, BlockState> stateContainer = block.getStateDefinition();
        List<BlockState> validStates = stateContainer.getPossibleStates();
        return validStates;
    }

    public static boolean isFullCube(BlockState stateIn, BlockGetter blockReaderIn, BlockPos posIn) {
        return stateIn.isCacheOpaqueCollisionShape();
    }

    public static Collection<Property<?>> getProperties(BlockState blockState) {
        return blockState.getProperties();
    }

    public static boolean isPropertyTrue(BlockState blockState, BooleanProperty prop) {
        Boolean value = (Boolean)blockState.getValues().get(prop);
        return Config.isTrue(value);
    }

    public static boolean isPropertyFalse(BlockState blockState, BooleanProperty prop) {
        Boolean value = (Boolean)blockState.getValues().get(prop);
        return Config.isFalse(value);
    }

    public static final class RenderSideCacheKey {
        private BlockState blockState1;
        private BlockState blockState2;
        private Direction facing;
        private int hashCode;

        private RenderSideCacheKey(BlockState blockState1In, BlockState blockState2In, Direction facingIn) {
            this.blockState1 = blockState1In;
            this.blockState2 = blockState2In;
            this.facing = facingIn;
        }

        private void init(BlockState blockState1In, BlockState blockState2In, Direction facingIn) {
            this.blockState1 = blockState1In;
            this.blockState2 = blockState2In;
            this.facing = facingIn;
            this.hashCode = 0;
        }

        public RenderSideCacheKey duplicate() {
            return new RenderSideCacheKey(this.blockState1, this.blockState2, this.facing);
        }

        public boolean equals(Object p_equals_1_) {
            if (this == p_equals_1_) {
                return true;
            } else if (!(p_equals_1_ instanceof RenderSideCacheKey)) {
                return false;
            } else {
                RenderSideCacheKey block$rendersidecachekey = (RenderSideCacheKey)p_equals_1_;
                return this.blockState1 == block$rendersidecachekey.blockState1 && this.blockState2 == block$rendersidecachekey.blockState2 && this.facing == block$rendersidecachekey.facing;
            }
        }

        public int hashCode() {
            if (this.hashCode == 0) {
                this.hashCode = 31 * this.hashCode + this.blockState1.hashCode();
                this.hashCode = 31 * this.hashCode + this.blockState2.hashCode();
                this.hashCode = 31 * this.hashCode + this.facing.hashCode();
            }

            return this.hashCode;
        }
    }
}
