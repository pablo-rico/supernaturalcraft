package org.papiricoh.supernaturalcraft.coven.church;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.ConsecratedGround;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

import java.util.List;

/**
 * Ritual effect {@code supernaturalcraft:consecrate_church} (v0.21): performed on an abandoned church's ground (the altar set
 * where its broken altar stands), it blesses a circle of {@code radius} as holy ground ({@link ConsecratedGround}) and makes the
 * church a sanctuary no demon or spirit may enter ({@link Sanctuaries}). The first rite in each church also leaves an Enochian
 * sigil page on the altar (tier 3 at most) and grants {@code main/consecrated_church}. Refused (spending nothing) outside a
 * church or in one already cleansed.
 */
public record ConsecrateChurchEffect(int radius) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("consecrate_church");
    public static final MapCodec<ConsecrateChurchEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(4, 32).optionalFieldOf("radius", 16).forGetter(ConsecrateChurchEffect::radius)
    ).apply(i, ConsecrateChurchEffect::new));
    public static final String ADVANCEMENT = "main/consecrated_church";

    @Override
    public ResourceLocation type() {
        return ID;
    }

    /** The church whose ground holds {@code pos}, or null. */
    public static @Nullable StructureStart church(ServerLevel level, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(ChurchStructure.KEY);
        if (s == null) return null;
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, s);
        if (start == null || !start.isValid()) {
            // The altar may stand just outside a piece's box (in the chancel's air): look a block down and round.
            start = level.structureManager().getStructureAt(pos, s);
        }
        return start != null && start.isValid() ? start : null;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        StructureStart start = church(level, altar);
        if (start == null) return Component.translatable("message.supernaturalcraft.church.not_a_church").withStyle(ChatFormatting.GRAY);
        if (Sanctuaries.get(level).consecrated(start.getChunkPos().toLong())) {
            return Component.translatable("message.supernaturalcraft.church.already").withStyle(ChatFormatting.GRAY);
        }
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        StructureStart start = church(level, altar);
        if (start == null) return false;
        long key = start.getChunkPos().toLong();
        Sanctuaries sanctuaries = Sanctuaries.get(level);
        boolean first = !sanctuaries.consecrated(key);
        ConsecratedGround.get(level).consecrate(altar, radius);
        sanctuaries.add(new Sanctuaries.Sanctuary(altar.immutable(), radius, key));
        level.playSound(null, altar, AllSounds.ALLEGIANCE_HEAL.get(), SoundSource.BLOCKS, 1.5f, 0.8f);
        if (first) {
            ResourceLocation sigil = enochianSigil(level);
            if (sigil != null) {
                ItemStack page = SigilPageItem.of(AllItems.SIGIL_PAGE.get(), sigil);
                level.addFreshEntity(new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, page));
            }
            if (ritualist != null) ChorusRewards.award(ritualist, ADVANCEMENT);
        }
        if (ritualist != null) {
            ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.church.consecrated", radius)
                    .withStyle(ChatFormatting.GOLD), false);
        }
        return true;
    }

    /** A random Enochian sigil of tier 3 at most (lower tiers likelier), or null if the datapack has none. */
    static @Nullable ResourceLocation enochianSigil(ServerLevel level) {
        var registry = level.registryAccess().registryOrThrow(SNRegistries.SIGIL);
        List<Holder.Reference<SigilComponent>> pool = registry.holders()
                .filter(h -> h.value().school() == School.ENOCHIAN && h.value().tier() <= 3).toList();
        if (pool.isEmpty()) return null;
        int total = 0;
        for (var h : pool) total += 4 - h.value().tier();
        int roll = level.random.nextInt(total);
        for (var h : pool) {
            roll -= 4 - h.value().tier();
            if (roll < 0) return h.key().location();
        }
        return pool.getLast().key().location();
    }
}
