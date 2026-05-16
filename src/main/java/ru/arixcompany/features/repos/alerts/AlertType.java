package ru.arixcompany.features.repos.alerts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.ChatFormatting;

@Getter
@AllArgsConstructor
public enum AlertType {
    SUCCESS("H", 0xFF50FF50),
    ERROR("I", 0xFFFF5050),
    INFO("G", 0xFF50D0FF),
    WARN("M", 0xFFFF8030),
    EVENT("F", 0xFFBB50FF),
    ACTIVATE("J", ChatFormatting.GREEN.getColor()),
    DEACTIVATE("K", ChatFormatting.RED.getColor());

    private final String iconName;
    private final int color;
}