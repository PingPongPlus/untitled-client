# Integrated voice runtime

Build with `./gradlew build` (Java 25). No VoiceChat.jar, `voicechatJar` property,
extraction task, or separately installed LabyMod addon is required. Fabric API
and the project's other normal dependencies are still required.

The full standalone LabyMod voice client and protocol 6 are maintained under
`src/client/java/net/labymod/voice`, including authentication, encrypted UDP,
fragmentation/acknowledgements, proximity, channel, user-property, and moderation
packets. The Opus JNI wrapper is under `net/labymod/opus`. Package names and JNI
field/method signatures are preserved for compatibility with the native binaries.
These Java sources were recovered from the supplied addon and compile with the
client; they are not precompiled classes or a nested copy of the addon.

Opus libraries are packaged from `src/client/resources/native-binaries` and
RNNoise libraries from `src/client/resources/natives`. The resources are copied
unchanged from the original addon, SHA-256
`ca6f7d03eaab1b70bb9df7d464b6110e65f28b82bf3b370ca30b1e7674044cab`.
RNNoise supports the bundled Windows x86/x64, Linux x86/x64/aarch64 and macOS
x64/aarch64 libraries. Opus retains the platforms supplied in the addon (Windows
x86/x64, Linux x64 and macOS x64/arm64). Native binaries remain necessary;
independence from the addon does not mean a pure Java audio codec.

Open **Voice menu → Audio devices** to choose microphones/speakers, toggle
**Noise suppression (RNNoise)**, toggle the noise gate, adjust the cutoff, and
test the microphone locally. Apply saves changes; Cancel discards them.
RNNoise defaults to on and reduces background noise during speech. The gate
defaults to off and can additionally block pauses. Processing is 48 kHz mono:
two 480-sample RNNoise frames → 960-sample gate → microphone gain/clipping → Opus.
The test meter shows the processed level used by the gate. If suppression cannot
load, capture continues with an actionable status and the existing noise gate.
Mute, deafen, push-to-talk, per-player volume, proximity attenuation and saved
device selection remain handled by the current Minecraft client.

**Middle-click volume** in **Right Shift → PLAYER** is enabled by default.
Aim at a player and press the middle mouse button to open their volume window.
The slider applies and saves their volume immediately from 0–200%; **Mute**,
**Unmute**, and **Reset** use the same saved per-player level as the voice menu.
**Done** or Escape returns to gameplay. The window does not pause the game.
Disabling the toggle restores normal middle-click behavior; **All off** disables
it too. Players outside your normal crosshair reach or behind blocks cannot be
selected, and middle clicks in other menus are unaffected.

**Microphone mode** in the voice menu cycles between Push to talk (default),
Voice activation and Continuous. Voice activation always uses the microphone
cutoff and the gate's 200 ms hold, even with the optional gate switched off.
Continuous transmits while connected unless muted/deafened; the optional gate
still applies. Mute and deafen override all activation modes.

The old addon's LabyMod-specific screens, dependency injection, version-specific
Minecraft mixins and LabyMod API integration are replaced by this client's
existing Fabric UI/device/lifecycle implementation. Channel and moderation
protocol APIs are included in source; this migration does not add their original
LabyMod screens to the client. The standalone runtime does not connect to or
load LabyMod's Java API.

`./gradlew voiceSmokeTest` exercises native RNNoise noise reduction and cleanup,
Opus encode/decode, protocol packet roundtrips, encryption, PCM framing,
microphone metering/gating, and device enumeration without opening a microphone
or contacting the voice service. Actual authentication and listening/talking
require a Minecraft account and live multiplayer session.
