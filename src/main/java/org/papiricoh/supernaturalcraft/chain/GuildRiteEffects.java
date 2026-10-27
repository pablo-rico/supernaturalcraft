package org.papiricoh.supernaturalcraft.chain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.WrittenName;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The guilds' two rites (v0.29): forging a standard, and binding a named officer to one's guild. */
public final class GuildRiteEffects {
    private GuildRiteEffects() {
    }

    /**
     * The founding rite: the banner on the altar (renamed on an anvil: that is the guild's name; its colour is the guild's)
     * becomes a Guild Standard, which founds the guild where it is planted. Refused without a name, or if the ritualist already
     * serves a guild.
     */
    public record Found() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("found_guild");
        public static final MapCodec<Found> CODEC = MapCodec.unit(new Found());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (ritualist == null) return Component.translatable("chain.supernaturalcraft.event.guild_bad_name");
            if (GuildLife.post(ChainSavedData.get(level.getServer()), ritualist.getUUID()) != null)
                return Component.translatable("chain.supernaturalcraft.event.guild_already_member");
            ItemStack banner = banner(level, altar, ritualist);
            if (banner.isEmpty() || GuildRules.validName(banner.getHoverName().getString()) == null || !banner.has(DataComponents.CUSTOM_NAME))
                return Component.translatable("chain.supernaturalcraft.event.guild_bad_name");
            return null;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (precheck(level, altar, ritualist) != null) return false;
            ItemStack banner = banner(level, altar, ritualist);
            DyeColor dye = banner.getItem() instanceof BannerItem b ? b.getColor() : DyeColor.WHITE;
            ItemStack standard = GuildStandardItem.fresh(GuildRules.validName(banner.getHoverName().getString()), dye);
            level.addFreshEntity(new ItemEntity(level, altar.getX() + .5, altar.getY() + 1.2, altar.getZ() + .5, standard));
            return true;
        }

        @Override
        public ItemStack displayResult() {
            return new ItemStack(AllItems.GUILD_STANDARD.get());
        }

        /** The renamed banner the rite is worked with: the activator it put up, else one among the offerings. */
        private static ItemStack banner(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (!(level.getBlockEntity(altar) instanceof RitualAltarBlockEntity a)) return ItemStack.EMPTY;
            if (a.activator().getItem() instanceof BannerItem) return a.activator();
            if (ritualist != null && ritualist.getMainHandItem().getItem() instanceof BannerItem) return ritualist.getMainHandItem();
            for (ItemStack stack : a.offerings()) if (stack.getItem() instanceof BannerItem) return stack;
            return ItemStack.EMPTY;
        }
    }

    /**
     * The binding rite: a book with an officer's name (any chain but SucroCorp's, up to rank IV, never a Lord) and the rite's
     * price bind it to the ritualist's guild at its own rank ({@link GuildLife#bind}). Only a guild's Lord or Commander may.
     */
    public record Bind() implements RitualEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("bind_officer");
        public static final MapCodec<Bind> CODEC = MapCodec.unit(new Bind());

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (ritualist == null || GuildLife.hirer(ritualist) == null) return Component.translatable("chain.supernaturalcraft.event.guild_no_right");
            var texts = ChainChallengeEffect.books(level, altar);
            if (texts.isEmpty()) return Component.translatable("chain.supernaturalcraft.event.rite_no_name");
            Officer o = named(level, texts);
            if (o == null) return Component.translatable("chain.supernaturalcraft.event.rite_unknown", texts.getFirst());
            if (o.faction == GuildLife.hirer(ritualist) || DuelStage.busy(o))
                return Component.translatable("chain.supernaturalcraft.event.rite_busy", ChainNames.name(o));
            if (!ChainSavedData.get(level.getServer()).hierarchy(GuildLife.hirer(ritualist)).npcFree(o.rank))
                return Component.translatable("chain.supernaturalcraft.event.guild_full");
            return null;
        }

        @Override
        public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
            if (ritualist == null || precheck(level, altar, ritualist) != null) return false;
            Officer o = named(level, ChainChallengeEffect.books(level, altar));
            return o != null && GuildLife.bind(level.getServer(), o, ritualist.getUUID()) != null;
        }

        /** The first officer whose name the books hold that a rite may bind. */
        private static @Nullable Officer named(ServerLevel level, java.util.List<String> texts) {
            var data = ChainSavedData.get(level.getServer());
            for (ChainFaction f : ChainFaction.values())
                for (Officer o : data.hierarchy(f).npcs())
                    if (GuildRules.bindable(o) && texts.stream().anyMatch(t -> WrittenName.holds(java.util.List.of(t), null, ChainNames.name(o)))) return o;
            return null;
        }
    }
}
