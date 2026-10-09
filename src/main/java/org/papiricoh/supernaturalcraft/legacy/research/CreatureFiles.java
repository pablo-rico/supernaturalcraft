package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Creature files in the field (v0.17): a member's file on a creature makes them hit it harder ({@link ResearchMath#bonus}, capped
 * by the config, never against bosses), and members' kills leave field notes behind: on the creature (less often the more of its
 * kind they have killed), now and then arcane notes from the supernatural, and from a boss a handful for every member who
 * fought it.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CreatureFiles {

    /** Chance of notes from the first kills of a kind. */
    public static final float FIRST_CHANCE = 0.35f;
    /** Lowest chance, however many have been killed. */
    public static final float MIN_CHANCE = 0.04f;
    /** Kills that halve the chance. */
    public static final int HALVING_KILLS = 10;
    /** Chance of an arcane note from a supernatural creature. */
    public static final float ARCANE_CHANCE = 0.06f;
    /** Notes every member near a fallen boss gets. */
    public static final int BOSS_NOTES = 3;
    public static final double BOSS_RADIUS = 48;

    private CreatureFiles() {
    }

    public static boolean isBoss(EntityType<?> type) {
        return type.is(AllTags.Entities.BOSSES) || type.is(Tags.EntityTypes.BOSSES);
    }

    /** The damage multiplier {@code player}'s file gives against {@code target} (1 = none). */
    public static float multiplier(Player player, LivingEntity target) {
        if (isBoss(target.getType())) return 1f;
        int level = Legacies.archive(player).fileLevel(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        if (level <= 0) return 1f;
        return (float) (1 + ResearchMath.bonus(SNConfig.CREATURE_FILE_DAMAGE_CAP.get(), level));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getEntity() instanceof Player player) || player == target) return;
        float m = multiplier(player, target);
        if (m > 1f) event.setAmount(event.getAmount() * m);
    }

    /** Chance a kill leaves notes, after {@code kills} of its kind. */
    public static float chance(int kills) {
        return Math.max(MIN_CHANCE, FIRST_CHANCE / (1f + Math.max(0, kills) / (float) HALVING_KILLS));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer player) || !Legacies.member(player)) return;
        EntityType<?> type = dead.getType();
        if (!ResearchService.fileable(type)) return;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (dead.getRandom().nextFloat() < chance(HunterLogs.get(player).kills(id))) {
            drop(event, dead, FieldNotesItem.stack(FieldNotesItem.creature(id), 1));
        }
        if (type.is(AllTags.Entities.SUPERNATURAL) && dead.getRandom().nextFloat() < ARCANE_CHANCE) {
            drop(event, dead, FieldNotesItem.stack(FieldNotesItem.ARCANE, 1));
        }
    }

    private static void drop(LivingDropsEvent event, LivingEntity dead, ItemStack stack) {
        event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), stack));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || !isBoss(dead.getType()) || dead.getServer() == null) return;
        String topic = FieldNotesItem.creature(BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()));
        for (ServerPlayer p : dead.getServer().getPlayerList().getPlayers()) {
            if (p.level() != dead.level() || p.distanceToSqr(dead) > BOSS_RADIUS * BOSS_RADIUS || !Legacies.member(p)) continue;
            give(p, FieldNotesItem.stack(topic, BOSS_NOTES));
        }
    }

    static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }
}
