package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the chains hold ground, for the book (v0.32): {@code sectors} true is the ring of the {@code Front} (its owners come in
 * {@link ChainSyncPayload.War}); false is Nations &amp; War's dominions, listed here: each power's seat and enclaves, the
 * nation each guild was consecrated as, and the live Nations &amp; War wars a chain fights. Sent to everyone when it changes
 * and on login.
 *
 * @param dominions a power's town ({@code faction}: its chain id; {@code seat}: its seat, else an enclave)
 * @param nations   a guild-nation ({@code slot} "guild_1".., the nation's name and colour)
 * @param wars      a Nations &amp; War war with a chain on a side ({@code a}/{@code b}: chain ids or nation names; {@code kind}:
 *                  "powers", "incursion", "assault" or "guilds"; {@code phase}: the war's phase name; scores)
 */
public record ChainRealmPayload(boolean sectors, List<Dominion> dominions, List<GuildNation> nations, List<RealmWar> wars) implements CustomPacketPayload {
    public record Dominion(String faction, String name, int x, int z, boolean seat, boolean contested) {}
    public record GuildNation(String slot, String nation, int color, int citizens) {}
    public record RealmWar(String a, String b, String kind, String phase, int scoreA, int scoreB) {}

    public static final ChainRealmPayload SECTORS = new ChainRealmPayload(true, List.of(), List.of(), List.of());

    public ChainRealmPayload {
        dominions = List.copyOf(dominions);
        nations = List.copyOf(nations);
        wars = List.copyOf(wars);
    }

    public static final Type<ChainRealmPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_realm"));
    public static final StreamCodec<FriendlyByteBuf, ChainRealmPayload> STREAM_CODEC = StreamCodec.of(ChainRealmPayload::write, ChainRealmPayload::read);

    private static void write(FriendlyByteBuf b, ChainRealmPayload p) {
        b.writeBoolean(p.sectors);
        b.writeVarInt(p.dominions.size());
        for (Dominion d : p.dominions) {
            b.writeUtf(d.faction, 32);
            b.writeUtf(d.name, 64);
            b.writeVarInt(d.x);
            b.writeVarInt(d.z);
            b.writeBoolean(d.seat);
            b.writeBoolean(d.contested);
        }
        b.writeVarInt(p.nations.size());
        for (GuildNation n : p.nations) {
            b.writeUtf(n.slot, 16);
            b.writeUtf(n.nation, 64);
            b.writeInt(n.color);
            b.writeVarInt(n.citizens);
        }
        b.writeVarInt(p.wars.size());
        for (RealmWar w : p.wars) {
            b.writeUtf(w.a, 64);
            b.writeUtf(w.b, 64);
            b.writeUtf(w.kind, 16);
            b.writeUtf(w.phase, 16);
            b.writeVarInt(w.scoreA);
            b.writeVarInt(w.scoreB);
        }
    }

    private static ChainRealmPayload read(FriendlyByteBuf b) {
        boolean sectors = b.readBoolean();
        int n = Math.min(b.readVarInt(), 256);
        List<Dominion> dominions = new ArrayList<>(n);
        for (int i = 0; i < n; i++) dominions.add(new Dominion(b.readUtf(32), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()));
        n = Math.min(b.readVarInt(), 16);
        List<GuildNation> nations = new ArrayList<>(n);
        for (int i = 0; i < n; i++) nations.add(new GuildNation(b.readUtf(16), b.readUtf(64), b.readInt(), b.readVarInt()));
        n = Math.min(b.readVarInt(), 64);
        List<RealmWar> wars = new ArrayList<>(n);
        for (int i = 0; i < n; i++) wars.add(new RealmWar(b.readUtf(64), b.readUtf(64), b.readUtf(16), b.readUtf(16), b.readVarInt(), b.readVarInt()));
        return new ChainRealmPayload(sectors, dominions, nations, wars);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
