package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * The Letters compass (v0.19): a brass compass of the order's. Its needle points where its holder set it at a war table (a pin,
 * kept on the compass) or, with nothing set, at their open case; it spins for an outsider, with no case, and outside the
 * Overworld. Use it to hear what it points at and how far; sneak-use it to let a set place go (back to the case).
 */
public class LettersCompassItem extends Item {

    /** Where a compass points, its label and its column. */
    public record Heading(Component label, int x, int z) {
    }

    public LettersCompassItem(Properties properties) {
        super(properties);
    }

    /** The place set on this compass at a war table, or null (it follows the open case). */
    public static @Nullable TablePin target(ItemStack stack) {
        return stack.get(AllDataComponents.COMPASS_TARGET.get());
    }

    /** Where the compass points for a hunter with this record, or null (it spins). */
    public static @Nullable Heading heading(ItemStack stack, Legacy legacy) {
        if (!legacy.member()) return null;
        TablePin pin = target(stack);
        if (pin != null) return new Heading(Component.literal(pin.name()), pin.x(), pin.z());
        CaseFile open = CaseOffice.open(legacy);
        if (open == null) return null;
        return new Heading(Component.translatable("legacy.supernaturalcraft.case.scenario." + open.scenario()), open.site().getX(), open.site().getZ());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (sp.isShiftKeyDown() && target(stack) != null) {
            stack.remove(AllDataComponents.COMPASS_TARGET.get());
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.cleared").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, sp.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.6f, 0.8f);
            return InteractionResultHolder.consume(stack);
        }
        Heading h = level.dimension() == Level.OVERWORLD ? heading(stack, Legacies.get(sp)) : null;
        if (h == null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.spins").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.consume(stack);
        }
        double dx = h.x() + 0.5 - sp.getX(), dz = h.z() + 0.5 - sp.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.compass.heading", h.label(), dist,
                Component.translatable("screen.supernaturalcraft.case.dir." + point(dx, dz))).withStyle(ChatFormatting.GOLD), true);
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
        TablePin pin = target(stack);
        if (pin != null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.letters_compass.pin", pin.name(), pin.x(), pin.z()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.letters_compass.clear").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.letters_compass.case").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.letters_compass").withStyle(ChatFormatting.DARK_GRAY));
    }
}
