# Minimal ClickGUI code guide

The UI uses Minecraft's native `Screen` and widgets with custom drawing.
Only the panel shell and FPS feature remain on this branch.

## Files

All UI files are in `src/client/java/pingplus/voicechat/client/gui`.

| File | Purpose |
| --- | --- |
| `ClickGuiScreen.java` | Centers and draws the panel; adds the FPS and close buttons. |
| `FlatButton.java` | Shared button appearance, hover animation, mouse and keyboard input. |
| `FpsButton.java` | Draws the FPS card and connects it to the FPS toggle. |
| `FpsHud.java` | Stores one enabled flag and renders the current FPS in the top-left corner. |
| `GuiTheme.java` | Fixed colors, rounded rectangles, and color blending. |

`VoicechatClient.java` creates one `FpsHud`, registers Right Shift, and attaches the
FPS renderer to Fabric's HUD event. Every new screen receives that same instance.

## Flow

1. Right Shift opens `ClickGuiScreen` during gameplay.
2. `init()` calculates the GUI-scaled bounds and registers the two buttons.
3. Minecraft calls `extractBackground()` for the panel, then renders its widgets.
4. Clicking `FpsButton` calls `FpsHud.toggle()`.
5. The HUD callback draws `Minecraft.getInstance().getFps()` when enabled and no
   screen is open. Attaching to the vanilla chat layer inherits F1 HUD visibility.

The flag starts enabled and lives only in memory. No settings files are loaded or
written; any old config file from the full version is ignored and left untouched.

## Extend it yourself

Add your controls inside `ClickGuiScreen.init()`. Use `FlatButton` for a normal
button or subclass it and override `drawContents()` for custom contents. Its
`BooleanSupplier` supplies the selected state; its `Runnable` handles activation.

Change colors in `GuiTheme`, panel dimensions in `ClickGuiScreen`, or the FPS card
layout in `FpsButton`. All drawing and mouse bounds use Minecraft GUI coordinates.

The full module system, search, categories, appearance controls, and persistence
are preserved on the `codex/full-clickgui` branch if you want to refer to them.
