package pingplus.voicechat.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.glfw.GLFW;
import pingplus.voicechat.client.PlayerSettings;
import pingplus.voicechat.client.VoicechatClient;
import pingplus.voicechat.client.gui.glass.*;
import java.util.*;
import java.util.function.*;

/** Floating category ClickGUI. All input and narration use native widget semantics. */
public final class ClickGuiScreen extends Screen {
    /** Render and hit-test in the same logical space, independently of Minecraft GUI scale. */
    public static final float UI_SCALE = 0.60f;
    private int canvasWidth, canvasHeight;
    private final FpsHud fpsHud;
    private final CoordinatesHud coordinatesHud;
    private final ArraylistHud arraylistHud;
    private final KeyMapping openGuiKey;
    private final List<Category> categories = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Boolean> expanded = new HashMap<>();
    private final Map<String, int[]> positions = new HashMap<>();
    private int scroll, maxScroll, panelWidth;
    private long opened = System.nanoTime(), closing;
    private float opacity = 1;
    private Category dragging;
    private double dragX, dragY;
    private KeyMapping pendingKey;
    private static final Identifier WALLPAPER = Identifier.fromNamespaceAndPath("voicechat", "textures/gui/title_background.png");

    public ClickGuiScreen(FpsHud fpsHud, KeyMapping openGuiKey, CoordinatesHud coordinatesHud, ArraylistHud arraylistHud) {
        super(Component.literal("Client controls"));
        this.fpsHud=fpsHud; this.openGuiKey=openGuiKey; this.coordinatesHud=coordinatesHud; this.arraylistHud=arraylistHud;
        expanded.put("Player scale",true); expanded.put("Swap interval",true); expanded.put("General",true); expanded.put("Arraylist options",true); expanded.put("Dock options",true);
    }

