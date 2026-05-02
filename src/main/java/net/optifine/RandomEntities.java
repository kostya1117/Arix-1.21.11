package net.optifine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.optifine.reflect.ReflectorRaw;
import net.optifine.render.RenderState;
import net.optifine.util.ArrayUtils;
import net.optifine.util.ResUtils;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;

public class RandomEntities {
    private static Map<String, RandomEntityProperties<Identifier>> mapProperties = new HashMap<>();
    private static Map<String, RandomEntityProperties<Identifier>> mapSpriteProperties = new HashMap<>();
    private static boolean active = false;
    private static RandomEntity randomEntity = new RandomEntity();
    private static RandomTileEntity randomTileEntity = new RandomTileEntity();
    private static boolean working = false;
    public static final String SUFFIX_PNG = ".png";
    public static final String SUFFIX_PROPERTIES = ".properties";
    public static final String SEPARATOR_DIGITS = ".";
    public static final String PREFIX_TEXTURES_ENTITY = "textures/entity/";
    public static final String PREFIX_TEXTURES_PAINTING = "textures/painting/";
    public static final String PREFIX_TEXTURES = "textures/";
    public static final String PREFIX_OPTIFINE_RANDOM = "optifine/random/";
    public static final String PREFIX_OPTIFINE = "optifine/";
    public static final String PREFIX_OPTIFINE_MOB = "optifine/mob/";
    private static final String[] DEPENDANT_SUFFIXES = new String[]{
        "_armor", "_eyes", "_exploding", "_shooting", "_fur", "_eyes", "_invulnerable", "_angry", "_tame", "_collar"
    };
    private static final String PREFIX_DYNAMIC_TEXTURE_HORSE = "horse/";
    private static final String[] HORSE_TEXTURES = (String[])ReflectorRaw.getFieldValue(null, Horse.class, String[].class, 0);
    private static final String[] HORSE_TEXTURES_ABBR = (String[])ReflectorRaw.getFieldValue(null, Horse.class, String[].class, 1);

    public static void entityLoaded(Entity entity, Level world) {
        if (world != null) {
            SynchedEntityData synchedentitydata = entity.getEntityData();
            synchedentitydata.spawnPosition = entity.blockPosition();
            synchedentitydata.spawnBiome = world.getBiome(synchedentitydata.spawnPosition).value();
            if (entity instanceof ShoulderRidingEntity shoulderridingentity) {
                checkEntityShoulder(shoulderridingentity, false);
            }
        }
    }

    public static void entityUnloaded(Entity entity, Level world) {
        if (entity instanceof ShoulderRidingEntity shoulderridingentity) {
            checkEntityShoulder(shoulderridingentity, true);
        }
    }

    public static void checkEntityShoulder(ShoulderRidingEntity entity, boolean attach) {
        LivingEntity livingentity = entity.getOwner();
        if (livingentity == null) {
            livingentity = Config.getMinecraft().player;
        }

        if (livingentity instanceof AbstractClientPlayer abstractclientplayer) {
            UUID uuid = entity.getUUID();
            if (attach) {
                abstractclientplayer.lastAttachedEntity = entity;
            } else {
                SynchedEntityData synchedentitydata = entity.getEntityData();
                if (abstractclientplayer.entityShoulderLeft != null && Config.equals(abstractclientplayer.entityShoulderLeft.getUUID(), uuid)) {
                    SynchedEntityData synchedentitydata1 = abstractclientplayer.entityShoulderLeft.getEntityData();
                    synchedentitydata.spawnPosition = synchedentitydata1.spawnPosition;
                    synchedentitydata.spawnBiome = synchedentitydata1.spawnBiome;
                    abstractclientplayer.entityShoulderLeft = null;
                }

                if (abstractclientplayer.entityShoulderRight != null && Config.equals(abstractclientplayer.entityShoulderRight.getUUID(), uuid)) {
                    SynchedEntityData synchedentitydata2 = abstractclientplayer.entityShoulderRight.getEntityData();
                    synchedentitydata.spawnPosition = synchedentitydata2.spawnPosition;
                    synchedentitydata.spawnBiome = synchedentitydata2.spawnBiome;
                    abstractclientplayer.entityShoulderRight = null;
                }
            }
        }
    }

    public static void worldChanged(Level oldWorld, Level newWorld) {
        if (newWorld instanceof ClientLevel clientlevel) {
            for (Entity entity : clientlevel.entitiesForRendering()) {
                entityLoaded(entity, newWorld);
            }
        }

        randomEntity.setEntity(null);
        randomTileEntity.setTileEntity(null);
    }

