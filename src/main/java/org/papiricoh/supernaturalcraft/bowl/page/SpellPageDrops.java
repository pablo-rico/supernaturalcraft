package org.papiricoh.supernaturalcraft.bowl.page;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.Optional;

/**
 * Where spell pages come from besides graves and chests: demons (occultists 15%, black-eyed demons
 * 5%, killed by a player; a crossroads demon called up hostile, always) and master librarians,
 * who sell them for emeralds and a book. Vanilla chests get theirs through a global loot modifier
 * rolling {@link #CHANCE_TABLE}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SpellPageDrops {

    /** One roll: now and then a spell page. Added to vanilla chests by {@code SNLootModifiers}. */
    public static final ResourceKey<LootTable> CHANCE_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, SupernaturalCraft.asResource("chests/spell_page_chance"));
    public static final float OCCULTIST_CHANCE = 0.15f;
    public static final float BLACK_EYED_CHANCE = 0.05f;

    private SpellPageDrops() {
    }

    /** A page with a random spell (weighted by its recipes' page weights), or empty if there are no spells. */
    public static ItemStack randomPage(RecipeManager recipes, RandomSource random) {
        ResourceLocation spell = BowlSpells.randomSpell(recipes, random);
        return spell == null ? ItemStack.EMPTY : SpellPageItem.of(spell);
    }

    /** Chance that {@code dead}, killed by a player or not, drops a page. */
    public static float chance(LivingEntity dead, boolean byPlayer) {
        if (dead instanceof CrossroadsDemonEntity demon) return demon.isHostile() ? 1f : 0f;
        if (!byPlayer) return 0f;
        if (dead.getType() == AllEntities.DEMON_OCCULTIST.get()) return OCCULTIST_CHANCE;
        if (dead.getType() == AllEntities.BLACK_EYED_DEMON.get()) return BLACK_EYED_CHANCE;
        return 0f;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        float chance = chance(dead, event.getSource().getEntity() instanceof Player);
        if (chance <= 0 || dead.getRandom().nextFloat() >= chance) return;
        ItemStack page = randomPage(dead.level().getRecipeManager(), dead.getRandom());
        if (!page.isEmpty()) event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), page));
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;
        event.getTrades().get(4).add((trader, random) -> offer(trader.level().getRecipeManager(), random, 12, 10));
        event.getTrades().get(5).add((trader, random) -> offer(trader.level().getRecipeManager(), random, 16, 15));
    }

    /** A librarian's offer: a random spell page for emeralds and a book; null if there are no spells. */
    @Nullable
    public static MerchantOffer offer(RecipeManager recipes, RandomSource random, int baseEmeralds, int xp) {
        ItemStack page = randomPage(recipes, random);
        if (page.isEmpty()) return null;
        return new MerchantOffer(new ItemCost(Items.EMERALD, baseEmeralds + random.nextInt(9)), Optional.of(new ItemCost(Items.BOOK, 1)),
                page, 3, xp, 0.2f);
    }
}
