Minecraft client with LabyMod-compatible voice chat, Spotify, liquid glass UI,
editable HUD widgets, ping, zoom, shoulder camera, and cosmetic player rendering.

This is the `legit` branch. `master` preserves the full feature set. This branch
removes entity health bars, KillAura, hand-swap automation, projectile preview,
Skyblock helpers and outlines, and Fullbright. Notification cards are removed
from both branches. Your own vanilla health and hunger bars remain available.

Build with `./gradlew build --offline`; the client JAR is
`build/libs/untitled-legit-1.0.1.jar`. Switch to the full client with `git switch master`,
or return to this version with `git switch legit`.

The isolated client check is `./gradlew -I tools/legit-test.gradle runClientGameTest --offline`.

Voice chat builds and runs without VoiceChat.jar. The protocol, Opus codec wrapper,
and native audio libraries are included in the client. RNNoise background noise
suppression and the noise gate are configurable in **Voice menu → Audio devices**.
See [voice setup and runtime details](docs/VOICE.md).

Ping is available under **Right Shift → HUD**. Move and resize it with **G**.
Its enabled state, glass background, and edges are saved. See [ping controls](docs/PING.md).

Music-reactive glass: enable **HUD → Spotify options → Music-reactive glass** in the control center.
The **Music tint** slider adjusts subtle album-art colors on glass panels, buttons, and the glass hotbar.
Colors transition smoothly between covers and fade out when playback pauses, artwork is missing, or the feature is disabled.
The setting is saved and defaults to off. It works with the Spotify HUD hidden, using the existing Windows Spotify media connection.
This feature follows album artwork; it does not capture audio or provide a beat visualizer.
