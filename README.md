# PingPlus UI starter

Fabric client mod for Minecraft 26.2 / Java 25.

The ClickGUI contains one FPS toggle and a Close button. Other client features
(such as the coordinates HUD and player Mixins) are separate from the menu.
Press **Right Shift** in a world to open the panel, then click **FPS: ON/OFF** to
show or hide the FPS display. Escape, Right Shift, or the close button closes it.
The opening key can be rebound under Controls → Key Binds → PingPlus Client.

FPS starts enabled. Its toggle survives reopening the panel but resets when you
restart Minecraft. There is no config file, search, category navigation, scrolling,
appearance settings, or other modules. The menu uses a simple dark rectangle and standard Minecraft buttons.

## Branches

- `codex/full-clickgui`: preserved full implementation, including settings and docs.
- `codex/fps-only-ui`: minimal version for you to extend.

## Development

- `./gradlew build` compiles and packages the mod.
- `./gradlew runClient` launches Minecraft for manual testing.
- [UI code guide](docs/CLICKGUI.md) explains the remaining classes.

Manual checks: open and close the panel, toggle FPS off and on, reopen the panel,
use Tab/Enter to activate the button, and resize or change GUI scale.
