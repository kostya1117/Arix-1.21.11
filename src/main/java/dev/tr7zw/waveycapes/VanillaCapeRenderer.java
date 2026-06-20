package dev.tr7zw.waveycapes;

import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.tr7zw.transition.mc.entitywrapper.PlayerWrapper;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >= 1.21.11 {

import net.minecraft.client.renderer.rendertype.*;
//? } else {
/*
import net.minecraft.client.renderer.*;
*///? }
import net.minecraft.resources.*;
import ru.arixcompany.utils.Textures;

public class VanillaCapeRenderer implements CapeRenderer {

    @Override
    public VertexConsumer getVertexConsumer(MultiBufferSource multiBufferSource, PlayerWrapper capeRenderInfo) {
        Identifier cape = capeRenderInfo.getCapeTexture();
     //   Identifier capeTexture = Textures.cape;
        if (cape != null) {
            //? if >= 1.21.11 {

            return multiBufferSource.getBuffer(RenderTypes.entityCutout(cape));
            //? } else {
            /*
            return multiBufferSource.getBuffer(RenderType.entityTranslucent(cape));
            *///? }
        }
        return null;
    }

    @Override
    public boolean vanillaUvValues() {
        return true;
    }

}
