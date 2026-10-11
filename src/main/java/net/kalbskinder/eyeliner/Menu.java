package net.kalbskinder.eyeliner;

import net.kalbskinder.Eyeliner;
import net.kalbskinder.eyeliner.action.ActionRegistry;
import net.kalbskinder.eyeliner.action.ClickHandler;
import net.kalbskinder.eyeliner.element.ScreenElement;
import net.kalbskinder.eyeliner.render.ScreenTransform;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A parsed .eyeliner document. Menus are immutable templates: parse once, then {@link #open(Player)} them
 * for as many players as you like. Every call creates an independent {@link MenuSession}.
 */
public final class Menu {

    private final ScreenElement root;
    private final List<String> warnings;
    private final ActionRegistry actions = new ActionRegistry();

    public Menu(ScreenElement root, List<String> warnings) {
        this.root = root;
        this.warnings = List.copyOf(warnings);
    }

    /**
     * Binds an action key to code for this menu only. Takes precedence over {@link Eyeliner#registerAction}.
     */
    public Menu onClick(String action, ClickHandler handler) {
        actions.register(action, handler);
        return this;
    }

    @Nullable ClickHandler handler(String action) {
        return actions.get(action);
    }

    /**
     * Opens the menu in front of the player, facing them.
     */
    public MenuSession open(Player player) {
        return open(player, TagResolver.empty());
    }

    /**
     * Opens the menu in front of the player. {@code placeholders} are available in all {@code <text>} elements,
     * e.g. {@code Placeholder.unparsed("player", player.getUsername())} for {@code <text>Hi <player></text>}.
     */
    public MenuSession open(Player player, TagResolver placeholders) {
        final Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        return openAt(player, ScreenTransform.inFrontOf(eye, root.distance()), placeholders);
    }

    /**
     * Opens the menu at a fixed place in the world. {@code center} is the middle of the screen and its yaw is the
     * direction the screen faces (players looking at it have the opposite yaw).
     */
    public MenuSession openAt(Player player, Pos center, TagResolver placeholders) {
        return Eyeliner.manager().open(this, player, center, placeholders);
    }

    public ScreenElement root() {
        return root;
    }

    /**
     * Non-fatal problems found while loading (unknown attributes, invalid values that fell back to defaults, ...).
     */
    public List<String> warnings() {
        return warnings;
    }
}
