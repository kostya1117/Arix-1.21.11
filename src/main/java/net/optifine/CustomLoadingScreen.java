package net.optifine;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Properties;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.optifine.render.BufferUploader;

public class CustomLoadingScreen {
    private Identifier locationTexture;
    private int scaleMode = 0;
    private int scale = 2;
    private boolean center;
    private static final int SCALE_DEFAULT = 2;
    private static final int SCALE_MODE_FIXED = 0;
    private static final int SCALE_MODE_FULL = 1;
    private static final int SCALE_MODE_STRETCH = 2;

    public CustomLoadingScreen(Identifier locationTexture, int scaleMode, int scale, boolean center) {
        this.locationTexture = locationTexture;
        this.scaleMode = scaleMode;
        this.scale = scale;
        this.center = center;
    }

    public static CustomLoadingScreen parseScreen(String path, int dimId, Properties props) {
        Identifier identifier = new Identifier(path);
        int i = parseScaleMode(getProperty("scaleMode", dimId, props));
        int j = i == 0 ? 2 : 1;
        int k = parseScale(getProperty("scale", dimId, props), j);
        boolean flag = Config.parseBoolean(getProperty("center", dimId, props), false);
        return new CustomLoadingScreen(identifier, i, k, flag);
    }

    private static String getProperty(String key, int dim, Properties props) {
        if (props == null) {
            return null;
        }

        String s = props.getProperty("dim" + dim + "." + key);
        return s != null ? s : props.getProperty(key);
    }

    private static int parseScaleMode(String str) {
        if (str == null) {
            return 0;
        }

        str = str.toLowerCase().trim();
        if (str.equals("fixed")) {
            return 0;
        }

        if (str.equals("full")) {
            return 1;
        }

        if (str.equals("stretch")) {
            return 2;
        }

        CustomLoadingScreens.warn("Invalid scale mode: " + str);
        return 0;
    }

    private static int parseScale(String str, int def) {
        if (str == null) {
            return def;
        } else {
            str = str.trim();
            int i = Config.parseInt(str, -1);
            if (i < 1) {
                CustomLoadingScreens.warn("Invalid scale: " + str);
                return def;
            } else {
                return i;
            }
        }
    }

    public void drawBackground(int width, int height) {
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float f = 16 * this.scale;
        float f1 = width / f;
        float f2 = height / f;
        float f3 = 0.0F;
        float f4 = 0.0F;
        if (this.center) {
            f3 = (f - width) / (f * 2.0F);
            f4 = (f - height) / (f * 2.0F);
        }

        switch (this.scaleMode) {
            case 1:
                f = Math.max(width, height);
                f1 = this.scale * width / f;
                f2 = this.scale * height / f;
                if (this.center) {
                    f3 = this.scale * (f - width) / (f * 2.0F);
                    f4 = this.scale * (f - height) / (f * 2.0F);
                }
                break;
            case 2:
                f1 = this.scale;
                f2 = this.scale;
                f3 = 0.0F;
                f4 = 0.0F;
        }

        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(0.0F, height, 0.0F).setUv(f3, f4 + f2).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(width, height, 0.0F).setUv(f3 + f1, f4 + f2).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(width, 0.0F, 0.0F).setUv(f3 + f1, f4).setColor(255, 255, 255, 255);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(f3, f4).setColor(255, 255, 255, 255);
        BufferUploader.draw(
            RenderPipelines.GUI_TEXTURED, bufferbuilder.buildOrThrow(), () -> "CustomLoadingScreen.background", this.locationTexture, FilterMode.NEAREST
        );
    }
}
