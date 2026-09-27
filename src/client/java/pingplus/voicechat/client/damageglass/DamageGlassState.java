package pingplus.voicechat.client.damageglass;

/** Per-render-state data; never shared between entities or deferred model draws. */
public interface DamageGlassState {
    int voicechat$damageGlass();
    void voicechat$damageGlass(int packed);
}
