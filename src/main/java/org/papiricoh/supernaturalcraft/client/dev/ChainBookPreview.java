package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainSection;
import org.papiricoh.supernaturalcraft.client.book.chain.OfficerModal;
import org.papiricoh.supernaturalcraft.client.chain.ChainTalkScreen;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.client.chain.DuelHud;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.network.ChainDuelPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Bond;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Challenge;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;
import org.papiricoh.supernaturalcraft.network.ChainTalkPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Chain of Command's v0.27 GUI (no server data needed: a realistic snapshot is written straight into {@link ClientChain}
 * every tick: both chains of 15 officers, the reader holding the Pit's throne, bonds of every kind, two challenges on the
 * reader, a war in progress). Screenshots {@code sn_<scene>_<shot>_s<scale>.png}:
 * <ul>
 *   <li>{@code chain_modal}: an officer's record, every tab (actions with a Lord's order open), a Host officer, a Fallen one, the
 *   reader's own post; at GUI scales 2 and 3.</li>
 *   <li>{@code chain_bonds}: the tree with the bonds drawn, the Pit (the reader's face on the throne) and the Host.</li>
 *   <li>{@code chain_war}: the front.</li>
 *   <li>{@code chain_duel}: two officers in a duel ring with the banner, a blow landing, then the winner.</li>
 *   <li>{@code chain_talk}: a demon speaking to the reader, an angel throwing down a challenge, an officer's last word.</li>
 * </ul>
 */
final class ChainBookPreview {

    static final List<String> NAMES = List.of("chain_modal", "chain_bonds", "chain_war", "chain_duel", "chain_talk");
    private static final BlockPos AT = new BlockPos(760, 0, -640);
    private static final int SHOT = 34;

    private record Beat(String name, int scale, Consumer<Minecraft> setup) {
    }

    private static List<String> queue;
    private static List<Beat> beats;
    private static int t = -1, beat, beatStart;
    private static Vec3 base;
    private static final List<Entity> SPAWNED = new ArrayList<>();
    private static Snapshot fake;
    private static int guiScale = -1;

    private ChainBookPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) if (!s.isBlank()) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
        }
        var server = mc.getSingleplayerServer();
        t++;
        if (t == 0) {
            guiScale = mc.options.guiScale().get();
            mc.options.hideGui = false;
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst()));
        }
        if (t < 60) return true;
        if (fake == null) fake = new Snapshot(mc);
        fake.apply();
        if (beats == null) {
            beats = plan(queue.getFirst());
            beat = 0;
            beatStart = t;
        }
        if (beat >= beats.size()) {
            String done = queue.removeFirst();
            if ("chain_duel".equals(done)) server.execute(ChainBookPreview::cleanUp);
            DuelHud.clear();
            mc.setScreen(null);
            beats = null;
            if (queue.isEmpty()) {
                queue = null;
                fake = null;
                t = -1;
                server.execute(() -> Allegiances.set(server.getPlayerList().getPlayers().getFirst(), Allegiance.HUMAN));
                mc.options.guiScale().set(Math.max(0, guiScale));
                mc.resizeDisplay();
                DevPreview.stop(mc);
            }
            return true;
        }
        Beat b = beats.get(beat);
        int local = t - beatStart;
        if ("chain_duel_start".equals(b.name)) duelTick(mc);
        if (local == 1) {
            if (mc.options.guiScale().get() != b.scale) {
                mc.options.guiScale().set(b.scale);
                mc.resizeDisplay();
            }
            b.setup.accept(mc);
        }
        if (local == 20 && mc.screen instanceof HunterBookScreen book && book.section() instanceof ChainSection c) c.settle();
        if (local == 22 && mc.screen instanceof ChainTalkScreen talk) talk.finishTyping();
        if (local == 26) mc.getToasts().clear();
        if (local == SHOT - 4) Screenshot.grab(mc.gameDirectory, "sn_" + b.name + "_s" + b.scale + ".png", mc.getMainRenderTarget(), m -> {
        });
        if (local >= SHOT) {
            beat++;
            beatStart = t;
        }
        return true;
    }

    // --- scenes ----------------------------------------------------------------------------------

    private static List<Beat> plan(String scene) {
        List<Beat> out = new ArrayList<>();
        switch (scene) {
            case "chain_modal" -> {
                for (int s : new int[]{2, 3}) {
                    out.add(new Beat("chain_modal_chronicle", s, mc -> modal(mc, "demon", 7, OfficerModal.Tab.CHRONICLE, false)));
                    out.add(new Beat("chain_modal_actions", s, mc -> modal(mc, "demon", 7, OfficerModal.Tab.ACTIONS, true)));
                }
                out.add(new Beat("chain_modal_bonds", 2, mc -> modal(mc, "demon", 7, OfficerModal.Tab.BONDS, false)));
                out.add(new Beat("chain_modal_duels", 2, mc -> modal(mc, "demon", 3, OfficerModal.Tab.DUELS, false)));
                out.add(new Beat("chain_modal_host", 2, mc -> modal(mc, "angel", 2, OfficerModal.Tab.DUELS, false)));
                out.add(new Beat("chain_modal_host_actions", 2, mc -> modal(mc, "angel", 4, OfficerModal.Tab.ACTIONS, false)));
                out.add(new Beat("chain_modal_fallen", 2, mc -> modal(mc, "angel", 9, OfficerModal.Tab.BONDS, false)));
                out.add(new Beat("chain_modal_hunter", 2, mc -> modal(mc, "demon", 1, OfficerModal.Tab.CHRONICLE, false)));
                out.add(new Beat("chain_modal_board", 2, mc -> modal(mc, "leviathan", 2, OfficerModal.Tab.CHRONICLE, false)));
                out.add(new Beat("chain_modal_board_lord", 2, mc -> modal(mc, "leviathan", 1, OfficerModal.Tab.ACTIONS, false)));
            }
            case "chain_bonds" -> {
                out.add(new Beat("chain_bonds_pit", 2, mc -> tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.DEMON)));
                out.add(new Beat("chain_bonds_board", 2, mc -> tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.LEVIATHAN)));
                out.add(new Beat("chain_bonds_pit", 3, mc -> tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.DEMON)));
                out.add(new Beat("chain_bonds_host", 2, mc -> tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.ANGEL)));
                out.add(new Beat("chain_events", 2, mc -> {
                    ChainSection c = tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.DEMON);
                    c.showBonds(false);
                    c.openEvents();
                }));
            }
            case "chain_war" -> {
                for (int s : new int[]{2, 3}) out.add(new Beat("chain_war", s, mc -> tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.DEMON).openWar()));
            }
            case "chain_duel" -> {
                out.add(new Beat("chain_duel_start", 2, ChainBookPreview::duelStart));
                out.add(new Beat("chain_duel_blow", 2, mc -> mc.getSingleplayerServer().execute(() -> hurt(0.35f))));
                out.add(new Beat("chain_duel_won", 2, mc -> DuelHud.set(ChainDuelPayload.end(41, demonId))));
                out.add(new Beat("chain_duel_yield", 3, ChainBookPreview::duelYield));
            }
            default -> {
                out.add(new Beat("chain_talk_demon", 2, mc -> ChainTalkScreen.open(new ChainTalkPayload(-1, "demon", 7,
                        "chain.supernaturalcraft.voice.demon.greet_ally.any.0", List.of(), "ally",
                        List.of("rumors", "pact", "aid", "sponsor", "leave"), 0))));
                out.add(new Beat("chain_talk_challenge", 2, mc -> ChainTalkScreen.open(new ChainTalkPayload(-1, "angel", 2,
                        "chain.supernaturalcraft.voice.angel.challenge_issued.any.0", List.of(), "challenge",
                        List.of("accept", "decline"), 900))));
                out.add(new Beat("chain_talk_end", 2, mc -> ChainTalkScreen.open(new ChainTalkPayload(-1, "angel", 9,
                        "chain.supernaturalcraft.voice.demon.message_accepted.any.0", List.of(), "end", List.of(), 0))));
                out.add(new Beat("chain_talk_demon", 3, mc -> ChainTalkScreen.open(new ChainTalkPayload(-1, "demon", 3,
                        "chain.supernaturalcraft.voice.demon.audience_greet.any.0", List.of(), "audience",
                        List.of("task", "promotion", "throne", "leave"), 600))));
            }
        }
        return out;
    }

    private static ChainSection tree(Minecraft mc, org.papiricoh.supernaturalcraft.chain.ChainFaction f) {
        HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.CHAIN);
        mc.setScreen(book);
        ChainSection c = (ChainSection) book.section();
        c.showFaction(f);
        c.showBonds(true);
        // A new book's section has not laid its tree out yet.
        c.firstOfRank(org.papiricoh.supernaturalcraft.chain.ChainRank.LORD);
        c.fitNow();
        return c;
    }

    private static void modal(Minecraft mc, String faction, int id, OfficerModal.Tab tab, boolean ordering) {
        ChainSection c = tree(mc, org.papiricoh.supernaturalcraft.chain.ChainFaction.byId(faction));
        c.showBonds(false);
        Entry e = ClientChain.entry(faction, id);
        if (e == null) return;
        c.openOfficer(e);
        c.modalTab(tab, ordering);
    }

    // --- the duel --------------------------------------------------------------------------------

    private static int demonId = -1, angelId = -1;

    private static void duelStart(Minecraft mc) {
        mc.setScreen(null);
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = p.serverLevel();
            cleanUp();
            BlackEyedDemon d = AllEntities.BLACK_EYED_DEMON.get().create(level);
            AngelEntity a = AllEntities.ANGEL.get().create(level);
            place(level, d, base.add(-2.5, 0, 0), -90);
            place(level, a, base.add(2.5, 0, 0), 90);
            d.setOfficerId(8);
            d.setOfficerRank(1);
            a.setOfficerId(7);
            a.setOfficerRank(1);
            a.setVessel(1);
            demonId = d.getId();
            angelId = a.getId();
            view(p, base.add(0.5, 3.2, -9.5), base.add(0, 1, 0));
        });
    }

    private static void place(ServerLevel level, Mob m, Vec3 at, float yaw) {
        m.moveTo(at.x, at.y, at.z, yaw, 0);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        SPAWNED.add(m);
    }

    private static void hurt(float share) {
        for (Entity e : SPAWNED) {
            if (e instanceof AngelEntity a) a.setHealth(a.getMaxHealth() * share);
            if (e instanceof BlackEyedDemon d) d.setHealth(d.getMaxHealth() * 0.8f);
        }
    }

    private static void duelYield(Minecraft mc) {
        DuelHud.set(new ChainDuelPayload(42, false, base.x, base.y, base.z, 6, demonId, angelId,
                ClientChain.name(ClientChain.entry("demon", 8)).getString(), ClientChain.name(ClientChain.entry("angel", 7)).getString(), false, -1));
    }

    private static void cleanUp() {
        for (Entity e : SPAWNED) if (!e.isRemoved()) e.discard();
        SPAWNED.clear();
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void setUp(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        p.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.getChunk(AT.getX() >> 4, AT.getZ() >> 4);
        base = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AT));
        org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, "main/yellow_eyed");
        Allegiances.set(p, new Allegiance(Faction.DEMON, 2, 1, 0, 0, 0, -1, Allegiance.MESSENGER_NONE, 0, false));
        view(p, base.add(0.5, 3.2, -9.5), base.add(0, 1, 0));
    }

    // --- the snapshot ----------------------------------------------------------------------------

    /** Both chains, their bonds, two challenges on the reader and a war, as the server would sync them. */
    private static final class Snapshot {
        final List<Entry> officers = new ArrayList<>();
        final List<History> history = new ArrayList<>();
        final List<ChainSyncPayload.Court> courts = new ArrayList<>();
        final List<Bond> bonds = new ArrayList<>();
        final List<Challenge> challenges = new ArrayList<>();
        final ChainSyncPayload.War war;
        final ChainSyncPayload.Task task;

        Snapshot(Minecraft mc) {
            String uuid = mc.player.getUUID().toString(), me = "player:" + uuid, name = mc.player.getGameProfile().getName();
            long day = mc.level.getDayTime() / 24000;
            String[] ranks = {"LORD", "COMMANDER", "COMMANDER", "LIEUTENANT", "LIEUTENANT", "LIEUTENANT", "CAPTAIN", "CAPTAIN",
                    "CAPTAIN", "CAPTAIN", "SOLDIER", "SOLDIER", "SOLDIER", "SOLDIER", "SOLDIER"};
            int[] sup = {0, 1, 1, 2, 2, 3, 4, 5, 6, 4, 7, 8, 9, 10, 7};
            int[] favorD = {0, 10, -85, 30, -20, 0, -45, 20, 55, -10, 0, 65, 0, -30, 5};
            int[] favorA = {-20, -50, 0, 15, -10, 0, -60, 0, 40, 0, -15, 0, 0, 0, 0};
            String[] board = {"supernaturalcraft:dick_roman", "supernaturalcraft:leviathan_executive", "supernaturalcraft:leviathan_chemist",
                    "supernaturalcraft:leviathan_guard"};
            for (int f = 0; f < 3; f++) {
                String faction = f == 0 ? "demon" : f == 1 ? "angel" : "leviathan",
                        type = f == 0 ? "supernaturalcraft:black_eyed_demon" : f == 1 ? "supernaturalcraft:angel" : board[3];
                for (int i = 0; i < 15; i++) {
                    int id = i + 1;
                    boolean player = f == 0 && id == 1;
                    String branch = f == 1 && (id == 6 || id == 9) ? "FALLEN" : "LOYAL";
                    double a = (f == 0 ? 0.3 : 3.6) + i * 0.17;
                    int r = 600 + (i * 157) % 1900;
                    officers.add(new Entry(id, faction, branch, ranks[i], Math.max(1, 6 - i / 3 + (i % 2)), sup[i], 0x5EEDL * 977 + f * 131 + i * 31,
                            "ALIVE", i % 5 == 3 ? 1 : 0, f == 0 && id == 7 ? 2 : f == 1 && id == 2 ? 1 : 0,
                            player ? "" : f == 2 ? board[Math.min(i, 3) == 0 ? 0 : 1 + i % 3] : type, i % 4,
                            (int) (Math.cos(a) * r), (int) (Math.sin(a) * r), "minecraft:overworld", (int) (Math.cos(a) * r) + 40,
                            (int) (Math.sin(a) * r) - 25, i % 3 == 0 ? -1 : day - i % 4, player ? uuid : "", player ? name : "",
                            f == 0 ? favorD[i] : favorA[i], i % 4));
                }
                officers.add(new Entry(16, faction, "LOYAL", "SOLDIER", 1, 7, 0x5EEDL * 977 + f * 131 + 999, "RETURNING", 0, 0, type, 1,
                        0, 0, "minecraft:overworld", 0, 0, -1, "", "", 0, 0));
            }
            courts.add(new ChainSyncPayload.Court("demon", true, 1500, 70, 420, -1));
            courts.add(new ChainSyncPayload.Court("angel", true, -900, 82, -1700, -1));
            courts.add(new ChainSyncPayload.Court("leviathan", true, 1700, 64, -1500, -1));
            // Bonds of every kind, across the sides and towards the reader.
            bond("demon:7", "demon:8", -55, false);
            bond("demon:8", "demon:7", -76, false);
            bond("demon:2", "demon:3", -45, false);
            bond("demon:3", "demon:2", -38, false);
            bond("demon:4", "demon:2", 62, false);
            bond("demon:9", "demon:10", 42, true);
            bond("demon:10", "demon:9", 40, true);
            bond("demon:11", "demon:7", 55, false);
            bond("demon:7", "angel:7", -82, false);
            bond("demon:5", "angel:4", -71, false);
            bond("angel:7", "demon:7", -64, false);
            bond("angel:9", "demon:6", 70, false);
            bond("demon:3", me, -85, false);
            bond("demon:12", me, 65, false);
            bond("demon:7", me, -45, false);
            bond("angel:2", me, -50, false);
            bond(me, "demon:3", -60, false);
            bond("angel:2", "angel:3", -45, false);
            bond("angel:3", "angel:2", -72, false);
            bond("angel:5", "angel:4", 55, false);
            bond("angel:11", "angel:12", 35, true);
            bond("angel:12", "angel:11", 33, true);
            bond("angel:8", "angel:10", -48, false);
            challenges.add(new Challenge(1, "OFFICER_OFFICER", "demon:8", "demon:7", false, 0, "PENDING", day));
            challenges.add(new Challenge(2, "THRONE", "demon:3", me, false, 31000, "PENDING", day));
            challenges.add(new Challenge(3, "OFFICER_HUNTER", "angel:2", me, true, 9400, "PENDING", day));
            // Pressure per sector and faction (demon, angel, leviathan); the Leviathans hold sectors 10 and 11.
            int[] push = new int[12 * 3];
            push[2 * 3] = 2; push[3 * 3 + 1] = 4; push[7 * 3] = 3; push[8 * 3 + 2] = 5; push[11 * 3 + 1] = 2;
            war = new ChainSyncPayload.War(day + 3, "demon", "angel", new int[]{5, 3, 0}, new int[]{14, 11, 6},
                    new byte[]{0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 2, 2}, push);
            task = new ChainSyncPayload.Task("SLAY", "demon", "angel", 7, 0, day + 4);
            long d = Math.max(12, day);
            h(d - 9, "founded", "demon", 2, "", "", 0);
            h(d - 8, "sworn", "demon", 1, me, "", 0);
            h(d - 7, "promote", "demon", 7, "rank:CAPTAIN", "", 0);
            h(d - 6, "rivalry", "demon", 7, "", "demon", 8);
            h(d - 6, "kill", "demon", 7, me, "", 0);
            h(d - 5, "pact", "demon", 9, "", "demon", 10);
            h(d - 5, "player_promoted", "demon", 1, "rank:LORD", "", 0);
            h(d - 4, "skirmish_seen", "demon", 7, "sector:4", "angel", 7);
            h(d - 4, "grudge", "demon", 8, "", "demon", 7);
            h(d - 3, "war_declared", "demon", 1, "days:5", "", 0);
            h(d - 3, "challenge", "demon", 8, "", "demon", 7);
            h(d - 3, "duel_won", "demon", 7, "", "angel", 11);
            h(d - 2, "vow", "angel", 1, "", "demon", 3);
            h(d - 2, "challenge_hunter", "demon", 3, me, "", 0);
            h(d - 2, "insult", "angel", 2, me, "", 0);
            h(d - 1, "sector_taken", "angel", 2, "sector:9", "", 0);
            h(d - 1, "champion_won", "demon", 2, "", "angel", 4);
            h(d - 1, "fall", "angel", 9, "", "", 0);
            h(d, "skirmish", "demon", 7, "sector:3", "angel", 8);
            h(d, "duel_yield", "demon", 3, "", "demon", 2);
        }

        private void bond(String from, String to, int regard, boolean pact) {
            bonds.add(new Bond(from, to, regard, pact));
        }

        private void h(long day, String key, String faction, int id, String detail, String otherFaction, int otherId) {
            history.add(new History(day, key, faction, id, detail, otherFaction, otherId));
        }

        void apply() {
            ClientChain.officers = officers;
            ClientChain.history = history;
            ClientChain.courts = courts;
            ClientChain.bonds = bonds;
            ClientChain.challenges = challenges;
            ClientChain.war = war;
            ClientChain.task = task;
        }
    }

    /** Called each tick while the duel scene runs: sends the start once the mobs exist on the client. */
    static void duelTick(Minecraft mc) {
        if (DuelHud.current() == null && demonId >= 0 && mc.level != null && mc.level.getEntity(demonId) != null) {
            DuelHud.set(new ChainDuelPayload(41, false, base.x, base.y, base.z, 6, demonId, angelId,
                    ClientChain.name(ClientChain.entry("demon", 8)).getString(), ClientChain.name(ClientChain.entry("angel", 7)).getString(), true, -1));
        }
    }
}
