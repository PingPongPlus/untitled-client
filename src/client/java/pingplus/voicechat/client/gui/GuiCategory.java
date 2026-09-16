package pingplus.voicechat.client.gui;

/** Stable identifiers for navigation; display labels are never used as logic keys. */
public enum GuiCategory {
    ALL("All modules"),
    HUD("HUD"),
    PLAYER("Player"),
    APPEARANCE("Appearance");

    private final String label;

    GuiCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
