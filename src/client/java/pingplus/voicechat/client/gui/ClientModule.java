package pingplus.voicechat.client.gui;

/** Add a module here, then supply its output in ClientHud. */
public enum ClientModule {
    FPS("FPS counter", "Live rendering performance", GuiCategory.HUD, "FPS"),
    COORDINATES("Coordinates", "Your position in the world", GuiCategory.HUD, "XYZ"),
    DIRECTION("Direction", "Keep your bearings", GuiCategory.HUD, "N"),
    MOVEMENT("Movement", "Sprint and sneak status", GuiCategory.PLAYER, ">>"),
    CLOCK("Clock", "Local time at a glance", GuiCategory.PLAYER, "12"),
    SESSION("Session timer", "Time in the current world", GuiCategory.PLAYER, "T");

    public final String title;
    public final String description;
    public final GuiCategory category;
    public final String icon;

    ClientModule(String title, String description, GuiCategory category, String icon) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.icon = icon;
    }
}