    @Override protected void init() {
        canvasWidth = (int)(width / UI_SCALE);
        canvasHeight = (int)(height / UI_SCALE);
        categories.clear(); entries.clear();
        int columns = Math.clamp((canvasWidth-16)/148,2,4);
        panelWidth = Math.min(140, (canvasWidth-24-(columns-1)*8)/columns);
        int startX = 16;
        Category hud = category("HUD", "On-screen information");
        toggle(hud,"Frame rate",fpsHud::isEnabled,fpsHud::toggle);
        disclosure(hud,"FPS options");
        if (expanded.getOrDefault("FPS options",false)) {
            toggle(hud,"FPS glass",fpsHud::isGlass,fpsHud::toggleGlass);
            toggle(hud,"FPS edges",fpsHud::isEdges,fpsHud::toggleEdges);
        }
        toggle(hud,"Ping",PingHud.INSTANCE::isEnabled,PingHud.INSTANCE::toggle);
        disclosure(hud,"Ping options");
        if (expanded.getOrDefault("Ping options",false)) {
            toggle(hud,"Ping glass",PingHud.INSTANCE::isGlass,PingHud.INSTANCE::toggleGlass);
            toggle(hud,"Ping edges",PingHud.INSTANCE::isEdges,PingHud.INSTANCE::toggleEdges);
        }
        toggle(hud,"Coordinates",coordinatesHud::isEnabled,coordinatesHud::toggle);
        disclosure(hud,"XYZ options");
        if (expanded.getOrDefault("XYZ options",false)) {
            toggle(hud,"XYZ glass",coordinatesHud::isGlass,coordinatesHud::toggleGlass);
            toggle(hud,"XYZ edges",coordinatesHud::isEdges,coordinatesHud::toggleEdges);
        }
        toggle(hud,"Player HP bars",()->PlayerSettings.playerHealthBar,()->PlayerSettings.playerHealthBar=!PlayerSettings.playerHealthBar);
        toggle(hud,"Spotify",pingplus.voicechat.client.spotify.SpotifySettings::enabled,pingplus.voicechat.client.spotify.SpotifySettings::toggle);
        disclosure(hud,"Spotify options");
        if (expanded.getOrDefault("Spotify options",false)) {
            toggle(hud,"Spotify edges",pingplus.voicechat.client.spotify.SpotifySettings::edges,pingplus.voicechat.client.spotify.SpotifySettings::toggleEdges);
            toggle(hud,"Music-reactive glass",pingplus.voicechat.client.spotify.SpotifySettings::musicGlass,pingplus.voicechat.client.spotify.SpotifySettings::toggleMusicGlass);
            add(hud,new EffectSlider(panelWidth-20,"Music tint",pingplus.voicechat.client.spotify.SpotifySettings::musicIntensity,
                    pingplus.voicechat.client.spotify.SpotifySettings::setMusicIntensity,100,1),26);
        }
        toggle(hud,"Arraylist",arraylistHud::isEnabled,arraylistHud::toggle);
        disclosure(hud,"Arraylist options");
        if (expanded.get("Arraylist options")) {
            toggle(hud,"Glass",arraylistHud::isGlass,arraylistHud::toggleGlass);
            toggle(hud,"Per-module boxes",arraylistHud::isRectangles,arraylistHud::toggleRectangles);
            toggle(hud,"Edges",arraylistHud::isEdges,arraylistHud::toggleEdges);
        }
        toggle(hud,"Scoreboard",ScoreboardHud.INSTANCE::isEnabled,ScoreboardHud.INSTANCE::toggle);
        disclosure(hud,"Scoreboard options");
        if (expanded.getOrDefault("Scoreboard options",false)) {
            toggle(hud,"Liquid glass",ScoreboardHud.INSTANCE::isGlass,ScoreboardHud.INSTANCE::toggleGlass);
            toggle(hud,"Glass edges",ScoreboardHud.INSTANCE::isEdges,ScoreboardHud.INSTANCE::toggleEdges);
        }
        toggle(hud,"Chat",ChatHud.INSTANCE::isEnabled,ChatHud.INSTANCE::toggle);
        disclosure(hud,"Chat options");
        if (expanded.getOrDefault("Chat options",false)) {
            toggle(hud,"Chat glass",ChatHud.INSTANCE::isGlass,ChatHud.INSTANCE::toggleGlass);
            toggle(hud,"Chat edges",ChatHud.INSTANCE::isEdges,ChatHud.INSTANCE::toggleEdges);
        }
        disclosure(hud,"Bars");
        if (expanded.getOrDefault("Bars",false)) {
            toggle(hud,"Health bar",()->!PlayerSettings.hideHealth,()->PlayerSettings.hideHealth=!PlayerSettings.hideHealth);
            toggle(hud,"Hunger bar",()->!PlayerSettings.hideHunger,()->PlayerSettings.hideHunger=!PlayerSettings.hideHunger);
            toggle(hud,"XP bar",()->!PlayerSettings.hideXp,()->PlayerSettings.hideXp=!PlayerSettings.hideXp);
            toggle(hud,"Locator bar",()->!PlayerSettings.hideLocator,()->PlayerSettings.hideLocator=!PlayerSettings.hideLocator);
        }
        toggle(hud,"Dock hotbar",()->PlayerSettings.dockHotbar,()->PlayerSettings.dockHotbar=!PlayerSettings.dockHotbar);
        disclosure(hud,"Dock options");
        if (expanded.getOrDefault("Dock options",false)) {
            add(hud,new DockScaleSlider(panelWidth-20),26);
            add(hud,new DockRadiusSlider(panelWidth-20),26);
            add(hud,new DockSizeSlider(panelWidth-20),26);
            toggle(hud,"Dock shelf",()->PlayerSettings.dockShelf,()->PlayerSettings.dockShelf=!PlayerSettings.dockShelf);
            toggle(hud,"Hotbar frame",()->PlayerSettings.dockFrames,()->PlayerSettings.dockFrames=!PlayerSettings.dockFrames);
            toggle(hud,"Auto hide",()->PlayerSettings.dockAutoHide,()->PlayerSettings.dockAutoHide=!PlayerSettings.dockAutoHide);
            add(hud,new DockAutoHideSlider(panelWidth-20),26);
        }
        toggle(hud,"AIR logo",LogoHud.INSTANCE::isEnabled,LogoHud.INSTANCE::toggle);
        disclosure(hud,"Logo options");
        if (expanded.getOrDefault("Logo options",false)) {
            toggle(hud,"Logo glass",LogoHud.INSTANCE::isGlass,LogoHud.INSTANCE::toggleGlass);
            toggle(hud,"Logo edges",LogoHud.INSTANCE::isEdges,LogoHud.INSTANCE::toggleEdges);
        }
        Category render = category("RENDER", "See the details");
        toggle(render,"Glass damage effect",pingplus.voicechat.client.damageglass.DamageGlassSettings::enabled,pingplus.voicechat.client.damageglass.DamageGlassSettings::toggleEnabled);
        toggle(render,"Impact ripple",pingplus.voicechat.client.damageglass.DamageGlassSettings::impactRipple,pingplus.voicechat.client.damageglass.DamageGlassSettings::toggleImpactRipple);
        toggle(render,"Death melt glass",pingplus.voicechat.client.damageglass.DamageGlassSettings::deathWave,pingplus.voicechat.client.damageglass.DamageGlassSettings::toggleDeathWave);
        disclosure(render,"Damage glass options");
        if (expanded.getOrDefault("Damage glass options",false)) {
            add(render,new Button(0,0,panelWidth-20,18,Component.literal("Preset: "+pingplus.voicechat.client.damageglass.DamageGlassSettings.preset().label),
                    b->{pingplus.voicechat.client.damageglass.DamageGlassSettings.cyclePreset();rebuildWidgets();},supplier->supplier.get()) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                    if(isHoveredOrFocused()) GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*.85f));
                    text(g,getMessage().getString(),getX()+4,getY()+4,GlassStyle.MUTED);
                }
            },18);
            toggle(render,"Always-on glass",pingplus.voicechat.client.damageglass.DamageGlassSettings::alwaysOn,pingplus.voicechat.client.damageglass.DamageGlassSettings::toggleAlwaysOn);
            add(render,new EffectSlider(panelWidth-20,"Reflectivity",
                    pingplus.voicechat.client.damageglass.DamageGlassSettings::reflectivity,
                    pingplus.voicechat.client.damageglass.DamageGlassSettings::setReflectivity,100,1),26);
            toggle(render,"Players",pingplus.voicechat.client.damageglass.DamageGlassSettings::players,pingplus.voicechat.client.damageglass.DamageGlassSettings::togglePlayers);
            toggle(render,"Mobs",pingplus.voicechat.client.damageglass.DamageGlassSettings::mobs,pingplus.voicechat.client.damageglass.DamageGlassSettings::toggleMobs);
        }
        toggle(render,"Hitboxes",()->PlayerSettings.hitboxes,()->PlayerSettings.hitboxes=!PlayerSettings.hitboxes);
        toggle(render,"Projectile preview",()->PlayerSettings.projectilePreview,()->PlayerSettings.projectilePreview=!PlayerSettings.projectilePreview);
        toggle(render,"Fullbright",()->PlayerSettings.fullbright,()->PlayerSettings.fullbright=!PlayerSettings.fullbright);
        toggle(render,"Xray",pingplus.voicechat.client.XrayFeature::enabled,pingplus.voicechat.client.XrayFeature::toggle);
        disclosure(render,"Xray options");
        if (expanded.getOrDefault("Xray options",false)) {
            for (var mineral : pingplus.voicechat.client.XrayFeature.Mineral.values())
                toggle(render,mineral.label,()->pingplus.voicechat.client.XrayFeature.selected(mineral),
                        ()->pingplus.voicechat.client.XrayFeature.toggle(mineral));
        }
        toggle(render,"Zoom",()->PlayerSettings.zoom,()->PlayerSettings.zoom=!PlayerSettings.zoom);
        add(render,new ZoomStrengthSlider(panelWidth-20),26);
        toggle(render,"Shoulder cam",()->PlayerSettings.shoulderCam,()->PlayerSettings.shoulderCam=!PlayerSettings.shoulderCam);
        add(render,new CornerSlider(panelWidth-20),26);
        add(render,new EffectSlider(panelWidth-20,"Glass blur",GlassEffectSettings::blurStep,GlassEffectSettings::setBlur),26);
        add(render,new EffectSlider(panelWidth-20,"Glass shadow",GlassEffectSettings::shadowStep,GlassEffectSettings::setShadow),26);
        toggle(render,"Weather Glass",WeatherGlassSettings::enabled,WeatherGlassSettings::toggleEnabled);
        disclosure(render,"Weather Glass options");
        if (expanded.getOrDefault("Weather Glass options",false)) {
            add(render,new Button(0,0,panelWidth-20,18,Component.literal("Weather mode: "+WeatherGlassSettings.mode().label),
                    b->{WeatherGlassSettings.cycleMode();rebuildWidgets();},supplier->supplier.get()) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                    if(isHoveredOrFocused()) GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*.85f));
                    text(g,getMessage().getString(),getX()+4,getY()+4,GlassStyle.MUTED);
                }
            },18);
            var alwaysWeather = new Toggle("Always active",WeatherGlassSettings::alwaysActive,WeatherGlassSettings::toggleAlwaysActive);
            alwaysWeather.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    "Rain and Frost stay active everywhere, including indoors and menus. Automatic always follows local weather and shelter.")));
            add(render,alwaysWeather,18);
            add(render,new EffectSlider(panelWidth-20,"Weather intensity",WeatherGlassSettings::intensity,WeatherGlassSettings::setIntensity,100,1),26);
            add(render,new Button(0,0,panelWidth-20,38,Component.empty(),b->{},supplier->supplier.get()) {
                { active=false; }
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                    text(g,"Always active:",getX()+4,getY()+2,GlassStyle.MUTED);
                    text(g,"Rain / Frost anywhere",getX()+4,getY()+14,GlassStyle.MUTED);
                    text(g,"Auto: weather + shelter",getX()+4,getY()+26,GlassStyle.MUTED);
                }
            },38);
        }
        Category player = category("PLAYER", "Shape your presence");
        toggle(player,"Body",()->PlayerSettings.mainBodyPart,()->PlayerSettings.mainBodyPart=!PlayerSettings.mainBodyPart);
        toggle(player,"Left arm",()->PlayerSettings.leftArm,()->PlayerSettings.leftArm=!PlayerSettings.leftArm);
        toggle(player,"Right arm",()->PlayerSettings.rightArm,()->PlayerSettings.rightArm=!PlayerSettings.rightArm);
        disclosure(player,"Player scale");
        if (expanded.get("Player scale")) {
            scale(player,"Width",PlayerSettings.xScale,v->PlayerSettings.xScale=v);
            scale(player,"Height",PlayerSettings.yScale,v->PlayerSettings.yScale=v);
            scale(player,"Depth",PlayerSettings.zScale,v->PlayerSettings.zScale=v);
            scale(player,"Head",PlayerSettings.xyzHeadscale,v->PlayerSettings.xyzHeadscale=v);
        }

        toggle(player, "Direction", ()->PlayerSettings.direction,()->PlayerSettings.direction = !PlayerSettings.direction);
        Category automation = category("AUTOMATION", "Small actions, effortless");
        toggle(automation,"Auto Tools",()->PlayerSettings.autoTools,()->{
            PlayerSettings.autoTools=!PlayerSettings.autoTools;
            if (!PlayerSettings.autoTools) pingplus.voicechat.client.AutoToolsFeature.finish(net.minecraft.client.Minecraft.getInstance());
        });
        disclosure(automation,"Auto Tools options");
        if (expanded.getOrDefault("Auto Tools options",false))
            toggle(automation,"Restore slot",()->PlayerSettings.autoToolsRestore,()->PlayerSettings.autoToolsRestore=!PlayerSettings.autoToolsRestore);
        toggle(automation,"Fast Place",()->PlayerSettings.fastPlace,()->PlayerSettings.fastPlace=!PlayerSettings.fastPlace);
        disclosure(automation,"Fast Place options");
        if (expanded.getOrDefault("Fast Place options",false)) {
            add(automation,new CombatSlider(panelWidth-20,"Place delay (ticks)",1,4,1,
                    pingplus.voicechat.client.FastPlaceFeature::delayTicks,v->PlayerSettings.fastPlaceDelayTicks=(int)v),26);
        }
        toggle(automation,"KillAura",()->PlayerSettings.killAura,()->{
            PlayerSettings.killAura=!PlayerSettings.killAura;
            if (!PlayerSettings.killAura) pingplus.voicechat.client.KillAuraFeature.clear();
        });
        disclosure(automation,"KillAura options");
        if (expanded.getOrDefault("KillAura options",false)) {
            toggle(automation,"Target players",()->PlayerSettings.killAuraPlayers,()->PlayerSettings.killAuraPlayers=!PlayerSettings.killAuraPlayers);
            toggle(automation,"Target mobs",()->PlayerSettings.killAuraMobs,()->PlayerSettings.killAuraMobs=!PlayerSettings.killAuraMobs);
            add(automation,new CombatSlider(panelWidth-20,"Range",1,4,.1,()->PlayerSettings.killAuraRange,v->PlayerSettings.killAuraRange=(float)v),26);
            add(automation,new CombatSlider(panelWidth-20,"Turn speed",45,540,15,()->PlayerSettings.killAuraTurnSpeed,v->PlayerSettings.killAuraTurnSpeed=(float)v),26);
        }
        toggle(automation,"Hand swap",()->PlayerSettings.handSwap,()->PlayerSettings.handSwap=!PlayerSettings.handSwap);
        disclosure(automation,"Swap interval");
        if (expanded.get("Swap interval")) add(automation,new SpeedSlider(panelWidth-20),26);
        Category skyblock = category("SKYBLOCK", "Hypixel skyblock helpers");
        disclosure(skyblock,"General");
        if (expanded.get("General")) {
            toggle(skyblock,"No particles",()->PlayerSettings.hideParticles,()->PlayerSettings.hideParticles=!PlayerSettings.hideParticles);
            toggle(skyblock,"No mob names",()->PlayerSettings.hideMobNames,()->{
                PlayerSettings.hideMobNames=!PlayerSettings.hideMobNames;
                rebuildWidgets();
            });
            // Health bar is only unlocked while mob names are hidden.
            if (PlayerSettings.hideMobNames) {
                toggle(skyblock,"Mob HP bar",()->PlayerSettings.mobHealthBar,()->PlayerSettings.mobHealthBar=!PlayerSettings.mobHealthBar);
                add(skyblock,new PercentSlider(panelWidth-20),26);
            }
            toggle(skyblock,"Slayer outline",()->PlayerSettings.slayerOutline,()->{
                PlayerSettings.slayerOutline=!PlayerSettings.slayerOutline;
                PlayerSettings.slayerBossHighlight=PlayerSettings.slayerOutline;
                VoicechatClient.syncSlayerCfg();
            });
            toggle(skyblock,"Boss yellow",()->PlayerSettings.slayerBoss,()->{
                PlayerSettings.slayerBoss=!PlayerSettings.slayerBoss;
                VoicechatClient.syncSlayerCfg();
            });
            toggle(skyblock,"Miniboss red",()->PlayerSettings.slayerMiniboss,()->{
                PlayerSettings.slayerMiniboss=!PlayerSettings.slayerMiniboss;
                VoicechatClient.syncSlayerCfg();
            });
            toggle(skyblock,"Boss lines",()->PlayerSettings.slayerLine,()->PlayerSettings.slayerLine=!PlayerSettings.slayerLine);
            toggle(skyblock,"Player outline",()->PlayerSettings.playerOutline,()->PlayerSettings.playerOutline=!PlayerSettings.playerOutline);
        }
        Category keys = category("KEYS", "Hotkeys, click then press");
        keyButton(keys, "GUI", VoicechatClient.openGuiKey());
        keyButton(keys, "HUD editor", VoicechatClient.hudEditorKey());
        keyButton(keys, "Voice", VoicechatClient.voiceMenuKey());
        keyButton(keys, "Talk", VoicechatClient.talkKey());
        keyButton(keys, "Mute", VoicechatClient.muteKey());
        keyButton(keys, "Zoom", VoicechatClient.zoomKey());
        keyButton(keys, "Shoulder cam", VoicechatClient.shoulderCamKey());
        keyButton(keys, "Killaura", VoicechatClient.killAuraKey());
        add(keys, new Button(0,0,panelWidth-20,18,Component.literal("Reset keys"),b->{resetKeys();rebuildWidgets();},supplier->supplier.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                if(isHoveredOrFocused()) GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*.85f));
                text(g,"Reset keys",getX()+4,getY()+4,GlassStyle.MUTED);
            }
        },18);
        // Stack shorter categories together, keeping the full Player settings column visible.
        int[] columnY = new int[columns];
        Arrays.fill(columnY, 30);
        int[] columnFor = columns == 2 ? new int[]{0,0,1,0,1,0}
                : columns == 3 ? new int[]{0,0,1,2,1,2} : new int[]{0,1,2,3,1,3};
        for (int i=0; i<categories.size(); i++) {
            Category c=categories.get(i);
            int col=columnFor[Math.min(i, columnFor.length-1)];
            c.x=startX+col*(panelWidth+8); c.y=columnY[col];
            columnY[col]+=c.height()+6;
            int[] saved=positions.get(c.title);
            if(saved!=null){
                c.x=Math.clamp(saved[0],8,Math.max(8,canvasWidth-panelWidth-8));
                c.y=Math.clamp(saved[1],28,Math.max(28,canvasHeight-18-c.height()));
            }
        }
        updateScroll(); layout();
    }
    private Category category(String name,String subtitle) { Category c=new Category(name,subtitle);categories.add(c);return c; }
    private void add(Category c,AbstractWidget w,int h) { entries.add(new Entry(c,addRenderableWidget(w),c.content));c.content+=h; }
    private void toggle(Category c,String name,BooleanSupplier state,Runnable action) { add(c,new Toggle(name,state,action),18); }
    private void disclosure(Category c,String name) {
        add(c,new Button(0,0,panelWidth-20,18,Component.literal(name),b->{expanded.put(name,!expanded.getOrDefault(name,false));rebuildWidgets();},supplier->supplier.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                if(isHoveredOrFocused()) GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*.85f));
                text(g,name,getX()+4,getY()+4,GlassStyle.MUTED);
                text(g,expanded.getOrDefault(name,false)?"-":"+",getRight()-13,getY()+4,GlassStyle.ACCENT);
            }
        },18);
    }
    private void scale(Category c,String label,float value,Consumer<Float> setter) {
        EditBox box=new EditBox(font,0,0,45,14,Component.literal(label+" scale")) {
            @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
                text(g,label,getX()-(panelWidth-80),getY()+3,GlassStyle.MUTED);
                GlassButtonRenderer.control(g,getX()-5,getY()-2,getWidth()+10,18,GlassStyle.alpha(isFocused()?0xFF555555:0xFF303030,opacity));
                g.nextStratum();super.extractWidgetRenderState(g,mx,my,dt);
            }
        };
        box.addFormatter((text,index)->net.minecraft.util.FormattedCharSequence.forward(text,GlassStyle.FONT));
        box.setBordered(false);box.setTextShadow(false);box.setTextColor(GlassStyle.TEXT);box.setMaxLength(1000);box.setValue(Float.toString(value));
        box.setResponder(text->{
            try {float n=Float.parseFloat(text);boolean valid=Float.isFinite(n)&&n>=-100000&&n<=100000;
                box.setTextColor(valid?GlassStyle.TEXT:0xFFFF9B99);if(valid)setter.accept(n);
            }catch(NumberFormatException ignored){box.setTextColor(0xFFFF9B99);}
        });
        add(c,box,20);
    }
    private void keyButton(Category c, String name, KeyMapping mapping) {
        String label = mapping == null ? name + ": -"
            : mapping == pendingKey ? name + ": press..." : name + ": " + mapping.getTranslatedKeyMessage().getString();
        add(c, new Button(0,0,panelWidth-20,18,Component.literal(label),b->{pendingKey=mapping;rebuildWidgets();},supplier->supplier.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                if(isHoveredOrFocused() || mapping == pendingKey) GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*.85f));
                text(g,mapping == pendingKey ? name + ": press..." : name + ": " + (mapping == null ? "-" : mapping.getTranslatedKeyMessage().getString()),getX()+4,getY()+4,mapping == pendingKey ? GlassStyle.ACCENT : GlassStyle.MUTED);
            }
        },18);
    }
    private void resetKeys() {
        if (VoicechatClient.openGuiKey() != null) VoicechatClient.openGuiKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT));
        if (VoicechatClient.hudEditorKey() != null) VoicechatClient.hudEditorKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G));
        if (VoicechatClient.voiceMenuKey() != null) VoicechatClient.voiceMenuKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_V));
        if (VoicechatClient.talkKey() != null) VoicechatClient.talkKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_CAPS_LOCK));
        if (VoicechatClient.muteKey() != null) VoicechatClient.muteKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M));
        if (VoicechatClient.zoomKey() != null) VoicechatClient.zoomKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C));
        if (VoicechatClient.shoulderCamKey() != null) VoicechatClient.shoulderCamKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R));
        if (VoicechatClient.killAuraKey() != null) VoicechatClient.killAuraKey().setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));
        pendingKey = null;
        saveKeys();
    }
    private void saveKeys() {
        KeyMapping.resetMapping();
        try { minecraft.options.save(); } catch (Exception ignored) {}
    }
    private void updateScroll() {
        maxScroll=Math.max(0,categories.stream().mapToInt(c->c.y+c.height()).max().orElse(0)-(canvasHeight-16));
        scroll=Math.clamp(scroll,0,maxScroll);
    }
    private void layout() {
        for(Entry e:entries){
            e.widget.setX(e.category.x+(e.widget instanceof EditBox?panelWidth-65:10));
            e.widget.setY(e.category.y+22+e.row-scroll);
        }
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt) {
        if(minecraft.level==null){
            double s=Math.max(width/1672.0,height/941.0);int w=(int)Math.ceil(1672*s),h=(int)Math.ceil(941*s);
            g.blit(RenderPipelines.GUI_TEXTURED,WALLPAPER,(width-w)/2,(height-h)/2,0,0,w,h,1672,941,1672,941);
        }
        g.fill(0,0,width,height,GlassStyle.alpha(0x50000000,opacity));
        pingplus.voicechat.client.gui.glass.GlassRain.draw(g);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        opacity=closing==0?Math.clamp((System.nanoTime()-opened)/220_000_000f,0,1):1-Math.clamp((System.nanoTime()-closing)/160_000_000f,0,1);
        extractBackground(g,mx,my,dt);
        g.pose().pushMatrix();
        g.pose().scale(UI_SCALE);
        mx = (int)(mx / UI_SCALE);
        my = (int)(my / UI_SCALE);
        text(g,"UNTITLED",16,13,GlassStyle.TEXT);text(g,"/  CONTROL CENTER",74,13,GlassStyle.MUTED);
        text(g,"ESC  /  CLOSE",canvasWidth-78,13,GlassStyle.MUTED);
        g.enableScissor(0,26,canvasWidth,canvasHeight-16);
        for(Category c:categories){
            int y=c.y-scroll;
            float light=.15f+.55f*(1-Math.clamp((float)Math.hypot(mx-c.x-panelWidth*.3,my-y-12)/200,0,1));
            GlassStyle.surface(g,c.x,y,panelWidth,c.height(),opacity,light);
        }
        g.nextStratum();
        for(Category c:categories){
            int y=c.y-scroll;
            text(g,c.title,c.x+10,y+6,GlassStyle.TEXT);
            GlassButtonRenderer.control(g,c.x+10,y+18,panelWidth-20,1,GlassStyle.alpha(0xFFBBBBBB,opacity*.5f));
            for(Entry e:entries)if(e.category==c){e.widget.setAlpha(opacity);e.widget.extractRenderState(g,mx,my,dt);}
        }
        g.disableScissor();
        text(g,"DRAG HEADERS  /  EXPAND SETTINGS",16,canvasHeight-13,GlassStyle.MUTED);
        if(maxScroll>0)text(g,"SCROLL",canvasWidth-48,canvasHeight-13,GlassStyle.ACCENT);
        g.pose().popMatrix();
    }
    private void text(GuiGraphicsExtractor g,String value,int x,int y,int color){g.text(font,GlassStyle.label(value),x,y,GlassStyle.alpha(color,opacity),false);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll=Math.clamp(scroll-(int)(vertical*28),0,maxScroll);layout();return true;}
    private MouseButtonEvent logicalMouse(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / UI_SCALE, event.y() / UI_SCALE, event.buttonInfo());
    }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x / UI_SCALE, y / UI_SCALE); }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if (pendingKey != null) {
            try {
                pendingKey.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
                saveKeys();
            } catch (Exception ignored) {}
            pendingKey = null;
            rebuildWidgets();
            return true;
        }
        MouseButtonEvent e = logicalMouse(event);
        if(openGuiKey != null && openGuiKey.matchesMouse(e)){onClose();return true;}
        if(closing!=0)return true;
        if(e.y()<26){if(e.x()>canvasWidth-85)onClose();return true;}
        if(e.y()>canvasHeight-16)return true;
        for(int i=categories.size()-1;i>=0;i--){Category c=categories.get(i);int y=c.y-scroll;
            if(e.button()==0&&e.x()>=c.x&&e.x()<c.x+panelWidth&&e.y()>=y&&e.y()<y+20){dragging=c;dragX=e.x()-c.x;dragY=e.y()-y;return true;}
            if(e.x()>=c.x&&e.x()<c.x+panelWidth&&e.y()>=y&&e.y()<y+c.height()){
                for(Entry entry:entries)if(entry.category==c&&entry.widget.mouseClicked(e,twice)){setFocused(entry.widget);setDragging(true);return true;}
                return true;
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        MouseButtonEvent e = logicalMouse(event);
        dx /= UI_SCALE; dy /= UI_SCALE;
        if(dragging!=null){dragging.x=Math.clamp((int)(e.x()-dragX),8,Math.max(8,canvasWidth-panelWidth-8));dragging.y=Math.clamp((int)(e.y()-dragY)+scroll,28,Math.max(28,canvasHeight-18-dragging.height()));positions.put(dragging.title,new int[]{dragging.x,dragging.y});updateScroll();layout();return true;}
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging!=null){dragging=null;return true;}return super.mouseReleased(logicalMouse(e));}
    @Override public boolean keyPressed(KeyEvent e){
        if (pendingKey != null) {
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
                pendingKey = null;
                rebuildWidgets();
                return true;
            }
            try {
                pendingKey.setKey(InputConstants.getKey(e));
                saveKeys();
            } catch (Exception ignored) {}
            pendingKey = null;
            rebuildWidgets();
            return true;
        }
        if(openGuiKey != null && openGuiKey.matches(e)){onClose();return true;}
        boolean handled=super.keyPressed(e);
        if(getFocused() instanceof AbstractWidget w){
            if(w.getY()<28)scroll=Math.max(0,scroll+w.getY()-30);
            if(w.getBottom()>canvasHeight-18)scroll=Math.min(maxScroll,scroll+w.getBottom()-(canvasHeight-20));
            layout();
        }
        return handled;
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(closing==0)closing=System.nanoTime();}
    @Override public void tick(){if(closing!=0&&System.nanoTime()-closing>=160_000_000L)minecraft.gui.setScreen(null);}

    private static final class Category {
        final String title,subtitle;int x,y,content;
        Category(String title,String subtitle){this.title=title;this.subtitle=subtitle;}
        int height(){return 22+content;}
    }
    private record Entry(Category category,AbstractWidget widget,int row){}
    private final class Toggle extends Button {
        private final String label;private final BooleanSupplier state;private float position,hover;private long last=System.nanoTime();
        Toggle(String label,BooleanSupplier state,Runnable action){super(0,0,panelWidth-20,18,Component.literal(label),b->action.run(),DEFAULT_NARRATION);this.label=label;this.state=state;position=state.getAsBoolean()?1:0;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
            long now=System.nanoTime();float step=(float)(1-Math.exp(-16*Math.min(.1,(now-last)/1e9)));last=now;
            position+=((state.getAsBoolean()?1:0)-position)*step;hover+=((isHoveredOrFocused()?1:0)-hover)*step;
            if(hover>.01)GlassButtonRenderer.control(g,getX(),getY(),width,height,GlassStyle.alpha(0xFF858585,opacity*hover*.85f));
            text(g,label,getX()+4,getY()+4,state.getAsBoolean()?GlassStyle.TEXT:GlassStyle.MUTED);
            int x=getRight()-30,y=getY()+3;
            int r=(int)(55+position*105), green=r,b=r;
            GlassButtonRenderer.control(g,x,y,26,13,GlassStyle.alpha(0xFF000000|(r<<16)|(green<<8)|b,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,x+2+Math.round(position*13),y+2,9,9,GlassStyle.alpha(0xFFF3F3F3,opacity));
        }
        @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage(){return Component.literal(label+(state.getAsBoolean()?", on":", off"));}
    }
    private final class CornerSlider extends AbstractSliderButton {
        CornerSlider(int w){super(0,0,w,26,Component.literal("Corner radius"),GlassCornerSettings.getScale()/GlassCornerSettings.MAX);updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Corners   %d %%",Math.round(GlassCornerSettings.getScale()*100))));}
        @Override protected void applyValue(){GlassCornerSettings.setScale((float)(value*GlassCornerSettings.MAX));updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class EffectSlider extends AbstractSliderButton {
        private final String label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final int steps, percentPerStep;
        EffectSlider(int w,String label,IntSupplier getter,IntConsumer setter){
            this(w,label,getter,setter,GlassEffectSettings.STEPS,25);
        }
        EffectSlider(int w,String label,IntSupplier getter,IntConsumer setter,int steps,int percentPerStep){
            super(0,0,w,26,Component.literal(label),getter.getAsInt()/(double)steps);
            this.label=label;this.getter=getter;this.setter=setter;this.steps=steps;this.percentPerStep=percentPerStep;updateMessage();
        }
        @Override protected void updateMessage(){setMessage(Component.literal(label+"   "+(getter.getAsInt()*percentPerStep)+" %"));}
        @Override protected void applyValue(){setter.accept((int)Math.round(value*steps));updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class CombatSlider extends AbstractSliderButton {
        private final String label;
        private final double min,max,step;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        CombatSlider(int w,String label,double min,double max,double step,DoubleSupplier getter,DoubleConsumer setter) {
            super(0,0,w,26,Component.literal(label),(getter.getAsDouble()-min)/(max-min));
            this.label=label;this.min=min;this.max=max;this.step=step;this.getter=getter;this.setter=setter;updateMessage();
        }
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"%s   %.1f",label,getter.getAsDouble())));}
        @Override protected void applyValue(){setter.accept(Math.clamp(min+Math.round(value*(max-min)/step)*step,min,max));updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(GlassStyle.TEXT,opacity));
        }
    }
    private final class SpeedSlider extends AbstractSliderButton {
        SpeedSlider(int w){super(0,0,w,26,Component.literal("Hand swap interval"),Math.clamp((PlayerSettings.handSwapIntervalTicks-1)/19.0,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Interval   %.2f s",PlayerSettings.handSwapIntervalTicks/20.0)));}
        @Override protected void applyValue(){PlayerSettings.handSwapIntervalTicks=1+(int)Math.round(value*19);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class PercentSlider extends AbstractSliderButton {
        PercentSlider(int w){super(0,0,w,26,Component.literal("Bar opacity"),Math.clamp(PlayerSettings.mobBarOpacity,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Bar opacity   %d%%",Math.round(value*100))));}
        @Override protected void applyValue(){PlayerSettings.mobBarOpacity=(float)value;updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class DockSizeSlider extends AbstractSliderButton {
        DockSizeSlider(int w){super(0,0,w,26,Component.literal("Hotbar size"),Math.clamp((PlayerSettings.dockSize-0.5)/1.5,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Size   %d %%",Math.round(PlayerSettings.dockSize*100))));}
        @Override protected void applyValue(){PlayerSettings.dockSize=(float)(0.5+value*1.5);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class DockAutoHideSlider extends AbstractSliderButton {
        DockAutoHideSlider(int w){super(0,0,w,26,Component.literal("Hide delay"),Math.clamp((PlayerSettings.dockAutoHideSeconds-5)/115.0,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Hide after   %d s",PlayerSettings.dockAutoHideSeconds)));}
        @Override protected void applyValue(){PlayerSettings.dockAutoHideSeconds=5+(int)Math.round(value*115);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class ZoomStrengthSlider extends AbstractSliderButton {
        ZoomStrengthSlider(int w){super(0,0,w,26,Component.literal("Zoom strength"),Math.clamp((PlayerSettings.zoomStrength-1)/7.0,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Zoom   %.1fx",1+PlayerSettings.zoomStrength)));}
        @Override protected void applyValue(){PlayerSettings.zoomStrength=(float)(1+value*7);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class DockScaleSlider extends AbstractSliderButton {
        DockScaleSlider(int w){super(0,0,w,26,Component.literal("Dock magnification"),Math.clamp(PlayerSettings.dockMaxScale/2.5,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Magnification   %d %%",Math.round(PlayerSettings.dockMaxScale*100))));}
        @Override protected void applyValue(){PlayerSettings.dockMaxScale=(float)(value*2.5);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
    private final class DockRadiusSlider extends AbstractSliderButton {
        DockRadiusSlider(int w){super(0,0,w,26,Component.literal("Dock falloff"),Math.clamp((PlayerSettings.dockRadius-0.5)/7.5,0,1));updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format(Locale.ROOT,"Falloff   %.1f slots",PlayerSettings.dockRadius)));}
        @Override protected void applyValue(){PlayerSettings.dockRadius=(float)(0.5+value*7.5);updateMessage();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            text(g,getMessage().getString(),getX()+4,getY()+3,GlassStyle.MUTED);
            int x=getX()+4,y=getY()+19,length=width-8;
            GlassButtonRenderer.control(g,x,y,length,3,GlassStyle.alpha(0xFF555555,opacity));
            GlassButtonRenderer.control(g,x,y,Math.max(1,(int)(length*value)),3,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
            g.nextStratum();
            GlassButtonRenderer.control(g,getX()+(int)Math.round((width-8)*value),y-3,8,9,GlassStyle.alpha(isHoveredOrFocused()?0xFFFFFFFF:GlassStyle.TEXT,opacity));
        }
    }
}
