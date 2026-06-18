package com.viaversion.viafabricplus.injection.mixin.compat.classic4j;

import de.florianreuth.classic4j.model.classicube.CCError;
import net.minecraft.network.chat.Component;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class MixinCCAuthenticationResponse {

    private static final Unsafe UNSAFE;
    private static final long DESCRIPTION_OFFSET;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);

            Field descriptionField = CCError.class.getDeclaredField("description");
            DESCRIPTION_OFFSET = UNSAFE.objectFieldOffset(descriptionField);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access CCError.description", e);
        }
    }

    private MixinCCAuthenticationResponse() {
    }

    public static void syncTranslations() {
        set(CCError.TOKEN, Component.translatable("classic4j_library.viafabricplus.error.token").getString());
        set(CCError.USERNAME, Component.translatable("classic4j_library.viafabricplus.error.username").getString());
        set(CCError.PASSWORD, Component.translatable("classic4j_library.viafabricplus.error.password").getString());
        set(CCError.VERIFICATION, Component.translatable("classic4j_library.viafabricplus.error.verification").getString());
        set(CCError.LOGIN_CODE, Component.translatable("classic4j_library.viafabricplus.error.logincode").getString());
    }

    private static void set(CCError error, String value) {
        UNSAFE.putObject(error, DESCRIPTION_OFFSET, value);
    }
}