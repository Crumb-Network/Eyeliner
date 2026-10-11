package net.kalbskinder.eyeliner.layout;

import net.kalbskinder.eyeliner.element.Element;
import net.kalbskinder.eyeliner.element.ScreenElement;
import net.kalbskinder.eyeliner.element.SpriteElement;
import net.kalbskinder.eyeliner.element.TextElement;
import net.kalbskinder.eyeliner.style.Alignment;
import net.kalbskinder.eyeliner.style.Colors;
import net.kalbskinder.eyeliner.style.Insets;
import net.kalbskinder.eyeliner.style.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * A small flexbox-like layout: containers stack their children in a row or column with a gap,
 * align them horizontally ({@code align}) and vertically ({@code valign}), and size themselves to their
 * content unless {@code width}/{@code height} are set. Percentages refer to the parent's content box.
 * Text wraps automatically when it is wider than the space available.
 */
public final class LayoutEngine {

    /**
     * Font size unit: text display scale = size * this. {@code size="5"} equals a vanilla text display.
     */
    public static final double FONT_SCALE_PER_SIZE = 0.2;

    /**
     * Upper bound used to resolve auto/percent sizes of the root.
     */
    private static final double MAX_SCREEN_SIZE = 16;

    private final MiniMessage miniMessage;
    private final TagResolver resolver;

    public LayoutEngine(MiniMessage miniMessage, TagResolver resolver) {
        this.miniMessage = miniMessage;
        this.resolver = resolver;
    }

    public LayoutNode layout(ScreenElement screen) {
        final Measured measured = measure(screen, MAX_SCREEN_SIZE, MAX_SCREEN_SIZE);
        return place(measured, 0, 0, 0);
    }

    private record TextMeasure(Component component, double scale, int lineWidth, double width, double height) {
    }

    private record Measured(Element element, double width, double height, List<Measured> children, TextMeasure text) {
    }

    private Measured measure(Element element, double availableWidth, double availableHeight) {
        final Style style = element.style();
        final Insets padding = style.padding();
        final Insets margin = style.margin();

        final double explicitWidth = style.width().isAuto() ? Double.NaN : Math.max(0, style.width().resolve(availableWidth, 0));
        final double explicitHeight = style.height().isAuto() ? Double.NaN : Math.max(0, style.height().resolve(availableHeight, 0));
        final double innerWidth = Math.max(0, (Double.isNaN(explicitWidth) ? availableWidth - margin.horizontal() : explicitWidth) - padding.horizontal());
        final double innerHeight = Math.max(0, (Double.isNaN(explicitHeight) ? availableHeight - margin.vertical() : explicitHeight) - padding.vertical());

        double contentWidth;
        double contentHeight;
        TextMeasure text = null;
        final List<Measured> children = new ArrayList<>();

        switch (element) {
            case TextElement textElement -> {
                text = measureText(textElement, innerWidth);
                contentWidth = text.width();
                contentHeight = text.height();
            }
            case SpriteElement sprite -> {
                contentWidth = sprite.size();
                contentHeight = sprite.size();
            }
            default -> {
                measureChildren(element, innerWidth, innerHeight, children);
                final boolean row = style.direction() == Alignment.Direction.ROW;
                double main = 0, cross = 0;
                for (Measured child : children) {
                    final Insets m = child.element().style().margin();
                    final double outerWidth = child.width() + m.horizontal();
                    final double outerHeight = child.height() + m.vertical();
                    main += row ? outerWidth : outerHeight;
                    cross = Math.max(cross, row ? outerHeight : outerWidth);
                }
                main += style.gap() * Math.max(0, children.size() - 1);
                contentWidth = row ? main : cross;
                contentHeight = row ? cross : main;
            }
        }

        final double width = Double.isNaN(explicitWidth) ? contentWidth + padding.horizontal() : explicitWidth;
        final double height = Double.isNaN(explicitHeight) ? contentHeight + padding.vertical() : explicitHeight;
        return new Measured(element, width, height, List.copyOf(children), text);
    }

