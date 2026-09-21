# Title-screen wallpaper

The title screen plays `14684159_3840_2160_30fps.mp4` (20.02 seconds) as a silent
loop at 960 × 540 and 24 fps. The runtime uses 481 preconverted JPEG frames,
so it works without FFmpeg or a native video decoder on the player's computer.
The source MP4 stays outside the project; it is not needed at runtime.

Two independently playing layers overlap for a two-second crossfade. The next
copy plays underneath while the outgoing copy becomes transparent, with opacity
updated every rendered frame rather than only at the video's 24 fps. The fade
finishes a quarter-second before the outgoing clip's endpoint. The incoming
layer keeps its playback clock when promoted, so it never jumps back to frame
zero. Adjust `CROSSFADE_SECONDS` in `AnimatedTitleBackground.java` to change the
overlap. No asset regeneration is needed.

`TitleScreenMixin` replaces the panorama call with `AnimatedTitleBackground`.
Each layer decodes JPEGs on a daemon worker with a four-frame prefetch queue and
its own dynamic GPU texture. The incoming layer preloads one second before the
fade. Playback uses elapsed time and skips frames if decoding falls behind.
Leaving the title screen releases both textures and workers; returning
restarts playback. A still from the new video displays while the first frame
loads, or if animation metadata cannot be loaded. No audio is played.

## Replace the video

Changing the MP4 alone does not update the animation. On macOS, regenerate it:

```sh
swift -module-cache-path /tmp/wallpaper-swift-cache tools/convert-wallpaper.swift \
  /path/to/your/video.mp4 \
  src/client/resources/assets/voicechat/textures/gui/wallpaper
./gradlew build
```

The converter writes `animation.properties` with the dimensions, frame rate and
frame count. It can also write to a new empty directory; use that when shortening
a video to avoid packaging old unused frames. The converter requires macOS;
the generated animation and Java player do not.

The checked-in conversion uses JPEG quality 0.85. Adjust `maximumSize`, `fps`,
and the JPEG quality in `convert-wallpaper.swift` to trade quality for file size.
