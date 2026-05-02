package net.optifine.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.optifine.Config;

public class RenderUtils {
    private static boolean flushRenderBuffers = true;
    private static long timeCheckMs = 0L;
    private static Minecraft mc = Minecraft.getInstance();

    public static boolean setFlushRenderBuffers(boolean flushRenderBuffers) {
        boolean flag = RenderUtils.flushRenderBuffers;
        RenderUtils.flushRenderBuffers = flushRenderBuffers;
        return flag;
    }

    public static boolean isFlushRenderBuffers() {
        return flushRenderBuffers;
    }

    public static void flushRenderBuffers() {
        if (flushRenderBuffers) {
            RenderBuffers renderbuffers = mc.renderBuffers();
            renderbuffers.bufferSource().flushRenderBuffers();
            renderbuffers.crumblingBufferSource().flushRenderBuffers();
        }
    }

    public static void finishRenderBuffers() {
        RenderBuffers renderbuffers = mc.renderBuffers();
        renderbuffers.bufferSource().endBatch();
        renderbuffers.crumblingBufferSource().endBatch();
    }

    public static boolean isMainFramebuffer(int framebufferIn) {
        return mc.getMainRenderTarget().hasFramebuffer(framebufferIn);
    }

    public static void frameStart() {
        if (System.currentTimeMillis() >= timeCheckMs) {
            timeCheckMs = System.currentTimeMillis() + 3000L;
            if (!flushRenderBuffers) {
                Config.warn("Flush render buffers is disabled!");
            }
        }
    }
}
