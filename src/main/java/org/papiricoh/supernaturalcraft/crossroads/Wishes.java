package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.ritual.effect.LocateStructureEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** What the crossroads demon hands over when a deal is sealed. */
public final class Wishes {

    private Wishes() {
    }

    /** One line of the demon's pockets: an item, how many, and how likely. */
    private record Rare(Supplier<? extends Item> item, int min, int max, int weight) {
    }

    /** The demon's pockets (Rare Item). */
    private static final List<Rare> RARE = List.of(
            new Rare(AllItems.COLT_BULLET, 1, 2, 2),
            new Rare(AllItems.CHOIR_SHARD, 2, 4, 3),
            new Rare(AllItems.HELLFIRE_EMBER, 3, 6, 4),
            new Rare(() -> Items.DIAMOND, 3, 6, 4),
            new Rare(() -> Items.TOTEM_OF_UNDYING, 1, 1, 3),
            new Rare(() -> Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 2),
            new Rare(() -> Items.NETHERITE_SCRAP, 2, 3, 2));

    public static ItemStack rareItem(RandomSource random) {
        int total = RARE.stream().mapToInt(Rare::weight).sum();
        int roll = random.nextInt(total);
        for (Rare r : RARE) {
            roll -= r.weight();
            if (roll < 0) return new ItemStack(r.item().get(), r.min() + random.nextInt(r.max() - r.min() + 1));
        }
        return new ItemStack(Items.DIAMOND);
    }

    /** @return false if there was nothing to grant (the deal is not sealed) */
    public static boolean grant(ServerPlayer p, DealTerms.Wish wish, int arg, Vec3 at) {
        return switch (wish) {
            case UPGRADE -> upgrade(p, arg);
            case RECOVER -> arg == 0 ? recoverBelongings(p) : CrossroadsHooks.petRevival.revive(p, at);
            case RARE_ITEM -> {
                give(p, rareItem(p.getRandom()));
                yield true;
            }
            case KNOWLEDGE -> {
                knowledge(p);
                yield true;
            }
            // "Make me one of you" (v0.13): a demon now; the crossroads keeps two hearts until a cure.
            case CONVERT -> CrossroadsHooks.soul.convert(p);
        };
    }

    private static boolean upgrade(ServerPlayer p, int arg) {
        ArcanaData data = ManaManager.get(p);
        if (!DealTerms.canUpgrade(data.bonusHearts(), data.bonusMana(), arg)) return false;
        if (arg == 0) {
            data.setBonusHearts(data.bonusHearts() + DealTerms.HEARTS_PER_DEAL);
            Boons.apply(p);
            p.heal(DealTerms.HEARTS_PER_DEAL * 2);
        } else {
            data.setBonusMana(data.bonusMana() + DealTerms.MANA_PER_DEAL);
            data.setMana(data.maxMana());
        }
        sync(p);
        return true;
    }

    /** Takes {@code amount} of the upgrade back (a collected soul loses what it bought). */
    public static void revokeUpgrade(ServerPlayer p, int arg) {
        ArcanaData data = ManaManager.get(p);
        if (arg == 0) {
            data.setBonusHearts(data.bonusHearts() - DealTerms.HEARTS_PER_DEAL);
            Boons.apply(p);
        } else {
            data.setBonusMana(data.bonusMana() - DealTerms.MANA_PER_DEAL);
        }
        sync(p);
    }

    static void sync(ServerPlayer p) {
        if (!(p instanceof FakePlayer)) SNNetworking.syncArcana(p);
    }

    private static boolean recoverBelongings(ServerPlayer p) {
        List<ItemStack> items = LostBelongings.get(p.server).claimAll(p.server, p.getUUID());
        if (items.isEmpty()) return false;
        items.forEach(s -> give(p, s));
        return true;
    }

    /** Maps to the nearest Hymnal Spire and grave; if there are none, the next great enemy's weakness. */
    private static void knowledge(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean spire = new LocateStructureEffect(AllTags.Structures.HYMNAL_SPIRES, SupernaturalCraft.asResource("hymnal_spire"),
                "map.supernaturalcraft.crossroads.spire", 0xE8C25A, 6).perform(level, p.blockPosition(), p);
        boolean grave = new LocateStructureEffect(AllTags.Structures.GRAVES, ResourceLocation.withDefaultNamespace("red_x"),
                "map.supernaturalcraft.crossroads.grave", 0x6A7A5A, 6).perform(level, p.blockPosition(), p);
        if (spire || grave) return;
        give(p, weaknessBook(p));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.knowledge_book")
                .withStyle(ChatFormatting.DARK_RED), true);
    }

    /** A book naming the first great enemy {@code p} has not beaten yet, and how to beat it. */
    public static ItemStack weaknessBook(ServerPlayer p) {
        BossProgression.Boss next = BossProgression.next(path -> {
            var adv = p.server.getAdvancements().get(SupernaturalCraft.asResource(path));
            return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
        });
        List<Filterable<Component>> pages = new ArrayList<>();
        if (next == null) {
            pages.add(Filterable.passThrough(Component.translatable("book.supernaturalcraft.crossroads.none")));
        } else {
            pages.add(Filterable.passThrough(Component.translatable("book.supernaturalcraft.crossroads.intro",
                    Component.translatable(next.key() + ".name"))));
            pages.add(Filterable.passThrough(Component.translatable(next.key() + ".weakness")));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("What the Crossroads Know"),
                "a crossroads demon", 0, pages, true));
        book.set(DataComponents.CUSTOM_NAME, Component.translatable("book.supernaturalcraft.crossroads.title")
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.DARK_RED)));
        return book;
    }

    static void give(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }
}
