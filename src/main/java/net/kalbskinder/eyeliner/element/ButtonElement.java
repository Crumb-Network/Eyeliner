package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.action.ClickType;
import net.kalbskinder.eyeliner.style.Style;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A clickable container. {@code onClick} fires for any click, {@code onLeftClick}/{@code onRightClick}
 * take precedence for their respective mouse button.
 */
public final class ButtonElement extends Element {

    private final @Nullable String onClick;
    private final @Nullable String onLeftClick;
    private final @Nullable String onRightClick;
    private final @Nullable Sound sound;

    public ButtonElement(@Nullable String id, Style style, List<Element> children) {
        super("button", id, style, children);
        this.onClick = actionKey(style, "onclick");
        this.onLeftClick = actionKey(style, "onleftclick");
        this.onRightClick = actionKey(style, "onrightclick");
        final Key soundKey = style.parse("sound", v -> {
            final String s = v.strip().toLowerCase(Locale.ROOT);
            if (s.equals("none") || s.equals("false") || s.isEmpty()) return null;
            return Key.key(s);
        });
        final double volume = style.nonNegative("soundvolume");
        final double pitch = style.parse("soundpitch", v -> {
            final double d = Double.parseDouble(v.strip());
            if (!(d >= 0.5 && d <= 2)) throw new IllegalArgumentException("expected 0.5..2");
            return d;
        });
        this.sound = soundKey == null || volume == 0 ? null
                : Sound.sound(soundKey, Sound.Source.MASTER, (float) volume, (float) pitch);
    }

    private static @Nullable String actionKey(Style style, String key) {
        if (!style.has(key)) return null;
        final String value = style.raw(key).strip();
        return value.isEmpty() ? null : value;
    }

    /**
     * The action key to run for the given click, or null if this button does nothing for it.
     */
    public @Nullable String actionFor(ClickType type) {
        final String specific = type == ClickType.LEFT ? onLeftClick : onRightClick;
        return specific != null ? specific : onClick;
    }

    public boolean hasAction() {
        return onClick != null || onLeftClick != null || onRightClick != null;
    }

    /**
     * Sound played to the player when clicked ({@code sound}, {@code soundVolume}, {@code soundPitch}), or null for none.
     * Any sound key works, including custom ones from a resource pack.
     */
    public @Nullable Sound sound() {
        return sound;
    }
}
