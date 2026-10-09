package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.CabinBuilder;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Decor;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Plan;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.Optional;

/**
 * Puts {@link BunkerLayout} into the world, touching only what lies inside the box it is given (a chunk's share in worldgen,
 * the whole plan for commands and tests): the hill over the way in, earth over the bunker's roof where the ground dips, every
 * block of the plan as it is (no block updates: the plan's shapes are final), then the decoration.
 */
public final class BunkerBuilder {

    /** Send to clients, no neighbour updates: stairs, bars and walls keep the plan's shapes. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private BunkerBuilder() {
    }

    /** A plan point in the world. */
    public static BlockPos at(BlockPos origin, int quarter, int[] local) {
        int[] w = BunkerLayout.toWorld(local, origin.getX(), origin.getY(), origin.getZ(), quarter);
        return new BlockPos(w[0], w[1], w[2]);
    }

    public static BoundingBox box(BlockPos origin, int quarter) {
        int[] b = BunkerLayout.box(origin.getX(), origin.getY(), origin.getZ(), quarter);
        return new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /** The door's ground at column ({@code x}, {@code z}). */
    public static BlockPos originOn(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    /** Builds the whole bunker now (commands, older worlds, tests), clearing the decoration of an earlier build first. */
    public static void placeAt(ServerLevel level, BlockPos origin, int quarter) {
        BoundingBox box = box(origin, quarter);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        AABB area = AABB.of(box);
        for (Entity e : level.getEntitiesOfClass(Entity.class, area, e -> e instanceof HangingEntity || e instanceof ArmorStand)) e.discard();
        build(level, box, origin, quarter);
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, int quarter) {
        Rotation rot = CabinBuilder.rotation(quarter);
        int[] local = localRange(box, origin, quarter);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // First the ground (the hill, the earth over the roof), then the plan over it: the plan's blocks are final.
        for (int pass = 0; pass < 2; pass++) {
            for (int lx = local[0]; lx <= local[1]; lx++) {
                for (int lz = local[2]; lz <= local[3]; lz++) {
                    int[] w = BunkerLayout.toWorld(new int[]{lx, 0, lz}, origin.getX(), origin.getY(), origin.getZ(), quarter);
                    if (w[0] < box.minX() || w[0] > box.maxX() || w[2] < box.minZ() || w[2] > box.maxZ()) continue;
                    if (pass == 0) {
                        int hill = BunkerLayout.hill(lx, lz);
                        if (hill > 0) raiseHill(level, box, p, w[0], w[2], origin.getY(), hill, lx, lz);
                        int top = BunkerLayout.columnTop(lx, lz);
                        if (top != Integer.MIN_VALUE && top < 0) cover(level, box, p, w[0], w[2], origin.getY(), top);
                        continue;
                    }
                    final int wx = w[0], wz = w[2];
                    BunkerLayout.column(lx, lz, (y, state) -> {
                        p.set(wx, origin.getY() + y, wz);
                        if (box.isInside(p)) level.setBlock(p, CabinBuilder.state(state).rotate(level, p, rot), FLAGS);
                    });
                }
            }
        }
        RandomSource random = RandomSource.create(origin.asLong() ^ box.minX() * 31L ^ box.minZ() * 17L);
        for (Decor d : BunkerLayout.decor()) {
            BlockPos at = at(origin, quarter, new int[]{d.x(), d.y(), d.z()});
            if (!box.isInside(at)) continue;
            try {
                decorate(level, at, d, quarter, random);
            } catch (RuntimeException e) {
                SupernaturalCraft.LOGGER.warn("Bunker decoration {} failed: {}", d, e.toString());
            }
        }
        // The dungeon's devil's trap: its parts are world-aligned, so it is painted after the turn.
        BlockPos trap = at(origin, quarter, BunkerLayout.TRAP);
        for (int part = 0; part < 9; part++) {
            p.set(trap.getX() + DevilsTrapBlock.dx(part), trap.getY(), trap.getZ() + DevilsTrapBlock.dz(part));
            if (box.isInside(p)) level.setBlock(p, AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part), FLAGS);
        }
    }

    /** The local x/z range a world box covers (clamped to the plan). */
    private static int[] localRange(BoundingBox box, BlockPos origin, int quarter) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] c : new int[][]{{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
            int[] l = BunkerLayout.rotate(c[0] - origin.getX(), c[1] - origin.getZ(), -quarter);
            minX = Math.min(minX, l[0]);
            maxX = Math.max(maxX, l[0]);
            minZ = Math.min(minZ, l[1]);
            maxZ = Math.max(maxZ, l[1]);
        }
        return new int[]{Math.max(minX, BunkerLayout.MIN_X), Math.min(maxX, BunkerLayout.MAX_X), Math.max(minZ, BunkerLayout.MIN_Z),
                Math.min(maxZ, BunkerLayout.MAX_Z)};
    }

