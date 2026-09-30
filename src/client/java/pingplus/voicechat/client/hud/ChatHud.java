package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.ChatVisiblity;
import pingplus.voicechat.client.gui.glass.GlassStyle;
import pingplus.voicechat.client.hud.editor.HudEditor;
import pingplus.voicechat.client.hud.editor.HudLayout;
import pingplus.voicechat.mixin.client.ChatComponentAccessor;

/** The vanilla chat panel promoted to a movable glass widget, rendered through HudEditor. */
public final class ChatHud {
    public static final ChatHud INSTANCE = new ChatHud();
    public static final String WIDGET_ID = "chat";
    private static final int PAD = 8;
    /** Logical bottom anchor for the gameplay render; consumed by ChatRelocateMixin. */
    private static final ThreadLocal<Integer> LOGICAL_ANCHOR = new ThreadLocal<>();
    private boolean enabled = true, glass = true, edges = false;

    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; }

    public boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        return enabled && client.player != null && client.options.chatVisibility().get() != ChatVisiblity.HIDDEN;
    }

    public boolean hasMessages() {
        return !((ChatComponentAccessor) Minecraft.getInstance().gui.hud.getChat()).voicechat$trimmedMessages().isEmpty();
    }

    public static Integer logicalAnchor() { return LOGICAL_ANCHOR.get(); }

    public static HudLayout.Bounds screenBounds() {
        Minecraft client = Minecraft.getInstance();
        return HudEditor.bounds(WIDGET_ID, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    private static double chatScale(Minecraft client) { return client.options.chatScale().get(); }

    public int width() {
        Minecraft client = Minecraft.getInstance();
        // Vanilla divides its wrap width by chat scale before scaling the text back up.
        return ChatComponent.getWidth(client.options.chatWidth().get()) + PAD * 2 + 8;
    }

    public int height() {
        Minecraft client = Minecraft.getInstance();
        var chat = client.gui.hud.getChat();
        double pct = chat.isChatFocused()
                ? client.options.chatHeightFocused().get()
                : client.options.chatHeightUnfocused().get();
        return (int) Math.ceil(chatScale(client) * ChatComponent.getHeight(pct)) + PAD * 2;
    }

    public int renderAnchor() { return height() - PAD + 40; }

    /** Compensate for vanilla's four-pixel text inset at any chat scale. */
    public float textOffsetX() { return PAD - (float) chatScale(Minecraft.getInstance()) * 4; }

    public void renderPanel(GuiGraphicsExtractor g) {
        if (!glass) return;
        Minecraft client = Minecraft.getInstance();
        var chat = client.gui.hud.getChat();
        var access = (ChatComponentAccessor) chat;
        var lines = access.voicechat$trimmedMessages();
        int start = access.voicechat$scrollbarPos();
        int count = Math.min(chat.getLinesPerPage(), lines.size() - start);
        int visibleRows = 0;
        float opacity = 0;
        for (int row = 0; row < count; row++) {
            int age = client.gui.hud.getGuiTicks() - lines.get(start + row).addedTime();
            float fade = chat.isChatFocused() ? 1 : (float) Math.clamp((200 - age) / 20.0, 0, 1);
            fade *= fade;
            if (fade > 1.0e-5f) visibleRows = row + 1;
            opacity = Math.max(opacity, fade);
        }
        if (visibleRows == 0) return;
        int lineHeight = (int) (9 * (client.options.chatLineSpacing().get() + 1));
        int panelHeight = (int) Math.ceil(visibleRows * lineHeight * chatScale(client)) + PAD * 2;
        float chatOpacity = client.options.chatOpacity().get().floatValue() * .9f + .1f;
        g.nextStratum();
        GlassStyle.surface(g, 0, height() - panelHeight, width(), panelHeight,
                .82f * opacity * chatOpacity, .04f, edges);
        g.nextStratum();
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, float dt, boolean editing) {
        Minecraft client = Minecraft.getInstance();
        if (!isActive()) return;
        // The open chat screen draws the messages itself; ChatRelocateMixin moves them into this widget.
        if (client.gui.screen() instanceof ChatScreen) return;
        if (!hasMessages()) return;
        renderPanel(g);
        g.pose().pushMatrix();
        g.pose().translate(textOffsetX(), 0);
        LOGICAL_ANCHOR.set(renderAnchor());
        try {
            client.gui.hud.getChat().extractRenderState(g, client.font, client.gui.hud.getGuiTicks(),
                    -100, -100, ChatComponent.DisplayMode.BACKGROUND, false);
        } finally {
            LOGICAL_ANCHOR.remove();
            g.pose().popMatrix();
        }
    }

    private ChatHud() {}
}
