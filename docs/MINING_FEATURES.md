# Auto Tools and Xray

Both features are available in the full `master` client and start disabled.
The `legit` branch excludes them. Their settings last for the current session,
like the other automation settings.

**Right Shift → AUTOMATION → Auto Tools** selects a tool from your hotbar when
you start or continue mining a block. It prefers tools that can harvest the block,
then compares their mining speeds, including Efficiency enchantments. It never
moves items out of your inventory or starts mining for you. Equal speeds keep
your current slot.

**Auto Tools options → Restore slot** is enabled by default and returns to the
previous slot when mining stops. Turn it off to keep the selected tool. A manual
hotbar change takes priority until you stop mining. Menus, paused gameplay,
inactive windows, item use, dead players and spectators suspend selection.

**Right Shift → RENDER → Xray** hides terrain and fluids in the chunk renderer,
reveals buried ores, and lights them while active. **Xray options**
filters diamonds, emeralds, ancient debris, and other ores. All filters start on.
Other ores include coal, iron, copper, gold, redstone, lapis and Nether ores,
including deepslate variants. Toggling Xray or changing an active filter rebuilds
the loaded terrain; switching it off restores normal rendering and respects your
Fullbright setting. Collision, block interaction and world data stay unchanged.
Xray displays the block information the server sends to the client.

Both enabled modules appear in the active-module list.

The local-world regression check runs with
`./gradlew -I tools/mining-features-test.gradle runClientGameTest --offline`.
It checks tool types, harvest tiers, restoration, manual selection, menus, buried
ore meshes, filters, visibility, terrain restoration and the actual menu controls.
