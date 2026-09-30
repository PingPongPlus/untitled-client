# Ping HUD

Open **Right Shift > HUD > Ping** to toggle the widget. **Ping options** contains
separate glass and edge toggles. All three preferences are saved in
`config/voicechat-info-hud.properties`.

Ping reads your entry in the server's player list without sending extra requests
and follows the server's update rate. Green is below 100 ms, amber is below
200 ms, and red is 200 ms or higher. Unavailable latency shows `-- ms`;
singleplayer shows `Local`.

Press **G** to move or resize it in the HUD editor. Position and scale use the
existing `config/voicechat-hud-layout.properties`. F1 hides the widget.
