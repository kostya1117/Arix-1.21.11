package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventArmRender;
import ru.arixcompany.features.event.render.EventHeldItemRenderer;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;

public class HandView extends Module {
    private final BooleanSetting skipSwapping =
            new BooleanSetting("Пропуск смены");

    private final ValueSetting scale =
            new ValueSetting("Масштаб").range(0.1f, 3f).setValue(1f).setStep(0.1f);

    private final ValueSetting rightPosX =
            new ValueSetting("Правая X").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting rightPosY =
            new ValueSetting("Правая Y").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting rightPosZ =
            new ValueSetting("Правая Z").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting leftPosX =
            new ValueSetting("Левая X").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting leftPosY =
            new ValueSetting("Левая Y").range(-3f, 3f).setStep(0.1f);

    private final ValueSetting leftPosZ =
            new ValueSetting("Левая Z").range(-3f, 3f).setStep(0.1f);

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

    public HandView() {
        super("HandView", Category.Render);
        setup(
                skipSwapping,
                scale,
                rightPosX, rightPosY, rightPosZ,
                leftPosX, leftPosY, leftPosZ,
                swingType,
                hitStrength,
                swingSpeed
        );
    }

    @EventHandler
    public void onHeldItemRender(EventHeldItemRenderer event) {
        if (!isState() || mc.player == null) return;

        HumanoidArm arm = resolveArm(event.getHand());

        applyBaseTransform(event.getMatrix(), arm);

        if (event.getHand() == InteractionHand.MAIN_HAND) {
            applySwingAnimation(
                    event.getMatrix(),
                    arm,
                    event.getSwingProgress()
            );
        }

        event.cancel();
    }

    @EventHandler
    public void onRenderArm(EventArmRender event) {
        if (!isState() || mc.player == null) return;

        applyBaseTransform(event.getMatrix(), resolveArm(event.getHand()));
    }

    private HumanoidArm resolveArm(InteractionHand hand) {
        if (mc.player == null)
            return hand == InteractionHand.MAIN_HAND
                    ? HumanoidArm.RIGHT
                    : HumanoidArm.LEFT;

        return hand == InteractionHand.MAIN_HAND
                ? mc.player.getMainArm()
                : mc.player.getMainArm().getOpposite();
    }

    private void applyBaseTransform(PoseStack poseStack, HumanoidArm arm) {
        float s = scale.getValue();
        poseStack.scale(s, s, s);

        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(
                    rightPosX.getValue(),
                    rightPosY.getValue(),
                    rightPosZ.getValue()
            );
        } else {
            poseStack.translate(
                    leftPosX.getValue(),
                    leftPosY.getValue(),
                    leftPosZ.getValue()
            );
        }
    }

    private void applySwingAnimation(PoseStack poseStack,
                                     HumanoidArm arm,
                                     float swingProgress) {

        swingProgress *= swingSpeed.getValue();

        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float strength = hitStrength.getValue();

        float sin1 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
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

                float rotationAngle = swingProgress * 360F * strength;
                poseStack.mulPose(Axis.XP.rotationDegrees(-rotationAngle));

                float tilt = Mth.sin(swingProgress * (float) Math.PI) * 15F * strength;
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * tilt));
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