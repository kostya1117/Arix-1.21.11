package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.features.event.player.EventTotemPop;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventParticleUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.utils.Textures;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Particles extends Module {

    private final SelectSetting mode = new SelectSetting("Вид")
            .value("Снежинки", "Звезды", "Сердца", "Доллары", "Свет");

    private final ValueSetting size = new ValueSetting("Размер")
            .range(0.05f, 1.0f).setValue(0.15f).step(0.05f);

    private final ListSetting triggers = new ListSetting("Появление")
            .value("Мир", "Атака", "Тотем")
            .selected("Мир", "Атака", "Тотем");

    private final ValueSetting worldAmount = new ValueSetting("Частиц в мире")
            .range(1, 500).setValue(150).step(5)
            .visible(() -> triggers.isSelected("Мир"));

    private final ValueSetting attackAmount = new ValueSetting("Частиц при атаке")
            .range(1, 50).setValue(10).step(1)
            .visible(() -> triggers.isSelected("Атака"));

    private final ValueSetting totemAmount = new ValueSetting("Частиц при тотеме")
            .range(1, 100).setValue(40).step(2)
            .visible(() -> triggers.isSelected("Тотем"));

    private final ValueSetting lifeTime = new ValueSetting("Время жизни")
            .range(20, 300).setValue(80).step(5);

    private final ValueSetting globalLimit = new ValueSetting("Общий лимит")
            .range(50, 2000).setValue(1000).step(50);

    private final GroupSetting behaviorGroup = new GroupSetting("Поведение",
            lifeTime, worldAmount, attackAmount, totemAmount, globalLimit);

    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PendingBurst> bursts = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private static final RenderPipeline PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/particles"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public Particles() {
        super("Particles", Category.Render);
        setup(mode, size, triggers, behaviorGroup);
    }

    @Override
    public void deactivate() {
        particles.clear();
        bursts.clear();
        super.deactivate();
    }

    @EventHandler
    public void onUpdate(EventGameTick e) {
        if (mc.player == null) return;

        particles.removeIf(p -> p.age >= p.maxAge);

        for (Particle p : particles) {
            p.update();
        }

        // Обработка постепенного появления
        bursts.removeIf(b -> {
            int toSpawn = Math.min(b.remaining, b.perTick);
            spawnInternal(b.pos, toSpawn, b.totem);
            b.remaining -= toSpawn;
            return b.remaining <= 0;
        });

        if (triggers.isSelected("Мир") && particles.size() < worldAmount.getValue()) {
            spawnInternal(mc.player.position().add(rand(-10, 10), rand(0, 5), rand(-10, 10)), 1, false);
        }
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (triggers.isSelected("Атака") && e.getTarget() != null) {
            spawnInternal(e.getTarget().position().add(0, e.getTarget().getBbHeight() / 1.5, 0), (int) attackAmount.getValue(), false);
        }
    }

    @EventHandler
    public void onTotem(EventTotemPop e) {
        if (triggers.isSelected("Тотем")) {
            int total = (int) totemAmount.getValue();
            bursts.add(new PendingBurst(e.getEntity().position().add(0, 1.0, 0), total, Math.max(1, total / 5), true));
        }
    }

    private void spawnInternal(Vec3 pos, int count, boolean totem) {
        for (int i = 0; i < count; i++) {
            if (particles.size() >= globalLimit.getValue()) return;

            Particle p = new Particle();
            p.pos = p.prevPos = pos;
            p.isTotem = totem;

            if (totem) {
                double f = random.nextFloat() * 2.0F - 1.0F;
                double g = random.nextFloat() * 2.0F - 1.0F;
                double h = random.nextFloat() * 2.0F - 1.0F;
                if (f * f + g * g + h * h <= 1.0D) {
                    p.velocity = new Vec3(f * 0.2D, g * 0.2D + 0.1D, h * 0.2D);
                } else {
                    p.velocity = new Vec3(0, 0.1D, 0);
                }
            } else {
                p.velocity = new Vec3(rand(-0.15f, 0.15f), rand(-0.05f, 0.2f), rand(-0.15f, 0.15f));
            }

            p.maxAge = (int) (lifeTime.getValue() * rand(0.8f, 1.2f));
            p.rotation = random.nextInt(360);
            p.rotSpeed = rand(-4f, 4f);
            particles.add(p);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (particles.isEmpty() || mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        PoseStack matrices = e.getMatrixStack();
        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);
        float delta = mc.getDeltaTracker().getGameTimeDeltaTicks();

        try (allocator) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);

            for (Particle p : particles) {
                float progress = (float) p.age / p.maxAge;
                float lifeScale = 1.0f - progress;

                float currentSize = size.getValue() * (0.5f + lifeScale * 0.5f);
                int alpha = (int) (lifeScale * 255);

                double renderX = net.minecraft.util.Mth.lerp(delta, p.prevPos.x, p.pos.x);
                double renderY = net.minecraft.util.Mth.lerp(delta, p.prevPos.y, p.pos.y);
                double renderZ = net.minecraft.util.Mth.lerp(delta, p.prevPos.z, p.pos.z);

                matrices.pushPose();
                matrices.translate(renderX - cam.x, renderY - cam.y, renderZ - cam.z);

                matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());
                matrices.mulPose(Axis.ZP.rotationDegrees(p.rotation));
                matrices.scale(currentSize, currentSize, currentSize);

                Matrix4f matrix = matrices.last().pose();
                VertexConsumer vertex = bufferSource.getBuffer(makeRenderType());

                Color color = p.isTotem ? new Color(140, 252, 63) : Arix.getInstance().getCurrentTheme().getMain();

                vertex.addVertex(matrix, -1, -1, 0).setUv(0, 1).setColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                vertex.addVertex(matrix, 1, -1, 0).setUv(1, 1).setColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                vertex.addVertex(matrix, 1, 1, 0).setUv(1, 0).setColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                vertex.addVertex(matrix, -1, 1, 0).setUv(0, 0).setColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);

                matrices.popPose();
            }
            bufferSource.endBatch();
        }
    }

    private RenderType makeRenderType() {
        Identifier tex = switch (mode.getSelected()) {
            case "Снежинки" -> Textures.snowflake;
            case "Звезды" -> Textures.star;
            case "Сердца" -> Textures.heart;
            case "Доллары" -> Textures.dollar;
            case "Свет" -> Textures.firefly;
            default -> Textures.glow;
        };

        return RenderType.create("arix_particles",
                RenderSetup.builder(PARTICLE_PIPELINE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, tex)
                        .createRenderSetup()
        );
    }

    private float rand(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    @Getter @Setter
    private class Particle {
        private Vec3 pos;
        private Vec3 prevPos;
        private Vec3 velocity;
        private int age = 0;
        private int maxAge;
        private float rotation;
        private float rotSpeed;
        private boolean isTotem;
        private boolean stuck = false;

        public void update() {
            prevPos = pos;
            age++;
            if (stuck) return;

            double friction = 0.98D;
            double gravity = 0.04D;

            if (isTotem) {
                friction = 0.88D;
            }

            velocity = velocity.scale(friction).subtract(0, gravity, 0);
            Vec3 nextPos = pos.add(velocity);

            if (mc.level != null) {
                BlockPos bp = BlockPos.containing(nextPos);
                if (!mc.level.getBlockState(bp).isAir()) {
                    stuck = true;
                    velocity = Vec3.ZERO;
                    rotSpeed = 0;
                    return;
                }
            }

            pos = nextPos;
            rotation += rotSpeed;
        }
    }

    private static class PendingBurst {
        protected Vec3 pos;
        protected int remaining;
        protected int perTick;
        protected boolean totem;

        public PendingBurst(Vec3 pos, int total, int perTick, boolean totem) {
            this.pos = pos;
            this.remaining = total;
            this.perTick = perTick;
            this.totem = totem;
        }
    }
}