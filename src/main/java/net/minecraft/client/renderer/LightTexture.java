package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.OptionalInt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.shaders.Shaders;
import org.joml.Vector3f;

public class LightTexture implements AutoCloseable {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private static final int TEXTURE_SIZE = 16;
    private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putVec3()
        .putVec3()
        .get();
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private final MappableRingBuffer ubo;
    private final RandomSource randomSource = RandomSource.create();
    private boolean allowed = true;
    private boolean custom = false;
    public static final int MAX_BRIGHTNESS = pack(15, 15);
    public static final int VANILLA_EMISSIVE_BRIGHTNESS = 15794417;
    private final DynamicTexture dynamicTexture;
    private final NativeImage dynamicImage;
    private final DynamicTexture whiteTexture;

    public LightTexture(GameRenderer p_109878_, Minecraft p_109879_) {
        this.renderer = p_109878_;
        this.minecraft = p_109879_;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.dynamicTexture = new DynamicTexture("Light Texture", 16, 16, true);
        this.dynamicImage = this.dynamicTexture.getPixels();
        this.dynamicImage.fillRect(0, 0, 16, 16, -1);
        this.dynamicTexture.upload();
        this.minecraft.getTextureManager().register(Identifier.withDefaultNamespace("dynamic/light_map_1"), this.dynamicTexture);
        this.texture = this.dynamicTexture.getTexture();
        this.texture.setUsage(this.texture.usage() | 12);
        this.textureView = this.dynamicTexture.getTextureView();
        gpudevice.createCommandEncoder().clearColorTexture(this.texture, -1);
        this.ubo = new MappableRingBuffer(() -> "Lightmap UBO", 130, LIGHTMAP_UBO_SIZE);
        this.whiteTexture = new DynamicTexture("White Texture", 16, 16, true);
        this.whiteTexture.getPixels().fillRect(0, 0, 16, 16, -1);
        this.whiteTexture.upload();
    }

    public GpuTextureView getTextureView() {
        return !this.allowed ? this.whiteTexture.getTextureView() : this.textureView;
    }

    @Override
    public void close() {
        this.texture.close();
        this.textureView.close();
        this.ubo.close();
        this.dynamicTexture.close();
        this.whiteTexture.close();
    }

    public void tick() {
        this.blockLightRedFlicker = this.blockLightRedFlicker
            + (this.randomSource.nextFloat() - this.randomSource.nextFloat()) * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1F;
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    private float calculateDarknessScale(LivingEntity p_234313_, float p_234314_, float p_234315_) {
        float f = 0.45F * p_234314_;
        return Math.max(0.0F, Mth.cos((p_234313_.tickCount - p_234315_) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float p_109882_) {
        if (this.updateLightTexture) {
            this.updateLightTexture = false;
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                this.custom = false;
                if (Config.isCustomColors()) {
                    boolean flag = this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION) || this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER);
                    float f = this.getDarknessGammaFactor(p_109882_);
                    float f1 = this.getDarknessLightFactor(clientlevel, p_109882_);
                    float f2 = f * 0.25F + f1 * 0.75F;
                    if (CustomColors.updateLightmap(clientlevel, this.blockLightRedFlicker, this.dynamicImage, flag, f2, p_109882_)) {
                        this.dynamicTexture.upload();
                        this.updateLightTexture = false;
                        profilerfiller.pop();
                        this.custom = true;
                        return;
                    }
                }

                this.custom = false;
                Camera camera = this.minecraft.gameRenderer.getMainCamera();
                int i = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, p_109882_);
                float f10 = clientlevel.dimensionType().ambientLight();
                float f11 = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, p_109882_);
                EndFlashState endflashstate = clientlevel.endFlashState();
                Vector3f vector3f;
                if (endflashstate != null) {
                    vector3f = new Vector3f(0.99F, 1.12F, 1.0F);
                    if (!this.minecraft.options.hideLightningFlash().get()) {
                        float f3 = endflashstate.getIntensity(p_109882_);
                        if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                            f11 += f3 / 3.0F;
                        } else {
                            f11 += f3;
                        }
                    }
                } else {
                    vector3f = new Vector3f(1.0F, 1.0F, 1.0F);
                }

                float f12 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f4 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, p_109882_) * f12;
                float f5 = this.calculateDarknessScale(this.minecraft.player, f4, p_109882_) * f12;
                if (Config.isShaders()) {
                    Shaders.setDarknessFactor(f4);
                    Shaders.setDarknessLightFactor(f5);
                }

                float f6 = this.minecraft.player.getWaterVision();
                float f7;
                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    f7 = GameRenderer.getNightVisionScale(this.minecraft.player, p_109882_);
                } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    f7 = f6;
                } else {
                    f7 = 0.0F;
                }

                float f8 = this.blockLightRedFlicker + 1.5F;
                float f9 = this.minecraft.options.gamma().get().floatValue();
                CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

                try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
                    Std140Builder.intoBuffer(gpubuffer$mappedview.data())
                        .putFloat(f10)
                        .putFloat(f11)
                        .putFloat(f8)
                        .putFloat(f7)
                        .putFloat(f5)
                        .putFloat(this.renderer.getDarkenWorldAmount(p_109882_))
                        .putFloat(Math.max(0.0F, f9 - f4))
                        .putVec3(ARGB.vector3fFromRGB24(i))
                        .putVec3(vector3f);
                }

                try (RenderPass renderpass = commandencoder.createRenderPass(() -> "Update light", this.textureView, OptionalInt.empty())) {
                    renderpass.setPipeline(RenderPipelines.LIGHTMAP);
                    RenderSystem.bindDefaultUniforms(renderpass);
                    renderpass.setUniform("LightmapInfo", this.ubo.currentBuffer());
                    renderpass.draw(0, 3);
                }

                this.ubo.rotate();
                profilerfiller.pop();
            }
        }
    }

    public static float getBrightness(DimensionType p_234317_, int p_234318_) {
        return getBrightness(p_234317_.ambientLight(), p_234318_);
    }

    public static float getBrightness(float p_362774_, int p_368270_) {
        float f = p_368270_ / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(p_362774_, f1, 1.0F);
    }

    public static int pack(int p_109886_, int p_109887_) {
        return p_109886_ << 4 | p_109887_ << 20;
    }

    public static int block(int p_109884_) {
        return (p_109884_ & 65535) >> 4;
    }

    public static int sky(int p_109895_) {
        return p_109895_ >>> 20 & 15;
    }

    public static int lightCoordsWithEmission(int p_363075_, int p_361575_) {
        if (p_361575_ == 0) {
            return p_363075_;
        }

        int i = Math.max(sky(p_363075_), p_361575_);
        int j = Math.max(block(p_363075_), p_361575_);
        return pack(j, i);
    }

    public boolean isAllowed() {
        return this.allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isCustom() {
        return this.custom;
    }

    public float getDarknessGammaFactor(float partialTicks) {
        float f = this.minecraft.options.darknessEffectScale().get().floatValue();
        return this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * f;
    }

    public float getDarknessLightFactor(ClientLevel clientLevel, float partialTicks) {
        float f = this.minecraft.options.darknessEffectScale().get().floatValue();
        float f1 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * f;
        return this.calculateDarknessScale(this.minecraft.player, f1, partialTicks) * f;
    }
}
