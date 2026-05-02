package net.minecraftforge.common.util;

public enum Result {
    ALLOW,
    DEFAULT,
    DENY;

    public boolean isAllowed() {
        return this == ALLOW;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isDenied() {
        return this == DENY;
    }
}
