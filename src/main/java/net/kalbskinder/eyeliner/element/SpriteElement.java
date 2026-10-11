package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.style.Style;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An item rendered with an item display, e.g. {@code <sprite item="minecraft:diamond" size="0.3" />}.
 */
public final class SpriteElement extends Element {

    private final Material material;
    private final double size;
    private final boolean glint;

    public SpriteElement(@Nullable String id, Style style) {
        super("sprite", id, style, List.of());
        this.material = style.parse("item", v -> {
            final Material m = Material.fromKey(v.strip());
            if (m == null) throw new IllegalArgumentException("unknown item");
            return m;
        });
        this.size = style.positive("spritesize");
        this.glint = style.bool("glint");
    }

    public Material material() {
        return material;
    }

    /**
     * Edge length of the sprite in blocks.
     */
    public double size() {
        return size;
    }

    /**
     * Whether the item has the enchantment glint.
     */
    public boolean glint() {
        return glint;
    }
}
