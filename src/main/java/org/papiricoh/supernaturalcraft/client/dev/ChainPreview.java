package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Hierarchy;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainSection;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SN_PREVIEW=chain}: the Chain of Command, well away from the origin. Both chains are filled (a Lord, Lieutenants,
 * Captains and Soldiers, one angel Fallen, a few kills against the player and three days of conclaves), then a demon Captain
 * materializes with its title card, an angel Lord shows its ring, and the book's tab opens on the Pit (an officer chosen) and
 * the Host.
 */
final class ChainPreview {

    private static final BlockPos AT = new BlockPos(620, 0, -540);
    private static int t = -1;
    private static Vec3 base;
    private static final List<Mob> SPAWNED = new ArrayList<>();

    private ChainPreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"chain".equals(DevPreview.env())) return false;
        var server = mc.getSingleplayerServer();
        int now = ++t;
        server.execute(() -> world(mc, server.getPlayerList().getPlayers().getFirst(), now));
        if (now == 170) mc.setScreen(new HunterBookScreen(HunterBookScreen.Tab.CHAIN));
        if (mc.screen instanceof HunterBookScreen b && b.section() instanceof ChainSection c) {
            if (now == 180) {
                c.showFaction(ChainFaction.DEMON);
                c.fitNow();
            }
            if (now == 200) c.jumpTo(c.firstOfRank(ChainRank.LORD), 2.6f);
            if (now == 220) {
                c.select(c.firstOfRank(ChainRank.CAPTAIN));
                c.settle();
            }
            if (now == 240) {
                c.showFaction(ChainFaction.ANGEL);
                c.fitNow();
            }
            if (now == 256) c.openEvents();
        }
        for (int[] s : new int[][]{{38, 0}, {75, 1}, {150, 2}, {195, 3}, {215, 4}, {234, 5}, {252, 6}, {268, 7}})
            if (now == s[0]) Screenshot.grab(mc.gameDirectory, "sn_chain_" + s[1] + ".png", mc.getMainRenderTarget(), m -> {
            });
        if (now >= 280) {
            mc.setScreen(null);
            server.execute(() -> {
                for (Mob m : SPAWNED) if (!m.isRemoved()) m.discard();
                SPAWNED.clear();
            });
            t = -1;
            DevPreview.stop(mc);
        }
        return true;
    }

    private static void world(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = false;
            p.setGameMode(GameType.CREATIVE);
            level.setDayTime(13500);
            level.setWeatherParameters(6000, 0, false, false);
            level.getChunk(AT.getX() >> 4, AT.getZ() >> 4);
            base = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AT));
            ChainEvents.reset(level.getServer());
            fill(level.getServer(), p);
            view(p, base.add(0, 1.6, -9), base.add(0, 1.2, 0));
        }
        var data = ChainSavedData.get(level.getServer());
        if (now == 20) {
            Officer captain = data.hierarchy(ChainFaction.DEMON).ofRank(ChainRank.CAPTAIN).getFirst();
            Mob m = OfficerSpawner.at(captain, level, BlockPos.containing(base), p);
            if (m != null) {
                m.setNoAi(true);
                m.setYRot(180);
                m.setYHeadRot(180);
                m.setYBodyRot(180);
                SPAWNED.add(m);
            }
        }
        if (now == 100) {
            Officer lord = data.hierarchy(ChainFaction.ANGEL).lord();
            Mob m = lord == null ? null : OfficerSpawner.at(lord, level, BlockPos.containing(base.add(3, 0, 0)), null);
            if (m != null) {
                m.setNoAi(true);
                SPAWNED.add(m);
            }
            view(p, base.add(1.5, 2.4, -7), base.add(1.5, 1, 0));
        }
    }

    private static void fill(net.minecraft.server.MinecraftServer server, ServerPlayer p) {
        var data = ChainSavedData.get(server);
        for (ChainFaction f : ChainFaction.values()) data.setAwake(f, true);
        // The tab is the journal entry's: Azazel's death opens it.
        org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, "main/yellow_eyed");
        long seed = 0x5EED;
        for (ChainFaction f : ChainFaction.values()) {
            Hierarchy h = data.hierarchy(f);
            String type = f == ChainFaction.DEMON ? "supernaturalcraft:black_eyed_demon" : "supernaturalcraft:angel";
            Officer lord = h.recruit(ChainRank.LORD, Officer.NONE, seed++ * 977, type, 0);
            List<Officer> lts = new ArrayList<>();
            for (int i = 0; i < 2; i++) lts.add(h.recruit(ChainRank.LIEUTENANT, lord.id, seed++ * 977, type, i));
            List<Officer> caps = new ArrayList<>();
            for (int i = 0; i < 4; i++) caps.add(h.recruit(ChainRank.CAPTAIN, lts.get(i % 2).id, seed++ * 977, type, i));
            for (int i = 0; i < 6; i++) {
                Officer s = h.recruit(ChainRank.SOLDIER, caps.get(i % 4).id, seed++ * 977, type, i);
                if (s != null && i == 1) s.recordKill(p.getUUID());
            }
            caps.getFirst().recordKill(p.getUUID());
            caps.getFirst().recordKill(p.getUUID());
            caps.getFirst().level = 3;
            lord.level = 5;
            if (f == ChainFaction.ANGEL) h.fall(caps.get(3));
        }
        for (int d = 0; d < 3; d++) ChainEvents.conclave(server);
        data.changed();
        ChainEvents.sync(p);
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }
}