    private void measureChildren(Element element, double innerWidth, double innerHeight, List<Measured> out) {
        final List<Element> elements = element.children();
        if (element.style().direction() == Alignment.Direction.COLUMN) {
            for (Element child : elements) out.add(measure(child, innerWidth, innerHeight));
            return;
        }
        // Row: fixed-size children first, then share the remaining width between text children so they wrap.
        final Measured[] measured = new Measured[elements.size()];
        double used = element.style().gap() * Math.max(0, elements.size() - 1);
        int textCount = 0;
        for (int i = 0; i < elements.size(); i++) {
            final Element child = elements.get(i);
            if (child instanceof TextElement && child.style().width().isAuto()) {
                textCount++;
                continue;
            }
            measured[i] = measure(child, innerWidth, innerHeight);
            used += measured[i].width() + child.style().margin().horizontal();
        }
        final double share = textCount == 0 ? 0 : Math.max(0, innerWidth - used) / textCount;
        for (int i = 0; i < elements.size(); i++) {
            if (measured[i] == null) measured[i] = measure(elements.get(i), share, innerHeight);
            out.add(measured[i]);
        }
    }

    private TextMeasure measureText(TextElement element, double availableWidth) {
        final Style style = element.style();
        final Component component = miniMessage.deserialize(element.content(), resolver)
                .colorIfAbsent(TextColor.color(Colors.rgb(style.color())));
        final double scale = style.fontSize() * FONT_SCALE_PER_SIZE;
        final double pixel = FontMetrics.PIXEL * scale;
        final int lineWidth = (int) Math.max(1, Math.floor(availableWidth / pixel + 1e-6));
        final FontMetrics.Measurement measurement = FontMetrics.measure(component, lineWidth);
        return new TextMeasure(component, scale, lineWidth,
                measurement.maxLineWidth() * pixel,
                measurement.lines() * FontMetrics.LINE_HEIGHT * pixel);
    }

    private LayoutNode place(Measured measured, double x, double y, int depth) {
        final Element element = measured.element();
        final Style style = element.style();
        final Insets padding = style.padding();
        final double contentX = x + padding.left();
        final double contentY = y + padding.top();
        final double contentWidth = measured.width() - padding.horizontal();
        final double contentHeight = measured.height() - padding.vertical();

        if (measured.text() != null) {
            final TextMeasure text = measured.text();
            final double blockX = alignStart(style.textAlign(), contentX, contentWidth, text.width());
            final double blockY = alignStart(style.valign(), contentY, contentHeight, text.height());
            return new LayoutNode(element, x, y, measured.width(), measured.height(), depth, List.of(),
                    new LayoutNode.TextLayout(text.component(), text.scale(), text.lineWidth(),
                            blockX, blockY, text.width(), text.height()));
        }

        final List<LayoutNode> children = new ArrayList<>();
        final boolean row = style.direction() == Alignment.Direction.ROW;
        double total = style.gap() * Math.max(0, measured.children().size() - 1);
        for (Measured child : measured.children()) {
            final Insets m = child.element().style().margin();
            total += row ? child.width() + m.horizontal() : child.height() + m.vertical();
        }

        double cursor = row
                ? alignStart(style.align(), contentX, contentWidth, total)
                : alignStart(style.valign(), contentY, contentHeight, total);
        for (Measured child : measured.children()) {
            final Insets m = child.element().style().margin();
            final double childX, childY;
            if (row) {
                childX = cursor + m.left();
                childY = alignStart(style.valign(), contentY, contentHeight, child.height() + m.vertical()) + m.top();
                cursor += child.width() + m.horizontal() + style.gap();
            } else {
                childX = alignStart(style.align(), contentX, contentWidth, child.width() + m.horizontal()) + m.left();
                childY = cursor + m.top();
                cursor += child.height() + m.vertical() + style.gap();
            }
            children.add(place(child, childX, childY, depth + 1));
        }
        return new LayoutNode(element, x, y, measured.width(), measured.height(), depth, List.copyOf(children), null);
    }

    private static double alignStart(Alignment.Horizontal align, double start, double available, double size) {
        return switch (align) {
            case LEFT -> start;
            case CENTER -> start + (available - size) / 2;
            case RIGHT -> start + available - size;
        };
    }

    private static double alignStart(Alignment.Vertical align, double start, double available, double size) {
        return switch (align) {
            case TOP -> start;
            case CENTER -> start + (available - size) / 2;
            case BOTTOM -> start + available - size;
        };
    }
}