    public static Identifier getTextureLocation(Identifier loc) {
        if (!active) {
            return loc;
        }

        IRandomEntity irandomentity = getRandomEntityRendered();
        if (irandomentity == null) {
            return loc;
        }

        if (working) {
            return loc;
        }

        try {
            working = true;
            String s = loc.getPath();
            if (s.startsWith("horse/")) {
                s = getHorseTexturePath(s, "horse/".length());
            }

            if (!s.startsWith("textures/entity/") && !s.startsWith("textures/painting/")) {
                return loc;
            }

            RandomEntityProperties<Identifier> randomentityproperties = mapProperties.get(s);
            return randomentityproperties == null ? loc : randomentityproperties.getResource(irandomentity, loc);
        } finally {
            working = false;
        }
    }

    private static String getHorseTexturePath(String path, int pos) {
        if (HORSE_TEXTURES != null && HORSE_TEXTURES_ABBR != null) {
            for (int i = 0; i < HORSE_TEXTURES_ABBR.length; i++) {
                String s = HORSE_TEXTURES_ABBR[i];
                if (path.startsWith(s, pos)) {
                    return HORSE_TEXTURES[i];
                }
            }

            return path;
        } else {
            return path;
        }
    }

    public static IRandomEntity getRandomEntityRendered() {
        if (RenderState.getEntity() != null) {
            randomEntity.setEntity(RenderState.getEntity());
            return randomEntity;
        }

        if (RenderState.getBlockEntity() != null) {
            BlockEntity blockentity = RenderState.getBlockEntity();
            if (blockentity.getLevel() != null) {
                randomTileEntity.setTileEntity(blockentity);
                return randomTileEntity;
            }
        }

        return null;
    }

    public static IRandomEntity getRandomEntity(Entity entityIn) {
        randomEntity.setEntity(entityIn);
        return randomEntity;
    }

    public static IRandomEntity getRandomBlockEntity(BlockEntity tileEntityIn) {
        randomTileEntity.setTileEntity(tileEntityIn);
        return randomTileEntity;
    }

    private static RandomEntityProperties<Identifier> makeProperties(Identifier loc, RandomEntityContext.Textures context) {
        String s = loc.getPath();
        Identifier identifier = getLocationProperties(loc, context.isLegacy());
        if (identifier != null) {
            RandomEntityProperties randomentityproperties = RandomEntityProperties.parse(identifier, loc, context);
            if (randomentityproperties != null) {
                return randomentityproperties;
            }
        }

        int[] aint = getLocationsVariants(loc, context.isLegacy(), context);
        if (aint == null) {
            return null;
        }

        String s1 = context.getConnectedParser().parseName(loc.getPath());
        return new RandomEntityProperties<>(s1, loc, aint, context);
    }

    private static Identifier getLocationProperties(Identifier loc, boolean legacy) {
        Identifier identifier = getLocationRandom(loc, legacy);
        if (identifier == null) {
            return null;
        }

        String s = identifier.getNamespace();
        String s1 = identifier.getPath();
        String s2 = StrUtils.removeSuffix(s1, ".png");
        String s3 = s2 + ".properties";
        Identifier identifier1 = new Identifier(s, s3);
        if (Config.hasResource(identifier1)) {
            return identifier1;
        }

        String s4 = getParentTexturePath(s2);
        if (s4 == null) {
            return null;
        }

        Identifier identifier2 = new Identifier(s, s4 + ".properties");
        return Config.hasResource(identifier2) ? identifier2 : null;
    }

    protected static Identifier getLocationRandom(Identifier loc, boolean legacy) {
        String s = loc.getNamespace();
        String s1 = loc.getPath();
        if (s1.startsWith("optifine/")) {
            return loc;
        }

        String s2 = "textures/";
        String s3 = "optifine/random/";
        if (legacy) {
            s2 = "textures/entity/";
            s3 = "optifine/mob/";
        }

        if (!s1.startsWith(s2)) {
            return null;
        }

        String s4 = StrUtils.replacePrefix(s1, s2, s3);
        return new Identifier(s, s4);
    }

    private static String getPathBase(String pathRandom) {
        if (pathRandom.startsWith("optifine/random/")) {
            return StrUtils.replacePrefix(pathRandom, "optifine/random/", "textures/");
        } else {
            return pathRandom.startsWith("optifine/mob/") ? StrUtils.replacePrefix(pathRandom, "optifine/mob/", "textures/entity/") : null;
        }
    }

    protected static Identifier getLocationIndexed(Identifier loc, int index) {
        if (loc == null) {
            return null;
        }

        String s = loc.getPath();
        int i = s.lastIndexOf(46);
        if (i < 0) {
            return null;
        }

        String s1 = s.substring(0, i);
        String s2 = s.substring(i);
        String s3 = StrUtils.endsWithDigit(s1) ? "." : "";
        String s4 = s1 + s3 + index + s2;
        return new Identifier(loc.getNamespace(), s4);
    }

