package org.papiricoh.supernaturalcraft.heaven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;

/**
 * Who may change what in Heaven (v0.18). Heaven is built, not mined: only operators may break or place anything, except a
 * plot's owner inside their own home yard once their home is theirs (Zachariah has fallen). Containers open for the plot's
 * owner, the hunters they trust and operators (the Roadhouse's for anyone). Explosions break nothing; buckets, fire and tools
 * that reshape blocks follow the building rule; ender pearls and chorus fruit do not work for anyone but operators.
 * The rules are plain methods taking the level (so tests run them anywhere); {@code HeavenEvents} applies them in Heaven.
 */
public final class HeavenProtection {

    private HeavenProtection() {
    }

    public static boolean operator(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /** Whether {@code player} may break or place a block at {@code pos}. */
    public static boolean mayEdit(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (operator(player)) return true;
        HeavenPlot plot = HeavenPlots.plotAt(level, pos);
        if (plot == null || plot.hub()) return false;
        return player.getUUID().equals(plot.owner) && plot.homeOpen && HeavenPlots.inHomeYard(level, plot, pos);
    }

    /** Whether {@code player} may open the container at {@code pos}. */
    public static boolean mayOpen(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (operator(player)) return true;
        HeavenPlot plot = HeavenPlots.plotAt(level, pos);
        if (plot == null || plot.hub()) return true;
        return player.getUUID().equals(plot.owner) || plot.trusted.contains(player.getUUID());
    }

    /** Whether opening the block at {@code pos} is opening a container the rule guards (the mod's own blocks are not). */
    public static boolean guardedContainer(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(SupernaturalCraft.MODID)) return false;
        return level.getBlockEntity(pos) instanceof Container;
    }

    /** Whether using {@code stack} on a block changes the world (fluids, fire, stripping, tilling, waxing, growing). */
    public static boolean reshapes(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof BucketItem || item instanceof SolidBucketItem || item instanceof FlintAndSteelItem
                || item instanceof FireChargeItem || item instanceof BoneMealItem || item instanceof DiggerItem
                || item instanceof ShearsItem || item instanceof HoneycombItem;
    }
}
