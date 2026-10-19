package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What Rowena leaves (v0.21, P6). Per hunter: the first victory (before they hold {@link #ADVANCEMENT}) her Grimoire and her bust; a
 * rematch the bust. The Ascension shard and the heart of Vitality come on their own. Then the Grand Witch walks out of the
 * smoke for whoever kept the Book ({@link #grandWitchComes}): she owes them the Mark's removal.
 */
public final class RowenaSpoils {

    public static final String ADVANCEMENT = BossProgression.Boss.ROWENA.advancement;

    private RowenaSpoils() {
    }

    public static void drop(ServerLevel level, RowenaEntity boss, List<ServerPlayer> hunters, Vec3 at) {
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch()) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter)) spawn(level, at, s);
    }

    public static List<ItemStack> share(ServerPlayer hunter) {
        return AuthorWorld.done(hunter, ADVANCEMENT) ? rematch() : first();
    }

    /** The first victory: her Grimoire and her bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.ROWENAS_GRIMOIRE.get()));
        out.add(new ItemStack(AllItems.ROWENA_TROPHY.get()));
        return out;
    }

    /** A rematch: her bust. */
    public static List<ItemStack> rematch() {
        return new ArrayList<>(List.of(new ItemStack(AllItems.ROWENA_TROPHY.get())));
    }

    /**
     * The Grand Witch comes to the battlefield for every hunter there who kept the Book (one of her, near {@code at}), and
     * tells them so. @return her, or null if none of them kept it
     */
    public static GrandWitchNpcEntity grandWitchComes(ServerLevel level, Vec3 at, List<ServerPlayer> hunters) {
        ServerPlayer keeper = null;
        for (ServerPlayer p : hunters) {
            if (RowenaServerHandlers.relation(p).book() == RowenaRelation.Book.KEPT) {
                keeper = p;
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.grand_witch.arrives")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
            }
        }
        return keeper == null ? null : GrandWitchNpcEntity.call(level, at, keeper);
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
