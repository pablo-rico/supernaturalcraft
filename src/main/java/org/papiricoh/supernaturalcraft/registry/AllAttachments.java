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

    public static void init() {
    }
}
