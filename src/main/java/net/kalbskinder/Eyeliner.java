package net.kalbskinder;

import net.kalbskinder.eyeliner.Menu;
import net.kalbskinder.eyeliner.MenuManager;
import net.kalbskinder.eyeliner.MenuSession;
import net.kalbskinder.eyeliner.action.ActionRegistry;
import net.kalbskinder.eyeliner.action.ClickHandler;
import net.kalbskinder.eyeliner.element.ElementFactory;
import net.kalbskinder.eyeliner.element.ScreenElement;
import net.kalbskinder.eyeliner.markup.EyelinerParseException;
import net.kalbskinder.eyeliner.markup.MarkupParser;
import net.kalbskinder.eyeliner.style.StyleSheet;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the Eyeliner UI library.
 *
 * <pre>{@code
 * Eyeliner.init();                                   // after MinecraftServer.init()
 * Eyeliner.registerAction("joinGame", ctx -> { ... ctx.close(); });
 * Menu menu = Eyeliner.loadResource("example.eyeliner");
 * menu.open(player);
 * }</pre>
 */
public final class Eyeliner {

    private static final Logger LOGGER = LoggerFactory.getLogger("Eyeliner");

    private static final StyleSheet STYLE_SHEET = new StyleSheet();
    private static final ElementFactory ELEMENTS = new ElementFactory(STYLE_SHEET);
    private static final ActionRegistry ACTIONS = new ActionRegistry();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static volatile MenuManager manager;

    private Eyeliner() {
    }

    /**
     * Registers Eyeliner's listeners on the global event handler. Call after {@code MinecraftServer.init()}.
     * Opening a menu calls this automatically if needed.
     */
    public static void init() {
        init(MinecraftServer.getGlobalEventHandler());
    }

    /**
     * Registers Eyeliner's listeners on a custom event node.
     */
    public static synchronized void init(EventNode<Event> parent) {
        if (manager != null) return;
        final MenuManager created = new MenuManager(ACTIONS, MINI_MESSAGE);
        parent.addChild(created.eventNode());
        manager = created;
    }

    /**
     * The session manager, initialised on first use.
     */
    public static MenuManager manager() {
        if (manager == null) init();
        return manager;
    }

    // ---- loading ----

    /**
     * Parses a menu from source text.
     *
     * @throws EyelinerParseException if the document is malformed
     */
    public static Menu parse(String source) {
        final List<String> warnings = new ArrayList<>();
        final ScreenElement root = ELEMENTS.buildDocument(MarkupParser.parse(source), warnings::add);
        for (String warning : warnings) LOGGER.warn(warning);
        return new Menu(root, warnings);
    }

    public static Menu load(Path path) throws IOException {
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (EyelinerParseException e) {
            throw new EyelinerParseException(path + ": " + e.getMessage());
        }
    }

    /**
     * Loads a menu from the classpath, e.g. {@code loadResource("menus/main.eyeliner")}.
     */
    public static Menu loadResource(String name) {
        final String path = name.startsWith("/") ? name.substring(1) : name;
        try (InputStream in = Eyeliner.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read resource " + path, e);
        } catch (EyelinerParseException e) {
            throw new EyelinerParseException(path + ": " + e.getMessage());
        }
    }

    // ---- actions ----

    /**
     * Binds an action key used in {@code onClick="..."} to Java code, for every menu.
     */
    public static void registerAction(String action, ClickHandler handler) {
        ACTIONS.register(action, handler);
    }

    public static void unregisterAction(String action) {
        ACTIONS.unregister(action);
    }

    // ---- styling ----

    /**
     * Registers a named variant usable as {@code variant="name"} on elements with the given tag
     * ({@link StyleSheet#ANY} for all). Values are attributes, e.g. {@code Map.of("bg", "#ff8800", "radius", "50%")}.
     */
    public static void registerVariant(String tag, String name, Map<String, String> attributes) {
        STYLE_SHEET.registerVariant(tag, name, attributes);
    }

    public static StyleSheet styleSheet() {
        return STYLE_SHEET;
    }

    public static ElementFactory elements() {
        return ELEMENTS;
    }

    // ---- sessions ----

    public static List<MenuSession> openMenus(Player player) {
        return manager().sessionsOf(player);
    }

    public static void closeAll(Player player) {
        manager().closeAll(player);
    }
}
