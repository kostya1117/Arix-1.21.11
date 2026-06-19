package ru.arixcompany.features.module.modules.render.cape.sim;

import java.util.List;
import ru.arixcompany.features.module.modules.render.cape.util.CapePoint;
import ru.arixcompany.features.module.modules.render.cape.util.Vector3;

public interface BasicSimulation {
    void simulate();
    void setGravityDirection(Vector3 gravityDirection);
    float getGravity();
    void setGravity(float gravity);
    boolean isSneaking();
    void setSneaking(boolean sneaking);
    boolean init(int partCount);
    boolean empty();
    void applyMovement(Vector3 movement);
    List<CapePoint> getPoints();
}
