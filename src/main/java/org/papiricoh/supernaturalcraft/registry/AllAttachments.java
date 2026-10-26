package org.papiricoh.supernaturalcraft.registry;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;

import java.util.function.Supplier;

public class AllAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SupernaturalCraft.MODID);

    /** Mana and learned sigils. Survives death: knowledge isn't lost when the body is. */
    public static final Supplier<AttachmentType<ArcanaData>> ARCANA = ATTACHMENT_TYPES.register("arcana",
            () -> AttachmentType.builder(ArcanaData::new).serialize(ArcanaData.CODEC).copyOnDeath().build());

    /** Magical mastery (v0.21): XP in each of the four schools. Separate from the arcana; survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.magic.mastery.MasteryData>> MASTERY = ATTACHMENT_TYPES.register("mastery",
            () -> AttachmentType.builder(org.papiricoh.supernaturalcraft.magic.mastery.MasteryData::new)
                    .serialize(org.papiricoh.supernaturalcraft.magic.mastery.MasteryData.CODEC).copyOnDeath().build());

    /** The Mark of Cain (v0.21): borne or not, and its Thirst. Survives death (that is the curse). */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.mark.MarkData>> MARK = ATTACHMENT_TYPES.register("mark",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.mark.MarkData.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.mark.MarkData.CODEC).copyOnDeath().build());

    /** Hell's Torment (0-1): grows while in Hell, fades outside it. Only visions and whispers. */
    public static final Supplier<AttachmentType<Float>> TORMENT = ATTACHMENT_TYPES.register("torment",
            () -> AttachmentType.builder(() -> 0f).serialize(com.mojang.serialization.Codec.FLOAT).build());

    /** A player's standing with the crossroads: an open deal survives death (that is the point). */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal>> CROSSROADS_DEAL = ATTACHMENT_TYPES.register("crossroads_deal",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.CODEC).copyOnDeath().build());
    /** A creature held in place by a bowl's Binding. */
    public static final Supplier<AttachmentType<java.util.Optional<org.papiricoh.supernaturalcraft.bowl.spell.Binding>>> BINDING = ATTACHMENT_TYPES.register("binding",
            () -> AttachmentType.<java.util.Optional<org.papiricoh.supernaturalcraft.bowl.spell.Binding>>builder(java.util.Optional::empty)
                    .serialize(org.papiricoh.supernaturalcraft.bowl.spell.Binding.CODEC.optionalFieldOf("binding").codec()).build());
    /**
     * A familiar's witch (v0.21, the {@code familiar_bond} spell): the player a tamed cat or parrot is bound to. P3 builds the
     * coven's familiars on it.
     */
    public static final Supplier<AttachmentType<java.util.Optional<java.util.UUID>>> FAMILIAR = ATTACHMENT_TYPES.register("familiar",
            () -> AttachmentType.<java.util.Optional<java.util.UUID>>builder(java.util.Optional::empty)
                    .serialize(net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("witch").codec()).build());
    /** Marks a tamed animal wearing a collar: the pet ledger keeps track of it. */
    public static final Supplier<AttachmentType<Boolean>> COLLARED = ATTACHMENT_TYPES.register("collared",
            () -> AttachmentType.builder(() -> false).serialize(com.mojang.serialization.Codec.BOOL).build());

    /** The Hunter's Journal: creatures seen and slain, items held, entries read and marked, spell designs. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.journal.HunterLog>> HUNTER_LOG = ATTACHMENT_TYPES.register("hunter_log",
            () -> AttachmentType.builder(org.papiricoh.supernaturalcraft.journal.HunterLog::new)
                    .serialize(org.papiricoh.supernaturalcraft.journal.HunterLog.CODEC).copyOnDeath().build());

    /** What Heaven owes a hunter (v0.12): the General's armour pieces Michael has left them, his Grace, their first flight. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger>> HEAVEN = ATTACHMENT_TYPES.register("heaven",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger.CODEC).copyOnDeath().build());

    /** Whose side a player is on (v0.13): human, angel or demon, their rank and their Grace or Corruption. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.allegiance.Allegiance>> ALLEGIANCE = ATTACHMENT_TYPES.register("allegiance",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.allegiance.Allegiance.HUMAN)
                    .serialize(org.papiricoh.supernaturalcraft.allegiance.Allegiance.CODEC).copyOnDeath().build());

    /** What the Trickster has done to a hunter (v0.14): pranks noticed, the day of the last, victories over him. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.trickster.TricksterLedger>> TRICKSTER = ATTACHMENT_TYPES.register("trickster",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.trickster.TricksterLedger.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.trickster.TricksterLedger.CODEC).copyOnDeath().build());

    /** On a mob (v0.14): the game time its party hat comes off (0 = none). Sent to the clients with {@code GabrielFxPayload.HAT}. */
    public static final Supplier<AttachmentType<Long>> PARTY_HAT = ATTACHMENT_TYPES.register("party_hat",
            () -> AttachmentType.builder(() -> 0L).serialize(com.mojang.serialization.Codec.LONG).build());

    /** The great enemies whose first defeat has given a hunter its hearts (v0.15). Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.balance.Vitality>> VITALITY = ATTACHMENT_TYPES.register("vitality",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.balance.Vitality.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.balance.Vitality.CODEC).copyOnDeath().build());

    /** A hunter's place in the Men of Letters (v0.17): rank, Henry's offer, their cases. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.legacy.Legacy>> LEGACY = ATTACHMENT_TYPES.register("legacy",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.legacy.Legacy.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.legacy.Legacy.CODEC).copyOnDeath().build());

    /** What a hunter has researched in the bunker (v0.17): topics, creature files, research under way, generated spells. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.legacy.Archive>> ARCHIVE = ATTACHMENT_TYPES.register("archive",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.legacy.Archive.EMPTY)
                    .serialize(org.papiricoh.supernaturalcraft.legacy.Archive.CODEC).copyOnDeath().build());

    /** A hunter's standing in Heaven (v0.18): the way back, their plot, their home, who may visit. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding>> HEAVEN_STANDING = ATTACHMENT_TYPES.register("heaven_standing",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding.CODEC).copyOnDeath().build());

    /** A hunter's memories (v0.18): the timeline their Heaven is built from, and the ones they have gathered. Survives death. */
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.memory.MemoryLog>> MEMORY_LOG = ATTACHMENT_TYPES.register("memory_log",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.memory.MemoryLog.EMPTY)
                    .serialize(org.papiricoh.supernaturalcraft.memory.MemoryLog.CODEC).copyOnDeath().build());

    /**
     * A hunter's dealings with Rowena (v0.21): trust, whether they have met, whether she gave the clue to Cain's farm, and what
     * they did with the Book of the Damned (P6). Server only (the client needs none of it yet). Survives death.
     */
    /** The last world day (day index) Cain gave this hunter a jar of his honey (v0.21); -1 never. */
    public static final Supplier<AttachmentType<Long>> CAIN_HONEY_DAY = ATTACHMENT_TYPES.register("cain_honey_day",
            () -> AttachmentType.builder(() -> -1L).serialize(com.mojang.serialization.Codec.LONG).copyOnDeath().build());
    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation>> ROWENA = ATTACHMENT_TYPES.register("rowena",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation.NONE)
                    .serialize(org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<org.papiricoh.supernaturalcraft.chain.ChainMark>> CHAIN_MARK = ATTACHMENT_TYPES.register("chain_mark",
            () -> AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.chain.ChainMark.EMPTY)
                    .serialize(org.papiricoh.supernaturalcraft.chain.ChainMark.CODEC).copyOnDeath().build());

    public static void init() {
    }
}
