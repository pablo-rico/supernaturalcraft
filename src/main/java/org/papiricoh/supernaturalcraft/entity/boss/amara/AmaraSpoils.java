package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.darkness.DarknessRules;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * What Amara leaves when she makes peace (v0.42), one share per hunter who fought her ({@link BossShares}): Penumbra, the
 * Sight, her trophy, Void Essence and her gift, Amara's Light. Each fighter's road reaches its end
 * ({@link DarknessRules.Step#DONE}). Her loot table is empty: everything is dealt here.
 */
public final class AmaraSpoils {

    private AmaraSpoils() {
    }

    public static List<ItemStack> spoils(AmaraEntity boss) {
        return List.of(new ItemStack(AllItems.PENUMBRA.get()), new ItemStack(AllItems.ECLIPSE_SIGHT.get()),
                new ItemStack(AllItems.ECLIPSE_TROPHY.get()), new ItemStack(AllItems.VOID_ESSENCE.get(), 3 + boss.getRandom().nextInt(3)),
                new ItemStack(DarknessRegistry.AMARAS_LIGHT.get()));
    }

    public static void deal(ServerLevel level, AmaraEntity boss, List<ServerPlayer> challengers) {
        for (ItemStack stack : spoils(boss)) {
            if (!BossShares.share(level, boss, stack)) {
                level.addFreshEntity(new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), stack));
            }
        }
        for (ServerPlayer p : challengers) Darknesses.advance(p, DarknessRules.Step.DONE);
    }
}
