# Fast Place

On the full `master` client, open **Right Shift > AUTOMATION > Fast Place**.
It starts disabled. Hold right-click while placing blocks to repeat placement
faster. Click its gear to open **Fast Place options** and set **Place delay (ticks)** from 1 to 4.
The default is 1 tick (up to 20 held-button placement attempts per second at the
normal client tick rate); vanilla's 4-tick delay is about 5 attempts per second.
Actual placed blocks depend on valid placement positions and server acceptance.

The client performs the usual placement once and shortens its repeat delay only
after successfully placing a block. Both hands are supported. Opening containers,
using other items, and failed placements retain the vanilla delay. Menus, paused
gameplay, inactive windows, dead players and spectators suspend the feature.
The toggle and delay follow the other automation settings' session lifetime.
Fast Place appears in the active-module list while enabled.

`legit` excludes Fast Place. The isolated runtime check is
`./gradlew -I tools/fast-place-test.gradle runClientGameTest --offline`.
