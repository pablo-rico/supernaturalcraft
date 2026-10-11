package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A boss's progression keys are every challenger's (v0.19): instead of one on the ground for whoever grabs it, each hunter who
 * fought gets their own, straight into the inventory (or owed, if they are away or fell during the long death). Trophies and
 * the rest of the spoils still fall once. Fights with no known challengers (spawned by command, tests) drop as before.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BossShares {

    private BossShares() {
    }

    /** What every challenger gets their own copy of. */
    public static boolean shared(ItemStack stack) {
        return stack.is(AllTags.Items.PROGRESSION_KEYS) || stack.is(AllItems.THE_COLT.get());
    }

    /** The arena {@code boss} fought in, or null. */
    public static @Nullable ArenaController arenaOf(ServerLevel level, Entity boss) {
        for (ArenaController a : ArenaSavedData.get(level).all()) {
            if (boss.getUUID().equals(a.bossId())) return a;
        }
        return null;
    }

    /** Everyone who fought {@code boss}: its arena's participants (fallen, away or not), else the players close by. */
    public static Set<UUID> fighters(ServerLevel level, Entity boss, double range) {
        Set<UUID> out = new LinkedHashSet<>();
        ArenaController arena = arenaOf(level, boss);
        if (arena != null) out.addAll(arena.participants());
        if (out.isEmpty()) {
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && p.distanceToSqr(boss) < range * range) out.add(p.getUUID());
            }
        }
        return out;
    }

    /**
     * Gives one copy of {@code stack} to every fighter of {@code boss}.
     *
     * @return false (nothing given) if nobody is known to have fought it
     */
    public static boolean share(ServerLevel level, Entity boss, ItemStack stack) {
        Set<UUID> who = fighters(level, boss, 48);
        if (who.isEmpty()) return false;
        for (UUID u : who) Stakes.give(level.getServer(), u, List.of(stack.copy()), false);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !dead.getType().is(AllTags.Entities.BOSSES)) return;
        event.getDrops().removeIf((ItemEntity item) -> shared(item.getItem()) && share(level, dead, item.getItem()));
    }
}