    // --- the hill and the cover ------------------------------------------------------------------------------------------------

    private static boolean soft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || !s.getFluidState().isEmpty()
                || s.is(BlockTags.FLOWERS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.SAPLINGS);
    }

    /** Earth from the real ground up to {@code hill} over the door's ground, trees cleared, turf and a few stones on top. */
    private static void raiseHill(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos p, int x, int z, int oy, int hill, int lx, int lz) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        p.set(x, ground, z);
        while (ground > oy - 8 && soft(level.getBlockState(p))) p.set(x, --ground, z);
        int top = oy + hill;
        for (int y = ground + 1; y <= oy + BunkerLayout.CLEAR_TO; y++) {
            p.set(x, y, z);
            if (!box.isInside(p)) continue;
            BlockState s = level.getBlockState(p);
            if (y <= top) {
                if (!soft(s)) continue;
                double n = Plan.noise(lx, y, lz, 41);
                BlockState earth = y < top ? (y < top - 3 && n < 0.3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState())
                        : n < 0.06 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : n < 0.1 ? Blocks.ANDESITE.defaultBlockState()
                        : n < 0.16 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(p, earth, FLAGS);
            } else if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || y == top + 1 && !s.isAir() && soft(s)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        p.set(x, top + 1, z);
        if (box.isInside(p) && level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)) {
            double n = Plan.noise(lx, 0, lz, 43);
            BlockState plant = n < 0.3 ? Blocks.SHORT_GRASS.defaultBlockState() : n < 0.36 ? Blocks.FERN.defaultBlockState()
                    : n < 0.39 ? Blocks.OXEYE_DAISY.defaultBlockState() : n < 0.41 ? Blocks.CORNFLOWER.defaultBlockState() : null;
            if (plant != null) level.setBlock(p, plant, FLAGS);
        }
    }

    /** Earth over the bunker where the ground dips below the door's: no roof shows. */
    private static void cover(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos p, int x, int z, int oy, int top) {
        for (int y = top + 1; y <= 0; y++) {
            p.set(x, oy + y, z);
            if (!box.isInside(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty()) continue;
            level.setBlock(p, (y == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), FLAGS);
        }
    }

    // --- decoration -------------------------------------------------------------------------------------------------------------

    private static Direction dir(String planDir, int quarter) {
        String d = BunkerLayout.rotate(planDir, quarter);
        return d.isEmpty() ? Direction.NORTH : Direction.byName(d);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static net.minecraft.nbt.Tag save(ItemStack stack, WorldGenLevel level) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(level.registryAccess());
    }

    private static void decorate(WorldGenLevel level, BlockPos at, Decor d, int quarter, RandomSource random) {
        ServerLevel server = level.getLevel();
        switch (d.kind()) {
            case LOOT -> RandomizableContainer.setBlockEntityLootTable(level, random, at,
                    ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(d.data())));
            case BANNER -> {
                if (level.getBlockEntity(at) instanceof BannerBlockEntity be) {
                    var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
                    BannerPatternLayers.Builder layers = new BannerPatternLayers.Builder();
                    for (String layer : d.extra().split(",")) {
                        if (layer.isBlank()) continue;
                        int c = layer.lastIndexOf(':');
                        Optional<Holder.Reference<BannerPattern>> pattern = patterns.get(ResourceKey.create(Registries.BANNER_PATTERN,
                                ResourceLocation.parse(layer.substring(0, c))));
                        pattern.ifPresent(h -> layers.add(h, DyeColor.byName(layer.substring(c + 1), DyeColor.WHITE)));
                    }
                    ItemStack stack = new ItemStack(Items.WHITE_BANNER);
                    stack.set(DataComponents.BANNER_PATTERNS, layers.build());
                    be.fromItem(stack, DyeColor.byName(d.data(), DyeColor.BLACK));
                }
            }
            case SIGN -> {
                if (level.getBlockEntity(at) instanceof SignBlockEntity be) {
                    // Through NBT, as structure templates do: setText would notify a level the block entity doesn't have yet.
                    SignText text = new SignText();
                    String[] lines = d.data().split("\\|", -1);
                    for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
                    if (!d.extra().isEmpty()) text = text.setColor(DyeColor.byName(d.extra(), DyeColor.BLACK)).setHasGlowingText(true);
                    CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                    SignText.DIRECT_CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text)
                            .ifSuccess(t -> tag.put("front_text", t));
                    tag.putBoolean("is_waxed", true);
                    be.loadWithComponents(tag, level.registryAccess());
                }
            }
            case VAULT -> {
                if (level.getBlockEntity(at) instanceof VaultBlockEntity be) {
                    ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(d.data()));
                    ItemStack key = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(d.extra())));
                    be.setConfig(new VaultConfig(table, 4.0, 4.5, key, Optional.empty()));
                }
            }
            // Entities are filled in through NBT (as structure templates do): their setters would reach into the live level from
            // the worldgen thread and wait on the very chunk being generated.
            case ITEM_FRAME -> {
                Direction facing = dir(d.facing(), quarter);
                String[] item = d.data().split("\\|");
                ItemFrame frame = d.extra().equals("glow") ? new GlowItemFrame(server, at, facing) : new ItemFrame(server, at, facing);
                CompoundTag tag = frame.saveWithoutId(new CompoundTag());
                tag.put("Item", stack(item[0]).save(level.registryAccess()));
                tag.putByte("ItemRotation", (byte) (item.length > 1 ? Integer.parseInt(item[1]) : 0));
                frame.load(tag);
                level.addFreshEntity(frame);
            }
            case PAINTING -> {
                Direction facing = dir(d.facing(), quarter);
                Optional<Holder.Reference<PaintingVariant>> variant = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT)
                        .get(ResourceKey.create(Registries.PAINTING_VARIANT, ResourceLocation.withDefaultNamespace(d.data())));
                variant.ifPresent(v -> level.addFreshEntity(new Painting(server, at, facing, v)));
            }
            case ARMOR_STAND -> {
                ArmorStand stand = new ArmorStand(server, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                float yaw = dir(d.facing(), quarter).toYRot();
                stand.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
                stand.setYHeadRot(yaw);
                stand.setYBodyRot(yaw);
                String[] items = d.data().split(",");
                ItemStack[] gear = new ItemStack[5];
                for (int i = 0; i < 5; i++) {
                    String id = i < items.length ? items[i] : "-";
                    gear[i] = id.equals("-") ? ItemStack.EMPTY : stack(id);
                    if (!d.extra().isEmpty() && id.contains("leather")) gear[i].set(DataComponents.DYED_COLOR, new DyedItemColor(Integer.parseInt(d.extra(), 16), true));
                }
                CompoundTag tag = stand.saveWithoutId(new CompoundTag());
                ListTag armor = new ListTag(), hands = new ListTag();
                for (int i : new int[]{3, 2, 1, 0}) armor.add(save(gear[i], level));
                hands.add(save(gear[4], level));
                hands.add(new CompoundTag());
                tag.put("ArmorItems", armor);
                tag.put("HandItems", hands);
                tag.putBoolean("ShowArms", true);
                stand.load(tag);
                level.addFreshEntity(stand);
            }
        }
    }
}
