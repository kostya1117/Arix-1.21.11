package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.optifine.shaders.ShadersTextureType;
import net.optifine.texture.SpriteSourceCollector;
import net.optifine.util.StrUtils;
import org.slf4j.Logger;

public class SpriteSourceList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
    private final List<SpriteSource> sources;

    private SpriteSourceList(List<SpriteSource> p_297576_) {
        this.sources = p_297576_;
    }

    public List<SpriteSource.Loader> list(ResourceManager p_298985_) {
        final Map<Identifier, SpriteSource.DiscardableLoader> map = new HashMap<>();
        SpriteSource.Output spritesource$output = new SpriteSource.Output() {
            @Override
            public void add(Identifier p_455897_, SpriteSource.DiscardableLoader p_455073_) {
                SpriteSource.DiscardableLoader spritesource$discardableloader = map.put(p_455897_, p_455073_);
                if (spritesource$discardableloader != null) {
                    spritesource$discardableloader.discard();
                }
            }

            @Override
            public void removeAll(Predicate<Identifier> p_299726_) {
                Iterator<Entry<Identifier, SpriteSource.DiscardableLoader>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<Identifier, SpriteSource.DiscardableLoader> entry = iterator.next();
                    if (p_299726_.test(entry.getKey())) {
                        entry.getValue().discard();
                        iterator.remove();
                    }
                }
            }
        };
        this.sources.forEach(sourceIn -> sourceIn.run(p_298985_, spritesource$output));
        this.filterSpriteNames(map.keySet());
        Builder<SpriteSource.Loader> builder = ImmutableList.builder();
        builder.add(loaderIn -> MissingTextureAtlasSprite.create());
        builder.addAll(map.values());
        return builder.build();
    }

    public static SpriteSourceList load(ResourceManager p_300689_, Identifier p_451225_) {
        Identifier identifier = ATLAS_INFO_CONVERTER.idToFile(p_451225_);
        List<SpriteSource> list = new ArrayList<>();

        for (Resource resource : p_300689_.getResourceStack(identifier)) {
            try (BufferedReader bufferedreader = resource.openAsReader()) {
                Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedreader));
                list.addAll(SpriteSources.FILE_CODEC.parse(dynamic).getOrThrow());
            } catch (Exception exception) {
                LOGGER.error("Failed to parse atlas definition {} in pack {}", identifier, resource.sourcePackId(), exception);
            }
        }

        return new SpriteSourceList(list);
    }

    public void addSpriteSources(Collection<Identifier> spriteNames) {
        for (Identifier identifier : spriteNames) {
            this.sources.add(new SingleFile(identifier, Optional.empty()));
        }
    }

    public List<SpriteSource> getSpriteSources() {
        return this.sources;
    }

    public Set<Identifier> getSpriteNames(ResourceManager resourceManager) {
        Set<Identifier> set = new LinkedHashSet<>();

        for (SpriteSource spritesource : this.sources) {
            SpriteSource.Output spritesource$output = new SpriteSourceCollector(set);
            spritesource.run(resourceManager, spritesource$output);
        }

        return set;
    }

    public void filterSpriteNames(Set<Identifier> spriteNames) {
        String s = ShadersTextureType.NORMAL.getSuffix();
        String s1 = ShadersTextureType.SPECULAR.getSuffix();
        String[] astring = new String[]{s, s1};
        Iterator iterator = spriteNames.iterator();

        while (iterator.hasNext()) {
            Identifier identifier = (Identifier)iterator.next();
            String s2 = identifier.getPath();
            if (s2.endsWith(s) || s2.endsWith(s1)) {
                String s3 = StrUtils.removeSuffix(s2, astring);
                Identifier identifier1 = new Identifier(identifier.getNamespace(), s3);
                if (spriteNames.contains(identifier1)) {
                    iterator.remove();
                }
            }
        }
    }
}
