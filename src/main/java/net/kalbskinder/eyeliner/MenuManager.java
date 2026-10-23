package net.kalbskinder.eyeliner;

import net.kalbskinder.eyeliner.action.ActionRegistry;
import net.kalbskinder.eyeliner.action.ClickContext;
import net.kalbskinder.eyeliner.action.ClickHandler;
import net.kalbskinder.eyeliner.action.ClickType;
import net.kalbskinder.eyeliner.layout.LayoutEngine;
import net.kalbskinder.eyeliner.layout.LayoutNode;
import net.kalbskinder.eyeliner.render.MenuRenderer;
import net.kalbskinder.eyeliner.render.ScreenTransform;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.network.packet.client.play.ClientAnimationPacket;
import net.minestom.server.network.packet.client.play.ClientAttackPacket;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps track of open menus and translates player input into hover and click events.
 * <p>
 * Input model: the client only reports clicks, not what they were aimed at (except for entities), so every
 * click is resolved by ray-casting the player's view against the screens they have open:
 * <ul>
 *     <li>left click: {@link ClientAttackPacket} (aimed at an interaction entity) or {@link ClientAnimationPacket} (arm swing, sent for every left click)</li>
 *     <li>right click: {@link ClientInteractEntityPacket} (aimed at an interaction entity) or {@link ClientUseItemPacket} (holding an item)</li>
 *     <li>hover: every rotation/position update ({@link PlayerMoveEvent})</li>
 * </ul>
 * One physical click often produces several packets, so clicks are debounced per player.
 */
