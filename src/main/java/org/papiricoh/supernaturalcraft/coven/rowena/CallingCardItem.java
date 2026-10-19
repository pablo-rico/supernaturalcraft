package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Rowena's calling card (v0.21): an embossed card a coven witch carried, in green ink that runs toward its owner's door. It
 * keeps her house's column ({@code compass_target}, the Letters compass's component) from the moment it is found
 * ({@link #pointed}); used in the Overworld it tells which way and how far; anywhere else the ink only pools.
 */
public class CallingCardItem extends Item {

    public static final String PIN_NAME = "Rowena";

    public CallingCardItem(Properties properties) {
        super(properties);
    }

    /** A card pointing at her house in {@code level}'s world. */
    public static ItemStack pointed(ServerLevel level) {
        ItemStack card = new ItemStack(AllItems.ROWENAS_CALLING_CARD.get());
        point(card, level);
        return card;
    }

    /** Sets where the card points (her house, as the world places it). */
    public static void point(ItemStack card, ServerLevel level) {
        BlockPos door = RowenaWorld.home(RowenaSite.of(level));
        card.set(AllDataComponents.COMPASS_TARGET.get(), new TablePin(PIN_NAME, door.getX(), door.getZ()));
    }

    public static @Nullable TablePin target(ItemStack stack) {
        return stack.get(AllDataComponents.COMPASS_TARGET.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (level.dimension() != Level.OVERWORLD) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.calling_card.pools").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.consume(stack);
        }
        if (target(stack) == null) point(stack, sp.serverLevel());
        TablePin pin = target(stack);
        double dx = pin.x() + 0.5 - sp.getX(), dz = pin.z() + 0.5 - sp.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        String key = dist < 24 ? "message.supernaturalcraft.calling_card.here" : "message.supernaturalcraft.calling_card.heading";
        sp.displayClientMessage(Component.translatable(key, dist,
                Component.translatable("screen.supernaturalcraft.case.dir." + point(dx, dz))).withStyle(ChatFormatting.DARK_GREEN), true);
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.2f);
        return InteractionResultHolder.consume(stack);
    }

    /** The compass point ({@code north}, {@code north_east}, …) of an offset. */
    static String point(double dx, double dz) {
        String[] points = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        double bearing = (Math.toDegrees(Math.atan2(dx, -dz)) + 360) % 360;
        return points[(int) Math.floor((bearing + 22.5) / 45) % 8];
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return target(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.rowenas_calling_card").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.rowenas_calling_card.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
