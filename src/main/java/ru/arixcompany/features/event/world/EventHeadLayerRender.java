package ru.arixcompany.features.event.world;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.event.Event;

@Getter
@AllArgsConstructor
public class EventHeadLayerRender extends Event {
    private final LivingEntityRenderState state;
    private final PoseStack matrix;
    private final EntityModel<?> model;
    private final SubmitNodeCollector collector;
    private final boolean isSelf;
    private final boolean isFriend;
}