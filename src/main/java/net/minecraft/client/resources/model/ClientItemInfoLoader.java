package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.optifine.Config;
import net.optifine.CustomItems;
import org.slf4j.Logger;

public class ClientItemInfoLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    public static CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> scheduleLoad(ResourceManager p_377664_, Executor p_378750_) {
        RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess().compositeAccess();
        return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> LISTER.listMatchingResources(p_377664_), p_378750_)
            .thenCompose(
                mapResourcesIn -> {
                    List<CompletableFuture<ClientItemInfoLoader.PendingLoad>> list = new ArrayList<>(mapResourcesIn.size());
                    mapResourcesIn.forEach(
                        (loc2In, res2In) -> list.add(
                            CompletableFuture.supplyAsync(
                                () -> {
                                    Identifier identifier = LISTER.fileToId(loc2In);

                                    try (Reader reader = res2In.openAsReader()) {
                                        PlaceholderLookupProvider placeholderlookupprovider = new PlaceholderLookupProvider(registryaccess$frozen);
                                        DynamicOps<JsonElement> dynamicops = placeholderlookupprovider.createSerializationContext(JsonOps.INSTANCE);
                                        ClientItem clientitem = ClientItem.CODEC
                                            .parse(dynamicops, StrictJsonParser.parse(reader))
                                            .ifError(
                                                errorIn -> LOGGER.error(
                                                    "Couldn't parse item model '{}' from pack '{}': {}", identifier, res2In.sourcePackId(), errorIn.message()
                                                )
                                            )
                                            .result()
                                            .map(
                                                item2In -> placeholderlookupprovider.hasRegisteredPlaceholders()
                                                    ? item2In.withRegistrySwapper(placeholderlookupprovider.createSwapper())
                                                    : item2In
                                            )
                                            .orElse(null);
                                        return new ClientItemInfoLoader.PendingLoad(identifier, clientitem);
                                    } catch (Exception exception) {
                                        LOGGER.error("Failed to open item model {} from pack '{}'", loc2In, res2In.sourcePackId(), exception);
                                        return new ClientItemInfoLoader.PendingLoad(identifier, null);
                                    }
                                },
                                p_378750_
                            )
                        )
                    );
                    return Util.sequence(list).thenApply(pendingLoadsIn -> {
                        Map<Identifier, ClientItem> map = new HashMap<>();

                        for (ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload : pendingLoadsIn) {
                            if (clientiteminfoloader$pendingload.clientItemInfo != null) {
                                map.put(clientiteminfoloader$pendingload.id, clientiteminfoloader$pendingload.clientItemInfo);
                            }
                        }

                        if (Config.isCustomItems()) {
                            Map<Identifier, Resource> map1 = CustomItems.getModelResources(false);

                            for (Identifier identifier : map1.keySet()) {
                                Identifier identifier1 = identifier.removePrefixSuffix("models/", ".json");
                                Identifier identifier2 = identifier1.removePrefix("item/");
                                if (!map.containsKey(identifier2)) {
                                    ItemModel.Unbaked itemmodel$unbaked = new BlockModelWrapper.Unbaked(identifier1, List.of());
                                    ClientItem.Properties clientitem$properties = ClientItem.Properties.DEFAULT;
                                    ClientItem clientitem = new ClientItem(itemmodel$unbaked, clientitem$properties);
                                    map.put(identifier2, clientitem);
                                }
                            }
                        }

                        return new ClientItemInfoLoader.LoadedClientInfos(map);
                    });
                }
            );
    }

    public record LoadedClientInfos(Map<Identifier, ClientItem> contents) {
    }

    record PendingLoad(Identifier id, ClientItem clientItemInfo) {
    }
}
