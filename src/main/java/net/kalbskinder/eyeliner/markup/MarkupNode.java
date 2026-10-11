package net.kalbskinder.eyeliner.markup;

import java.util.List;
import java.util.Map;

/**
 * A raw element read from a .eyeliner file, before any validation or styling.
 *
 * @param tag        lower-cased tag name
 * @param attributes attributes in declaration order, names lower-cased
 * @param children   child elements
 * @param text       inner text (whitespace collapsed), or the raw inner markup for raw-text tags
 * @param line       1-based line of the opening tag
 * @param column     1-based column of the opening tag
 */
public record MarkupNode(
        String tag,
        Map<String, String> attributes,
        List<MarkupNode> children,
        String text,
        int line,
        int column
) {

    public String location() {
        return "<" + tag + "> at " + line + ":" + column;
    }
}
