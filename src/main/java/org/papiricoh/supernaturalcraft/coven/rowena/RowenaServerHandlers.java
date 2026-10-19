package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.magic.item.HexedInkItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;

/**
 * Server side of the talk with Rowena (v0.21), over the shared dialogue ({@link AllegianceDialogue}, id {@link RowenaDialogue#ID}):
 * opening it when a hunter uses her ({@code main/met_rowena} the first time), every answer after that, and the purchases —
 * pages ({@link RowenaLessons}: paid in emeralds, gold and a reagent, handed over as a spell page to read) and wares (taglocks,
 * Witchcraft hexed ink). After {@code main/devil_went_down} she tells where Cain keeps his bees ({@code main/rowenas_clue}) and
 * writes his address down ({@code cains_address}; again if it was lost).
 */
public final class RowenaServerHandlers {

    public static final String MET = "main/met_rowena", CLUE = "main/rowenas_clue", DEVIL_WENT_DOWN = "main/devil_went_down";

    private RowenaServerHandlers() {
    }

    /** Hooks her into the shared dialogue (once, at mod construction). */
    public static void register() {
        AllegianceDialogue.register(RowenaDialogue.ID, RowenaServerHandlers::answer);
    }

    public static RowenaRelation relation(ServerPlayer player) {
        return player.getData(AllAttachments.ROWENA);
    }

    public static void setRelation(ServerPlayer player, RowenaRelation r) {
        player.setData(AllAttachments.ROWENA, r);
    }

    /** What she knows of {@code player}, and what she would teach them now. */
    public static RowenaDialogue.Context context(ServerPlayer player) {
        RowenaRelation r = relation(player);
        var arcana = ManaManager.get(player);
        // A page bought (read or not) is off her offer.
        // v0.21 (P6): her ally, once the Grand Witch has fallen with her, may buy her Master's pages too.
        boolean witchBeaten = AuthorWorld.done(player, org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.GRAND_WITCH.advancement);
        List<String> offer = RowenaLessons.offer(id -> arcana.knowsRite(SupernaturalCraft.asResource(id)) || r.bought().contains(id),
                tier -> Mastery.mayLearn(player, School.WITCHCRAFT, tier), r.trust(), r.book() == RowenaRelation.Book.GIVEN && witchBeaten);
        RowenaDialogue.Ending ending = new RowenaDialogue.Ending(org.papiricoh.supernaturalcraft.coven.ending.BookChoice.ready(player), r.book(),
                witchBeaten, org.papiricoh.supernaturalcraft.mark.Marks.marked(player));
        return new RowenaDialogue.Context(r.met(), AuthorWorld.done(player, DEVIL_WENT_DOWN), r.clueGiven(), offer, ending);
    }

    /** {@code player} used her: she turns and greets them. */
    public static void open(RowenaNpcEntity npc, ServerPlayer player) {
        if (npc.distanceTo(player) > RowenaNpcEntity.REACH) return;
        RowenaDialogue.Context ctx = context(player);
        String node = RowenaDialogue.start(ctx);
        if (!ctx.met()) {
            setRelation(player, relation(player).meet());
            ChorusRewards.award(player, MET);
        }
        npc.talkTo(player);
        say(player, npc, node, ctx);
    }

    private static void say(ServerPlayer player, Entity npc, String node, RowenaDialogue.Context ctx) {
        AllegianceDialogue.ask(player, npc, RowenaDialogue.ID, node, RowenaDialogue.options(node, ctx), 0);
    }

