package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventArmRender;
import ru.arixcompany.features.event.render.EventHeldItemRenderer;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;

public class HandView extends Module {
    private final BooleanSetting skipSwapping =
            new BooleanSetting("Пропуск смены");

    private final ValueSetting rightPosX =
            new ValueSetting("X").range(-3f, 3f).setStep(0.1f);
    private final ValueSetting rightPosY =
            new ValueSetting("Y").range(-3f, 3f).setStep(0.1f);
    private final ValueSetting rightPosZ =
            new ValueSetting("Z").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting leftPosX =
            new ValueSetting("X").range(-3f, 3f).setStep(0.1f);
    private final ValueSetting leftPosY =
            new ValueSetting("Y").range(-3f, 3f).setStep(0.1f);
    private final ValueSetting leftPosZ =
            new ValueSetting("Z").range(-3f, 3f).setStep(0.1f);

    private final SelectSetting swingType =
            new SelectSetting("Тип взмаха")
                    .value(
                            "360",
                            "Swipe",
                            "Down",
                            "Smooth",
                            "Power",
                            "Twist",
                            "Novoline",
                            "FDP",
                            "Sigma"
                    );

    private final ValueSetting hitStrength =
            new ValueSetting("Сила взмаха").range(0.5f, 3f).setValue(1f).setStep(0.1f);

    private final ValueSetting swingSpeed =
            new ValueSetting("Длительность взмаха").range(0.5f, 4f).setValue(1f).setStep(0.1f);

    private final ValueSetting eatShake =
            new ValueSetting("Шейк при еде").range(0f, 2f).setValue(1f).setStep(0.1f);

    public HandView() {
        super("HandView", Category.Render);
        setup(
                new GroupSetting("Правая рука", rightPosX, rightPosY, rightPosZ),
                new GroupSetting("Левая рука",  leftPosX,  leftPosY,  leftPosZ),
                new GroupSetting("Взмах",skipSwapping, swingType, hitStrength, swingSpeed,eatShake)
        );
    }

    @EventHandler
    public void onHeldItemRender(EventHeldItemRenderer event) {
        if (!isState() || mc.player == null) return;

        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == event.getHand()) {
            ItemStack useItem = mc.player.getUseItem();
            if (!useItem.isEmpty()) {
                var useAnimation = useItem.getUseAnimation();

                if (useAnimation == net.minecraft.world.item.ItemUseAnimation.EAT ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.DRINK) {
                    HumanoidArm arm = resolveArm(event.getHand());
                    applyBaseTransform(event.getMatrix(), arm);
                    applyVanillaEatAnimation(event.getMatrix(), arm, useItem);
                    event.cancel();
                    return;
                }

                if (useAnimation == net.minecraft.world.item.ItemUseAnimation.BOW ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.CROSSBOW ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.TRIDENT ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.SPEAR ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.BLOCK ||
                    useAnimation == net.minecraft.world.item.ItemUseAnimation.BRUSH) {
                    return;
                }
            }
        }

        HumanoidArm arm = resolveArm(event.getHand());
        applyBaseTransform(event.getMatrix(), arm);
        applySwingAnimation(event.getMatrix(), arm, event.getSwingProgress());
        event.cancel();
    }

    @EventHandler
    public void onRenderArm(EventArmRender event) {
        if (!isState() || mc.player == null) return;

        applyBaseTransform(event.getMatrix(), resolveArm(event.getHand()));
    }

    private HumanoidArm resolveArm(InteractionHand hand) {
        if (mc.player == null)
            return hand == InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;

        return hand == InteractionHand.MAIN_HAND
                ? mc.player.getMainArm()
                : mc.player.getMainArm().getOpposite();
    }

    private void applyBaseTransform(PoseStack poseStack, HumanoidArm arm) {
        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(rightPosX.getValue(), rightPosY.getValue(), rightPosZ.getValue());
        } else {
            poseStack.translate(leftPosX.getValue(), leftPosY.getValue(), leftPosZ.getValue());
        }
    }

    private void applyVanillaEatAnimation(PoseStack poseStack, HumanoidArm arm, ItemStack item) {
        if (mc.player == null) return;

        float f  = (float) mc.player.getUseItemRemainingTicks();
        float f1 = f / item.getUseDuration(mc.player);
        int   i  = arm == HumanoidArm.RIGHT ? 1 : -1;

        if (f1 < 0.8F) {
            float jiggle = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F) * eatShake.getValue();
            poseStack.translate(0.0F, jiggle, 0.0F);
        }

        float f3 = 1.0F - (float) Math.pow(f1, 27.0);
        poseStack.translate(f3 * 0.6F * i, f3 * -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * f3 * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(f3 * 10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * f3 * 30.0F));

        poseStack.translate(i * 0.56F, -0.52F, -0.72F);
    }

    private void applySwingAnimation(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        swingProgress *= swingSpeed.getValue();

        int   i        = arm == HumanoidArm.RIGHT ? 1 : -1;
        float strength = hitStrength.getValue();

        float sin1      = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2      = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);

        switch (swingType.getSelected()) {

            case "Twist" -> {
                poseStack.translate(i * 0.56F, -0.36F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(80 * i));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -90 * strength));
                poseStack.mulPose(Axis.ZP.rotationDegrees((sin1 - sin2) * 60 * i * strength));
                poseStack.mulPose(Axis.XP.rotationDegrees(-30));
                poseStack.translate(0, -0.1F, 0.05F);
            }

            case "Swipe" -> {
                poseStack.translate(0.56F * i, -0.32F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(60 * i));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-60 * i));
                poseStack.mulPose(Axis.XP.rotationDegrees((sin2 * sin1) * -120 * strength));
                poseStack.mulPose(Axis.XP.rotationDegrees(-60));
            }

            case "Down" -> {
                poseStack.translate(i * 0.56F, -0.32F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(76 * i));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -155 * strength));
                poseStack.mulPose(Axis.XP.rotationDegrees(-100));
            }

            case "Smooth" -> {
                poseStack.translate(i * 0.56F, -0.42F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45F + sin1 * -20F * strength)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * sin2 * -20F * strength));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -80F * strength));
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45F));
            }

            case "Power" -> {
                poseStack.translate(i * 0.56F, -0.32F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(sinSmooth * -60 * strength));
            }

            case "360" -> {
                poseStack.translate(i * 0.56F, -0.42F, -0.72F);
                float bounce = Mth.sin(swingProgress * (float) Math.PI) * 0.15F * strength;
                poseStack.translate(0, bounce, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-swingProgress * 360F * strength));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * Mth.sin(swingProgress * (float) Math.PI) * 15F * strength));
            }

            case "Novoline" -> {
                poseStack.translate(i * 0.56F, -0.42F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 60F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * -40F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-120F * sin2 * strength));
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -30F * sin1 * strength));
                poseStack.mulPose(Axis.XP.rotationDegrees(-40F));
            }

            case "FDP" -> {
                poseStack.translate(i * 0.56F, -0.52F, -0.72F);
                float move = sinSmooth * 0.4F * strength;
                poseStack.translate(i * move, 0, -move);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 75F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-90F * sin2 * strength));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 25F * sin1 * strength));
            }

            case "Sigma" -> {
                poseStack.translate(i * 0.56F, -0.36F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 90F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-140F * sin2 * strength));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 50F * sin1 * strength));
            }
        }
    }

    public boolean skipSwapping() {
        return isState() && skipSwapping.isValue();
    }
}
