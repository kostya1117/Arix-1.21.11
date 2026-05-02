package com.mojang.blaze3d.opengl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.optifine.Config;
import net.optifine.GlErrors;
import net.optifine.util.ArrayUtils;
import net.optifine.util.StrUtils;
import net.optifine.util.TimedEvent;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

public class GlDebug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CIRCULAR_LOG_SIZE = 10;
    private final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
    private volatile GlDebug. LogEntry lastEntry;
    private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
    private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);
    private static int[] ignoredErrors = makeIgnoredErrors();

    private static int[] makeIgnoredErrors() {
        String s = System.getProperty("gl.ignore.errors");
        if (s == null) {
            return new int[0];
        }

        String[] astring = Config.tokenize(s, ",");
        int[] aint = new int[0];

        for (int i = 0; i < astring.length; i++) {
            String s1 = astring[i].trim();
            int j = s1.startsWith("0x") ? Config.parseHexInt(s1, -1) : Config.parseInt(s1, -1);
            if (j < 0) {
                Config.warn("Invalid error id: " + s1);
            } else {
                Config.log("Ignore OpenGL error: " + j);
                aint = ArrayUtils.addIntToArray(aint, j);
            }
        }

        return aint;
    }

    private static String printUnknownToken(int p_396556_) {
        return "Unknown (0x" + HexFormat.of().withUpperCase().toHexDigits(p_396556_) + ")";
    }

    public static String sourceToString(int p_392197_) {
        switch (p_392197_) {
            case 33350:
                return "API";
            case 33351:
                return "WINDOW SYSTEM";
            case 33352:
                return "SHADER COMPILER";
            case 33353:
                return "THIRD PARTY";
            case 33354:
                return "APPLICATION";
            case 33355:
                return "OTHER";
            default:
                return printUnknownToken(p_392197_);
        }
    }

    public static String typeToString(int p_393670_) {
        switch (p_393670_) {
            case 33356:
                return "ERROR";
            case 33357:
                return "DEPRECATED BEHAVIOR";
            case 33358:
                return "UNDEFINED BEHAVIOR";
            case 33359:
                return "PORTABILITY";
            case 33360:
                return "PERFORMANCE";
            case 33361:
                return "OTHER";
            case 33384:
                return "MARKER";
            default:
                return printUnknownToken(p_393670_);
        }
    }

    public static String severityToString(int p_395913_) {
        switch (p_395913_) {
            case 33387:
                return "NOTIFICATION";
            case 37190:
                return "HIGH";
            case 37191:
                return "MEDIUM";
            case 37192:
                return "LOW";
            default:
                return printUnknownToken(p_395913_);
        }
    }

    private void printDebugLog(int p_391432_, int p_393126_, int p_395489_, int p_393407_, int p_397884_, long p_395821_, long p_396708_) {
        if (p_393126_ != 33385 && p_393126_ != 33386) {
            if (!ArrayUtils.contains(ignoredErrors, p_395489_)) {
                if (!Config.isShaders() || p_391432_ != 33352) {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft == null || minecraft.getWindow() == null || !minecraft.getWindow().isClosed()) {
                        if (GlErrors.isEnabled(p_395489_)) {
                            String s = sourceToString(p_391432_);
                            String s1 = typeToString(p_393126_);
                            String s2 = severityToString(p_393407_);
                            String s3 = GLDebugMessageCallback.getMessage(p_397884_, p_395821_);
                            s3 = StrUtils.trim(s3, " \n\r\t");
                            String s4 = String.format("OpenGL %s %s: %s (%s)", s, s1, p_395489_, s3);
                            Exception exception = new Exception("Stack trace");
                            StackTraceElement[] astacktraceelement = exception.getStackTrace();
                            StackTraceElement[] astacktraceelement1 = astacktraceelement.length > 2
                                ? Arrays.copyOfRange(astacktraceelement, 2, astacktraceelement.length)
                                : astacktraceelement;
                            exception.setStackTrace(astacktraceelement1);
                            if (RenderType.drawRenderType != null) {
                                LOGGER.info("Draw RenderType: " + RenderType.drawRenderType);
                            }

                            if (p_393126_ == 33356) {
                                LOGGER.error(s4, exception);
                            } else {
                                LOGGER.info(s4, exception);
                            }

                            if (Config.isShowGlErrors() && TimedEvent.isActive("ShowGlErrorDebug", 10000L) && minecraft.level != null) {
                                String s5 = Config.getGlErrorString(p_395489_);
                                if (p_395489_ == 0 || Config.equals(s5, "Unknown")) {
                                    s5 = s3;
                                }

                                String s6 = I18n.get("of.message.openglError", p_395489_, s5);
                                minecraft.schedule(() -> minecraft.gui.getChat().addMessage(Component.literal(s6)));
                            }

                            String s7 = GLDebugMessageCallback.getMessage(p_397884_, p_395821_);
                            synchronized (this.MESSAGE_BUFFER) {
                                GlDebug.LogEntry gldebug$logentry = this.lastEntry;
                                if (gldebug$logentry != null && gldebug$logentry.isSame(p_391432_, p_393126_, p_395489_, p_393407_, s7)) {
                                    gldebug$logentry.count++;
                                } else {
                                    gldebug$logentry = new GlDebug.LogEntry(p_391432_, p_393126_, p_395489_, p_393407_, s7);
                                    this.MESSAGE_BUFFER.add(gldebug$logentry);
                                    this.lastEntry = gldebug$logentry;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public List<String> getLastOpenGlDebugMessages() {
        synchronized (this.MESSAGE_BUFFER) {
            List<String> list = Lists.newArrayListWithCapacity(this.MESSAGE_BUFFER.size());

            for (GlDebug.LogEntry gldebug$logentry : this.MESSAGE_BUFFER) {
                list.add(gldebug$logentry + " x " + gldebug$logentry.count);
            }

            return list;
        }
    }

    public static  GlDebug enableDebugCallback(int p_394351_, boolean p_393026_, Set<String> p_393339_) {
        if (p_394351_ <= 0) {
            return null;
        }

        GLCapabilities glcapabilities = GL.getCapabilities();
        if (glcapabilities.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
            GlDebug gldebug1 = new GlDebug();
            p_393339_.add("GL_KHR_debug");
            GL11.glEnable(37600);
            if (p_393026_) {
                GL11.glEnable(33346);
            }

            for (int j = 0; j < DEBUG_LEVELS.size(); j++) {
                boolean flag1 = j < p_394351_;
                KHRDebug.glDebugMessageControl(4352, 4352, DEBUG_LEVELS.get(j), (int[])null, flag1);
            }

            KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(gldebug1::printDebugLog), DebugMemoryUntracker::untrack), 0L);
            return gldebug1;
        } else if (glcapabilities.GL_ARB_debug_output && GlDevice.USE_GL_ARB_debug_output) {
            GlDebug gldebug = new GlDebug();
            p_393339_.add("GL_ARB_debug_output");
            if (p_393026_) {
                GL11.glEnable(33346);
            }

            for (int i = 0; i < DEBUG_LEVELS_ARB.size(); i++) {
                boolean flag = i < p_394351_;
                ARBDebugOutput.glDebugMessageControlARB(4352, 4352, DEBUG_LEVELS_ARB.get(i), (int[])null, flag);
            }

            ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(gldebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
            return gldebug;
        } else {
            return null;
        }
    }

    static class LogEntry {
        private final int id;
        private final int source;
        private final int type;
        private final int severity;
        private final String message;
        int count = 1;

        LogEntry(int p_393196_, int p_394115_, int p_392842_, int p_391912_, String p_391249_) {
            this.id = p_392842_;
            this.source = p_393196_;
            this.type = p_394115_;
            this.severity = p_391912_;
            this.message = p_391249_;
        }

        boolean isSame(int p_397925_, int p_394526_, int p_397126_, int p_397660_, String p_391197_) {
            return p_394526_ == this.type
                && p_397925_ == this.source
                && p_397126_ == this.id
                && p_397660_ == this.severity
                && p_391197_.equals(this.message);
        }

        @Override
        public String toString() {
            return "id="
                + this.id
                + ", source="
                + GlDebug.sourceToString(this.source)
                + ", type="
                + GlDebug.typeToString(this.type)
                + ", severity="
                + GlDebug.severityToString(this.severity)
                + ", message='"
                + this.message
                + "'";
        }
    }
}
