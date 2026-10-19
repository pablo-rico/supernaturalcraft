package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceDialogueScreen;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.witchboss.ClientGrandWitchFx;
import org.papiricoh.supernaturalcraft.client.witchboss.ClientRowenaFx;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchDialogue;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchNpcEntity;
import org.papiricoh.supernaturalcraft.coven.ending.Unmarking;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.CovenStaffItem;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchBalance;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.RowenaAllyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RecitationBowlEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaBalance;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.network.AllegianceDialoguePayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The witch bosses (v0.21, P6) in the real renderer, at a plain 70° with no FOV effects. Screenshots {@code sn_<scene>_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=rowena_fight}: her house placed ({@code /supernatural coven rowena place}) and a real fight in its front
 *   garden, the façade and the porch behind her ({@code /supernatural boss summon rowena} at the arena's centre; the first spells
 *   by day, the rest at night): the intro, each attack forced in turn (a shot of its telegraph,
 *   close shots of its effect and one wide), the familiar and the curse down its lock, her victims rising, one killed (she heals),
 *   one freed (it hurts her), the recitation spoken (the house turning red, the hex) and broken (the bowls smashed one by one, her
 *   fall and stun), the phase changes and her death.</li>
 *   <li>{@code SN_PREVIEW=grand_witch_fight}: the Grand Coven called on open ground ({@code /supernatural boss summon grand_witch}):
 *   the channel and the shield (by day and by night), one channeller interrupted, the shield broken, frost, inferno, crush, Rowena
 *   arriving, the mass taglock with her green circles, the smoke veil, her death.</li>
 *   <li>{@code SN_PREVIEW=witch_ending}: the choice with the Book (Rowena's panel), the Grand Witch's panel, the Mark taken off by
 *   Rowena (her seal from her bowl) and by the Grand Witch (from her hands), in first and third person, by day and by night, and the
 *   Coven Staff in hand and casting.</li>
 * </ul>
 */
final class WitchBossPreview {

    static final List<String> NAMES = List.of("rowena_fight", "grand_witch_fight", "witch_ending");
    private static final int FOV = 70;
    private static final int X = 7200, Z = 7200;

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
    }

    private static List<String> queue;
    private static String scene;
    private static int t;
    private static volatile Vec3 base;
    private static List<Beat> beats;
    private static int beatIndex, beatStart;
    private static final List<Entity> staged = new ArrayList<>();
    private static volatile Vec3 camFrom, camAt;
    private static int savedFov = -1;
    private static double savedFovEffect;

    private WitchBossPreview() {
    }

    static boolean tick(Minecraft mc) {
        String env = DevPreview.env();
        if (env == null) return false;
        if (queue == null) {
            List<String> want = new ArrayList<>();
            for (String s : env.split(",")) want.add(s.trim());
            if (want.isEmpty() || !NAMES.containsAll(want)) return false;
            queue = want;
            savedFov = mc.options.fov().get();
            savedFovEffect = mc.options.fovEffectScale().get();
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
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
            beats = null;
            beatIndex = 0;
            beatStart = 0;
            base = null;
            camFrom = null;
            Fight.reset();
            MagicFxManager.clear();
            ClientRowenaFx.clear();
            ClientGrandWitchFx.clear();
            mc.options.fov().set(FOV);
            mc.options.fovEffectScale().set(0.0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
        }
        t++;
        if (t == 0) {
            String which = scene;
            server.execute(() -> stage(server, which));
            return true;
        }
        if (base == null || t < 20) return true;
        Vec3 f = camFrom, a = camAt;
        if (f != null && t % 5 == 0) {
            server.execute(() -> {
                ServerPlayer p = player(server);
                if (p.position().distanceToSqr(f) > 0.09) look(p, f, a);
                p.setDeltaMovement(Vec3.ZERO);
            });
        }
        boolean done = scene.equals("witch_ending") ? beats(mc, server) : Fight.tick(mc, server);
        if (done) {
            server.execute(() -> unstage(server));
            mc.setScreen(null);
            scene = null;
        }
        return true;
    }

    // --- staging -------------------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void stage(MinecraftServer server, String which) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        p.setGameMode(GameType.SPECTATOR);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.removeAllEffects();
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.setDayTime(which.equals("rowena_fight") ? 18000 : 6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 160;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60),
                e -> e instanceof LuciferEntity || e instanceof RowenaNpcEntity || e instanceof GrandWitchNpcEntity || e instanceof RowenaAllyEntity
                        || e instanceof CovenWitchEntity || e instanceof FamiliarEntity || e instanceof HexedVillagerEntity
                        || e instanceof RecitationBowlEntity || e instanceof ArmorStand || e instanceof TelegraphMarker)) {
            old.discard();
        }
        Vec3 b = Vec3.atBottomCenterOf(g);
        if (!which.equals("rowena_fight")) floor(level, g, 22, 22);
        p.teleportTo(level, b.x, b.y + 3, b.z + 8, 180, 15);
        base = b;
    }

    private static void unstage(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel level = server.overworld();
        staged.forEach(Entity::discard);
        staged.clear();
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        if (Marks.marked(p)) Marks.remove(p);
        p.getInventory().clearContent();
        p.setGameMode(GameType.SPECTATOR);
    }

    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : h == 1 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 12; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static <T extends Mob> T mob(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        staged.add(e);
        return e;
    }

    /** Puts the camera's eye at {@code eye} (the player's feet 1.62 below), looking at {@code at}. */
    private static void view(ServerPlayer p, Vec3 eye, Vec3 at) {
        Vec3 from = eye.subtract(0, 1.62, 0);
        camFrom = from;
        camAt = at;
        look(p, from, at);
    }

    private static void look(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    private static void daylight(MinecraftServer server, boolean day) {
        long time = day ? 6000 : 18000;
        server.execute(() -> server.overworld().setDayTime(time));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.setDayTime(time);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    /**
     * Where to put the camera {@code dist} out from {@code from} along {@code dir} and {@code height} up, without passing a wall
     * (her house): stopped half a block short of whatever is in the way.
     */
    private static Vec3 clear(ServerLevel level, Vec3 from, Vec3 want, Entity by) {
        var hit = level.clip(new ClipContext(from, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        if (hit.getType() != HitResult.Type.BLOCK) return want;
        Vec3 d = want.subtract(from);
        double len = d.length(), free = Math.max(0.6, from.distanceTo(hit.getLocation()) - 0.5);
        return from.add(d.scale(free / Math.max(0.01, len)));
    }

    // --- the fights --------------------------------------------------------------------------------------------------------------

    static final class Fight {

        /**
         * One moment to force: its phase and name; what the server does to start it (queue an attack, or an action); for an attack,
         * its windup (the telegraph's shot at 70 % of it; -1 for an action); the camera (distance, height, look bias); the shots
         * after it lands (or after the action) — the second also taken wide; at night or by day; and something done every four
         * ticks for a while from {@code duringAt} ticks in (the bowls struck).
         */
        record Step(int phase, String name, BiConsumer<ServerPlayer, LuciferEntity> start, int windup, double dist, double height,
                    double lookBias, int[] after, boolean night, int duringAt, BiConsumer<ServerPlayer, LuciferEntity> during) {
            Step(int phase, String name, Supplier<BossAttack<LuciferEntity>> attack, int windup, double dist, double height, double lookBias,
                 int[] after, boolean night) {
                this(phase, name, (p, b) -> queue(b, attack), windup, dist, height, lookBias, after, night, -1, null);
            }
        }

        private static void queue(LuciferEntity b, Supplier<BossAttack<LuciferEntity>> attack) {
            if (b instanceof RowenaEntity r) r.queue(attack);
            else if (b instanceof GrandWitchEntity w) w.queue(attack);
        }

        private static List<Step> rowenaSteps() {
            List<Step> s = new ArrayList<>();
            s.add(new Step(1, "fling", RowenaAttacks.Fling::new, RowenaBalance.FLING_WINDUP, 6, 2.2, 0.45, new int[]{1, 6, 12}, false));
            s.add(new Step(1, "ignis", RowenaAttacks.Ignis::new, RowenaBalance.IGNIS_WINDUP, 6, 3, 0.85, new int[]{2, 10, 24}, false));
            s.add(new Step(1, "glacies", RowenaAttacks.Glacies::new, RowenaBalance.GLACIES_WINDUP, 6, 3, 0.85, new int[]{3, 12, 40}, true));
            s.add(new Step(1, "smoke_blink", RowenaAttacks.SmokeBlink::new, RowenaBalance.SMOKE_WINDUP, 6, 2.5, 0.7, new int[]{1, 8}, true));
            s.add(new Step(2, "familiar", RowenaAttacks.CallFamiliar::new, RowenaBalance.FAMILIAR_WINDUP, 5, 2.4, 0.4, new int[]{2, 10}, true));
            s.add(new Step(2, "homing", (p, b) -> {
                if (b instanceof RowenaEntity r) r.sendCurse(p.serverLevel(), p);
            }, -1, 6, 3, 0.5, new int[]{4, 12, 22}, true, -1, null));
            // From here on her familiar is sent away (its stolen lock would curse and watch the camera through every later shot).
            s.add(new Step(2, "victims", (p, b) -> {
                familiarsGone = true;
                queue(b, RowenaAttacks.SummonVictims::new);
            }, RowenaBalance.VICTIMS_WINDUP, 6, 2.8, 0.6, new int[]{3, 10, 20}, true, -1, null));
            s.add(new Step(2, "victim_killed", (p, b) -> {
                if (b instanceof RowenaEntity r && !r.victims().isEmpty()) {
                    HexedVillagerEntity v = r.victims().getFirst();
                    v.invulnerableTime = 0;
                    v.hurt(AllDamageTypes.source(p.serverLevel(), AllDamageTypes.SMITE, p), 500f);
                }
            }, -1, 6, 2.8, 0.5, new int[]{3, 10, 18}, true, -1, null));
            s.add(new Step(2, "victim_freed", (p, b) -> {
                if (b instanceof RowenaEntity r && !r.victims().isEmpty()) r.victims().getFirst().free(p);
            }, -1, 6, 2.8, 0.5, new int[]{6, 20, 26}, true, -1, null));
            // The recitation spoken: shots through it, the red at its end, the hex.
            s.add(new Step(3, "recitation_spoken", (p, b) -> queue(b, RowenaAttacks.Recitation::new), 20, 8, 4, 0.6,
                    new int[]{30, 150, 250, 270, 300, 306}, true, -1, null));
            // Broken: the bowls smashed one by one (a blow every four ticks from 30 in), her fall and her stun.
            s.add(new Step(3, "recitation_broken", (p, b) -> queue(b, RowenaAttacks.Recitation::new), 20, 8, 3.5, 0.6,
                    new int[]{40, 56, 74, 84, 110}, true, 30, (p, b) -> {
                if (b instanceof RowenaEntity r && !r.bowls().isEmpty()) r.bowls().getFirst().strike();
            }));
            return s;
        }

        private static List<Step> witchSteps() {
            List<Step> s = new ArrayList<>();
            s.add(new Step(1, "shield_day", (p, b) -> {
            }, -1, 9, 3.5, 0.6, new int[]{4, 20}, false, -1, null));
            s.add(new Step(1, "shield_night", (p, b) -> {
            }, -1, 6, 2.2, 0.8, new int[]{6, 20}, true, -1, null));
            s.add(new Step(1, "interrupt", (p, b) -> {
                if (b instanceof GrandWitchEntity w && !w.channelers().isEmpty()) w.interrupt(w.channelers().getFirst());
            }, -1, 8, 3.0, 0.5, new int[]{1, 6, 14}, false, -1, null));
            s.add(new Step(1, "shield_broken", (p, b) -> {
                if (b instanceof GrandWitchEntity w) for (CovenWitchEntity c : w.channelers()) if (c.channeling()) w.interrupt(c);
            }, -1, 6, 2.6, 0.8, new int[]{1, 6, 16}, false, -1, null));
            s.add(new Step(2, "frost_hex", GrandWitchAttacks.FrostHex::new, GrandWitchBalance.FROST_WINDUP, 7, 4, 0.2, new int[]{2, 10, 36}, false));
            s.add(new Step(2, "inferno", GrandWitchAttacks.Inferno::new, GrandWitchBalance.INFERNO_WINDUP, 11, 5, 0.9, new int[]{2, 10, 24}, true));
            s.add(new Step(2, "crush", GrandWitchAttacks.Crush::new, GrandWitchBalance.CRUSH_WINDUP, 6, 3, 0.15, new int[]{1, 8, 22}, false));
            s.add(new Step(3, "mass_taglock", GrandWitchAttacks.MassTaglock::new, GrandWitchBalance.MASS_FOLLOW + GrandWitchBalance.MASS_HOLD, 10, 6,
                    0.4, new int[]{1, 8, 16}, true));
            s.add(new Step(3, "smoke_veil", GrandWitchAttacks.SmokeVeil::new, GrandWitchBalance.VEIL_WINDUP, 8, 3, 0.4, new int[]{1, 8}, false));
            return s;
        }

        private static List<Step> steps;
        private static volatile int bossId = -1;
        private static int step, queuedAt = -1, windupAt = -1, activeAt = -1, transitionAt = -1, deathAt = -1, summonedAt = -1, idleSince = -1;
        private static boolean rowena, familiarsGone;

        static void reset() {
            bossId = -1;
            step = 0;
            steps = null;
            queuedAt = windupAt = activeAt = transitionAt = deathAt = summonedAt = idleSince = -1;
            familiarsGone = false;
        }

        static boolean tick(Minecraft mc, MinecraftServer server) {
            Vec3 c = base;
            if (t == 20) {
                rowena = scene.equals("rowena_fight");
                steps = rowena ? rowenaSteps() : witchSteps();
                mc.options.hideGui = false;
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    ServerLevel level = p.serverLevel();
                    Vec3 centre = c;
                    if (rowena) {
                        // Her house, built where we stand (its door to the south); the fight in its front garden.
                        p.teleportTo(level, c.x, c.y + 1, c.z, 180, 0);
                        command(p, "supernatural coven rowena place");
                        BlockPos centrePos = RowenaSummoning.centre(level);
                        centre = centrePos != null ? Vec3.atBottomCenterOf(centrePos)
                                : Vec3.atBottomCenterOf(org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouse.originOn(level, (int) c.x, (int) c.z)).add(0, 0, org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout.ARENA_CENTRE.z());
                    }
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().invulnerable = true;
                    p.getAbilities().mayfly = true;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    camFrom = null;
                    p.teleportTo(level, centre.x, centre.y, centre.z + 3, 180, 0);
                    base = centre;
                });
            }
            if (t == 70) {
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    Vec3 b = base;
                    command(p, "supernatural boss summon " + (rowena ? "rowena" : "grand_witch") + " " + (int) Math.floor(b.x) + " "
                            + (int) Math.floor(b.y) + " " + (int) Math.floor(b.z));
                    LuciferEntity boss = p.serverLevel().getEntitiesOfClass(LuciferEntity.class, p.getBoundingBox().inflate(64),
                            e -> rowena ? e instanceof RowenaEntity : e instanceof GrandWitchEntity).stream().findFirst().orElse(null);
                    bossId = boss == null ? -1 : boss.getId();
                });
                summonedAt = t;
            }
            if (summonedAt < 0) return false;
            int since = t - summonedAt;
            if (since == 20 || since == 50 || since == 85) shot(mc, "intro_" + since);
            if (since > 400 && bossId < 0) return true;
            LuciferEntity boss = mc.level == null || bossId < 0 ? null : mc.level.getEntity(bossId) instanceof LuciferEntity e ? e : null;
            if (t % 4 == 0) server.execute(() -> {
                ServerPlayer p = player(server);
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.removeAllEffects();
                // Thawed and put out: the frost hex's freezing and the fire would cover the next shots.
                p.setTicksFrozen(0);
                p.clearFire();
                if (familiarsGone) {
                    for (FamiliarEntity f : p.serverLevel().getEntitiesOfClass(FamiliarEntity.class, p.getBoundingBox().inflate(64))) f.discard();
                }
            });
            if (deathAt >= 0) return death(mc, server);
            if (boss == null || since < 110) return false;
            byte state = boss.state();
            if (transitionAt >= 0) {
                int k = t - transitionAt;
                if (k == 14 || k == 42 || k == 70) shot(mc, "phase" + boss.lookPhase() + "_transition_" + k);
                if (state == LuciferEntity.TRANSITION || k < 30) return false;
                transitionAt = -1;
                return false;
            }
            if (step >= steps.size()) {
                if (state == LuciferEntity.IDLE) {
                    deathAt = t;
                    int id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) {
                            b.setAbsorptionAmount(0);
                            BossHealthGuard.set(b, 1f);
                            b.invulnerableTime = 0;
                            b.hurt(AllDamageTypes.source(p.serverLevel(), AllDamageTypes.SMITE, p), 50f);
                            placeCamera(p, b, rowena ? 6 : 7, rowena ? 2.5 : 3, 0.6);
                        }
                    });
                }
                return false;
            }
            Step s = steps.get(step);
            String name = "phase" + s.phase + "_" + s.name;
            if (queuedAt < 0) {
                if (state != LuciferEntity.IDLE) {
                    idleSince = -1;
                    return false;
                }
                if (idleSince < 0) idleSince = t;
                if (t - idleSince < 6) return false;
                if (boss.lookPhase() < s.phase) {
                    transitionAt = t;
                    int to = s.phase, id = bossId, phases = boss.maxPhase();
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) {
                            BossHealthGuard.set(b, b.getMaxHealth() * (phases - to + 1) / phases + 1);
                            b.beginTransition(to);
                            placeCamera(p, b, rowena ? 6 : 8, rowena ? 2.6 : 2.6, 0.9);
                        }
                    });
                    return false;
                }
                queuedAt = t;
                windupAt = activeAt = -1;
                int id = bossId;
                daylight(server, !s.night);
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) {
                        placeCamera(p, b, s.dist, s.height, s.lookBias);
                        s.start.accept(p, b);
                    }
                });
                if (s.windup < 0) activeAt = t + 2;
                return false;
            }
            if (s.windup >= 0) {
                if (windupAt < 0 && state == LuciferEntity.WINDUP) windupAt = t;
                if (windupAt >= 0 && activeAt < 0 && t == windupAt + Math.max(4, (int) (s.windup * 0.7))) shot(mc, name + "_telegraph");
                if (activeAt < 0 && state == LuciferEntity.ACTIVE) activeAt = t;
            }
            if (activeAt >= 0) {
                int k = t - activeAt;
                if (s.duringAt >= 0 && k >= s.duringAt && k <= s.duringAt + 48 && (k - s.duringAt) % 4 == 0 && s.during != null) {
                    int id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) s.during.accept(p, b);
                    });
                }
                for (int i = 0; i < s.after.length; i++) {
                    int a = s.after[i];
                    if (k == a) shot(mc, name + "_fx_" + a);
                    // The second moment also seen wide (the camera stepped back, then in again).
                    if (i == Math.min(1, s.after.length - 1)) {
                        int id = bossId;
                        if (k == a + 1) server.execute(() -> {
                            ServerPlayer p = player(server);
                            if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) placeWide(p, b, rowena ? 10 : Math.max(12, s.dist * 1.4));
                        });
                        if (k == a + 5) shot(mc, name + "_fx_" + a + "_wide");
                        if (k == a + 6) server.execute(() -> {
                            ServerPlayer p = player(server);
                            if (p.serverLevel().getEntity(id) instanceof LuciferEntity b) placeCamera(p, b, s.dist, s.height, s.lookBias);
                        });
                    }
                }
            }
            int last = activeAt < 0 ? 0 : activeAt + Math.max(s.after[s.after.length - 1], s.after[Math.min(1, s.after.length - 1)] + 6);
            boolean finished = activeAt >= 0 && t > last && (state == LuciferEntity.IDLE || state == LuciferEntity.RECOVER && t > last + 30
                    || s.windup < 0 && t > last + 4);
            if (finished || t - queuedAt > 520) {
                if (activeAt < 0) org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("[preview] {} never used {}", scene, s.name);
                step++;
                queuedAt = -1;
                idleSince = -1;
            }
            return false;
        }

        private static boolean death(Minecraft mc, MinecraftServer server) {
            int k = t - deathAt;
            if (k == 16 || k == 40 || k == 70 || k == 110 || k == 150) shot(mc, "death_" + k);
            if (k == 200) shot(mc, "victory");
            return k >= 230;
        }

        /** The wide camera: {@code dist} blocks south of the boss (kept inside the arena), looking at her from 25° above. */
        private static void placeWide(ServerPlayer p, LuciferEntity b, double dist) {
            ServerLevel level = p.serverLevel();
            Vec3 bp = b.position();
            double ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(bp)).getY();
            Vec3 at = new Vec3(bp.x, Math.min(bp.y, ground) + 1.2, bp.z);
            Vec3 want = at.add(0, dist * Math.tan(Math.toRadians(25)), dist);
            Vec3 c = base;
            double limit = (rowena ? org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout.ARENA_RADIUS
                    : org.papiricoh.supernaturalcraft.SNConfig.GRAND_WITCH_ARENA_RADIUS.get()) - 2.0;
            if (c != null) {
                Vec3 off = want.subtract(c).multiply(1, 0, 1);
                if (off.length() > limit) want = new Vec3(c.x + off.x / off.length() * limit, want.y, c.z + off.z / off.length() * limit);
            }
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            view(p, clear(level, at, want, p), at);
        }

        /**
         * The camera: the hunter hovering {@code dist} out from the boss (on the side they stand now, kept inside her walls) and
         * {@code height} up, looking at the point {@code lookBias} of the way from the hunter's feet to her.
         */
        private static void placeCamera(ServerPlayer p, LuciferEntity b, double dist, double height, double lookBias) {
            ServerLevel level = p.serverLevel();
            Vec3 bp = b.position();
            Vec3 dir = p.position().subtract(bp).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 0.5 ? new Vec3(0, 0, 1) : dir.normalize();
            Vec3 from = bp.add(0, 1.2, 0);
            Vec3 want = bp.add(dir.scale(dist)).add(0, height, 0);
            // Never at the arena's wall (it fills the view, and the Cage pushes back): inside it by three blocks.
            Vec3 c = base;
            double limit = (rowena ? org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout.ARENA_RADIUS
                    : org.papiricoh.supernaturalcraft.SNConfig.GRAND_WITCH_ARENA_RADIUS.get()) - 3.0;
            if (c != null) {
                Vec3 off = want.subtract(c).multiply(1, 0, 1);
                if (off.length() > limit) want = new Vec3(c.x + off.x / off.length() * limit, want.y, c.z + off.z / off.length() * limit);
            }
            Vec3 eye = clear(level, from, want, p);
            Vec3 floor = new Vec3(eye.x, bp.y, eye.z);
            Vec3 at = floor.lerp(bp, lookBias).add(0, 0.6, 0);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            view(p, eye, at);
        }
    }

    // --- SN_PREVIEW=witch_ending ---------------------------------------------------------------------------------------------

    private static boolean beats(Minecraft mc, MinecraftServer server) {
        if (beats == null) beats = ending(mc);
        if (beatIndex >= beats.size()) return true;
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
        if (local == 2 && b.client != null) b.client.run();
        if (b.name != null && local == b.length - 2) shot(mc, b.name);
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return false;
    }

    private static RowenaNpcEntity rowenaNpc;
    private static GrandWitchNpcEntity witchNpc;
    private static ArmorStand dummy;

    private static List<Beat> ending(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        // Rowena at home, the Grand Witch beside her, the hunter before them.
        out.add(new Beat(null, (p, c) -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            rowenaNpc = mob(level, AllEntities.ROWENA_NPC.get(), c.add(-1.5, 0, 0), 0);
            witchNpc = mob(level, AllEntities.GRAND_WITCH_NPC.get(), c.add(2.5, 0, 0), 0);
            level.setBlockAndUpdate(BlockPos.containing(c.add(-3, 0, 0.5)), org.papiricoh.supernaturalcraft.registry.AllBlocks.SPELL_BOWL.get().defaultBlockState());
            view(p, c.add(0, 1.62, 4), c.add(0, 1.4, 0));
        }, null, 30));
        // The choice with the Book.
        out.add(new Beat("book_choice", null, () -> openDialogue(mc, rowenaNpc, RowenaDialogue.ID, RowenaDialogue.BOOK_ASK,
                List.of(RowenaDialogue.OPT_GIVE, RowenaDialogue.OPT_REFUSE)), 40));
        out.add(new Beat("grand_witch_panel", null, () -> openDialogue(mc, witchNpc, GrandWitchDialogue.ID, GrandWitchDialogue.HELLO,
                List.of(GrandWitchDialogue.OPT_UNMARK, GrandWitchDialogue.LEAVE)), 40));
        out.add(new Beat(null, null, () -> mc.setScreen(null), 6));
        for (boolean day : new boolean[]{true, false}) {
            String when = day ? "day" : "night";
            for (boolean byRowena : new boolean[]{true, false}) {
                String who = byRowena ? "rowena" : "grand_witch";
                for (boolean first : new boolean[]{true, false}) {
                    String view = first ? "first" : "third";
                    out.add(new Beat(null, (p, c) -> {
                        daylight(p.server, day);
                        Marks.grant(p, null, false);
                        if (first) view(p, c.add(0, 1.62, 3), c.add(0, 1.2, 0));
                        else view(p, c.add(-0.5, 1.9, 5.8), c.add(0, 1.2, 3));
                    }, () -> mc.options.setCameraType(first ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_FRONT), 30));
                    out.add(new Beat(null, (p, c) -> {
                        LivingEntity survivor = byRowena ? rowenaNpc : witchNpc;
                        Vec3 from = byRowena ? c.add(-3, 0.6, 0.5) : witchNpc.position().add(0, 1.6, 0);
                        Unmarking.perform(p, survivor, from, false);
                    }, null, 1));
                    int[] at = {18, 34, 50, 70};
                    for (int i = 0; i < at.length; i++) {
                        int gap = i == 0 ? at[0] : at[i] - at[i - 1];
                        out.add(new Beat("unmark_" + who + "_" + view + "_" + when + "_" + at[i], null, null, gap));
                    }
                    out.add(new Beat(null, null, () -> mc.options.setCameraType(CameraType.FIRST_PERSON), 10));
                }
            }
        }
        // The Coven Staff, in hand and casting.
        out.add(new Beat(null, (p, c) -> {
            daylight(p.server, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.COVEN_STAFF.get()));
            dummy = EntityType.ARMOR_STAND.create(p.serverLevel());
            dummy.moveTo(c.x, c.y, c.z - 4, 0, 0);
            p.serverLevel().addFreshEntity(dummy);
            staged.add(dummy);
            view(p, c.add(0, 1.62, 2), c.add(0, 1.2, -4));
        }, null, 60));
        out.add(new Beat("staff_first_person", null, null, 4));
        out.add(new Beat(null, (p, c) -> CovenStaffItem.hex(p, p.getMainHandItem(), dummy), null, 6));
        out.add(new Beat("staff_cast", null, null, 4));
        out.add(new Beat(null, (p, c) -> view(p, c.add(2.2, 2.0, 4.5), c.add(0, 1.2, 2)), () -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT), 30));
        out.add(new Beat("staff_third_person", null, null, 4));
        out.add(new Beat(null, null, () -> mc.options.setCameraType(CameraType.FIRST_PERSON), 4));
        return out;
    }

    private static void openDialogue(Minecraft mc, Entity speaker, String dialogue, String node, List<String> options) {
        if (speaker == null || mc.level == null) return;
        // The client's copy of the staged speaker (the same id).
        AllegianceDialogueScreen s = new AllegianceDialogueScreen(new AllegianceDialoguePayload(speaker.getId(), dialogue, node, options, 0));
        mc.setScreen(s);
        s.finishTyping();
    }
}
