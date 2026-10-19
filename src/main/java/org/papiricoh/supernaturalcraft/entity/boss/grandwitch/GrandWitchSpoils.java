package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Grand Witch leaves (v0.21, P6). Per hunter: the first victory (before they hold {@link #ADVANCEMENT}) the Coven Staff and
 * her bust; a rematch the bust. The Ascension shard and the heart of Vitality come on their own. Rowena then tells her allies to
 * come to her house: she will take the Mark off them there ({@link #rowenaThanks}).
 */
public final class GrandWitchSpoils {

    public static final String ADVANCEMENT = BossProgression.Boss.GRAND_WITCH.advancement;

    private GrandWitchSpoils() {
    }

    public static void drop(ServerLevel level, GrandWitchEntity boss, List<ServerPlayer> hunters, Vec3 at) {
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch()) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter)) spawn(level, at, s);
    }

    public static List<ItemStack> share(ServerPlayer hunter) {
        return AuthorWorld.done(hunter, ADVANCEMENT) ? rematch() : first();
    }

    /** The first victory: the Coven Staff and her bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.COVEN_STAFF.get()));
        out.add(new ItemStack(AllItems.GRAND_WITCH_TROPHY.get()));
        return out;
    }

    public static List<ItemStack> rematch() {
        return new ArrayList<>(List.of(new ItemStack(AllItems.GRAND_WITCH_TROPHY.get())));
    }

    /** Rowena's word to every hunter who gave her the Book: come home with me, and I'll take that Mark off you. */
    public static void rowenaThanks(List<ServerPlayer> hunters) {
        for (ServerPlayer p : hunters) {
            if (RowenaServerHandlers.relation(p).book() != RowenaRelation.Book.GIVEN) continue;
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.grand_witch.rowena_thanks")
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), false);
        }
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
