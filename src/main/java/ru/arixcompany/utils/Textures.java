package ru.arixcompany.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;

@UtilityClass
public class Textures implements IMinecraft {
    public static final Identifier arrow = Identifier.arix("images/arrow.png");
    public static final Identifier circle = Identifier.arix("images/circle.png");
    public static final Identifier glow = Identifier.arix("images/glow.png");
    public static final Identifier hitbubble = Identifier.arix("images/hitbubble.png");
    public static final Identifier target = Identifier.arix("images/target.png");
    public static final Identifier waypoint = Identifier.arix("images/waypoint.png");

    //particles
    public static final Identifier dollar = Identifier.arix("images/particles/dollar.png");
    public static final Identifier firefly = Identifier.arix("images/particles/firefly.png");
    public static final Identifier heart = Identifier.arix("images/particles/heart.png");
    public static final Identifier snowflake = Identifier.arix("images/particles/snowflake.png");
    public static final Identifier star = Identifier.arix("images/particles/star.png");
    public static final Identifier cape = Identifier.arix("images/cape.png");
}
