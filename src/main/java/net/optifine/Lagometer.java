package net.optifine;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.optifine.reflect.Reflector;
import net.optifine.render.ProjectionMatrixBuffer;
import net.optifine.util.MathUtils;
import net.optifine.util.MemoryMonitor;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class Lagometer {
    private static Minecraft mc;
    public static boolean active = false;
    public static Lagometer.TimerNano timerTick = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerScheduledExecutables = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerChunkUpload = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerChunkUpdate = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerVisibility = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerTerrain = new Lagometer.TimerNano();
    public static Lagometer.TimerNano timerServer = new Lagometer.TimerNano();
    private static long[] timesFrame = new long[512];
    private static long[] timesTick = new long[512];
    private static long[] timesScheduledExecutables = new long[512];
    private static long[] timesChunkUpload = new long[512];
    private static long[] timesChunkUpdate = new long[512];
    private static long[] timesVisibility = new long[512];
    private static long[] timesTerrain = new long[512];
    private static long[] timesServer = new long[512];
    private static boolean[] gcs = new boolean[512];
    private static int numRecordedFrameTimes = 0;
    private static long prevFrameTimeNano = -1L;
    private static long renderTimeNano = 0L;
    private static final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("lagometer");
    private static boolean renderLines;

    public static void updateLagometer() {
        if (mc == null) {
            mc = Minecraft.getInstance();
        }

        if (mc.debugEntries.isOverlayVisible() && mc.getDebugOverlay().isRenderFpsCharts()) {
            active = true;
            long timeNowNano = System.nanoTime();
            if (prevFrameTimeNano == -1L) {
                prevFrameTimeNano = timeNowNano;
            } else {
                int j = numRecordedFrameTimes & timesFrame.length - 1;
                numRecordedFrameTimes++;
                boolean flag = MemoryMonitor.isGcEvent();
                timesFrame[j] = timeNowNano - prevFrameTimeNano - renderTimeNano;
                timesTick[j] = timerTick.timeNano;
                timesScheduledExecutables[j] = timerScheduledExecutables.timeNano;
                timesChunkUpload[j] = timerChunkUpload.timeNano;
                timesChunkUpdate[j] = timerChunkUpdate.timeNano;
                timesVisibility[j] = timerVisibility.timeNano;
                timesTerrain[j] = timerTerrain.timeNano;
                timesServer[j] = timerServer.timeNano;
                gcs[j] = flag;
                timerTick.reset();
                timerScheduledExecutables.reset();
                timerVisibility.reset();
                timerChunkUpdate.reset();
                timerChunkUpload.reset();
                timerTerrain.reset();
                timerServer.reset();
                prevFrameTimeNano = System.nanoTime();
            }
        } else {
            active = false;
            prevFrameTimeNano = -1L;
        }
    }

    public static void renderLagometer(GuiGraphics graphicsIn, int scaleFactor) {
        int i = mc.getWindow().getWidth();
        int j = mc.getWindow().getHeight();
        int k = j - 80;
        int l = j - 160;
        String s = Config.isShowFrameTime() ? "33" : "30";
        String s1 = Config.isShowFrameTime() ? "17" : "60";
        Matrix3x2fStack matrix3x2fstack = graphicsIn.pose();
        matrix3x2fstack.pushMatrix();
        matrix3x2fstack.scale(1.0F / scaleFactor, 1.0F / scaleFactor);
        graphicsIn.drawString(mc.font, s, 1, l, -3881788, false);
        graphicsIn.drawString(mc.font, s1, 1, k, -3881788, false);
        matrix3x2fstack.popMatrix();
        float f = 1.0F - (float)((System.currentTimeMillis() - MemoryMonitor.getStartTimeMs()) / 1000.0);
        f = Config.limit(f, 0.0F, 1.0F);
        int i1 = (int)Mth.lerp(f, 180.0F, 255.0F);
        int j1 = (int)Mth.lerp(f, 110.0F, 155.0F);
        int k1 = (int)Mth.lerp(f, 15.0F, 20.0F);
        int l1 = i1 << 16 | j1 << 8 | k1;
        int i2 = 512 / scaleFactor + 2;
        int j2 = j / scaleFactor - 8;
        graphicsIn.fill(i2 - 1, j2 - 1, i2 + 50, j2 + 10, -1605349296);
        graphicsIn.drawString(mc.font, " " + MemoryMonitor.getGcRateMb() + " MB/s", i2, j2, l1);
        renderLines = true;
    }

    public static void renderLines(GuiRenderer rendererIn) {
        if (renderLines) {
            long i = System.nanoTime();
            GlStateManager._clear(256);
            RenderSystem.backupProjectionMatrix();
            int j = mc.getWindow().getWidth();
            int k = mc.getWindow().getHeight();
            float f = Reflector.ForgeHooksClient_getGuiFarPlane.exists() ? Reflector.ForgeHooksClient_getGuiFarPlane.callFloat() : 21000.0F;
            Matrix4f matrix4f = MathUtils.makeOrtho4f(0.0F, j, 0.0F, k, 1000.0F, f);
            RenderSystem.setProjectionMatrix(projectionMatrixBuffer.getBuffer(matrix4f), ProjectionType.ORTHOGRAPHIC);
            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
            matrix4fstack.pushMatrix();
            matrix4fstack.translate(0.0F, 0.0F, -0.95F * f);
            GlStateManager.disableTexture();
            GlStateManager._depthMask(false);
            GlStateManager._disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (int l = 0; l < timesFrame.length; l++) {
                int i1 = (l - numRecordedFrameTimes & timesFrame.length - 1) * 100 / timesFrame.length;
                i1 += 155;
                float f1 = k;
                long j1 = 0L;
                if (gcs[l]) {
                    j1 = renderTime(l, timesFrame[l], i1, i1 / 2, 0, f1, bufferbuilder);
                } else {
                    j1 = renderTime(l, timesFrame[l], i1, i1, i1, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesServer[l], i1 / 2, i1 / 2, i1 / 2, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesTerrain[l], 0, i1, 0, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesVisibility[l], i1, i1, 0, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesChunkUpdate[l], i1, 0, 0, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesChunkUpload[l], i1, 0, i1, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesScheduledExecutables[l], 0, 0, i1, f1, bufferbuilder);
                    f1 -= (float)renderTime(l, timesTick[l], 0, i1, i1, f1, bufferbuilder);
                }
            }

            renderTimeDivider(0, timesFrame.length, 33333333L, 196, 196, 196, k, bufferbuilder);
            renderTimeDivider(0, timesFrame.length, 16666666L, 196, 196, 196, k, bufferbuilder);
            tesselator.draw(RenderPipelines.LINES, bufferbuilder, () -> "Lagometer");
            GlStateManager._enableCull();
            GlStateManager._depthMask(true);
            GlStateManager.enableTexture();
            matrix4fstack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            renderTimeNano = System.nanoTime() - i;
            renderLines = false;
        }
    }

    private static long renderTime(int frameNum, long time, int r, int g, int b, float baseHeight, BufferBuilder buffer) {
        long i = time / 200000L;
        if (i < 3L) {
            return 0L;
        }

        buffer.addVertex(frameNum + 0.5F, baseHeight - (float)i + 0.5F, 0.0F).setColor(r, g, b, 255).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(1.0F);
        buffer.addVertex(frameNum + 0.5F, baseHeight + 0.5F, 0.0F).setColor(r, g, b, 255).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(1.0F);
        return i;
    }

    private static long renderTimeDivider(int frameStart, int frameEnd, long time, int r, int g, int b, float baseHeight, BufferBuilder buffer) {
        long i = time / 200000L;
        if (i < 3L) {
            return 0L;
        }

        buffer.addVertex(frameStart + 0.5F, baseHeight - (float)i + 0.5F, 0.0F).setColor(r, g, b, 255).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(1.0F);
        buffer.addVertex(frameEnd + 0.5F, baseHeight - (float)i + 0.5F, 0.0F).setColor(r, g, b, 255).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(1.0F);
        return i;
    }

    public static boolean isActive() {
        return active;
    }

    public static class TimerNano {
        public long timeStartNano = 0L;
        public long timeNano = 0L;

        public void start() {
            if (Lagometer.active) {
                if (this.timeStartNano == 0L) {
                    this.timeStartNano = System.nanoTime();
                }
            }
        }

        public void end() {
            if (Lagometer.active) {
                if (this.timeStartNano != 0L) {
                    this.timeNano = this.timeNano + (System.nanoTime() - this.timeStartNano);
                    this.timeStartNano = 0L;
                }
            }
        }

        private void reset() {
            this.timeNano = 0L;
            this.timeStartNano = 0L;
        }
    }
}
