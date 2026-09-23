# Glass client UI

Open the ClickGUI with the existing client-controls key (Right Shift by default).
The interface renders at 60% of its logical size, including text and hit targets,
leaving room for more categories. `ClickGuiScreen.UI_SCALE` controls this independently
of Minecraft GUI scale. Mouse clicks, releases and dragging use the inverse transform.

Drag category headers to move panels. Click Player scale or Swap interval to expand
settings. Scroll to reach panels on smaller windows. Tab/Shift-Tab navigate controls;
keyboard focus scrolls into view. Escape or the configured key closes the screen.

All existing controls are retained: FPS, coordinates, hitboxes, body, both arms,
hand swapping, four scale values, and the 1–20 tick swap interval. Settings retain
their existing session lifetime; this redesign does not add disk persistence or
change gameplay logic. Panel positions survive resizing and settings expansion
within the current screen. Closing and reopening starts with the responsive layout.

## Implementation

- `ClickGuiScreen`: category layout, dragging, custom controls, input and animations.
- `GlassStyle`: palette, UI font, and resource-independent rounded geometry.
- `GlassButtonRenderer`: extracts antialiased GPU geometry for glass and tinted controls.
- `GlassBackdrop` / `GlassGuiRendererMixin`: one scene capture and shared separable blur
  per frame, reused across all panels and controls. Targets resize with the framebuffer.
- `glass_button.fsh`: rounded glass, screen-space refraction, frosted sampling, rim lighting.
- `glass_control.fsh`: antialiased switches, slider tracks/thumbs, and input surfaces.
- `GlassLoadingOverlayMixin`: changes startup/reload visuals while leaving vanilla
  completion callbacks, exception handling, progress calculation, and fade lifecycle intact.
- Loading/transition mixins retain original status text, progress and connection actions.

During resource reload, glass geometry falls back to built-in solid rendering. The
startup monogram and progress indicator never depend on the custom font or shaders.
The Rajdhani font is distributed under its bundled SIL Open Font License. The world
and vanilla HUD retain their normal Minecraft rendering; only the interface is restyled.

## Verification

`./gradlew build --offline` compiles/packages the mod and runs the voice smoke tests.
`./gradlew runClientGameTest --offline` launches an isolated client, exercises glass
controls, checks expansion, numeric validation, slider input, panel dragging, keyboard
focus, resizing and GUI scales, and performs a resource reload in a temporary world.
It also checks the close key and captures screenshots in
`build/run/clientGameTest/screenshots`. The test mod is not included in the production JAR.

Visual checks performed on macOS/OpenGL. Other GPU backends have not been verified.
