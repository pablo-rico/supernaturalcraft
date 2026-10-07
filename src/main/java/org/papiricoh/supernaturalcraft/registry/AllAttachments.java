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

    public static void init() {
    }
}