    private static String getParentTexturePath(String path) {
        for (int i = 0; i < DEPENDANT_SUFFIXES.length; i++) {
            String s = DEPENDANT_SUFFIXES[i];
            if (path.endsWith(s)) {
                return StrUtils.removeSuffix(path, s);
            }
        }

        return null;
    }

    public static int[] getLocationsVariants(Identifier loc, boolean legacy, RandomEntityContext context) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        Identifier identifier = getLocationRandom(loc, legacy);
        if (identifier == null) {
            return null;
        }

        for (int i = 1; i < list.size() + 10; i++) {
            int j = i + 1;
            Identifier identifier1 = getLocationIndexed(identifier, j);
            if (Config.hasResource(identifier1)) {
                list.add(j);
            }
        }

        if (list.size() <= 1) {
            return null;
        }

        Integer[] ainteger = list.toArray(new Integer[list.size()]);
        int[] aint = ArrayUtils.toPrimitive(ainteger);
        Config.dbg(context.getName() + ": " + loc.getPath() + ", variants: " + aint.length);
        return aint;
    }

    public static void update() {
        mapProperties.clear();
        mapSpriteProperties.clear();
        active = false;
        if (Config.isRandomEntities()) {
            initialize();
        }
    }

    private static void initialize() {
        String[] astring = new String[]{"optifine/random/", "optifine/mob/"};
        String[] astring1 = new String[]{".png", ".properties"};
        String[] astring2 = ResUtils.collectFiles(astring, astring1);
        Set set = new HashSet();

        for (int i = 0; i < astring2.length; i++) {
            String s = astring2[i];
            s = StrUtils.removeSuffix(s, astring1);
            s = StrUtils.trimTrailing(s, "0123456789");
            s = StrUtils.removeSuffix(s, ".");
            s = s + ".png";
            String s1 = getPathBase(s);
            if (!set.contains(s1)) {
                set.add(s1);
                Identifier identifier = new Identifier(s1);
                if (Config.hasResource(identifier)) {
                    RandomEntityProperties<Identifier> randomentityproperties = mapProperties.get(s1);
                    if (randomentityproperties == null) {
                        randomentityproperties = makeProperties(identifier, new RandomEntityContext.Textures(false));
                        if (randomentityproperties == null) {
                            randomentityproperties = makeProperties(identifier, new RandomEntityContext.Textures(true));
                        }

                        if (randomentityproperties != null) {
                            mapProperties.put(s1, randomentityproperties);
                        }
                    }
                }
            }
        }

        active = !mapProperties.isEmpty();
    }

    public static synchronized void registerSprites(Identifier atlasLocation, Set<Identifier> spriteLocations) {
        if (!mapProperties.isEmpty()) {
            String s = getTexturePrefix(atlasLocation);
            Set<Identifier> set = new HashSet<>();

            for (Identifier identifier : spriteLocations) {
                String s1 = "textures/" + s + identifier.getPath() + ".png";
                RandomEntityProperties<Identifier> randomentityproperties = mapProperties.get(s1);
                if (randomentityproperties != null) {
                    mapSpriteProperties.put(identifier.getPath(), randomentityproperties);
                    List<Identifier> list = randomentityproperties.getAllResources();
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            Identifier identifier1 = list.get(i);
                            Identifier identifier2 = TextureUtils.getSpriteLocation(identifier1);
                            set.add(identifier2);
                            mapSpriteProperties.put(identifier2.getPath(), randomentityproperties);
                        }
                    }
                }
            }

            spriteLocations.addAll(set);
        }
    }

    private static String getTexturePrefix(Identifier atlasLocation) {
        return atlasLocation.getPath().endsWith("/paintings.png") ? "painting/" : "";
    }

    public static TextureAtlasSprite getRandomSprite(TextureAtlasSprite spriteIn) {
        if (!active) {
            return spriteIn;
        }

        IRandomEntity irandomentity = getRandomEntityRendered();
        if (irandomentity == null) {
            return spriteIn;
        }

        if (working) {
            return spriteIn;
        }

        try {
            working = true;
            Identifier identifier = spriteIn.getName();
            String s = identifier.getPath();
            RandomEntityProperties<Identifier> randomentityproperties = mapSpriteProperties.get(s);
            if (randomentityproperties == null) {
                return spriteIn;
            }

            Identifier identifier1 = randomentityproperties.getResource(irandomentity, identifier);
            if (identifier1 == identifier) {
                return spriteIn;
            }

            Identifier identifier2 = TextureUtils.getSpriteLocation(identifier1);
            return spriteIn.getTextureAtlas().getSprite(identifier2);
        } finally {
            working = false;
        }
    }

    public static void dbg(String str) {
        Config.dbg("RandomEntities: " + str);
    }

    public static void warn(String str) {
        Config.warn("RandomEntities: " + str);
    }
}
