package net.kalbskinder.eyeliner.layout;

import net.kalbskinder.eyeliner.element.Element;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An element with its final position. Coordinates are in blocks, relative to the top-left corner of the
 * screen, x pointing right and y pointing down (like CSS). The box is the border box (excludes margin).
 *
 * @param depth nesting depth, used to layer elements in front of their parents
 * @param text  text layout, only for text elements
 */
public record LayoutNode(
        Element element,
        double x, double y, double width, double height,
        int depth,
        List<LayoutNode> children,
        @Nullable TextLayout text
) {

    /**
     * @param component  the final component (MiniMessage parsed, base color applied)
     * @param scale      uniform text display scale
     * @param lineWidth  wrap width in font pixels, sent as the display's line width
     * @param blockX     left edge of the text block
     * @param blockY     top edge of the text block
     * @param blockWidth width of the text block in blocks
     * @param blockHeight height of the text block in blocks
     */
    public record TextLayout(Component component, double scale, int lineWidth,
                             double blockX, double blockY, double blockWidth, double blockHeight) {
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
