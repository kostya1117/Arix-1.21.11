package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.util.StrUtils;

public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String p_248876_, String p_251478_) {
        this.prefix = p_248876_;
        this.extension = p_251478_;
    }

    public static FileToIdConverter json(String p_248754_) {
        return new FileToIdConverter(p_248754_, ".json");
    }

    public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> p_375453_) {
        return json(Registries.elementsDirPath(p_375453_));
    }

    public Identifier idToFile(Identifier p_452775_) {
        return p_452775_.withPath(this.prefix + "/" + p_452775_.getPath() + this.extension);
    }

    public Identifier fileToId(Identifier p_450329_) {
        if (!p_450329_.getPath().startsWith(this.prefix)) {
            return p_450329_.withPath(StrUtils.removeSuffix(p_450329_.getPath(), this.extension));
        }

        String s = p_450329_.getPath();
        return p_450329_.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
    }

    public Map<Identifier, Resource> listMatchingResources(ResourceManager p_252045_) {
        return p_252045_.listResources(this.prefix, locIn -> locIn.getPath().endsWith(this.extension));
    }

    public Map<Identifier, List<Resource>> listMatchingResourceStacks(ResourceManager p_249881_) {
        return p_249881_.listResourceStacks(this.prefix, locIn -> locIn.getPath().endsWith(this.extension));
    }

    public boolean matches(Identifier loc) {
        String s = loc.getPath();
        return s.startsWith(this.prefix) && s.endsWith(this.extension);
    }
}
