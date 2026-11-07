package org.papiricoh.supernaturalcraft.british;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

import java.util.Optional;

/**
 * Season 12 on the server (v0.41): what the hunter's answers to Hess do ({@link BritishDialogue.Act}), the British kit, the
 * betrayal of a hunter who joined, and the two compasses (the dossier's to the compound, the trace's to Kelly's safe house).
 */
public final class BritishServer {

    private BritishServer() {
    }

    /** Called once at start-up: Hess's ultimatum comes back through the shared dialogue. */
    public static void registerDialogues() {
        AllegianceDialogue.register(BritishDialogue.ULTIMATUM, (player, speaker, node, option) -> {
            if (speaker instanceof HessEnvoyEntity h && player.getUUID().equals(h.hunter()) && !h.leaving()) ultimatum(player, h, node, option);
        });
    }

    /** Hess's answer handler (public for tests: fake players answer by hand). */
    public static void ultimatum(ServerPlayer p, HessEnvoyEntity hess, String node, String option) {
        BritishDialogue.Turn t = BritishDialogue.answer(node, option);
        switch (t.act()) {
            case JOIN -> join(p);
            case REFUSE -> refuse(p);
            case DONE -> {
                hess.triggerAnim("action", "leave");
                hess.leave(16);
                return;
            }
            default -> {
            }
        }
        if (t.next() != null) hess.say(p, t.next());
        else hess.leave(16);
    }

    /** The hunter works with the British for now: their kit, and the road to the nephilim. */
    public static void join(ServerPlayer p) {
        long today = BritishOrder.today(p);
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.JOINED, today));
        handKit(p);
        Nephilims.advance(p, NephilimRules.Step.ULTIMATUM);
        tell(p, "message.supernaturalcraft.british.joined", ChatFormatting.GOLD);
    }

    /** The hunter turned Hess down (or said nothing): Ketch will come by night. */
    public static void refuse(ServerPlayer p) {
        long today = BritishOrder.today(p);
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.REFUSED, today));
        Nephilims.advance(p, NephilimRules.Step.ULTIMATUM);
        tell(p, "message.supernaturalcraft.british.refused", ChatFormatting.DARK_RED);
        if (Legacies.member(p)) tell(p, "message.supernaturalcraft.british.bunker_threatened", ChatFormatting.DARK_AQUA);
    }

    /** London has decided the hunter is a liability: they are written off, and Ketch is sent anyway. */
    public static void betray(ServerPlayer p) {
        long today = BritishOrder.today(p);
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.BETRAYED, today));
        tell(p, "message.supernaturalcraft.british.betrayed", ChatFormatting.DARK_RED);
    }

    /** The British starter kit (once). */
    public static void handKit(ServerPlayer p) {
        if (BritishOrder.get(p).has(British.KIT_GIVEN)) return;
        BritishOrder.update(p, b -> b.with(British.KIT_GIVEN));
        give(p, new ItemStack(BritishRegistry.BRITISH_PISTOL.get()));
        give(p, new ItemStack(BritishRegistry.ENCHANTED_ROUND.get(), BritishRules.KIT_ROUNDS));
        give(p, new ItemStack(BritishRegistry.HEX_GRENADE.get(), BritishRules.KIT_GRENADES));
    }

    /** Ketch's dossier read: the compound is raised (if it is not yet) and a compass points at it. */
    public static boolean readDossier(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        BlockPos at = BritishSites.ensureCompound(overworld);
        BritishOrder.update(p, b -> b.with(British.DOSSIER_READ));
        give(p, compass(at, "item.supernaturalcraft.british_compass", ChatFormatting.DARK_AQUA));
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.british.compound", at.getX(), at.getZ()).withStyle(ChatFormatting.DARK_AQUA));
        return true;
    }

    /** The nephilim's trace: the safe house is raised (if it is not yet) and a compass points at it. */
    public static void handOverTrace(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        BlockPos at = BritishSites.ensureSafehouse(overworld);
        Nephilims.advance(p, NephilimRules.Step.TRACING);
        give(p, compass(at, "item.supernaturalcraft.nephilim_compass", ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.british.safehouse", at.getX(), at.getZ()).withStyle(ChatFormatting.GOLD));
    }

    static ItemStack compass(BlockPos at, String name, ChatFormatting style) {
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, at)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable(name).withStyle(style));
        return compass;
    }

    static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    static void tell(ServerPlayer p, String key, ChatFormatting style) {
        p.sendSystemMessage(Component.translatable(key).withStyle(style));
    }
}