    /** An answer from the shared dialogue (already checked against the question asked). */
    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof RowenaNpcEntity npc) || !npc.isAlive() || npc.distanceTo(player) > RowenaNpcEntity.REACH * 2) return;
        choose(player, npc, node, option);
    }

    /**
     * Answers {@code option} to {@code node}. @return the node she answers with ({@code ""} if the talk ended), or null if refused.
     * Public for tests (FakePlayers answer by hand).
     */
    public static @Nullable String choose(ServerPlayer player, RowenaNpcEntity npc, String node, String option) {
        RowenaDialogue.Context ctx = context(player);
        String next = RowenaDialogue.next(node, option, ctx);
        if (next == null) return null;
        if (next.isEmpty()) {
            npc.stopTalking();
            return next;
        }
        if (next.startsWith(RowenaDialogue.BUY_PAGE)) {
            next = buyPage(player, npc, next.substring(RowenaDialogue.BUY_PAGE.length()), ctx) ? RowenaDialogue.BOUGHT : RowenaDialogue.TOO_POOR;
        } else if (next.startsWith(RowenaDialogue.BUY_WARE)) {
            next = buyWare(player, npc, next.substring(RowenaDialogue.BUY_WARE.length())) ? RowenaDialogue.SOLD : RowenaDialogue.TOO_POOR;
        } else if (next.equals(RowenaDialogue.CLUE)) {
            setRelation(player, relation(player).giveClue());
            ChorusRewards.award(player, CLUE);
            // v0.21 (P4): she writes his address down (its ink leads to his gate).
            hand(player, org.papiricoh.supernaturalcraft.cain.CainsAddressItem.pointed(player.serverLevel()));
            npc.give();
        } else if (next.equals(RowenaDialogue.CLUE_AGAIN) && player.getInventory().countItem(AllItems.CAINS_ADDRESS.get()) == 0) {
            hand(player, org.papiricoh.supernaturalcraft.cain.CainsAddressItem.pointed(player.serverLevel()));
            npc.give();
        } else if (next.equals(RowenaDialogue.BOOK_GIVEN)) {
            // v0.21 (P6): the choice, final.
            if (!org.papiricoh.supernaturalcraft.coven.ending.BookChoice.give(player, npc)) return null;
            npc.give();
        } else if (next.equals(RowenaDialogue.BOOK_KEPT)) {
            if (!org.papiricoh.supernaturalcraft.coven.ending.BookChoice.keep(player, npc)) return null;
        } else if (next.equals(RowenaDialogue.UNMARK)) {
            String why = org.papiricoh.supernaturalcraft.coven.ending.Unmarking.why(player, true, true);
            if (why == null) {
                npc.give();
                net.minecraft.world.phys.Vec3 from = npc.position().add(0, 1.4, 0);
                RowenaSavedData data = RowenaSavedData.get(player.serverLevel());
                if (data.built() && data.house() != null) {
                    net.minecraft.core.BlockPos bowl = RowenaWorld.bowl(new RowenaSite.Site(data.house(), data.rotation()));
                    if (bowl.distSqr(npc.blockPosition()) < 24 * 24) from = net.minecraft.world.phys.Vec3.atBottomCenterOf(bowl).add(0, 0.6, 0);
                }
                org.papiricoh.supernaturalcraft.coven.ending.Unmarking.perform(player, npc, from, true);
                next = RowenaDialogue.UNMARKED;
            } else {
                next = RowenaDialogue.NEEDS_BLOOD;
            }
        }
        npc.talkTo(player);
        RowenaDialogue.Context after = context(player);
        say(player, npc, next, after);
        if (next.equals(RowenaDialogue.BOOK_KEPT)) npc.stopTalking();
        return next;
    }

    /** Sells {@code page} if she offers it and the hunter can pay. */
    public static boolean buyPage(ServerPlayer player, RowenaNpcEntity npc, String page, RowenaDialogue.Context ctx) {
        RowenaLessons.Page p = RowenaLessons.page(page);
        if (p == null || !ctx.offer().contains(page)) return false;
        RowenaLessons.Price price = RowenaLessons.price(p.tier());
        List<ItemStack> cost = new ArrayList<>();
        cost.add(new ItemStack(Items.EMERALD, price.emeralds()));
        if (price.gold() > 0) cost.add(new ItemStack(Items.GOLD_INGOT, price.gold()));
        if (price.reagent() != null) {
            Item reagent = BuiltInRegistries.ITEM.get(ResourceLocation.parse(price.reagent()));
            cost.add(new ItemStack(reagent, price.reagentCount()));
        }
        if (!pay(player, cost)) return false;
        hand(player, SpellPageItem.of(SupernaturalCraft.asResource(page)));
        setRelation(player, relation(player).trusted(RowenaLessons.TRUST_PER_PAGE).buy(page));
        npc.give();
        return true;
    }

    /** Sells one of her wares. */
    public static boolean buyWare(ServerPlayer player, RowenaNpcEntity npc, String ware) {
        int[] w = RowenaLessons.WARES.get(ware);
        if (w == null || !pay(player, List.of(new ItemStack(Items.EMERALD, w[0])))) return false;
        ItemStack goods = switch (ware) {
            case "taglocks" -> new ItemStack(AllItems.TAGLOCK.get(), w[1]);
            case "hexed_ink" -> HexedInkItem.of(School.WITCHCRAFT).copyWithCount(w[1]);
            default -> ItemStack.EMPTY;
        };
        if (!goods.isEmpty()) hand(player, goods);
        npc.give();
        return true;
    }

    /** Takes {@code cost} from the inventory if it is all there (creative players pay nothing). */
    public static boolean pay(ServerPlayer player, List<ItemStack> cost) {
        if (player.getAbilities().instabuild) return true;
        Inventory inv = player.getInventory();
        for (ItemStack c : cost) if (inv.countItem(c.getItem()) < c.getCount()) return false;
        for (ItemStack c : cost) {
            int left = c.getCount();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(c.getItem())) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    private static void hand(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
