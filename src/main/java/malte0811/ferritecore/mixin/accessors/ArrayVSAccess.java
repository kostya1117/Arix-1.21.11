package malte0811.ferritecore.mixin.accessors;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface ArrayVSAccess extends VoxelShapeAccess {
    void setXPoints(DoubleList newPoints);

    void setYPoints(DoubleList newPoints);

    void setZPoints(DoubleList newPoints);

    DoubleList getXPoints();

    DoubleList getYPoints();

    DoubleList getZPoints();
}
