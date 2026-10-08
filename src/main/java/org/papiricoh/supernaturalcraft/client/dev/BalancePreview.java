package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * {@code SN_PREVIEW=balance} (v0.15): Ascension Shards, ascended weapons and Hunter's Gear in the real renderer, well away from
 * the origin. Screenshots land in {@code runs/client/screenshots/sn_balance_*.png}: shards in hand (first and third person),
 * the icons with their tier marks and the tooltips of an ascended weapon, armour and shard, the Hellforge with a shard waiting,
 * Hunter's Gear on an armour stand (front, back, side) and worn (front and back), and the hotbar and inventory.
 */
final class BalancePreview {

    private static final int STEP = 40;
    private static int t = -1;
    private static BlockPos ground;
    private static final List<Entity> shown = new ArrayList<>();

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client) {
            this(name, server, client, STEP);
        }
    }

    private static List<Beat> beats;
    private static int beatIndex, beatStart;

    private BalancePreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"balance".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        t++;
        if (t == 0) {
            beats = beats(mc);
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst(), 1600, 412));
            return true;
        }
        if (t < 20 || ground == null) return true;
        if (beatIndex >= beats.size()) {
            if (t - beatStart == 5) {
                mc.setScreen(null);
                server.execute(BalancePreview::clearShown);
            }
            if (t - beatStart == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (local == 0) server.execute(() -> b.server.accept(server.getPlayerList().getPlayers().getFirst(), c));
        if (local == 4 && b.client != null) b.client.run();
        if (local == b.length - 2) {
            Screenshot.grab(mc.gameDirectory, "sn_balance_" + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= b.length) {
            beatIndex++;
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
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy < 6; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        survival(p);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y, c.z, 180, 0);
    }

    private static void clearShown() {
        for (Entity e : shown) e.discard();
        shown.clear();
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    /** The hotbar slot is the client's to choose; it tells the server itself. */
    private static void select(Minecraft mc, int slot) {
        mc.execute(() -> {
            if (mc.player != null) mc.player.getInventory().selected = slot;
            mc.getToasts().clear();
        });
    }

    private static void survival(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    static ItemStack ascended(Item item, int level) {
        ItemStack s = new ItemStack(item);
        s.set(AllDataComponents.ASCENSION, level);
        return s;
    }

    private static List<ItemStack> gear(int level) {
        return List.of(ascended(AllItems.HUNTERS_CAP.get(), level), ascended(AllItems.HUNTERS_JACKET.get(), level),
                ascended(AllItems.HUNTERS_JEANS.get(), level), ascended(AllItems.HUNTERS_BOOTS.get(), level));
    }

    private static void wear(net.minecraft.world.entity.LivingEntity e, int level) {
        List<ItemStack> g = gear(level);
        e.setItemSlot(EquipmentSlot.HEAD, g.get(0));
        e.setItemSlot(EquipmentSlot.CHEST, g.get(1));
        e.setItemSlot(EquipmentSlot.LEGS, g.get(2));
        e.setItemSlot(EquipmentSlot.FEET, g.get(3));
    }

    private static void hotbar(ServerPlayer p) {
        p.getInventory().clearContent();
        for (int tier = 1; tier <= 5; tier++) p.getInventory().setItem(tier - 1, new ItemStack(AllItems.shardOf(tier).get(), tier));
        p.getInventory().setItem(5, ascended(AllItems.ANGEL_BLADE.get(), 2));
        p.getInventory().setItem(6, ascended(AllItems.ARCHANGEL_BLADE.get(), 5));
        p.getInventory().setItem(7, ascended(AllItems.EMBER_STAFF.get(), 1));
        p.getInventory().setItem(8, ascended(AllItems.MICHAEL_LANCE.get(), 4));
        List<ItemStack> g = gear(4);
        for (int i = 0; i < g.size(); i++) p.getInventory().setItem(9 + i, g.get(i));
        p.getInventory().setItem(13, ascended(AllItems.GENERAL_HELMET.get(), 5));
        p.getInventory().setItem(14, new ItemStack(AllItems.GENERAL_CHESTPLATE.get()));
        p.getInventory().setItem(15, ascended(AllItems.PENUMBRA.get(), 3));
    }

    /** A plain screen showing icons (with their decorations) and three tooltips side by side. */
    private static final class Showcase extends Screen {
        private final int tip;

        Showcase(int tip) {
            super(Component.literal("balance"));
            this.tip = tip;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float partial) {
            g.fill(0, 0, width, height, 0xFF202028);
            List<ItemStack> icons = new ArrayList<>();
            for (int tier = 1; tier <= 5; tier++) icons.add(new ItemStack(AllItems.shardOf(tier).get()));
            for (int level = 1; level <= 5; level++) icons.add(ascended(AllItems.ANGEL_BLADE.get(), level));
            icons.addAll(gear(4));
            icons.add(new ItemStack(AllItems.GENERAL_HELMET.get()));
            icons.add(ascended(AllItems.GENERAL_HELMET.get(), 5));
            g.pose().pushPose();
            g.pose().scale(2, 2, 1);
            for (int i = 0; i < icons.size(); i++) {
                int x = 8 + (i % 10) * 20, y = 6 + (i / 10) * 20;
                g.renderItem(icons.get(i), x, y);
                g.renderItemDecorations(font, icons.get(i), x, y);
            }
            g.pose().popPose();
            ItemStack[] tips = {ascended(AllItems.ARCHANGEL_BLADE.get(), 5), ascended(AllItems.HUNTERS_JACKET.get(), 4),
                    new ItemStack(AllItems.ASCENSION_SHARD_3.get())};
            g.renderComponentTooltip(font, Screen.getTooltipFromItem(minecraft, tips[tip]), 6, 110);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static List<Beat> beats(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        // Shards in hand.
        out.add(new Beat("shard_fp", (p, c) -> {
            hotbar(p);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 180, 15);
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            select(mc, 4);
        }, 70));
        out.add(new Beat("shard3_fp", (p, c) -> {
        }, () -> select(mc, 2), 60));
        out.add(new Beat("blade_v_fp", (p, c) -> {
        }, () -> select(mc, 6), 60));
        out.add(new Beat("shard_tp", (p, c) -> {
        }, () -> {
            select(mc, 4);
            camera(mc, CameraType.THIRD_PERSON_FRONT, false);
        }, 60));
        // Icons and tooltips.
        for (int i = 0; i < 3; i++) {
            int tip = i;
            out.add(new Beat("icons_tooltip_" + i, (p, c) -> {
            }, () -> {
                camera(mc, CameraType.FIRST_PERSON, true);
                mc.setScreen(new Showcase(tip));
            }, 25));
        }
        // The Hellforge with a shard waiting.
        out.add(new Beat("hellforge", (p, c) -> {
            p.setExperienceLevels(30);
            BlockPos at = BlockPos.containing(c).south(3);
            p.serverLevel().setBlockAndUpdate(at, AllBlocks.HELLFORGE.get().defaultBlockState());
            p.openMenu(AllBlocks.HELLFORGE.get().defaultBlockState().getMenuProvider(p.serverLevel(), at));
            if (p.containerMenu instanceof HellforgeMenu menu) {
                menu.container().setItem(HellforgeMenu.WEAPON, ascended(AllItems.ANGEL_BLADE.get(), 2));
                menu.container().setItem(HellforgeMenu.SHARD, new ItemStack(AllItems.ASCENSION_SHARD_3.get(), 2));
                menu.broadcastChanges();
            }
        }, () -> camera(mc, CameraType.FIRST_PERSON, true), 50));
        out.add(new Beat("hellforge_wrong", (p, c) -> {
            if (p.containerMenu instanceof HellforgeMenu menu) {
                menu.container().setItem(HellforgeMenu.WEAPON, ascended(AllItems.HUNTERS_JACKET.get(), 4));
                menu.broadcastChanges();
            }
        }, null));
        // Hunter's Gear on an armour stand, close.
        out.add(new Beat("stand_front", (p, c) -> {
            p.closeContainer();
            p.serverLevel().setBlockAndUpdate(BlockPos.containing(c).south(3), Blocks.AIR.defaultBlockState());
            ArmorStand stand = EntityType.ARMOR_STAND.create(p.serverLevel());
            stand.moveTo(c.x, c.y, c.z - 3, 0, 0);
            stand.setYBodyRot(0);
            stand.setYHeadRot(0);
            wear(stand, 2);
            p.serverLevel().addFreshEntity(stand);
            shown.add(stand);
            p.setGameMode(GameType.SPECTATOR);
            view(p, c.add(0, 0.2, -0.6), c.add(0, 1.0, -3));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("stand_back", (p, c) -> view(p, c.add(0, 0.2, -5.4), c.add(0, 1.0, -3)), null));
        out.add(new Beat("stand_side", (p, c) -> view(p, c.add(2.4, 0.2, -3), c.add(0, 1.0, -3)), null));
        // Worn by the player.
        out.add(new Beat("worn_front", (p, c) -> {
            clearShown();
            survival(p);
            wear(p, 4);
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 180, 0);
        }, () -> {
            select(mc, 6);
            camera(mc, CameraType.THIRD_PERSON_FRONT, false);
        }, 60));
        out.add(new Beat("worn_back", (p, c) -> {
        }, () -> camera(mc, CameraType.THIRD_PERSON_BACK, false)));
        // The hotbar and the inventory with their tier marks.
        out.add(new Beat("inventory", (p, c) -> hotbar(p), () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }));
        return out;
    }
}
