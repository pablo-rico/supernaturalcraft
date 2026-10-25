package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.RecitationFx;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.entity.magic.SpellCarrier;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload.Kind;
import org.papiricoh.supernaturalcraft.network.RitualChargePayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SN_PREVIEW=magic_fx,magic_rituals,magic_bowl,magic_witchcraft} (any of them, in any order): the magic's effects (v0.21),
 * staged on a flat floor well away from the origin and triggered with synthetic payloads straight into {@link ClientMagicFx} (no
 * server gameplay involved, but for the bolts and the glyph trap, which are real entities). Each key moment is shot wide and close
 * (5-8 blocks, {@code _close}), and each scene ends with a few moments again by day ({@code _day}): additive light must still read
 * against sunlit ground. Screenshots {@code sn_magic_<scene>_<moment>.png}.
 * <ul>
 *   <li>{@code magic_fx}: each school's cast circles (feet and hand, and the caster's own at the hand), four bolts in flight (far and close), every impact in rows of
 *   four (wide, then each pair close), chain arcs, wards raised/struck/broken, hexes, devil's trap flares under the demons and the
 *   Mark.</li>
 *   <li>{@code magic_rituals}: a chalk circle lighting up as a rite charges, the completion pillar, a failed rite's implosion,
 *   a summoning rift, and the other schools' pillars side by side.</li>
 *   <li>{@code magic_bowl}: letters rising off a boiling bowl, the seal written in smoke, sent to a distant target, and a
 *   backlash.</li>
 *   <li>{@code magic_witchcraft}: a curse flown from the bowl and landing on its victim, a Ward of the Name turning one away, a
 *   player bearing a Mirror Hex (third person), a glyph trap written, armed and sprung, and a lingering zone.</li>
 * </ul>
 */
final class MagicPreview {

