package de.florianmichael.vialoadingbase.platform;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.configuration.ViaVersionConfig;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.UnsupportedSoftware;
import com.viaversion.viaversion.api.platform.ViaPlatform;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.VersionInfo;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.vialoadingbase.platform.viaversion.VLBViaAPIWrapper;
import de.florianmichael.vialoadingbase.platform.viaversion.VLBViaConfig;
import de.florianmichael.vialoadingbase.util.VLBTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ViaVersionPlatformImpl implements ViaPlatform<UserConnection> {

    private final ViaAPI<UserConnection> api = new VLBViaAPIWrapper();

    private final Logger logger;
    private final VLBViaConfig config;

    public ViaVersionPlatformImpl(final Logger logger) {
        this.logger = logger;
        config = new VLBViaConfig(
                new File(ViaLoadingBase.getInstance().getRunDirectory(), "viaversion.yml"),
                logger
        );
    }

    public static List<ProtocolVersion> createVersionList() {
        final List<ProtocolVersion> versions = new ArrayList<>(ProtocolVersion.getProtocols())
                .stream()
                .filter(version -> version.newerThanOrEqualTo(ProtocolVersion.v1_8))
                .collect(Collectors.toList());
        Collections.reverse(versions);
        return versions;
    }

    /**
     * Запускает задачу асинхронно через планировщик ViaVersion.
     *
     * @param runnable задача для выполнения
     * @return VLBTask обёртка над Future задачи
     */
    @Override
    public VLBTask runAsync(Runnable runnable) {
        return new VLBTask(Via.getManager().getScheduler().execute(runnable));
    }

    /**
     * Запускает повторяющуюся асинхронную задачу.
     * Интервал задаётся в тиках (1 тик = 50 мс).
     *
     * @param runnable задача для выполнения
     * @param ticks    интервал повторения в тиках
     * @return VLBTask обёртка над Future задачи
     */
    @Override
    public VLBTask runRepeatingAsync(Runnable runnable, long ticks) {
        return new VLBTask(
                Via.getManager().getScheduler().scheduleRepeating(runnable, 0, ticks * 50, TimeUnit.MILLISECONDS)
        );
    }

    /**
     * Запускает задачу синхронно.
     * В контексте ViaLoadingBase делегирует в runAsync,
     * так как отдельного синхронного потока нет.
     *
     * @param runnable задача для выполнения
     * @return VLBTask обёртка над Future задачи
     */
    @Override
    public VLBTask runSync(Runnable runnable) {
        return this.runAsync(runnable);
    }

    /**
     * Запускает задачу синхронно с задержкой в тиках.
     * В контексте ViaLoadingBase делегирует в асинхронный планировщик.
     *
     * @param runnable задача для выполнения
     * @param ticks    задержка перед запуском в тиках
     * @return VLBTask обёртка над Future задачи
     */
    @Override
    public VLBTask runSync(Runnable runnable, long ticks) {
        return new VLBTask(
                Via.getManager().getScheduler().schedule(runnable, ticks * 50, TimeUnit.MILLISECONDS)
        );
    }

    /**
     * Запускает повторяющуюся синхронную задачу.
     * Делегирует в runRepeatingAsync.
     *
     * @param runnable задача для выполнения
     * @param ticks    интервал повторения в тиках
     * @return VLBTask обёртка над Future задачи
     */
    @Override
    public VLBTask runRepeatingSync(Runnable runnable, long ticks) {
        return this.runRepeatingAsync(runnable, ticks);
    }

    /**
     * Указывает, является ли платформа прокси.
     * ViaLoadingBase работает как прокси, поэтому возвращает true.
     *
     * @return true
     */
    @Override
    public boolean isProxy() {
        return true;
    }

    /**
     * Вызывается при перезагрузке платформы.
     * В ViaLoadingBase не требует дополнительных действий.
     */
    @Override
    public void onReload() {
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public ViaVersionConfig getConf() {
        return config;
    }

    @Override
    public ViaAPI<UserConnection> getApi() {
        return api;
    }

    @Override
    public File getDataFolder() {
        return ViaLoadingBase.getInstance().getRunDirectory();
    }

    @Override
    public String getPluginVersion() {
        return VersionInfo.VERSION;
    }

    @Override
    public String getPlatformName() {
        return "ViaLoadingBase";
    }

    @Override
    public String getPlatformVersion() {
        return ViaLoadingBase.VERSION;
    }

    public VLBViaConfig getConfig() {
        return config;
    }

    @Override
    public Collection<UnsupportedSoftware> getUnsupportedSoftwareClasses() {
        return ViaPlatform.super.getUnsupportedSoftwareClasses();
    }

    /**
     * Проверяет, установлен ли плагин с указанным именем.
     * ViaLoadingBase не управляет плагинами, всегда возвращает false.
     *
     * @param s имя плагина
     * @return false
     */
    @Override
    public boolean hasPlugin(String s) {
        return false;
    }

    /**
     * Возвращает дамп данных платформы в виде JsonObject.
     * Если поставщик дампа не задан — возвращает пустой JsonObject.
     *
     * @return JsonObject с данными дампа или пустой объект
     */
    @Override
    public JsonObject getDump() {
        if (ViaLoadingBase.getInstance().getDumpSupplier() == null) {
            return new JsonObject();
        }
        return ViaLoadingBase.getInstance().getDumpSupplier().get();
    }
}