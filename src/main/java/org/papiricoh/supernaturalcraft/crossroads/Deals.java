package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.DealOfferPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;

/** Making a deal: what the demon offers, and sealing one. */
public final class Deals {

    /** Max distance (blocks) between the player and the demon for a choice to count. */
    public static final double REACH = 8;

    private Deals() {
    }

    /** One wish on offer. */
    public record Option(DealTerms.Wish wish, int arg) {
        public int days() {
            return wish.days;
        }
    }

    /** What the demon will grant {@code p} right now (computed on the server). */
    public static List<Option> offer(ServerPlayer p) {
        List<Option> out = new ArrayList<>();
        ArcanaData data = ManaManager.get(p);
        if (DealTerms.canUpgrade(data.bonusHearts(), data.bonusMana(), 0)) out.add(new Option(DealTerms.Wish.UPGRADE, 0));
        if (DealTerms.canUpgrade(data.bonusHearts(), data.bonusMana(), 1)) out.add(new Option(DealTerms.Wish.UPGRADE, 1));
        if (LostBelongings.get(p.server).hasUnclaimed(p.getUUID())) out.add(new Option(DealTerms.Wish.RECOVER, 0));
        if (CrossroadsHooks.petRevival.available(p)) out.add(new Option(DealTerms.Wish.RECOVER, 1));
        out.add(new Option(DealTerms.Wish.RARE_ITEM, 0));
        out.add(new Option(DealTerms.Wish.KNOWLEDGE, 0));
        if (CrossroadsHooks.soul.mayConvert(p)) out.add(new Option(DealTerms.Wish.CONVERT, 0));
        return out;
    }

    public static boolean allowed(ServerPlayer p, DealTerms.Wish wish, int arg) {
        return wish != null && offer(p).contains(new Option(wish, arg));
    }

    /** Right-click on a neutral demon: if it is yours and you are free, hear its offer. */
    public static void openOffer(ServerPlayer p, CrossroadsDemonEntity demon) {
        if (!demon.isSummoner(p)) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.not_yours").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        if (Debts.get(p).active()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.already_bound").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        List<Option> options = offer(p);
        demon.offering();
        if (p instanceof FakePlayer) return;
        PacketDistributor.sendToPlayer(p, new DealOfferPayload(demon.getId(),
                options.stream().map(o -> o.wish().id()).toList(),
                options.stream().map(Option::arg).toList(),
                options.stream().map(Option::days).toList(), CrossroadsHooks.soul.mayConvert(p)));
    }

    /**
     * Seals a deal: the wish is granted, the contract handed over, the clock starts. The demon (if
     * any) kisses on it and leaves.
     *
     * @return false if the deal cannot be made (already bound, not on offer, nothing to grant)
     */
    public static boolean seal(ServerPlayer p, @Nullable CrossroadsDemonEntity demon, DealTerms.Wish wish, int arg) {
        return seal(p, demon, wish, arg, false);
    }

    /**
     * {@link #seal(ServerPlayer, CrossroadsDemonEntity, DealTerms.Wish, int)} with "Bind my soul" ticked (v0.13): if the
     * hounds collect, the soul rises a demon. A wish settled at once ("Make me one of you") leaves no debt at all.
     */
    public static boolean seal(ServerPlayer p, @Nullable CrossroadsDemonEntity demon, DealTerms.Wish wish, int arg, boolean bindSoul) {
        if (wish == null || Debts.get(p).active() || !allowed(p, wish, arg)) return false;
        boolean bind = bindSoul && !wish.settledAtOnce() && CrossroadsHooks.soul.mayConvert(p);
        Vec3 at = demon != null ? demon.position() : p.position();
        if (!Wishes.grant(p, wish, arg, at)) return false;
        CrossroadsDeal deal = CrossroadsDeal.sealed(wish, arg, Debts.now(p));
        if (wish.settledAtOnce()) deal = deal.withState(CrossroadsDeal.State.FREE);
        if (bind) deal = deal.withSoulBound(true);
        Debts.set(p, deal);
        ItemStack contract = new ItemStack(AllItems.CROSSROADS_CONTRACT.get());
        ContractTerms terms = new ContractTerms(p.getUUID(), p.getGameProfile().getName(), wish.clause(arg), deal.dueAt());
        contract.set(AllDataComponents.CONTRACT.get(), wish.settledAtOnce() ? terms.withStatus(ContractTerms.PAID) : terms);
        Wishes.give(p, contract);
        ChorusRewards.award(p, "main/deal_with_the_devil");
        if (bind) CrossroadsHooks.soul.bound(p);
        if (!wish.settledAtOnce()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.sealed", wish.days).withStyle(ChatFormatting.DARK_RED), true);
        }
        if (demon != null) demon.sealed();
        return true;
    }
}
