package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.Comparator;

public class ScoreboardDraggable extends DraggableComponent {

    private static final float BG_RADIUS = 4f;

    private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator
            .comparing(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    public ScoreboardDraggable() {
        super("Scoreboard", 0, 0, 120, 60);

        if (mc.getWindow() != null) {
            this.x = mc.getWindow().getGuiScaledWidth() - 130;
            this.y = mc.getWindow().getGuiScaledHeight() / 2 - 30;
            this.renderX = this.x;
            this.renderY = this.y;
        }
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }

        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Скорбоард");
    }

    public static boolean isCustomScoreboardActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Скорбоард");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.level == null || mc.player == null) return;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = getRelevantObjective(scoreboard);
        if (objective == null) return;

        Font font = mc.font;
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        DisplayEntry[] entries = scoreboard.listPlayerScores(objective)
                .stream()
                .filter(entry -> !entry.isHidden())
                .sorted(SCORE_DISPLAY_ORDER)
                .limit(15L)
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    Component ownerName = entry.ownerName();
                    Component displayName = PlayerTeam.formatNameForTeam(team, ownerName);
                    Component scoreText = entry.formatValue(numberFormat);
                    int scoreWidth = font.width(scoreText);
                    return new DisplayEntry(displayName, scoreText, scoreWidth);
                })
                .toArray(DisplayEntry[]::new);

        if (entries.length == 0) return;

        Component title = objective.getDisplayName();

        int titleWidth = font.width(title);
        int maxContentWidth = titleWidth;
        int colonWidth = font.width(": ");

        for (DisplayEntry entry : entries) {
            maxContentWidth = Math.max(
                    maxContentWidth,
                    font.width(entry.name()) + (entry.scoreWidth() > 0 ? colonWidth + entry.scoreWidth() : 0)
            );
        }

        int count = entries.length;

        float totalW = maxContentWidth + 4;
        float headerH = 9f;
        float bodyH = count * 9f + 1f;
        float totalH = headerH + bodyH;

        this.width = totalW;
        this.height = totalH;

        int topColor = applyAlpha(mc.options.getBackgroundColor(0.4F), alpha);
        int bottomColor = applyAlpha(mc.options.getBackgroundColor(0.3F), alpha);
        int textColor = applyAlpha(0xFFFFFFFF, alpha);

        RenderUtils.fillRoundRectGradient(
                rx, ry, totalW, totalH,
                BG_RADIUS,
                topColor, bottomColor
        );

        int titleX = Mth.floor(rx + 2 + maxContentWidth / 2f - titleWidth / 2f);
        int titleY = Mth.floor(ry + 1);
        graphics.drawString(font, title, titleX, titleY, textColor, false);

        int textLeft = Mth.floor(rx + 2);
        int scoreRight = Mth.floor(rx + totalW);

        for (int i = 0; i < count; i++) {
            DisplayEntry entry = entries[i];
            int lineY = Mth.floor(ry + totalH - (count - i) * 9f);

            graphics.drawString(font, entry.name(), textLeft, lineY, textColor, false);

            if (entry.scoreWidth() > 0) {
                graphics.drawString(font, entry.score(), scoreRight - entry.scoreWidth(), lineY, textColor, false);
            }
        }
    }

    private Objective getRelevantObjective(Scoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
        if (team != null) {
            DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
            if (slot != null) {
                Objective obj = scoreboard.getDisplayObjective(slot);
                if (obj != null) return obj;
            }
        }

        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    private int applyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        a = (int) (a * Mth.clamp(alpha, 0f, 1f));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private record DisplayEntry(Component name, Component score, int scoreWidth) {}
}