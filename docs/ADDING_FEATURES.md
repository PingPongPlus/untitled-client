# Adding a feature

- Put HUD widgets in `src/client/java/pingplus/voicechat/client/hud/`; measurements in `hud/metrics/`, layout/editor code in `hud/editor/`.
- Keep screens in `client/gui/`, shared glass in `gui/glass/`, and feature logic in its own client package.
- Example: a minimal FPS widget (`FpsHud.java`). FPS already exists; use a new class and widget ID for another feature.

```java
package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.gui.glass.GlassStyle;

public final class FpsHud {
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    private String text() { return "FPS  " + Minecraft.getInstance().getFps(); }
    public int width() { return Minecraft.getInstance().font.width(text()) + 16; }
    public int height() { return Minecraft.getInstance().font.lineHeight + 10; }

    public void render(GuiGraphicsExtractor g) {
        var client = Minecraft.getInstance();
        if (!enabled || client.player == null) return;
        g.nextStratum();
        GlassStyle.surface(g, 0, 0, width(), height(), .88f, .08f, false);
        g.nextStratum();
        g.text(client.font, text(), 8, 5, GlassStyle.TEXT, false);
    }
}
```

- In `VoicechatClient.initializeClientFeatures()`, import the widget and `hud.editor.HudEditor`, then register it. Draw at `(0, 0)`; the editor handles position, scale, and saved layout.

```java
FpsHud fpsHud = new FpsHud();
HudEditor.register(new HudEditor.Entry(
        "fps", "FPS", fpsHud::width, fpsHud::height,
        (w, h) -> 18, (w, h) -> 13,
        fpsHud::isEnabled,
        (g, mx, my, dt, editing) -> fpsHud.render(g),
        java.util.List::of));
```

- Pass the same instance to `ClickGuiScreen`; in `init()`, add the HUD toggle. Its existing toggle helper also supports **All off**.

```java
toggle(hud, "Frame rate", fpsHud::isEnabled, fpsHud::toggle);
```

- Optional: inside `ArraylistHud.features()`, add `new Named("Frame rate", fpsHud::isEnabled)` to the active-feature list.
- Check with `./gradlew build --offline`; verify **Right Shift** toggles/All off and **G** dragging/resizing. More widget details: [WIDGETS.md](WIDGETS.md).
