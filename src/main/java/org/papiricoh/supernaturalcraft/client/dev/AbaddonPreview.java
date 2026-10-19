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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.abaddon.AbaddonHud;
import org.papiricoh.supernaturalcraft.client.abaddon.ClientAbaddonFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonHeraldEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonKnightEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.KnightsCleaverItem;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Abaddon, Knight of Hell (v0.21), in the real renderer, well away from the origin, at a plain 70° with no FOV effects. Screenshots:
 * {@code sn_abaddon_*.png}, {@code sn_abaddon_fight_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=abaddon}: her, a knight, a possessed knight and the herald side by side by day and by night; close-ups
 *   (her face, the possessed knight's blazing visor, smoking joints and burning blade, the herald's bell); the Knights' Cleaver
 *   in first and third person; every clip of hers and her retinue's at its blow; then each effect staged close on a plain floor
 *   (the herald's mark and its strike, the sweep, a knight rising, the smoke out of her and into a knight, each way of casting
 *   her out with the stun after, the pin, the Brand, the trap holding her and breaking, the cleaver's wave, her death with her
 *   knights crumbling and the Book left glowing) and the four title cards.</li>
 *   <li>{@code SN_PREVIEW=abaddon_fight}: a real fight called by {@code /supernatural boss summon abaddon} in her hall: the intro,
 *   each attack forced in turn (a shot of the telegraph, then of the effect, close), the phase changes (camera and title card),
 *   in Possession each way of casting her out (exorcism, Banish by Name, salt, a devil's trap, the knight killed) with her bar's
 *   states, a devil's trap painted under her and her breaking it, then her death and the Book. The first phase by day, the
 *   rest by night.</li>
 * </ul>
 */
final class AbaddonPreview {

    static final List<String> NAMES = List.of("abaddon", "abaddon_fight");
    private static final int FOV = 70;
    private static final int X = 7200, Z = 6400;

    /** One moment: what the server sets up, what the client does, the picture's name (taken at its end), how long it lasts. */
    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length, boolean shot) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
            this(name, server, client, length, true);
        }
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

    private AbaddonPreview() {
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
            ClientAbaddonFx.clear();
            AbaddonHud.force(null);
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
        boolean done = scene.equals("abaddon_fight") ? Fight.tick(mc, server) : beats(mc, server);
        if (done) {
            server.execute(() -> unstage(server));
            AbaddonHud.force(null);
            mc.setScreen(null);
            scene = null;
        }
        return true;
    }

    private static boolean beats(Minecraft mc, MinecraftServer server) {
        if (beats == null) beats = model(mc);
        if (beatIndex >= beats.size()) return true;
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = base;
        if (local == 0 && b.server != null) server.execute(() -> b.server.accept(player(server), c));
        if (local == 2 && b.client != null) b.client.run();
        if (b.shot && local == b.length - 2) shot(mc, b.name);
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return false;
    }

    // --- staging ---------------------------------------------------------------------------------------------------------------

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
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(12000, 0, false, false);
        int x = X + NAMES.indexOf(which) * 160;
        level.getChunk(x >> 4, Z >> 4);
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, Z));
        for (Entity old : level.getEntitiesOfClass(Entity.class, new AABB(g).inflate(60),
                e -> e instanceof LuciferEntity || e instanceof AbaddonKnightEntity || e instanceof AbaddonHeraldEntity || e instanceof ArmorStand
                        || e instanceof TelegraphMarker || e instanceof ItemEntity)) {
            old.discard();
        }
        Vec3 b = Vec3.atBottomCenterOf(g);
        if (which.equals("abaddon")) floor(level, g, 20, 20);
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
        p.getInventory().clearContent();
        p.setGameMode(GameType.SPECTATOR);
    }

    /** A plain floor of her hall's stone. */
    private static void floor(ServerLevel level, BlockPos g, int rx, int rz) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                int h = Math.floorMod(dx * 7 + dz * 13, 11);
                level.setBlockAndUpdate(g.offset(dx, -1, dz), h == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : h == 1 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                for (int dy = 0; dy < 10; dy++) level.setBlockAndUpdate(g.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
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

    private static AbaddonEntity abaddon(ServerLevel level, Vec3 at, float yaw) {
        return mob(level, AllEntities.ABADDON.get(), at, yaw);
    }

    private static AbaddonKnightEntity knight(ServerLevel level, Vec3 at, float yaw) {
        return mob(level, AllEntities.ABADDON_KNIGHT.get(), at, yaw);
    }

    private static AbaddonHeraldEntity herald(ServerLevel level, Vec3 at, float yaw) {
        return mob(level, AllEntities.ABADDON_HERALD.get(), at, yaw);
    }

    private static ArmorStand stand(ServerLevel level, Vec3 at, float yaw) {
        ArmorStand s = EntityType.ARMOR_STAND.create(level);
        s.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(s);
        staged.add(s);
        return s;
    }

    private static void clearStaged() {
        staged.forEach(Entity::discard);
        staged.clear();
        Minecraft.getInstance().execute(() -> {
            MagicFxManager.clear();
            ClientAbaddonFx.clear();
        });
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

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static void fx(ServerPlayer p, AbaddonFxPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    private static Beat still(String name, BiConsumer<ServerPlayer, Vec3> server) {
        return new Beat(name, server, null, 30);
    }

    private static Beat wait(String name, int length) {
        return new Beat(name, (p, c) -> {
        }, null, length);
    }

    /** A 3×3 devil's trap painted with its centre at {@code centre}. */
    static void paintTrap(ServerLevel level, BlockPos centre) {
        for (int part = 0; part < 9; part++) {
            level.setBlockAndUpdate(centre.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part)),
                    AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part));
        }
    }

    static void clearTrap(ServerLevel level, BlockPos centre) {
        for (int part = 0; part < 9; part++) {
            BlockPos p = centre.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part));
            if (level.getBlockState(p).getBlock() instanceof DevilsTrapBlock) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
    }

    // --- SN_PREVIEW=abaddon --------------------------------------------------------------------------------------------------

    private static List<Beat> model(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.2, 0);
        // Her, a knight, a possessed knight, the herald, facing us (south).
        out.add(new Beat("lineup_day", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            AbaddonEntity a = abaddon(level, c.add(-3.6, 0, 0), 0);
            knight(level, c.add(-1.2, 0, 0), 0);
            knight(level, c.add(1.4, 0, 0), 0).setPossessor(a.getUUID());
            herald(level, c.add(3.8, 0, 0), 0);
            view(p, c.add(0, 1.8, 9.5), c.add(0, 1.1, 0));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 50));
        out.add(still("lineup_night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(new Beat("abaddon_front", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            abaddon(p.serverLevel(), c, 0);
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }, null, 30));
        out.add(still("abaddon_side", (p, c) -> view(p, c.add(2.6, 1.6, 0.2), c.add(eye))));
        out.add(still("abaddon_back", (p, c) -> view(p, c.add(-0.3, 1.6, -2.6), c.add(eye))));
        out.add(still("abaddon_face", (p, c) -> view(p, c.add(0.1, 1.66, 1.0), c.add(0, 1.64, 0))));
        out.add(still("abaddon_face_night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(new Beat("knight_front", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            knight(p.serverLevel(), c, 0);
            view(p, c.add(0.4, 1.8, 3.0), c.add(0, 1.3, 0));
        }, null, 30));
        out.add(new Beat("knight_possessed", (p, c) -> ((AbaddonKnightEntity) staged.getFirst()).setPossessor(java.util.UUID.randomUUID()),
                null, 40));
        out.add(still("knight_possessed_visor", (p, c) -> view(p, c.add(0.15, 1.95, 1.1), c.add(0, 1.85, 0))));
        // His blade hangs at his right (west, as he faces south).
        out.add(still("knight_possessed_blade", (p, c) -> view(p, c.add(-1.6, 1.4, 1.6), c.add(-0.45, 1.1, 0))));
        out.add(still("knight_possessed_night", (p, c) -> {
            p.serverLevel().setDayTime(18000);
            view(p, c.add(0.4, 1.8, 3.0), c.add(0, 1.3, 0));
        }));
        out.add(new Beat("herald_front", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            herald(p.serverLevel(), c, 0);
            view(p, c.add(0.3, 1.6, 2.6), c.add(eye));
        }, null, 30));
        out.add(still("herald_bell", (p, c) -> view(p, c.add(1.3, 1.2, 1.4), c.add(0.35, 0.8, 0))));
        // The Knights' Cleaver in the hand: first person, then third (front).
        out.add(new Beat("cleaver_first_person", (p, c) -> {
            clearStaged();
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.KNIGHTS_CLEAVER.get()));
            camFrom = null;
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3, 180, 10);
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 60));
        out.add(new Beat("cleaver_first_person_night", (p, c) -> p.serverLevel().setDayTime(18000), null, 20));
        out.add(new Beat("cleaver_third_person", (p, c) -> p.serverLevel().setDayTime(6000),
                () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false), 30));
        out.add(new Beat("cleaver_swing", (p, c) -> {
            ItemStack stack = p.getMainHandItem();
            if (stack.getItem() instanceof KnightsCleaverItem k) {
                KnightsCleaverItem.cleave(p, stack);
                k.play(p, stack, "cleave");
            }
        }, () -> camera(mc, CameraType.THIRD_PERSON_BACK, false), 6));
        out.add(wait("cleaver_wave", 8));
        out.add(new Beat("cleaver_wave_late", (p, c) -> {
        }, null, 14));
        // Every clip she plays once, caught at its blow; her retinue's too.
        for (String clip : AbaddonAssets.TRIGGERED) {
            int at = AbaddonAssets.HIT_TICKS.getOrDefault(clip, 20);
            out.add(new Beat("clip_" + clip, (p, c) -> {
                clearStaged();
                p.setGameMode(GameType.SPECTATOR);
                p.getInventory().clearContent();
                AbaddonEntity e = abaddon(p.serverLevel(), c, 0);
                e.triggerAnim("action", clip);
                view(p, c.add(2.0, 1.6, 2.4), c.add(eye));
            }, () -> camera(mc, CameraType.FIRST_PERSON, false), at + 6));
        }
        for (String clip : List.of("slash", "guard", "mark", "cast")) {
            int at = AbaddonAssets.RETINUE_HIT_TICKS.getOrDefault(clip, 10);
            boolean isKnight = clip.equals("slash") || clip.equals("guard");
            out.add(new Beat("clip_" + (isKnight ? "knight_" : "herald_") + clip, (p, c) -> {
                clearStaged();
                Mob e = isKnight ? knight(p.serverLevel(), c, 0) : herald(p.serverLevel(), c, 0);
                if (e instanceof AbaddonKnightEntity k) k.triggerAnim("action", clip);
                if (e instanceof AbaddonHeraldEntity h) h.triggerAnim("action", clip);
                view(p, c.add(2.0, 1.7, 2.6), c.add(eye));
            }, null, at + 6));
        }
        // --- the effects, staged on the plain floor ---
        // The herald's mark: the circle and the Brand over the hunter (an armour stand), the strike.
        out.add(new Beat("herald_mark", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            AbaddonHeraldEntity h = herald(level, c.add(-2.5, 0, 0), 0);
            h.triggerAnim("action", "mark");
            ArmorStand s = stand(level, c.add(1.5, 0, 4), 180);
            int windup = AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD;
            TelegraphMarker.circle(level, s.position(), AbaddonBalance.MARK_RADIUS, TelegraphMarker.RED, windup + 4);
            fx(p, new AbaddonFxPayload(h.getId(), AbaddonFxPayload.HERALD_MARK, s.getId(), Math.round(AbaddonBalance.MARK_RADIUS * 10), s.position(),
                    windup));
            view(p, c.add(5, 3.2, 9.5), c.add(0, 1.2, 2.5));
        }, null, 12));
        out.add(new Beat("herald_mark_glyph", (p, c) -> view(p, c.add(3.4, 2.4, 7.0), c.add(1.5, 2.2, 4)), null, 18));
        out.add(new Beat("herald_mark_hold", (p, c) -> view(p, c.add(4.5, 4.0, 9.0), c.add(1.5, 0.3, 4)), null, 12));
        out.add(new Beat("mark_strike", (p, c) -> fx(p, new AbaddonFxPayload(staged.getFirst().getId(), AbaddonFxPayload.MARK_STRIKE,
                Math.round(AbaddonBalance.MARK_RADIUS * 10), 0, c.add(1.5, 0, 4), 16)), null, 4));
        out.add(wait("mark_strike_fire", 8));
        out.add(wait("mark_strike_scorch", 20));
        // The sweep.
        out.add(new Beat("sweep_telegraph", (p, c) -> {
            clearStaged();
            AbaddonEntity a = abaddon(p.serverLevel(), c, 0);
            a.triggerAnim("action", "hellfire_sweep");
            TelegraphMarker.cone(p.serverLevel(), c, 0, AbaddonBalance.SWEEP_REACH, TelegraphMarker.RED, 40);
            view(p, c.add(7, 4.5, 10), c.add(0, 0.5, 4));
        }, null, 18));
        out.add(new Beat("sweep_run", (p, c) -> fx(p, new AbaddonFxPayload(staged.getFirst().getId(), AbaddonFxPayload.SWEEP, 0,
                Math.round(AbaddonBalance.SWEEP_REACH), c, 20)), null, 4));
        out.add(wait("sweep_fire", 8));
        out.add(wait("sweep_rolling", 14));
        out.add(still("sweep_close", (p, c) -> view(p, c.add(2.5, 1.5, 9.5), c.add(0, 0.8, 4))));
        // A knight rising out of the floor.
        out.add(new Beat("summon_cracks", (p, c) -> {
            clearStaged();
            AbaddonKnightEntity k = knight(p.serverLevel(), c.add(0, 0, 2), 0);
            fx(p, new AbaddonFxPayload(-1, AbaddonFxPayload.SUMMON, k.getId(), 0, k.position(), 30));
            view(p, c.add(2.5, 1.8, 6.5), c.add(0, 0.8, 2));
        }, null, 5));
        out.add(wait("summon_rising", 8));
        out.add(wait("summon_risen", 18));
        // The smoke out of her along the yellow lane, into the knight's helm; the possessed knight.
        out.add(new Beat("smoke_out_telegraph", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            AbaddonEntity a = abaddon(level, c, 0);
            knight(level, c.add(0, 0, 7), 180);
            a.triggerAnim("action", "smoke_out");
            TelegraphMarker.line(level, c, 0, AbaddonBalance.SMOKE_WIDTH, 7, TelegraphMarker.YELLOW, 40);
            view(p, c.add(6, 3.0, 4.5), c.add(0, 1.2, 3.5));
        }, null, 18));
        out.add(new Beat("smoke_out_leaves", (p, c) -> {
            AbaddonEntity a = (AbaddonEntity) staged.getFirst();
            a.triggerAnim("action", "possess");
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.SMOKE_OUT, staged.get(1).getId(), 0, a.position().add(0, 1.3, 0),
                    AbaddonBalance.SMOKE_FLIGHT));
        }, null, 4));
        out.add(wait("smoke_out_serpent", 5));
        out.add(new Beat("possess_dive", (p, c) -> ((AbaddonEntity) staged.getFirst()).beginPossession((AbaddonKnightEntity) staged.get(1)),
                null, 8));
        out.add(wait("possess_visor", 8));
        out.add(still("possessed_close", (p, c) -> view(p, c.add(1.6, 2.0, 9.5), c.add(0, 1.4, 7))));
        // Each way of casting her out, from a fresh possession each time.
        int[] hows = {AbaddonFxPayload.BY_EXORCISE, AbaddonFxPayload.BY_NAME, AbaddonFxPayload.BY_SALT, AbaddonFxPayload.BY_TRAP,
                AbaddonFxPayload.BY_VESSEL};
        String[] names = {"exorcise", "name", "salt", "trap", "vessel"};
        for (int i = 0; i < hows.length; i++) {
            int how = hows[i];
            String n = names[i];
            out.add(new Beat("expel_" + n + "_before", (p, c) -> {
                clearStaged();
                ServerLevel level = p.serverLevel();
                level.setDayTime(how % 2 == 0 ? 6000 : 18000);
                AbaddonEntity a = abaddon(level, c.add(-3, 0, 0), 0);
                AbaddonKnightEntity k = knight(level, c.add(0, 0, 2), 0);
                a.beginPossession(k);
                view(p, c.add(3.2, 2.2, 7.5), c.add(0, 1.3, 2));
            }, null, 24));
            out.add(new Beat("expel_" + n, (p, c) -> {
                AbaddonEntity a = (AbaddonEntity) staged.getFirst();
                a.expel(p, how);
                if (how == AbaddonFxPayload.BY_VESSEL) staged.get(1).kill();
            }, null, 4));
            out.add(wait("expel_" + n + "_tear", 6));
            out.add(wait("expel_" + n + "_coil", 10));
            out.add(wait("expel_" + n + "_reformed", 12));
            out.add(wait("expel_" + n + "_stunned", 20));
        }
        // Of her own will (the server's payload, staged).
        out.add(new Beat("release", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            AbaddonEntity a = abaddon(level, c.add(0, 0, 2), 0);
            AbaddonKnightEntity k = knight(level, c.add(0, 0, 2), 0);
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.POSSESS, k.getId(), 0, a.position(), 0));
            view(p, c.add(3.2, 2.2, 7.5), c.add(0, 1.3, 2));
        }, null, 12));
        out.add(wait("release_condensed", 16));
        // The pin.
        out.add(new Beat("pin", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            AbaddonEntity a = abaddon(level, c, 0);
            a.triggerAnim("action", "telekinetic_pin");
            ArmorStand s = stand(level, c.add(0.5, 0, 4.5), 180);
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.PIN, s.getId(), 0, s.position(), AbaddonBalance.PIN_TICKS));
            view(p, c.add(4, 2.4, 8.5), c.add(0.3, 1.0, 3.5));
        }, null, 10));
        out.add(wait("pin_held", 16));
        out.add(still("pin_close", (p, c) -> view(p, c.add(2.2, 1.6, 7.2), c.add(0.5, 0.9, 4.5))));
        // The Brand: its rings and gaps, the seal burning in, the burst.
        out.add(new Beat("brand_windup", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            level.setDayTime(18000);
            AbaddonEntity a = abaddon(level, c, 0);
            a.triggerAnim("action", "brand");
            int show = AbaddonBalance.BRAND_WINDUP + 4;
            for (float r : AbaddonBalance.BRAND_RINGS) {
                TelegraphMarker.ring(level, c, r + AbaddonBalance.BRAND_BAND, TelegraphMarker.RED, show);
                TelegraphMarker.ring(level, c, Math.max(0.5f, r - AbaddonBalance.BRAND_BAND), TelegraphMarker.RED, show);
                for (int g = 0; g < AbaddonBalance.BRAND_GAPS; g++) {
                    double ang = r + g * Math.PI;
                    TelegraphMarker.circle(level, c.add(Math.cos(ang) * r, 0, Math.sin(ang) * r), AbaddonBalance.BRAND_GAP_RADIUS, TelegraphMarker.SAFE,
                            show);
                }
            }
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.BRAND, AbaddonBalance.BRAND_RINGS.length, AbaddonBalance.BRAND_GAPS, c, show));
            view(p, c.add(0, 15, 17), c.add(0, 0, 1));
        }, null, 24));
        out.add(new Beat("brand_charged", (p, c) -> {
        }, null, 16));
        out.add(new Beat("brand_burst", (p, c) -> {
        }, null, 6));
        out.add(new Beat("brand_fire", (p, c) -> {
        }, null, 10));
        out.add(new Beat("brand_seal_close", (p, c) -> {
            clearStaged();
            AbaddonEntity a = abaddon(p.serverLevel(), c, 0);
            a.triggerAnim("action", "brand");
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.BRAND, 1, 0, c, 60));
            view(p, c.add(0.5, 4.2, 4.5), c.add(0, 0, 0.3));
        }, null, 40));
        // The devil's trap holding her, and her breaking it.
        out.add(new Beat("trapped", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            BlockPos centre = BlockPos.containing(c);
            paintTrap(level, centre);
            AbaddonEntity a = abaddon(level, c, 0);
            a.beginTrapped(level, centre);
            view(p, c.add(2.6, 2.6, 4.6), c.add(0, 0.9, 0));
        }, null, 30));
        out.add(still("trapped_night", (p, c) -> p.serverLevel().setDayTime(18000)));
        out.add(new Beat("trap_break", (p, c) -> {
            ((AbaddonEntity) staged.getFirst()).breakFree(p.serverLevel());
            clearTrap(p.serverLevel(), BlockPos.containing(c));
        }, null, 4));
        out.add(wait("trap_break_shards", 8));
        // The cleaver's wave, staged from a stand-in wielder.
        out.add(new Beat("cleave", (p, c) -> {
            clearStaged();
            p.serverLevel().setDayTime(6000);
            ArmorStand s = stand(p.serverLevel(), c.add(-4, 0, 2), -90);
            fx(p, new AbaddonFxPayload(s.getId(), AbaddonFxPayload.CLEAVE, -90, (int) AbaddonBalance.CLEAVE_REACH, s.position(), 10));
            view(p, c.add(0.5, 2.6, 8.5), c.add(0.5, 0.4, 2));
        }, null, 5));
        out.add(wait("cleave_mid", 4));
        out.add(wait("cleave_trail", 12));
        // Her death: her knights crumble, she burns, goes to smoke and down; the Book left glowing.
        out.add(new Beat("death_start", (p, c) -> {
            clearStaged();
            ServerLevel level = p.serverLevel();
            level.setDayTime(18000);
            AbaddonEntity a = abaddon(level, c, 0);
            for (int i = 0; i < 3; i++) knight(level, c.add(-3 + i * 3, 0, -3), 0);
            view(p, c.add(2.6, 2.0, 6.5), c.add(0, 1.0, -0.5));
        }, null, 20, false));
        out.add(new Beat("death_crumble", (p, c) -> {
            AbaddonEntity a = (AbaddonEntity) staged.getFirst();
            a.triggerAnim("action", "death");
            fx(p, new AbaddonFxPayload(a.getId(), AbaddonFxPayload.TITLE, 3, 1, a.position(), 100));
            for (int i = 1; i < staged.size(); i++) staged.get(i).discard();
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 14));
        out.add(wait("death_burning", 60));
        out.add(still("death_burning_close", (p, c) -> view(p, c.add(0.6, 1.6, 2.6), c.add(0, 1.2, 0))));
        out.add(wait("death_burning_late", 46));
        out.add(wait("death_smoke", 14));
        out.add(wait("death_sucked", 18));
        out.add(new Beat("death_book", (p, c) -> {
            staged.getFirst().discard();
            ItemEntity book = new ItemEntity(p.serverLevel(), c.x, c.y + 1.2, c.z, new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
            book.setPickUpDelay(32767);
            p.serverLevel().addFreshEntity(book);
            staged.add(book);
            view(p, c.add(1.4, 1.4, 2.6), c.add(0, 0.3, 0));
        }, null, 30));
        // The title cards (GUI on).
        for (int i = 1; i <= 4; i++) {
            int phase = Math.min(i, 3);
            boolean fall = i == 4;
            out.add(new Beat(fall ? "title_death" : "title_phase" + phase, (p, c) -> {
                clearStaged();
                p.serverLevel().setDayTime(fall ? 18000 : 12500);
                abaddon(p.serverLevel(), c, 0);
                view(p, c.add(0.3, 1.6, 3.2), c.add(eye));
                fx(p, new AbaddonFxPayload(-1, AbaddonFxPayload.TITLE, phase, fall ? 1 : 0, c, fall ? 100 : 90));
            }, () -> camera(mc, CameraType.FIRST_PERSON, true), 36));
        }
        return out;
    }

    // --- SN_PREVIEW=abaddon_fight ----------------------------------------------------------------------------------------------

    /** The real fight: her hall, her summoning, each attack forced in turn and photographed, the castings out, the trap, her death. */
    static final class Fight {

        /** What to do on a custom step's tick: the hunter, her. */
        interface Act {
            void run(ServerPlayer p, AbaddonEntity b);
        }

        /**
         * A step: an attack forced (its windup, camera and the ticks after it lands to photograph), or a custom sequence (acts by
         * tick, shots by tick, its length).
         */
        record Step(int phase, String name, Supplier<BossAttack<LuciferEntity>> attack, int windup, double dist, double height, double lookBias,
                    int[] after, List<Object[]> acts, int length) {
            static Step attack(int phase, String name, Supplier<BossAttack<LuciferEntity>> attack, int windup, double dist, double height,
                               double bias, int... after) {
                return new Step(phase, name, attack, windup, dist, height, bias, after, List.of(), 0);
            }

            static Step custom(int phase, String name, int length, int[] shots, Object... acts) {
                List<Object[]> list = new ArrayList<>();
                for (int i = 0; i < acts.length; i += 2) list.add(new Object[]{acts[i], acts[i + 1]});
                return new Step(phase, name, null, 0, 0, 0, 0, shots, list, length);
            }
        }

        private static AbaddonKnightEntity knightOf(AbaddonEntity b) {
            List<Entity> ks = b.retinue(true, false);
            if (ks.isEmpty()) {
                AbaddonAttacks.summonRetinue(b, 1, false);
                ks = b.retinue(true, false);
            }
            Entity best = null;
            for (Entity e : ks) if (best == null || e.distanceToSqr(b) < best.distanceToSqr(b)) best = e;
            return (AbaddonKnightEntity) best;
        }

        /** A possession, then a casting out by {@code how} (the knight killed for the vessel). */
        private static Step expel(String name, int how) {
            return Step.custom(2, name, 130, new int[]{12, 30, 52, 58, 66, 80, 100},
                    0, (Act) (p, b) -> {
                        AbaddonKnightEntity k = knightOf(b);
                        placeCamera(p, k, 7, 3, 0.8);
                    },
                    6, (Act) (p, b) -> b.beginPossession(knightOf(b)),
                    50, (Act) (p, b) -> {
                        if (how == AbaddonFxPayload.BY_VESSEL) {
                            // Killed outright (a kill passes through to the knight, not to her).
                            if (b.possessing() != null && p.serverLevel().getEntity(b.possessing()) instanceof AbaddonKnightEntity k) k.kill();
                        } else {
                            b.expel(p, how);
                        }
                    });
        }

        private static final List<Step> STEPS = List.of(
                Step.attack(1, "herald_mark", AbaddonAttacks.HeraldMark::new, AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD, 8, 4.5, 0.4,
                        2, 8, 24),
                Step.attack(1, "hellfire_sweep", AbaddonAttacks.HellfireSweep::new, AbaddonBalance.SWEEP_WINDUP, 7, 5, 0.5, 2, 8, 18),
                Step.attack(1, "summon_retinue", AbaddonAttacks.SummonRetinue::new, AbaddonBalance.SUMMON_WINDUP, 9, 4, 0.6, 3, 10, 22),
                Step.attack(2, "smoke_out", AbaddonAttacks.SmokeOut::new, AbaddonBalance.SMOKE_WINDUP, 8, 4, 0.5, 2, 6, 14, 30),
                Step.custom(2, "wait_out", 330, new int[]{40, 160, 300}),
                expel("expel_exorcise", AbaddonFxPayload.BY_EXORCISE),
                expel("expel_name", AbaddonFxPayload.BY_NAME),
                expel("expel_salt", AbaddonFxPayload.BY_SALT),
                expel("expel_trap", AbaddonFxPayload.BY_TRAP),
                expel("expel_vessel", AbaddonFxPayload.BY_VESSEL),
                Step.custom(2, "devils_trap", 160, new int[]{8, 40, 90, 124, 128, 136},
                        0, (Act) (p, b) -> {
                            paintTrap(p.serverLevel(), b.blockPosition());
                            placeCamera(p, b, 6, 3.5, 0.8);
                        },
                        150, (Act) (p, b) -> clearTrap(p.serverLevel(), BlockPos.containing(b.position()))),
                Step.attack(3, "telekinetic_pin", AbaddonAttacks.TelekineticPin::new, AbaddonBalance.PIN_WINDUP, 6, 3, 0.4, 2, 12, 30),
                Step.attack(3, "brand", AbaddonAttacks.Brand::new, AbaddonBalance.BRAND_WINDUP, 15, 14, 1.0, 2, 8, 20));

        private static volatile int bossId = -1;
        private static int step, queuedAt = -1, windupAt = -1, activeAt = -1, transitionAt = -1, deathAt = -1, summonedAt = -1, customAt = -1;
        private static int idleSince = -1;

        static void reset() {
            bossId = -1;
            step = 0;
            queuedAt = windupAt = activeAt = transitionAt = deathAt = summonedAt = idleSince = customAt = -1;
        }

        static boolean tick(Minecraft mc, MinecraftServer server) {
            Vec3 c = base;
            if (t == 20) {
                mc.options.hideGui = false;
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    ServerLevel level = p.serverLevel();
                    level.setDayTime(6000);
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().invulnerable = true;
                    p.getAbilities().mayfly = true;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    camFrom = null;
                    p.teleportTo(level, c.x, c.y, c.z + 3, 180, 0);
                });
            }
            if (t == 40) {
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    command(p, "supernatural boss summon abaddon");
                    AbaddonEntity boss = p.serverLevel().getEntitiesOfClass(AbaddonEntity.class, p.getBoundingBox().inflate(64)).stream()
                            .findFirst().orElse(null);
                    bossId = boss == null ? -1 : boss.getId();
                });
                summonedAt = t;
            }
            if (summonedAt < 0) return false;
            int since = t - summonedAt;
            if (since == 20 || since == 50 || since == 85 || since == 100) shot(mc, "intro_" + since);
            if (since > 400 && bossId < 0) return true;
            AbaddonEntity boss = mc.level == null || bossId < 0 ? null : mc.level.getEntity(bossId) instanceof AbaddonEntity e ? e : null;
            if (t % 4 == 0) server.execute(() -> {
                ServerPlayer p = player(server);
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.removeAllEffects();
            });
            if (deathAt >= 0) return death(mc, server);
            if (boss == null || since < 120) return false;
            byte state = boss.state();
            if (transitionAt >= 0) {
                int k = t - transitionAt;
                if (k == 14 || k == 42 || k == 70 || k == 95) shot(mc, "phase" + boss.phase() + "_transition_" + k);
                if (state == LuciferEntity.TRANSITION || k < 30) return false;
                transitionAt = -1;
                return false;
            }
            if (step >= STEPS.size()) {
                if (state == LuciferEntity.IDLE) {
                    deathAt = t;
                    int id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof AbaddonEntity b) {
                            b.setAbsorptionAmount(0);
                            BossHealthGuard.set(b, 1f);
                            b.invulnerableTime = 0;
                            b.hurt(AllDamageTypes.source(p.serverLevel(), AllDamageTypes.SMITE, p), 50f);
                            placeCamera(p, b, 7, 2.6, 0.7);
                        }
                    });
                }
                return false;
            }
            Step s = STEPS.get(step);
            if (queuedAt < 0) {
                if (state != LuciferEntity.IDLE) {
                    idleSince = -1;
                    return false;
                }
                if (idleSince < 0) idleSince = t;
                if (t - idleSince < 6) return false;
                if (boss.phase() < s.phase) {
                    transitionAt = t;
                    int to = s.phase, id = bossId;
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof AbaddonEntity b) {
                            if (to >= 2) p.serverLevel().setDayTime(18000);
                            BossHealthGuard.set(b, b.getMaxHealth() * (AbaddonBalance.PHASES - to + 1) / AbaddonBalance.PHASES + 1);
                            b.beginTransition(to);
                            placeCamera(p, b, 8, 2.6, 0.9);
                        }
                    });
                    return false;
                }
                queuedAt = t;
                windupAt = activeAt = customAt = -1;
                int id = bossId;
                if (s.attack != null) {
                    server.execute(() -> {
                        ServerPlayer p = player(server);
                        if (p.serverLevel().getEntity(id) instanceof AbaddonEntity b) {
                            placeCamera(p, b, s.dist, s.height, s.lookBias);
                            b.queue(s.attack);
                        }
                    });
                } else {
                    customAt = t;
                }
                return false;
            }
            String name = "phase" + s.phase + "_" + s.name;
            if (s.attack == null) {
                int k = t - customAt, id = bossId;
                for (Object[] act : s.acts) {
                    if ((Integer) act[0] == k) {
                        Act a = (Act) act[1];
                        server.execute(() -> {
                            ServerPlayer p = player(server);
                            if (p.serverLevel().getEntity(id) instanceof AbaddonEntity b) a.run(p, b);
                        });
                    }
                }
                for (int at : s.after) if (k == at) shot(mc, name + "_" + k);
                if (k >= s.length) {
                    step++;
                    queuedAt = -1;
                    idleSince = -1;
                }
                return false;
            }
            if (windupAt < 0 && state == LuciferEntity.WINDUP) windupAt = t;
            if (windupAt >= 0 && activeAt < 0 && t == windupAt + Math.max(4, (int) (s.windup * 0.7))) shot(mc, name + "_telegraph");
            if (activeAt < 0 && state == LuciferEntity.ACTIVE) activeAt = t;
            if (activeAt >= 0) {
                for (int k : s.after) if (t == activeAt + k) shot(mc, name + "_fx_" + k);
            }
            int last = activeAt < 0 ? 0 : activeAt + s.after[s.after.length - 1];
            boolean finished = activeAt >= 0 && t > last && (state == LuciferEntity.IDLE || state == LuciferEntity.RECOVER && t > last + 30);
            if (finished || t - queuedAt > 240) {
                if (activeAt < 0) org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("[preview] Abaddon never used {}", s.name);
                step++;
                queuedAt = -1;
                idleSince = -1;
            }
            return false;
        }

        /** Her death: the knights crumbling, her burning, the smoke dragged down, the Book, the victory. */
        private static boolean death(Minecraft mc, MinecraftServer server) {
            int k = t - deathAt;
            if (k == 12 || k == 40 || k == 80 || k == 120 || k == 150 || k == 168 || k == 180 || k == 192) shot(mc, "death_" + k);
            if (k == 205) {
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    ItemEntity book = p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(40),
                            e -> e.getItem().is(AllItems.BOOK_OF_THE_DAMNED.get())).stream().findFirst().orElse(null);
                    if (book != null) {
                        book.setPickUpDelay(32767);
                        view(p, book.position().add(1.4, 1.5, 2.4), book.position().add(0, 0.2, 0));
                    }
                });
            }
            if (k == 230) shot(mc, "book");
            if (k == 250) shot(mc, "victory");
            return k >= 270;
        }

        /**
         * The camera: the hunter hovering {@code dist} out from {@code target} (on the side they stand now) and {@code height} up,
         * looking at the point {@code lookBias} of the way from the hunter's feet to it.
         */
        private static void placeCamera(ServerPlayer p, Entity target, double dist, double height, double lookBias) {
            if (target == null) return;
            Vec3 bp = target.position();
            Vec3 dir = p.position().subtract(bp).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 0.5 ? new Vec3(0, 0, 1) : dir.normalize();
            Vec3 feet = bp.add(dir.scale(dist));
            Vec3 floor = new Vec3(feet.x, bp.y, feet.z);
            Vec3 at = floor.lerp(bp, lookBias).add(0, 0.8, 0);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            view(p, floor.add(0, height, 0), at);
        }
    }
}
