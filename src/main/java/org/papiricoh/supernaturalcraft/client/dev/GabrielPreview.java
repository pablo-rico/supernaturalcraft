package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.PieProjectile;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.trickster.PrankRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Gabriel, the Trickster (v0.14) in the real renderer, well away from the origin. Screenshots land in
 * {@code runs/client/screenshots/sn_gabriel_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=gabriel}: each costume front and back (close) and all five in a row (far); a nurse double; the real one in
 *   the commercial with his wings and the six-winged shadow among four spokesmen; the pie in flight; mobs in party hats; the
 *   remote in hand (first and third person); the trophy; the loot in the inventory.</li>
 *   <li>{@code SN_PREVIEW=gabriel_fight}: a real summoning; the four channels forced one after another (all the HUD comes from the
 *   server's own payloads: static, the OSD, the signs, the quiz panel, the heart monitor, the shadow, the banner bar, title
 *   cards) and his death.</li>
 *   <li>{@code SN_PREVIEW=gabriel_pranks}: every prank, through the server's prank code when it has a
 *   {@code TricksterPranks.play(ServerPlayer, Prank)}, else through its payloads.</li>
 * </ul>
 */
final class GabrielPreview {

    private static final int STEP = 40;
    private static int t = -1;
    private static BlockPos ground;
    private static final List<Entity> shown = new ArrayList<>();
    private static GabrielEntity boss;

