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
import ru.arixcompany.features.event.world.EventUpdate;
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

    private final ValueSetting globalLimit = new ValueSetting("Общий лимит")
            .range(50, 2000).setValue(1000).step(50);

    private final ValueSetting speed = new ValueSetting("Скорость").range(0.1f, 2.0f).setValue(0.4f);
    private final ValueSetting gravity = new ValueSetting("Гравитация").range(-0.02f, 0.02f).setValue(0.005f);
    private final ValueSetting friction = new ValueSetting("Трение").range(0.8f, 1.0f).setValue(0.97f);
    private final ValueSetting bounce = new ValueSetting("Отскок").range(0.0f, 1.0f).setValue(0.6f);
    private final ValueSetting lifeTime = new ValueSetting("Время жизни").range(20, 300).setValue(80).step(5);
    private final BooleanSetting collision = new BooleanSetting("Коллизия").setValue(true);

    private final GroupSetting behaviorGroup = new GroupSetting("Поведение",
            speed, gravity, friction, bounce, lifeTime, collision, globalLimit);

    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
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
        setup(mode, size, triggers, worldAmount, attackAmount, totemAmount, behaviorGroup);
    }

    @Override
    public void deactivate() {
        particles.clear();
        super.deactivate();
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        particles.removeIf(p -> p.age >= p.maxAge);

        for (Particle p : particles) {
            p.update(friction.getValue(), gravity.getValue(), bounce.getValue(), collision.isValue());
        }

        if (triggers.isSelected("Мир") && particles.size() < worldAmount.getValue()) {
            spawn(mc.player.position().add(rand(-10, 10), rand(0, 5), rand(-10, 10)), 1, false);
        }
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (triggers.isSelected("Атака") && e.getTarget() != null) {
            spawn(e.getTarget().position().add(0, e.getTarget().getBbHeight() / 1.5, 0), (int) attackAmount.getValue(), false);
        }
    }

    @EventHandler
    public void onTotem(EventTotemPop e) {
        if (triggers.isSelected("Тотем")) {
            spawn(e.getEntity().position().add(0, 1.0, 0), (int) totemAmount.getValue(), true);
        }
    }

    private void spawn(Vec3 pos, int count, boolean totem) {
        for (int i = 0; i < count; i++) {
            if (particles.size() >= globalLimit.getValue()) return;

            Particle p = new Particle();
            p.pos = pos;

            float s = speed.getValue();
            p.velocity = new Vec3(rand(-0.2f, 0.2f) * s, rand(-0.1f, 0.3f) * s, rand(-0.2f, 0.2f) * s);

            p.maxAge = (int) (lifeTime.getValue() * rand(0.8f, 1.2f));
            p.rotation = random.nextInt(360);
            p.rotSpeed = rand(-5f, 5f);
            p.isTotem = totem;
            particles.add(p);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (particles.isEmpty() || mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        PoseStack matrices = e.getMatrixStack();
        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);

        try (allocator) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);

            for (Particle p : particles) {
                float progress = (float) p.age / p.maxAge;
                float lifeScale = 1.0f - progress;

                float currentSize = size.getValue() * (0.4f + lifeScale * 0.6f);
                int alpha = (int) (lifeScale * 255);

                matrices.pushPose();
                matrices.translate(p.pos.x - cam.x, p.pos.y - cam.y, p.pos.z - cam.z);

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
        private Vec3 velocity;
        private int age = 0;
        private int maxAge;
        private float rotation;
        private float rotSpeed;
        private boolean isTotem;

        public void update(float friction, float gravity, float bounce, boolean collision) {
            age++;

            velocity = velocity.subtract(0, gravity, 0).scale(friction);

            Vec3 nextPos = pos.add(velocity);

            if (collision && mc.level != null) {
                BlockPos bp = BlockPos.containing(nextPos);
                if (!mc.level.getBlockState(bp).isAir()) {
                    velocity = new Vec3(velocity.x * 0.6, -velocity.y * bounce, velocity.z * 0.6);
                    rotSpeed *= 0.5f;
                    nextPos = pos.add(velocity);
                }
            }

            pos = nextPos;
            rotation += rotSpeed;
        }
    }
}