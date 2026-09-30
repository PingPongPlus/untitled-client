package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;
import pingplus.voicechat.client.gui.glass.GlassButtonRenderer;

import java.util.Comparator;
import java.util.List;

/** Editable server sidebar, preserving vanilla score selection and rich text. */
public final class ScoreboardHud {
    public static final ScoreboardHud INSTANCE = new ScoreboardHud();
    private static final int PAD = 5, GAP = 6;
    private boolean enabled = true, glass = true, edges = false;
    private View view;

    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; }

    public static Objective objective() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return null;
        var scoreboard = client.level.getScoreboard();
        var team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
        if (team != null && team.getColor().isPresent()) {
            var teamObjective = scoreboard.getDisplayObjective(team.getColor().get().displaySlot());
            if (teamObjective != null) return teamObjective;
        }
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    public boolean isVisible() {
        Objective objective = objective();
        view = enabled && objective != null ? createView(objective) : null;
        return view != null;
    }

    public int width() { return view == null ? 100 : view.width(); }
    public int height() { return view == null ? 20 : view.height(); }

    public record Row(Component name, Component score, int scoreWidth) {}
    public record View(Component title, List<Row> rows, int width, int height) {}

    public static View createView(Objective objective) {
        var font = Minecraft.getInstance().font;
        var scoreboard = objective.getScoreboard();
        var format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<Row> rows = scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(15)
                .map(entry -> {
                    Component name = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
                    Component score = entry.formatValue(format);
                    return new Row(name, score, font.width(score));
                }).toList();
        int contentWidth = font.width(objective.getDisplayName());
        for (Row row : rows) {
            contentWidth = Math.max(contentWidth, font.width(row.name()) + (row.scoreWidth() > 0 ? GAP + row.scoreWidth() : 0));
        }
        return new View(objective.getDisplayName(), rows, contentWidth + PAD * 2,
                (rows.size() + 1) * (font.lineHeight + 2) + PAD * 2);
    }

    public void render(GuiGraphicsExtractor g) {
        if (view == null || !enabled) return;
        var font = Minecraft.getInstance().font;
        if (glass) {
            g.nextStratum();
            GlassButtonRenderer.drawHudRect(g, 0, 0, view.width(), view.height(), 0xE01FFF00, false, edges);
            g.nextStratum();
        }
        g.text(font, view.title(), (view.width() - font.width(view.title())) / 2, PAD, 0xFFFFFFFF, !glass);
        int y = PAD + font.lineHeight + 2;
        for (Row row : view.rows()) {
            g.text(font, row.name(), PAD, y, 0xFFFFFFFF, !glass);
            g.text(font, row.score(), view.width() - PAD - row.scoreWidth(), y, 0xFFFFFFFF, !glass);
            y += font.lineHeight + 2;
        }
    }

    private ScoreboardHud() {}
}
