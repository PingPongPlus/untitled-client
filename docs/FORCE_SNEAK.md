# Force Sneak

Open **Right Shift → PLAYER → Force Sneak** to hold the gameplay Sneak input.
It keeps working with the inventory, containers, chat, and client menus open.
Minecraft sends the resulting input to the server through its normal player
input packets.

Force Sneak starts off and lasts for the current client session. Turning it off,
or pressing **All off**, restores the normal Sneak key on the next game tick.
Your keybind and Minecraft's Hold/Toggle setting stay unchanged. Inventory
Shift-click still uses the real keyboard modifier.

Minecraft's usual Sneak behavior applies, including slower walking, descending
while flying, and dismounting. Dead players and spectators are unaffected.
Paused single-player worlds resume the updated input when gameplay resumes.
The feature is available in both `master` and `legit` and appears in the active
feature list.

The isolated local-world check is
`./gradlew -I tools/force-sneak-test.gradle runClientGameTest --offline`.
It checks the real menu toggle, client crouching and server input, inventory and
chat screens, disabled behavior, the manual Sneak key, spectator handling, and
All off. Combine with `-I tools/crosshair-test.gradle` to run both feature checks.
