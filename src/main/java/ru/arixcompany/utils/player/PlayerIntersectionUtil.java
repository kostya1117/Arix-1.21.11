package ru.arixcompany.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import ru.arixcompany.utils.IMinecraft;

import java.util.List;
import java.util.function.Predicate;

@UtilityClass
public class PlayerIntersectionUtil implements IMinecraft {

    public boolean canChangeIntoPose(Pose pose) {
        return mc.player.level().noCollision(mc.player, mc.player.getDimensions(pose).makeBoundingBox(mc.player.getPosPlayer()).deflate(1.0E-7));
    }

    public boolean isPlayerInBlock(Block block) {
        return isBoxInBlock(mc.player.getBoundingBox().inflate(-1e-3), block);
    }

    public boolean isBoxInBlock(AABB box, Block block) {
        return isBox(box,pos -> mc.level.getBlockState(pos).getBlock().equals(block));
    }

    public boolean isBoxInBlocks(AABB box, List<Block> blocks) {
        return isBox(box,pos -> blocks.contains(mc.level.getBlockState(pos).getBlock()));
    }

    public boolean isBox(AABB box, Predicate<BlockPos> pos) {
        return BlockPos.betweenClosedStream(box).anyMatch(pos);
    }
}
