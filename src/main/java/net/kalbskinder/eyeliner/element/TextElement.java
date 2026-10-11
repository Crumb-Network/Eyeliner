package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.style.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A text label. The content is MiniMessage, e.g. {@code <text>Hello <bold><player></bold></text>}.
 */
public final class TextElement extends Element {

    private final String content;

    public TextElement(@Nullable String id, Style style, String content) {
        super("text", id, style, List.of());
        this.content = content;
    }

    /**
     * MiniMessage source of the label.
     */
    public String content() {
        return content;
    }
}
