package net.kalbskinder.eyeliner.action;

/**
 * Java code bound to an action key used in a .eyeliner file, e.g. {@code onClick="joinGame"}.
 * Handlers run on the clicking player's tick thread.
 */
@FunctionalInterface
public interface ClickHandler {

    void onClick(ClickContext context);
}
