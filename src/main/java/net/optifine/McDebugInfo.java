package net.optifine;

import net.minecraft.client.Minecraft;
import net.optifine.reflect.Reflector;

public class McDebugInfo {
    private Minecraft minecraft = Minecraft.getInstance();
    private long debugUpdateTimeLast = 0L;

    public McDebugInfo() {
        this.debugUpdateTimeLast = this.getDebugUpdateTime();
    }

    public boolean isChanged() {
        long i = this.getDebugUpdateTime();
        if (i == this.debugUpdateTimeLast) {
            return false;
        }

        this.debugUpdateTimeLast = i;
        return true;
    }

    private long getDebugUpdateTime() {
        return Reflector.getFieldValueLong(this.minecraft, Reflector.Minecraft_debugUpdateTime, -1L);
    }
}
