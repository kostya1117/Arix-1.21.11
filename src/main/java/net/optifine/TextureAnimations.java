package net.optifine;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.ResUtils;
import net.optifine.util.TextureUtils;

public class TextureAnimations {
    private static TextureAnimation[] textureAnimations = null;
    private static int countAnimationsActive = 0;
    private static int frameCountAnimations = 0;

    public static void reset() {
        textureAnimations = null;
    }

    public static void update() {
        textureAnimations = null;
        countAnimationsActive = 0;
        PackResources[] apackresources = Config.getResourcePacks();
        textureAnimations = getTextureAnimations(apackresources);
        updateAnimations();
    }

    public static void updateAnimations() {
        if (textureAnimations != null && Config.isAnimatedTextures()) {
            int i = 0;

            for (int j = 0; j < textureAnimations.length; j++) {
                TextureAnimation textureanimation = textureAnimations[j];
                textureanimation.updateTexture();
                if (textureanimation.isActive()) {
                    i++;
                }
            }

            int k = Config.getMinecraft().levelRenderer.getFrameCount();
            if (k != frameCountAnimations) {
                countAnimationsActive = i;
                frameCountAnimations = k;
            }

            if (SmartAnimations.isActive()) {
                SmartAnimations.resetTexturesRendered();
            }
        } else {
            countAnimationsActive = 0;
        }
    }

    private static TextureAnimation[] getTextureAnimations(PackResources[] rps) {
        List list = new ArrayList();

        for(int i = 0; i < rps.length; ++i) {
            PackResources rp = rps[i];
            TextureAnimation[] tas = getTextureAnimations(rp);
            if (tas != null) {
                list.addAll(Arrays.asList(tas));
            }
        }

        TextureAnimation[] anims = (TextureAnimation[])list.toArray(new TextureAnimation[list.size()]);
        return anims;
    }


    private static TextureAnimation[] getTextureAnimations(PackResources rp) {
        String[] animPropNames = ResUtils.collectFiles(rp, (String)"optifine/anim/", (String)".properties", (String[])null);
        if (animPropNames.length <= 0) {
            return null;
        } else {
            List list = new ArrayList();

            for(int i = 0; i < animPropNames.length; ++i) {
                String propName = animPropNames[i];
                Config.dbg("Texture animation: " + propName);

                try {
                    Identifier propLoc = new Identifier(propName);
                    InputStream in = Config.getResourceStream(rp, PackType.CLIENT_RESOURCES, propLoc);
                    Properties props = new PropertiesOrdered();
                    props.load(in);
                    in.close();
                    TextureAnimation anim = makeTextureAnimation(props, propLoc);
                    if (anim != null) {
                        Identifier locDstTex = new Identifier(anim.getDstTex());
                        if (!Config.hasResource(rp, locDstTex)) {
                            Config.dbg("Skipped: " + propName + ", target texture not loaded from same resource pack");
                        } else {
                            list.add(anim);
                        }
                    }
                } catch (FileNotFoundException var10) {
                    Config.warn("File not found: " + var10.getMessage());
                } catch (IOException var11) {
                    var11.printStackTrace();
                }
            }

            TextureAnimation[] anims = (TextureAnimation[])list.toArray(new TextureAnimation[list.size()]);
            return anims;
        }
    }

