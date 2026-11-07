package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * What holding a seal gives (v0.40): to every hunter who was there (and the one it called), the day it won on the count (once
 * per event), their record, the family's spoil ({@link #spoil}: two, three if they held it alone), the first time they hold one
 * of a family the page of its sigil ({@link #page}), the advancements and, for an angel, the Host's regard
 * ({@link SealRegard}). A seal let break goes on their record, and a demon who let it break wins the Pit's regard.
 */
public final class SealSpoils {

    private SealSpoils() {
    }

    /** The spoil of each family: the shroud of a Witness, a feather burnt off an angel, a plague's ash, the ash of the altar. */
    public static Item spoil(SealFamily family) {
        return switch (family) {
            case DEAD -> AllItems.WITNESS_SHROUD.get();
            case ANGELS -> AllItems.SCORCHED_FEATHER.get();
            case PLAGUES -> AllItems.PLAGUE_ASH.get();
            case RITES -> AllItems.SACRIFICIAL_ASH.get();
        };
    }

    /** The sigil a family teaches the first time a hunter holds one of its seals. */
    public static ResourceLocation page(SealFamily family) {
        return SupernaturalCraft.asResource(switch (family) {
            case DEAD -> "banishing";
            case ANGELS -> "smite";
            case PLAGUES -> "mend";
            case RITES -> "exorcise";
        });
    }

    /** The seal held: every share. */
    public static void held(ServerLevel level, SealEvent e, List<ServerPlayer> hunters) {
        SealSavedData data = SealSavedData.get(level.getServer());
        boolean delayed = data.delay();
        Component kind = Component.translatable(e.kind.nameKey());
        int count = hunters.size() <= 1 ? 3 : 2;
        for (ServerPlayer p : hunters) {
            SealRecord before = SealWorld.record(p);
            boolean firstOfFamily = true;
            for (SealKind k : SealKind.of(e.kind.family)) if (before.heldKind(k)) firstOfFamily = false;
            SealRecord after = before.held(e.kind);
            SealWorld.setRecord(p, after);
            give(p, new ItemStack(spoil(e.kind.family), count));
            if (firstOfFamily) give(p, SigilPageItem.of(AllItems.SIGIL_PAGE.get(), page(e.kind.family)));
            ChorusRewards.award(p, SealWorld.HELD);
            if (e.kind == SealKind.WITNESSES) ChorusRewards.award(p, SealWorld.WITNESSES);
            if (Kin.isAngel(p)) SealRegard.host(p);
            p.sendSystemMessage(Component.translatable(delayed ? "message.supernaturalcraft.seal.held" : "message.supernaturalcraft.seal.held_late", kind)
                    .withStyle(ChatFormatting.GOLD));
            if (e.kind.family == SealFamily.ANGELS && after.angelsDefended() >= SealRules.URIEL_AFTER && after.envoy() == 0) {
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.seal.uriel_hint").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }
        }
        if (delayed) SealWorld.syncAll(level.getServer());
    }

    /** The seal broke under these hunters' noses: their record, and the Pit's thanks to a demon among them. */
    public static void broken(ServerLevel level, SealEvent e, List<ServerPlayer> hunters) {
        for (ServerPlayer p : hunters) {
            SealWorld.setRecord(p, SealWorld.record(p).letBroken());
            if (Kin.isDemon(p)) SealRegard.pit(p);
        }
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
