package net.kalbskinder.eyeliner.element;

import net.kalbskinder.eyeliner.markup.EyelinerParseException;
import net.kalbskinder.eyeliner.markup.MarkupNode;
import net.kalbskinder.eyeliner.style.Style;
import net.kalbskinder.eyeliner.style.StyleSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Turns {@link MarkupNode}s into validated {@link Element}s.
 * New element types can be added with {@link #register(String, Set, Builder)}
 * (plus {@link StyleSheet#registerTag} for their defaults).
 */
public final class ElementFactory {

    @FunctionalInterface
    public interface Builder {
        Element build(MarkupNode node, Style style, List<Element> children);
    }

    private record Definition(Set<String> allowedChildren, Builder builder) {
    }

    private final StyleSheet styleSheet;
    private final Map<String, Definition> definitions = new ConcurrentHashMap<>();

    public ElementFactory(StyleSheet styleSheet) {
        this.styleSheet = styleSheet;
        register("screen", Set.of("text", "button", "sprite"),
                (node, style, children) -> new ScreenElement(id(node), style, children));
        register("button", Set.of("text", "sprite"),
                (node, style, children) -> new ButtonElement(id(node), style, children));
        register("text", Set.of(),
                (node, style, _) -> new TextElement(id(node), style, node.text()));
        register("sprite", Set.of(),
                (node, style, _) -> new SpriteElement(id(node), style));
    }

    /**
     * Registers a tag. {@code allowedChildren} lists the tags that may be nested inside it.
     */
    public void register(String tag, Set<String> allowedChildren, Builder builder) {
        definitions.put(tag, new Definition(Set.copyOf(allowedChildren), builder));
    }

    /**
     * Builds the tree for a whole document. The root must be a {@code <screen>}.
     */
    public ScreenElement buildDocument(MarkupNode root, Consumer<String> warnings) {
        if (!root.tag().equals("screen")) {
            throw new EyelinerParseException("The root element must be <screen>, found <" + root.tag() + ">", root.line(), root.column());
        }
        return (ScreenElement) build(root, null, warnings);
    }

    private Element build(MarkupNode node, Style parent, Consumer<String> warnings) {
        final Definition definition = definitions.get(node.tag());
        if (definition == null || !styleSheet.isKnownTag(node.tag())) {
            throw new EyelinerParseException("Unknown element <" + node.tag() + ">", node.line(), node.column());
        }
        final Style style = styleSheet.resolve(node.tag(), node.attributes(), parent, node.location(), warnings);

        final List<Element> children = new ArrayList<>();
        for (MarkupNode child : node.children()) {
            if (!definitions.containsKey(child.tag()) || !styleSheet.isKnownTag(child.tag())) {
                throw new EyelinerParseException("Unknown element <" + child.tag() + ">", child.line(), child.column());
            }
            if (!definition.allowedChildren().contains(child.tag())) {
                throw new EyelinerParseException("<" + child.tag() + "> is not allowed inside <" + node.tag() + ">"
                        + (definition.allowedChildren().isEmpty() ? "" : " (allowed: " + String.join(", ", definition.allowedChildren()) + ")"),
                        child.line(), child.column());
            }
            children.add(build(child, style, warnings));
        }

        // Loose text such as <button>Play</button> becomes an implicit <text> child.
        if (!node.text().isEmpty() && !node.tag().equals("text")) {
            if (definition.allowedChildren().contains("text")) {
                final MarkupNode implicit = new MarkupNode("text", Map.of(), List.of(), node.text(), node.line(), node.column());
                children.add(build(implicit, style, warnings));
            } else {
                warnings.accept("Ignoring text inside " + node.location());
            }
        }

        return definition.builder().build(node, style, children);
    }

    private static String id(MarkupNode node) {
        return node.attributes().get("id");
    }
}