    private static TextureAnimation makeTextureAnimation(Properties props, Identifier propLoc) {
        String s = props.getProperty("from");
        String s1 = props.getProperty("to");
        int i = Config.parseInt(props.getProperty("x"), -1);
        int j = Config.parseInt(props.getProperty("y"), -1);
        int k = Config.parseInt(props.getProperty("w"), -1);
        int l = Config.parseInt(props.getProperty("h"), -1);
        if (s == null || s1 == null) {
            Config.warn("TextureAnimation: Source or target texture not specified");
            return null;
        }

        if (i >= 0 && j >= 0 && k >= 0 && l >= 0) {
            s = s.trim();
            s1 = s1.trim();
            String s2 = TextureUtils.getBasePath(propLoc.getPath());
            s = TextureUtils.fixResourcePath(s, s2);
            s1 = TextureUtils.fixResourcePath(s1, s2);
            byte[] abyte = getCustomTextureData(s, k);
            if (abyte == null) {
                Config.warn("TextureAnimation: Source texture not found: " + s1);
                return null;
            }

            int i1 = abyte.length / 4;
            int j1 = i1 / (k * l);
            int k1 = j1 * k * l;
            if (i1 != k1) {
                Config.warn("TextureAnimation: Source texture has invalid number of frames: " + s + ", frames: " + (float)i1 / (k * l));
                return null;
            }

            Identifier identifier = new Identifier(s1);

            try {
                InputStream inputstream = Config.getResourceStream(identifier);
                if (inputstream == null) {
                    Config.warn("TextureAnimation: Target texture not found: " + s1);
                    return null;
                }

                BufferedImage bufferedimage = readTextureImage(inputstream);
                if (i + k <= bufferedimage.getWidth() && j + l <= bufferedimage.getHeight()) {
                    return new TextureAnimation(s, abyte, s1, identifier, i, j, k, l, props);
                }

                Config.warn("TextureAnimation: Animation coordinates are outside the target texture: " + s1);
                return null;
            } catch (IOException ioexception) {
                Config.warn("TextureAnimation: Target texture not found: " + s1);
                return null;
            }
        } else {
            Config.warn("TextureAnimation: Invalid coordinates");
            return null;
        }
    }

    private static byte[] getCustomTextureData(String imagePath, int tileWidth) {
        byte[] abyte = loadImage(imagePath, tileWidth);
        if (abyte == null) {
            abyte = loadImage("/anim" + imagePath, tileWidth);
        }

        return abyte;
    }

    private static byte[] loadImage(String name, int targetWidth) {
        Options options = Config.getGameSettings();

        try {
            Identifier identifier = new Identifier(name);
            InputStream inputstream = Config.getResourceStream(identifier);
            if (inputstream == null) {
                return null;
            }

            BufferedImage bufferedimage = readTextureImage(inputstream);
            inputstream.close();
            if (bufferedimage == null) {
                return null;
            }

            if (targetWidth > 0 && bufferedimage.getWidth() != targetWidth) {
                double d0 = bufferedimage.getHeight() / bufferedimage.getWidth();
                int j = (int)(targetWidth * d0);
                bufferedimage = scaleBufferedImage(bufferedimage, targetWidth, j);
            }

            int l1 = bufferedimage.getWidth();
            int i = bufferedimage.getHeight();
            int[] aint = new int[l1 * i];
            byte[] abyte = new byte[l1 * i * 4];
            bufferedimage.getRGB(0, 0, l1, i, aint, 0, l1);

            for (int k = 0; k < aint.length; k++) {
                int l = aint[k] >> 24 & 0xFF;
                int i1 = aint[k] >> 16 & 0xFF;
                int j1 = aint[k] >> 8 & 0xFF;
                int k1 = aint[k] & 0xFF;
                abyte[k * 4 + 0] = (byte)i1;
                abyte[k * 4 + 1] = (byte)j1;
                abyte[k * 4 + 2] = (byte)k1;
                abyte[k * 4 + 3] = (byte)l;
            }

            return abyte;
        } catch (FileNotFoundException filenotfoundexception) {
            return null;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static BufferedImage readTextureImage(InputStream par1InputStream) throws IOException {
        BufferedImage bufferedimage = ImageIO.read(par1InputStream);
        par1InputStream.close();
        return bufferedimage;
    }

    private static BufferedImage scaleBufferedImage(BufferedImage image, int width, int height) {
        BufferedImage bufferedimage = new BufferedImage(width, height, 2);
        Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2d.drawImage(image, 0, 0, width, height, null);
        return bufferedimage;
    }

    public static int getCountAnimations() {
        return textureAnimations == null ? 0 : textureAnimations.length;
    }

    public static int getCountAnimationsActive() {
        return countAnimationsActive;
    }
}
