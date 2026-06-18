package com.viaversion.viafabricplus;

import com.viaversion.viafabricplus.injection.mixin.base.integration.MixinViaBedrockConfig;
import com.viaversion.viafabricplus.injection.mixin.base.integration.MixinViaLegacyConfig;
import com.viaversion.viafabricplus.injection.mixin.compat.classic4j.MixinCCAuthenticationResponse;
import lombok.experimental.UtilityClass;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.vialegacy.ViaLegacy;

@UtilityClass
public class ViaFixMixin {

    public void fixMixin() {
        var configbedrok = ViaBedrock.getConfig();
        if (configbedrok != null) {
            MixinViaBedrockConfig.sync((net.raphimc.viabedrock.ViaBedrockConfig) configbedrok);
        }
        var config = ViaLegacy.getConfig();
        if (config != null) {
            MixinViaLegacyConfig.sync(config);
        }
        try {
            MixinCCAuthenticationResponse.syncTranslations();
        } catch (Throwable ignored) {
        }
    }
}
