package com.viaversion.viafabricplus.injection.mixin.base.access;

import com.viaversion.viafabricplus.injection.access.base.IExtensionProtocolMetadataStorage;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.data.ClassicProtocolExtension;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.storage.ExtensionProtocolMetadataStorage;

import java.lang.reflect.Field;
import java.util.EnumMap;

public class MixinExtensionProtocolMetadataStorage implements IExtensionProtocolMetadataStorage {

    private static final Field FIELD_SERVER_EXTENSIONS;

    static {
        try {
            FIELD_SERVER_EXTENSIONS = ExtensionProtocolMetadataStorage.class.getDeclaredField("serverExtensions");
            FIELD_SERVER_EXTENSIONS.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access ExtensionProtocolMetadataStorage.serverExtensions", e);
        }
    }

    private final ExtensionProtocolMetadataStorage storage;

    public MixinExtensionProtocolMetadataStorage(ExtensionProtocolMetadataStorage storage) {
        this.storage = storage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EnumMap<ClassicProtocolExtension, Integer> viaFabricPlus$getServerExtensions() {
        try {
            return (EnumMap<ClassicProtocolExtension, Integer>) FIELD_SERVER_EXTENSIONS.get(this.storage);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}