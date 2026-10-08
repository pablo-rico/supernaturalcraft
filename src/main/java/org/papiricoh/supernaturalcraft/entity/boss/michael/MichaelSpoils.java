package org.papiricoh.supernaturalcraft.entity.boss.michael;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger;

import java.util.List;

/**
 * What Michael leaves, every victory and for every hunter who fought him: the Lance of Michael, his Grace and his
 * likeness, and one piece of the General's armour that hunter has not been given yet (helmet, chestplate, leggings,
 * boots: the full set takes four victories). Each hunter's pieces are kept in their {@link HeavenLedger}.
 */
public final class MichaelSpoils {

    private MichaelSpoils() {
    }

    /** The armour pieces in the order they are given. */
    public static List<Item> pieces() {
        return List.of(AllItems.GENERAL_HELMET.get(), AllItems.GENERAL_CHESTPLATE.get(), AllItems.GENERAL_LEGGINGS.get(),
                AllItems.GENERAL_BOOTS.get());
    }

    public static void drop(ServerLevel level, MichaelEntity boss, Vec3 at) {
        drop(level, boss.challengers(), at);
    }

    /** Everyone's share at {@code at}; with no hunter to credit, one share without armour. */
    public static void drop(ServerLevel level, List<ServerPlayer> hunters, Vec3 at) {
        if (hunters.isEmpty()) {
            always(level, at);
            return;
        }
        for (ServerPlayer hunter : hunters) {
            always(level, at);
            spawn(level, at, nextArmourPiece(hunter));
        }
    }

    private static void always(ServerLevel level, Vec3 at) {
        spawn(level, at, new ItemStack(AllItems.MICHAEL_LANCE.get()));
        spawn(level, at, new ItemStack(AllItems.MICHAELS_GRACE.get()));
        spawn(level, at, new ItemStack(AllItems.MICHAEL_TROPHY.get()));
    }

    /** The next piece of the General's armour {@code hunter} has not been given yet, written down as given. */
    public static ItemStack nextArmourPiece(ServerPlayer hunter) {
        HeavenLedger ledger = hunter.getData(AllAttachments.HEAVEN);
        int piece = ledger.nextPiece();
        hunter.setData(AllAttachments.HEAVEN, ledger.give(piece));
        return new ItemStack(pieces().get(piece));
    }

    static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
