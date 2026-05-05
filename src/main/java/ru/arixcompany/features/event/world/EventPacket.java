package ru.arixcompany.features.event.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.protocol.Packet;
import ru.arixcompany.features.event.Event;

@Getter
@AllArgsConstructor
public class EventPacket extends Event {

    private final Packet<?> packet;
    private final Type type;

    public boolean isSend() {
        return type == Type.SEND;
    }

    public boolean isReceive() {
        return type == Type.RECEIVE;
    }

    public enum Type {
        SEND,
        RECEIVE
    }
}
