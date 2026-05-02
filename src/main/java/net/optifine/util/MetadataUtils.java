package net.optifine.util;

import com.google.gson.JsonObject;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;

public class MetadataUtils {
    public static TextureMetadataSection parseTextureMetadataSection(JsonObject json) {
        boolean flag = Json.getBoolean(json, "blur", false);
        boolean flag1 = Json.getBoolean(json, "clamp", false);
        String s = Json.getString(json, "mipmap_strategy", "auto");
        MipmapStrategy mipmapstrategy = parseMipmapStrategy(s);
        float f = Json.getFloat(json, "alpha_cutoff_bias", 0.0F);
        return new TextureMetadataSection(flag, flag1, mipmapstrategy, f);
    }

    private static MipmapStrategy parseMipmapStrategy(String str) {
        for (MipmapStrategy mipmapstrategy : MipmapStrategy.values()) {
            if (mipmapstrategy.getSerializedName().equals(str)) {
                return mipmapstrategy;
            }
        }

        return MipmapStrategy.AUTO;
    }
}
