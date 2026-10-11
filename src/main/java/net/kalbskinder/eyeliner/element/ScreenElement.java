package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.style.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The root panel. Its size is fixed (in blocks) and it decides where the menu is placed.
 */
public final class ScreenElement extends Element {

    private final double distance;
    private final double closeDistance;
    private final double reach;
    private final int brightness;

    public ScreenElement(@Nullable String id, Style style, List<Element> children) {
        super("screen", id, style, children);
        this.distance = style.positive("distance");
        this.closeDistance = style.nonNegative("closedistance");
        this.reach = style.positive("reach");
        this.brightness = style.parse("brightness", v -> {
            final int level = Integer.parseInt(v.strip());
            if (level != -1 && (level < 0 || level > 15)) throw new IllegalArgumentException("expected 0..15, or -1 for world light");
            return level;
        });
    }

    /**
     * Distance in blocks between the player's eyes and the screen when opened in front of a player.
     */
    public double distance() {
        return distance;
    }

    /**
     * The menu closes automatically once the player is further away than this. 0 disables it.
     */
    public double closeDistance() {
        return closeDistance;
    }

    /**
     * Maximum distance at which buttons can be hovered and clicked.
     */
    public double reach() {
        return reach;
    }

    /**
     * Light level the UI is rendered with (0..15), or -1 to use the world's light.
     */
    public int brightness() {
        return brightness;
    }
}
