package org.papiricoh.supernaturalcraft.weapon.ascension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;

/**
 * The great enemies' Ascension Shards (v0.15): every time one falls, each hunter who fought it (its challengers, or
 * everyone within {@link #RANGE} blocks, as for the shared boss credit) receives {@link #MIN}–{@link #MAX} shards of the tier
 * it leaves ({@link ProgressionScale.BossStats#shardTier}), straight into the inventory (or at their feet if it is full).
 * First kill and rematches alike; the Author leaves none.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ShardSpoils {

    public static final int MIN = 1, MAX = 2;
    public static final double RANGE = 48;

    private ShardSpoils() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !dead.getType().is(AllTags.Entities.BOSSES)) return;
        Boss boss = bossOf(dead);
        if (boss == null) return;
        List<ServerPlayer> fighters = dead instanceof LuciferEntity lucifer && !lucifer.challengers().isEmpty() ? lucifer.challengers()
                : level.getEntitiesOfClass(ServerPlayer.class, dead.getBoundingBox().inflate(RANGE), p -> p.isAlive() && !p.isSpectator());
        if (fighters.isEmpty() && event.getSource().getEntity() instanceof ServerPlayer killer) fighters = List.of(killer);
        for (ServerPlayer p : fighters) give(p, boss, level.random);
    }

    public static @Nullable Boss bossOf(LivingEntity e) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id.getNamespace().equals(SupernaturalCraft.MODID) ? Boss.byEntity(id.getPath()) : null;
    }

    /** Gives {@code p} the shards {@code boss} leaves. @return how many (0 for the Author) */
    public static int give(ServerPlayer p, Boss boss, RandomSource random) {
        int tier = ProgressionScale.of(boss).shardTier();
        if (tier <= 0) return 0;
        int n = MIN + random.nextInt(MAX - MIN + 1);
        ItemStack shards = new ItemStack(AllItems.shardOf(tier).get(), n);
        if (!p.getInventory().add(shards)) p.drop(shards, false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.5f + 0.15f * tier);
        return n;
    }
}
