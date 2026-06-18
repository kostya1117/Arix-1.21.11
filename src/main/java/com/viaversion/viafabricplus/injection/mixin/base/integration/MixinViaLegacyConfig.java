/*
 * This file is part of ViaFabricPlus - https://github.com/ViaVersion/ViaFabricPlus
 * Copyright (C) 2021-2026 the original authors
 *                         - Florian Reuth <git@florianreuth.de>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viafabricplus.injection.mixin.base.integration;

import com.viaversion.viafabricplus.settings.impl.GeneralSettings;

import java.lang.reflect.Field;

public final class MixinViaLegacyConfig {

    private static final Field FIELD_LEGACY_SKULL_LOADING;
    private static final Field FIELD_LEGACY_SKIN_LOADING;

    static {
        try {
            Class<?> configClass = Class.forName("net.raphimc.vialegacy.ViaLegacyConfig");
            FIELD_LEGACY_SKULL_LOADING = configClass.getDeclaredField("legacySkullLoading");
            FIELD_LEGACY_SKULL_LOADING.setAccessible(true);
            FIELD_LEGACY_SKIN_LOADING = configClass.getDeclaredField("legacySkinLoading");
            FIELD_LEGACY_SKIN_LOADING.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access ViaLegacyConfig fields", e);
        }
    }

    private MixinViaLegacyConfig() {
    }

    public static void sync(Object config) {
        try {
            boolean value = GeneralSettings.INSTANCE.loadSkinsAndSkullsInLegacyVersions.getValue();
            FIELD_LEGACY_SKULL_LOADING.setBoolean(config, value);
            FIELD_LEGACY_SKIN_LOADING.setBoolean(config, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
