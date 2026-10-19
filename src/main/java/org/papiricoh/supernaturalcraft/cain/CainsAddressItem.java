package org.papiricoh.supernaturalcraft.cain;

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
 * Cain's address (v0.21): the note Rowena scribbles once Lucifer is back in his box ({@code main/rowenas_clue}). It keeps his
 * farm's gate ({@code compass_target}, the Letters compass's component, so the compass can follow it too); used in the
 * Overworld it tells which way and how far.
 */
public class CainsAddressItem extends Item {

    public static final String PIN_NAME = "Cain";

    public CainsAddressItem(Properties properties) {
        super(properties);
    }

    /** A note pointing at his farm in {@code level}'s world. */
    public static ItemStack pointed(ServerLevel level) {
        ItemStack note = new ItemStack(AllItems.CAINS_ADDRESS.get());
        point(note, level);
        return note;
    }

    public static void point(ItemStack note, ServerLevel level) {
        BlockPos gate = CainWorld.address(level.getServer().overworld());
        note.set(AllDataComponents.COMPASS_TARGET.get(), new TablePin(PIN_NAME, gate.getX(), gate.getZ()));
    }

    public static @Nullable TablePin target(ItemStack stack) {
        return stack.get(AllDataComponents.COMPASS_TARGET.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (level.dimension() != Level.OVERWORLD) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.cains_address.elsewhere").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.consume(stack);
        }
        if (target(stack) == null) point(stack, sp.serverLevel());
        TablePin pin = target(stack);
        double dx = pin.x() + 0.5 - sp.getX(), dz = pin.z() + 0.5 - sp.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        String key = dist < 24 ? "message.supernaturalcraft.cains_address.here" : "message.supernaturalcraft.cains_address.heading";
        sp.displayClientMessage(Component.translatable(key, dist,
                Component.translatable("screen.supernaturalcraft.case.dir." + compass(dx, dz))).withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.0f);
        return InteractionResultHolder.consume(stack);
    }

    /** The compass point ({@code north}, {@code north_east}, …) of an offset. */
    static String compass(double dx, double dz) {
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
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.cains_address").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.cains_address.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
