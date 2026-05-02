package net.optifine.util;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

public class PlayerUtils {
    public static Identifier getTexturePath(ClientAsset.Texture tex) {
        return tex == null ? null : tex.texturePath();
    }
}
