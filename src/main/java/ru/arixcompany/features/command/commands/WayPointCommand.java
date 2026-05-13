package ru.arixcompany.features.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.command.AbstractCommand;
import ru.arixcompany.features.command.arguments.WayPointArgumentType;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.repos.WayPointRepo;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.HashMap;
import java.util.Map;

public class WayPointCommand extends AbstractCommand {
    private static final Map<WayPointRepo.WayPoint, WaypointScreenData> waypointScreenData = new HashMap<>();
    private static final net.minecraft.resources.Identifier WAYPOINT_TEX =
            net.minecraft.resources.Identifier.withDefaultNamespace("textures/arix/waypoint.png");

    public WayPointCommand() {
        super("waypoint", "Управление вейпоинтами");
        EventRepo.register(this);
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {

        builder.then(literal("list").executes(context -> {

            if (WayPointRepo.getWayPoints().isEmpty()) {
                MessageSender.print(Component.literal("⚠ Список вейпоинтов пуст.")
                        .withStyle(ChatFormatting.YELLOW));
                return SINGLE_SUCCESS;
            }

            MessageSender.print(Component.literal("📍 Список вейпоинтов:")
                    .withStyle(ChatFormatting.GOLD));

            WayPointRepo.getWayPoints().forEach(wp -> {
                MutableComponent msg = Component.literal("• ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(wp.getName()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" [" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]")
                                .withStyle(ChatFormatting.GRAY));

                MessageSender.print(msg);
            });

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("name", StringArgumentType.word())
                .executes(context -> {
                    String name = context.getArgument("name", String.class);
                    int x = (int) mc.player.getX();
                    int y = (int) mc.player.getY();
                    int z = (int) mc.player.getZ();
                    addWaypoint(name, x, y, z);
                    return SINGLE_SUCCESS;
                })

                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    String name = context.getArgument("name", String.class);
                                    int x = context.getArgument("x", Integer.class);
                                    int z = context.getArgument("z", Integer.class);
                                    addWaypoint(name, x, 0, z);
                                    return SINGLE_SUCCESS;
                                }))
                        .then(argument("y", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            String name = context.getArgument("name", String.class);
                                            int x = context.getArgument("x", Integer.class);
                                            int y = context.getArgument("y", Integer.class);
                                            int z = context.getArgument("z", Integer.class);
                                            addWaypoint(name, x, y, z);
                                            return SINGLE_SUCCESS;
                                        }))))));

        builder.then(literal("remove")
                .then(argument("name", WayPointArgumentType.create())
                        .executes(context -> {

                            WayPointRepo.WayPoint wp =
                                    context.getArgument("name", WayPointRepo.WayPoint.class);

                            WayPointRepo.removeWayPoint(wp);

                            MessageSender.print(Component.literal("✓ Вейпоинт удалён.")
                                    .withStyle(ChatFormatting.GREEN));

                            return SINGLE_SUCCESS;
                        })));

        builder.then(literal("clear").executes(context -> {

            int size = WayPointRepo.getWayPoints().size();
            WayPointRepo.getWayPoints().clear();

            MessageSender.print(Component.literal("✓ Удалено " + size + " вейпоинтов.")
                    .withStyle(ChatFormatting.GREEN));

            return SINGLE_SUCCESS;
        }));
    }

    private void addWaypoint(String name, int x, int y, int z) {
        String server = mc.isSingleplayer()
                ? "SinglePlayer"
                : mc.getConnection().getServerData().ip ;

        WayPointRepo.WayPoint wp = new WayPointRepo.WayPoint(x, y, z, name, server);
        WayPointRepo.addWayPoint(wp);

        MessageSender.print(Component.literal("✓ Вейпоинт ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" добавлен ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("X: " + x).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Y: " + y).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Z: " + z).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        waypointScreenData.clear();

        if (mc.level == null || mc.player == null) return;
        if (WayPointRepo.getWayPoints().isEmpty()) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (WayPointRepo.WayPoint wp : WayPointRepo.getWayPoints()) {
            if (wp.getName() == null) continue;

            Vec3 worldPos = new Vec3(wp.getX() + 0.5, wp.getY() + 1.0, wp.getZ() + 0.5);
            double distance = cameraPos.distanceTo(worldPos);

            Vec3 direction = worldPos.subtract(cameraPos).normalize();
            double projDist = Math.min(distance, 100.0);
            Vec3 projectionPos = cameraPos.add(direction.multiply(projDist,projDist,projDist));

            Vec3 screenPos = ProjectUtils.worldSpaceToScreenSpace(projectionPos);

            if (screenPos != null && screenPos.z >= 0.0 && screenPos.z <= 1.0) {
                waypointScreenData.put(wp, new WaypointScreenData(
                        screenPos.x, screenPos.y, screenPos.z,
                        distance
                ));
            }
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (mc.level == null || mc.player == null) return;
        if (waypointScreenData.isEmpty()) return;

        for (Map.Entry<WayPointRepo.WayPoint, WaypointScreenData> entry : waypointScreenData.entrySet()) {
            WayPointRepo.WayPoint wp = entry.getKey();
            WaypointScreenData data = entry.getValue();

            double distance = data.distance;

            float screenX = (float) data.x;
            float screenY = (float) data.y;

            String nameText = wp.getName();
            String distText = formatDistance(distance);

            float fontSize = 10f;

            int distColor = getDistanceColor(distance);
            int gray = ColorUtil.rgba(160, 160, 160, 255);

            MutableComponent label =
                    Component.literal(nameText).withColor(-1)
                            .append(Component.literal(" (").withColor(gray))
                            .append(Component.literal(distText).withColor(distColor))
                            .append(Component.literal(")").withColor(gray));

            float labelW = FontManager.get(fontSize).getComponentWidth(label);

            float iconSize = 20f;
            float iconX = screenX - iconSize / 2f;
            float iconY = screenY - 35f;

            RenderUtils.drawImage(
                    event.getGuiGraphics(),
                    WAYPOINT_TEX,
                    iconX, iconY,
                    iconSize, iconSize,
                    Colors.accent(1f)
            );

            FontManager.get(fontSize).drawComponent(
                    event.getGuiGraphics(),
                    label,
                    screenX - labelW / 2f,
                    screenY - 10f,
                    -1
            );
        }
    }

    private String formatDistance(double distance) {
        if (distance >= 10000.0) {
            return String.format("%.1fkm", distance / 1000.0);
        } else if (distance >= 1000.0) {
            return String.format("%.2fkm", distance / 1000.0);
        }
        return String.format("%.0fm", distance);
    }

    private int getDistanceColor(double distance) {
        if (distance < 50) {
            return ColorUtil.rgba(100, 255, 100, 255);
        } else if (distance < 200) {
            return ColorUtil.rgba(255, 255, 100, 255);
        } else if (distance < 1000) {
            return ColorUtil.rgba(255, 165, 50, 255);
        } else {
            return ColorUtil.rgba(255, 80, 80, 255);
        }
    }


        private record WaypointScreenData(double x, double y, double z, double distance) {
    }
}