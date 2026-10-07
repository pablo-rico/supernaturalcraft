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

    public static void init() {
    }
}
