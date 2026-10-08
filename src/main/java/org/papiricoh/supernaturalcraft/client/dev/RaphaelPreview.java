package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.GarrisonAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelSummoning;
import org.papiricoh.supernaturalcraft.network.RaphaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Raphael, the archangel of the storm (v0.16) in the real renderer, well away from the origin. Screenshots land in
 * {@code runs/client/screenshots/sn_raphael_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=raphael}: the vessel front, back and side (by day and by night), the wings, the veins of phase III, each of
 *   his clips at its moment, held in the holy fire, the shadow of his wings on a wall at a flash, the threads of his garrison, the
 *   snap's safe band, a title card, the garrison, the Stormcaller (first and third person, inventory), the trophy, the oil.</li>
 *   <li>{@code SN_PREVIEW=raphael_house}: a real summoning; the abandoned house from outside and in, a ring lit, then the roof torn
 *   off (phase III).</li>
 *   <li>{@code SN_PREVIEW=raphael_fight}: a real summoning in a thunderstorm and the three phases forced, then his death.</li>
 * </ul>
 */
final class RaphaelPreview {

    private static final int STEP = 40;
    private static int t = -1;
    private static BlockPos ground;
    private static final List<Entity> shown = new ArrayList<>();
    private static RaphaelEntity boss;

    /** One moment: what the server sets up, what the client does, the picture's name (taken at its end), how long it lasts. */
    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client) {
            this(name, server, client, STEP);
        }
    }

    private static List<Beat> beats;
    private static int beatIndex, beatStart;

    private RaphaelPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (!"raphael".equals(scene) && !"raphael_house".equals(scene) && !"raphael_fight".equals(scene)) return false;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        t++;
        if ("raphael_fight".equals(scene)) return fight(mc, server);
        if (t == 0) {
            beats = "raphael".equals(scene) ? model(mc) : house(mc);
            // SN_RAPHAEL_ONLY=shadow,threads keeps only the beats whose names start so (quicker reviews).
            String only = System.getenv("SN_RAPHAEL_ONLY");
            if (only != null && !only.isBlank()) {
                List<String> keep = List.of(only.split(","));
                beats = beats.stream().filter(b -> keep.stream().anyMatch(k -> b.name.startsWith(k.trim()))).toList();
            }
            int z = "raphael".equals(scene) ? 412 : 1212;
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst(), 2000, z, "raphael".equals(scene)));
            return true;
        }
        if (t < 20 || ground == null) return true;
        if (beatIndex >= beats.size()) {
            if (t - beatStart == 5) {
                mc.setScreen(null);
                server.execute(RaphaelPreview::clearShown);
            }
            if (t - beatStart == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (local == 0) server.execute(() -> b.server.accept(server.getPlayerList().getPlayers().getFirst(), c));
        if (local == 2 && b.client != null) b.client.run();
        if (local == b.length - 2) {
            String prefix = "raphael".equals(scene) ? "sn_raphael_" : "sn_raphael_house_";
            Screenshot.grab(mc.gameDirectory, prefix + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return true;
    }

    // --- set-up and helpers --------------------------------------------------------------------------------------------

    private static void setUp(ServerPlayer p, int x, int z, boolean stage) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(x >> 4, z >> 4);
        ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (stage) {
            // A clean stage of old boards, nothing above it, and a wall at its north end (for the wings' shadow).
            for (int dx = -14; dx <= 14; dx++) {
                for (int dz = -14; dz <= 14; dz++) {
                    level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                    for (int dy = 0; dy < 10; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        p.setGameMode(GameType.SPECTATOR);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y + 2, c.z + 6, 180, 0);
    }

    private static void wall(ServerLevel level, boolean up) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int dy = 0; dy < 7; dy++) {
                level.setBlockAndUpdate(ground.offset(dx, dy, -5), up ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()
                        : Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void clearShown() {
        Minecraft.getInstance().execute(org.papiricoh.supernaturalcraft.client.raphael.ClientRaphael::clear);
        for (Entity e : shown) e.discard();
        shown.clear();
        boss = null;
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        shown.add(e);
        return e;
    }

    private static RaphaelEntity raphael(ServerLevel level, Vec3 at, float yaw, boolean wings, boolean veins) {
        RaphaelEntity r = place(level, AllEntities.RAPHAEL.get(), at, yaw);
        r.forceLook(veins ? RaphaelBalance.PHASES : 1);
        flag(r, "WINGS", wings);
        flag(r, "VEINS", veins);
        boss = r;
        return r;
    }

    /** Sets one of his synced looks directly (the preview has no fight to set them). */
    @SuppressWarnings("unchecked")
    static void flag(RaphaelEntity r, String field, boolean on) {
        try {
            var f = RaphaelEntity.class.getDeclaredField(field);
            f.setAccessible(true);
            r.getEntityData().set((EntityDataAccessor<Boolean>) f.get(null), on);
        } catch (ReflectiveOperationException | ClassCastException e) {
            org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("preview: no {} on Raphael", field);
        }
    }

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static void survival(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    private static void fx(ServerPlayer p, RaphaelFxPayload payload) {
        PacketDistributor.sendToPlayer(p, payload);
    }

    // --- SN_PREVIEW=raphael --------------------------------------------------------------------------------------------

    private static List<Beat> model(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.1, 0);
        out.add(new Beat("front", (p, c) -> {
            clearShown();
            raphael(p.serverLevel(), c, 0, false, false);
            view(p, c.add(0.3, 0.1, 1.7), c.add(eye));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("face", (p, c) -> view(p, c.add(0.15, 0.25, 0.85), c.add(0, 1.75, 0)), null));
        out.add(new Beat("back", (p, c) -> view(p, c.add(-0.3, 0.1, -1.7), c.add(eye)), null));
        out.add(new Beat("side", (p, c) -> view(p, c.add(1.7, 0.1, 0.2), c.add(eye)), null));
        out.add(new Beat("wings_front", (p, c) -> {
            clearShown();
            raphael(p.serverLevel(), c, 0, true, false);
            view(p, c.add(0, 0.4, 3.8), c.add(0, 1.4, 0));
        }, null));
        out.add(new Beat("wings_back", (p, c) -> view(p, c.add(0.4, 0.5, -3.8), c.add(0, 1.4, 0)), null));
        out.add(new Beat("wings_three_quarter", (p, c) -> view(p, c.add(2.9, 0.5, 2.6), c.add(0, 1.4, 0)), null));
        out.add(new Beat("wings_night", (p, c) -> {
            p.serverLevel().setDayTime(18000);
            view(p, c.add(0, 0.4, 3.8), c.add(0, 1.4, 0));
        }, null));
        out.add(new Beat("veins_night", (p, c) -> {
            clearShown();
            raphael(p.serverLevel(), c, 0, false, true);
            view(p, c.add(0.3, 0.1, 1.7), c.add(eye));
        }, null));
        out.add(new Beat("veins_wings_night", (p, c) -> {
            clearShown();
            raphael(p.serverLevel(), c, 0, true, true);
            view(p, c.add(0, 0.4, 3.8), c.add(0, 1.4, 0));
        }, null));
        out.add(new Beat("veins_day", (p, c) -> {
            p.serverLevel().setDayTime(6000);
            view(p, c.add(0.3, 0.1, 1.7), c.add(eye));
        }, null));
        // Every clip he plays once, caught at its moment (three-quarter view; the wings out where the clip spreads them).
        for (String clip : RaphaelAssets.TRIGGERED) {
            boolean wings = clip.equals("wings_reveal") || clip.equals("death") || clip.equals("blink");
            int at = RaphaelAssets.HIT_TICKS.getOrDefault(clip, RaphaelAssets.CLIP_TICKS.getOrDefault(clip, 40) / 2);
            if (clip.equals("smite")) at = 16;
            int length = at + 6;
            out.add(new Beat("clip_" + clip, (p, c) -> {
                clearShown();
                RaphaelEntity r = raphael(p.serverLevel(), c, 0, wings, false);
                r.triggerAnim("action", clip);
                double far = wings ? 4.0 : 2.3;
                view(p, c.add(far * 0.6, 0.6, far * 0.8), c.add(eye));
            }, null, length));
        }
        out.add(new Beat("clip_smite_fist", (p, c) -> {
            clearShown();
            RaphaelEntity r = raphael(p.serverLevel(), c, 0, false, false);
            r.triggerAnim("action", "smite");
            view(p, c.add(1.3, 0.4, 1.9), c.add(eye));
        }, null, RaphaelBalance.SMITE_WINDUP + 5));
        // Held in a lit ring of holy fire.
        out.add(new Beat("trapped", (p, c) -> {
            clearShown();
            ServerLevel level = p.serverLevel();
            RaphaelEntity r = raphael(level, c, 0, false, false);
            flag(r, "TRAPPED", true);
            BlockPos o = BlockPos.containing(c);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        level.setBlockAndUpdate(o.offset(dx, 0, dz), AllBlocks.HOLY_OIL_FIRE.get().defaultBlockState());
                    }
                }
            }
            fx(p, new RaphaelFxPayload(r.getId(), RaphaelFxPayload.TRAP, r.getId(), 0, Vec3.atBottomCenterOf(o), 400));
            view(p, c.add(1.9, 1.2, 3.2), c.add(eye));
        }, null, 50));
        out.add(new Beat("trapped_close", (p, c) -> view(p, c.add(0.4, 0.3, 2.6), c.add(eye)), null));
        // The shadow of his wings thrown up a wall by a flash behind the camera.
        out.add(new Beat("shadow_wall", (p, c) -> {
            clearShown();
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setBlockAndUpdate(o.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            level.setDayTime(18000);
            wall(level, true);
            RaphaelEntity r = raphael(level, c, 180, false, true);
            view(p, c.add(1.5, 0.8, 7.5), c.add(0, 2.0, -4));
        }, null, 30));
        out.add(new Beat("shadow_flash", (p, c) -> {
            if (boss != null) fx(p, new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.FLASH, 1, 0, c.add(0.5, 0, 9), 6));
        }, null, 5));
        out.add(new Beat("shadow_flash_late", (p, c) -> {
            if (boss != null) fx(p, new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.FLASH, 1, 0, c.add(-4, 0, 8), 6));
        }, null, 9));
        out.add(new Beat("shadow_floor", (p, c) -> {
            ServerLevel level = p.serverLevel();
            wall(level, false);
            if (boss != null) fx(p, new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.FLASH, 1, 0, c.add(3, 0, 6), 8));
            view(p, c.add(0.01, 9, 3), c.add(0, 0, -2));
        }, null, 6));
        // The threads of his garrison.
        out.add(new Beat("threads", (p, c) -> {
            clearShown();
            ServerLevel level = p.serverLevel();
            level.setDayTime(18000);
            RaphaelEntity r = raphael(level, c, 0, false, false);
            r.triggerAnim("action", "heal_channel");
            double[][] spots = {{-6, -4}, {6, -4}, {-5, 5}, {5, 5}};
            for (double[] s : spots) {
                GarrisonAngelEntity a = place(level, AllEntities.GARRISON_ANGEL.get(), c.add(s[0], 0, s[1]),
                        (float) Math.toDegrees(Math.atan2(s[0], -s[1])));
                fx(p, new RaphaelFxPayload(a.getId(), RaphaelFxPayload.TETHER, r.getId(), 0, a.position(), 400));
            }
            view(p, c.add(0, 5, 11), c.add(0, 0.8, 0));
        }, null, 30));
        out.add(new Beat("threads_close", (p, c) -> view(p, c.add(3, 1.2, 4), c.add(-2, 1.2, -2)), null, 30));
        // The snap: its safe band, then the burst.
        out.add(new Beat("snap_band", (p, c) -> {
            clearShown();
            RaphaelEntity r = raphael(p.serverLevel(), c, 0, false, true);
            r.triggerAnim("action", "snap");
            fx(p, new RaphaelFxPayload(r.getId(), RaphaelFxPayload.SNAP, RaphaelBalance.SNAP_RADIUS, RaphaelBalance.SNAP_SAFE_WIDTH, c, 44));
            view(p, c.add(0.01, 16, 12), c);
        }, null, 30));
        out.add(new Beat("snap_burst", (p, c) -> {
        }, null, 22));
        // A title card (with the HUD).
        out.add(new Beat("title_phase3", (p, c) -> {
            if (boss != null) fx(p, new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.TITLE, 3, 0, c, 90));
            view(p, c.add(0.3, 0.4, 2.8), c.add(eye));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 34));
        out.add(new Beat("title_fall", (p, c) -> {
            if (boss != null) fx(p, new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.TITLE, 3, 1, c, 90));
        }, null, 34));
        // His garrison, front and back.
        out.add(new Beat("garrison_front", (p, c) -> {
            clearShown();
            p.serverLevel().setDayTime(6000);
            place(p.serverLevel(), AllEntities.GARRISON_ANGEL.get(), c, 0);
            view(p, c.add(0.3, 0.1, 1.8), c.add(eye));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("garrison_back", (p, c) -> view(p, c.add(-0.3, 0.1, -1.8), c.add(eye)), null));
        out.add(new Beat("garrison_with_him", (p, c) -> {
            clearShown();
            raphael(p.serverLevel(), c, 0, false, false);
            place(p.serverLevel(), AllEntities.GARRISON_ANGEL.get(), c.add(-1.6, 0, 0.6), 0);
            place(p.serverLevel(), AllEntities.GARRISON_ANGEL.get(), c.add(1.6, 0, 0.6), 0);
            view(p, c.add(0, 0.4, 4), c.add(eye));
        }, null));
        // The Stormcaller in hand, the trophy, the oil, the inventory.
        out.add(new Beat("staff_fp", (p, c) -> {
            clearShown();
            survival(p);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 180, 10);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.RAPHAELS_STORMCALLER.get()));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 70));
        out.add(new Beat("staff_fp_held", (p, c) -> survival(p), () -> camera(mc, CameraType.FIRST_PERSON, true), 40));
        out.add(new Beat("staff_tp", (p, c) -> {
        }, () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false)));
        out.add(new Beat("trophy", (p, c) -> {
            BlockPos at = BlockPos.containing(c).relative(Direction.NORTH, 2);
            p.serverLevel().setBlockAndUpdate(at, AllBlocks.RAPHAEL_TROPHY.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            p.setGameMode(GameType.SPECTATOR);
            view(p, Vec3.atBottomCenterOf(at).add(0.5, 0.3, 1.1), Vec3.atCenterOf(at).add(0, 0.2, 0));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("oil", (p, c) -> {
            ServerLevel level = p.serverLevel();
            BlockPos o = BlockPos.containing(c).relative(Direction.SOUTH, 4);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        level.setBlockAndUpdate(o.offset(dx, 0, dz), AllBlocks.HOLY_OIL_SLICK.get().defaultBlockState());
                    }
                }
            }
            view(p, Vec3.atBottomCenterOf(o).add(0.5, 4, 4), Vec3.atBottomCenterOf(o));
        }, null));
        out.add(new Beat("inventory", (p, c) -> {
            survival(p);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3, 180, 0);
            ItemStack[] loot = {new ItemStack(AllItems.RAPHAELS_STORMCALLER.get()), new ItemStack(AllItems.RAPHAEL_TROPHY.get()),
                    new ItemStack(AllItems.RAPHAEL_SPAWN_EGG.get()), new ItemStack(AllItems.GARRISON_ANGEL_SPAWN_EGG.get())};
            for (int i = 0; i < loot.length; i++) p.getInventory().setItem(i, loot[i]);
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }));
        return out;
    }

    // --- SN_PREVIEW=raphael_house --------------------------------------------------------------------------------------

    private static List<Beat> house(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("summon", (p, c) -> {
            ServerLevel level = p.serverLevel();
            level.setWeatherParameters(0, 6000, true, true);
            boss = RaphaelSummoning.summon(level, ground, p);
            if (boss != null) boss.buildHouseNow();
            view(p, c.add(22, 12, 26), c);
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), 80));
        out.add(new Beat("outside_front", (p, c) -> view(p, c.add(4, 6, 22), c.add(0, 2, 0)), null));
        out.add(new Beat("outside_corner", (p, c) -> view(p, c.add(-18, 10, -16), c.add(0, 2, 0)), null));
        out.add(new Beat("inside", (p, c) -> view(p, c.add(-6, 1.8, 6), c.add(2, 1.2, 0)), null));
        out.add(new Beat("inside_rings", (p, c) -> view(p, c.add(0.01, 2.2, 0), c.add(0, 0, 0.5)), null));
        out.add(new Beat("ring_lit", (p, c) -> {
            if (boss != null) boss.ignite(0);
            view(p, c.add(-4, 3, 5), c.add(0, 0.5, 0));
        }, null, 50));
        out.add(new Beat("roof_off", (p, c) -> {
            if (boss != null) {
                BossHealthGuard.set(boss, boss.getMaxHealth() * 0.3f);
                boss.beginTransition(RaphaelBalance.PHASES);
            }
            view(p, c.add(22, 12, 26), c);
        }, null, 260));
        out.add(new Beat("roof_off_above", (p, c) -> view(p, c.add(0.01, 26, 6), c), null));
        out.add(new Beat("roof_off_inside", (p, c) -> view(p, c.add(-6, 1.8, 6), c.add(2, 3, 0)), null));
        out.add(new Beat("restored", (p, c) -> {
            ServerLevel level = p.serverLevel();
            if (boss != null) boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
            level.setWeatherParameters(6000, 0, false, false);
            view(p, c.add(16, 14, 20), c);
        }, null, 60));
        return out;
    }

    // --- SN_PREVIEW=raphael_fight --------------------------------------------------------------------------------------

    /** Tick each phase is forced, and when he falls. */
    private static final int[] PHASE_AT = {0, 700, 1300};
    private static final int DEATH_AT = 1900, END = 2100;

    private static boolean fight(Minecraft mc, MinecraftServer server) {
        int now = t;
        if (now == 1) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        server.execute(() -> fightServer(server.getPlayerList().getPlayers().getFirst(), now));
        if (now >= 100 && now % 40 == 0 && now < END) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_raphael_fight_%04d.png", now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now >= END) mc.stop();
        return true;
    }

    private static void fightServer(ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            setUp(p, 2000, 2012, false);
            level.setDayTime(14000);
            level.setWeatherParameters(0, 6000, true, true);
            survival(p);
            Vec3 c = Vec3.atBottomCenterOf(ground);
            p.teleportTo(level, c.x, c.y, c.z - 3, 0, 10);
        }
        if (now == 12) boss = RaphaelSummoning.summon(level, ground, p);
        if (boss == null || boss.isRemoved()) return;
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            if (to.length() > 9 || to.length() < 4) p.teleportTo(level, boss.getX() - to.normalize().x * 7, boss.getY(), boss.getZ() - to.normalize().z * 7, p.getYRot(), p.getXRot());
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(0, 1.2, 0));
        }
        for (int i = 1; i < PHASE_AT.length; i++) {
            if (now == PHASE_AT[i] && boss.phase() < i + 1) {
                BossHealthGuard.set(boss, boss.getMaxHealth() * (RaphaelBalance.PHASES - i) / RaphaelBalance.PHASES + 1);
                boss.beginTransition(i + 1);
            }
        }
        if (now == DEATH_AT) {
            boss.setAbsorptionAmount(0);
            BossHealthGuard.set(boss, 1f);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 50f);
        }
        if (now == DEATH_AT + 20 && boss.isAlive()) boss.kill();
        if (now == END - 5) {
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
            level.setWeatherParameters(6000, 0, false, false);
        }
    }
}
