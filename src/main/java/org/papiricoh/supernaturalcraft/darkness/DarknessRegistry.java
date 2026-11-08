package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;

/**
 * v0.42 (season 11's core): registered through the shared {@code DeferredRegister}s, like a slice; {@link #init()} is called from
 * the mod's constructor. Amara's sounds and music, Casifer's and the Darkness's own.
 */
public final class DarknessRegistry {

    private DarknessRegistry() {
    }

    /** {@code supernaturalcraft:amara.<id>}. */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> AMARA_SOUNDS =
            AllSounds.registerGroup(AmaraAssets.SOUND_GROUP, AmaraAssets.SOUNDS);
    /** {@code supernaturalcraft:music.amara.phase<n>}. */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> AMARA_MUSIC =
            AllSounds.registerGroup(AmaraAssets.MUSIC_GROUP, AmaraAssets.MUSIC);
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> CASIFER_SOUNDS =
            AllSounds.registerGroup("casifer", S11Assets.SOUNDS.get("casifer"));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> DARKNESS_SOUNDS =
            AllSounds.registerGroup("darkness", S11Assets.SOUNDS.get("darkness"));

    // --- Season 11's people ---------------------------------------------------------------------------------------------
    /** Casifer: Lucifer wearing Castiel (Castiel's rig, his own texture). */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity>> CASIFER =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("casifer", () -> net.minecraft.world.entity.EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(10).build("casifer"));
    /** Chuck in season 11: the visit that shows who he is, and his part in his sister's fight. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity>> CHUCK_ALLY =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("chuck_ally", () -> net.minecraft.world.entity.EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).fireImmune().clientTrackingRange(10).build("chuck_ally"));

    /** Amara at dawn, growing up visit by visit. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity>> AMARA_VISITOR =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("amara_visitor", () -> net.minecraft.world.entity.EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 1.85f).fireImmune().clientTrackingRange(10).build("amara_visitor"));
    /** A villager the black fog took (its villager is kept and comes back when it is cured). */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.darkness.fog.FoggedVillagerEntity>> FOGGED_VILLAGER =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("fogged_villager", () -> net.minecraft.world.entity.EntityType.Builder
                    .of(org.papiricoh.supernaturalcraft.darkness.fog.FoggedVillagerEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("fogged_villager"));

    // --- Her gift ----------------------------------------------------------------------------------------------------
    /** Amara's Light: her gift at the reconciliation (a mote of dawn in black glass; it lights its bearer's way). */
    public static final net.neoforged.neoforge.registries.DeferredItem<org.papiricoh.supernaturalcraft.darkness.AmarasLightItem> AMARAS_LIGHT =
            org.papiricoh.supernaturalcraft.registry.AllItems.ITEMS.register("amaras_light",
                    () -> new org.papiricoh.supernaturalcraft.darkness.AmarasLightItem(new net.minecraft.world.item.Item.Properties()
                            .stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // --- The road's state ---------------------------------------------------------------------------------------------
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Darkness>> DARKNESS =
            org.papiricoh.supernaturalcraft.registry.AllAttachments.ATTACHMENT_TYPES.register("darkness",
                    () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> Darkness.EMPTY)
                            .serialize(Darkness.CODEC).copyOnDeath().build());

    public static List<Map<String, DeferredHolder<SoundEvent, SoundEvent>>> soundGroups() {
        return List.of(AMARA_SOUNDS, AMARA_MUSIC, CASIFER_SOUNDS, DARKNESS_SOUNDS);
    }

    /** {@code supernaturalcraft:amara.<id>}. */
    public static SoundEvent amara(String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = AMARA_SOUNDS.get(AmaraAssets.SOUND_GROUP + "." + id);
        if (h == null) throw new IllegalArgumentException("No Amara sound " + id);
        return h.get();
    }

    /** {@code supernaturalcraft:music.amara.phase<n>}, n = 1..6. */
    public static SoundEvent music(int phase) {
        return AMARA_MUSIC.get(AmaraAssets.MUSIC_GROUP + ".phase" + phase).get();
    }

    public static void init() {
    }
}
