package org.papiricoh.supernaturalcraft.heaven.plot;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Writes one {@link LayoutDecor} (v0.18): a container's loot table, a sign's lines, a banner's patterns, a lectern's book, a
 * pot's sherds, or a decorative entity (frames, paintings, armour stands, displays). Entities are tagged {@link #TAG} so a plot
 * written again can clear the old ones first. Anything that cannot be understood is logged and skipped, never thrown.
 * <p>Data formats beyond {@link LayoutDecor}'s javadoc: {@code banner_pattern} = {@code pattern=colour} pairs joined by
 * {@code ;} ({@code minecraft:stripe_top=red;minecraft:border=black}); {@code sign} lines starting with {@code @} are translation
 * keys; {@code pot} = up to four item ids joined by {@code ,} (back, left, right, front). Positions: block-based kinds use the
 * block at the floor of (x, y, z); an armour stand at whole coordinates stands in the middle of that block.
 */
public final class DecorWriter {

    private static final Logger LOG = LogUtils.getLogger();
    /** Every decorative entity a plot writer spawns carries this tag. */
    public static final String TAG = "supernaturalcraft.plot_decor";

    private DecorWriter() {
    }

    /** @return whether it was placed */
    public static boolean place(ServerLevel level, BlockPos origin, LayoutDecor d) {
        try {
            return switch (d.kind()) {
                case "loot" -> loot(level, block(origin, d), d.data());
                case "item_frame", "glow_item_frame" -> frame(level, block(origin, d), d);
                case "painting" -> painting(level, block(origin, d), d);
                case "armor_stand" -> armorStand(level, origin, d);
                case "banner_pattern" -> banner(level, block(origin, d), d.data());
                case "block_display" -> display(level, origin, d, true);
                case "item_display" -> display(level, origin, d, false);
                case "sign" -> sign(level, block(origin, d), d.data());
                case "lectern_book" -> lectern(level, block(origin, d), d.data());
                case "pot" -> pot(level, block(origin, d), d.data());
                default -> {
                    LOG.warn("Heaven layout: unknown decor kind '{}'", d.kind());
                    yield false;
                }
            };
        } catch (RuntimeException e) {
            LOG.warn("Heaven layout: could not place {} at {}: {}", d.kind(), block(origin, d), e.toString());
            return false;
        }
    }

    /** Discards the decorative entities a writer left inside {@code box} (loaded chunks only). */
    public static int clear(ServerLevel level, AABB box) {
        List<Entity> found = level.getEntitiesOfClass(Entity.class, box, e -> e.getTags().contains(TAG));
        found.forEach(Entity::discard);
        return found.size();
    }

    private static BlockPos block(BlockPos origin, LayoutDecor d) {
        return origin.offset((int) Math.floor(d.x()), (int) Math.floor(d.y()), (int) Math.floor(d.z()));
    }

    private static @Nullable Direction facing(LayoutDecor d, Direction fallback) {
        if (d.facing() == null || d.facing().isEmpty()) return fallback;
        Direction dir = Direction.byName(d.facing());
        return dir == null ? fallback : dir;
    }

    private static ItemStack item(String id) {
        if (id == null || id.isBlank()) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            LOG.warn("Heaven layout: unknown item '{}'", id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(rl));
    }

    private static void spawn(ServerLevel level, Entity e) {
        e.addTag(TAG);
        e.setSilent(true);
        level.addFreshEntity(e);
    }

    // --- block entities ---------------------------------------------------------------------------------------------------

