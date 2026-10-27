package org.papiricoh.supernaturalcraft.chain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.WrittenName;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * The challenge rite (v0.27): a book with an officer's name written in it, laid on the altar with a Gauntlet of Ash (to call a
 * demon or a fallen angel) or a Seal of Summons (a loyal angel). The named officer comes alone to a ring around the altar
 * ({@link DuelStage}). Against the other side it is a duel to the death; against an officer above the ritualist in their own
 * chain, a duel for its post. {@code side}: {@code hell}, {@code heaven} or a faction's id ({@code leviathan}), which officers this rite can call.
 */
public record ChainChallengeEffect(String side) implements RitualEffect {
    public static final ResourceLocation ID = SupernaturalCraft.asResource("chain_challenge");
    public static final MapCodec<ChainChallengeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("side").forGetter(ChainChallengeEffect::side)).apply(i, ChainChallengeEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null) return Component.translatable("chain.supernaturalcraft.event.rite_no_name");
        List<String> texts = books(level, altar);
        if (texts.isEmpty()) return Component.translatable("chain.supernaturalcraft.event.rite_no_name");
        Officer o = named(level, texts);
        if (o == null) return Component.translatable("chain.supernaturalcraft.event.rite_unknown", texts.getFirst());
        if (DuelStage.busy(o) || DuelStage.busy(ritualist.getUUID()) || !o.alive())
            return Component.translatable("chain.supernaturalcraft.event.rite_busy", ChainNames.name(o));
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null || precheck(level, altar, ritualist) != null) return false;
        Officer o = named(level, books(level, altar));
        var data = ChainSavedData.get(level.getServer());
        long day = ChainEvents.day(level.getServer());
        boolean same = ChainEvents.sameSide(ritualist, o);
        Officer post = ChainLife.held(data, ritualist.getUUID());
        // One's own side is called only for its post: by one who holds a lesser one in the same chain.
        if (same && (post == null || post.faction != o.faction || post.rank.ordinal() >= o.rank.ordinal()
                || !Membership.within(o.rank, Membership.cap(org.papiricoh.supernaturalcraft.allegiance.Allegiances.get(ritualist).rank()))
                && o.rank != ChainRank.LORD)) {
            ChainEvents.notice(ritualist, "rite_busy", ChainNames.name(o));
            return false;
        }
        Challenge c = data.challenge(o.rank == ChainRank.LORD && same ? Challenge.Kind.THRONE : Challenge.Kind.HUNTER_OFFICER,
                Bonds.Actor.of(ritualist.getUUID()), Bonds.Actor.of(o), !same, day);
        c.reason = same ? "post" : "rite";
        if (!DuelStage.begin(ritualist, c, o, altar.above(), DuelStage.ARRIVAL)) { c.state = Challenge.State.DONE; return false; }
        data.log(new ChainSavedData.History(day, "challenge", o.faction, o.id, "player:" + ritualist.getUUID()));
        ChainEvents.notice(ritualist, "rite_coming", ChainNames.name(o));
        return true;
    }

    /** The named officer this rite can call: alive, of flesh, on the rite's side, the first whose name the books hold. */
    private @Nullable Officer named(ServerLevel level, List<String> texts) {
        var data = ChainSavedData.get(level.getServer());
        ChainFaction camp = "hell".equals(side) ? ChainFaction.DEMON : "heaven".equals(side) ? ChainFaction.ANGEL : ChainFaction.byId(side);
        for (ChainFaction f : ChainFaction.values())
            for (Officer o : data.hierarchy(f).npcs())
                if (o.camp() == camp && texts.stream().anyMatch(t -> WrittenName.holds(List.of(t), null, ChainNames.name(o)))) return o;
        return null;
    }

    /** The words of every book among the altar's offerings (a book and quill or a signed book, title included). */
    private static List<String> books(ServerLevel level, BlockPos altar) {
        List<String> out = new ArrayList<>();
        if (!(level.getBlockEntity(altar) instanceof RitualAltarBlockEntity a)) return out;
        for (ItemStack stack : a.offerings()) {
            var writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (stack.is(Items.WRITABLE_BOOK) && writable != null)
                out.add(String.join(" ", writable.pages().stream().map(Filterable::raw).toList()));
            var written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (stack.is(Items.WRITTEN_BOOK) && written != null)
                out.add(written.title().raw() + " " + String.join(" ", written.pages().stream().map(p -> p.raw().getString()).toList()));
        }
        out.removeIf(String::isBlank);
        return out;
    }
}
