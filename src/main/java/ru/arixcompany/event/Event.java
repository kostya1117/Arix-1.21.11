package ru.arixcompany.event;

import lombok.Getter;
import ru.arixcompany.utils.IMinecraft;

@Getter
public class Event implements IMinecraft {
    private boolean cancelled = false;

    public void cancel() { cancelled = true; }
}