package ru.arixcompany.features.event;

import lombok.Getter;

@Getter
public enum EventPriority {
    LOWEST(-200),
    LOW(-100),
    NORMAL(0),
    HIGH(100),
    HIGHEST(200);

    private final int value;

    EventPriority(int value) {
        this.value = value;
    }
}
