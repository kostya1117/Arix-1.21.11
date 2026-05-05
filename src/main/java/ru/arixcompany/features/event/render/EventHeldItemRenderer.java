package ru.arixcompany.features.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.InteractionHand;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
public class EventHeldItemRenderer extends Event {
    public final InteractionHand hand;
    public final PoseStack matrix;
    public final float swingProgress;
}