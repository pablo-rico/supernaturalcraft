package org.papiricoh.supernaturalcraft.hands;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Track the Hands (v0.42): a rite that draws an explorer's map to the nearest reliquary whose Hand the ritualist has not taken.
 * The search starts when the rite does ({@link HandLocator#request}) and runs a little each tick while it charges; at its end
 * whatever is left of it runs at once ({@link HandLocator#complete}). Refused (nothing spent) without a ritualist or once every
 * Hand is theirs; fails (nothing spent) if no reliquary lies within the search's reach.
 */
public record TrackHandsEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("track_the_hands");
    public static final MapCodec<TrackHandsEffect> CODEC = MapCodec.unit(new TrackHandsEffect());
    public static final int MAP_COLOR = 0xC8A24A;

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = refusal(ritualist);
        if (why == null) HandLocator.request(ritualist);
        return why;
    }

    public static @Nullable Component refusal(@Nullable ServerPlayer ritualist) {
        if (ritualist == null) return Component.translatable("message.supernaturalcraft.hands.track.nobody");
        if (Hands.claims(ritualist).unclaimed().isEmpty()) return Component.translatable("message.supernaturalcraft.hands.track.all");
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (refusal(ritualist) != null) return false;
        HandLocator.Located l = HandLocator.complete(ritualist);
        if (l == null) {
            ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.track.nothing").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ItemStack map = map(level.getServer().overworld(), l);
        if (!ritualist.addItem(map)) level.addFreshEntity(new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, map));
        ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.track.found",
                Component.translatable("item.supernaturalcraft." + l.hand().item)).withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    /** An explorer's map of the Overworld round {@code l}, its reliquary marked. */
    public static ItemStack map(ServerLevel overworld, HandLocator.Located l) {
        BlockPos target = l.pos();
        ItemStack map = MapItem.create(overworld, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(overworld, map);
        MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.supernaturalcraft.hand_map." + l.hand().reliquary));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }

    @Override
    public ItemStack displayResult() {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.supernaturalcraft.hand_map"));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }
}
