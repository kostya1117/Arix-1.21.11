package net.optifine.shaders;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumSet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.optifine.Config;
import net.optifine.Lagometer;
import net.optifine.reflect.Reflector;
import net.optifine.render.GlBlendState;
import net.optifine.render.GlCullState;
import net.optifine.render.ICamera;
import net.optifine.render.RenderState;
import net.optifine.util.MathUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ShadersRender {
    private static final Identifier END_PORTAL_TEXTURE = new Identifier("textures/entity/end_portal.png");
    public static boolean frustumTerrainShadowChanged = false;
    public static boolean frustumEntitiesShadowChanged = false;
    public static int countEntitiesRenderedShadow;
    public static int countTileEntitiesRenderedShadow;
    private static AvatarRenderState playerRenderState = new AvatarRenderState();

    public static void setFrustrumPosition(ICamera frustum, double x, double y, double z) {
        frustum.setCameraPosition(x, y, z);
    }

    public static void beginTerrainSolid() {
        if (Shaders.isRenderingWorld) {
            Shaders.fogEnabled = true;
            Shaders.useProgram(Shaders.ProgramTerrain);
            Shaders.setRenderStage(RenderStage.TERRAIN_SOLID);
        }
    }

    public static void beginTerrainCutout() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramTerrain);
            Shaders.setRenderStage(RenderStage.TERRAIN_CUTOUT);
        }
    }

    public static void endTerrain() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramTexturedLit);
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void beginTranslucent() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramWater);
            Shaders.setRenderStage(RenderStage.TERRAIN_TRANSLUCENT);
        }
    }

    public static void endTranslucent() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramTexturedLit);
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void beginTripwire() {
        if (Shaders.isRenderingWorld) {
            Shaders.setRenderStage(RenderStage.TRIPWIRE);
        }
    }

    public static void endTripwire() {
        if (Shaders.isRenderingWorld) {
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void renderHandSolid(GameRenderer er, Matrix4f viewIn, Camera activeRenderInfo, float partialTicks, boolean sleepingIn) {
        if (!Shaders.isShadowPass) {
            boolean flag = Shaders.isItemToRenderMainTranslucent();
            boolean flag1 = Shaders.isItemToRenderOffTranslucent();
            if (!flag || !flag1) {
                Shaders.readCenterDepth();
                Shaders.beginHand(false);
                Shaders.setSkipRenderHands(flag, flag1);
                er.renderHand(partialTicks, sleepingIn, viewIn, false);
                Shaders.endHand();
                Shaders.setHandsRendered(!flag, !flag1);
                Shaders.setSkipRenderHands(false, false);
            }
        }
    }

    public static void renderHandTranslucent(GameRenderer er, Matrix4f viewIn, Camera activeRenderInfo, float partialTicks, boolean sleepingIn) {
        if (!Shaders.isShadowPass && !Shaders.isBothHandsRendered()) {
            Shaders.readCenterDepth();
            GlStateManager._enableBlend();
            Shaders.beginHand(true);
            Shaders.setSkipRenderHands(Shaders.isHandRenderedMain(), Shaders.isHandRenderedOff());
            er.renderHand(partialTicks, sleepingIn, viewIn, true);
            Shaders.endHand();
            Shaders.setHandsRendered(true, true);
            Shaders.setSkipRenderHands(false, false);
        }
    }

    public static void renderItemFP(
        ItemInHandRenderer itemRenderer,
        float partialTicks,
        PoseStack matrixStackIn,
        SubmitNodeCollector bufferIn,
        LocalPlayer playerEntityIn,
        int combinedLightIn,
        boolean renderTranslucent
    ) {
        RenderState.setEntityRenderState(getPlayerRenderState(playerEntityIn));
        if (renderTranslucent) {
            matrixStackIn.pushPose();
            DrawBuffers drawbuffers = GlState.getDrawBuffers();
            GlState.setDrawBuffers(Shaders.drawBuffersNone);
            itemRenderer.renderHandsWithItems(partialTicks, matrixStackIn, bufferIn, playerEntityIn, combinedLightIn);
            GlState.setDrawBuffers(drawbuffers);
            matrixStackIn.popPose();
        }

        itemRenderer.renderHandsWithItems(partialTicks, matrixStackIn, bufferIn, playerEntityIn, combinedLightIn);
        RenderState.setEntityRenderState(null);
    }

    private static AvatarRenderState getPlayerRenderState(LocalPlayer playerEntityIn) {
        playerRenderState.entity = playerEntityIn;
        return playerRenderState;
    }

    public static void beginBlockDamage() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramDamagedBlock);
            Shaders.setRenderStage(RenderStage.DESTROY);
            if (Shaders.ProgramDamagedBlock.getId() == Shaders.ProgramTerrain.getId()) {
                GlState.setDrawBuffers(Shaders.drawBuffersColorAtt[0]);
                GlStateManager._depthMask(false);
            }
        }
    }

    public static void endBlockDamage() {
        if (Shaders.isRenderingWorld) {
            GlStateManager._depthMask(true);
            Shaders.useProgram(Shaders.ProgramTexturedLit);
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void beginOutline() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramBasic);
            Shaders.setRenderStage(RenderStage.OUTLINE);
        }
    }

    public static void endOutline() {
        if (Shaders.isRenderingWorld) {
            Shaders.useProgram(Shaders.ProgramTexturedLit);
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void beginDebug() {
        if (Shaders.isRenderingWorld) {
            Shaders.setRenderStage(RenderStage.DEBUG);
        }
    }

    public static void endDebug() {
        if (Shaders.isRenderingWorld) {
            Shaders.setRenderStage(RenderStage.NONE);
        }
    }

    public static void renderShadowMap(GameRenderer entityRenderer, Camera activeRenderInfo, DeltaTracker deltaTracker, float partialTicks) {
        if (Shaders.hasShadowMap) {
            Minecraft minecraft = Minecraft.getInstance();
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.popPush("shadow pass");
            LevelRenderer levelrenderer = minecraft.levelRenderer;
            Shaders.isShadowPass = true;
            Shaders.checkGLError("pre shadow");
            GpuBufferSlice gpubufferslice = RenderSystem.getProjectionMatrixBuffer();
            ProjectionType projectiontype = RenderSystem.getProjectionType();
            RenderSystem.getModelViewStack().pushMatrix();
            profilerfiller.popPush("shadow clear");
            Shaders.sfb.bindFramebuffer();
            Shaders.checkGLError("shadow bind sfb");
            profilerfiller.popPush("shadow camera");
            updateActiveRenderInfo(activeRenderInfo, minecraft, partialTicks);
            minecraft.getBlockEntityRenderDispatcher().prepare(activeRenderInfo);
            minecraft.getEntityRenderDispatcher().prepare(activeRenderInfo, minecraft.crosshairPickEntity);
            PoseStack posestack = new PoseStack();
            Shaders.setCameraShadow(posestack, activeRenderInfo, partialTicks);
            Matrix4f matrix4f = posestack.last().pose();
            Shaders.checkGLError("shadow camera");
            Shaders.dispatchComputes(Shaders.dfb, Shaders.ProgramShadow.getComputePrograms());
            Shaders.useProgram(Shaders.ProgramShadow);
            Shaders.sfb.setDrawBuffers();
            Shaders.checkGLError("shadow drawbuffers");
            GL30.glReadBuffer(0);
            Shaders.checkGLError("shadow readbuffer");
            Shaders.sfb.setDepthTexture();
            Shaders.sfb.setColorTextures(true);
            Shaders.checkFramebufferStatus("shadow fb");
            GlStateManager._depthMask(true);
            GlStateManager.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager._clear(256);

            for (int i = 0; i < Shaders.usedShadowColorBuffers; i++) {
                if (Shaders.shadowBuffersClear[i]) {
                    Vector4f vector4f = Shaders.shadowBuffersClearColor[i];
                    if (vector4f != null) {
                        GlStateManager.clearColor(vector4f.x(), vector4f.y(), vector4f.z(), vector4f.w());
                    } else {
                        GlStateManager.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    GlState.setDrawBuffers(Shaders.drawBuffersColorAtt[i]);
                    GlStateManager._clear(16384);
                }
            }

            Shaders.sfb.setDrawBuffers();
            Shaders.checkGLError("shadow clear");
            profilerfiller.popPush("shadow frustum");
            Frustum frustum = makeShadowFrustum(activeRenderInfo, partialTicks);
            profilerfiller.popPush("shadow culling");
            Vec3 vec3 = activeRenderInfo.position();
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(515);
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager.lockCull(new GlCullState(false));
            GlStateManager.lockBlend(new GlBlendState(false));
            profilerfiller.popPush("shadow prepareterrain");
            minecraft.getTextureManager().bindTexture(TextureAtlas.LOCATION_BLOCKS);
            profilerfiller.popPush("shadow setupterrain");
            levelrenderer.setShadowRenderInfos(true);
            Lagometer.timerVisibility.start();
            if (!levelrenderer.isDebugFrustum()) {
                applyFrustumShadow(levelrenderer, frustum);
            }

            Lagometer.timerVisibility.end();
            profilerfiller.popPush("shadow updatechunks");
            profilerfiller.popPush("shadow terrain");
            double d0 = vec3.x();
            double d1 = vec3.y();
            double d2 = vec3.z();
            Lagometer.timerTerrain.start();
            ChunkSectionsToRender chunksectionstorender = levelrenderer.prepareChunkRenders(matrix4f, d0, d1, d2);
            GpuSampler gpusampler = levelrenderer.getChunkLayerSampler();
            if (Shaders.isRenderShadowTerrain()) {
                chunksectionstorender.renderGroup(ChunkSectionLayerGroup.OPAQUE, gpusampler);
                Shaders.checkGLError("shadow terrain opaque");
            }

            FeatureRenderDispatcher featurerenderdispatcher = levelrenderer.getFeatureRenderDispatcher();
            SubmitNodeStorage submitnodestorage = levelrenderer.getSubmitNodeStorage();
            LevelRenderState levelrenderstate = levelrenderer.getLevelRenderState();
            levelrenderstate.reset();
            profilerfiller.popPush("shadow entities");
            countEntitiesRenderedShadow = 0;
            countTileEntitiesRenderedShadow = 0;
            TickRateManager tickratemanager = minecraft.level.tickRateManager();
            float f = tickratemanager.runsNormally() ? partialTicks : 1.0F;
            LevelRenderer levelrenderer1 = minecraft.levelRenderer;
            MultiBufferSource.BufferSource multibuffersource$buffersource = levelrenderer1.getRenderTypeTextures().bufferSource();
            Shaders.beginEntities();
            profilerfiller.push("extract shadow entities");
            if (Shaders.isRenderShadowEntities()) {
                levelrenderer.extractVisibleEntities(activeRenderInfo, frustum, deltaTracker, levelrenderstate);
            }

            profilerfiller.pop();
            levelrenderer.submitEntities(posestack, levelrenderstate, submitnodestorage);
            featurerenderdispatcher.renderAllFeatures();
            countEntitiesRenderedShadow = levelrenderstate.entityRenderStates.size();
            multibuffersource$buffersource.endLastBatch();
            levelrenderer1.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS));
            multibuffersource$buffersource.endBatch(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            multibuffersource$buffersource.endBatch(RenderTypes.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            multibuffersource$buffersource.endBatch(RenderTypes.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
            Shaders.endEntities();
            Shaders.beginBlockEntities();
            SignRenderer.updateTextRenderDistance();
            boolean flag = Reflector.IForgeBlockEntity_getRenderBoundingBox.exists();
            float f1 = tickratemanager.isFrozen() ? f : partialTicks;
            profilerfiller.popPush("extract shadow blockEntities");
            if (Shaders.isRenderShadowBlockEntities()) {
                levelrenderer.extractVisibleBlockEntities(activeRenderInfo, partialTicks, levelrenderstate, frustum);
            }

            profilerfiller.pop();
            levelrenderer.submitBlockEntities(posestack, levelrenderstate, submitnodestorage);
            featurerenderdispatcher.renderAllFeatures();
            countTileEntitiesRenderedShadow = levelrenderstate.blockEntityRenderStates.size();
            levelrenderstate.reset();
            levelrenderer1.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.bedSheet());
            multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
            multibuffersource$buffersource.endBatch(Sheets.signSheet());
            multibuffersource$buffersource.endBatch(Sheets.chestSheet());
            multibuffersource$buffersource.endBatch();
            Shaders.endBlockEntities();
            Lagometer.timerTerrain.end();
            Shaders.checkGLError("shadow entities");
            GlStateManager._depthMask(true);
            GlStateManager._disableBlend();
            GlStateManager.unlockCull();
            GlStateManager._enableCull();
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
            GlStateManager.alphaFunc(516, 0.1F);
            if (Shaders.usedShadowDepthBuffers >= 2) {
                GlStateManager._activeTexture(33989);
                Shaders.checkGLError("pre copy shadow depth");
                GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, Shaders.shadowMapWidth, Shaders.shadowMapHeight);
                Shaders.checkGLError("copy shadow depth");
                GlStateManager._activeTexture(33984);
            }

            GlStateManager._disableBlend();
            GlStateManager._depthMask(true);
            minecraft.getTextureManager().bindTexture(TextureAtlas.LOCATION_BLOCKS);
            Shaders.checkGLError("shadow pre-translucent");
            Shaders.sfb.setDrawBuffers();
            Shaders.checkGLError("shadow drawbuffers pre-translucent");
            Shaders.checkFramebufferStatus("shadow pre-translucent");
            if (Shaders.isRenderShadowTranslucent()) {
                Lagometer.timerTerrain.start();
                profilerfiller.popPush("shadow translucent");
                chunksectionstorender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, gpusampler);
                Shaders.checkGLError("shadow translucent");
                Lagometer.timerTerrain.end();
            }

            GlStateManager.unlockBlend();
            GlStateManager._depthMask(true);
            GlStateManager._enableCull();
            GlStateManager._disableBlend();
            GL30.glFlush();
            Shaders.checkGLError("shadow flush");
            Shaders.isShadowPass = false;
            levelrenderer.setShadowRenderInfos(false);
            profilerfiller.popPush("shadow postprocess");
            if (Shaders.hasGlGenMipmap) {
                Shaders.sfb.generateDepthMipmaps(Shaders.shadowMipmapEnabled);
                Shaders.sfb.generateColorMipmaps(true, Shaders.shadowColorMipmapEnabled);
            }

            Shaders.checkGLError("shadow postprocess");
            if (Shaders.hasShadowcompPrograms) {
                Shaders.renderShadowComposites();
            }

            Shaders.dfb.bindFramebuffer();
            GlStateManager._viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
            GlState.setDrawBuffers(null);
            minecraft.getTextureManager().bindTexture(TextureAtlas.LOCATION_BLOCKS);
            Shaders.useProgram(Shaders.ProgramTerrain);
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(gpubufferslice, projectiontype);
            Shaders.checkGLError("shadow end");
        }
    }

    public static void applyFrustumShadow(LevelRenderer renderGlobal, Frustum frustum) {
        Minecraft minecraft = Config.getMinecraft();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("apply_shadow_frustum");
        int i = (int)Shaders.getShadowRenderDistance();
        int j = (int)Config.getGameRenderer().getRenderDistance();
        boolean flag = i > 0 && i < j;
        int k = flag ? i : -1;
        if (frustumTerrainShadowChanged || renderGlobal.needsFrustumUpdate()) {
            renderGlobal.applyFrustum(frustum, false, k);
            frustumTerrainShadowChanged = false;
        }

        if (frustumEntitiesShadowChanged || minecraft.level.getSectionStorage().isUpdated()) {
            renderGlobal.applyFrustumEntities(frustum, k);
            frustumEntitiesShadowChanged = false;
        }

        profilerfiller.pop();
    }

    public static Frustum makeShadowFrustum(Camera camera, float partialTicks) {
        if (!Shaders.isShadowCulling()) {
            return FrustumDummy.INSTANCE;
        }

        Minecraft minecraft = Config.getMinecraft();
        GameRenderer gamerenderer = Config.getGameRenderer();
        PoseStack posestack = new PoseStack();
        if (Reflector.ForgeEventFactoryClient_fireComputeCameraAngles.exists()) {
            ViewportEvent.ComputeCameraAngles viewportevent$computecameraangles = (ViewportEvent.ComputeCameraAngles)Reflector.ForgeEventFactoryClient_fireComputeCameraAngles
                .call(gamerenderer, camera, partialTicks);
            camera.setRotation(
                viewportevent$computecameraangles.getYaw(), viewportevent$computecameraangles.getPitch(), viewportevent$computecameraangles.getRoll()
            );
            posestack.mulPose(Axis.ZP.rotationDegrees(viewportevent$computecameraangles.getRoll()));
        }

        posestack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        posestack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
        float f1 = gamerenderer.getFov(camera, partialTicks, true);
        float f = Math.max(f1, minecraft.options.fov().get().intValue());
        Matrix4f matrix4f = gamerenderer.getProjectionMatrix(f);
        Matrix4f matrix4f1 = posestack.last().pose();
        Vec3 vec3 = camera.position();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        Frustum frustum = new ShadowFrustum(matrix4f1, matrix4f);
        frustum.prepare(d0, d1, d2);
        return frustum;
    }

    public static void updateActiveRenderInfo(Camera activeRenderInfo, Minecraft mc, float partialTicks) {
        activeRenderInfo.setup(
            mc.level,
            mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity(),
            !mc.options.getCameraType().isFirstPerson(),
            mc.options.getCameraType().isMirrored(),
            partialTicks
        );
    }

    public static void preRenderChunkLayer(ChunkSectionLayer blockLayerIn) {
        if (blockLayerIn == ChunkSectionLayer.SOLID) {
            beginTerrainSolid();
        }

        if (blockLayerIn == ChunkSectionLayer.CUTOUT) {
            beginTerrainCutout();
        }

        if (blockLayerIn == ChunkSectionLayer.TRANSLUCENT) {
            beginTranslucent();
        }

        if (blockLayerIn == ChunkSectionLayer.TRIPWIRE) {
            beginTripwire();
        }

        if (Shaders.isRenderBackFace(blockLayerIn)) {
            GlStateManager._disableCull();
        }

        Shaders.setTextureMatrix(Shaders.MATRIX_IDENTITY);
    }

    public static void postRenderChunkLayer(ChunkSectionLayer blockLayerIn) {
        if (Shaders.isRenderBackFace(blockLayerIn)) {
            GlStateManager._enableCull();
        }
    }

    public static void preRender(RenderType renderType) {
        if (Shaders.isRenderingWorld) {
            if (!Shaders.isShadowPass) {
                if (renderType.isGlint()) {
                    renderEnchantedGlintBegin();
                } else if (renderType.getName().equals("eyes")) {
                    Shaders.beginSpiderEyes();
                } else if (renderType.getName().equals("crumbling")) {
                    beginBlockDamage();
                } else if (renderType == RenderTypes.LINES || renderType == RenderTypes.LINES_TRANSLUCENT) {
                    Shaders.beginLines();
                } else if (renderType == RenderTypes.waterMask()) {
                    Shaders.beginWaterMask();
                } else if (renderType.getName().equals("beacon_beam")) {
                    Shaders.beginBeacon();
                }
            }
        }
    }

    public static void postRender(RenderType renderType) {
        if (Shaders.isRenderingWorld) {
            if (!Shaders.isShadowPass) {
                if (renderType.isGlint()) {
                    renderEnchantedGlintEnd();
                } else if (renderType.getName().equals("eyes")) {
                    Shaders.endSpiderEyes();
                } else if (renderType.getName().equals("crumbling")) {
                    endBlockDamage();
                } else if (renderType == RenderTypes.LINES || renderType == RenderTypes.LINES_TRANSLUCENT) {
                    Shaders.endLines();
                } else if (renderType == RenderTypes.waterMask()) {
                    Shaders.endWaterMask();
                } else if (renderType.getName().equals("beacon_beam")) {
                    Shaders.endBeacon();
                }
            }
        }
    }

    public static void enableArrayPointerVbo() {
        GL20.glEnableVertexAttribArray(Shaders.midBlockAttrib);
        GL20.glEnableVertexAttribArray(Shaders.midTexCoordAttrib);
        GL20.glEnableVertexAttribArray(Shaders.tangentAttrib);
        GL20.glEnableVertexAttribArray(Shaders.entityAttrib);
        GL20.glEnableVertexAttribArray(Shaders.velocityAttrib);
    }

    public static void setupArrayPointersVbo(boolean enableArrayIn) {
        int i = 18;
        if (enableArrayIn) {
            enableArrayPointerVbo();
        }

        GL20.glVertexAttribPointer(Shaders.midBlockAttrib, 3, 5120, false, 72, 32L);
        GL20.glVertexAttribPointer(Shaders.midTexCoordAttrib, 2, 5126, false, 72, 36L);
        GL20.glVertexAttribPointer(Shaders.tangentAttrib, 4, 5122, false, 72, 44L);
        GL20.glVertexAttribPointer(Shaders.entityAttrib, 3, 5122, false, 72, 52L);
        GL20.glVertexAttribPointer(Shaders.velocityAttrib, 3, 5126, false, 72, 60L);
    }

    public static void beaconBeamBegin() {
        Shaders.useProgram(Shaders.ProgramBeaconBeam);
    }

    public static void beaconBeamStartQuad1() {
    }

    public static void beaconBeamStartQuad2() {
    }

    public static void beaconBeamDraw1() {
    }

    public static void beaconBeamDraw2() {
        GlStateManager._disableBlend();
    }

    public static void renderEnchantedGlintBegin() {
        Shaders.useProgram(Shaders.ProgramArmorGlint);
    }

    public static void renderEnchantedGlintEnd() {
        if (Shaders.isRenderingWorld) {
            if (Shaders.isRenderingFirstPersonHand() && Shaders.isRenderBothHands()) {
                Shaders.useProgram(Shaders.ProgramHand);
            } else {
                Shaders.useProgram(Shaders.ProgramEntities);
            }
        } else {
            Shaders.useProgram(Shaders.ProgramNone);
        }
    }

    public static boolean renderEndPortal(
        EnumSet<Direction> directionsIn, float offsetUp, PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, int combinedLightIn, int combinedOverlayIn
    ) {
        Matrix4f matrix4f = matrixEntryIn.pose();
        Matrix3f matrix3f = matrixEntryIn.normal();
        VertexConsumer vertexconsumer = bufferIn;
        float f = 0.5F;
        float f1 = f * 0.5F;
        float f2 = f * 1.0F;
        float f3 = f * 1.0F;
        float f4 = 0.0F;
        float f5 = 0.2F;
        float f6 = f4;
        float f7 = f5;
        float f8 = (float)(System.currentTimeMillis() % 100000L) / 100000.0F;
        float f9 = f8;
        float f10 = offsetUp;
        int i = combinedLightIn;
        int j = combinedOverlayIn;
        float f11 = 0.0F;
        float f12 = 0.0F;
        float f13 = 0.0F;
        if (directionsIn.contains(Direction.SOUTH)) {
            Vec3i vec3i = Direction.SOUTH.getUnitVec3i();
            float f14 = vec3i.getX();
            float f15 = vec3i.getY();
            float f16 = vec3i.getZ();
            float f17 = MathUtils.getTransformX(matrix3f, f14, f15, f16);
            float f18 = MathUtils.getTransformY(matrix3f, f14, f15, f16);
            float f19 = MathUtils.getTransformZ(matrix3f, f14, f15, f16);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f17, f18, f19);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f17, f18, f19);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + 1.0F, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f17, f18, f19);
            vertexconsumer.addVertex(matrix4f, f11, f12 + 1.0F, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f17, f18, f19);
        }

        if (directionsIn.contains(Direction.NORTH)) {
            Vec3i vec3i1 = Direction.NORTH.getUnitVec3i();
            float f20 = vec3i1.getX();
            float f25 = vec3i1.getY();
            float f30 = vec3i1.getZ();
            float f35 = MathUtils.getTransformX(matrix3f, f20, f25, f30);
            float f40 = MathUtils.getTransformY(matrix3f, f20, f25, f30);
            float f45 = MathUtils.getTransformZ(matrix3f, f20, f25, f30);
            vertexconsumer.addVertex(matrix4f, f11, f12 + 1.0F, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f35, f40, f45);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + 1.0F, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f35, f40, f45);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f35, f40, f45);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f35, f40, f45);
        }

        if (directionsIn.contains(Direction.EAST)) {
            Vec3i vec3i2 = Direction.EAST.getUnitVec3i();
            float f21 = vec3i2.getX();
            float f26 = vec3i2.getY();
            float f31 = vec3i2.getZ();
            float f36 = MathUtils.getTransformX(matrix3f, f21, f26, f31);
            float f41 = MathUtils.getTransformY(matrix3f, f21, f26, f31);
            float f46 = MathUtils.getTransformZ(matrix3f, f21, f26, f31);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + 1.0F, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f36, f41, f46);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + 1.0F, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f36, f41, f46);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f36, f41, f46);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f36, f41, f46);
        }

        if (directionsIn.contains(Direction.WEST)) {
            Vec3i vec3i3 = Direction.WEST.getUnitVec3i();
            float f22 = vec3i3.getX();
            float f27 = vec3i3.getY();
            float f32 = vec3i3.getZ();
            float f37 = MathUtils.getTransformX(matrix3f, f22, f27, f32);
            float f42 = MathUtils.getTransformY(matrix3f, f22, f27, f32);
            float f47 = MathUtils.getTransformZ(matrix3f, f22, f27, f32);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f37, f42, f47);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f37, f42, f47);
            vertexconsumer.addVertex(matrix4f, f11, f12 + 1.0F, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f37, f42, f47);
            vertexconsumer.addVertex(matrix4f, f11, f12 + 1.0F, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f37, f42, f47);
        }

        if (directionsIn.contains(Direction.DOWN)) {
            Vec3i vec3i4 = Direction.DOWN.getUnitVec3i();
            float f23 = vec3i4.getX();
            float f28 = vec3i4.getY();
            float f33 = vec3i4.getZ();
            float f38 = MathUtils.getTransformX(matrix3f, f23, f28, f33);
            float f43 = MathUtils.getTransformY(matrix3f, f23, f28, f33);
            float f48 = MathUtils.getTransformZ(matrix3f, f23, f28, f33);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f38, f43, f48);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f38, f43, f48);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f38, f43, f48);
            vertexconsumer.addVertex(matrix4f, f11, f12, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f38, f43, f48);
        }

        if (directionsIn.contains(Direction.UP)) {
            Vec3i vec3i5 = Direction.UP.getUnitVec3i();
            float f24 = vec3i5.getX();
            float f29 = vec3i5.getY();
            float f34 = vec3i5.getZ();
            float f39 = MathUtils.getTransformX(matrix3f, f24, f29, f34);
            float f44 = MathUtils.getTransformY(matrix3f, f24, f29, f34);
            float f49 = MathUtils.getTransformZ(matrix3f, f24, f29, f34);
            vertexconsumer.addVertex(matrix4f, f11, f12 + f10, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f39, f44, f49);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + f10, f13 + 1.0F)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f4 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f39, f44, f49);
            vertexconsumer.addVertex(matrix4f, f11 + 1.0F, f12 + f10, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f7 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f39, f44, f49);
            vertexconsumer.addVertex(matrix4f, f11, f12 + f10, f13)
                .setColor(f1, f2, f3, 1.0F)
                .setUv(f5 + f8, f6 + f9)
                .setOverlay(j)
                .setLight(i)
                .setNormal(f39, f44, f49);
        }

        return true;
    }
}
