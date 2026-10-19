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
    /** Whose a taglock (or any personal component) is (v0.21): the bowl's target. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.bowl.spell.Taglock>> TAGLOCK =
            DATA_COMPONENTS.registerComponentType("taglock", b -> b.persistent(org.papiricoh.supernaturalcraft.bowl.spell.Taglock.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.bowl.spell.Taglock.STREAM_CODEC));
    /**
     * Which of a grimoire's pages were written in hexed ink, and of what school (v0.21): one entry per page, {@code none} for
     * plain Enochian ink. A spell of that school cast from such a page is stronger ({@code magic.spell.InkBonus}).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<org.papiricoh.supernaturalcraft.magic.school.School>>> PAGE_INKS =
            DATA_COMPONENTS.registerComponentType("page_inks", b -> b.persistent(org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs.CODEC.listOf())
                    .networkSynchronized(org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs.STREAM_CODEC.apply(
                            net.minecraft.network.codec.ByteBufCodecs.list(org.papiricoh.supernaturalcraft.magic.spell.SpellBook.PAGES))));
    /** The school a reagent is bound to (v0.21): hexed ink. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.magic.school.School>> SCHOOL =
            DATA_COMPONENTS.registerComponentType("school", b -> b.persistent(org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs.STREAM_CODEC));
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

    // --- v0.15: the power curve -----------------------------------------------------------------------
    /** Ascension of a weapon or a piece of armour, 0-5: raised one tier at a time at the Hellforge with a shard. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ASCENSION =
            DATA_COMPONENTS.registerComponentType("ascension", b -> b.persistent(clamped(0, org.papiricoh.supernaturalcraft.balance.ProgressionScale.MAX_TIER))
                    .networkSynchronized(clampedStream(0, org.papiricoh.supernaturalcraft.balance.ProgressionScale.MAX_TIER)));

    // --- v0.17: the Men of Letters ----------------------------------------------------------------------
    /** What a stack of field notes is about: a research topic subject ({@code creature:<entity id>}, {@code arcane}, {@code relic}, {@code place}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_TOPIC =
            DATA_COMPONENTS.registerComponentType("note_topic", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** A cursed object's roll and whether it has been identified. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData>> ARTIFACT =
            DATA_COMPONENTS.registerComponentType("artifact", b -> b.persistent(org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData.STREAM_CODEC));
    /** Which of its owner's cases a case file is ({@code CaseFile#index}). */
    /** What a Heavenly Form says (v0.18): its cabinet, when it was issued, to whom. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.entity.boss.zachariah.HeavenlyForm>> HEAVENLY_FORM =
            DATA_COMPONENTS.registerComponentType("heavenly_form", b -> b.persistent(org.papiricoh.supernaturalcraft.entity.boss.zachariah.HeavenlyForm.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.entity.boss.zachariah.HeavenlyForm.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CASE_INDEX =
            DATA_COMPONENTS.registerComponentType("case_index", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Where a Letters compass was set to point at a war table (v0.19): a pin's name and column. None = the open case. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin>> COMPASS_TARGET =
            DATA_COMPONENTS.registerComponentType("compass_target", b -> b.persistent(org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin.STREAM_CODEC));

    /** What Rowena's Grimoire remembers (v0.21, P6): the last aimed bowl spell its bearer spoke at a bowl. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.supernaturalcraft.entity.boss.rowena.GrimoireEcho>> GRIMOIRE_ECHO =
            DATA_COMPONENTS.registerComponentType("grimoire_echo", b -> b.persistent(org.papiricoh.supernaturalcraft.entity.boss.rowena.GrimoireEcho.CODEC)
                    .networkSynchronized(org.papiricoh.supernaturalcraft.entity.boss.rowena.GrimoireEcho.STREAM_CODEC));

    public static void init() {
    }
}