    static final List<String> NAMES = List.of("magic_fx", "magic_rituals", "magic_bowl", "magic_witchcraft");
    private static final School[] SCHOOLS = {School.ENOCHIAN, School.INFERNAL, School.WITCHCRAFT, School.HUNTER};
    private static final int X = 720, Z = 720;
    private static final List<String> GREAT = List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #", "#       #",
            "# c   c #", " #  c  # ", "  #####  ");

    private static List<String> queue;
    private static String scene;
    private static int t;
    private static volatile Vec3 base;
    private static volatile BlockPos ground;
    /** Server entity ids (the integrated server and the client share them). */
    private static final int[] ids = new int[8];
    private static final int[] bolts = new int[4];
    private static Vec3 camFrom, camAt;
    private static int savedFov = -1;
    private static double savedFovEffect;

    private MagicPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
            mc.options.hideGui = true;
            // A plain 70 degrees and no flying zoom: the dev client's wider view stretches what stands at the frame's edges.
            savedFov = mc.options.fov().get();
            savedFovEffect = mc.options.fovEffectScale().get();
            mc.options.fov().set(70);
            mc.options.fovEffectScale().set(0.0);
        }
        if (scene == null) {
            if (queue.isEmpty()) {
                if (savedFov > 0) {
                    mc.options.fov().set(savedFov);
                    mc.options.fovEffectScale().set(savedFovEffect);
                }
                DevPreview.stop(mc);
                return true;
            }
            scene = queue.removeFirst();
            t = -1;
            MagicFxManager.clear();
        }
        t++;
        MinecraftServer server = mc.getSingleplayerServer();
        if (t == 0) {
            String which = scene;
            server.execute(() -> stage(server, which));
            return true;
        }
        if (base == null) return true;
        if (camFrom != null && t % 5 == 0) {
            Vec3 f = camFrom, a = camAt;
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.25) look(p, f, a);
            });
        }
        boolean done = switch (scene) {
            case "magic_fx" -> fx(mc, server);
            case "magic_rituals" -> rituals(mc, server);
            case "magic_witchcraft" -> witchcraft(mc, server);
            default -> bowl(mc, server);
        };
        if (done) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            scene = null;
            base = null;
            camFrom = null;
        }
        return true;
    }

    // --- staging -----------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        // The preview world keeps eclipses from earlier scenes: one would keep the day shots dark.
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        level.setDayTime(18000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 80;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        p.teleportTo(level, x + 0.5, g.getY() + 6, Z + 12.5, 180, 20);
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                level.setBlockAndUpdate(g.offset(dx, -1, dz), (dx + dz) % 7 == 0 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 10; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        AABB around = new AABB(g).inflate(30);
        for (Zombie old : level.getEntitiesOfClass(Zombie.class, around)) old.discard();
        for (WardEntity old : level.getEntitiesOfClass(WardEntity.class, around)) old.discard();
        switch (which) {
            case "magic_rituals" -> {
                level.setBlockAndUpdate(g, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
                for (PatternGeometry.Cell cell : PatternGeometry.cells(GREAT)) {
                    level.setBlockAndUpdate(g.offset(cell.dx(), 0, cell.dz()), cell.symbol() == 'c'
                            ? Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)
                            : AllBlocks.CHALK_LINE.get().defaultBlockState());
                }
                if (level.getBlockEntity(g) instanceof RitualAltarBlockEntity altar) {
                    altar.offerings().set(0, new ItemStack(Items.GOLD_INGOT));
                    altar.offerings().set(1, new ItemStack(AllItems.SALT.get()));
                    altar.offerings().set(2, new ItemStack(Items.AMETHYST_SHARD));
                    altar.setChanged();
                    level.sendBlockUpdated(g, altar.getBlockState(), altar.getBlockState(), 3);
                }
            }
            case "magic_bowl" -> {
                bowlAt(level, g);
                ids[0] = zombie(level, Vec3.atBottomCenterOf(g).add(13, 0, -7), 200).getId();
            }
            case "magic_witchcraft" -> {
                bowlAt(level, g.offset(-9, 0, -4));
                ids[0] = zombie(level, Vec3.atBottomCenterOf(g).add(0, 0, 0), 0).getId();
                ids[1] = zombie(level, Vec3.atBottomCenterOf(g).add(6, 0, 0), 0).getId();
                ids[2] = zombie(level, Vec3.atBottomCenterOf(g).add(-6, 0, 6), 0).getId();
            }
            default -> {
                for (int i = 0; i < 4; i++) ids[i] = zombie(level, Vec3.atBottomCenterOf(g).add(-9 + 6 * i, 0, 0), 0).getId();
            }
        }
        ground = g;
        base = Vec3.atBottomCenterOf(g);
    }

    private static void bowlAt(ServerLevel level, BlockPos at) {
        level.setBlockAndUpdate(at, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        if (level.getBlockEntity(at) instanceof SpellBowlBlockEntity bowl) {
            bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.AMETHYST_SHARD)),
                    List.of(Dose.of(BowlLiquid.HOLY_WATER), Dose.of(BowlLiquid.HOLY_WATER))));
        }
    }

    /** A still target: a husk, so day shots are not muddied by a zombie burning in the sun. */
    private static Zombie zombie(ServerLevel level, Vec3 at, float yaw) {
        Zombie z = EntityType.HUSK.create(level);
        z.moveTo(at.x, at.y, at.z, yaw, 0);
        z.setYHeadRot(yaw);
        z.setYBodyRot(yaw);
        z.setNoAi(true);
        z.setSilent(true);
        z.setInvulnerable(true);
        z.setPersistenceRequired();
        level.addFreshEntity(z);
        return z;
    }

    private static void view(MinecraftServer server, Vec3 from, Vec3 at) {
        camFrom = from;
        camAt = at;
        server.execute(() -> look(player(server), from, at));
    }

    private static void look(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    /** Day ({@code true}, a clear noon) or the scenes' night. */
    private static void daylight(MinecraftServer server, boolean day) {
        long time = day ? 6000 : 18000;
        server.execute(() -> server.overworld().setDayTime(time));
        // The server only syncs the time once a second: set the client's too, so a shot right after sees it.
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.setDayTime(time);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    private static void fire(int kind, School school, int entity, Vec3 pos, Vec3 target, float arg, float arg2, int duration) {
        ClientMagicFx.handle(new MagicFxPayload(kind, school, entity, pos, target, arg, arg2, duration));
    }

    // --- magic_fx ----------------------------------------------------------------------------------------------------------

    /** Impacts in rows of four: {style, school, radius} (radius < 0: on the ground, not on the zombie). */
    private static final Object[][] IMPACTS = {
            {"smite", School.ENOCHIAN, -1f}, {"mend", School.ENOCHIAN, 0f}, {"banishing", School.ENOCHIAN, 0f}, {"radiance", School.ENOCHIAN, 0f},
            {"halo", School.ENOCHIAN, 0f}, {"judgement", School.ENOCHIAN, -2f}, {"hellfire", School.INFERNAL, 2f}, {"brimstone", School.INFERNAL, -3f},
            {"soul_rend", School.INFERNAL, 0f}, {"hellchain", School.INFERNAL, 0f}, {"frost", School.WITCHCRAFT, -2f}, {"hex", School.WITCHCRAFT, 0f},
            {"pain", School.WITCHCRAFT, 0f}, {"glamour", School.WITCHCRAFT, 0f}, {"repel", School.HUNTER, -3f}, {"reveal", School.HUNTER, -8f},
            {"snare", School.HUNTER, 0f}, {"exorcise", School.HUNTER, 0f}, {"salt_shot", School.HUNTER, 0f}, {"silver_edge", School.HUNTER, 0f},
            {"generic", School.NONE, 0f}};
    private static final int IMPACT_FROM = 200, ROW = 110;

    /** Zombie {@code i}'s place in the fx scene. */
    private static Vec3 slot(Vec3 b, int i) {
        return b.add(-9 + 6 * i, 0, 0);
    }

    /** Fires impact {@code k} of {@link #IMPACTS} on zombie {@code i}. */
    private static void impact(Vec3 b, int k, int i) {
        if (k >= IMPACTS.length) return;
        String style = (String) IMPACTS[k][0];
        School s = (School) IMPACTS[k][1];
        float r = (Float) IMPACTS[k][2];
        Vec3 z = slot(b, i);
        boolean onGround = r < 0;
        Vec3 pos = onGround ? z.add(0, 0.1, 0) : z.add(0, 1.0, 0);
        fire(Kind.IMPACT, s, onGround ? -1 : ids[i], pos, b.add(0, 2, 12), MagicAssets.impactIndex(style), Math.abs(r), 16);
    }

    /** Four bolts, one per school, flying across behind the zombies from x = -14. */
    private static void launchBolts(MinecraftServer server, Vec3 b) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            for (int i = 0; i < 4; i++) {
                SigilBolt bolt = new SigilBolt(AllEntities.SIGIL_BOLT.get(), level);
                bolt.moveTo(b.x - 14, b.y + 1.6 + i * 0.5, b.z - 3 + i * 0.6, 0, 0);
                bolt.setDeltaMovement(0.32, 0, 0);
                level.addFreshEntity(bolt);
                bolts[i] = bolt.getId();
            }
        });
    }

    private static void boltTrails(Vec3 b) {
        for (int i = 0; i < 4; i++) {
            fire(Kind.BOLT_TRAIL, SCHOOLS[i], bolts[i], b.add(-14, 1.6 + i * 0.5, -3 + i * 0.6), b.add(-13, 1.6 + i * 0.5, -3 + i * 0.6),
                    MagicAssets.impactIndex(i == 0 ? "smite" : i == 1 ? "hellfire" : i == 2 ? "frost" : "repel"), 0.32f, 100);
        }
    }

    private static boolean fx(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        if (t == 20) view(server, b.add(0, 4.5, 13), b.add(0, 1.2, 0));
        // Cast circles: feet (tier 3) and hand (tier 2), one school per zombie.
        if (t == 40) {
            for (int i = 0; i < 4; i++) {
                Vec3 z = slot(b, i);
                fire(Kind.CAST_CIRCLE, SCHOOLS[i], ids[i], z.add(0, 0.05, 0), z.add(0, 0, 4), 3, 1, 90);
                fire(Kind.CAST_CIRCLE, SCHOOLS[i], ids[i], z.add(0, 1.4, 0.8), z.add(0, 1.4, 5), 2, 0, 90);
            }
        }
        if (t == 50) shot(mc, "circles_open");
        if (t == 90) shot(mc, "circles");
        if (t == 96) view(server, b.add(-6, 2.4, 6), b.add(-6, 0.9, 0));
        if (t == 104) shot(mc, "circles_close");
        // The caster's own circle at the hand, as they see it (tier 4, the largest).
        if (t == 108 && mc.player != null) {
            Vec3 eye = mc.player.getEyePosition(), look = mc.player.getLookAngle();
            fire(Kind.CAST_CIRCLE, School.ENOCHIAN, mc.player.getId(), eye.add(look.scale(0.6)).add(0, -0.35, 0), eye.add(look.scale(4)), 4, 0, 20);
        }
        if (t == 116) shot(mc, "circles_hand_first_person");
        // Bolts, one per school: from far (~22 blocks), then close (~6).
        if (t == 140) launchBolts(server, b);
        if (t == 143) boltTrails(b);
        if (t == 145) view(server, b.add(0, 4, 22), b.add(0, 1.8, -2));
        if (t == 165) shot(mc, "bolts_far");
        if (t == 168) view(server, b.add(1, 2.6, 4.5), b.add(-1.5, 2.2, -2));
        if (t == 176) shot(mc, "bolts");
        if (t == 186) shot(mc, "bolts_close_b");
        // Every impact, four to a row: wide, then the left pair close, then the right pair close.
        int rows = (IMPACTS.length + 3) / 4;
        for (int row = 0; row < rows; row++) {
            int at = IMPACT_FROM + row * ROW;
            if (t == at - 4) view(server, b.add(0, 3.5, 10.5), b.add(0, 1, 0));
            if (t == at) for (int i = 0; i < 4; i++) impact(b, row * 4 + i, i);
            if (t == at + 5) shot(mc, "impacts_" + row + "_a");
            if (t == at + 18) shot(mc, "impacts_" + row + "_b");
            if (t == at + 34) shot(mc, "impacts_" + row + "_c");
            for (int pair = 0; pair < 2; pair++) {
                int pt = at + 50 + pair * 30;
                if (row * 4 + pair * 2 >= IMPACTS.length) continue;
                Vec3 mid = slot(b, pair * 2).add(3, 0, 0);
                if (t == pt - 4) view(server, mid.add(0, 2.4, 6), mid.add(0, 1, 0));
                if (t == pt) {
                    impact(b, row * 4 + pair * 2, pair * 2);
                    impact(b, row * 4 + pair * 2 + 1, pair * 2 + 1);
                }
                String side = pair == 0 ? "l" : "r";
                if (t == pt + 5) shot(mc, "impacts_" + row + "_close_" + side + "_a");
                if (t == pt + 16) shot(mc, "impacts_" + row + "_close_" + side + "_b");
            }
        }
        int more = IMPACT_FROM + rows * ROW;
        // Chain arcs hopping between the zombies, wards raised, struck and broken.
        if (t == more - 4) view(server, b.add(0, 3.5, 10.5), b.add(0, 1, 0));
        if (t == more) chains(b);
        if (t == more + 3) shot(mc, "chain");
        if (t == more + 8) view(server, b.add(-3, 2.2, 5.5), b.add(-3, 1, 0));
        if (t == more + 12) chains(b);
        if (t == more + 15) shot(mc, "chain_close");
        if (t == more + 26) view(server, b.add(0, 3.5, 10.5), b.add(0, 1, -2));
        if (t == more + 30) {
            for (int i = 0; i < 3; i++) {
                fire(Kind.WARD_PULSE, SCHOOLS[i], -1, b.add(-7 + 7 * i, 0.05, -3), b.add(-7 + 7 * i, 0.05, -3), 2.5f, i, 30);
            }
        }
        if (t == more + 36) shot(mc, "wards_a");
        if (t == more + 50) shot(mc, "wards_b");
        if (t == more + 56) view(server, b.add(0, 2.6, 4), b.add(0, 0.8, -3));
        if (t == more + 60) fire(Kind.WARD_PULSE, School.ENOCHIAN, -1, b.add(0, 0.05, -3), b.add(0, 0.05, -3), 2.5f, MagicFxPayload.WARD_RAISED, 30);
        if (t == more + 68) shot(mc, "wards_close_raised");
        if (t == more + 80) fire(Kind.WARD_PULSE, School.ENOCHIAN, -1, b.add(0, 0.05, -3), b.add(0, 0.05, -3), 2.5f, MagicFxPayload.WARD_HIT, 14);
        if (t == more + 82) shot(mc, "wards_close_hit");
        if (t == more + 92) fire(Kind.WARD_PULSE, School.ENOCHIAN, -1, b.add(0, 0.05, -3), b.add(0, 0.05, -3), 2.5f, MagicFxPayload.WARD_BROKEN, 30);
        if (t == more + 97) shot(mc, "wards_close_broken");
        // Hexes, the Mark, then the devil's traps (each under its demon).
        int signs = more + 120;
        if (t == signs - 4) view(server, b.add(0, 3.5, 10.5), b.add(0, 1, 0));
        if (t == signs) signs(b);
        if (t == signs + 8) shot(mc, "signs");
        if (t == signs + 10) view(server, b.add(-6, 2.3, 5.5), b.add(-6, 1, 0));
        if (t == signs + 14) shot(mc, "signs_close_hex");
        if (t == signs + 18) view(server, b.add(6, 2.3, 5.5), b.add(6, 1, 0));
        if (t == signs + 22) shot(mc, "signs_close_mark");
        int traps = signs + 40;
        if (t == traps - 4) view(server, b.add(0, 7, 12), b.add(0, 0, 0));
        if (t == traps) traps(b, 3);
        if (t == traps + 4) shot(mc, "traps_a");
        if (t == traps + 14) shot(mc, "traps_b");
        if (t == traps + 18) view(server, b.add(-6, 3, 5.5), b.add(-6, 0.3, 0));
        if (t == traps + 22) traps(b, 2);
        if (t == traps + 26) shot(mc, "traps_close");
        if (t == traps + 36) shot(mc, "traps_close_b");
        // By day: bolts, a row of impacts, the signs.
        int day = traps + 50;
        if (t == day) daylight(server, true);
        if (t == day + 4) view(server, b.add(1, 2.6, 4.5), b.add(-1.5, 2.2, -2));
        if (t == day + 6) launchBolts(server, b);
        if (t == day + 9) boltTrails(b);
        if (t == day + 42) shot(mc, "bolts_day");
        if (t == day + 50) view(server, b.add(0, 3.5, 10.5), b.add(0, 1, 0));
        if (t == day + 54) {
            impact(b, 0, 0);
            impact(b, 6, 1);
            impact(b, 10, 2);
            impact(b, 14, 3);
        }
        if (t == day + 59) shot(mc, "impacts_day_a");
        if (t == day + 70) shot(mc, "impacts_day_b");
        if (t == day + 80) {
            signs(b);
            traps(b, 3);
        }
        if (t == day + 86) shot(mc, "signs_day");
        if (t == day + 100) daylight(server, false);
        return t >= day + 104;
    }

    private static void chains(Vec3 b) {
        for (int i = 0; i < 3; i++) {
            fire(Kind.CHAIN_ARC, i == 0 ? School.ENOCHIAN : i == 1 ? School.INFERNAL : School.WITCHCRAFT, ids[i + 1], slot(b, i).add(0, 1, 0),
                    slot(b, i + 1).add(0, 1, 0), i, MagicAssets.impactIndex("smite"), 24);
        }
    }

    private static void signs(Vec3 b) {
        fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[0], slot(b, 0).add(0, 1, 0), slot(b, 0).add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 40);
        fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[1], slot(b, 1).add(0, 1, 0), slot(b, 1).add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.2f, 40);
        fire(Kind.MARK_FLARE, School.INFERNAL, ids[2], slot(b, 2), slot(b, 2), 0.7f, MagicFxPayload.MARK_BLOODLUST, 50);
        fire(Kind.MARK_FLARE, School.INFERNAL, ids[3], slot(b, 3), slot(b, 3), 0.4f, MagicFxPayload.MARK_KILL, 30);
    }

    /** The trap flares, centred under the first {@code n} zombies: caught, straining, broken. */
    private static void traps(Vec3 b, int n) {
        for (int i = 0; i < n; i++) {
            Vec3 c = slot(b, i).add(0, 0.05, 0);
            fire(Kind.TRAP_FLARE, School.HUNTER, i == MagicFxPayload.TRAP_BROKEN ? -1 : ids[i], c, c, 1.5f, i, 30);
        }
    }

    // --- magic_rituals ----------------------------------------------------------------------------------------------------

    private static List<Short> cells() {
        List<Short> out = new ArrayList<>();
        for (PatternGeometry.Cell c : PatternGeometry.cells(GREAT)) out.add(RitualChargePayload.pack(c.dx(), c.dz()));
        return out;
    }

    private static boolean rituals(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        BlockPos g = ground;
        Vec3 top = b.add(0, 1, 0);
        // The charge, wide then close.
        if (t == 20) view(server, b.add(0, 7.5, 8.5), b.add(0, 0.5, 0));
        if (t == 40) ClientMagicFx.handleCharge(new RitualChargePayload(g, School.ENOCHIAN, cells(), 200, 0f));
        if (t == 90) shot(mc, "charge_25");
        if (t == 100) view(server, b.add(0, 3, 5.5), b.add(0, 0.4, 0));
        if (t == 112) shot(mc, "charge_close_30");
        if (t == 160) shot(mc, "charge_close_60");
        if (t == 200) view(server, b.add(0, 7.5, 8.5), b.add(0, 0.5, 0));
        if (t == 230) shot(mc, "charge_95");
        // The pillar.
        if (t == 236) view(server, b.add(0, 4, 17), b.add(0, 6, 0));
        if (t == 240) {
            ClientMagicFx.handleCharge(RitualChargePayload.stop(g));
            fire(Kind.RITUAL_COMPLETE, School.ENOCHIAN, -1, top, top, 24, 9, 40);
        }
        if (t == 243) shot(mc, "complete_a");
        if (t == 252) shot(mc, "complete_b");
        if (t == 255) view(server, b.add(0, 2.2, 7), b.add(0, 4, 0));
        if (t == 260) shot(mc, "complete_close");
        if (t == 272) shot(mc, "complete_close_b");
        // The implosion, close.
        if (t == 300) view(server, b.add(0, 6, 9), b.add(0, 0.8, 0));
        if (t == 320) ClientMagicFx.handleCharge(new RitualChargePayload(g, School.INFERNAL, cells(), 80, 0.3f));
        if (t == 370) shot(mc, "charge_infernal");
        if (t == 374) view(server, b.add(0, 2.4, 5.5), b.add(0, 1.1, 0));
        if (t == 400) {
            ClientMagicFx.handleCharge(RitualChargePayload.stop(g));
            fire(Kind.RITUAL_FAIL, School.INFERNAL, -1, top, top, 4, 1, 30);
        }
        if (t == 408) shot(mc, "fail_a");
        if (t == 420) shot(mc, "fail_b");
        if (t == 426) shot(mc, "fail_pop");
        if (t == 436) shot(mc, "fail_after");
        // The rift: crack and opening wide, open close, closing wide.
        if (t == 470) view(server, b.add(2, 3, 10), b.add(0, 2, -3));
        if (t == 480) fire(Kind.SUMMON_RIFT, School.INFERNAL, -1, b.add(0, 0, -3), b.add(0, 0, 10), 4.5f, 1.8f, 80);
        if (t == 484) shot(mc, "rift_crack");
        if (t == 494) shot(mc, "rift_opening");
        if (t == 498) view(server, b.add(1.5, 2.4, 4), b.add(0, 2.3, -3));
        if (t == 506) shot(mc, "rift_open_close");
        if (t == 520) shot(mc, "rift_open_close_b");
        if (t == 524) view(server, b.add(2, 3, 10), b.add(0, 2, -3));
        if (t == 535) shot(mc, "rift_open");
        if (t == 553) shot(mc, "rift_closing");
        if (t == 557) shot(mc, "rift_sealed");
        // The other schools' pillars side by side.
        if (t == 580) view(server, b.add(0, 5, 22), b.add(0, 6, 0));
        if (t == 590) {
            fire(Kind.RITUAL_COMPLETE, School.INFERNAL, -1, top.add(-10, 0, 0), top.add(-10, 0, 0), 20, 6, 40);
            fire(Kind.RITUAL_COMPLETE, School.WITCHCRAFT, -1, top, top, 20, 6, 40);
            fire(Kind.RITUAL_COMPLETE, School.HUNTER, -1, top.add(10, 0, 0), top.add(10, 0, 0), 20, 6, 40);
        }
        if (t == 596) shot(mc, "schools_a");
        if (t == 610) shot(mc, "schools_b");
        // By day: a pillar and a rift, close.
        if (t == 640) daylight(server, true);
        if (t == 644) view(server, b.add(0, 2.4, 8), b.add(0, 4, 0));
        if (t == 650) fire(Kind.RITUAL_COMPLETE, School.WITCHCRAFT, -1, top, top, 24, 9, 40);
        if (t == 656) shot(mc, "complete_day");
        if (t == 668) shot(mc, "complete_day_b");
        if (t == 700) view(server, b.add(1.5, 2.4, 4.5), b.add(0, 2.3, -3));
        if (t == 704) fire(Kind.SUMMON_RIFT, School.INFERNAL, -1, b.add(0, 0, -3), b.add(0, 0, 10), 4.5f, 1.8f, 80);
        if (t == 735) shot(mc, "rift_day");
        if (t == 790) daylight(server, false);
        return t >= 794;
    }

    // --- magic_bowl ------------------------------------------------------------------------------------------------------

    private static final String WORDS = "Ignis Dolor Exardescat";

    private static boolean bowl(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        BlockPos g = ground;
        Vec3 top = b.add(0, 0.6, 0);
        int seal = MagicAssets.sealIndex("curse_of_pain");
        if (t == 20) view(server, b.add(1.3, 1.6, 2.0), b.add(0, 0.7, 0));
        if (t >= 40 && t < 40 + WORDS.length() * 3 && (t - 40) % 3 == 0) {
            int i = (t - 40) / 3;
            float k = i / (float) WORDS.length();
            RecitationFx.reciting(g, k);
            if (i == 9) RecitationFx.typo(g);
            else RecitationFx.letter(g, String.valueOf(WORDS.charAt(i)), k);
        }
        if (t > 40 && t < 40 + WORDS.length() * 3) RecitationFx.reciting(g, (t - 40) / (WORDS.length() * 3f));
        if (t == 60) shot(mc, "letters_a");
        if (t == 70) shot(mc, "letters_typo");
        if (t == 100) shot(mc, "letters_boil");
        if (t == 108) {
            RecitationFx.done(g, true);
            view(server, b.add(2.4, 2.4, 3.6), b.add(0, 1.5, 0));
            fire(Kind.BOWL_SEAL, School.WITCHCRAFT, -1, top, top, seal, 0, 30);
        }
        if (t == 118) shot(mc, "seal_writing");
        if (t == 130) shot(mc, "seal_written");
        if (t == 150) shot(mc, "seal_hold");
        if (t == 162) shot(mc, "seal_burn");
        if (t == 185) view(server, b.add(7, 5, 10), b.add(6, 1.5, -3));
        if (t == 190) {
            Vec3 dest = b.add(13, 0, -7);
            fire(Kind.BOWL_SEND, School.WITCHCRAFT, ids[0], top, dest, seal, 0, 40);
        }
        if (t == 196) shot(mc, "send_lift");
        if (t == 214) shot(mc, "send_flight");
        if (t == 226) view(server, b.add(10.5, 2.4, -2.5), b.add(13, 1, -7));
        if (t == 238) shot(mc, "send_land");
        if (t == 250) shot(mc, "send_stamp");
        if (t == 270) view(server, b.add(2, 2.2, 3), b.add(0, 0.9, 0));
        if (t == 280) fire(Kind.BOWL_BACKLASH, School.INFERNAL, -1, top, top, 2.5f, seal, 24);
        if (t == 287) shot(mc, "backlash_suck");
        if (t == 295) shot(mc, "backlash_pop");
        if (t == 305) shot(mc, "backlash_ink");
        // By day: the seal and a backlash.
        if (t == 320) daylight(server, true);
        if (t == 324) view(server, b.add(2.4, 2.4, 3.6), b.add(0, 1.5, 0));
        if (t == 328) fire(Kind.BOWL_SEAL, School.WITCHCRAFT, -1, top, top, seal, 0, 30);
        if (t == 350) shot(mc, "seal_day");
        if (t == 370) fire(Kind.BOWL_BACKLASH, School.INFERNAL, -1, top, top, 2.5f, seal, 24);
        if (t == 385) shot(mc, "backlash_day");
        if (t == 400) daylight(server, false);
        return t >= 404;
    }

    // --- magic_witchcraft ------------------------------------------------------------------------------------------------

    /** A spell-less carrier in witchcraft's plum: the glyph trap and the lingering zone only need its colour and size. */
    private static SpellCarrier carrier(int color, float area) {
        return new SpellCarrier(Spell.EMPTY, 1, 1, area, color);
    }

    private static boolean witchcraft(Minecraft mc, MinecraftServer server) {
        Vec3 b = base;
        BlockPos g = ground;
        Vec3 bowlTop = Vec3.atBottomCenterOf(g.offset(-9, 0, -4)).add(0, 0.6, 0);
        Vec3 victim = b, warded = b.add(6, 0, 0), glyphAt = b.add(-6, 0, 2.5), lingerAt = b.add(6, 0, 5);
        int seal = MagicAssets.sealIndex("curse_of_pain");
        // A curse flown from the bowl lands on its victim.
        if (t == 20) view(server, b.add(-3, 4.5, 9), b.add(-4, 1.2, -2));
        if (t == 30) fire(Kind.BOWL_SEAL, School.WITCHCRAFT, -1, bowlTop, bowlTop, seal, 0, 24);
        if (t == 60) fire(Kind.BOWL_SEND, School.WITCHCRAFT, ids[0], bowlTop, victim, seal, 0, 30);
        if (t == 74) shot(mc, "send_flight");
        if (t == 80) view(server, victim.add(1.5, 2.2, 5), victim.add(0, 1, 0));
        if (t == 90) fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[0], victim.add(0, 1, 0), victim.add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 40);
        if (t == 92) fire(Kind.IMPACT, School.WITCHCRAFT, ids[0], victim.add(0, 1, 0), bowlTop, MagicAssets.impactIndex("pain"), 0, 30);
        if (t == 94) shot(mc, "curse_land");
        if (t == 106) shot(mc, "curse_hold");
        // A Ward of the Name turns one away.
        if (t == 120) view(server, warded.add(-1, 2.2, 5), warded.add(0, 1, 0));
        if (t == 126) fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[1], warded.add(0, 1, 0), warded.add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.5f, 40);
        if (t == 129) shot(mc, "ward_turns");
        if (t == 140) shot(mc, "ward_flicker");
        // The player bearing a Mirror Hex and a Ward of the Name, seen from behind.
        if (t == 160) {
            server.execute(() -> {
                ServerPlayer p = player(server);
                p.addEffect(new MobEffectInstance(AllMobEffects.MIRRORED, 400, 0, false, false, true));
                p.addEffect(new MobEffectInstance(AllMobEffects.NAME_WARDED, 400, 0, false, false, true));
            });
            view(server, b.add(0, 1.2, 6), b.add(0, 1.2, 0));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (t == 172) shot(mc, "mirrored_a");
        if (t == 184) shot(mc, "mirrored_b");
        if (t == 186) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server.execute(() -> player(server).removeAllEffects());
        }
        // A glyph trap: written, armed, lying in wait, sprung.
        if (t == 196) view(server, glyphAt.add(0, 2.6, 4.5), glyphAt.add(0, 0.1, 0));
        if (t == 200) {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                WardEntity glyph = WardEntity.glyph(level, player(server), carrier(0xB23A8E, 0), 600);
                glyph.setSchool(School.WITCHCRAFT);
                glyph.moveTo(glyphAt.x, glyphAt.y, glyphAt.z);
                level.addFreshEntity(glyph);
            });
        }
        if (t == 210) shot(mc, "glyph_writing");
        if (t == 224) shot(mc, "glyph_armed");
        if (t == 240) shot(mc, "glyph_idle");
        if (t == 252) shot(mc, "glyph_glint");
        if (t == 260) {
            server.execute(() -> {
                if (server.overworld().getEntity(ids[2]) instanceof Zombie z) z.teleportTo(glyphAt.x, glyphAt.y, glyphAt.z);
            });
        }
        if (t == 266) fire(Kind.IMPACT, School.WITCHCRAFT, ids[2], glyphAt.add(0, 1, 0), glyphAt.add(0, 1, 4), MagicAssets.impactIndex("hex"),
                0, 34);
        if (t == 270) shot(mc, "glyph_sprung");
        if (t == 282) shot(mc, "glyph_sprung_b");
        // A lingering zone.
        if (t == 290) {
            view(server, lingerAt.add(-1, 3.2, 5.5), lingerAt.add(0, 0.3, 0));
            server.execute(() -> {
                ServerLevel level = server.overworld();
                WardEntity zone = WardEntity.lingering(level, player(server), carrier(0xB23A8E, 2.5f), 300);
                zone.setSchool(School.WITCHCRAFT);
                zone.moveTo(lingerAt.x, lingerAt.y, lingerAt.z);
                level.addFreshEntity(zone);
            });
        }
        if (t == 320) shot(mc, "linger");
        // By day: the curse landing, the glyph and the zone.
        if (t == 340) daylight(server, true);
        if (t == 344) view(server, b.add(0, 3.6, 9), b.add(0, 0.8, 1.5));
        if (t == 350) fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[0], victim.add(0, 1, 0), victim.add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 40);
        if (t == 352) fire(Kind.HEX_PULSE, School.WITCHCRAFT, ids[1], warded.add(0, 1, 0), warded.add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.5f, 40);
        if (t == 356) shot(mc, "day");
        if (t == 366) shot(mc, "day_b");
        if (t == 380) daylight(server, false);
        return t >= 384;
    }
}
