# Smooth KillAura

Enable **AUTOMATION → KillAura** in the control center. It starts disabled.
Expand **KillAura options** for player/mob filters, range (1–4 blocks), and turn speed
(45–540 degrees per second). Actual reach is capped by the player's normal reach
and held item's attack range. Settings last for the current client session.

The camera eases toward one target per frame, keeping that target until it becomes
invalid. Each swing adds a random 1–2 tick pause (50–100 ms at 20 TPS) after the
normal attack cooldown. Attacks require the aim ray to hit the
target, and cannot pass through solid blocks or another pickable entity.
Dead, invisible, allied, spectator, creative-player and same-vehicle targets are
excluded. Menus, non-blocking item use, death and leaving the world suspend targeting
and attacks. Blocking does not suspend KillAura; eating, drinking and charging items do.
The mob filter includes passive mobs. KillAura appears in the active-module list.

Validation: `gradlew aimSmoothingTest --offline` tests rotation math.
`gradlew -I tools/killaura-test.gradle runClientGameTest --offline` runs an isolated
local-world regression for disabled state, mob filtering, menus, visible aiming,
and server-confirmed damage, with automatic multiplayer joining disabled.
