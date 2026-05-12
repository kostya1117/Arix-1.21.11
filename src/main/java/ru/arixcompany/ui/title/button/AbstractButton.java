package ru.arixcompany.ui.title.button;

import lombok.RequiredArgsConstructor;
import ru.arixcompany.utils.IMinecraft;

@RequiredArgsConstructor
public abstract class AbstractButton implements Button, IMinecraft {
    public final String name;
    public final Runnable action;

    public float x, y, width, height;

    public AbstractButton size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public AbstractButton position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
}
