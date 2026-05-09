package ru.arixcompany.features.event.world;

import ru.arixcompany.features.event.Event;

/**
 * Стреляет внутри Minecraft.tick() ДО level.tick() (до отправки move пакета).
 * Используется для атак в ауре — чтобы ServerboundInteractPacket уходил
 * до ServerboundMovePlayerPacket, как в ванилле при клике мышью.
 */
public class EventPreTick extends Event {
}
