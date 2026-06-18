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

import com.viaversion.viafabricplus.settings.impl.BedrockSettings;
import net.raphimc.viabedrock.ViaBedrockConfig;

import java.lang.reflect.Field;

public final class MixinViaBedrockConfig {

    private static final Field FIELD_ENABLE_EXPERIMENTAL_FEATURES;

    static {
        try {
            FIELD_ENABLE_EXPERIMENTAL_FEATURES = ViaBedrockConfig.class.getDeclaredField("enableExperimentalFeatures");
            FIELD_ENABLE_EXPERIMENTAL_FEATURES.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access ViaBedrockConfig.enableExperimentalFeatures", e);
        }
    }

    private MixinViaBedrockConfig() {
    }

    public static void sync(ViaBedrockConfig config) {
        try {
            FIELD_ENABLE_EXPERIMENTAL_FEATURES.setBoolean(
                    config,
                    BedrockSettings.INSTANCE.experimentalFeatures.getValue()
            );
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
