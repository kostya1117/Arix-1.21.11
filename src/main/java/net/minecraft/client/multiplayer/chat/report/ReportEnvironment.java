package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public record ReportEnvironment(String clientVersion, ReportEnvironment.@Nullable Server server) {
    public static ReportEnvironment local() {
        return create(null);
    }

    public static ReportEnvironment thirdParty(String p_238999_) {
        return create(new ReportEnvironment.Server.ThirdParty(p_238999_));
    }

    public static ReportEnvironment create(ReportEnvironment.@Nullable Server p_239956_) {
        return new ReportEnvironment(getClientVersion(), p_239956_);
    }

    public ClientInfo clientInfo() {
        return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
    }

    public @Nullable ThirdPartyServerInfo thirdPartyServerInfo() {
        return this.server instanceof ReportEnvironment.Server.ThirdParty reportenvironment$server$thirdparty
            ? new ThirdPartyServerInfo(reportenvironment$server$thirdparty.ip)
            : null;
    }

    public @Nullable RealmInfo realmInfo() {
        return new RealmInfo("0", 0);
    }

    private static String getClientVersion() {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append(SharedConstants.getCurrentVersion().id());
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            stringbuilder.append(" (modded)");
        }

        return stringbuilder.toString();
    }

    
    public interface Server {
        record ThirdParty(String ip) implements ReportEnvironment.Server {
        }
    }
}
