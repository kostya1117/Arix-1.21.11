package net.optifine.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

public class FrameEvent {
    private static Map<String, Integer> mapEventFrames = new HashMap<>();

    public static boolean isActive(String name, int frameInterval) {
        synchronized (mapEventFrames) {
            GameRenderer gamerenderer = Minecraft.getInstance().gameRenderer;
            if (gamerenderer == null) {
                return false;
            }

            int i = gamerenderer.getFrameCount();
            if (i <= 0) {
                return false;
            }

            Integer integer = mapEventFrames.get(name);
            if (integer == null) {
                integer = new Integer(i);
                mapEventFrames.put(name, integer);
            }

            int j = integer;
            if (i > j && i < j + frameInterval) {
                return false;
            }

            mapEventFrames.put(name, new Integer(i));
            return true;
        }
    }
}
