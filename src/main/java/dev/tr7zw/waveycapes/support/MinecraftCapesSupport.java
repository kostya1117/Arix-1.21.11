package dev.tr7zw.waveycapes.support;

import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.tr7zw.transition.mc.entitywrapper.PlayerWrapper;
import dev.tr7zw.waveycapes.CapeRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >= 1.21.11 {

import net.minecraft.client.renderer.rendertype.*;
//? } else {
/*
import net.minecraft.client.renderer.*;
*///? }
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.Identifier;

public class MinecraftCapesSupport implements ModSupport { // Можешь переименовать в OptifineSupport

    private final OptifineRenderer render = new OptifineRenderer();

    // Безопасная проверка, является ли текстура плащом OptiFine
    private boolean isOptifineCape(Identifier tex) {
        if (tex == null) return false;
        try {
            // Метод isOptiFine() добавляется самим OptiFine в класс Identifier
            // Если текстура начинается с "optifine", он вернет true
            return tex.isOptiFine();
        } catch (NoSuchMethodError e) {
            // Если OptiFine не установлен в сборке, этого метода не существует
            return false;
        }
    }

    @Override
    public boolean shouldBeUsed(PlayerWrapper capeRenderInfo) {
        // WaveyCapes сам достает текстуру из PlayerSkin.cape().texturePath()
        // Мы просто проверяем, не подменил ли её OptiFine
        return isOptifineCape(capeRenderInfo.getCapeTexture());
    }

    @Override
    public CapeRenderer getRenderer() {
        return render;
    }

    private class OptifineRenderer implements CapeRenderer {

        @Override
        public VertexConsumer getVertexConsumer(MultiBufferSource multiBufferSource, PlayerWrapper capeRenderInfo) {
            Identifier capeTexture = capeRenderInfo.getCapeTexture();

            // Рендерим плащ OptiFine с физикой волн
            //? if >= 1.21.11 {
            return ItemRenderer.getFoilBuffer(multiBufferSource,
                    RenderTypes.entityTranslucent(capeTexture), false, false);
            //? } else if >= 1.21.9 {
            /*
            return ItemRenderer.getFoilBuffer(multiBufferSource,
                    RenderType.entityTranslucent(capeTexture), false, false);
            *///? } else if >= 1.21.0 {
            /*
            return ItemRenderer.getArmorFoilBuffer(multiBufferSource,
                    RenderType.entityTranslucent(capeTexture), false);
            *///? } else {
            /*
            return ItemRenderer.getArmorFoilBuffer(multiBufferSource,
                    RenderType.entityTranslucent(capeTexture), false, false);
            *///? }
        }
    }

    @Override
    public boolean blockFeatureRenderer(Object feature) {
        return false;
    }
}