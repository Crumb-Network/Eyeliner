package net.kalbskinder.eyeliner.render;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.MetadataHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * A client-side only entity for a single viewer. It is never added to an instance, does not tick and is
 * invisible to other players; it only exists as spawn/metadata packets sent to {@link #viewer()}.
 * Changing the metadata after {@link #spawn()} immediately sends the change to the viewer.
 */
public final class PacketEntity<M extends EntityMeta> {

    private final int id = Entity.generateId(); // shared counter, so ids never collide with real entities
    private final UUID uuid = UUID.randomUUID();
    private final EntityType type;
    private final Pos position;
    private final Player viewer;
    private final MetadataHolder metadata;
    private final M meta;
    private volatile boolean spawned;

    @SuppressWarnings("removal")
    public PacketEntity(EntityType type, Pos position, Player viewer, BiFunction<Entity, MetadataHolder, M> metaFactory) {
        this.type = type;
        this.position = position;
        this.viewer = viewer;
        this.metadata = new MetadataHolder(changes -> {
            if (spawned) viewer.sendPacket(new EntityMetaDataPacket(id, changes));
        });
        this.meta = metaFactory.apply(null, metadata);
    }

    public int id() {
        return id;
    }

    public Player viewer() {
        return viewer;
    }

    public Pos position() {
        return position;
    }

    public M meta() {
        return meta;
    }

    /**
     * Packets that create this entity, with all metadata set so far, on the client.
     * Marks the entity as spawned, so later metadata changes are sent as updates.
     */
    public List<SendablePacket> spawn() {
        spawned = true;
        return List.of(
                new SpawnEntityPacket(id, uuid, type, position, position.yaw(), 0, Vec.ZERO),
                new EntityMetaDataPacket(id, metadata.getEntries())
        );
    }

    public void markDespawned() {
        spawned = false;
    }
}
