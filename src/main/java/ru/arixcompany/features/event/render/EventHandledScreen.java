package ru.arixcompany.features.event.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import ru.arixcompany.features.event.Event;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventHandledScreen extends Event {
    GuiGraphics guiGraphics;
    int backgroundWidth;
    int backgroundHeight;
    private final Slot slotHover;
}