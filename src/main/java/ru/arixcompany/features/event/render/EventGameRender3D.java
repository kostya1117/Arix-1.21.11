package ru.arixcompany.features.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.Camera;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventGameRender3D extends Event {
    private final PoseStack matrixStack;
    private final Camera camera;
    private final float tickDelta;
}
