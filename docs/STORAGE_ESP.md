# Storage ESP

On the full `master` client, open **Right Shift → RENDER → Storage ESP**.
It starts disabled. Colored boxes highlight storage through solid terrain:

- Gold: normal and trapped chests, including both halves of double chests.
- Orange: barrels.
- Purple: shulker boxes of every color.
- Cyan: Ender chests.

Click its gear to open **Storage ESP options**, filter each type, turn on **Filled boxes**, or
change **Storage range** from 16 to 128 blocks. The default range is 64 blocks
and outlines start without fill. Settings last for the current session.

Storage detection uses the block entities in already loaded chunks. It does not
request distant chunks or change blocks, container contents, or interaction.
Detection refreshes every five client ticks. Drawing checks current block state
and distance, so removed containers and out-of-range targets lose their outline
immediately. Changing filters or range refreshes detection immediately.
Menus and the F1 hidden-HUD mode suppress drawing. Disabled features and world
changes discard cached targets. Storage ESP appears in the active-module list.
The `legit` branch excludes Storage ESP.

The isolated local-world test is
`./gradlew -I tools/storage-esp-test.gradle runClientGameTest --offline`.
It checks storage behind a solid wall, storage types, double chests, range and
filters, visible colors in normal gameplay frames, optional fill, F1 hiding,
removal, and menu controls.
