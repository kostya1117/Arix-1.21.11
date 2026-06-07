package ru.arixcompany.features.module.modules.render.customModels;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import ru.arixcompany.features.module.modules.render.CustomModels;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CustomPlayerModelRenderer {

    private static final Map<String, Supplier<CustomModel>> MODEL_FACTORIES = new HashMap<>();
    private static final Map<String, CustomModel> MODEL_CACHE = new HashMap<>();

    static {
        MODEL_FACTORIES.put(CustomModels.RABBIT, CustomPlayerModelRenderer::createRabbit);
        MODEL_FACTORIES.put(CustomModels.FREDDY, CustomPlayerModelRenderer::createFreddy);
        MODEL_FACTORIES.put(CustomModels.TUNG, CustomPlayerModelRenderer::createTung);
    }

    public static boolean render(
            String modelName,
            PlayerModel vanillaModel,
            AvatarRenderState state,
            PoseStack matrices,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            int color,
            int outlineColor
    ) {
        CustomModel customModel = getModel(modelName, state.id);
        if (customModel == null) {
            return false;
        }

        customModel.copyAngles(vanillaModel, state);

        RenderType layer = RenderTypes.entityTranslucent(customModel.texture);

        final int finalLight = light;
        final int finalOverlay = overlay;
        final int finalColor = color;

        collector.submitCustomGeometry(
                matrices,
                layer,
                (pose, buf) -> {
                    PoseStack innerStack = new PoseStack();
                    innerStack.last().pose().set(pose.pose());
                    innerStack.last().normal().set(pose.normal());

                    customModel.transform(innerStack, state);
                    customModel.root.render(innerStack, buf, finalLight, finalOverlay, finalColor);
                }
        );

        return true;
    }

    private static CustomModel getModel(String modelName, int entityId) {
        Supplier<CustomModel> factory = MODEL_FACTORIES.get(modelName);
        if (factory == null) {
            return null;
        }

        if (MODEL_CACHE.size() > 128) {
            MODEL_CACHE.clear();
        }

        return MODEL_CACHE.computeIfAbsent(modelName + "#" + entityId, ignored -> factory.get());
    }

    private static CustomModel createRabbit() {
        ModelPart root = createRoot(64, 64, data -> {
            PartDefinition rabbit = data.addOrReplaceChild(
                    "rabbit",
                    box(28, 45, -5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F),
                    PartPose.offset(0.0F, 24.0F, 0.0F)
            );
            rabbit.addOrReplaceChild("right_leg", box(0, 32, -2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(-3.0F, -2.0F, -1.0F));
            rabbit.addOrReplaceChild("left_leg", box(0, 32, -2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(3.0F, -2.0F, -1.0F));
            rabbit.addOrReplaceChild("left_arm", box(24, 16, 0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), PartPose.offsetAndRotation(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F));
            rabbit.addOrReplaceChild("right_arm", box(24, 16, -2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), PartPose.offsetAndRotation(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F));

            PartDefinition head = rabbit.addOrReplaceChild(
                    "head",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F)
                            .texOffs(0, 45).addBox(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F)
                            .texOffs(56, 0).addBox(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
                            .texOffs(56, 0).addBox(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
                            .texOffs(46, 0).addBox(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F)
                            .texOffs(46, 0).addBox(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F),
                    PartPose.offset(0.0F, -14.0F, -1.0F)
            );
            head.addOrReplaceChild("nose", box(0, 7, -1.5F, -4.0F, -5.5F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);
        });

        ModelPart rabbit = root.getChild("rabbit");
        return new CustomModel(CustomModels.RABBIT, Identifier.fromNamespaceAndPath("arix", "models/rabbit.png"),
                root, rabbit,
                rabbit.getChild("head"),
                rabbit,
                rabbit.getChild("left_arm"),
                rabbit.getChild("right_arm"),
                rabbit.getChild("left_leg"),
                rabbit.getChild("right_leg")) {
            @Override
            void transform(PoseStack matrices, AvatarRenderState state) {
                matrices.scale(1.25F, 1.25F, 1.25F);
                matrices.translate(0.0F, -0.3F, 0.0F);
            }

            @Override
            void copyAngles(PlayerModel vanilla, AvatarRenderState state) {
                copyHeadAngles(vanilla);
                clearAngles(body);
                copyLimbAngles(vanilla);
                leftArm.zRot -= 0.0873F;
                rightArm.zRot += 0.0873F;
            }
        };
    }

    private static CustomModel createFreddy() {
        ModelPart root = createRoot(100, 80, data -> {
            PartDefinition body = data.addOrReplaceChild("body", box(0, 0, -1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F), PartPose.offset(0.0F, -9.0F, 0.0F));
            body.addOrReplaceChild("torso", box(8, 0, -6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F), PartPose.rotation((float) Math.PI / 180.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("crotch", box(56, 0, -5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F), PartPose.offset(0.0F, 9.5F, 0.0F));

            PartDefinition head = body.addOrReplaceChild("head", box(39, 22, -5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F), PartPose.offset(0.0F, -13.0F, -0.5F));
            head.addOrReplaceChild("jaw", box(49, 65, -5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));
            head.addOrReplaceChild("nose", box(17, 67, -4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F), PartPose.offset(0.0F, -2.0F, -4.5F));
            PartDefinition rightEar = head.addOrReplaceChild("right_ear", box(8, 0, -1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, -1.0471976F));
            rightEar.addOrReplaceChild("right_ear_pad", box(85, 0, -2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
            PartDefinition leftEar = head.addOrReplaceChild("left_ear", box(40, 0, -1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, 1.0471976F));
            leftEar.addOrReplaceChild("left_ear_pad", box(40, 39, -2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
            PartDefinition hat = head.addOrReplaceChild("hat", box(70, 24, -3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -8.4F, 0.0F, (float)(-Math.PI) / 180.0F, 0.0F, 0.0F));
            hat.addOrReplaceChild("hat_top", box(78, 61, -2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 0.1F, 0.0F));

            PartDefinition rightArm = body.addOrReplaceChild("right_arm", box(48, 0, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offsetAndRotation(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.2617994F));
            rightArm.addOrReplaceChild("right_arm_pad", box(70, 10, -2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", box(90, 20, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));
            rightForearm.addOrReplaceChild("right_hand", box(20, 26, -2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F));

            PartDefinition leftArm = body.addOrReplaceChild("left_arm", box(62, 10, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offsetAndRotation(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2617994F));
            leftArm.addOrReplaceChild("left_arm_pad", box(38, 54, -2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", box(90, 48, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));
            leftForearm.addOrReplaceChild("left_hand", box(58, 56, -1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F));

            PartDefinition rightLeg = body.addOrReplaceChild("right_leg", box(90, 8, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offset(-3.3F, 12.5F, 0.0F));
            rightLeg.addOrReplaceChild("right_leg_pad", box(73, 33, -3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            PartDefinition rightLowerLeg = rightLeg.addOrReplaceChild("right_lower_leg", box(20, 35, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) Math.PI / 90.0F, 0.0F, 0.0F));
            rightLowerLeg.addOrReplaceChild("right_lower_leg_pad", box(0, 39, -2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            rightLowerLeg.addOrReplaceChild("right_foot", box(22, 39, -2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, (float)(-Math.PI) / 90.0F, 0.0F, 0.0F));

            PartDefinition leftLeg = body.addOrReplaceChild("left_leg", box(54, 10, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), PartPose.offset(3.3F, 12.5F, 0.0F));
            leftLeg.addOrReplaceChild("left_leg_pad", box(48, 39, -3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            PartDefinition leftLowerLeg = leftLeg.addOrReplaceChild("left_lower_leg", box(72, 48, -1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) Math.PI / 90.0F, 0.0F, 0.0F));
            leftLowerLeg.addOrReplaceChild("left_lower_leg_pad", box(16, 50, -2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
            leftLowerLeg.addOrReplaceChild("left_foot", box(72, 50, -2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, (float)(-Math.PI) / 90.0F, 0.0F, 0.0F));
        });

        ModelPart body = root.getChild("body");
        return new CustomModel(CustomModels.FREDDY, Identifier.fromNamespaceAndPath("arix", "models/freddy.png"),
                root, body, body.getChild("head"), body,
                body.getChild("left_arm"), body.getChild("right_arm"),
                body.getChild("left_leg"), body.getChild("right_leg")) {
            @Override
            void transform(PoseStack matrices, AvatarRenderState state) {
                matrices.scale(0.75F, 0.65F, 0.75F);
                matrices.translate(0.0F, 0.85F, 0.0F);
            }

            @Override
            void copyAngles(PlayerModel vanilla, AvatarRenderState state) {
                copyHeadAngles(vanilla);
                clearAngles(body);
                copyLimbAngles(vanilla);
            }
        };
    }

    private static CustomModel createTung() {
        ModelPart root = createRoot(64, 64, data -> {
            PartDefinition tung = data.addOrReplaceChild("tung", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
            PartDefinition chest = tung.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.offset(0.0F, -11.0F, 1.0F));
            chest.addOrReplaceChild("body", box(0, 18, -3.0F, -5.5F, -2.5F, 6.0F, 11.0F, 5.0F, 0.3F), PartPose.offset(0.0F, -5.5F, 0.0F));

            PartDefinition head = chest.addOrReplaceChild("head",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-3.5F, -11.17242F, -2.89262F, 7.0F, 12.0F, 6.0F, new CubeDeformation(0.15F))
                            .texOffs(35, 4).addBox(-3.45F, -7.67242F, -2.99262F, 2.0F, 2.0F, 0.9F, new CubeDeformation(0.5F))
                            .texOffs(35, 4).mirror().addBox(1.45F, -7.67242F, -2.99262F, 2.0F, 2.0F, 0.9F, new CubeDeformation(0.5F)).mirror(false)
                            .texOffs(36, 29).addBox(-4.1F, -8.42242F, -3.69262F, 3.25F, 0.5F, 2.25F)
                            .texOffs(36, 29).addBox(0.85F, -8.42242F, -3.69262F, 3.25F, 0.5F, 2.25F),
                    PartPose.offset(0.0F, -12.12758F, -0.10738F)
            );
            head.addOrReplaceChild("nose", box(30, 24, -1.0F, -2.5F, -1.0F, 1.5F, 5.0F, 2.0F, 0.15F), PartPose.offsetAndRotation(0.25F, -5.67242F, -2.64262F, 0.34906585F, 0.0F, 0.0F));
            head.addOrReplaceChild("brow_left", box(30, 31, -1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(-2.25F, -9.27242F, -2.64262F));
            head.addOrReplaceChild("brow_right", box(30, 31, -1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(2.25F, -9.27242F, -2.64262F));
            head.addOrReplaceChild("eye_left", box(6, 34, -0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, 0.4F), PartPose.offset(-2.35F, -5.97242F, -2.69262F));
            head.addOrReplaceChild("eye_right", box(6, 34, -0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, 0.4F), PartPose.offset(2.35F, -5.97242F, -2.69262F));

            PartDefinition leftArm = chest.addOrReplaceChild("left_arm", box(26, 0, 0.25F, 5.15F, -1.0F, 2.0F, 5.0F, 2.0F, 0.1F), PartPose.offset(3.75F, -10.35F, 0.0F));
            leftArm.addOrReplaceChild("left_arm_top", box(22, 24, -1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.75F, 2.35F, 0.0F, 0.0F, 0.0F, 0.17453292F));
            PartDefinition rightArm = chest.addOrReplaceChild("right_arm", box(26, 0, -2.25F, 5.15F, -1.0F, 2.0F, 5.0F, 2.0F, 0.1F), PartPose.offset(-3.75F, -10.35F, 0.0F));
            rightArm.addOrReplaceChild("right_arm_top", box(22, 24, -1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-0.75F, 2.35F, 0.0F, 0.0F, 0.0F, -0.17453292F));
            rightArm.addOrReplaceChild("bat", box(34, 6, 0.0F, -1.0F, -1.0F, 12.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-1.25F, 9.1F, 0.1F, -1.5707964F, 1.134464F, -1.5707964F));

            tung.addOrReplaceChild("left_leg",
                    CubeListBuilder.create()
                            .texOffs(26, 7).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.15F))
                            .texOffs(22, 32).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
                            .texOffs(26, 0).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                            .texOffs(22, 18).addBox(-1.5F, 9.0F, -3.0F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(2.0F, -11.0F, 1.0F)
            );
            tung.addOrReplaceChild("right_leg",
                    CubeListBuilder.create()
                            .texOffs(26, 7).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.15F))
                            .texOffs(22, 32).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
                            .texOffs(26, 0).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                            .texOffs(22, 18).addBox(-1.5F, 9.0F, -3.0F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(-2.0F, -11.0F, 1.0F)
            );
        });

        ModelPart tung = root.getChild("tung");
        ModelPart chest = tung.getChild("chest");
        return new CustomModel(CustomModels.TUNG, Identifier.fromNamespaceAndPath("arix", "models/tung.png"),
                root, tung, chest.getChild("head"), chest,
                chest.getChild("left_arm"), chest.getChild("right_arm"),
                tung.getChild("left_leg"), tung.getChild("right_leg"));
    }

    private static ModelPart createRoot(int textureWidth, int textureHeight, Consumer<PartDefinition> builder) {
        MeshDefinition meshDef = new MeshDefinition();
        builder.accept(meshDef.getRoot());
        return LayerDefinition.create(meshDef, textureWidth, textureHeight).bakeRoot();
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d, float dilation) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d, new CubeDeformation(dilation));
    }

    static class CustomModel {
        protected final String name;
        protected final Identifier texture;
        protected final ModelPart root;
        protected final ModelPart bodyRoot;
        protected final ModelPart head;
        protected final ModelPart body;
        protected final ModelPart leftArm;
        protected final ModelPart rightArm;
        protected final ModelPart leftLeg;
        protected final ModelPart rightLeg;

        protected CustomModel(
                String name, Identifier texture,
                ModelPart root, ModelPart bodyRoot,
                ModelPart head, ModelPart body,
                ModelPart leftArm, ModelPart rightArm,
                ModelPart leftLeg, ModelPart rightLeg
        ) {
            this.name = name;
            this.texture = texture;
            this.root = root;
            this.bodyRoot = bodyRoot;
            this.head = head;
            this.body = body;
            this.leftArm = leftArm;
            this.rightArm = rightArm;
            this.leftLeg = leftLeg;
            this.rightLeg = rightLeg;
        }

        void transform(PoseStack matrices, AvatarRenderState state) {
        }

        void copyAngles(PlayerModel vanilla, AvatarRenderState state) {
            copyHeadAngles(vanilla);
            copyBodyAngles(vanilla);
            copyLimbAngles(vanilla);
        }

        protected void copyHeadAngles(PlayerModel vanilla) {
            head.xRot = vanilla.head.xRot;
            head.yRot = vanilla.head.yRot;
            head.zRot = vanilla.head.zRot;
        }

        protected void copyBodyAngles(PlayerModel vanilla) {
            body.xRot = vanilla.body.xRot;
            body.yRot = vanilla.body.yRot;
            body.zRot = vanilla.body.zRot;
        }

        protected void copyLimbAngles(PlayerModel vanilla) {
            leftArm.xRot = vanilla.leftArm.xRot;
            leftArm.yRot = vanilla.leftArm.yRot;
            leftArm.zRot = vanilla.leftArm.zRot;

            rightArm.xRot = vanilla.rightArm.xRot;
            rightArm.yRot = vanilla.rightArm.yRot;
            rightArm.zRot = vanilla.rightArm.zRot;

            leftLeg.xRot = vanilla.leftLeg.xRot;
            leftLeg.yRot = vanilla.leftLeg.yRot;
            leftLeg.zRot = vanilla.leftLeg.zRot;

            rightLeg.xRot = vanilla.rightLeg.xRot;
            rightLeg.yRot = vanilla.rightLeg.yRot;
            rightLeg.zRot = vanilla.rightLeg.zRot;
        }

        protected void clearAngles(ModelPart part) {
            part.xRot = 0.0F;
            part.yRot = 0.0F;
            part.zRot = 0.0F;
        }
    }

    private CustomPlayerModelRenderer() {
    }
}