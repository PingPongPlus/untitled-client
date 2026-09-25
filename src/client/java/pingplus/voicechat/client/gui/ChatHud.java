package pingplus.voicechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import pingplus.voicechat.client.gui.glass.GlassButtonRenderer;
import pingplus.voicechat.client.gui.hud.HudEditor;
import pingplus.voicechat.client.gui.hud.HudLayout;
import pingplus.voicechat.mixin.client.ChatComponentAccessor;

/** The vanilla chat panel promoted to a movable glass widget, rendered through HudEditor. */
public final class ChatHud {
    public static final ChatHud INSTANCE = new ChatHud();
    public static final String WIDGET_ID = "chat";
    /** Logical bottom anchor for the gameplay render; consumed by ChatRelocateMixin. */
    private static final ThreadLocal<Integer> LOGICAL_ANCHOR = new ThreadLocal<>();
    private boolean enabled = true, glass = true, edges = true;

    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; }

    public boolean isActive() { return enabled && Minecraft.getInstance().player != null; }

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
        return (int) Math.ceil(chatScale(client) * ChatComponent.getWidth(client.options.chatWidth().get()));
    }

    public int height() {
        Minecraft client = Minecraft.getInstance();
        var chat = client.gui.hud.getChat();
        double pct = chat.isChatFocused()
                ? client.options.chatHeightFocused().get()
                : client.options.chatHeightUnfocused().get();
        return (int) Math.ceil(chatScale(client) * ChatComponent.getHeight(pct));
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, float dt, boolean editing) {
        Minecraft client = Minecraft.getInstance();
        if (!isActive()) return;
        // The open chat screen draws the messages itself; ChatRelocateMixin moves them into this widget.
        if (client.gui.screen() instanceof ChatScreen) return;
        if (!hasMessages()) return;
        if (glass) {
            g.nextStratum();
            GlassButtonRenderer.drawHudRect(g, 0, 0, width(), height(), 0xE01FFF00, false, edges);
            g.nextStratum();
        }
        LOGICAL_ANCHOR.set(height() + 40);
        try {
            client.gui.hud.getChat().extractRenderState(g, client.font, client.gui.hud.getGuiTicks(),
                    -100, -100, ChatComponent.DisplayMode.BACKGROUND, false);
        } finally {
            LOGICAL_ANCHOR.remove();
        }
    }

    private ChatHud() {}
}