    /** One moment: what the server sets up, what the client does, the picture's name (taken at its end), how long it lasts. */
    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client) {
            this(name, server, client, STEP);
        }
    }

    private static List<Beat> beats;
    private static int beatIndex, beatStart;

    private GabrielPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (!"gabriel".equals(scene) && !"gabriel_fight".equals(scene) && !"gabriel_pranks".equals(scene)) return false;
        var server = mc.getSingleplayerServer();
        t++;
        if ("gabriel_fight".equals(scene)) return fight(mc, server);
        if (t == 0) {
            beats = "gabriel".equals(scene) ? model(mc) : pranks(mc);
            int z = "gabriel".equals(scene) ? 412 : 1212;
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst(), 1400, z));
            return true;
        }
        if (t < 20 || ground == null) return true;
        if (beatIndex >= beats.size()) {
            if (t - beatStart == 5) {
                mc.setScreen(null);
                server.execute(GabrielPreview::clearShown);
            }
            if (t - beatStart == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(beatIndex);
        int local = t - beatStart;
        if (beatStart == 0) {
            beatStart = t;
            local = 0;
        }
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (local == 0) server.execute(() -> b.server.accept(server.getPlayerList().getPlayers().getFirst(), c));
        if (local == 4 && b.client != null) b.client.run();
        if (local == b.length - 2) {
            String prefix = "gabriel".equals(scene) ? "sn_gabriel_" : "sn_gabriel_pranks_";
            Screenshot.grab(mc.gameDirectory, prefix + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= b.length) {
            beatIndex++;
            // The next beat's tick 0 is the next tick.
            beatStart = t + 1;
        }
        return true;
    }

    // --- set-up and helpers --------------------------------------------------------------------------------------------

    private static void setUp(ServerPlayer p, int x, int z) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(x >> 4, z >> 4);
        ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        // A clean stage: a stone floor, nothing above it.
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        p.setGameMode(GameType.SPECTATOR);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y + 2, c.z + 6, 180, 0);
    }

    private static void clearShown() {
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

    private static GabrielDoubleEntity double_(ServerLevel level, Vec3 at, float yaw, GabrielDoubleEntity.Role role, Channel.Costume costume) {
        GabrielDoubleEntity d = place(level, AllEntities.GABRIEL_DOUBLE.get(), at, yaw);
        d.setRole(role);
        d.setCostume(costume);
        return d;
    }

    private static GabrielEntity gabriel(ServerLevel level, Vec3 at, float yaw, int phase) {
        GabrielEntity g = place(level, AllEntities.GABRIEL.get(), at, yaw);
        g.forceLook(phase);
        boss = g;
        return g;
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

    // --- SN_PREVIEW=gabriel --------------------------------------------------------------------------------------------

    private static List<Beat> model(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 0.8, 0);
        // Each costume, worn by a double (the real one only wears his jacket while he arrives or falls): front and back, close.
        for (Channel.Costume costume : Channel.Costume.values()) {
            out.add(new Beat(costume.id() + "_front", (p, c) -> {
                clearShown();
                p.setGameMode(GameType.SPECTATOR);
                double_(p.serverLevel(), c, 0, GabrielDoubleEntity.Role.EXTRA, costume);
                view(p, c.add(0.5, -0.5, 2.2), c.add(eye));
            }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
            out.add(new Beat(costume.id() + "_back", (p, c) -> view(p, c.add(-0.5, -0.4, -2.2), c.add(eye)), null));
        }
        // All five in a row, from further off.
        out.add(new Beat("costumes_far", (p, c) -> {
            clearShown();
            Channel.Costume[] all = Channel.Costume.values();
            for (int i = 0; i < all.length; i++) {
                double_(p.serverLevel(), c.add((i - 2) * 1.5, 0, 0), 0, GabrielDoubleEntity.Role.EXTRA, all[i]);
            }
            view(p, c.add(0, 0.6, 8), c.add(0, 0.8, 0));
        }, null));
        out.add(new Beat("nurse", (p, c) -> {
            clearShown();
            double_(p.serverLevel(), c, 0, GabrielDoubleEntity.Role.NURSE, Channel.Costume.LAB_COAT);
            view(p, c.add(0.5, -0.5, 2.2), c.add(eye));
        }, null));
        // The real one in the commercial: wings out, his shadow on the floor, four spokesmen beside him.
        out.add(new Beat("wings_front", (p, c) -> {
            clearShown();
            gabriel(p.serverLevel(), c, 0, 4);
            view(p, c.add(0, 0, 4.5), c.add(0, 1.1, 0));
        }, null));
        out.add(new Beat("wings_back", (p, c) -> view(p, c.add(1, 0.2, -4.5), c.add(0, 1.1, 0)), null));
        out.add(new Beat("spokesmen", (p, c) -> {
            clearShown();
            GabrielEntity g = gabriel(p.serverLevel(), c.add(1.8, 0, 0), 0, 4);
            for (double x : new double[]{-3.6, -1.8, 0, 3.6}) {
                double_(p.serverLevel(), c.add(x, 0, 0), 0, GabrielDoubleEntity.Role.SPOKESMAN, Channel.Costume.SUIT);
            }
            PacketDistributor.sendToPlayer(p, new GabrielFxPayload(g.getId(), GabrielFxPayload.REVEAL, 0, 0, g.position(), 400));
            view(p, c.add(0, 5.5, 8), c.add(0, 0, 0));
        }, null, 50));
        out.add(new Beat("shadow_top", (p, c) -> {
            if (boss != null) view(p, boss.position().add(0.01, 8, 1.5), boss.position());
        }, null));
        // The pie, hanging in the air.
        out.add(new Beat("pie", (p, c) -> {
            clearShown();
            PieProjectile pie = new PieProjectile(AllEntities.GABRIEL_PIE.get(), p.serverLevel());
            pie.moveTo(c.x, c.y + 1.2, c.z, 0, 0);
            pie.setNoGravity(true);
            pie.setDeltaMovement(Vec3.ZERO);
            p.serverLevel().addFreshEntity(pie);
            shown.add(pie);
            view(p, c.add(0.5, -0.5, 1.3), c.add(0, 1.2, 0));
        }, null));
        // Party hats on a zombie, a cow, a villager and a chicken.
        out.add(new Beat("party_hats", (p, c) -> {
            clearShown();
            ServerLevel level = p.serverLevel();
            Mob[] mobs = {place(level, EntityType.HUSK, c.add(-2.4, 0, 0), 0), place(level, EntityType.COW, c.add(-0.6, 0, 0), 20),
                    place(level, EntityType.VILLAGER, c.add(1.2, 0, 0), -10), place(level, EntityType.CHICKEN, c.add(2.6, 0, 0.5), 0)};
            for (Mob m : mobs) PacketDistributor.sendToPlayer(p, new GabrielFxPayload(m.getId(), GabrielFxPayload.HAT, 0, 0, m.position(), 2000));
            view(p, c.add(0, 0.4, 5), c.add(0, 1.1, 0));
        }, null));
        out.add(new Beat("party_hats_side", (p, c) -> view(p, c.add(5, 0.4, 1.5), c.add(0, 1.1, 0)), null));
        // The remote in hand: first person, then third.
        out.add(new Beat("remote_fp", (p, c) -> {
            clearShown();
            survival(p);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 180, 15);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.TRICKSTER_REMOTE.get()));
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 70));
        out.add(new Beat("remote_tp", (p, c) -> {
        }, () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false)));
        out.add(new Beat("blade_fp", (p, c) -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.GABRIEL_BLADE.get())),
                () -> camera(mc, CameraType.FIRST_PERSON, true), 60));
        // The trophy, and the spoils in the inventory.
        out.add(new Beat("trophy", (p, c) -> {
            BlockPos at = BlockPos.containing(c).relative(Direction.NORTH, 2);
            p.serverLevel().setBlockAndUpdate(at, AllBlocks.GABRIEL_TROPHY.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            p.setGameMode(GameType.SPECTATOR);
            view(p, Vec3.atBottomCenterOf(at).add(0.9, 1.3, 1.8), Vec3.atCenterOf(at));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("inventory", (p, c) -> {
            survival(p);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3, 180, 0);
            ItemStack[] loot = {new ItemStack(AllItems.TRICKSTER_REMOTE.get()), new ItemStack(AllItems.GABRIEL_BLADE.get()),
                    new ItemStack(AllItems.GABRIEL_TROPHY.get()), new ItemStack(AllItems.TRICKSTER_CANDY.get(), 5),
                    new ItemStack(AllItems.TRICKSTER_BAIT.get()), new ItemStack(AllItems.CANDY_WRAPPER.get()),
                    new ItemStack(AllItems.GABRIEL_SPAWN_EGG.get())};
            for (int i = 0; i < loot.length; i++) p.getInventory().setItem(i, loot[i]);
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }));
        return out;
    }

    // --- SN_PREVIEW=gabriel_pranks -------------------------------------------------------------------------------------

    private static BlockPos chest;

    private static List<Beat> pranks(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("set", (p, c) -> {
            ServerLevel level = p.serverLevel();
            survival(p);
            p.teleportTo(level, c.x, c.y, c.z + 4, 180, 15);
            chest = BlockPos.containing(c).relative(Direction.WEST, 2);
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            place(level, EntityType.COW, c.add(1.5, 0, 0), 30);
            place(level, EntityType.VILLAGER, c.add(-0.5, 0, -1.5), 0);
        }, () -> camera(mc, CameraType.FIRST_PERSON, true)));
        for (PrankRules.Prank prank : PrankRules.Prank.values()) {
            out.add(new Beat(prank.id(), (p, c) -> play(p, c, prank), null, 60));
        }
        return out;
    }

    /** Plays a prank through the server's own code if it is there, else stages it and sends its payloads. */
    private static void play(ServerPlayer p, Vec3 c, PrankRules.Prank prank) {
        try {
            Class<?> pranks = Class.forName("org.papiricoh.supernaturalcraft.trickster.TricksterPranks");
            pranks.getMethod("play", ServerPlayer.class, PrankRules.Prank.class).invoke(null, p, prank);
            return;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Not written yet: do it by hand.
        }
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        switch (prank) {
            case CANDY_WRAPPER -> {
                ItemEntity wrapper = new ItemEntity(level, c.x + 0.6, c.y + 0.1, c.z + 2.6, new ItemStack(AllItems.CANDY_WRAPPER.get()));
                wrapper.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(wrapper);
                shown.add(wrapper);
                at = wrapper.position();
            }
            case PARTY_HAT -> {
                for (Entity e : shown) {
                    if (e.getType() == EntityType.COW) {
                        PacketDistributor.sendToPlayer(p, new GabrielFxPayload(e.getId(), GabrielFxPayload.HAT, 0, 0, e.position(), 2000));
                        at = e.position();
                    }
                }
            }
            case LAUGH_TRACK -> at = c.add(-10, 0, -10);
            case TV_LINE -> p.sendSystemMessage(net.minecraft.network.chat.Component.literal("<Villager> I'm not a doctor, but I play one on TV."));
            case CHEST -> {
                if (chest != null) {
                    level.blockEvent(chest, Blocks.CHEST, 1, 1);
                    BlockPos shut = chest;
                    level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 30,
                            () -> level.blockEvent(shut, Blocks.CHEST, 1, 0)));
                    at = Vec3.atCenterOf(chest);
                }
            }
        }
        PacketDistributor.sendToPlayer(p, new GabrielFxPayload(-1, GabrielFxPayload.PRANK, prank.ordinal(), 0, at, 0));
    }

    // --- SN_PREVIEW=gabriel_fight --------------------------------------------------------------------------------------

    /** Tick each channel begins (the first after his arrival), and when he falls. */
    private static final int[] PHASE_AT = {220, 620, 1020, 1420};
    private static final int DEATH_AT = 1820, END = 2000;

    private static boolean fight(Minecraft mc, net.minecraft.server.MinecraftServer server) {
        int now = t;
        if (now == 1) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        server.execute(() -> fightServer(server.getPlayerList().getPlayers().getFirst(), now));
        if (now >= 100 && now % 40 == 0 && now < END) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_gabriel_fight_%04d.png", now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now >= END) mc.stop();
        return true;
    }

    private static void fightServer(ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            setUp(p, 1400, 2012);
            level.setDayTime(18000);
            survival(p);
            Vec3 c = Vec3.atBottomCenterOf(ground);
            p.teleportTo(level, c.x, c.y, c.z + 10, 180, 10);
        }
        if (now == 12) boss = GabrielSummoning.summon(level, ground.relative(Direction.SOUTH, 4), p);
        if (boss == null || boss.isRemoved()) return;
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            if (to.length() > 9) p.teleportTo(level, boss.getX() - to.normalize().x * 8, boss.getY(), boss.getZ() - to.normalize().z * 8, p.getYRot(), p.getXRot());
            // Look a little to one side of him, so the hunter's own back does not hide him.
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2, to.length() * 0.4));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, 0.8, 0));
        }
        for (int i = 1; i < PHASE_AT.length; i++) {
            if (now == PHASE_AT[i] && boss.phase() < i + 1) {
                boss.setHealth(boss.getMaxHealth() * (GabrielEntity.MAX_PHASE - i) / GabrielEntity.MAX_PHASE + 1);
                boss.beginTransition(i + 1);
            }
        }
        if (now == DEATH_AT) {
            boss.setAbsorptionAmount(0);
            boss.setHealth(1f);
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 50f);
        }
        if (now == DEATH_AT + 20 && boss.isAlive()) boss.kill();
        if (now == END - 5) {
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
    }
}
