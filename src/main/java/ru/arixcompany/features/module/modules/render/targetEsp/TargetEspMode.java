package ru.arixcompany.features.module.modules.render.targetEsp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.utils.IMinecraft;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public abstract class TargetEspMode implements IMinecraft {

    private final String name;

    public abstract void render(EventRender3D event, LivingEntity target, float tickDelta);

    public void onUpdate() {}

    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    protected float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    protected int lerpInt(int from, int to, float delta) {
        return (int) (from + (to - from) * delta);
    }

    protected double lerp(double current, double old, float tickDelta) {
        return old + (current - old) * tickDelta;
    }
}
