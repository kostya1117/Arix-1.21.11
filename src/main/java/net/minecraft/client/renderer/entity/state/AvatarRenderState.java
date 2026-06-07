package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import ru.arixcompany.features.module.modules.render.customModels.ICustomPlayerModelState;


public class AvatarRenderState extends HumanoidRenderState implements ICustomPlayerModelState {
    public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
    public float capeFlap;
    public float capeLean;
    public float capeLean2;
    public int arrowCount;
    public int stingerCount;
    public boolean isSpectator;
    public boolean showHat = true;
    public boolean showJacket = true;
    public boolean showLeftPants = true;
    public boolean showRightPants = true;
    public boolean showLeftSleeve = true;
    public boolean showRightSleeve = true;
    public boolean showCape = true;
    public float fallFlyingTimeInTicks;
    public boolean shouldApplyFlyingYRot;
    public float flyingYRot;
    public  Component scoreText;
    public Parrot. Variant parrotOnLeftShoulder;
    public Parrot. Variant parrotOnRightShoulder;
    public int id;
    public boolean showExtraEars = false;
    public final ItemStackRenderState heldOnHead = new ItemStackRenderState();

    private boolean sunshine$hasCustomModel;
    private String sunshine$customModel = "";

    public float fallFlyingScale() {
        return Mth.clamp(this.fallFlyingTimeInTicks * this.fallFlyingTimeInTicks / 100.0F, 0.0F, 1.0F);
    }

    @Override
    public boolean hasCustomModel() {
        return this.sunshine$hasCustomModel;
    }

    @Override
    public String getCustomModel() {
        return this.sunshine$customModel;
    }

    @Override
    public void setCustomModel(boolean enabled, String model) {
        this.sunshine$hasCustomModel = enabled;
        this.sunshine$customModel = model == null ? "" : model;
    }
}
