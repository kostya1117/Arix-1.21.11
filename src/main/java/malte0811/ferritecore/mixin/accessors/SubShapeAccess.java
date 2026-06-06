package malte0811.ferritecore.mixin.accessors;

import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public interface SubShapeAccess extends DiscreteVSAccess {
    DiscreteVoxelShape getParent();

    int getStartX();

    int getStartY();

    int getStartZ();

    int getEndX();

    int getEndY();

    int getEndZ();
}
