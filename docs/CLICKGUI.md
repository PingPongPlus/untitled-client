# ClickGUI developer guide

The GUI is a normal Minecraft `Screen`. Fabric connects the opening key and HUD
render callback to the client. There is no separate UI framework or raw OpenGL
renderer: custom shapes and text go through Minecraft's `GuiGraphicsExtractor`.

## Start here

Read these classes in order (Java paths are relative to `src/client/java`):

| Class | Responsibility |
| --- | --- |
| `pingplus/voicechat/client/VoicechatClient.java` | Creates shared settings, registers the opening key, and connects tick/HUD callbacks. |
| `pingplus/voicechat/client/gui/ClickGuiScreen.java` | Calculates layout, creates widgets, filters modules, and handles navigation. |
| `pingplus/voicechat/client/gui/FlatButton.java` | Reusable themed button with keyboard activation, hover animation, and focus. |
| `pingplus/voicechat/client/gui/ModuleCard.java` | Specializes the button to display and toggle one module. |
| `pingplus/voicechat/client/gui/OpacitySlider.java` | Gives Minecraft's native slider custom visuals and connects it to settings. |
| `pingplus/voicechat/client/gui/ClientConfig.java` | Owns module/appearance settings and their properties file. |
| `pingplus/voicechat/client/gui/ClientHud.java` | Reads settings and draws the enabled modules during gameplay. |
| `pingplus/voicechat/client/gui/ClientModule.java` | Module catalog: titles, descriptions, categories, and icons. |
| `pingplus/voicechat/client/gui/GuiCategory.java` | Typed categories, separate from their display labels. |
| `pingplus/voicechat/client/gui/GuiTheme.java` | Shared colors, rounded rectangles, and color blending. |

## From key press to screen

1. `VoicechatClient.onInitializeClient()` creates one `ClientConfig` and one
   `ClientHud`. Both the HUD and every new screen use the same config instance.
2. The registered Right Shift mapping is checked at the end of each client tick.
   It opens the GUI only when a player exists and another screen is not open.
3. `client.gui.setScreen(...)` opens `ClickGuiScreen`.
4. Minecraft initializes the screen with its current GUI width and height.
   `init()` calculates bounds, creates navigation and search widgets, and builds
   the selected category's content.
5. Escape, the close button, or the opening key closes the screen. When search has
   focus, the opening key is left to text input; Escape still closes it.
6. `removed()` saves pending changes. `isPauseScreen()` returns false, so opening
   the panel does not pause the world.

The key can be rebound in Minecraft's Controls menu; its translation lives in
`src/client/resources/assets/voicechat/lang/en_us.json`.

## Layout and rendering

All dimensions are **GUI coordinates**, not framebuffer pixels. Minecraft applies
its GUI scale, so mouse coordinates and drawn widget bounds use the same units.

`calculateLayout()` centers a panel with a maximum size of 620 × 366. It selects
one or two card columns based on the available content width, then calculates how
many complete rows fit between the header and footer. The principal measurements
are named constants at the top of `ClickGuiScreen`.

`extractBackground()` draws the dimmed backdrop, panel, heading, preview, and
footer through small `draw...()` methods. Minecraft's inherited screen rendering
then renders the widgets registered with `addRenderableWidget()`.

Each widget owns its drawing and receives input through Minecraft's widget system.
`FlatButton` supplies shared borders and hover behavior. `ModuleCard` overrides
`drawContents()` to add its icon, title, description, state, and toggle indicator.
`OpacitySlider` inherits the native slider's mouse and keyboard behavior.

`GuiTheme.drawRoundedRect()` draws a rectangular center plus one-pixel horizontal
strips at the corners. Their widths follow the circle equation. It uses only
Minecraft's fill operations. Theme colors use **0xAARRGGBB**: alpha, red, green, blue.

Hover animation approaches a target of 0 or 1 using elapsed real time:

```java
double blend = 1 - Math.exp(-elapsedSeconds * HOVER_RESPONSE);
hoverProgress += (target - hoverProgress) * blend;
```

This gives roughly the same transition speed at different frame rates. Disabling
animations assigns the target immediately.

## Filtering and scrolling

`matchingModules()` filters the catalog by category and a case-insensitive search
of the title and description. `refreshContent()` replaces only the content widgets;
it keeps the search field alive so its cursor is not lost while typing.

Scrolling changes `firstVisibleRow` and creates the cards in that range. Offscreen
cards are not registered widgets, so they cannot be clicked or focused. Footer
arrow buttons provide a keyboard-accessible alternative to the mouse wheel.

Appearance uses the same content area but creates accent, animation, and opacity
controls instead of module cards.

## What happens when you click a card?

```text
ModuleCard action
  -> ClientConfig.toggleModule(module)
  -> ClientConfig.save()
  -> next widget render reads the new state
  -> ClientHud reads that same state during gameplay
```

The card changes data; it does not draw the gameplay HUD itself. This separation
allows settings to remain active after the screen closes.

`ClientConfig` keeps mutable fields private. Callers use `toggleModule()`,
`cycleAccent()`, `toggleAnimations()`, and `setHudOpacity()`. These methods mark the
config dirty automatically, so a caller cannot forget that step. `save()` writes
only if something changed.

Module toggles and appearance buttons save immediately. Slider dragging updates
memory and the preview continuously, then saves on release. Keyboard slider
changes are also saved when the screen is removed. The properties keys and their
format remain compatible with the original implementation.

## Adding a module

For another text HUD module:

1. Add an entry to `ClientModule`, for example:

   ```java
   HEALTH("Health", "Current player health", GuiCategory.PLAYER, "HP")
   ```

2. Add its output to the exhaustive switch in `ClientHud.moduleText()`:

   ```java
   case HEALTH -> "HP  " + Math.round(player.getHealth());
   ```

The card, category filtering, search, enabled count, and persistence are generated
from the catalog, so they need no extra registration. New modules start disabled
unless explicitly added to the default set in `ClientConfig`.

A module that changes behavior instead of displaying text needs its own client
event logic. Consult `config.isEnabled(module)` in that logic; do not execute
gameplay behavior inside a widget rendering method.

Enum names are persisted as keys (`FPS`, `COORDINATES`, etc.). Changing a display
title is safe; renaming an enum entry requires migrating the old config key.

## Changing appearance

- Change shared colors in `GuiTheme`.
- Add or edit accent name/color pairs in `ClientConfig.ACCENTS`.
- Change panel sizing and card spacing in `ClickGuiScreen`'s layout constants.
- Change card contents in `ModuleCard.drawContents()` and the common button shell
  in `FlatButton.extractWidgetRenderState()`.
- Change HUD chip styling in `ClientHud.drawChip()`; the preview uses that method too.

## Validation

Run `./gradlew build` to compile and package the client. There is no automated
in-game UI test suite. Use the manual checklist in the README to check appearance,
focus, scrolling, rebinding, and resizing. Settings persistence can be checked
without a game by constructing `ClientConfig` with a temporary `Path`.
