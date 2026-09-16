# PingPlus client

Fabric client mod for Minecraft 26.2 / Java 25.

## ClickGUI

Enter a world and press **Right Shift**. Rebind **Open ClickGUI** under
Options → Controls → Key Binds → PingPlus Client. Escape or the opening key
closes the panel. The game continues while the panel is open.

- Filter by All modules, HUD, or Player, or type into the search field.
- Click a module card to toggle its actual HUD display.
- Scroll or use the arrow buttons when cards do not fit the window.
- Tab navigates controls; Enter/Space activate cards. The slider supports native
  keyboard controls (Enter to select, then arrow keys).
- Appearance includes four accent colors, hover animations, and HUD background
  opacity with a preview.
- Settings save to `config/pingplus-client.properties` in the game directory.

Available modules: FPS, coordinates, direction, sprint/sneak status, local clock,
and current-world session timer. Only FPS is enabled by default. HUD elements are
hidden while a screen is open and follow Minecraft's F1 visibility setting.
Session time resets when the client world changes, including dimension changes.

## Development

`./gradlew build` builds the mod; `./gradlew runClient` launches the development client.

See [the ClickGUI developer guide](docs/CLICKGUI.md) for the screen lifecycle, rendering,
settings flow, and examples of adding modules.

Client UI code lives in `src/client/java/pingplus/voicechat/client/gui`.
`ClientModule` defines the catalog, `ClientConfig` stores settings, `ClientHud`
provides module output, and `ClickGuiScreen` arranges reusable widgets. Add new
modules to the enum and implement their behavior in the HUD or client event hooks.

Manual checks: open/close and rebind the GUI; toggle all modules; search for a match
and an empty result; navigate by keyboard; resize and change GUI scale; scroll the
cards at large GUI scales; change appearance and restart to verify persistence;
hide the HUD with F1.
