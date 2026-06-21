package dev.tr7zw.transition.mc.entitywrapper;

import dev.tr7zw.transition.mc.PlayerUtil;
//? if >= 1.21.2 {
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier; // Используем Identifier, как в вашем коде ClientAsset
//? } else {
/*
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.resources.ResourceLocation;
*/
//? }
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.utils.IMinecraft;

public class PlayerWrapper extends LivingEntityWrapper implements IMinecraft {

    //? if >= 1.21.2 {
    private final AvatarRenderState renderState;

    public PlayerWrapper(AvatarRenderState renderState) {
        super(renderState);
        this.renderState = renderState;
    }

    @Override
    public AvatarRenderState getRenderState() {
        return renderState;
    }
    //? } else {
    /*
    private final AbstractClientPlayer player;

    public PlayerWrapper(AbstractClientPlayer player) {
           super(player);
           this.player = player;
    }
    */
    //? }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /**
     * Получаем текстуру плаща.
     */
    //? if >= 1.21.2 {
    public Identifier getCapeTexture() {
//        if (renderState.skin.cape() != null) {
//            // Согласно интерфейсу ClientAsset.Texture, используем texturePath()
//            return renderState.skin.cape().texturePath();
//        }
//        return null;
        if (mc.player == null || mc.level == null)
            return null;
        return mc.player.getLocationCape();
    }
    //? } else {
    /*
    public ResourceLocation getCapeTexture() {
        return player.getLocationCape();
    }
    */
    //? }

    public boolean isPlayerInvisible() {
        //? if >= 1.21.2 {
        return renderState.isInvisible;
        //? } else {
        /*
        return player.isInvisible();
        */
        //? }
    }

    public boolean isCapeVisible() {
        //$ is_cape_visible
        return renderState.showCape && !isPlayerInvisible();
    }

    public boolean hasElytraEquipped() {
        //? if >= 1.21.2 {
        return renderState.chestEquipment.is(Items.ELYTRA);
        //? } else {
        /*
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        */
        //? }
    }

    public boolean hasChestplateEquipped() {
        //? if >= 1.21.2 {
        return !hasElytraEquipped() && !renderState.chestEquipment.isEmpty();
        //? } else {
        /*
        return !hasElytraEquipped() && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        */
        //? }
    }
}