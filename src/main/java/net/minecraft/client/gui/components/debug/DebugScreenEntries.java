package net.minecraft.client.gui.components.debug;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.optifine.gui.DebugEntryOF;
import org.jspecify.annotations.Nullable;

public class DebugScreenEntries {
    private static final Map<Identifier, DebugScreenEntry> ENTRIES_BY_ID = new LinkedHashMap<>();
    public static final Identifier GAME_VERSION = register("game_version", new DebugEntryVersion());
    public static final Identifier FPS = register("fps", new DebugEntryFps());
    public static final Identifier TPS = register("tps", new DebugEntryTps());
    public static final Identifier MEMORY = register("memory", new DebugEntryMemory());
    public static final Identifier SYSTEM_SPECS = register("system_specs", new DebugEntrySystemSpecs());
    public static final Identifier LOOKING_AT_BLOCK = register("looking_at_block", new DebugEntryLookingAtBlock());
    public static final Identifier LOOKING_AT_FLUID = register("looking_at_fluid", new DebugEntryLookingAtFluid());
    public static final Identifier LOOKING_AT_ENTITY = register("looking_at_entity", new DebugEntryLookingAtEntity());
    public static final Identifier CHUNK_RENDER_STATS = register("chunk_render_stats", new DebugEntryChunkRenderStats());
    public static final Identifier CHUNK_GENERATION_STATS = register("chunk_generation_stats", new DebugEntryChunkGeneration());
    public static final Identifier ENTITY_RENDER_STATS = register("entity_render_stats", new DebugEntryEntityRenderStats());
    public static final Identifier PARTICLE_RENDER_STATS = register("particle_render_stats", new DebugEntryParticleRenderStats());
    public static final Identifier CHUNK_SOURCE_STATS = register("chunk_source_stats", new DebugEntryChunkSourceStats());
    public static final Identifier PLAYER_POSITION = register("player_position", new DebugEntryPosition());
    public static final Identifier PLAYER_SECTION_POSITION = register("player_section_position", new DebugEntrySectionPosition());
    public static final Identifier LIGHT_LEVELS = register("light_levels", new DebugEntryLight());
    public static final Identifier HEIGHTMAP = register("heightmap", new DebugEntryHeightmap());
    public static final Identifier BIOME = register("biome", new DebugEntryBiome());
    public static final Identifier LOCAL_DIFFICULTY = register("local_difficulty", new DebugEntryLocalDifficulty());
    public static final Identifier ENTITY_SPAWN_COUNTS = register("entity_spawn_counts", new DebugEntrySpawnCounts());
    public static final Identifier SOUND_MOOD = register("sound_mood", new DebugEntrySoundMood());
    public static final Identifier POST_EFFECT = register("post_effect", new DebugEntryPostEffect());
    public static final Identifier ENTITY_HITBOXES = register("entity_hitboxes", new DebugEntryNoop());
    public static final Identifier CHUNK_BORDERS = register("chunk_borders", new DebugEntryNoop());
    public static final Identifier THREE_DIMENSIONAL_CROSSHAIR = register("3d_crosshair", new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_PATHS = register("chunk_section_paths", new DebugEntryNoop());
    public static final Identifier GPU_UTILIZATION = register("gpu_utilization", new DebugEntryGpuUtilization());
    public static final Identifier SIMPLE_PERFORMANCE_IMPACTORS = register("simple_performance_impactors", new DebugEntrySimplePerformanceImpactors());
    public static final Identifier CHUNK_SECTION_OCTREE = register("chunk_section_octree", new DebugEntryNoop());
    public static final Identifier VISUALIZE_WATER_LEVELS = register("visualize_water_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_HEIGHTMAP = register("visualize_heightmap", new DebugEntryNoop());
    public static final Identifier VISUALIZE_COLLISION_BOXES = register("visualize_collision_boxes", new DebugEntryNoop());
    public static final Identifier VISUALIZE_ENTITY_SUPPORTING_BLOCKS = register("visualize_entity_supporting_blocks", new DebugEntryNoop());
    public static final Identifier VISUALIZE_BLOCK_LIGHT_LEVELS = register("visualize_block_light_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_LEVELS = register("visualize_sky_light_levels", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SOLID_FACES = register("visualize_solid_faces", new DebugEntryNoop());
    public static final Identifier VISUALIZE_CHUNKS_ON_SERVER = register("visualize_chunks_on_server", new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_SECTIONS = register("visualize_sky_light_sections", new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_VISIBILITY = register("chunk_section_visibility", new DebugEntryNoop());
    public static final Identifier OPTIFINE = register("optifine", new DebugEntryOF());
    public static final Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES;
    private static Set<Identifier> locationsLeft = ImmutableSet.of(
        GAME_VERSION,
        FPS,
        SIMPLE_PERFORMANCE_IMPACTORS,
        GPU_UTILIZATION,
        TPS,
        CHUNK_RENDER_STATS,
        ENTITY_RENDER_STATS,
        PARTICLE_RENDER_STATS,
        CHUNK_SOURCE_STATS,
        OPTIFINE,
        PLAYER_POSITION,
        PLAYER_SECTION_POSITION,
        LIGHT_LEVELS,
        HEIGHTMAP,
        BIOME,
        LOCAL_DIFFICULTY,
        CHUNK_GENERATION_STATS,
        ENTITY_SPAWN_COUNTS,
        SOUND_MOOD,
        POST_EFFECT
    );
    private static Set<Identifier> locationsRight = ImmutableSet.of(MEMORY, SYSTEM_SPECS, LOOKING_AT_BLOCK, LOOKING_AT_FLUID, LOOKING_AT_ENTITY);
    private static Set<Identifier> locationsFps = ImmutableSet.of(FPS, SIMPLE_PERFORMANCE_IMPACTORS, GPU_UTILIZATION);
    private static Set<Identifier> locationsNewLine = ImmutableSet.of(PLAYER_POSITION, SYSTEM_SPECS, LOOKING_AT_BLOCK, LOOKING_AT_FLUID, LOOKING_AT_ENTITY);
    private static Map<Identifier, Integer> locationsIndex = new HashMap<>();

    private static Identifier register(String p_458601_, DebugScreenEntry p_424237_) {
        return register(Identifier.withDefaultNamespace(p_458601_), p_424237_);
    }

    public static Identifier register(Identifier p_455443_, DebugScreenEntry p_424183_) {
        ENTRIES_BY_ID.put(p_455443_, p_424183_);
        return p_455443_;
    }

    public static Map<Identifier, DebugScreenEntry> allEntries() {
        return Map.copyOf(ENTRIES_BY_ID);
    }

    public static  DebugScreenEntry getEntry(Identifier p_452419_) {
        return ENTRIES_BY_ID.get(p_452419_);
    }

    public static boolean isLineLeft(Identifier locationIn) {
        return locationsLeft.contains(locationIn);
    }

    public static boolean isFpsLine(Identifier locationIn) {
        return locationsFps.contains(locationIn);
    }

    public static boolean isNewLine(Identifier locationIn) {
        return locationsNewLine.contains(locationIn);
    }

    public static int getIndex(Identifier loc) {
        Integer integer = locationsIndex.get(loc);
        return integer == null ? -1 : integer;
    }

    public static int compareIndex(Identifier loc1, Identifier loc2) {
        return getIndex(loc1) - getIndex(loc2);
    }

    static {
        Map<Identifier, DebugScreenEntry> map = new LinkedHashMap<>(ENTRIES_BY_ID);
        ENTRIES_BY_ID.clear();

        for (Identifier identifier : locationsLeft) {
            ENTRIES_BY_ID.put(identifier, map.remove(identifier));
        }

        for (Identifier identifier2 : locationsRight) {
            ENTRIES_BY_ID.put(identifier2, map.remove(identifier2));
        }

        ENTRIES_BY_ID.putAll(map);
        Identifier[] aidentifier = ENTRIES_BY_ID.keySet().toArray(new Identifier[0]);

        for (int i = 0; i < aidentifier.length; i++) {
            Identifier identifier1 = aidentifier[i];
            locationsIndex.put(identifier1, i);
        }

        Map<Identifier, DebugScreenEntryStatus> map1 = Map.of(
            OPTIFINE,
            DebugScreenEntryStatus.IN_OVERLAY,
            THREE_DIMENSIONAL_CROSSHAIR,
            DebugScreenEntryStatus.IN_OVERLAY,
            GAME_VERSION,
            DebugScreenEntryStatus.IN_OVERLAY,
            TPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            FPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            MEMORY,
            DebugScreenEntryStatus.IN_OVERLAY,
            SYSTEM_SPECS,
            DebugScreenEntryStatus.IN_OVERLAY,
            PLAYER_POSITION,
            DebugScreenEntryStatus.IN_OVERLAY,
            PLAYER_SECTION_POSITION,
            DebugScreenEntryStatus.IN_OVERLAY,
            SIMPLE_PERFORMANCE_IMPACTORS,
            DebugScreenEntryStatus.IN_OVERLAY
        );
        Map<Identifier, DebugScreenEntryStatus> map2 = Map.of(
            OPTIFINE,
            DebugScreenEntryStatus.IN_OVERLAY,
            TPS,
            DebugScreenEntryStatus.IN_OVERLAY,
            FPS,
            DebugScreenEntryStatus.ALWAYS_ON,
            GPU_UTILIZATION,
            DebugScreenEntryStatus.IN_OVERLAY,
            MEMORY,
            DebugScreenEntryStatus.IN_OVERLAY,
            SIMPLE_PERFORMANCE_IMPACTORS,
            DebugScreenEntryStatus.IN_OVERLAY
        );
        PROFILES = Map.of(DebugScreenProfile.DEFAULT, map1, DebugScreenProfile.PERFORMANCE, map2);
    }
}
