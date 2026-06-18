package com.viaversion.viafabricplus.protocoltranslator.impl.platform;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusConfig;
import com.viaversion.viafabricplus.protocoltranslator.protocol.ViaFabricPlusProtocol;
import com.viaversion.viafabricplus.protocoltranslator.util.JLoggerToSLF4J;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import com.viaversion.viaversion.util.GsonUtil;
import java.io.File;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

public final class ViaFabricPlusViaVersionPlatform extends UserConnectionViaVersionPlatform {

    private static final String MOD_ID = "viafabricplus";
    private static final String MOD_NAME = "ViaFabricPlus";

    public ViaFabricPlusViaVersionPlatform(File dataFolder) {
        super(dataFolder);
    }

    @Override
    public Logger createLogger(final String name) {
        return new JLoggerToSLF4J(LoggerFactory.getLogger(name));
    }

    @Override
    public String getPlatformName() {
        return "ViaFabricPlus";
    }

    @Override
    public String getPlatformVersion() {
        return ViaFabricPlusImpl.INSTANCE.getVersion();
    }

    @Override
    protected AbstractViaConfig createConfig() {
        return new ViaFabricPlusConfig(new File(getDataFolder(), "viaversion.yml"), this.getLogger());
    }

    @Override
    public void sendCustomPayload(UserConnection connection, String channel, byte[] message) {
        final PacketWrapper customPayload = PacketWrapper.create(ViaFabricPlusProtocol.INSTANCE.getCustomPayloadPacketType(), connection);
        customPayload.write(Types.STRING, channel);
        customPayload.write(Types.REMAINING_BYTES, message);
        customPayload.scheduleSendToServer(ViaFabricPlusProtocol.class);
    }

    @Override
    public void sendCustomPayloadToClient(final UserConnection connection, final String channel, final byte[] message) {
        final PacketWrapper customPayload = PacketWrapper.create(ViaFabricPlusProtocol.INSTANCE.getClientboundCustomPayloadPacketType(), connection);
        customPayload.write(Types.STRING, channel);
        customPayload.write(Types.REMAINING_BYTES, message);
        customPayload.scheduleSend(ViaFabricPlusProtocol.class);
    }

    @Override
    public JsonObject getDump() {
        final JsonObject platformDump = new JsonObject();
        platformDump.addProperty("impl_version", ViaFabricPlusImpl.INSTANCE.getImplVersion());
        platformDump.addProperty("native_version", ProtocolTranslator.NATIVE_VERSION.toString());
        platformDump.addProperty("target_version", ProtocolTranslator.getTargetVersion().toString());
        platformDump.addProperty("in_world", Minecraft.getInstance().level != null);

        final JsonArray mods = new JsonArray();

        final JsonObject mod = new JsonObject();
        mod.addProperty("id", MOD_ID);
        mod.addProperty("name", MOD_NAME);
        mod.addProperty("version", ViaFabricPlusImpl.INSTANCE.getVersion());

        final JsonArray authors = new JsonArray();

        final JsonObject author1 = new JsonObject();
        author1.addProperty("name", "Florian Reuth (EnZaXD) and ArixCoder");
        final JsonObject author1Contact = new JsonObject();
        author1Contact.addProperty("email", "git@florianreuth.de");
        author1Contact.addProperty("homepage", "https://github.com/florianreuth");
        author1.add("contact", author1Contact);
        authors.add(author1);

        final JsonObject author2 = new JsonObject();
        author2.addProperty("name", "RK_01");
        final JsonObject author2Contact = new JsonObject();
        author2Contact.addProperty("homepage", "https://github.com/RaphiMC");
        author2.add("contact", author2Contact);
        authors.add(author2);

        mod.add("authors", authors);
        mods.add(mod);

        platformDump.add("mods", mods);

        final com.google.gson.JsonObject settings = new com.google.gson.JsonObject();
        SaveManager.INSTANCE.getSettingsSave().writeSettings(settings);
        platformDump.add("settings", GsonUtil.getGson().fromJson(settings.toString(), JsonObject.class));

        return platformDump;
    }

}