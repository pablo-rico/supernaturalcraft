package org.papiricoh.supernaturalcraft.magic;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.List;

/**
 * Demons carry scraps of their masters' grimoires. A page drops with a sigil picked from
 * whatever the loaded datapacks define, so new sigils join the loot automatically.
 */
public final class SigilPageDrops {

    private SigilPageDrops() {
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!dead.getType().is(AllTags.Entities.DEMONS) || !(event.getSource().getEntity() instanceof Player)) return;
        float chance = dead.getType() == AllEntities.DEMON_OCCULTIST.get() ? 0.25f : 0.08f;
        if (dead.getRandom().nextFloat() >= chance) return;
        ResourceLocation id = randomSigil(dead.level().registryAccess().registryOrThrow(SNRegistries.SIGIL), dead.getRandom(), 2);
        if (id != null) {
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(),
                    SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id)));
        }
    }

    /** A random sigil of at most {@code maxTier}, weighted toward the lower tiers. */
    public static @Nullable ResourceLocation randomSigil(Registry<SigilComponent> sigils, RandomSource random, int maxTier) {
        List<Holder.Reference<SigilComponent>> pool = sigils.holders()
                .filter(h -> h.value().tier() <= maxTier).toList();
        if (pool.isEmpty()) return null;
        int total = 0;
        for (var h : pool) total += 4 - h.value().tier();
        int roll = random.nextInt(total);
        for (var h : pool) {
            roll -= 4 - h.value().tier();
            if (roll < 0) return h.key().location();
        }
        return pool.getLast().key().location();
    }
}
