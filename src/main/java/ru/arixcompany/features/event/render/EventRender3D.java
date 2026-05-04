package ru.arixcompany.features.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventRender3D extends Event {
   private final PoseStack matrixStack;
   private final float tickDelta;
}
