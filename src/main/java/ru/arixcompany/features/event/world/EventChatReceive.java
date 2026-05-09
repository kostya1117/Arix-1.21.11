package ru.arixcompany.features.event.world;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.event.Event;

@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventChatReceive extends Event {
    Component message;
}