package org.papiricoh.supernaturalcraft.magic;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.List;

/**
 * Demons and angels carry scraps of their masters' grimoires. A page drops with a sigil picked from whatever the loaded
 * datapacks define, so new sigils join the loot automatically, but only from the schools of the dead one's kind: Hell's
 * rank and file carry Infernal and Witchcraft sigils, the Host's Enochian ones, and both the Hunter's and the schoolless
 * forms and modifiers every caster needs. Bosses, their minions (anything dying inside a live arena), hellhounds and
 * Heaven's own servants drop none.
 */
public final class SigilPageDrops {

    /** Chance that a demon or an angel killed by a player drops a page. */
    public static final float CHANCE = 0.04f;
    /** An occultist carries a grimoire: far more often, and up to an Adept's sigil (tier 3). */
    public static final float OCCULTIST_CHANCE = 0.15f;
    public static final List<School> DEMON_SCHOOLS = List.of(School.INFERNAL, School.WITCHCRAFT, School.HUNTER, School.NONE);
    public static final List<School> ANGEL_SCHOOLS = List.of(School.ENOCHIAN, School.HUNTER, School.NONE);

    private SigilPageDrops() {
    }

    /** The schools whose pages {@code type} may carry, or empty if it carries none. */
    public static List<School> schools(EntityType<?> type) {
        if (type == AllEntities.BLACK_EYED_DEMON.get() || type == AllEntities.DEMON_OCCULTIST.get()
                || type == AllEntities.CROSSROADS_DEMON.get() || type == AllEntities.DEMON_BRUTE.get()
                || type == AllEntities.SULFUR_PRIEST.get() || type == AllEntities.DEMON_FLAYER.get()
                || type == AllEntities.DEMON_TORMENTOR.get()) return DEMON_SCHOOLS;
        if (type == AllEntities.ANGEL.get() || type == AllEntities.FALLEN_ANGEL.get() || type == AllEntities.GRIGORI.get()
                || type == AllEntities.ANGEL_TEMPLAR.get() || type == AllEntities.ANGEL_HERALD.get()
                || type == AllEntities.ANGEL_CHOIRMASTER.get() || type == AllEntities.ANGEL_DOMINION.get()) return ANGEL_SCHOOLS;
        return List.of();
    }

    /** Chance that {@code type}, killed by a player, drops a page. */
    public static float chance(EntityType<?> type) {
        if (schools(type).isEmpty()) return 0f;
        return type == AllEntities.DEMON_OCCULTIST.get() ? OCCULTIST_CHANCE : CHANCE;
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player) || !(dead.level() instanceof ServerLevel level)) return;
        EntityType<?> type = dead.getType();
        float chance = chance(type);
        if (chance <= 0 || type.is(AllTags.Entities.BOSSES) || dead.getRandom().nextFloat() >= chance) return;
        // A boss's minions fall by the dozen: they would bury the arena in pages.
        if (ArenaSavedData.get(level).at(dead.position()) != null) return;
        int maxTier = type == AllEntities.DEMON_OCCULTIST.get() ? 3 : 2;
        ResourceLocation id = randomSigil(level.registryAccess().registryOrThrow(SNRegistries.SIGIL), dead.getRandom(), maxTier, schools(type));
        if (id != null) {
            event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(),
                    SigilPageItem.of(AllItems.SIGIL_PAGE.get(), id)));
        }
    }

    /** A random sigil of at most {@code maxTier} (never a Master's) from {@code schools}, weighted toward the lower tiers. */
    public static @Nullable ResourceLocation randomSigil(Registry<SigilComponent> sigils, RandomSource random, int maxTier,
                                                         List<School> schools) {
        List<Holder.Reference<SigilComponent>> pool = sigils.holders()
                .filter(h -> h.value().tier() <= Math.min(maxTier, 3) && schools.contains(h.value().school())).toList();
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
