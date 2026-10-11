package org.papiricoh.supernaturalcraft.debug;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A crowd of stand-in players for load audits ({@code /supernatural debug crowd}): real server players with no client behind
 * them (their packets go nowhere but are still built, encoded for the counters and counted), walking about around a point and
 * ticking every tick as a connected player does, so every per-player handler and sync of the mod runs for each. Development
 * use only; they leave with the server.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AuditCrowd {

    private static final List<Bot> BOTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private AuditCrowd() {
    }

    /** One stand-in. */
    public static final class Bot extends ServerPlayer {
        final Vec3 home;
        final double roam;
        Vec3 target;

        Bot(ServerLevel level, String name, Vec3 home, double roam) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
            this.connection = new Nowhere(level.getServer(), this);
            this.home = home;
            this.roam = roam;
            setGameMode(GameType.SURVIVAL);
        }
    }

    private static final class Nowhere extends ServerGamePacketListenerImpl {
        Nowhere(MinecraftServer server, ServerPlayer player) {
            super(server, new Connection(PacketFlow.SERVERBOUND) {
                @Override
                public void setListenerForServerboundHandshake(PacketListener listener) {
                }
            }, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(net.minecraft.network.chat.Component reason) {
        }

        // Every channel a connected modded client has: what the server sends them is still built (and counted), then dropped.
        @Override
        public boolean hasChannel(net.minecraft.resources.ResourceLocation payloadId) {
            return true;
        }

        @Override
        public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
            return true;
        }

        @Override
        public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
            return true;
        }

        @Override
        public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
            return net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE;
        }
    }

    public static int size() {
        return BOTS.size();
    }

    public static List<Bot> bots() {
        return List.copyOf(BOTS);
    }

    /**
     * Adds {@code n} stand-ins within {@code roam} blocks of {@code at}: a third angels, a third demons, a third hunters, all
     * Men of Letters, mana half spent (so it regenerates and syncs).
     */
    public static int spawn(ServerLevel level, Vec3 at, int n, double roam) {
        for (int i = 0; i < n; i++) {
            Bot b = new Bot(level, "sn-audit-" + (BOTS.size() + 1), at, roam);
            Vec3 p = spot(level, at, roam);
            b.moveTo(p.x, p.y, p.z, RANDOM.nextFloat() * 360, 0);
            level.addNewPlayer(b);
            listed(level.getServer()).put(b.getUUID(), b);
            players(level.getServer()).add(b);
            Faction f = Faction.values()[i % 3];
            Allegiance a = f == Faction.HUMAN ? Allegiance.HUMAN : Allegiance.HUMAN.convert(f).withRank(2 + i % 3);
            Allegiances.set(b, a);
            Legacies.set(b, Legacy.NONE.withRank(1 + i % 3));
            ManaManager.get(b).setMana(ManaManager.get(b).maxMana() / 2);
            BOTS.add(b);
        }
        return BOTS.size();
    }

    /** Sends every stand-in inside {@code radius} of {@code at} (a boss's arena), staying there. */
    public static void gather(Vec3 at, double radius) {
        for (Bot b : BOTS) {
            Vec3 p = spot(b.serverLevel(), at, radius);
            b.teleportTo(p.x, p.y, p.z);
            b.target = null;
        }
    }

    public static int clear() {
        int n = BOTS.size();
        for (Bot b : BOTS) {
            players(b.getServer()).remove(b);
            listed(b.getServer()).remove(b.getUUID());
            b.serverLevel().removePlayerImmediately(b, Entity.RemovalReason.DISCARDED);
        }
        BOTS.clear();
        return n;
    }

    /** The player list's own list (its getter is read-only), so the stand-ins count as online. */
    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> players(MinecraftServer server) {
        return (List<ServerPlayer>) net.neoforged.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class, server.getPlayerList(), "players");
    }

    /** The player list's own index by id, so code that looks players up there finds the stand-ins too. */
    @SuppressWarnings("unchecked")
    private static java.util.Map<UUID, ServerPlayer> listed(MinecraftServer server) {
        return (java.util.Map<UUID, ServerPlayer>) net.neoforged.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class, server.getPlayerList(), "playersByUUID");
    }

    private static Vec3 spot(ServerLevel level, Vec3 around, double r) {
        double a = RANDOM.nextDouble() * Math.PI * 2, d = Math.sqrt(RANDOM.nextDouble()) * r;
        int x = (int) Math.floor(around.x + Math.cos(a) * d), z = (int) Math.floor(around.z + Math.sin(a) * d);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5, Math.max(y, level.getMinBuildHeight() + 1), z + 0.5);
    }

    /** Each stand-in walks a little towards a point near its home, then ticks as a connected player would. */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Pre event) {
        if (BOTS.isEmpty()) return;
        BOTS.removeIf(Entity::isRemoved);
        for (Bot b : BOTS) {
            if (b.target == null || b.position().distanceToSqr(b.target) < 1) b.target = spot(b.serverLevel(), b.home, b.roam);
            Vec3 step = b.target.subtract(b.position()).multiply(1, 0, 1);
            if (step.lengthSqr() > 0.0484) step = step.normalize().scale(0.22);
            int y = b.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(b.getX() + step.x),
                    (int) Math.floor(b.getZ() + step.z));
            if (!b.isDeadOrDying()) b.moveTo(b.getX() + step.x, y, b.getZ() + step.z, (float) Math.toDegrees(Math.atan2(-step.x, step.z)), 0);
            b.doTick();
            // They stand in for players who would fight back and heal: they take every blow, and never stay down.
            if (b.getHealth() < b.getMaxHealth()) b.setHealth(b.getMaxHealth());
            b.clearFire();
            b.getFoodData().setFoodLevel(20);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        clear();
    }
}