public final class MenuManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("Eyeliner");
    private static final long CLICK_COOLDOWN_NANOS = 150_000_000L;

    private final ActionRegistry globalActions;
    private final MiniMessage miniMessage;
    private final EventNode<PlayerEvent> node = EventNode.type("eyeliner", EventFilter.PLAYER);
    private final Map<UUID, List<MenuSession>> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private final Set<String> warnedActions = ConcurrentHashMap.newKeySet();

    public MenuManager(ActionRegistry globalActions, MiniMessage miniMessage) {
        this.globalActions = globalActions;
        this.miniMessage = miniMessage;

        node.addListener(PlayerPacketEvent.class, this::onPacket);
        node.addListener(PlayerMoveEvent.class, event -> onLook(event.getPlayer(), event.getNewPosition()));
        node.addListener(PlayerDisconnectEvent.class, event -> {
            final List<MenuSession> removed = sessions.remove(event.getPlayer().getUuid());
            if (removed != null) removed.forEach(MenuSession::discard);
            lastClick.remove(event.getPlayer().getUuid());
        });
        node.addListener(PlayerSpawnEvent.class, event -> {
            // Changing instance clears all entities on the client.
            for (MenuSession session : sessionsOf(event.getPlayer())) {
                if (session.instance() != event.getInstance()) {
                    session.discard();
                    remove(session);
                }
            }
        });
    }

    /**
     * The event node with all Eyeliner listeners; it has to be attached to an event handler.
     */
    public EventNode<PlayerEvent> eventNode() {
        return node;
    }

    MiniMessage miniMessage() {
        return miniMessage;
    }

    public List<MenuSession> sessionsOf(Player player) {
        final List<MenuSession> list = sessions.get(player.getUuid());
        return list == null ? List.of() : List.copyOf(list);
    }

    public void closeAll(Player player) {
        sessionsOf(player).forEach(MenuSession::close);
    }

    MenuSession open(Menu menu, Player player, Pos center, TagResolver placeholders) {
        if (player.getInstance() == null) {
            throw new IllegalStateException("Player " + player.getUsername() + " is not in an instance yet; open menus in PlayerSpawnEvent or later");
        }
        final LayoutNode layout = new LayoutEngine(miniMessage, placeholders).layout(menu.root());
        final ScreenTransform transform = new ScreenTransform(center, layout.width(), layout.height());
        final MenuRenderer.Result rendered = new MenuRenderer(player, transform, menu.root()).render(layout);
        final MenuSession session = new MenuSession(this, menu, player, center, transform, layout, rendered);
        sessions.computeIfAbsent(player.getUuid(), _ -> new CopyOnWriteArrayList<>()).add(session);
        session.show();
        onLook(player, player.getPosition());
        return session;
    }

    void remove(MenuSession session) {
        sessions.computeIfPresent(session.player().getUuid(), (_, list) -> {
            list.remove(session);
            return list.isEmpty() ? null : list;
        });
    }

    // ---- input ----

    private void onPacket(PlayerPacketEvent event) {
        final Player player = event.getPlayer();
        if (!sessions.containsKey(player.getUuid())) return;
        switch (event.getPacket()) {
            case ClientAttackPacket(int targetId) -> click(player, ClickType.LEFT, targetId);
            case ClientAnimationPacket _ -> click(player, ClickType.LEFT, -1);
            case ClientInteractEntityPacket packet -> click(player, ClickType.RIGHT, packet.targetId());
            case ClientUseItemPacket _ -> click(player, ClickType.RIGHT, -1);
            default -> {
            }
        }
    }

    private record Target(MenuSession session, MenuRenderer.@Nullable RenderedButton button) {
    }

    /**
     * Finds the closest open screen the player is looking at and the button under the crosshair.
     */
    private @Nullable Target target(Player player, Pos position) {
        final Pos eye = position.add(0, player.getEyeHeight(), 0);
        final var direction = ScreenTransform.direction(position.yaw(), position.pitch());
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MenuSession session : sessionsOf(player)) {
            final double[] hit = session.raycast(eye, direction);
            if (hit == null || hit[2] >= bestDistance) continue;
            bestDistance = hit[2];
            best = new Target(session, session.buttonAt(hit[0], hit[1]));
        }
        return best;
    }

    private void onLook(Player player, Pos position) {
        final List<MenuSession> open = sessionsOf(player);
        if (open.isEmpty()) return;
        final Target target = target(player, position);
        for (MenuSession session : open) {
            final double closeDistance = session.menu().root().closeDistance();
            if (closeDistance > 0 && session.distanceTo(position.add(0, player.getEyeHeight(), 0)) > closeDistance) {
                session.close();
                continue;
            }
            session.setHovered(target != null && target.session() == session ? target.button() : null);
        }
    }

    private void click(Player player, ClickType type, int targetId) {
        Target target = target(player, player.getPosition());
        if ((target == null || target.button() == null) && targetId >= 0) {
            // The crosshair is not over a button (e.g. due to latency), fall back to the clicked hit box.
            for (MenuSession session : sessionsOf(player)) {
                final MenuRenderer.RenderedButton button = session.buttonByInteraction(targetId);
                if (button != null) target = new Target(session, button);
            }
        }
        if (target == null || target.button() == null) return;

        final long now = System.nanoTime();
        final Long previous = lastClick.put(player.getUuid(), now);
        if (previous != null && now - previous < CLICK_COOLDOWN_NANOS) return;

        final MenuSession session = target.session();
        final MenuRenderer.RenderedButton button = target.button();
        final String action = button.element().actionFor(type);
        if (action == null) return;

        final Sound sound = button.element().sound();
        if (sound != null) player.playSound(sound, Sound.Emitter.self());

        ClickHandler handler = session.menu().handler(action);
        if (handler == null) handler = globalActions.get(action);
        if (handler == null) {
            if (warnedActions.add(action)) {
                LOGGER.warn("No handler registered for action '{}' (register it with Eyeliner.registerAction)", action);
            }
            return;
        }
        try {
            handler.onClick(new ClickContext(player, session, button.element(), action, type));
        } catch (Exception e) {
            LOGGER.error("Action '{}' threw an exception", action, e);
        }
    }
}
