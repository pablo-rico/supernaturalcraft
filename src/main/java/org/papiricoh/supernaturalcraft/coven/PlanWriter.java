package org.papiricoh.supernaturalcraft.coven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * Writes a finished {@link LayoutPlan} into the world round an origin (v0.21: the church, the coven, Rowena's house), touching
 * only what lies inside the box it is given (a chunk's share at worldgen, everything for commands and older worlds). Decor it
 * understands: {@code loot}, {@code sign}, {@code item_frame}, {@code glow_item_frame}, {@code armor_stand} (others are skipped).
 */
public final class PlanWriter {

    private PlanWriter() {
    }

    /** Everything the plan touches round {@code origin}, at least {@code height} above it. */
    public static BoundingBox fullBox(BlockPos origin, LayoutPlan plan, int height) {
        int minX = -1, minY = -1, minZ = -1, maxX = 1, maxY = height, maxZ = 1;
        for (ArenaCell c : plan.cells()) {
            minX = Math.min(minX, c.dx());
            maxX = Math.max(maxX, c.dx());
            minY = Math.min(minY, c.dy());
            maxY = Math.max(maxY, c.dy());
            minZ = Math.min(minZ, c.dz());
            maxZ = Math.max(maxZ, c.dz());
        }
        for (LayoutDecor d : plan.decor()) {
            minX = Math.min(minX, (int) Math.floor(d.x()));
            maxX = Math.max(maxX, (int) Math.floor(d.x()));
            minY = Math.min(minY, (int) Math.floor(d.y()));
            maxY = Math.max(maxY, (int) Math.floor(d.y()));
            minZ = Math.min(minZ, (int) Math.floor(d.z()));
            maxZ = Math.max(maxZ, (int) Math.floor(d.z()));
        }
        return new BoundingBox(origin.getX() + minX, origin.getY() + minY, origin.getZ() + minZ,
                origin.getX() + maxX, origin.getY() + maxY, origin.getZ() + maxZ);
    }

    /** A structure piece's box: the plan's footprint from the first air layer up (a thin beard levels the ground to it). */
    public static BoundingBox pieceBox(BlockPos origin, LayoutPlan plan, int height) {
        BoundingBox full = fullBox(origin, plan, height);
        return new BoundingBox(full.minX(), origin.getY(), full.minZ(), full.maxX(), Math.max(origin.getY(), full.maxY()), full.maxZ());
    }

    /** Loads every chunk of {@code box} (commands, previews, older worlds). */
    public static void load(ServerLevel level, BoundingBox box) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, LayoutPlan plan) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (ArenaCell cell : plan.cells()) {
            p.set(origin.getX() + cell.dx(), origin.getY() + cell.dy(), origin.getZ() + cell.dz());
            if (box.isInside(p)) level.setBlock(p, HorsemenGround.state(cell.block()), Block.UPDATE_CLIENTS);
        }
        for (LayoutDecor d : plan.decor()) decor(level, box, origin, d);
    }

    /** Puts one block (inside {@code box}). */
    public static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    private static void decor(WorldGenLevel level, BoundingBox box, BlockPos origin, LayoutDecor d) {
        BlockPos at = origin.offset((int) Math.floor(d.x()), (int) Math.floor(d.y()), (int) Math.floor(d.z()));
        if (!box.isInside(at)) return;
        Direction facing = d.facing() == null || d.facing().isEmpty() ? Direction.NORTH : Direction.byName(d.facing());
        if (facing == null) facing = Direction.NORTH;
        ServerLevel server = level.getLevel();
        switch (d.kind()) {
            case "loot" -> {
                ResourceLocation table = ResourceLocation.tryParse(d.data());
                if (table != null && level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity be) {
                    be.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, table), at.asLong() ^ origin.asLong());
                }
            }
            case "sign" -> {
                if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
                    String[] lines = d.data().split("\\|", -1);
                    SignText text = new SignText();
                    for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
                    sign.setText(text, true);
                    sign.setText(text, false);
                }
            }
            case "item_frame", "glow_item_frame" -> {
                ItemFrame frame = d.kind().equals("item_frame") ? new ItemFrame(server, at, facing) : new GlowItemFrame(server, at, facing);
                frame.setItem(stack(d.data()), false);
                level.addFreshEntity(frame);
            }
            case "armor_stand" -> {
                ArmorStand stand = new ArmorStand(server, d.x() + origin.getX(), d.y() + origin.getY(), d.z() + origin.getZ());
                stand.setYRot(facing.toYRot());
                String[] items = d.data().split(",", -1);
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
                for (int i = 0; i < Math.min(items.length, slots.length); i++) stand.setItemSlot(slots[i], stack(items[i]));
                level.addFreshEntity(stand);
            }
            default -> {
            }
        }
    }

    private static ItemStack stack(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null || id.isBlank()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
