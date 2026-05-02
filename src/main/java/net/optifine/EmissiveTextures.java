package net.optifine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.optifine.render.RenderUtils;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.TextureUtils;

public class EmissiveTextures {
    private static String suffixEmissive = null;
    private static String suffixEmissivePng = null;
    private static boolean active = false;
    private static boolean render = false;
    private static boolean hasEmissive = false;
    private static boolean renderEmissive = false;
    private static final String SUFFIX_PNG = ".png";
    private static final Identifier LOCATION_TEXTURE_EMPTY = TextureUtils.LOCATION_TEXTURE_EMPTY;
    private static final Identifier LOCATION_SPRITE_EMPTY = TextureUtils.LOCATION_SPRITE_EMPTY;
    private static TextureManager textureManager;
    private static int countRecursive = 0;

    public static boolean isActive() {
        return active;
    }

    public static String getSuffixEmissive() {
        return suffixEmissive;
    }

    public static void beginRender() {
        if (render) {
            countRecursive++;
        } else {
            render = true;
            hasEmissive = false;
        }
    }

    public static Identifier getEmissiveTexture(Identifier locationIn) {
        if (!render) {
            return locationIn;
        }

        AbstractTexture abstracttexture = textureManager.getTexture(locationIn);
        if (abstracttexture instanceof TextureAtlas) {
            return locationIn;
        }

        Identifier identifier = null;
        if (abstracttexture instanceof SimpleTexture) {
            identifier = ((SimpleTexture)abstracttexture).locationEmissive;
        }

        if (!renderEmissive) {
            if (identifier != null) {
                hasEmissive = true;
            }

            return locationIn;
        } else {
            if (identifier == null) {
                identifier = LOCATION_TEXTURE_EMPTY;
            }

            return identifier;
        }
    }

    public static TextureAtlasSprite getEmissiveSprite(TextureAtlasSprite sprite) {
        if (!render) {
            return sprite;
        }

        TextureAtlasSprite textureatlassprite = sprite.spriteEmissive;
        if (!renderEmissive) {
            if (textureatlassprite != null) {
                hasEmissive = true;
            }

            return sprite;
        } else {
            if (textureatlassprite == null) {
                textureatlassprite = sprite.getTextureAtlas().getSprite(LOCATION_SPRITE_EMPTY);
            }

            return textureatlassprite;
        }
    }

    public static BakedQuad getEmissiveQuad(BakedQuad quad) {
        if (!render) {
            return quad;
        }

        BakedQuad bakedquad = quad.getQuadEmissive();
        if (!renderEmissive) {
            if (bakedquad != null) {
                hasEmissive = true;
            }

            return quad;
        } else {
            return bakedquad;
        }
    }

    public static boolean hasEmissive() {
        return countRecursive > 0 ? false : hasEmissive;
    }

    public static void beginRenderEmissive() {
        renderEmissive = true;
    }

    public static boolean isRenderEmissive() {
        return renderEmissive;
    }

    public static void endRenderEmissive() {
        RenderUtils.flushRenderBuffers();
        renderEmissive = false;
    }

    public static void endRender() {
        if (countRecursive > 0) {
            countRecursive--;
        } else {
            render = false;
            hasEmissive = false;
        }
    }

    public static void update() {
        textureManager = Minecraft.getInstance().getTextureManager();
        active = false;
        suffixEmissive = null;
        suffixEmissivePng = null;
        if (Config.isEmissiveTextures()) {
            try {
                String s = "optifine/emissive.properties";
                Identifier identifier = new Identifier(s);
                InputStream inputstream = Config.getResourceStream(identifier);
                if (inputstream == null) {
                    return;
                }

                dbg("Loading " + s);
                Properties properties = new PropertiesOrdered();
                properties.load(inputstream);
                inputstream.close();
                suffixEmissive = properties.getProperty("suffix.emissive");
                if (suffixEmissive != null) {
                    suffixEmissivePng = suffixEmissive + ".png";
                }

                active = suffixEmissive != null;
            } catch (FileNotFoundException filenotfoundexception) {
                return;
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
            }
        }
    }

    public static void updateIcons(TextureAtlas textureMap, Set<Identifier> locations) {
        if (active) {
            for (Identifier identifier : locations) {
                checkEmissive(textureMap, identifier);
            }
        }
    }

    private static void checkEmissive(TextureAtlas textureMap, Identifier locSprite) {
        String s = getSuffixEmissive();
        if (s != null) {
            if (!locSprite.getPath().endsWith(s)) {
                Identifier identifier = new Identifier(locSprite.getNamespace(), locSprite.getPath() + s);
                Identifier identifier1 = textureMap.getSpritePath(identifier);
                if (Config.hasResource(identifier1)) {
                    TextureAtlasSprite textureatlassprite = textureMap.registerSprite(locSprite);
                    TextureAtlasSprite textureatlassprite1 = textureMap.registerSprite(identifier);
                    textureatlassprite1.isSpriteEmissive = true;
                    textureatlassprite.spriteEmissive = textureatlassprite1;
                    textureMap.registerSprite(LOCATION_SPRITE_EMPTY);
                }
            }
        }
    }

    public static void refreshIcons(TextureAtlas textureMap) {
        for (TextureAtlasSprite textureatlassprite : textureMap.getRegisteredSprites()) {
            refreshIcon(textureatlassprite, textureMap);
        }
    }

    private static void refreshIcon(TextureAtlasSprite sprite, TextureAtlas textureMap) {
        if (sprite.spriteEmissive != null) {
            TextureAtlasSprite textureatlassprite = textureMap.getUploadedSprite(sprite.getName());
            if (textureatlassprite != null) {
                TextureAtlasSprite textureatlassprite1 = textureMap.getUploadedSprite(sprite.spriteEmissive.getName());
                if (textureatlassprite1 != null) {
                    textureatlassprite1.isSpriteEmissive = true;
                    textureatlassprite.spriteEmissive = textureatlassprite1;
                }
            }
        }
    }

    private static void dbg(String str) {
        Config.dbg("EmissiveTextures: " + str);
    }

    private static void warn(String str) {
        Config.warn("EmissiveTextures: " + str);
    }

    public static boolean isEmissive(Identifier loc) {
        return suffixEmissivePng == null ? false : loc.getPath().endsWith(suffixEmissivePng);
    }

    public static void loadTexture(Identifier loc, ReloadableTexture tex) {
        if (loc != null && tex != null) {
            tex.isEmissive = false;
            tex.locationEmissive = null;
            if (suffixEmissivePng != null) {
                String s = loc.getPath();
                if (s.endsWith(".png")) {
                    if (s.endsWith(suffixEmissivePng)) {
                        tex.isEmissive = true;
                    } else {
                        String s1 = s.substring(0, s.length() - ".png".length()) + suffixEmissivePng;
                        Identifier identifier = new Identifier(loc.getNamespace(), s1);
                        if (Config.hasResource(identifier)) {
                            tex.locationEmissive = identifier;
                        }
                    }
                }
            }
        }
    }
}
