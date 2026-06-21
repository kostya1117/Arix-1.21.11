package team.creative.itemphysiclite;

import net.minecraft.world.level.block.Blocks;
import team.creative.creativecore.common.util.type.list.SortingBlockList;

public class ItemPhysicLiteConfig {

    public boolean oldRotation;

    public float rotateSpeed = 1.0F; //min 0 max 10

    public SortingBlockList blockRequireOffset = new SortingBlockList().add(Blocks.SNOW).add(Blocks.SOUL_SAND).add(Blocks.MUD);
    
}
