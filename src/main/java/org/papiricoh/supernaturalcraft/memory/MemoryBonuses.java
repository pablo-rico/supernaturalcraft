package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The gifts of completed memory sets (v0.18), read from the hunter's log ({@link MemorySets}). What can live on the hunter
 * themselves is applied here (the extra heart, the companions' mending, the blows against creatures seen); the rest are getters
 * that their own systems ask: Aegis ({@code balance/DefenceEvents}), research time ({@code legacy/research}), max mana (the
 * allegiance passives) and the days of a deal (the crossroads).
 */
public final class MemoryBonuses {

    public static final ResourceLocation HEART_ID = SupernaturalCraft.asResource("memory_heart");
    /** Ticks between two rounds of the companions' mending, and how long each lasts. */
    public static final int COMPANION_PERIOD = 100, COMPANION_DURATION = 120;

    private MemoryBonuses() {
    }

    public static boolean has(Player p, MemorySets.Set set) {
        MemoryLog log = Memories.get(p);
        return MemorySets.complete(set, log.entries(), log.collected());
    }

    /** Share of a great enemy's blow turned aside: add it to the other Aegis sources. */
    public static float aegis(Player p) {
        return has(p, MemorySets.Set.VICTORIES) ? MemorySets.VICTORY_AEGIS : 0f;
    }

    /** Multiply research time by this (≤ 1): divide the research speed by it. */
    public static double researchTimeFactor(Player p) {
        return has(p, MemorySets.Set.CASES) ? MemorySets.CASES_RESEARCH_TIME : 1.0;
    }

    /** Extra max mana. */
    public static int extraMana(Player p) {
        return has(p, MemorySets.Set.KIN) ? MemorySets.KIN_MANA : 0;
    }

    /** Days added to the term of a deal struck now. */
    public static int extraDealDays(Player p) {
        return has(p, MemorySets.Set.CROSSROADS) ? MemorySets.CROSSROADS_DAYS : 0;
    }

    /** Extra share of damage {@code p} deals {@code target}: a creature they have seen, never a great enemy. */
    public static float sightingDamageBonus(Player p, Entity target) {
        if (target.getType().is(AllTags.Entities.BOSSES) || target instanceof Player) return 0f;
        if (!has(p, MemorySets.Set.SIGHTINGS)) return 0f;
        return HunterLogs.get(p).hasSeen(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())) ? MemorySets.SIGHTING_DAMAGE : 0f;
    }

    // --- on the hunter ---------------------------------------------------------------------------------------------------

    /** Puts the All Victories heart on {@code p} (idempotent; saved with the player so a relog keeps the health). */
    public static void refresh(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        double value = has(p, MemorySets.Set.ALL_VICTORIES) ? MemorySets.ALL_VICTORIES_HEALTH : 0;
        AttributeModifier current = inst.getModifier(HEART_ID);
        if (value == 0) {
            if (current != null) inst.removeModifier(HEART_ID);
        } else if (current == null || current.amount() != value) {
            inst.addOrReplacePermanentModifier(new AttributeModifier(HEART_ID, value, AttributeModifier.Operation.ADD_VALUE));
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Companions: {@code p}'s tamed pets close by mend. */
    public static void companions(ServerPlayer p) {
        if (!has(p, MemorySets.Set.COMPANIONS)) return;
        UUID me = p.getUUID();
        for (LivingEntity pet : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(MemorySets.COMPANION_RANGE),
                e -> e instanceof OwnableEntity o && me.equals(o.getOwnerUUID()) && e.isAlive())) {
            pet.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COMPANION_DURATION, 0, true, false));
        }
    }

    /** {@code p} just completed {@code sets}: tell them, and what each gives. */
    public static void completed(ServerPlayer p, EnumSet<MemorySets.Set> sets) {
        for (MemorySets.Set s : sets) {
            p.displayClientMessage(Component.translatable(MemoryText.message("set_complete"), Component.translatable(s.key()),
                    Component.translatable(s.key() + ".bonus")).withStyle(ChatFormatting.GOLD), false);
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
    }
}
