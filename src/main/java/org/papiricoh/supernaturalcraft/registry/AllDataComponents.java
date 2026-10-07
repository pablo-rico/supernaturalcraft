package org.papiricoh.supernaturalcraft.registry;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;

public class AllDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SupernaturalCraft.MODID);

    /** The six pages of a grimoire. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellBook>> SPELL_BOOK =
            DATA_COMPONENTS.registerComponentType("spell_book", b -> b.persistent(SpellBook.CODEC).networkSynchronized(SpellBook.STREAM_CODEC));

    /** The single spell inscribed on a scroll. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Spell>> SCROLL_SPELL =
            DATA_COMPONENTS.registerComponentType("scroll_spell", b -> b.persistent(Spell.CODEC).networkSynchronized(Spell.STREAM_CODEC));

    /** Which sigil a loose page teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SIGIL_PAGE =
            DATA_COMPONENTS.registerComponentType("sigil_page", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Rounds loaded in the Colt. Clamped rather than rejected, so six-round guns from before load as full. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLT_AMMO =
            DATA_COMPONENTS.registerComponentType("colt_ammo", b -> b.persistent(clamped(0, ColtItem.CAPACITY))
                    .networkSynchronized(clampedStream(0, ColtItem.CAPACITY)));

    /** The Colt's chamber under the hammer: the cylinder turns a fifth with every shot. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLT_CHAMBER =
            DATA_COMPONENTS.registerComponentType("colt_chamber", b -> b.persistent(clamped(0, ColtItem.CAPACITY - 1))
                    .networkSynchronized(clampedStream(0, ColtItem.CAPACITY - 1)));

    /** A Colt reload in progress. Synced so every client animates it, never saved. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ColtReload.State>> COLT_RELOAD =
            DATA_COMPONENTS.registerComponentType("colt_reload", b -> b.networkSynchronized(ColtReload.State.STREAM_CODEC));

    private static Codec<Integer> clamped(int min, int max) {
        return Codec.INT.xmap(v -> Mth.clamp(v, min, max), v -> v);
    }

    private static StreamCodec<ByteBuf, Integer> clampedStream(int min, int max) {
        return ByteBufCodecs.VAR_INT.map(v -> Mth.clamp(v, min, max), v -> v);
    }

    /** Runes graved into a weapon at the Hellforge. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.weapon.RuneSet>> RUNES =
            DATA_COMPONENTS.registerComponentType("runes", b -> b.persistent(org.papiricoh.supernaturalcraft.weapon.RuneSet.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.weapon.RuneSet.STREAM_CODEC));

    /** A hungry weapon's appetite and master. */
    /** Light drunk by Penumbra, 0-100. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PENUMBRA_LIGHT =
            DATA_COMPONENTS.registerComponentType("penumbra_light", b -> b.persistent(com.mojang.serialization.Codec.intRange(0, 100))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.weapon.curse.CurseState>> CURSE =
            DATA_COMPONENTS.registerComponentType("curse", b -> b.persistent(org.papiricoh.supernaturalcraft.weapon.curse.CurseState.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.weapon.curse.CurseState.STREAM_CODEC));

    // --- v0.8: the spell bowl ------------------------------------------------------------------
    /** What a spell bowl holds, carried on the item when the bowl is picked up. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.bowl.BowlContents>> BOWL_CONTENTS =
            DATA_COMPONENTS.registerComponentType("bowl_contents", b -> b.persistent(org.papiricoh.supernaturalcraft.bowl.BowlContents.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.bowl.BowlContents.STREAM_CODEC));
    /** Which bowl spell a spell page teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BOWL_SPELL =
            DATA_COMPONENTS.registerComponentType("bowl_spell", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.bowl.spell.BloodSample>> BLOOD_SAMPLE =
            DATA_COMPONENTS.registerComponentType("blood_sample", b -> b.persistent(org.papiricoh.supernaturalcraft.bowl.spell.BloodSample.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.bowl.spell.BloodSample.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.bowl.spell.PetBond>> PET_BOND =
            DATA_COMPONENTS.registerComponentType("pet_bond", b -> b.persistent(org.papiricoh.supernaturalcraft.bowl.spell.PetBond.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.bowl.spell.PetBond.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.hex.HexBag>> HEX_BAG =
            DATA_COMPONENTS.registerComponentType("hex_bag", b -> b.persistent(org.papiricoh.supernaturalcraft.hex.HexBag.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.hex.HexBag.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.crossroads.ContractTerms>> CONTRACT =
            DATA_COMPONENTS.registerComponentType("contract", b -> b.persistent(org.papiricoh.supernaturalcraft.crossroads.ContractTerms.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.crossroads.ContractTerms.STREAM_CODEC));

    // --- v0.10: the Author ------------------------------------------------------------------------------
    /** What a "The End" manuscript says. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.author.Manuscript>> MANUSCRIPT =
            DATA_COMPONENTS.registerComponentType("manuscript", b -> b.persistent(org.papiricoh.supernaturalcraft.author.Manuscript.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.author.Manuscript.STREAM_CODEC));
    /** What the Author's Pen rewrites when used (0 biome, 1 weather and hour, 2 a creature). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PEN_MODE =
            DATA_COMPONENTS.registerComponentType("pen_mode", b -> b.persistent(clamped(0, 2)).networkSynchronized(clampedStream(0, 2)));

    public static void init() {
    }
}
