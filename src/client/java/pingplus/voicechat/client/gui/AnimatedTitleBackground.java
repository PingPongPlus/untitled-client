package pingplus.voicechat.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

/** Two independently playing layers, with a render-rate fade and bounded frame buffers. */
public final class  AnimatedTitleBackground implements AutoCloseable {
    private static final double CROSSFADE_SECONDS = 2.0;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimatedTitleBackground.class);
    private static final String RESOURCE_ROOT = "/assets/voicechat/textures/gui/wallpaper/";
    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath(
            "voicechat", "textures/gui/title_background.png");

    private final Minecraft minecraft;
    private int imageWidth;
    private int imageHeight;
    private int frameCount;
    private int fps;
    private CrossfadeLoop loop;
    private Layer outgoing;
    private Layer incoming;
    private long fadeStartedAt = -1;
    private boolean closed;

    public AnimatedTitleBackground(Minecraft minecraft) {
        this.minecraft = minecraft;
        try (InputStream input = resource("animation.properties")) {
            Properties properties = new Properties();
            properties.load(input);
            imageWidth = Integer.parseInt(properties.getProperty("width"));
            imageHeight = Integer.parseInt(properties.getProperty("height"));
            frameCount = Integer.parseInt(properties.getProperty("frames"));
            fps = Integer.parseInt(properties.getProperty("fps"));
            if (imageWidth <= 0 || imageHeight <= 0 || frameCount <= 0 || fps <= 0) {
                throw new IOException("Invalid background animation metadata");
            }
            loop = new CrossfadeLoop(frameCount / (double) fps, CROSSFADE_SECONDS);
            outgoing = new Layer(0);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Cannot load title animation; using static background", exception);
        }
    }

    /** All GPU operations and layer transitions run on the render thread. */
    public void draw(GuiGraphicsExtractor graphics, int width, int height) {
        if (closed) return;
        long now = System.nanoTime();
        if (outgoing == null) {
            drawTexture(graphics, FALLBACK, width, height, 960, 540, 1.0F);
            return;
        }
        outgoing.update(now);
        if (!outgoing.ready()) {
            drawTexture(graphics, FALLBACK, width, height, 960, 540, 1.0F);
            return;
        }
        outgoing.start(now);
        double age = outgoing.age(now);
        if (incoming == null && age >= loop.prepareAt()) {
            incoming = new Layer(1 - outgoing.slot);
        }
        if (incoming != null) {
            incoming.update(now); // Preload frame zero without starting its playback clock.
            if (fadeStartedAt < 0 && age >= loop.startsAt() && incoming.ready()) {
                incoming.start(now);
                fadeStartedAt = now;
            }
        }

        if (fadeStartedAt >= 0) {
            double elapsedFade = (now - fadeStartedAt) / 1_000_000_000.0;
            if (elapsedFade >= loop.duration()) {
                outgoing.close();
                outgoing = incoming; // Keep its existing clock and decoder: no restart at the seam.
                incoming = null;
                fadeStartedAt = -1;
            } else {
                // Opaque incoming underneath + fading outgoing above gives a true crossfade
                // without darkening the picture. Alpha changes at the game's render rate.
                drawTexture(graphics, incoming.id, width, height, imageWidth, imageHeight, 1.0F);
                drawTexture(graphics, outgoing.id, width, height, imageWidth, imageHeight,
                        loop.outgoingOpacity(elapsedFade));
                return;
            }
        }
        drawTexture(graphics, outgoing.id, width, height, imageWidth, imageHeight, 1.0F);
    }

    private static void drawTexture(GuiGraphicsExtractor graphics, Identifier id,
                                    int width, int height, int sourceWidth, int sourceHeight, float opacity) {
        double scale = Math.max(width / (double) sourceWidth, height / (double) sourceHeight);
        int drawWidth = (int) Math.ceil(sourceWidth * scale);
        int drawHeight = (int) Math.ceil(sourceHeight * scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, id,
                (width - drawWidth) / 2, (height - drawHeight) / 2,
                0.0F, 0.0F, drawWidth, drawHeight,
                sourceWidth, sourceHeight, sourceWidth, sourceHeight, ARGB.white(opacity));
    }

    private record Frame(int index, NativeImage pixels) {}

    private final class Layer implements AutoCloseable {
        private final int slot;
        private final Identifier id;
        private final ArrayBlockingQueue<Frame> frames = new ArrayBlockingQueue<>(4);
        private final Thread worker;
        private volatile boolean stopped;
        private volatile int wantedFrame;
        private DynamicTexture texture;
        private long startedAt = -1;

        Layer(int slot) {
            this.slot = slot;
            id = Identifier.fromNamespaceAndPath("voicechat", "dynamic/title_video_" + slot);
            worker = Thread.ofPlatform().daemon().name("title-background-decoder-" + slot)
                    .start(this::decode);
        }

        boolean ready() { return texture != null; }

        void start(long now) {
            if (startedAt < 0) startedAt = now;
        }

        double age(long now) {
            return startedAt < 0 ? 0 : (now - startedAt) / 1_000_000_000.0;
        }

        void update(long now) {
            wantedFrame = Math.min(frameCount - 1, (int) (age(now) * fps));
            Frame latest = null;
            Frame head;
            while ((head = frames.peek()) != null && head.index() <= wantedFrame) {
                Frame next = frames.poll();
                if (latest != null) latest.pixels().close();
                latest = next;
            }
            if (latest == null) return;
            if (texture == null) {
                texture = new DynamicTexture(() -> "Title background layer " + slot, latest.pixels());
                minecraft.getTextureManager().register(id, texture);
            } else {
                texture.setPixels(latest.pixels());
                texture.upload();
            }
        }

        private void decode() {
            try {
                for (int index = 0; index < frameCount && !stopped; index++) {
                    index = Math.max(index, wantedFrame); // Catch up after a slow frame or window pause.
                    NativeImage pixels = readFrame(index);
                    try {
                        // Bounded prefetch keeps decoding off the rendering path.
                        frames.put(new Frame(index, pixels));
                        pixels = null;
                    } finally {
                        if (pixels != null) pixels.close();
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Cannot decode title animation layer {}", slot, exception);
            } finally {
                // Also handles a producer completing just as close() drains the queue.
                if (stopped) discardFrames();
            }
        }

        private void discardFrames() {
            Frame frame;
            while ((frame = frames.poll()) != null) frame.pixels().close();
        }

        @Override
        public void close() {
            stopped = true;
            worker.interrupt();
            discardFrames();
            if (texture != null) {
                minecraft.getTextureManager().release(id);
                texture = null;
            }
        }
    }

    private NativeImage readFrame(int index) throws IOException {
        try (InputStream input = resource(String.format(Locale.ROOT, "frame_%04d.jpg", index))) {
            BufferedImage decoded = ImageIO.read(input);
            if (decoded == null || decoded.getWidth() != imageWidth || decoded.getHeight() != imageHeight) {
                throw new IOException("Invalid animation frame " + index);
            }
            int[] pixels = decoded.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
            NativeImage image = new NativeImage(imageWidth, imageHeight, false);
            try {
                for (int y = 0; y < imageHeight; y++) {
                    for (int x = 0; x < imageWidth; x++) {
                        image.setPixel(x, y, pixels[y * imageWidth + x]);
                    }
                }
                return image;
            } catch (RuntimeException | Error exception) {
                image.close();
                throw exception;
            }
        }
    }

    private static InputStream resource(String name) throws IOException {
        InputStream input = AnimatedTitleBackground.class.getResourceAsStream(RESOURCE_ROOT + name);
        if (input == null) throw new IOException("Missing animation resource: " + name);
        return input;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (outgoing != null) outgoing.close();
        if (incoming != null) incoming.close();
    }
}
