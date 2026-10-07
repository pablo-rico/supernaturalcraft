package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.Optional;

/**
 * Draws an explorer's map to the nearest structure in a tag (the Hymnal Map, to the nearest
 * Hymnal Spire). If none lies within {@code radius} placement cells, the ritual fails and
 * nothing is consumed.
 */
public record LocateStructureEffect(TagKey<Structure> structure, ResourceLocation decoration, String name, int color, int radius)
        implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("locate_structure");
    public static final MapCodec<LocateStructureEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TagKey.codec(Registries.STRUCTURE).fieldOf("structure").forGetter(LocateStructureEffect::structure),
            ResourceLocation.CODEC.fieldOf("decoration").forGetter(LocateStructureEffect::decoration),
            Codec.STRING.fieldOf("name").forGetter(LocateStructureEffect::name),
            Codec.INT.optionalFieldOf("color", 0xE8C25A).forGetter(LocateStructureEffect::color),
            Codec.intRange(1, 32).optionalFieldOf("radius", 6).forGetter(LocateStructureEffect::radius)
    ).apply(i, LocateStructureEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Optional<HolderSet.Named<Structure>> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(structure);
        @Nullable Pair<BlockPos, Holder<Structure>> found = set.isEmpty() ? null
                : level.getChunkSource().getGenerator().findNearestMapStructure(level, set.get(), altar, radius, false);
        if (found == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.locate.nothing").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        // The search answers with the corner of the start chunk; the map should point at the heart of it.
        BlockPos target = found.getFirst();
        StructureStart start = level.getChunk(target.getX() >> 4, target.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStartForStructure(found.getSecond().value());
        if (start != null && start.isValid()) target = start.getBoundingBox().getCenter();
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        Holder<MapDecorationType> icon = BuiltInRegistries.MAP_DECORATION_TYPE.getHolder(decoration).map(h -> (Holder<MapDecorationType>) h)
                .orElse(net.minecraft.world.level.saveddata.maps.MapDecorationTypes.TARGET_X);
        MapItemSavedData.addTargetDecoration(map, target, "+", icon);
        map.set(DataComponents.ITEM_NAME, Component.translatable(name));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(color));
        if (ritualist != null && ritualist.addItem(map)) return true;
        level.addFreshEntity(new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, map));
        return true;
    }

    @Override
    public ItemStack displayResult() {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.ITEM_NAME, Component.translatable(name));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(color));
        return map;
    }
}
