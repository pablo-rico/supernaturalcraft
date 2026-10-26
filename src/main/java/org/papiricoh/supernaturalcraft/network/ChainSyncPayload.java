package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import java.util.List;

/**
 * The chains as one hunter sees them. IDs are faction-local. Kills, {@code favor} (the officer's regard for the receiver,
 * -100..100) and {@code met} are personalized. {@code homeX/Z}: the centre of the officer's
 * {@link org.papiricoh.supernaturalcraft.chain.Territory} (its seat for the ranks that hold court); {@code seen*}: its last
 * sighting ({@code seenDay} -1: never); {@code player}: the UUID of the hunter holding the post, or "".
 * {@code courts}: one per faction, its stronghold ({@code built} false until it stands; the origin of its plan) and its
 * interregnum ({@code vacantUntil} -1 while the throne is held).
 * {@code bonds}: the notable ones ({@link org.papiricoh.supernaturalcraft.chain.Bonds#NOTABLE}), actors as
 * {@code "demon:12"} / {@code "player:<uuid>"}. {@code challenges}: those still open in either chain. {@code war}: the front and
 * the war. {@code task}: the receiver's task from a Lord, empty or one.
 */
public record ChainSyncPayload(List<Entry> officers, List<History> history, List<Court> courts, List<Bond> bonds,
                               List<Challenge> challenges, War war, List<Task> task) implements CustomPacketPayload {
    public record Entry(int id, String faction, String branch, String rank, int level, int superior,
                        long nameSeed, String status, int scars, int kills, String entityType, int variant,
                        int homeX, int homeZ, String seenDimension, int seenX, int seenZ, long seenDay,
                        String player, String playerName, int favor, int met) {}
    /** {@code otherFaction}: "" when the other party is not an officer. */
    public record History(long day, String key, String faction, int officerId, String detail, String otherFaction, int otherId) {}
    public record Court(String faction, boolean built, int x, int y, int z, long vacantUntil) {}
    public record Bond(String from, String to, int regard, boolean pact) {}
    /** {@code remaining}: ticks of play left to answer (for a hunter); {@code kind}/{@code state}: the enum names. */
    public record Challenge(int id, String kind, String challenger, String defender, boolean lethal, int remaining, String state, long day) {}
    /**
     * {@code until}: the day the war ends, -1 in peace; scores and prestige indexed by faction ordinal; {@code front}: the
     * owner (faction ordinal) of each sector, {@code frontScore} its push (positive: the Pit); both empty before it is drawn.
     */
    public record War(long until, int demonScore, int angelScore, int demonPrestige, int angelPrestige, byte[] front, int[] frontScore) {}
    public record Task(String kind, String lord, String targetFaction, int targetId, int count, long until) {}

    public static final War PEACE = new War(-1, 0, 0, 0, 0, new byte[0], new int[0]);

    public ChainSyncPayload {
        officers = List.copyOf(officers); history = List.copyOf(history); courts = List.copyOf(courts);
        bonds = List.copyOf(bonds); challenges = List.copyOf(challenges); task = List.copyOf(task);
    }
    public static final Type<ChainSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainSyncPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeCollection(p.officers, (buf, o) -> {
            buf.writeVarInt(o.id); buf.writeUtf(o.faction, 16); buf.writeUtf(o.branch, 16); buf.writeUtf(o.rank, 16);
            buf.writeVarInt(o.level); buf.writeVarInt(o.superior); buf.writeLong(o.nameSeed); buf.writeUtf(o.status, 16);
            buf.writeVarInt(o.scars); buf.writeVarInt(o.kills); buf.writeUtf(o.entityType, 128); buf.writeVarInt(o.variant);
            buf.writeVarInt(o.homeX); buf.writeVarInt(o.homeZ); buf.writeUtf(o.seenDimension, 128); buf.writeVarInt(o.seenX);
            buf.writeVarInt(o.seenZ); buf.writeVarLong(o.seenDay); buf.writeUtf(o.player, 40); buf.writeUtf(o.playerName, 32);
            buf.writeVarInt(o.favor); buf.writeVarInt(o.met);
        });
        b.writeCollection(p.history, (buf, h) -> {
            buf.writeLong(h.day); buf.writeUtf(h.key, 128); buf.writeUtf(h.faction, 16); buf.writeVarInt(h.officerId); buf.writeUtf(h.detail, 256);
            buf.writeUtf(h.otherFaction, 16); buf.writeVarInt(h.otherId);
        });
        b.writeCollection(p.courts, (buf, c) -> {
            buf.writeUtf(c.faction, 16); buf.writeBoolean(c.built); buf.writeVarInt(c.x); buf.writeVarInt(c.y); buf.writeVarInt(c.z);
            buf.writeVarLong(c.vacantUntil);
        });
        b.writeCollection(p.bonds, (buf, d) -> {
            buf.writeUtf(d.from, 48); buf.writeUtf(d.to, 48); buf.writeVarInt(d.regard); buf.writeBoolean(d.pact);
        });
        b.writeCollection(p.challenges, (buf, c) -> {
            buf.writeVarInt(c.id); buf.writeUtf(c.kind, 24); buf.writeUtf(c.challenger, 48); buf.writeUtf(c.defender, 48);
            buf.writeBoolean(c.lethal); buf.writeVarInt(c.remaining); buf.writeUtf(c.state, 16); buf.writeVarLong(c.day);
        });
        War w = p.war;
        b.writeVarLong(w.until); b.writeVarInt(w.demonScore); b.writeVarInt(w.angelScore); b.writeVarInt(w.demonPrestige);
        b.writeVarInt(w.angelPrestige); b.writeByteArray(w.front); b.writeVarIntArray(w.frontScore);
        b.writeCollection(p.task, (buf, t) -> {
            buf.writeUtf(t.kind, 16); buf.writeUtf(t.lord, 16); buf.writeUtf(t.targetFaction, 16); buf.writeVarInt(t.targetId);
            buf.writeVarInt(t.count); buf.writeVarLong(t.until);
        });
    }, b -> new ChainSyncPayload(b.readList(buf -> new Entry(buf.readVarInt(), buf.readUtf(16), buf.readUtf(16),
            buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(),
            buf.readUtf(40), buf.readUtf(32), buf.readVarInt(), buf.readVarInt())),
            b.readList(buf -> new History(buf.readLong(), buf.readUtf(128), buf.readUtf(16), buf.readVarInt(), buf.readUtf(256), buf.readUtf(16), buf.readVarInt())),
            b.readList(buf -> new Court(buf.readUtf(16), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong())),
            b.readList(buf -> new Bond(buf.readUtf(48), buf.readUtf(48), buf.readVarInt(), buf.readBoolean())),
            b.readList(buf -> new Challenge(buf.readVarInt(), buf.readUtf(24), buf.readUtf(48), buf.readUtf(48), buf.readBoolean(),
                    buf.readVarInt(), buf.readUtf(16), buf.readVarLong())),
            new War(b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readByteArray(64), b.readVarIntArray(64)),
            b.readList(buf -> new Task(buf.readUtf(16), buf.readUtf(16), buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readVarLong()))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
