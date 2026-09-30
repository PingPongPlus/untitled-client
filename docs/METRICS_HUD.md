# CPS and speed

Right Shift → HUD has independent **CPS** and **Speed** toggles. Their gears open
glass and edge options, using the global glass tint. Both are enabled by default;
**All off** disables them and their options. Preferences survive restarts.

- **CPS** shows left (`L`) and right (`R`) mouse presses during the last second.
  Holding a button or automatic block placement does not add physical clicks.
  Clicks in menus, inventories and the HUD editor are ignored.
- **Speed** shows horizontal movement in blocks per second (`b/s`), averaged over
  a quarter second. Jump height is excluded. Pauses, disconnects, world changes,
  and large position corrections reset the reading.

Press **G** to drag or resize each widget independently. Right-click a widget in
the editor to restore its default position and size. Layout uses the existing
`voicechat-hud-layout.properties`; toggles use `voicechat-metrics-hud.properties`.
