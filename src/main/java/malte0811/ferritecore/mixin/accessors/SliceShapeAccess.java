package malte0811.ferritecore.mixin.accessors;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SliceShapeAccess extends VoxelShapeAccess {
    VoxelShape getDelegate();

    Direction.Axis getAxis();
}
