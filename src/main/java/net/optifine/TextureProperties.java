package net.optifine;

import java.util.Properties;
import net.optifine.config.ConfigUtils;
import net.optifine.shaders.ITextureFormat;
import net.optifine.shaders.TextureFormatLabPbr;

public class TextureProperties {
    private static ITextureFormat textureFormat;
    private static final int ALPHA_CUTOUT = 16;
    private static int alphaCutout = 16;

    public static void update() {
        textureFormat = null;
        alphaCutout = 16;

        try {
            Properties properties = ConfigUtils.readProperties("optifine/texture.properties");
            if (properties == null) {
                return;
            }

            if (Config.isShaders()) {
                textureFormat = parseTextureFormat(properties.getProperty("format"));
            }

            alphaCutout = parseAlphaCutout(properties.getProperty("alpha.cutout"));
        } catch (Exception exception) {
            Config.warn("Error reading texture.properties", exception);
        }
    }

    private static int parseAlphaCutout(String prop) {
        if (prop == null) {
            return 16;
        }

        int i = Config.parseInt(prop, 16);
        i = Config.limit(i, 1, 255);
        Config.log("Alpha cutout: " + i);
        return i;
    }

    private static ITextureFormat parseTextureFormat(String formatStr) {
        if (formatStr == null) {
            return null;
        }

        String[] astring = Config.tokenize(formatStr, "/");
        String s = astring[0];
        String s1 = astring.length > 1 ? astring[1] : null;
        if (s.equals("lab-pbr")) {
            return new TextureFormatLabPbr(s1);
        }

        Config.warn("Unknown texture format: " + formatStr);
        return null;
    }

    public static ITextureFormat getTextureFormat() {
        return textureFormat;
    }

    public static int getAlphaCutout() {
        return alphaCutout;
    }
}
