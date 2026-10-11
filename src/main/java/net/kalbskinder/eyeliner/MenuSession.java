package net.kalbskinder.eyeliner;

import net.kalbskinder.eyeliner.layout.LayoutNode;
import net.kalbskinder.eyeliner.render.MenuRenderer;
import net.kalbskinder.eyeliner.render.PacketEntity;
import net.kalbskinder.eyeliner.render.ScreenTransform;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One menu shown to one player. Created by {@link Menu#open}.
 */
public final class MenuSession {

    private final MenuManager manager;
    private final Menu menu;
    private final Player player;
    private final Instance instance;
    private final Pos center;
    private final ScreenTransform transform;
    private final LayoutNode layout;
    private final MenuRenderer.Result rendered;
    private final Map<Integer, MenuRenderer.RenderedButton> buttonsByInteraction = new HashMap<>();

    private MenuRenderer.@Nullable RenderedButton hovered;
    private boolean open;

    MenuSession(MenuManager manager, Menu menu, Player player, Pos center, ScreenTransform transform,
                LayoutNode layout, MenuRenderer.Result rendered) {
        this.manager = manager;
        this.menu = menu;
        this.player = player;
        this.instance = player.getInstance();
        this.center = center;
        this.transform = transform;
        this.layout = layout;
        this.rendered = rendered;
        for (MenuRenderer.RenderedButton button : rendered.buttons()) {
            for (int id : button.interactionIds()) buttonsByInteraction.put(id, button);
        }
    }

    synchronized void show() {
        final List<SendablePacket> packets = new ArrayList<>();
        for (PacketEntity<?> entity : rendered.entities()) packets.addAll(entity.spawn());
        player.sendPackets(packets);
        open = true;
    }

    /**
     * Removes the menu from the player's screen. Safe to call more than once.
     */
    public void close() {
        synchronized (this) {
            if (!open) return;
            open = false;
            hovered = null;
            final List<Integer> ids = new ArrayList<>(rendered.entities().size());
            for (PacketEntity<?> entity : rendered.entities()) {
                entity.markDespawned();
                ids.add(entity.id());
            }
            if (player.isOnline()) player.sendPacket(new DestroyEntitiesPacket(ids));
        }
        manager.remove(this);
    }

    /**
     * Forgets the session without sending packets, used when the client already dropped the entities.
     */
    synchronized void discard() {
        open = false;
        for (PacketEntity<?> entity : rendered.entities()) entity.markDespawned();
    }

    public synchronized boolean isOpen() {
        return open;
    }

    public Menu menu() {
        return menu;
    }

    public Player player() {
        return player;
    }

    Instance instance() {
        return instance;
    }

    /**
     * Center of the screen in the world; its yaw is the direction the screen faces.
     */
    public Pos position() {
        return center;
    }

    /**
     * Replaces the content of the {@code <text id="...">} element. The layout is not recalculated, so the new
     * text should have roughly the same size as the old one.
     *
     * @return false if there is no text element with this id
     */
    public boolean setText(String id, Component text) {
        final PacketEntity<TextDisplayMeta> entity = rendered.textsById().get(id);
        if (entity == null) return false;
        synchronized (this) {
            if (open) entity.meta().setText(text);
        }
        return true;
    }

    /**
     * Same as {@link #setText(String, Component)} with MiniMessage input.
     */
    public boolean setText(String id, String miniMessage) {
        return setText(id, manager.miniMessage().deserialize(miniMessage));
    }

    // ---- input, called by MenuManager on the player's thread ----

    /**
     * Ray-casts against the screen.
     *
     * @return (ui x, ui y, distance) if the ray hits the screen rectangle within reach, otherwise null
     */
    double @Nullable [] raycast(Point eye, Vec direction) {
        final double[] hit = transform.raycast(eye, direction, menu.root().reach());
        if (hit == null || !layout.contains(hit[0], hit[1])) return null;
        return hit;
    }

    MenuRenderer.@Nullable RenderedButton buttonAt(double x, double y) {
        final List<MenuRenderer.RenderedButton> buttons = rendered.buttons();
        for (int i = buttons.size() - 1; i >= 0; i--) { // later buttons are drawn on top
            if (buttons.get(i).node().contains(x, y)) return buttons.get(i);
        }
        return null;
    }

    MenuRenderer.@Nullable RenderedButton buttonByInteraction(int entityId) {
        return buttonsByInteraction.get(entityId);
    }

    synchronized void setHovered(MenuRenderer.@Nullable RenderedButton button) {
        if (!open || button == hovered) return;
        if (hovered != null) recolor(hovered, hovered.bg());
        hovered = button;
        if (button != null) recolor(button, button.hoverBg());
    }

    private static void recolor(MenuRenderer.RenderedButton button, int color) {
        for (PacketEntity<TextDisplayMeta> fill : button.fills()) fill.meta().setBackgroundColor(color);
    }

    double distanceTo(Point point) {
        return transform.origin().distance(point);
    }
}
