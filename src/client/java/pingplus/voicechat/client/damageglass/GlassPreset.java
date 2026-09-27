package pingplus.voicechat.client.damageglass;

/** Stable names are persisted; shader IDs are explicit rather than relying on enum ordering. */
public enum GlassPreset {
    CRYSTAL("Crystal", 0, 0), SMOKED("Smoked", 1, 15), FROSTED("Frosted", 2, 10),
    CHROME("Chrome", 3, 100), TINTED("Tinted", 4, 25);

    public final String label;
    public final int shaderId, reflectivity;
    GlassPreset(String label, int shaderId, int reflectivity) {
        this.label = label; this.shaderId = shaderId; this.reflectivity = reflectivity;
    }
    public static GlassPreset parse(String value) {
        try { return valueOf(value); }
        catch (IllegalArgumentException | NullPointerException e) { return CRYSTAL; }
    }
}
