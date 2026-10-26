package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainGates;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Chain's v0.26 scenes. {@code chain_dungeon}: a tour of both strongholds, empty (the chains asleep): from the air, from its
 * gate, every room of its plan and the throne ({@code sn_chain_dungeon_<faction>_<n>}). {@code chain_all}: everything at once
 * — each stronghold manned (garrison, Lord, Commanders, card and bars), then a feud staged beside the player
 * ({@code sn_chain_all_<n>}). {@code chain_mobs}: the nine of the rank and file and the repainted first angel vessel in a row.
 * {@code chain_gui}: a demon Lord and a Captain materialize beside the player — the officer card, then the bars.
 */
final class StrongholdPreview {

    static final List<String> NAMES = List.of("chain_dungeon", "chain_all", "chain_mobs", "chain_gui");
    /** Ticks per stronghold in the tour: raising, the air, the gate, then the rooms. */
    private static final int PER = 1400, ROOM = 40;
    private static final BlockPos AT = new BlockPos(700, 0, -620);
    private static int t = -1;
    private static Vec3 base;
    private static final List<Mob> SPAWNED = new ArrayList<>();

    private StrongholdPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = DevPreview.env();
        if (!NAMES.contains(scene)) return false;
        var server = mc.getSingleplayerServer();
        int now = ++t;
        int end = switch (scene) { case "chain_dungeon" -> 2 * PER; case "chain_all" -> 2 * PER + 400; case "chain_mobs" -> 140; default -> 260; };
        server.execute(() -> world(mc, server.getPlayerList().getPlayers().getFirst(), scene, now));
        if (now >= end) {
            server.execute(() -> {
                for (Mob m : SPAWNED) if (!m.isRemoved()) m.discard();
                SPAWNED.clear();
            });
            t = -1;
            DevPreview.stop(mc);
        }
        return true;
    }

    private static void grab(Minecraft mc, String name) {
        mc.execute(() -> Screenshot.grab(mc.gameDirectory, "sn_" + name + ".png", mc.getMainRenderTarget(), m -> {
        }));
    }

    private static void world(Minecraft mc, ServerPlayer p, String scene, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            p.setGameMode(GameType.CREATIVE);
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            level.getChunk(AT.getX() >> 4, AT.getZ() >> 4);
            base = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AT));
            ChainEvents.reset(level.getServer());
            for (ChainFaction f : ChainFaction.values()) ChainGates.plant(level.getServer(), f, true, 0x5EED + f.ordinal());
            // The structure tour walks empty halls: a sleeping chain mans nothing.
            if (scene.equals("chain_dungeon")) for (ChainFaction f : ChainFaction.values()) ChainSavedData.get(level.getServer()).setAwake(f, false);
            ChainEvents.sync(p);
        }
        switch (scene) {
            case "chain_dungeon" -> dungeon(mc, p, level, now, "chain_dungeon_", true);
            case "chain_all" -> {
                if (now < 2 * PER) dungeon(mc, p, level, now, "chain_all_", false);
                else feud(mc, p, level, now - 2 * PER);
            }
            case "chain_mobs" -> mobs(mc, p, level, now);
            default -> gui(mc, p, level, now);
        }
    }

    /** Two strongholds, {@link #PER} ticks each: raise, then the air, the gate, every room and the throne. */
    private static void dungeon(Minecraft mc, ServerPlayer p, ServerLevel level, int now, String prefix, boolean empty) {
        ChainFaction f = now < PER ? ChainFaction.DEMON : ChainFaction.ANGEL;
        int at = now % PER;
        String tag = prefix + f.id() + "_";
        if (at == 2) {
            mc.options.hideGui = empty;
            BlockPos o = SeatWorld.locate(level.getServer(), f);
            view(p, Vec3.atCenterOf(o).add(0, 50, 75), Vec3.atCenterOf(o));
            SeatWorld.ensure(level, f);
        }
        SeatWorld.Anchors a = SeatWorld.built(level.getServer(), f) == null ? null : SeatWorld.ensure(level, f);
        if (a == null) return;
        Vec3 o = Vec3.atCenterOf(a.origin());
        if (empty) {
            if (at == 260) view(p, o.add(0, 50, 75), o);
            if (at == 300) grab(mc, tag + "air_front");
            if (at == 310) view(p, o.add(55, 40, 55), o);
            if (at == 350) grab(mc, tag + "air_corner");
            if (at == 360) view(p, o.add(-60, 30, -45), o);
            if (at == 400) grab(mc, tag + "air_back");
        }
        if (at == 410) view(p, Vec3.atBottomCenterOf(a.entrance()).add(0, 2.2, 0), Vec3.atBottomCenterOf(a.arrival()).add(0, 1.5, 0));
        if (at == 460) grab(mc, tag + "gate");
        // One stop per room of the plan, standing among its guard posts and looking at their middle.
        List<String> rooms = rooms(a);
        int stop = (at - 470) / ROOM;
        if (at >= 470 && stop < Math.min(rooms.size(), 18)) {
            String room = rooms.get(stop);
            Vec3 c = centre(a, room);
            if ((at - 470) % ROOM == 0) {
                Vec3 eye = c.add(Vec3.atBottomCenterOf(a.entrance()).subtract(c).multiply(1, 0, 1).normalize().scale(2.5)).add(0, 2.6, 0);
                view(p, eye, c.add(0, 1, 0));
            }
            if ((at - 470) % ROOM == ROOM - 4) grab(mc, tag + "room_" + room);
        }
        int throne = 470 + Math.min(rooms.size(), 18) * ROOM;
        if (at == throne) {
            mc.options.hideGui = empty;
            Vec3 lord = Vec3.atBottomCenterOf(a.lord());
            Vec3 into = Vec3.atBottomCenterOf(a.entrance()).subtract(lord).multiply(1, 0, 1).normalize();
            view(p, lord.add(into.scale(9)).add(0, 1.7, 0), lord.add(0, 1.4, 0));
        }
        if (at == throne + 60) grab(mc, tag + "throne");
        if (at == throne + 140) grab(mc, tag + "throne_late");
        if (!a.vaults().isEmpty() && at == throne + 150) {
            Vec3 v = Vec3.atBottomCenterOf(a.vaults().getFirst());
            view(p, v.add(0, 2.2, 3.5), v);
        }
        if (!a.vaults().isEmpty() && at == throne + 190) grab(mc, tag + "vault");
    }

    private static List<String> rooms(SeatWorld.Anchors a) {
        List<String> out = new ArrayList<>();
        for (String name : a.guards().keySet()) {
            String room = org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors.roomOf(name);
            if (room != null && !out.contains(room)) out.add(room);
        }
        return out;
    }

    private static Vec3 centre(SeatWorld.Anchors a, String room) {
        Vec3 sum = Vec3.ZERO;
        int n = 0;
        for (var e : a.guards().entrySet())
            if (room.equals(org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors.roomOf(e.getKey()))) {
                sum = sum.add(Vec3.atBottomCenterOf(e.getValue()));
                n++;
            }
        return sum.scale(1.0 / Math.max(1, n));
    }

    /** A feud staged beside the player, after the strongholds of {@code chain_all}. */
    private static void feud(Minecraft mc, ServerPlayer p, ServerLevel level, int at) {
        if (at == 1) {
            mc.options.hideGui = false;
            view(p, base.add(0, 1.8, -10), base.add(0, 1.2, 0));
        }
        if (at == 20) {
            var data = ChainSavedData.get(level.getServer());
            var court = data.court(ChainFaction.DEMON);
            var pair = org.papiricoh.supernaturalcraft.chain.Feud.pick(data.hierarchy(ChainFaction.DEMON), false,
                    org.papiricoh.supernaturalcraft.chain.ChainRules.rng(1, 0, 0));
            if (pair != null) {
                court.feudChallenger = pair.challenger().id;
                court.feudDefender = pair.defender().id;
                data.changed();
                org.papiricoh.supernaturalcraft.chain.FeudStage.here(p, ChainFaction.DEMON);
            }
        }
        if (at == 90) grab(mc, "chain_all_feud_0");
        if (at == 220) grab(mc, "chain_all_feud_1");
        if (at == 360) grab(mc, "chain_all_feud_2");
    }

    private static void mobs(Minecraft mc, ServerPlayer p, ServerLevel level, int now) {
        if (now == 2) {
            mc.options.hideGui = true;
            List<EntityType<? extends Mob>> types = List.of(AllEntities.DEMON_BRUTE.get(), AllEntities.SULFUR_PRIEST.get(),
                    AllEntities.DEMON_FLAYER.get(), AllEntities.DEMON_TORMENTOR.get(), AllEntities.ANGEL.get(),
                    AllEntities.ANGEL_TEMPLAR.get(), AllEntities.ANGEL_HERALD.get(), AllEntities.ANGEL_CHOIRMASTER.get(),
                    AllEntities.ANGEL_DOMINION.get(), AllEntities.GRIGORI.get());
            for (int i = 0; i < types.size(); i++) {
                Mob m = types.get(i).create(level);
                if (m == null) continue;
                m.moveTo(base.add(-9 + i * 2, 0, 0), 180, 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
                m.setNoAi(true);
                m.setYHeadRot(180);
                m.setYBodyRot(180);
                if (m instanceof AngelEntity a && types.get(i) == AllEntities.ANGEL.get()) a.setVessel(0);
                level.addFreshEntity(m);
                SPAWNED.add(m);
            }
            view(p, base.add(-5, 1.8, -7), base.add(-5, 1.1, 0));
        }
        if (now == 70) grab(mc, "chain_mobs_0");
        if (now == 80) view(p, base.add(5, 1.8, -7), base.add(5, 1.1, 0));
        if (now == 130) grab(mc, "chain_mobs_1");
    }

    private static void gui(Minecraft mc, ServerPlayer p, ServerLevel level, int now) {
        var data = ChainSavedData.get(level.getServer());
        if (now == 2) {
            mc.options.hideGui = false;
            view(p, base.add(0, 1.6, -8), base.add(0, 1.2, 0));
        }
        if (now == 20) {
            Officer lord = data.hierarchy(ChainFaction.DEMON).lord();
            Officer captain = data.hierarchy(ChainFaction.DEMON).ofRank(ChainRank.CAPTAIN).getFirst();
            for (Officer o : new Officer[]{lord, captain}) {
                Mob m = OfficerSpawner.at(o, level, BlockPos.containing(base.add(o == lord ? -1.5 : 1.5, 0, 0)), p);
                if (m != null) {
                    m.setNoAi(true);
                    m.setYRot(180);
                    m.setYHeadRot(180);
                    m.setYBodyRot(180);
                    SPAWNED.add(m);
                }
            }
        }
        if (now == 60) grab(mc, "chain_gui_0");
        if (now == 200) grab(mc, "chain_gui_1");
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }
}
