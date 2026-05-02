package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public record RealmsDescriptionDto(@SerializedName("name")  String name, @SerializedName("description") String description)
    implements ReflectionBasedSerialization {
}
