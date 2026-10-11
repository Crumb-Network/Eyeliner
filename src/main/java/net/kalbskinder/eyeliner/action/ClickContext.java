package net.kalbskinder.eyeliner.action;

import net.kalbskinder.eyeliner.MenuSession;
import net.kalbskinder.eyeliner.element.ButtonElement;
import net.minestom.server.entity.Player;

/**
 * Information about a button click.
 *
 * @param player  the player who clicked
 * @param session the open menu the button belongs to; call {@link MenuSession#close()} to close it
 * @param button  the clicked button
 * @param action  the action key that was resolved
 * @param type    which mouse button was used
 */
public record ClickContext(Player player, MenuSession session, ButtonElement button, String action, ClickType type) {

    /**
     * Convenience for {@code session().close()}.
     */
    public void close() {
        session.close();
    }
}
