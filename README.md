# Reverse Engineered Laby Voice Chat for Minecraft 26.2

Fabric client integration using the standalone protocol and Opus codec from the supplied LabyMod voicechat.jar. Minecraft stays at **26.2**, Fabric Loader at **0.19.5**, Fabric API at **0.160.0+26.2**, and Java at **25**. The addon UI and Minecraft 1.21.11 classes are not loaded.

## Install and use

Build output: `build/libs/untitled-1.0.1.jar`. Put this JAR in a Minecraft 26.2 Fabric instance with Fabric API. The built mod includes the required voice runtime; the original addon JAR is only a build input.

1. Sign into Minecraft with your Microsoft/Minecraft account and join a multiplayer server.
2. Press **V**, then **Enable voice**. Voice chat starts disabled on first use.
3. Hold **Caps Lock** to speak. Rebind this in Minecraft's Controls under LabyMod Voice Chat.
4. Press **M** to mute your microphone. The settings screen also provides deafen, master playback volume, microphone gain, proximity range, and individual player volume/mute.
5. If the connection fails, the screen and HUD show its status. Use **Reconnect** after addressing the cause.

Settings persist in `config/laby-voicechat.json`. Output and individual volume range from 0–200%; proximity range is 8–64 blocks. Muting a player is local playback control. Deafen also prevents microphone transmission. Opening a screen or losing window focus suspends push-to-talk. Disconnecting from the Minecraft server closes the voice connection and audio devices. When enabled, voice reconnects on the next multiplayer join.

Open **V → Audio devices** to select the input microphone and output speakers independently. Click each selector to cycle compatible devices, or choose **System default**. Hover for the full device name. **Refresh devices** rescans plugged-in hardware. **Apply** saves device identities and restarts audio while retaining the authenticated voice connection; **Cancel** leaves the previous selections unchanged. **Test microphone** temporarily pauses live audio and displays a local input meter for the selected microphone, even when voice is disabled or disconnected. It never stores or transmits captured samples. Closing the screen restores live audio. If the meter reports no signal, check the headset mute switch and the Windows recording-device input level. An unavailable saved device is reported instead of silently falling back to another microphone. Player rows refresh when reopening the screen or pressing Refresh. Nearby users must also be connected to LabyMod voice and identified as being on the same Minecraft server.

## Build

Place the supplied original addon at `libs/voicechat.jar`, then run:

```powershell
.\gradlew.bat build
```

Alternatively:

```powershell
.\gradlew.bat build '-PvoicechatJar=C:/path/to/voicechat.jar'
```

`libs/` and `.work/` are ignored by Git. The local supplied JAR has already been copied to `libs/voicechat.jar`. Another checkout needs its own copy. No decompiled addon source is committed.

The `voiceRuntime` task selects only `net/labymod/voice/**`, `net/labymod/opus/**`, and `native-binaries/**`. The final mod packages these components directly. The supplied file's SHA-256 is `ca6f7d03eaab1b70bb9df7d464b6110e65f28b82bf3b370ca30b1e7674044cab`.

## Implementation and verification

- Uses the supplied protocol version 6 client: encrypted UDP, reliable control packets, keepalives, Mojang session authentication, server identity, and visible-player updates.
- Fetches LabyMod's public encryption key from the endpoint in the supplied runtime and connects to `voice.labymod.net:8066`. The Minecraft session service authenticates the current account; no token is stored in the mod's settings.
- Microphone capture uses OpenAL at 48 kHz stereo16 and averages the two channels to mono, matching the supplied addon's ALCapture path. Java Sound is retained for output. Saved Java microphone selections migrate by matching the device name; if a saved device is shown as unavailable, select its OpenAL entry again. Audio is encoded as signed 16-bit mono, 960 samples per frame (20 ms), with Opus at 48 kbit/s. Every Opus frame has the addon's eight-byte big-endian sequence prefix.
- Per-speaker bounded queues sort sequence numbers, discard duplicate/late frames, and expire inactive decoders. Playback mixes speakers with proximity attenuation and clipping protection.
- The GUI uses Minecraft 26.2's `GuiGraphicsExtractor` and `minecraft.gui` APIs. Key mappings and the status/speaker HUD use Fabric APIs.

`build` runs `voiceSmokeTest`: protocol version, Mojang handshake serialization, server identity, AES roundtrip, exact sequence-header bytes, malformed frame rejection, audio packet roundtrip, PCM clipping, and native Opus encode/decode with a generated tone. Tests do not use the microphone, authenticate an account, or contact the voice service. The microphone meter is checked against silence and signed PCM fixtures. Stereo-to-mono tests cover left-only/right-only input and both full-scale signed limits. A separate local Alienware 510H hardware check received 150 frames in three seconds through OpenAL. A second check passed 50 real headset frames through the production capture, downmix, Opus encoder, sequence framing, and decoder: input peak 0.0748 and decoded peak 0.0848. No microphone audio was saved or transmitted during these checks. The HUD now distinguishes transmitting audio from sending silent microphone frames.

The development client was also launched successfully on Minecraft 26.2 with this mod. Its offline `Player` session cannot authenticate live voice; test live calls from a normally signed-in launcher.

**End-to-end verification still required:** join a multiplayer server with this mod and a second signed-in LabyMod client. Confirm mutual voice discovery and two-way speech, then check mute, deafen, player volume, range, reconnect, and server disconnect. Local capture/codec checks cannot establish live service compatibility or two-way remote playback.

This integration provides proximity voice. LabyMod private-channel management, moderation UI, noise suppression, and LabyMod-specific server API controls are not implemented. Native codec platform support is limited to binaries present in the supplied addon; Windows x64 is the locally tested platform.
