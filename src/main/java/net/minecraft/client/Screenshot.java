package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Screenshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";

    public static void grab(File p_92290_, RenderTarget p_92293_, Consumer<Component> p_92294_) {
        grab(p_92290_, null, p_92293_, 1, p_92294_);
    }

    public static void grab(File p_92296_, @Nullable String p_92297_, RenderTarget p_92300_, int p_407783_, Consumer<Component> p_92301_) {
        Minecraft minecraft = Config.getMinecraft();
        Window window = minecraft.getWindow();
        Options options = Config.getGameSettings();
        int i = window.getWidth();
        int j = window.getHeight();
        int k = options.guiScale().get();
        int l = window.calculateScale(minecraft.options.guiScale().get(), minecraft.options.forceUnicodeFont().get());
        int i1 = Config.getScreenshotSize();
        boolean flag = p_92300_ == minecraft.getMainRenderTarget() && i1 > 1;
        if (flag) {
            options.guiScale().set(l * i1);

            try {
                window.resizeFramebuffer(i * i1, j * i1);
            } catch (Exception exception) {
                LOGGER.warn("Couldn't save screenshot", exception);
                p_92301_.accept(Component.translatable("screenshot.failure", exception.getMessage()));
            }

            RenderTarget rendertarget = minecraft.getMainRenderTarget();
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
            RenderSystem.getModelViewStack().pushMatrix();
            minecraft.gameRenderer.render(minecraft.getDeltaTracker(), true);
            RenderSystem.getModelViewStack().popMatrix();
        }

        takeScreenshot(
            p_92300_,
            p_407783_,
            imageIn -> {
                File file1 = new File(p_92296_, "screenshots");
                file1.mkdir();
                File file2;
                if (p_92297_ == null) {
                    file2 = getFile(file1);
                } else {
                    file2 = new File(file1, p_92297_);
                }

                Object object = null;
                if (Reflector.ScreenshotEvent_Constructor.exists()) {
                    object = Reflector.ScreenshotEvent_Constructor.newInstance(imageIn, file2);
                    if (Reflector.postForgeBusEvent(Reflector.ScreenshotEvent_BUS, object)) {
                        Component component = (Component)Reflector.call(object, Reflector.ScreenshotEvent_getCancelMessage);
                        p_92301_.accept(component);
                        return;
                    }

                    file2 = (File)Reflector.call(object, Reflector.ScreenshotEvent_getScreenshotFile);
                }

                File file3 = file2;
                Object object1 = object;
                Util.ioPool()
                    .execute(
                        () -> {
                            try {
                                NativeImage nativeimage = imageIn;

                                try {
                                    imageIn.writeToFile(file3);
                                    Component component1 = Component.literal(file3.getName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(styleIn -> styleIn.withClickEvent(new ClickEvent.OpenFile(file3.getAbsoluteFile())));
                                    if (object1 != null && Reflector.call(object1, Reflector.ScreenshotEvent_getResultMessage) != null) {
                                        p_92301_.accept((Component)Reflector.call(object1, Reflector.ScreenshotEvent_getResultMessage));
                                    } else {
                                        p_92301_.accept(Component.translatable("screenshot.success", component1));
                                    }
                                } catch (Throwable throwable1) {
                                    if (imageIn != null) {
                                        try {
                                            nativeimage.close();
                                        } catch (Throwable throwable) {
                                            throwable1.addSuppressed(throwable);
                                        }
                                    }

                                    throw throwable1;
                                }

                                if (imageIn != null) {
                                    imageIn.close();
                                }
                            } catch (Exception exception1) {
                                LOGGER.warn("Couldn't save screenshot", exception1);
                                p_92301_.accept(Component.translatable("screenshot.failure", exception1.getMessage()));
                            }
                        }
                    );
            }
        );
        if (flag) {
            Config.getGameSettings().guiScale().set(k);
            window.resizeFramebuffer(i, j);
        }
    }

    public static void takeScreenshot(RenderTarget p_92282_, Consumer<NativeImage> p_391783_) {
        takeScreenshot(p_92282_, 1, p_391783_);
    }

    public static void takeScreenshot(RenderTarget p_410184_, int p_407182_, Consumer<NativeImage> p_409284_) {
        int i = p_410184_.width;
        int j = p_410184_.height;
        GpuTexture gputexture = p_410184_.getColorTexture();
        if (gputexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        }

        if (i % p_407182_ == 0 && j % p_407182_ == 0) {
            GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)i * j * gputexture.getFormat().pixelSize());
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToBuffer(
                    gputexture,
                    gpubuffer,
                    0L,
                    () -> {
                        try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                            int k = j / p_407182_;
                            int l = i / p_407182_;
                            NativeImage nativeimage = new NativeImage(l, k, false);

                            for (int i1 = 0; i1 < k; i1++) {
                                for (int j1 = 0; j1 < l; j1++) {
                                    if (p_407182_ == 1) {
                                        int i3 = gpubuffer$mappedview.data().getInt((j1 + i1 * i) * gputexture.getFormat().pixelSize());
                                        nativeimage.setPixelABGR(j1, j - i1 - 1, i3 | 0xFF000000);
                                    } else {
                                        int k1 = 0;
                                        int l1 = 0;
                                        int i2 = 0;

                                        for (int j2 = 0; j2 < p_407182_; j2++) {
                                            for (int k2 = 0; k2 < p_407182_; k2++) {
                                                int l2 = gpubuffer$mappedview.data()
                                                    .getInt((j1 * p_407182_ + j2 + (i1 * p_407182_ + k2) * i) * gputexture.getFormat().pixelSize());
                                                k1 += ARGB.red(l2);
                                                l1 += ARGB.green(l2);
                                                i2 += ARGB.blue(l2);
                                            }
                                        }

                                        int j3 = p_407182_ * p_407182_;
                                        nativeimage.setPixelABGR(j1, k - i1 - 1, ARGB.color(255, k1 / j3, l1 / j3, i2 / j3));
                                    }
                                }
                            }

                            p_409284_.accept(nativeimage);
                        }

                        gpubuffer.close();
                    },
                    0
                );
        } else {
            throw new IllegalArgumentException("Image size is not divisible by downscale factor");
        }
    }

    private static File getFile(File p_92288_) {
        String s = Util.getFilenameFormattedDateTime();
        int i = 1;

        while (true) {
            File file1 = new File(p_92288_, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            i++;
        }
    }
}
