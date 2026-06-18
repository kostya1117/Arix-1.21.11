package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.misc.Protect;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardDraggable extends DraggableComponent {

    private static final float FONT_SIZE = 10f;
    private static final float BG_RADIUS = 4f;

    public BooleanSetting customFont = new BooleanSetting("Свой шрифт");
    public BooleanSetting hideNumbers = new BooleanSetting("Убрать цифры");

    private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator
            .comparing(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    public ScoreboardDraggable() {
        super("Scoreboard", 0, 0, 120, 60);
        setup(customFont, hideNumbers);

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

        boolean useCustom = customFont.isValue();
        Font vanillaFont = mc.font;
        CustomFont customFontRenderer = useCustom ? FontManager.get(FONT_SIZE) : null;

        float lineH = useCustom ? customFontRenderer.getHeight() : 9f;
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
                    int scoreWidth = hideNumbers.isValue() ? 0 : useCustom
                            ? Mth.floor(customFontRenderer.getComponentWidth(scoreText))
                            : vanillaFont.width(scoreText);
                    return new DisplayEntry(displayName, scoreText, scoreWidth);
                })
                .toArray(DisplayEntry[]::new);

        if (entries.length == 0) return;

        Component title = applyHideAnarchy(objective.getDisplayName());

        int titleWidth = useCustom
                ? Mth.floor(customFontRenderer.getComponentWidth(title))
                : vanillaFont.width(title);
        int maxContentWidth = titleWidth;
        int colonWidth = useCustom
                ? Mth.floor(customFontRenderer.getComponentWidth(Component.literal(": ")))
                : vanillaFont.width(": ");

        for (DisplayEntry entry : entries) {
            int nameWidth = useCustom
                    ? Mth.floor(customFontRenderer.getComponentWidth(entry.name()))
                    : vanillaFont.width(entry.name());
            maxContentWidth = Math.max(
                    maxContentWidth,
                    nameWidth + (entry.scoreWidth() > 0 ? colonWidth + entry.scoreWidth() : 0)
            );
        }

        int count = entries.length;

        float totalW = maxContentWidth + 4;
        float headerH = useCustom ? lineH + 1f : 9f;
        float bodyH = count * lineH + 1f;
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

        float titleX = rx + 2 + maxContentWidth / 2f - titleWidth / 2f;
        float titleY = ry + 1;

        if (useCustom) {
            customFontRenderer.drawComponent(graphics, title, titleX, titleY, textColor);
        } else {
            graphics.drawString(vanillaFont, title, Mth.floor(titleX), Mth.floor(titleY), textColor, false);
        }

        float textLeft = rx + 2;
        float scoreRight = rx + totalW;

        for (int i = 0; i < count; i++) {
            DisplayEntry entry = entries[i];
            float lineY = ry + totalH - (count - i) * lineH;

            if (useCustom) {
                customFontRenderer.drawComponent(graphics, entry.name(), textLeft, lineY, textColor);
            } else {
                graphics.drawString(vanillaFont, entry.name(), Mth.floor(textLeft), Mth.floor(lineY), textColor, false);
            }

            if (entry.scoreWidth() > 0) {
                float sx = scoreRight - entry.scoreWidth();
                if (useCustom) {
                    customFontRenderer.drawComponent(graphics, entry.score(), sx, lineY, textColor);
                } else {
                    graphics.drawString(vanillaFont, entry.score(), Mth.floor(sx), Mth.floor(lineY), textColor, false);
                }
            }
        }
    }

    private static Component applyHideAnarchy(Component comp) {
        Protect protect = Arix.getInstance().getModuleRepo().getModule(Protect.class);
        if (protect == null || !protect.isState() || !protect.hideAnarchy.isValue()) return comp;
        return filterComponentDigits(comp);
    }

    private static Component filterComponentDigits(Component comp) {
        ComponentContents contents = comp.getContents();
        ComponentContents newContents = contents;

        if (contents instanceof PlainTextContents ptc) {
            String text = ptc.text();
            String filtered = text.replaceAll("\\d+", "HIDDEN");
            if (!filtered.equals(text)) {
                newContents = PlainTextContents.create(filtered);
            }
        }

        List<Component> siblings = comp.getSiblings();
        List<Component> newSiblings = null;
        boolean siblingsChanged = false;

        if (!siblings.isEmpty()) {
            newSiblings = new ArrayList<>(siblings.size());
            for (Component sib : siblings) {
                Component filteredSib = filterComponentDigits(sib);
                newSiblings.add(filteredSib);
                if (filteredSib != sib) siblingsChanged = true;
            }
        }

        if (newContents == contents && !siblingsChanged) return comp;

        MutableComponent result = MutableComponent.create(newContents).setStyle(comp.getStyle());
        List<Component> finalSiblings = siblingsChanged ? newSiblings : siblings;
        for (Component sib : finalSiblings) {
            result.append(sib);
        }
        return result;
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