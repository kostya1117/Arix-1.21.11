package ru.arixcompany.utils.render.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventPostTick;
import ru.arixcompany.features.event.world.EventPreTick;
import ru.arixcompany.features.module.modules.render.Particles;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.Render3dUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ParticleSystem implements IMinecraft {
	private static final ParticleSystem INSTANCE = new ParticleSystem();
	private static final int TOTEM_DURATION = 20;
	private static final float GRAVITY_STRENGTH = 0.04F;
	private static final float TOTEM_AIR_FRICTION = 1.1F;
	private static final float TOTEM_GROUND_FRICTION = 0.7F;
	private static final float TOTEM_COLLISION_FRICTION = 0.4F;
	private static final double WORLD_MIN_RADIUS = 3.0;
	private static final double WORLD_MAX_RADIUS = 60.0;
	private static final double WORLD_MAX_HEIGHT = 25.0;
	private static final double WORLD_DESPAWN_DISTANCE = 65.0;
	private static final int[] RANDOM_COLORS = {
			0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00,
			0xFF00FFFF, 0xFF0000FF, 0xFF8B00FF, 0xFFFF00FF,
			0xFFFF1493, 0xFFFFFFFF, 0xFF00FF7F, 0xFFFF6347
	};

	//128 x 3 = 384
	private static final int PARTICLE_POOL_SIZE = 384;
	private static final int WORLD_PARTICLE_POOL_SIZE = 384;

	private final ParticlePool particlePool = new ParticlePool(PARTICLE_POOL_SIZE);
	private final ParticlePool worldParticlePool = new ParticlePool(WORLD_PARTICLE_POOL_SIZE);

	private final List<Particle3D> particles = new ArrayList<>();
	private final List<Particle3D> worldParticles = new ArrayList<>();
	private final List<TotemEmitter> totemEmitters = new ArrayList<>();

	@Getter
	private final Settings settings = new Settings();

	private float walkParticleAccumulator;
	private Vec3 lastPlayerPos = Vec3.ZERO;
	private Vec3 playerVelocity = Vec3.ZERO;
	private double playerSpeed;
	private long lastWorldSpawnTime;

	public ParticleSystem() {
		EventRepo.register(this);
	}

	public static ParticleSystem getInstance() {
		return INSTANCE;
	}


	@EventHandler
	public void tick(EventPostTick event) {
		if (!settings.isEnabled() || mc.player == null || mc.level == null) {
			clear();
			return;
		}

		if (settings.isWalkTrigger()) {
			handleWalkParticles(mc);
		} else {
			walkParticleAccumulator = 0.0F;
		}

		if (settings.isProjectileTrigger()) {
			handleProjectileParticles(mc);
		}

		handleTotemEmitters();
		handleWorldParticles(mc);
		updateParticles(particles, particlePool);
		updateParticles(worldParticles, worldParticlePool);
	}

	public void spawnAttackParticles(Entity target) {
		if (!settings.isEnabled() || !settings.isAttackTrigger() || target == null) return;

		float spreadValue = settings.getSpread() * 0.15F;
		for (int i = 0; i < settings.getAttackAmount(); i++) {
			Vec3 position = new Vec3(
					target.getX(),
					target.getY() + Math.random() * target.getBbHeight(),
					target.getZ()
			);
			Vec3 velocity = new Vec3(
					(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed(),
					(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed(),
					(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed()
			);
			particles.add(createParticle(position, velocity, settings.getSize(), settings.getLifeTime()));
		}
	}

	public void spawnTotemParticles(Entity entity) {
		if (settings.isEnabled() && settings.isTotemTrigger() && entity != null) {
			totemEmitters.add(new TotemEmitter(entity, TOTEM_DURATION));
		}
	}

	@EventHandler
	public void render(EventRender3D context) {
		if ((particles.isEmpty() && worldParticles.isEmpty())
				|| mc.player == null || mc.level == null) return;

		//if (Render3dUtils.isInView(mc.player)) {
		PoseStack matrices = context.getMatrixStack();
		MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		for (Particle3D particle : particles) {
			particle.render(matrices, immediate, settings.getGlowSize(), tickDelta);
		}
		for (Particle3D particle : worldParticles) {
			particle.render(matrices, immediate, settings.getWorldGlowSize(), tickDelta);
		}

		immediate.endBatch();
		//	}
	}
	private void clear() {
		releaseAll(particles, particlePool);
		releaseAll(worldParticles, worldParticlePool);
		totemEmitters.clear();
		walkParticleAccumulator = 0.0F;
		lastPlayerPos = Vec3.ZERO;
		playerVelocity = Vec3.ZERO;
		playerSpeed = 0.0;
	}

	private static void releaseAll(List<Particle3D> list, ParticlePool pool) {
		for (Particle3D p : list) {
			pool.release(p);
		}
		list.clear();
	}

	private static void updateParticles(List<Particle3D> targetParticles, ParticlePool pool) {
		Iterator<Particle3D> iterator = targetParticles.iterator();
		while (iterator.hasNext()) {
			Particle3D particle = iterator.next();
			particle.update();
			if (particle.isDead()) {
				iterator.remove();
				pool.release(particle);
			}
		}
	}

	private void handleWalkParticles(Minecraft client) {
		double velocitySq = client.player.getDeltaMovement().lengthSqr();
		boolean isMoving = velocitySq > 0.0001 && !client.player.isShiftKeyDown();

		if (!isMoving) {
			walkParticleAccumulator = 0.0F;
			return;
		}

		float particlesPerTick = settings.getWalkAmount() / 20.0F;
		walkParticleAccumulator += particlesPerTick;
		int particlesToSpawn = (int) walkParticleAccumulator;
		walkParticleAccumulator -= particlesToSpawn;

		if (particlesToSpawn <= 0) return;

		float yaw = client.player.yRot;
		double radians = Math.toRadians(yaw + 90.0F);
		double offsetX = Math.cos(radians) * 0.5;
		double offsetZ = Math.sin(radians) * 0.5;
		float spreadValue = settings.getSpread() * 0.05F;

		for (int i = 0; i < particlesToSpawn; i++) {
			Vec3 position = new Vec3(
					client.player.getX() - offsetX + (Math.random() - 0.5) * 0.3,
					client.player.getY() + 0.3 + Math.random() * (client.player.getBbHeight() - 0.3),
					client.player.getZ() - offsetZ + (Math.random() - 0.5) * 0.3
			);
			Vec3 velocity = new Vec3(
					(Math.random() - 0.5) * spreadValue * settings.getSpeed(),
					(Math.random() - 0.5) * spreadValue * 0.5 * settings.getSpeed(),
					(Math.random() - 0.5) * spreadValue * settings.getSpeed()
			);
			particles.add(createParticle(position, velocity,
					settings.getSize() * 0.6F, settings.getLifeTime() * 0.5F));
		}
	}

	private void handleProjectileParticles(Minecraft client) {
		float spreadValue = settings.getSpread() * 0.03F;

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof ThrowableProjectile
					|| entity instanceof Arrow
					|| entity instanceof ThrownTrident)) continue;

			Projectile projectile = (Projectile) entity;
			boolean isMoving = Math.abs(projectile.getX() - projectile.xo) > 0.01
					|| Math.abs(projectile.getY() - projectile.yo) > 0.01
					|| Math.abs(projectile.getZ() - projectile.zo) > 0.01;

			if (!isMoving && projectile.getDeltaMovement().lengthSqr() <= 0.01) continue;

			for (int i = 0; i < 2; i++) {
				Vec3 position = new Vec3(
						projectile.getX() + (Math.random() - 0.5) * 0.5,
						projectile.getY() + Math.random() * projectile.getBbHeight(),
						projectile.getZ() + (Math.random() - 0.5) * 0.5
				);
				Vec3 velocity = new Vec3(
						(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed(),
						(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed(),
						(Math.random() - 0.5) * 2.0 * spreadValue * settings.getSpeed()
				);
				particles.add(createParticle(position, velocity,
						settings.getSize() * 0.5F, settings.getLifeTime() * 0.3F));
			}
		}
	}

	private void handleTotemEmitters() {
		Iterator<TotemEmitter> iterator = totemEmitters.iterator();
		while (iterator.hasNext()) {
			TotemEmitter emitter = iterator.next();
			emitter.tick();
			if (emitter.isAlive()) {
				spawnTotemBurst(emitter.getEntity(), emitter.getProgress());
			} else {
				iterator.remove();
			}
		}
	}

	private void handleWorldParticles(Minecraft client) {
		if (!settings.isWorldParticles()) {
			releaseAll(worldParticles, worldParticlePool);
			lastPlayerPos = Vec3.ZERO;
			playerVelocity = Vec3.ZERO;
			playerSpeed = 0.0;
			return;
		}

		Vec3 currentPos = client.player.position();
		if (lastPlayerPos != Vec3.ZERO) {
			playerVelocity = currentPos.subtract(lastPlayerPos);
			playerSpeed = playerVelocity.horizontalDistance();
		}
		lastPlayerPos = currentPos;

		double despawnDistanceSq = WORLD_DESPAWN_DISTANCE * WORLD_DESPAWN_DISTANCE;

		Iterator<Particle3D> it = worldParticles.iterator();
		while (it.hasNext()) {
			Particle3D p = it.next();
			if (p.getHorizontalDistanceSquaredTo(currentPos) > despawnDistanceSq) {
				it.remove();
				worldParticlePool.release(p);
			}
		}

		int delay = calculateWorldSpawnDelay(playerSpeed);
		long now = System.currentTimeMillis();
		if (worldParticles.size() >= settings.getWorldAmount()
				|| now - lastWorldSpawnTime < delay) return;

		int spawnCount = calculateWorldSpawnCount(
				playerSpeed, worldParticles.size(), settings.getWorldAmount());

		for (int i = 0; i < spawnCount && worldParticles.size() < settings.getWorldAmount(); i++) {
			worldParticles.add(createWorldParticle(currentPos));
		}

		lastWorldSpawnTime = now;
	}

	private void spawnTotemBurst(Entity entity, float progress) {
		if (entity == null || entity.isRemoved()) return;

		float spreadMultiplier = 1.0F - progress * 0.5F;
		int particleCount = settings.getTotemAmount();
		if (particleCount < 1)
			particleCount = 1;
		for (int i = 0; i < particleCount; i++) {
			double x = Math.random() * 2.0 - 1.0;
			double y = Math.random() * 2.0 - 1.0;
			double z = Math.random() * 2.0 - 1.0;

			if (x * x + y * y + z * z > 1.0) continue;

			Vec3 position = new Vec3(
					entity.getX() + x * entity.getBbWidth() * 0.5,
					entity.getY(0.5) + y * entity.getBbHeight() * 0.5,
					entity.getZ() + z * entity.getBbWidth() * 0.5
			);
			double velocityScale = settings.getSpread() * 0.18 * spreadMultiplier * settings.getSpeed();
			double upward = Math.random() < 0.4
					? (0.15 + Math.random() * 0.2) * settings.getSpeed()
					: (0.03 + Math.random() * 0.07) * settings.getSpeed();
			Vec3 velocity = new Vec3(x * velocityScale, upward, z * velocityScale);

			particles.add(createParticle(position, velocity,
					settings.getSize() * 0.8F, settings.getLifeTime() * 0.8F, getTotemColor()));
		}
	}


	private Particle3D createParticle(Vec3 position, Vec3 velocity,
	                                  float size, float lifeTime) {
		return createParticle(position, velocity, size, lifeTime, getParticleColor());
	}

	private Particle3D createParticle(Vec3 position, Vec3 velocity,
	                                  float size, float lifeTime, int color) {
		Particle3D p = particlePool.acquire();
		p.init(position, velocity, color, size, lifeTime);
		p.setGravity(getGravity());
		p.setVelocityMultiplier(0.99F);
		p.setMode(settings.getParticleMode());
		p.setGlowMode(settings.getGlowMode());
		return p;
	}

	private Particle3D createWorldParticle(Vec3 playerPos) {
		double radius = WORLD_MIN_RADIUS + Math.random() * (WORLD_MAX_RADIUS - WORLD_MIN_RADIUS);
		double angle = Math.random() * Math.PI * 2.0;
		double spawnX = playerPos.x;
		double spawnZ = playerPos.z;

		if (playerSpeed > 0.05 && playerVelocity.horizontalDistance() > 0.01) {
			Vec3 normalizedVelocity = playerVelocity.normalize();
			double forwardAngle = Math.atan2(normalizedVelocity.z, normalizedVelocity.x);
			double angleSpread = Math.PI * 0.4;
			angle = forwardAngle + (Math.random() - 0.5) * angleSpread * 2.0;
			double forwardOffset = radius * 0.7 * Math.min(playerSpeed * 8.0, 1.0);
			spawnX += normalizedVelocity.x * forwardOffset;
			spawnZ += normalizedVelocity.z * forwardOffset;
		}

		Vec3 position = new Vec3(
				spawnX + Math.cos(angle) * radius,
				playerPos.y - 5.0 + Math.random() * WORLD_MAX_HEIGHT,
				spawnZ + Math.sin(angle) * radius
		);
		Vec3 velocity = new Vec3(
				(Math.random() - 0.5) * 0.08,
				(Math.random() - 0.5) * 0.02,
				(Math.random() - 0.5) * 0.08
		);

		float gravity = settings.isWorldPhysics() ? 0.0002F : 0.0F;

		Particle3D p = worldParticlePool.acquire(); // ← из пула, не new
		p.init(position, velocity, getParticleColor(), settings.getWorldSize(), settings.getWorldLifeTime());
		p.setGravity(gravity);
		p.setVelocityMultiplier(0.99F);
		p.setMode(settings.getWorldMode());
		p.setGlowMode(settings.getGlowMode());
		p.setSpinning(settings.isSpinning());
		p.setCollision(settings.isWorldPhysics());
		return p;
	}

	private static int calculateWorldSpawnDelay(double playerSpeed) {
		int baseDelay = 40;
		if (playerSpeed <= 0.05) return baseDelay;
		double speedFactor = Math.min(playerSpeed * 5.0, 4.0);
		return Math.max((int) (baseDelay / (1.0 + speedFactor)), 8);
	}

	private static int calculateWorldSpawnCount(double playerSpeed,
	                                            int currentCount, int maxCount) {
		int spawnCount = 1;
		if (playerSpeed > 0.1) {
			spawnCount = Math.min(8, maxCount - currentCount);
			spawnCount = Math.max(1, (int) (spawnCount * Math.min(playerSpeed * 5.0, 1.0)));
		}
		return spawnCount;
	}

	private int getParticleColor() {
		if (settings.isRandomColor()) {
			return RANDOM_COLORS[ThreadLocalRandom.current().nextInt(RANDOM_COLORS.length)];
		}
		return settings.getColor();
	}

	private int getTotemColor() {
		int[] totemColors = {
				0xFF7CFC00, 0xFFFFD700, 0xFF32CD32,
				0xFFFFA500, 0xFF00FF00, 0xFFADFF2F
		};
		return totemColors[ThreadLocalRandom.current().nextInt(totemColors.length)];
	}

	private float getGravity() {
		return (1.0F - 0.9F) * GRAVITY_STRENGTH;
	}

	public final class Settings {
		Particles particle = Arix.getInstance().getModuleRepo().getModule(Particles.class);
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isSpinning() {
			return particle != null && particle.spinning.isValue();
		}
		public boolean isAttackTrigger() {
			return particle != null && particle.attackTrigger.isValue();
		}
		public boolean isTotemTrigger() {
			return particle != null && particle.totemTrigger.isValue();
		}
		public boolean isWalkTrigger() {
			return particle != null && particle.walkTrigger.isValue();
		}
		public boolean isProjectileTrigger() {
			return particle != null && particle.projectileTrigger.isValue();
		}
		public boolean isWorldParticles() {
			return particle != null && particle.worldParticles.isValue();
		}
		public boolean isWorldPhysics() {
			return particle != null && particle.worldPhysics.isValue();
		}
		public boolean isRandomColor() {
			return particle != null && particle.randomColor.isValue();
		}

		public Particle3D.ParticleMode getParticleMode() {
			return particle != null ? mapParticleMode(particle.particleMode.getSelected()) : Particle3D.ParticleMode.CUBES;
		}
		public Particle3D.ParticleMode getWorldMode() {
			return particle != null ? mapParticleMode(particle.worldMode.getSelected()) : Particle3D.ParticleMode.CUBES;
		}
		public Particle3D.GlowMode getGlowMode() {
			return particle != null ? mapGlowMode(particle.glowMode.getSelected()) : null;
		}

		public int getAttackAmount() {
			return particle != null ? particle.attackAmount.getInt() : 30;
		}
		public int getWalkAmount() {
			return particle != null ? particle.walkAmount.getInt() : 15;
		}
		public int getTotemAmount() {
			return particle != null ? particle.totemAmount.getInt() : 40;
		}
		public int getWorldAmount() {
			return particle != null ? particle.worldAmount.getInt() : 150;
		}
		public float getSpread() {
			return particle != null ? particle.spread.getValue() : 1.0f;
		}
		public float getSpeed() {
			return particle != null ? particle.speed.getValue() : 1.0f;
		}
		public float getLifeTime() {
			return particle != null ? particle.lifeTime.getValue() : 2.0f;
		}
		public float getSize() {
			return particle != null ? particle.size.getValue() : 0.6f;
		}
		public float getGlowSize() {
			return particle != null ? particle.glowSize.getValue() : 3.0f;
		}
		public float getWorldLifeTime() {
			return particle != null ? particle.worldLifeTime.getValue() : 15.0f;
		}
		public float getWorldSize() {
			return particle != null ? particle.worldSize.getValue() : 0.5f;
		}
		public float getWorldGlowSize() {
			return particle != null ? particle.worldGlowSize.getValue() : 2.0f;
		}
		public int getColor() {
			return ColorUtil.getTheme().getMain().getRGB();
		}

	}
	private Particle3D.ParticleMode mapParticleMode(String modeName) {
		return switch (modeName) {
			case "Кубы" -> Particle3D.ParticleMode.CUBES;
			case "Корона" -> Particle3D.ParticleMode.CROWN;
			case "Взрыв кубов" -> Particle3D.ParticleMode.CUBE_BLAST;
			case "Доллары" -> Particle3D.ParticleMode.DOLLAR;
			case "Сердца" -> Particle3D.ParticleMode.HEART;
			case "Молния" -> Particle3D.ParticleMode.LIGHTNING;
			case "Линия" -> Particle3D.ParticleMode.LINE;
			case "Ромб" -> Particle3D.ParticleMode.RHOMBUS;
			case "Снежинки" -> Particle3D.ParticleMode.SNOWFLAKE;
			case "Звезды" -> Particle3D.ParticleMode.STAR;
			case "Звезды альт" -> Particle3D.ParticleMode.STAR_ALT;
			case "Треугольник" -> Particle3D.ParticleMode.TRIANGLE;
			case "Случайный" -> Particle3D.ParticleMode.RANDOM;
			default -> Particle3D.ParticleMode.CUBES;
		};
	}

	private Particle3D.GlowMode mapGlowMode(String glowName) {
		return switch (glowName) {
			case "Блум" -> Particle3D.GlowMode.BLOOM;
			case "Блум образец" -> Particle3D.GlowMode.BLOOM_SAMPLE;
			case "Оба" -> Particle3D.GlowMode.BOTH;
			default -> null;
		};
	}
}
