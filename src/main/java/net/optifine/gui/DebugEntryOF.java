package net.optifine.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.optifine.Config;
import net.optifine.DynamicLights;
import net.optifine.SmartAnimations;
import net.optifine.TextureAnimations;
import net.optifine.shaders.Shaders;

public class DebugEntryOF implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayerIn, Level worldIn, LevelChunk chunkIn, LevelChunk serverChunkIn) {
        StringBuilder stringbuilder = new StringBuilder(32);
        stringbuilder.append("OptiFine_1.21.11_HD_U_J9");
        if (Config.isDynamicLights()) {
            stringbuilder.append(", DL: ");
            stringbuilder.append(String.valueOf(DynamicLights.getCount()));
        }

        stringbuilder.append(", A: ");
        TextureAtlas textureatlas = Config.getTextureMapBlocks();
        if (SmartAnimations.isActive()) {
            stringbuilder.append(textureatlas.getCountAnimationsActive() + TextureAnimations.getCountAnimationsActive());
            stringbuilder.append("/");
        }

        stringbuilder.append(textureatlas.getCountAnimations() + TextureAnimations.getCountAnimations());
        displayerIn.addLine(stringbuilder.toString());
        stringbuilder.setLength(0);
        String s = Shaders.getShaderPackName();
        if (s != null) {
            stringbuilder.append("SH: ");
            stringbuilder.append(s);
            if (Config.isShadersShadows()) {
                LevelRenderer levelrenderer = Minecraft.getInstance().levelRenderer;
                int i = levelrenderer.getRenderedChunksShadow();
                int j = levelrenderer.getCountEntitiesRenderedShadow();
                int k = levelrenderer.getCountTileEntitiesRenderedShadow();
                stringbuilder.append(", SP C: ");
                stringbuilder.append(i);
                stringbuilder.append(", E: ");
                stringbuilder.append(j);
                stringbuilder.append("+");
                stringbuilder.append(k);
            }

            displayerIn.addLine(stringbuilder.toString());
        }
    }

    @Override
    public boolean isAllowed(boolean reducedDebugIn) {
        return true;
    }
}
