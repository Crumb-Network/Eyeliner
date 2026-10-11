package net.kalbskinder.eyeliner.markup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hand-written parser for the HTML-like .eyeliner syntax.
 * <p>
 * Supported: elements, self-closing elements, single/double/unquoted attribute values,
 * value-less (boolean) attributes, {@code <!-- comments -->} and the entities
 * {@code &lt; &gt; &amp; &quot; &apos;}.
 * <p>
 * Tags listed as raw-text tags (by default {@code <text>}) keep their inner markup verbatim,
 * so MiniMessage tags such as {@code <bold>} or {@code <gradient:red:blue>} can be used inside them.
 */
public final class MarkupParser {

    public static final Set<String> DEFAULT_RAW_TEXT_TAGS = Set.of("text");

    private final String src;
    private final Set<String> rawTextTags;
    private int pos;
    private int line = 1;
    private int column = 1;

    private MarkupParser(String src, Set<String> rawTextTags) {
        this.src = src;
        this.rawTextTags = rawTextTags;
    }

    public static MarkupNode parse(String source) {
        return parse(source, DEFAULT_RAW_TEXT_TAGS);
    }

    public static MarkupNode parse(String source, Set<String> rawTextTags) {
        MarkupParser parser = new MarkupParser(source, rawTextTags);
        return parser.parseDocument();
    }

    private MarkupNode parseDocument() {
        MarkupNode root = null;
        while (true) {
            skipWhitespaceAndComments();
            if (eof()) break;
            if (peek() != '<') throw error("Text is not allowed outside of the root element");
            if (root != null) throw error("A document may only contain a single root element");
            root = parseElement();
        }
        if (root == null) throw new EyelinerParseException("Document is empty");
        return root;
    }

    private MarkupNode parseElement() {
        final int startLine = line, startColumn = column;
        expect('<');
        final String tag = readName("tag name");
        final Map<String, String> attributes = new LinkedHashMap<>();

        while (true) {
            skipWhitespace();
            if (eof()) throw error("Unclosed tag <" + tag + ">");
            char c = peek();
            if (c == '/') {
                advance();
                expect('>');
                return new MarkupNode(tag, Collections.unmodifiableMap(attributes), List.of(), "", startLine, startColumn);
            }
            if (c == '>') {
                advance();
                break;
            }
            final int attrLine = line, attrColumn = column;
            final String name = readName("attribute name");
            skipWhitespace();
            String value = "true"; // value-less attributes act as boolean flags
            if (!eof() && peek() == '=') {
                advance();
                skipWhitespace();
                value = readAttributeValue();
            }
            if (attributes.put(name, value) != null) {
                throw new EyelinerParseException("Duplicate attribute '" + name + "' on <" + tag + ">", attrLine, attrColumn);
            }
        }

        if (rawTextTags.contains(tag)) {
            final String raw = readRawUntilClosing(tag);
            return new MarkupNode(tag, Collections.unmodifiableMap(attributes), List.of(),
                    collapseWhitespace(raw), startLine, startColumn);
        }

        final List<MarkupNode> children = new ArrayList<>();
        final StringBuilder text = new StringBuilder();
        while (true) {
            if (eof()) throw new EyelinerParseException("Missing closing tag </" + tag + ">", startLine, startColumn);
            if (startsWith("<!--")) {
                skipComment();
            } else if (startsWith("</")) {
                advance();
                advance();
                final String closing = readName("closing tag name");
                if (!closing.equals(tag)) {
                    throw error("Expected </" + tag + "> (opened at line " + startLine + ") but found </" + closing + ">");
                }
                skipWhitespace();
                expect('>');
                break;
            } else if (peek() == '<') {
                children.add(parseElement());
                text.append(' ');
            } else {
                text.append(advance());
            }
        }
        return new MarkupNode(tag, Collections.unmodifiableMap(attributes), List.copyOf(children),
                decodeEntities(collapseWhitespace(text.toString())), startLine, startColumn);
    }

    private String readRawUntilClosing(String tag) {
        final int startLine = line, startColumn = column;
        final String closing = "</" + tag;
        final StringBuilder raw = new StringBuilder();
        while (true) {
            if (eof()) throw new EyelinerParseException("Missing closing tag </" + tag + ">", startLine, startColumn);
            if (startsWithIgnoreCase(closing)) {
                final int after = pos + closing.length();
                if (after < src.length() && (src.charAt(after) == '>' || Character.isWhitespace(src.charAt(after)))) {
                    for (int i = 0; i < closing.length(); i++) advance();
                    skipWhitespace();
                    expect('>');
                    return raw.toString();
                }
            }
            raw.append(advance());
        }
    }

    private String readAttributeValue() {
        if (eof()) throw error("Expected attribute value");
        final char quote = peek();
        if (quote == '"' || quote == '\'') {
            final int startLine = line, startColumn = column;
            advance();
            final StringBuilder value = new StringBuilder();
            while (true) {
                if (eof()) throw new EyelinerParseException("Unterminated attribute value", startLine, startColumn);
                char c = advance();
                if (c == quote) break;
                value.append(c);
            }
            return decodeEntities(value.toString());
        }
        final StringBuilder value = new StringBuilder();
        while (!eof() && !Character.isWhitespace(peek()) && peek() != '>' && !startsWith("/>")) {
            value.append(advance());
        }
        if (value.isEmpty()) throw error("Expected attribute value");
        return decodeEntities(value.toString());
    }

    private String readName(String what) {
        final StringBuilder name = new StringBuilder();
        while (!eof()) {
            char c = peek();
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':' || c == '.') {
                name.append(advance());
            } else {
                break;
            }
        }
        if (name.isEmpty()) throw error("Expected " + what);
        return name.toString().toLowerCase(Locale.ROOT);
    }

    private void skipWhitespaceAndComments() {
        while (true) {
            skipWhitespace();
            if (startsWith("<!--")) skipComment();
            else return;
        }
    }

    private void skipComment() {
        final int startLine = line, startColumn = column;
        while (!startsWith("-->")) {
            if (eof()) throw new EyelinerParseException("Unterminated comment", startLine, startColumn);
            advance();
        }
        advance();
        advance();
        advance();
    }

    private void skipWhitespace() {
        while (!eof() && Character.isWhitespace(peek())) advance();
    }

    private void expect(char expected) {
        if (eof() || peek() != expected) throw error("Expected '" + expected + "'");
        advance();
    }

    private boolean startsWith(String s) {
        return src.startsWith(s, pos);
    }

    private boolean startsWithIgnoreCase(String s) {
        return src.regionMatches(true, pos, s, 0, s.length());
    }

    private boolean eof() {
        return pos >= src.length();
    }

    private char peek() {
        return src.charAt(pos);
    }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private EyelinerParseException error(String message) {
        return new EyelinerParseException(message, line, column);
    }

    static String collapseWhitespace(String s) {
        return s.replaceAll("\\s+", " ").strip();
    }

    static String decodeEntities(String s) {
        if (s.indexOf('&') < 0) return s;
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
