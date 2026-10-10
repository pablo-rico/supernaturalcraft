package org.papiricoh.supernaturalcraft.crossroads.wild;

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/**
 * Puts a natural crossroads ({@link CrossroadsLayout#plan}) into the world round an origin (the crossing, {@code y} = the first
 * air layer above the ground), touching only what lies inside the box it is given, then sets the {@code crossroads_soil} at
 * {@link CrossroadsLayout#CENTRE}. Decor it understands: {@code loot}, {@code sign}, {@code item_frame},
 * {@code glow_item_frame} and {@code armor_stand} (others are skipped).
 */
public final class CrossroadsBuilder {

    private CrossroadsBuilder() {
    }

    /** Which way the main road runs (0-3), from the crossroads' seed. */
    public static int dirOf(long seed) {
        return (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
    }

    public static LayoutPlan plan(long seed) {
        return CrossroadsLayout.plan(seed, dirOf(seed));
    }

    /**
     * Everything the plan touches, round {@code origin}: its cells, decor and centre (at least a block either way, the full
     * {@link CrossroadsLayout#HEIGHT} above).
     */
    public static BoundingBox fullBox(BlockPos origin, LayoutPlan plan) {
        LayoutPoint c = CrossroadsLayout.CENTRE;
        int minX = c.x() - 1, minY = c.y() - 1, minZ = c.z() - 1, maxX = c.x() + 1, maxY = CrossroadsLayout.HEIGHT, maxZ = c.z() + 1;
        for (ArenaCell cell : plan.cells()) {
            minX = Math.min(minX, cell.dx());
            maxX = Math.max(maxX, cell.dx());
            minY = Math.min(minY, cell.dy());
            maxY = Math.max(maxY, cell.dy());
            minZ = Math.min(minZ, cell.dz());
            maxZ = Math.max(maxZ, cell.dz());
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

    /**
     * The structure piece's box: the plan's footprint, from the first air layer up. A thin beard levels the terrain to its
     * floor; what lies below it (the roads, the soil) is still written, within the same chunks.
     */
    public static BoundingBox pieceBox(BlockPos origin, LayoutPlan plan) {
        BoundingBox full = fullBox(origin, plan);
        return new BoundingBox(full.minX(), origin.getY(), full.minZ(), full.maxX(), Math.max(origin.getY(), full.maxY()), full.maxZ());
    }

    /** Builds a crossroads on the ground at {@code near} (commands, previews and tests). @return its origin */
    public static BlockPos placeDirect(ServerLevel level, BlockPos near, long seed) {
        LayoutPlan plan = plan(seed);
        BoundingBox reach = fullBox(near, plan);
        for (int cx = reach.minX() >> 4; cx <= reach.maxX() >> 4; cx++) {
            for (int cz = reach.minZ() >> 4; cz <= reach.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ()) - 1;
        BlockPos origin = new BlockPos(near.getX(), ground + 1, near.getZ());
        build(level, fullBox(origin, plan), origin, plan);
        return origin;
    }

    /** Where the soil lies for a crossroads at {@code origin}. */
    public static BlockPos centre(BlockPos origin) {
        LayoutPoint c = CrossroadsLayout.CENTRE;
        return origin.offset(c.x(), c.y(), c.z());
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, LayoutPlan plan) {
        for (ArenaCell cell : plan.cells()) {
            put(level, box, origin.offset(cell.dx(), cell.dy(), cell.dz()), HorsemenGround.state(cell.block()));
        }
        put(level, box, centre(origin), AllBlocks.CROSSROADS_SOIL.get().defaultBlockState());
        for (LayoutDecor d : plan.decor()) decor(level, box, origin, d);
    }

    private static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
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
