package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.util.StrUtils;
import org.slf4j.Logger;

public record SingleFile(Identifier resourceId, Optional<Identifier> spriteId) implements SpriteSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<SingleFile> MAP_CODEC = RecordCodecBuilder.mapCodec(
        file2In -> file2In.group(
                Identifier.CODEC.fieldOf("resource").forGetter(SingleFile::resourceId),
                Identifier.CODEC.optionalFieldOf("sprite").forGetter(SingleFile::spriteId)
            )
            .apply(file2In, SingleFile::new)
    );
    public static final String PREFIX_VIRTUAL = "_virtual_/";

    public SingleFile(Identifier p_460815_) {
        this(p_460815_, Optional.empty());
    }

    @Override
    public void run(ResourceManager p_261920_, SpriteSource.Output p_261578_) {
        Identifier identifier = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);
        if (TextureAtlas.isAbsoluteLocation(this.resourceId)) {
            identifier = new Identifier(this.resourceId.getNamespace(), this.resourceId.getPath() + ".png");
        }

        if (this.resourceId.getPath().startsWith("_virtual_/")) {
            identifier = new Identifier(this.resourceId.getNamespace(), "textures/" + StrUtils.removePrefix(this.resourceId.getPath(), "_virtual_/") + ".png");
        }

        Optional<Resource> optional = p_261920_.getResource(identifier);
        if (optional.isPresent()) {
            p_261578_.add(this.spriteId.orElse(this.resourceId), optional.get());
        } else {
            LOGGER.warn("Missing sprite: {}", identifier);
        }
    }

    @Override
    public MapCodec<SingleFile> codec() {
        return MAP_CODEC;
    }
}
