package malte0811.ferritecore.mixin.accessors;

import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public interface VoxelShapeAccess {
    DiscreteVoxelShape getShape();

    @Nullable
    VoxelShape[] getFaces();

    void setShape(DiscreteVoxelShape newPart);

    void setFaces(@Nullable VoxelShape[] newCache);
}
