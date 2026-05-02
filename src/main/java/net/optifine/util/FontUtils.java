package net.optifine.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class FontUtils {
    public static Properties readFontProperties(Identifier locationFontTexture) {
        String s = locationFontTexture.getPath();
        Properties properties = new PropertiesOrdered();
        String s1 = ".png";
        if (!s.endsWith(s1)) {
            return properties;
        }

        String s2 = s.substring(0, s.length() - s1.length()) + ".properties";

        try {
            Identifier identifier = new Identifier(locationFontTexture.getNamespace(), s2);
            InputStream inputstream = Config.getResourceStream(Config.getResourceManager(), identifier);
            if (inputstream == null) {
                return properties;
            }

            Config.log("Loading " + s2);
            properties.load(inputstream);
            inputstream.close();
        } catch (FileNotFoundException filenotfoundexception) {
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        return properties;
    }

    public static Int2ObjectMap<Float> readCustomCharWidths(Properties props) {
        Int2ObjectMap<Float> map = new Int2ObjectOpenHashMap();
        Set keySet = props.keySet();

        for (Object o : keySet) {
            String key = (String) o;
            String prefix = "width.";
            if (key.startsWith(prefix)) {
                String numStr = key.substring(prefix.length());
                int num = Config.parseInt(numStr, -1);
                if (num >= 0) {
                    String value = props.getProperty(key);
                    float width = Config.parseFloat(value, -1.0F);
                    if (width >= 0.0F) {
                        char ch = (char) num;
                        map.put(ch, new Float(width));
                    }
                }
            }
        }

        return map;
    }

    public static float readFloat(Properties props, String key, float defOffset) {
        String s = props.getProperty(key);
        if (s == null) {
            return defOffset;
        } else {
            float f = Config.parseFloat(s, Float.MIN_VALUE);
            if (f == Float.MIN_VALUE) {
                Config.warn("Invalid value for " + key + ": " + s);
                return defOffset;
            } else {
                return f;
            }
        }
    }

    public static boolean readBoolean(Properties props, String key, boolean defVal) {
        String s = props.getProperty(key);
        if (s == null) {
            return defVal;
        } else {
            String s1 = s.toLowerCase().trim();
            if (s1.equals("true") || s1.equals("on")) {
                return true;
            } else if (!s1.equals("false") && !s1.equals("off")) {
                Config.warn("Invalid value for " + key + ": " + s);
                return defVal;
            } else {
                return false;
            }
        }
    }

    public static Identifier getHdFontLocation(Identifier fontLoc) {
        if (!Config.isCustomFonts()) {
            return fontLoc;
        }

        if (fontLoc == null) {
            return fontLoc;
        }

        if (!Config.isMinecraftThread()) {
            return fontLoc;
        }

        String s = fontLoc.getPath();
        String s1 = "textures/";
        String s2 = "optifine/";
        if (!s.startsWith(s1)) {
            return fontLoc;
        }

        s = s.substring(s1.length());
        s = s2 + s;
        Identifier identifier = new Identifier(fontLoc.getNamespace(), s);
        return Config.hasResource(Config.getResourceManager(), identifier) ? identifier : fontLoc;
    }

    public static void reloadFonts() {
        PreparableReloadListener.PreparationBarrier preparablereloadlistener$preparationbarrier = new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T x) {
                return CompletableFuture.completedFuture(x);
            }
        };
        Executor executor = Util.backgroundExecutor();
        Minecraft minecraft = Minecraft.getInstance();
        FontManager fontmanager = (FontManager)Reflector.getFieldValue(minecraft, Reflector.Minecraft_fontResourceManager);
        if (fontmanager != null) {
            PreparableReloadListener.SharedState preparablereloadlistener$sharedstate = new PreparableReloadListener.SharedState(Config.getResourceManager());
            fontmanager.reload(preparablereloadlistener$sharedstate, executor, preparablereloadlistener$preparationbarrier, minecraft);
        }
    }
}