    private static boolean loot(ServerLevel level, BlockPos pos, String table) {
        if (!(level.getBlockEntity(pos) instanceof RandomizableContainer c)) return false;
        c.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)), pos.asLong() * 31 + level.getSeed());
        return true;
    }

    private static boolean sign(ServerLevel level, BlockPos pos, String data) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;
        String[] lines = data.split("\\|", -1);
        SignText text = new SignText();
        for (int i = 0; i < Math.min(4, lines.length); i++) {
            String l = lines[i];
            text = text.setMessage(i, l.startsWith("@") ? Component.translatable(l.substring(1)) : Component.literal(l));
        }
        sign.setText(text, true);
        sign.setWaxed(true);
        sign.setChanged();
        level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
        return true;
    }

    private static boolean banner(ServerLevel level, BlockPos pos, String data) {
        if (!(level.getBlockEntity(pos) instanceof BannerBlockEntity banner)) return false;
        BlockState state = level.getBlockState(pos);
        DyeColor base = state.getBlock() instanceof AbstractBannerBlock b ? b.getColor() : DyeColor.WHITE;
        BannerPatternLayers.Builder layers = new BannerPatternLayers.Builder();
        var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        for (String pair : data.split(";")) {
            int eq = pair.lastIndexOf('=');
            if (eq <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(pair.substring(0, eq).trim());
            DyeColor colour = DyeColor.byName(pair.substring(eq + 1).trim(), null);
            if (id == null || colour == null) continue;
            Optional<Holder.Reference<BannerPattern>> p = patterns.get(ResourceKey.create(Registries.BANNER_PATTERN, id));
            p.ifPresent(h -> layers.add(h, colour));
        }
        ItemStack stack = new ItemStack(Items.WHITE_BANNER);
        stack.set(DataComponents.BANNER_PATTERNS, layers.build());
        banner.fromItem(stack, base);
        banner.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }

    private static boolean lectern(ServerLevel level, BlockPos pos, String key) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LecternBlock) || state.getValue(LecternBlock.HAS_BOOK)) return false;
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Memories"), "", 0,
                List.of(Filterable.passThrough(Component.translatable(key))), true));
        return LecternBlock.tryPlaceBook(null, level, pos, state, book);
    }

    private static boolean pot(ServerLevel level, BlockPos pos, String data) {
        if (!(level.getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot)) return false;
        List<Optional<Item>> sides = new ArrayList<>();
        for (String id : data.split(",")) {
            ItemStack s = item(id);
            sides.add(s.isEmpty() ? Optional.empty() : Optional.of(s.getItem()));
        }
        while (sides.size() < 4) sides.add(Optional.empty());
        ItemStack stack = new ItemStack(Items.DECORATED_POT);
        stack.set(DataComponents.POT_DECORATIONS, new PotDecorations(sides.get(0), sides.get(1), sides.get(2), sides.get(3)));
        pot.setFromItem(stack);
        pot.setChanged();
        level.sendBlockUpdated(pos, pot.getBlockState(), pot.getBlockState(), 3);
        return true;
    }

    // --- entities ---------------------------------------------------------------------------------------------------------

    private static boolean frame(ServerLevel level, BlockPos pos, LayoutDecor d) {
        Direction dir = facing(d, Direction.SOUTH);
        ItemFrame frame = d.kind().equals("glow_item_frame") ? new GlowItemFrame(level, pos, dir) : new ItemFrame(level, pos, dir);
        frame.setItem(item(d.data()), false);
        // A fixed frame never pops off its wall and is not knocked down (the plot's protection does the rest).
        CompoundTag t = new CompoundTag();
        frame.saveWithoutId(t);
        t.putBoolean("Fixed", true);
        frame.load(t);
        spawn(level, frame);
        return true;
    }

    private static boolean painting(ServerLevel level, BlockPos pos, LayoutDecor d) {
        Direction dir = facing(d, Direction.SOUTH);
        ResourceLocation id = ResourceLocation.tryParse(d.data());
        if (id == null) return false;
        Optional<Holder.Reference<PaintingVariant>> variant = level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT)
                .getHolder(ResourceKey.create(Registries.PAINTING_VARIANT, id));
        if (variant.isEmpty()) {
            LOG.warn("Heaven layout: unknown painting '{}'", d.data());
            return false;
        }
        Painting painting = new Painting(level, pos, dir, variant.get());
        if (!painting.survives()) return false;
        spawn(level, painting);
        return true;
    }

    private static boolean armorStand(ServerLevel level, BlockPos origin, LayoutDecor d) {
        double x = origin.getX() + d.x() + (d.x() == Math.floor(d.x()) ? 0.5 : 0);
        double z = origin.getZ() + d.z() + (d.z() == Math.floor(d.z()) ? 0.5 : 0);
        ArmorStand stand = new ArmorStand(level, x, origin.getY() + d.y(), z);
        Direction dir = facing(d, Direction.SOUTH);
        stand.setYRot(dir.toYRot());
        stand.setYBodyRot(dir.toYRot());
        stand.setYHeadRot(dir.toYRot());
        String[] items = d.data().split(",", -1);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        for (int i = 0; i < Math.min(items.length, slots.length); i++) {
            ItemStack s = item(items[i]);
            if (!s.isEmpty()) stand.setItemSlot(slots[i], s);
        }
        if (items.length > 4 && !items[4].isBlank()) stand.setShowArms(true);
        spawn(level, stand);
        return true;
    }

    private static boolean display(ServerLevel level, BlockPos origin, LayoutDecor d, boolean block) {
        CompoundTag t = new CompoundTag();
        if (block) {
            t.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BLOCK_DISPLAY).toString());
            BlockState state = PlotWriter.parse(level, d.data());
            if (state == null) return false;
            t.put("block_state", NbtUtils.writeBlockState(state));
        } else {
            t.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ITEM_DISPLAY).toString());
            ItemStack s = item(d.data());
            if (s.isEmpty()) return false;
            t.put("item", s.save(level.registryAccess()));
        }
        CompoundTag transform = new CompoundTag();
        transform.put("left_rotation", floats(0, 0, 0, 1));
        transform.put("right_rotation", floats(0, 0, 0, 1));
        transform.put("translation", floats(0, 0, 0));
        transform.put("scale", floats(d.scale(), d.scale(), d.scale()));
        t.put("transformation", transform);
        Optional<Entity> made = EntityType.create(t, level);
        if (made.isEmpty()) return false;
        Entity e = made.get();
        Direction dir = facing(d, Direction.SOUTH);
        e.moveTo(origin.getX() + d.x(), origin.getY() + d.y(), origin.getZ() + d.z(), dir.toYRot(), 0);
        spawn(level, e);
        return true;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) list.add(FloatTag.valueOf(v));
        return list;
    }

    /** For tests: whether {@code pos} holds a container whose loot table is {@code table}. */
    public static boolean hasLoot(ServerLevel level, BlockPos pos, ResourceKey<LootTable> table) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof RandomizableContainer c && table.equals(c.getLootTable());
    }
}
