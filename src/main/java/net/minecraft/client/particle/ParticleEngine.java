package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import sp.SPConfig;
import sp.mixin.SPAccessor;

public class ParticleEngine {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS);
    protected ClientLevel level;
    private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
    private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
    private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap<>();
    private final ParticleResources resourceManager;
    private final RandomSource random = RandomSource.create();
    private static final Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> factories = new HashMap<>();
    private static final List<ParticleRenderType> particleRenderOrder = new ArrayList<>(RENDER_ORDER);

    public ParticleEngine(ClientLevel p_107299_, ParticleResources p_423228_) {
        this.level = p_107299_;
        this.resourceManager = p_423228_;
    }

    public void createTrackingEmitter(Entity p_107330_, ParticleOptions p_107331_) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, p_107330_, p_107331_));
    }

    public void createTrackingEmitter(Entity p_107333_, ParticleOptions p_107334_, int p_107335_) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, p_107333_, p_107334_, p_107335_));
    }

    public  Particle createParticle(
        ParticleOptions p_107371_, double p_107372_, double p_107373_, double p_107374_, double p_107375_, double p_107376_, double p_107377_
    ) {
        Particle particle = this.makeParticle(p_107371_, p_107372_, p_107373_, p_107374_, p_107375_, p_107376_, p_107377_);
        if (particle != null) {
            this.add(particle);
            return particle;
        } else {
            return null;
        }
    }

    private <T extends ParticleOptions>  Particle makeParticle(
        T p_107396_, double p_107397_, double p_107398_, double p_107399_, double p_107400_, double p_107401_, double p_107402_
    ) {
        ParticleProvider<T> particleprovider = (ParticleProvider<T>)this.resourceManager.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getId(p_107396_.getType()));
        if (Reflector.ForgeParticleResources_getProvider.exists()) {
            particleprovider = (ParticleProvider<T>)Reflector.call(this.resourceManager, Reflector.ForgeParticleResources_getProvider, p_107396_.getType());
        }

        return particleprovider == null
            ? null
            : particleprovider.createParticle(p_107396_, this.level, p_107397_, p_107398_, p_107399_, p_107400_, p_107401_, p_107402_, this.random);
    }

    public void add(Particle p_107345_) {
        if (p_107345_ != null) {
            if (!(p_107345_ instanceof FireworkParticles.SparkParticle) || Config.isFireworkParticles()) {
                Optional<ParticleLimit> optional = p_107345_.getParticleLimit();
                if (optional.isPresent()) {
                    if (this.hasSpaceInParticleLimit(optional.get())) {
                        this.particlesToAdd.add(p_107345_);
                        this.updateCount(optional.get(), 1);
                    }
                } else {
                    this.particlesToAdd.add(p_107345_);
                }
            }
        }
    }

    public void tick() {
        this.particles.forEach((typeIn, listIn) -> {
            Profiler.get().push(typeIn.name());
            listIn.tickParticles();
            Profiler.get().pop();
        });

        if (!this.trackingEmitters.isEmpty()) {
            List<TrackingEmitter> list = Lists.newArrayList();
            for (TrackingEmitter trackingemitter : this.trackingEmitters) {
                trackingemitter.tick();
                if (!trackingemitter.isAlive()) {
                    list.add(trackingemitter);
                }
            }
            this.trackingEmitters.removeAll(list);
        }

        Particle particle;
        if (!this.particlesToAdd.isEmpty()) {
            while ((particle = this.particlesToAdd.poll()) != null) {
                this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle);
            }
        }

        enforceParticleLimit();
    }

    private Particle[] spHeapParticles;
    private double[] spHeapScores;
    private Set<Particle> spKeep;

    private void enforceParticleLimit() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.instance.particleLimit);
        boolean smartCulling = SPConfig.instance.smartCameraCulling;

        var world = client.level;
        double protectionThresholdSq = (world != null && (world.isRaining() || world.isThundering())) ? 512.0 : 25.0;

        if (!smartCulling) {
            int total = 0;
            for (ParticleGroup<?> r : particles.values()) {
                total += r.size();
            }
            if (total <= limit) return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Vec3 camDir = Vec3.directionFromRotation(camera.xRot(), camera.yRot());

        double fov = client.options.fov().get();
        double frustumThreshold = Mth.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        // 1. LIMIT = 0 LOGIC
        if (limit == 0) {
            for (ParticleGroup<?> r : particles.values()) {
                Queue<? extends Particle> q = r.getAll();
                Iterator<? extends Particle> it = q.iterator();
                while (it.hasNext()) {
                    Particle p = (Particle) it.next();
                    it.remove();

                    // FIX: Null Check
                    if (p == null) continue;

                    p.remove();
                    decrementGroupCount(p);
                }
            }
            return;
        }

        if (this.spHeapParticles == null || this.spHeapParticles.length < limit) {
            this.spHeapParticles = new Particle[limit];
            this.spHeapScores = new double[limit];
            this.spKeep = Collections.newSetFromMap(new IdentityHashMap<>(limit));
        } else {
            this.spKeep.clear();
        }

        final Particle[] heapParticles = this.spHeapParticles;
        final double[] heapScores = this.spHeapScores;
        int heapSize = 0;

        boolean dirty = false;

        // 2. MAIN SCORING LOOP
        if (limit > 0) for (ParticleGroup<?> r : particles.values()) {
            Queue<? extends Particle> qView = r.getAll();
            @SuppressWarnings("unchecked")
            Queue<Particle> q = (Queue<Particle>) (Queue<?>) qView;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();

                // FIX: Null Check (Safely remove garbage if found)
                if (p == null) {
                    it.remove();
                    continue;
                }

                SPAccessor acc = p;

                double dx = acc.smartparticles$getX() - px;
                double dy = acc.smartparticles$getY() - py;
                double dz = acc.smartparticles$getZ() - pz;
                double distSq = dx * dx + dy * dy + dz * dz;

                boolean protectedParticle = distSq <= protectionThresholdSq;
                boolean inFrustum = false;

                if (!protectedParticle) {
                    double ex = acc.smartparticles$getX() - camPos.x;
                    double ey = acc.smartparticles$getY() - camPos.y;
                    double ez = acc.smartparticles$getZ() - camPos.z;

                    double dot = ex * camDir.x + ey * camDir.y + ez * camDir.z;

                    if (dot > 0) {
                        double eDistSq = ex * ex + ey * ey + ez * ez;
                        if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                            inFrustum = true;
                        }
                    }
                }

                if (smartCulling && !inFrustum && !protectedParticle) {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                    continue;
                }

                double score = distSq;
                if (!smartCulling && !inFrustum && !protectedParticle) {
                    score += frustumPenalty;
                }

                if (heapSize < limit) {
                    heapParticles[heapSize] = p;
                    heapScores[heapSize] = score;
                    heapSiftUp(heapParticles, heapScores, heapSize);
                    heapSize++;
                } else if (score < heapScores[0]) {
                    heapParticles[0] = p;
                    heapScores[0] = score;
                    heapSiftDown(heapParticles, heapScores, heapSize, 0);
                    dirty = true;
                } else {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
        }

        if (!dirty) return;

        Set<Particle> keep = this.spKeep;
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        // 3. FINAL CLEANUP LOOP
        for (ParticleGroup<?> r : particles.values()) {
            Queue<? extends Particle> qView = r.getAll();
            @SuppressWarnings("unchecked")
            Queue<Particle> q = (Queue<Particle>) (Queue<?>) qView;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();

                // FIX: Null Check
                if (p == null) {
                    it.remove();
                    continue;
                }

                if (!keep.contains(p)) {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
        }
    }
    private void decrementGroupCount(Particle p) {
        p.getParticleLimit().ifPresent(group -> {
            int current = trackedParticleCounts.getInt(group);
            if (current <= 1) {
                trackedParticleCounts.removeInt(group);
            } else {
                trackedParticleCounts.put(group, current - 1);
            }
        });
    }

    private static void heapSiftUp(Particle[] ps, double[] ds, int idx) {
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (ds[parent] >= ds[idx]) return;
            swap(ps, ds, parent, idx);
            idx = parent;
        }
    }

    private static void heapSiftDown(Particle[] ps, double[] ds, int size, int idx) {
        while (true) {
            int left = (idx << 1) + 1;
            if (left >= size) return;

            int right = left + 1;
            int largest = left;

            if (right < size && ds[right] > ds[left]) {
                largest = right;
            }

            if (ds[idx] >= ds[largest]) return;

            swap(ps, ds, idx, largest);
            idx = largest;
        }
    }

    private static void swap(Particle[] ps, double[] ds, int a, int b) {
        Particle tp = ps[a];
        ps[a] = ps[b];
        ps[b] = tp;

        double td = ds[a];
        ds[a] = ds[b];
        ds[b] = td;
    }

    private ParticleGroup<?> createParticleGroup(ParticleRenderType p_428647_) {
        if (p_428647_ == ParticleRenderType.ITEM_PICKUP) {
            return new ItemPickupParticleGroup(this);
        } else if (p_428647_ == ParticleRenderType.ELDER_GUARDIANS) {
            return new ElderGuardianParticleGroup(this);
        } else if (factories.containsKey(p_428647_)) {
            return factories.get(p_428647_).apply(this);
        } else {
            return p_428647_ == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, p_428647_);
        }
    }

    protected void updateCount(ParticleLimit p_423291_, int p_172283_) {
        this.trackedParticleCounts.addTo(p_423291_, p_172283_);
    }

    public void extract(ParticlesRenderState p_423938_, Frustum p_424803_, Camera p_430521_, float p_426823_) {
        for (ParticleRenderType particlerendertype : particleRenderOrder) {
            ParticleGroup<?> particlegroup = this.particles.get(particlerendertype);
            if (particlegroup != null && !particlegroup.isEmpty()) {
                p_423938_.add(particlegroup.extractRenderState(p_424803_, p_430521_, p_426823_));
            }
        }
    }

    public void setLevel( ClientLevel p_107343_) {
        this.level = p_107343_;
        this.clearParticles();
        this.trackingEmitters.clear();
    }

    public String countParticles() {
        return String.valueOf(this.particles.values().stream().mapToInt(ParticleGroup::size).sum());
    }

    private boolean hasSpaceInParticleLimit(ParticleLimit p_426844_) {
        return this.trackedParticleCounts.getInt(p_426844_) < p_426844_.limit();
    }

    public void clearParticles() {
        this.particles.clear();
        this.particlesToAdd.clear();
        this.trackingEmitters.clear();
        this.trackedParticleCounts.clear();
    }

    private boolean reuseBlockMarker(BlockMarker blockMarkerIn, Queue<Particle> particlesIn) {
        for (Particle particle : particlesIn) {
            if (particle instanceof BlockMarker blockmarker
                && blockmarker.xo == blockMarkerIn.xo
                && blockmarker.yo == blockMarkerIn.yo
                && blockmarker.zo == blockMarkerIn.zo
                && blockmarker.sprite == blockMarkerIn.sprite) {
                blockmarker.age = 0;
                return true;
            }
        }

        return false;
    }

    public static void updateTerrainParticleColor(Particle particle, BlockState state, BlockAndTintGetter world, BlockPos pos, RenderEnv renderEnv) {
        renderEnv.reset(state, pos);
        int i = CustomColors.getColorMultiplier(true, state, world, pos, renderEnv);
        if (i != -1 && particle instanceof SingleQuadParticle singlequadparticle) {
            singlequadparticle.rCol = 0.6F * (i >> 16 & 0xFF) / 255.0F;
            singlequadparticle.gCol = 0.6F * (i >> 8 & 0xFF) / 255.0F;
            singlequadparticle.bCol = 0.6F * (i & 0xFF) / 255.0F;
        }
    }

    public int getCountParticles() {
        int i = 0;

        for (ParticleGroup particlegroup : this.particles.values()) {
            i += particlegroup.size();
        }

        return i;
    }

    public static void registerParticleGroup(ParticleRenderType type, Function<ParticleEngine, ParticleGroup<?>> factory) {
        if (factories.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException(
                type.name() + " already has a factory registered. Previous factory was " + factories.get(type) + ". This factory was " + factory
            );
        }

        particleRenderOrder.add(type);
    }
}
