package net.optifine.shaders;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.util.SamplerUtils;
import net.optifine.util.TextureUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ShadersTex {
    public static final int initialBufferSize = 1048576;
    public static ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4194304);
    public static IntBuffer intBuffer = byteBuffer.asIntBuffer();
    public static int[] intArray = new int[1048576];
    public static final int defBaseTexColor = 0;
    public static final int defNormTexColor = -8421377;
    public static final int defSpecTexColor = 0;
    public static Map<Integer, MultiTexID> multiTexMap = new HashMap<>();

    public static IntBuffer getIntBuffer(int size) {
        if (intBuffer.capacity() < size) {
            int i = roundUpPOT(size);
            byteBuffer = BufferUtils.createByteBuffer(i * 4);
            intBuffer = byteBuffer.asIntBuffer();
        }

        return intBuffer;
    }

    public static int[] getIntArray(int size) {
        if (intArray == null) {
            intArray = new int[1048576];
        }

        if (intArray.length < size) {
            intArray = new int[roundUpPOT(size)];
        }

        return intArray;
    }

    public static int roundUpPOT(int x) {
        int i = x - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    public static int log2(int x) {
        int i = 0;
        if ((x & -65536) != 0) {
            i += 16;
            x >>= 16;
        }

        if ((x & 0xFF00) != 0) {
            i += 8;
            x >>= 8;
        }

        if ((x & 240) != 0) {
            i += 4;
            x >>= 4;
        }

        if ((x & 6) != 0) {
            i += 2;
            x >>= 2;
        }

        if ((x & 2) != 0) {
            i++;
        }

        return i;
    }

    public static IntBuffer fillIntBuffer(int size, int value) {
        int[] aint = getIntArray(size);
        IntBuffer intbuffer = getIntBuffer(size);
        Arrays.fill(intArray, 0, size, value);
        intBuffer.put(intArray, 0, size);
        return intBuffer;
    }

    public static MultiTexID getMultiTexID(GlTexture tex) {
        MultiTexID multitexid = tex.multiTex;
        if (multitexid == null) {
            int i = tex.getGlTextureId();
            multitexid = multiTexMap.get(i);
            if (multitexid == null) {
                GlTexture gltexture = (GlTexture)RenderSystem.getGlDevice()
                    .createTexture(
                        tex.getLabel() + ".norm", tex.usage(), tex.getFormat(), tex.getWidth(), tex.getHeight(), tex.getDepthOrLayers(), tex.getMipLevels()
                    );
                GlTexture gltexture1 = (GlTexture)RenderSystem.getGlDevice()
                    .createTexture(
                        tex.getLabel() + ".spec", tex.usage(), tex.getFormat(), tex.getWidth(), tex.getHeight(), tex.getDepthOrLayers(), tex.getMipLevels()
                    );
                multitexid = new MultiTexID(tex, gltexture, gltexture1);
                initTextureNS(multitexid, tex.getWidth(), tex.getHeight());
                multiTexMap.put(i, multitexid);
            }

            tex.multiTex = multitexid;
        }

        return multitexid;
    }

    public static void deleteTextures(GlTexture atex, int texid) {
        MultiTexID multitexid = atex.multiTex;
        if (multitexid != null) {
            atex.multiTex = null;
            multiTexMap.remove(multitexid.base);
            GlStateManager._deleteTexture(multitexid.norm);
            GlStateManager._deleteTexture(multitexid.spec);
            if (multitexid.base != texid) {
                SMCLog.warning("Error : MultiTexID.base mismatch: " + multitexid.base + ", texid: " + texid);
                GlStateManager._deleteTexture(multitexid.base);
            }
        }
    }

    public static void bindNSTextures(int normTex, int specTex, boolean normalBlend, boolean specularBlend, boolean mipmaps) {
        if (Shaders.isRenderingWorld && GlStateManager.getActiveTextureUnit() == 33984) {
            if (Shaders.configNormalMap) {
                GlStateManager._activeTexture(33985);
                GlStateManager._bindTexture(normTex);
                if (!normalBlend) {
                    int i = mipmaps ? 9984 : 9728;
                    GlStateManager._texParameter(3553, 10241, i);
                    GlStateManager._texParameter(3553, 10240, 9728);
                }
            }

            if (Shaders.configSpecularMap) {
                GlStateManager._activeTexture(33987);
                GlStateManager._bindTexture(specTex);
                if (!specularBlend) {
                    int j = mipmaps ? 9984 : 9728;
                    GlStateManager._texParameter(3553, 10241, j);
                    GlStateManager._texParameter(3553, 10240, 9728);
                }
            }

            GlStateManager._activeTexture(33984);
        }
    }

    public static void bindNSTextures(MultiTexID multiTex) {
        bindNSTextures(multiTex.norm, multiTex.spec, true, true, false);
    }

    public static void bindTextures(int baseTex, int normTex, int specTex) {
        if (Shaders.isRenderingWorld && GlStateManager.getActiveTextureUnit() == 33984) {
            GlStateManager._activeTexture(33985);
            GlStateManager._bindTexture(normTex);
            GlStateManager._activeTexture(33987);
            GlStateManager._bindTexture(specTex);
            GlStateManager._activeTexture(33984);
        }

        GlStateManager._bindTexture(baseTex);
    }

    public static void bindTextures(MultiTexID multiTex, boolean normalBlend, boolean specularBlend, boolean mipmaps) {
        if (Shaders.isRenderingWorld && GlStateManager.getActiveTextureUnit() == 33984) {
            if (Shaders.configNormalMap) {
                GlStateManager._activeTexture(33985);
                GlStateManager._bindTexture(multiTex.norm);
                if (!normalBlend) {
                    int i = mipmaps ? 9984 : 9728;
                    GlStateManager._texParameter(3553, 10241, i);
                    GlStateManager._texParameter(3553, 10240, 9728);
                }
            }

            if (Shaders.configSpecularMap) {
                GlStateManager._activeTexture(33987);
                GlStateManager._bindTexture(multiTex.spec);
                if (!specularBlend) {
                    int j = mipmaps ? 9984 : 9728;
                    GlStateManager._texParameter(3553, 10241, j);
                    GlStateManager._texParameter(3553, 10240, 9728);
                }
            }

            GlStateManager._activeTexture(33984);
        }

        GlStateManager._bindTexture(multiTex.base);
    }

    public static void bindTexture(GlTexture texture) {
        bindTexture(texture, texture.getParentTexture());
    }

    public static void bindTexture(AbstractTexture texture) {
        GlTexture gltexture = texture.getGlTexture();
        if (gltexture != null) {
            bindTexture(gltexture, texture);
        }
    }

    private static void bindTexture(GlTexture glTexture, AbstractTexture parentTexture) {
        int i = glTexture.getGlTextureId();
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = false;
        if (parentTexture instanceof TextureAtlas textureatlas) {
            flag = textureatlas.isNormalBlend();
            flag1 = textureatlas.isSpecularBlend();
            flag2 = textureatlas.isMipmaps();
        }

        MultiTexID multitexid = glTexture.getMultiTexID();
        if (multitexid != null) {
            bindTextures(multitexid, flag, flag1, flag2);
        } else {
            GlStateManager._bindTexture(i);
        }

        if (GlStateManager.getActiveTextureUnit() == 33984) {
            int j = Shaders.atlasSizeX;
            int k = Shaders.atlasSizeY;
            if (parentTexture instanceof TextureAtlas) {
                Shaders.atlasSizeX = ((TextureAtlas)parentTexture).atlasWidth;
                Shaders.atlasSizeY = ((TextureAtlas)parentTexture).atlasHeight;
            } else {
                Shaders.atlasSizeX = 0;
                Shaders.atlasSizeY = 0;
            }
        }
    }

    public static void initDynamicTextureNS(DynamicTexture tex) {
        MultiTexID multitexid = tex.getMultiTexID();
        NativeImage nativeimage = tex.getPixels();
        int i = nativeimage.getWidth();
        int j = nativeimage.getHeight();
        initTextureNS(multitexid, i, j);
    }

    public static void initTextureNS(MultiTexID multiTex, int width, int height) {
        NativeImage nativeimage = makeImageColor(width, height, -8421377);
        TextureUtils.prepareImage(multiTex.norm, width, height);
        nativeimage.uploadTextureSub(0, 0, 0, 0, 0, width, height, false, false, false, true);
        NativeImage nativeimage1 = makeImageColor(width, height, 0);
        TextureUtils.prepareImage(multiTex.spec, width, height);
        nativeimage1.uploadTextureSub(0, 0, 0, 0, 0, width, height, false, false, false, true);
        GlStateManager._bindTexture(multiTex.base);
    }

    public static void updateDynTexSubImage1(int[] src, int width, int height, int posX, int posY, int page) {
        int i = width * height;
        IntBuffer intbuffer = getIntBuffer(i);
        intbuffer.clear();
        int j = page * i;
        if (src.length >= j + i) {
            intbuffer.put(src, j, i).position(0).limit(i);
            TextureUtils.resetDataUnpacking();
            GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intbuffer);
            intbuffer.clear();
        }
    }

    public static AbstractTexture createDefaultTexture() {
        DynamicTexture dynamictexture = new DynamicTexture("Shaders.defaultTexture", 1, 1, true);
        dynamictexture.getPixels().setPixelABGR(0, 0, -1);
        dynamictexture.upload();
        return dynamictexture;
    }

    public static void allocateTextureMapNS(int mipmapLevels, int width, int height, TextureAtlas tex) {
        MultiTexID multitexid = getMultiTexID(tex.getGlTexture());
        if (Shaders.configNormalMap) {
            SMCLog.info("Allocate texture map normal: " + width + "x" + height + ", mipmaps: " + mipmapLevels);
            TextureUtils.prepareImage(multitexid.norm, mipmapLevels, width, height);
            NativeImage.setMinMagFilters(false, mipmapLevels > 0);
            NativeImage.setClamp(false);
        }

        if (Shaders.configSpecularMap) {
            SMCLog.info("Allocate texture map specular: " + width + "x" + height + ", mipmaps: " + mipmapLevels);
            TextureUtils.prepareImage(multitexid.spec, mipmapLevels, width, height);
            NativeImage.setMinMagFilters(false, mipmapLevels > 0);
            NativeImage.setClamp(false);
        }

        GlStateManager._bindTexture(multitexid.base);
    }

    private static NativeImage[] generateMipmaps(NativeImage image, int levels) {
        if (levels < 0) {
            levels = 0;
        }

        NativeImage[] anativeimage = new NativeImage[levels + 1];
        anativeimage[0] = image;
        if (levels > 0) {
            for (int i = 1; i <= levels; i++) {
                NativeImage nativeimage = anativeimage[i - 1];
                NativeImage nativeimage1 = new NativeImage(nativeimage.getWidth() >> 1, nativeimage.getHeight() >> 1, false);
                int j = nativeimage1.getWidth();
                int k = nativeimage1.getHeight();

                for (int l = 0; l < j; l++) {
                    for (int i1 = 0; i1 < k; i1++) {
                        nativeimage1.setPixelABGR(
                            l,
                            i1,
                            blend4Simple(
                                nativeimage.getPixelABGR(l * 2 + 0, i1 * 2 + 0),
                                nativeimage.getPixelABGR(l * 2 + 1, i1 * 2 + 0),
                                nativeimage.getPixelABGR(l * 2 + 0, i1 * 2 + 1),
                                nativeimage.getPixelABGR(l * 2 + 1, i1 * 2 + 1)
                            )
                        );
                    }
                }

                anativeimage[i] = nativeimage1;
            }
        }

        return anativeimage;
    }

    public static BufferedImage readImage(Identifier resLoc) {
        try {
            if (!Config.hasResource(resLoc)) {
                return null;
            }

            InputStream inputstream = Config.getResourceStream(resLoc);
            if (inputstream == null) {
                return null;
            }

            BufferedImage bufferedimage = ImageIO.read(inputstream);
            inputstream.close();
            return bufferedimage;
        } catch (IOException ioexception) {
            return null;
        }
    }

    public static int[][] genMipmapsSimple(int maxLevel, int width, int[][] data) {
        for (int i = 1; i <= maxLevel; i++) {
            if (data[i] == null) {
                int j = width >> i;
                int k = j * 2;
                int[] aint = data[i - 1];
                int[] aint1 = data[i] = new int[j * j];

                for (int i1 = 0; i1 < j; i1++) {
                    for (int l = 0; l < j; l++) {
                        int j1 = i1 * 2 * k + l * 2;
                        aint1[i1 * j + l] = blend4Simple(aint[j1], aint[j1 + 1], aint[j1 + k], aint[j1 + k + 1]);
                    }
                }
            }
        }

        return data;
    }

    public static void uploadTexSub1(int[][] src, int width, int height, int posX, int posY, int page) {
        TextureUtils.resetDataUnpacking();
        int i = width * height;
        IntBuffer intbuffer = getIntBuffer(i);
        int j = src.length;
        int k = 0;
        int l = width;
        int i1 = height;
        int j1 = posX;
        int k1 = posY;

        while (l > 0 && i1 > 0 && k < j) {
            int l1 = l * i1;
            int[] aint = src[k];
            intbuffer.clear();
            if (aint.length >= l1 * (page + 1)) {
                intbuffer.put(aint, l1 * page, l1).position(0).limit(l1);
                GL11.glTexSubImage2D(3553, k, j1, k1, l, i1, 32993, 33639, intbuffer);
            }

            l >>= 1;
            i1 >>= 1;
            j1 >>= 1;
            k1 >>= 1;
            k++;
        }

        intbuffer.clear();
    }

    public static int blend4Alpha(int c0, int c1, int c2, int c3) {
        int i = c0 >>> 24 & 0xFF;
        int j = c1 >>> 24 & 0xFF;
        int k = c2 >>> 24 & 0xFF;
        int l = c3 >>> 24 & 0xFF;
        int i1 = i + j + k + l;
        int j1 = (i1 + 2) / 4;
        int k1;
        if (i1 != 0) {
            k1 = i1;
        } else {
            k1 = 4;
            i = 1;
            j = 1;
            k = 1;
            l = 1;
        }

        int l1 = (k1 + 1) / 2;
        return j1 << 24
            | ((c0 >>> 16 & 0xFF) * i + (c1 >>> 16 & 0xFF) * j + (c2 >>> 16 & 0xFF) * k + (c3 >>> 16 & 0xFF) * l + l1) / k1 << 16
            | ((c0 >>> 8 & 0xFF) * i + (c1 >>> 8 & 0xFF) * j + (c2 >>> 8 & 0xFF) * k + (c3 >>> 8 & 0xFF) * l + l1) / k1 << 8
            | ((c0 >>> 0 & 0xFF) * i + (c1 >>> 0 & 0xFF) * j + (c2 >>> 0 & 0xFF) * k + (c3 >>> 0 & 0xFF) * l + l1) / k1 << 0;
    }

    public static int blend4Simple(int c0, int c1, int c2, int c3) {
        return ((c0 >>> 24 & 0xFF) + (c1 >>> 24 & 0xFF) + (c2 >>> 24 & 0xFF) + (c3 >>> 24 & 0xFF) + 2) / 4 << 24
            | ((c0 >>> 16 & 0xFF) + (c1 >>> 16 & 0xFF) + (c2 >>> 16 & 0xFF) + (c3 >>> 16 & 0xFF) + 2) / 4 << 16
            | ((c0 >>> 8 & 0xFF) + (c1 >>> 8 & 0xFF) + (c2 >>> 8 & 0xFF) + (c3 >>> 8 & 0xFF) + 2) / 4 << 8
            | ((c0 >>> 0 & 0xFF) + (c1 >>> 0 & 0xFF) + (c2 >>> 0 & 0xFF) + (c3 >>> 0 & 0xFF) + 2) / 4 << 0;
    }

    public static void genMipmapAlpha(int[] aint, int offset, int width, int height) {
        int i = Math.min(width, height);
        int o2 = offset;
        int w2 = width;
        int h2 = height;
        int o1 = 0;
        int w1 = 0;
        int h1 = 0;
        int j = 0;

        while (w2 > 1 && h2 > 1) {
            o1 = o2 + w2 * h2;
            w1 = w2 / 2;
            h1 = h2 / 2;

            for (int i2 = 0; i2 < h1; i2++) {
                int j2 = o1 + i2 * w1;
                int k2 = o2 + i2 * 2 * w2;

                for (int l2 = 0; l2 < w1; l2++) {
                    aint[j2 + l2] = blend4Alpha(aint[k2 + l2 * 2], aint[k2 + l2 * 2 + 1], aint[k2 + w2 + l2 * 2], aint[k2 + w2 + l2 * 2 + 1]);
                }
            }

            j++;
            w2 = w1;
            h2 = h1;
            o2 = o1;
        }

        while (j > 0) {
            w2 = width >> --j;
            h2 = height >> j;
            o2 = o1 - w2 * h2;
            int i3 = o2;

            for (int j3 = 0; j3 < h2; j3++) {
                for (int k3 = 0; k3 < w2; k3++) {
                    if (aint[i3] == 0) {
                        aint[i3] = aint[o1 + j3 / 2 * w1 + k3 / 2] & 16777215;
                    }

                    i3++;
                }
            }

            o1 = o2;
            w1 = w2;
        }
    }

    public static void genMipmapSimple(int[] aint, int offset, int width, int height) {
        int i = Math.min(width, height);
        int o2 = offset;
        int w2 = width;
        int h2 = height;
        int o1 = 0;
        int w1 = 0;
        int h1 = 0;
        int j = 0;

        while (w2 > 1 && h2 > 1) {
            o1 = o2 + w2 * h2;
            w1 = w2 / 2;
            h1 = h2 / 2;

            for (int i2 = 0; i2 < h1; i2++) {
                int j2 = o1 + i2 * w1;
                int k2 = o2 + i2 * 2 * w2;

                for (int l2 = 0; l2 < w1; l2++) {
                    aint[j2 + l2] = blend4Simple(aint[k2 + l2 * 2], aint[k2 + l2 * 2 + 1], aint[k2 + w2 + l2 * 2], aint[k2 + w2 + l2 * 2 + 1]);
                }
            }

            j++;
            w2 = w1;
            h2 = h1;
            o2 = o1;
        }

        while (j > 0) {
            w2 = width >> --j;
            h2 = height >> j;
            o2 = o1 - w2 * h2;
            int i3 = o2;

            for (int j3 = 0; j3 < h2; j3++) {
                for (int k3 = 0; k3 < w2; k3++) {
                    if (aint[i3] == 0) {
                        aint[i3] = aint[o1 + j3 / 2 * w1 + k3 / 2] & 16777215;
                    }

                    i3++;
                }
            }

            o1 = o2;
            w1 = w2;
        }
    }

    public static boolean isSemiTransparent(int[] aint, int width, int height) {
        int i = width * height;
        if (aint[0] >>> 24 == 255 && aint[i - 1] == 0) {
            return true;
        }

        for (int j = 0; j < i; j++) {
            int k = aint[j] >>> 24;
            if (k != 0 && k != 255) {
                return true;
            }
        }

        return false;
    }

    public static void updateSubTex1(int[] src, int width, int height, int posX, int posY) {
        int i = 0;
        int j = width;
        int k = height;
        int l = posX;

        for (int i1 = posY; j > 0 && k > 0; i1 /= 2) {
            GL11.glCopyTexSubImage2D(3553, i, l, i1, 0, 0, j, k);
            i++;
            j /= 2;
            k /= 2;
            l /= 2;
        }
    }

    public static void updateSubImage(MultiTexID multiTex, int[] src, int width, int height, int posX, int posY, boolean linear, boolean clamp) {
        int i = width * height;
        IntBuffer intbuffer = getIntBuffer(i);
        TextureUtils.resetDataUnpacking();
        intbuffer.clear();
        intbuffer.put(src, 0, i);
        intbuffer.position(0).limit(i);
        GlStateManager._bindTexture(multiTex.base);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 10242, 10497);
        GL11.glTexParameteri(3553, 10243, 10497);
        GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intbuffer);
        if (src.length == i * 3) {
            intbuffer.clear();
            intbuffer.put(src, i, i).position(0);
            intbuffer.position(0).limit(i);
        }

        GlStateManager._bindTexture(multiTex.norm);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 10242, 10497);
        GL11.glTexParameteri(3553, 10243, 10497);
        GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intbuffer);
        if (src.length == i * 3) {
            intbuffer.clear();
            intbuffer.put(src, i * 2, i);
            intbuffer.position(0).limit(i);
        }

        GlStateManager._bindTexture(multiTex.spec);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 10242, 10497);
        GL11.glTexParameteri(3553, 10243, 10497);
        GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intbuffer);
        GlStateManager._activeTexture(33984);
    }

    public static Identifier getNSMapLocation(Identifier location, String mapName) {
        if (location == null) {
            return null;
        }

        String s = location.getPath();
        String[] astring = s.split(".png");
        String s1 = astring[0];
        return new Identifier(location.getNamespace(), s1 + "_" + mapName + ".png");
    }

    private static NativeImage loadNSMapImage(ResourceManager manager, Identifier location, int width, int height, int defaultColor) {
        NativeImage nativeimage = loadNSMapFile(manager, location, width, height);
        if (nativeimage == null) {
            nativeimage = makeImageColor(width, height, defaultColor);
        }

        return nativeimage;
    }

    private static NativeImage makeImageColor(int width, int height, int defaultColor) {
        NativeImage nativeimage = new NativeImage(width, height, false);
        nativeimage.fillRect(0, 0, width, height, defaultColor);
        return nativeimage;
    }

    private static NativeImage loadNSMapFile(ResourceManager manager, Identifier location, int width, int height) {
        if (location == null) {
            return null;
        }

        try {
            Resource resource = manager.getResourceOrThrow(location);
            NativeImage nativeimage = NativeImage.read(resource.open());
            if (nativeimage == null) {
                return null;
            }

            if (nativeimage.getWidth() == width && nativeimage.getHeight() == height) {
                return nativeimage;
            }

            nativeimage.close();
            return null;
        } catch (IOException ioexception) {
            return null;
        }
    }

    public static void loadSimpleTextureNS(
        int textureID, NativeImage nativeImage, GpuSampler sampler, ResourceManager resourceManager, Identifier location, MultiTexID multiTex
    ) {
        boolean flag = SamplerUtils.isBlur(sampler);
        boolean flag1 = SamplerUtils.isClamp(sampler);
        loadSimpleTextureNS(textureID, nativeImage, flag, flag1, resourceManager, location, multiTex);
    }

    public static void loadSimpleTextureNS(
        int textureID, NativeImage nativeImage, boolean blur, boolean clamp, ResourceManager resourceManager, Identifier location, MultiTexID multiTex
    ) {
        int i = nativeImage.getWidth();
        int j = nativeImage.getHeight();
        Identifier identifier = getNSMapLocation(location, "n");
        NativeImage nativeimage = loadNSMapImage(resourceManager, identifier, i, j, -8421377);
        TextureUtils.prepareImage(multiTex.norm, 0, i, j);
        nativeimage.uploadTextureSub(0, 0, 0, 0, 0, i, j, blur, clamp, false, true);
        Identifier identifier1 = getNSMapLocation(location, "s");
        NativeImage nativeimage1 = loadNSMapImage(resourceManager, identifier1, i, j, 0);
        TextureUtils.prepareImage(multiTex.spec, 0, i, j);
        nativeimage1.uploadTextureSub(0, 0, 0, 0, 0, i, j, blur, clamp, false, true);
        GlStateManager._bindTexture(multiTex.base);
    }

    public static void mergeImage(int[] aint, int dstoff, int srcoff, int size) {
    }

    public static int blendColor(int color1, int color2, int factor1) {
        int i = 255 - factor1;
        return ((color1 >>> 24 & 0xFF) * factor1 + (color2 >>> 24 & 0xFF) * i) / 255 << 24
            | ((color1 >>> 16 & 0xFF) * factor1 + (color2 >>> 16 & 0xFF) * i) / 255 << 16
            | ((color1 >>> 8 & 0xFF) * factor1 + (color2 >>> 8 & 0xFF) * i) / 255 << 8
            | ((color1 >>> 0 & 0xFF) * factor1 + (color2 >>> 0 & 0xFF) * i) / 255 << 0;
    }

    public static void updateTextureMinMagFilter() {
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstracttexture = texturemanager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (abstracttexture != null) {
            MultiTexID multitexid = abstracttexture.getMultiTexID();
            GlStateManager._bindTexture(multitexid.base);
            GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilB]);
            GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilB]);
            GlStateManager._bindTexture(multitexid.norm);
            GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilN]);
            GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilN]);
            GlStateManager._bindTexture(multitexid.spec);
            GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilS]);
            GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilS]);
            GlStateManager._bindTexture(0);
        }
    }

    public static int[][] getFrameTexData(int[][] src, int width, int height, int frameIndex) {
        int i = src.length;
        int[][] aint = new int[i][];

        for (int j = 0; j < i; j++) {
            int[] aint1 = src[j];
            if (aint1 != null) {
                int k = (width >> j) * (height >> j);
                int[] aint2 = new int[k * 3];
                aint[j] = aint2;
                int l = aint1.length / 3;
                int i1 = k * frameIndex;
                int j1 = 0;
                System.arraycopy(aint1, i1, aint2, j1, k);
                i1 += l;
                j1 += k;
                System.arraycopy(aint1, i1, aint2, j1, k);
                i1 += l;
                j1 += k;
                System.arraycopy(aint1, i1, aint2, j1, k);
            }
        }

        return aint;
    }

    public static int[][] prepareAF(TextureAtlasSprite tas, int[][] src, int width, int height) {
        boolean flag = true;
        return src;
    }

    public static void fixTransparentColor(TextureAtlasSprite tas, int[] aint) {
    }
}
