# Custom crosshair

Open **Right Shift → HUD → Crosshair** to replace the normal aiming reticle.
The feature starts disabled. Its gear opens **Crosshair options**, including a
live preview that works with the feature off and outside a world.

- **Shape** cycles through Cross, Dot, Circle, and T.
- **Size**, **Gap**, and **Thickness** adjust the geometry in GUI pixels. Size is
  the arm length for Cross/T, circle radius before adding Gap, and dot width.
  Size ranges from 1–24. For a tiny reticle, use Size 1, Gap 0, and Thickness 1;
  switch off Outline and Attack cooldown ring for the smallest footprint.
- **Red**, **Green**, and **Blue** set the color. **Opacity (%)** ranges from 10–100.
- **Outline** adds a one-pixel black border. **Center dot** adds a dot to the reticle.
- **Attack cooldown ring** fills clockwise as attacks recharge. When enabled it
  replaces Minecraft's crosshair attack indicator; the hotbar indicator and the
  saved vanilla attack-indicator preference are unaffected.
- **Reset crosshair** restores the default appearance and keeps the main toggle.

The crosshair stays at the center of the screen and follows Minecraft's normal
first-person, hidden-HUD, spectator-target, and 3D debug crosshair rules. Turning
it off restores the vanilla reticle. **All off** disables it and its extra toggles
while preserving size, color, and other numeric settings.

Preferences are saved in `config/voicechat-crosshair.properties`. The crosshair
does not move or resize through the HUD editor; use its own settings instead.

Build with `./gradlew build --offline`. The focused local-world rendering and UI
check is `./gradlew -I tools/crosshair-test.gradle runClientGameTest --offline`.
It checks rendered shapes, outlines, center dot, cooldown progress, vanilla
restoration, camera/F1/debug/spectator rules, settings reload, scaled controls,
small-window scrolling, and All off. Screenshots are saved to
`build/run/clientGameTest/screenshots`.
