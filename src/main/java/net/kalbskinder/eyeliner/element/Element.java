package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.style.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An immutable, validated UI element. A parsed menu is a tree of these and can be shown to any number of players.
 */
public abstract class Element {

    private final String tag;
    private final @Nullable String id;
    private final Style style;
    private final List<Element> children;

    protected Element(String tag, @Nullable String id, Style style, List<Element> children) {
        this.tag = tag;
        this.id = id;
        this.style = style;
        this.children = List.copyOf(children);
    }

    public String tag() {
        return tag;
    }

    /**
     * Optional {@code id="..."} attribute, used to find elements at runtime (e.g. to change text).
     */
    public @Nullable String id() {
        return id;
    }

    public Style style() {
        return style;
    }

    public List<Element> children() {
        return children;
    }
}
