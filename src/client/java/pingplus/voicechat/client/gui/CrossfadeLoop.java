package pingplus.voicechat.client.gui;

/** Continuous transition timing, independent of the source video's frame rate. */
final class CrossfadeLoop {
    private final double fadeSeconds;
    private final double fadeStartsAt;

    CrossfadeLoop(double videoSeconds, double requestedFadeSeconds) {
        if (videoSeconds <= 0 || requestedFadeSeconds <= 0) throw new IllegalArgumentException();
        fadeSeconds = Math.min(requestedFadeSeconds, videoSeconds / 3.0);
        // Finish before the final frame, so the clip's endpoint is never visible.
        double endMargin = Math.min(0.25, videoSeconds / 10.0);
        fadeStartsAt = videoSeconds - fadeSeconds - endMargin;
    }

    double prepareAt() {
        return Math.max(0, fadeStartsAt - 1.0);
    }

    double startsAt() {
        return fadeStartsAt;
    }

    double duration() {
        return fadeSeconds;
    }

    float outgoingOpacity(double elapsedFadeSeconds) {
        double t = Math.clamp(elapsedFadeSeconds / fadeSeconds, 0.0, 1.0);
        return (float) (1.0 - t * t * (3.0 - 2.0 * t));
    }
}
