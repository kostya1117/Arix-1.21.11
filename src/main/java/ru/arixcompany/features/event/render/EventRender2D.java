package ru.arixcompany.features.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.event.Event;

@Getter
@AllArgsConstructor
public class EventRender2D extends Event {
    GuiGraphics guiGraphics;
}
