package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The Apocalypse's sound definitions (v0.40), called from {@code SNSoundDefinitions}: each event {@code <group>.<id>} points at
 * its own file ({@code sounds/<group>/<id>.ogg}) once one exists, until then at a re-pitched vanilla event ({@link #FALLBACK}).
 * Rerun {@code runData} after files land. Subtitles: {@code ApocalypseLang.SUBTITLES} and {@code SealLang.SUBTITLES}.
 */
public final class ApocalypseSoundData {

    private ApocalypseSoundData() {
    }

    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("alastair.hum", new Object[]{"entity.allay.ambient_without_item", 0.5f}),
            Map.entry("alastair.chains", new Object[]{"block.chain.step", 0.7f}),
            Map.entry("alastair.chain_break", new Object[]{"block.chain.break", 0.6f}),
            Map.entry("alastair.hook_throw", new Object[]{"item.trident.throw", 0.7f}),
            Map.entry("alastair.hook_hit", new Object[]{"item.trident.hit", 0.6f}),
            Map.entry("alastair.scalpel", new Object[]{"entity.player.attack.sweep", 1.6f}),
            Map.entry("alastair.rack_rise", new Object[]{"block.piston.extend", 0.5f}),
            Map.entry("alastair.rack_clamp", new Object[]{"block.anvil.land", 0.7f}),
            Map.entry("alastair.smoke", new Object[]{"entity.blaze.shoot", 0.5f}),
            Map.entry("alastair.laugh", new Object[]{"entity.evoker.prepare_wololo", 0.6f}),
            Map.entry("alastair.hurt", new Object[]{"entity.evoker.hurt", 0.7f}),
            Map.entry("alastair.death", new Object[]{"entity.evoker.death", 0.6f}),
            Map.entry("ruby.smoke_in", new Object[]{"entity.blaze.shoot", 1.2f}),
            Map.entry("ruby.smoke_out", new Object[]{"block.fire.extinguish", 0.8f}),
            Map.entry("ruby.laugh", new Object[]{"entity.witch.celebrate", 1.2f}),
            Map.entry("ruby.slash", new Object[]{"entity.player.attack.sweep", 1.3f}),
            Map.entry("ruby.shove", new Object[]{"entity.breeze.wind_burst", 1.1f}),
            Map.entry("ruby.hurt", new Object[]{"entity.witch.hurt", 1.3f}),
            Map.entry("ruby.death", new Object[]{"entity.witch.death", 1.2f}),
            Map.entry("uriel.slash", new Object[]{"entity.player.attack.sweep", 0.8f}),
            Map.entry("uriel.palm", new Object[]{"entity.warden.sonic_boom", 1.4f}),
            Map.entry("uriel.smite", new Object[]{"block.beacon.power_select", 0.6f}),
            Map.entry("uriel.flash", new Object[]{"item.totem.use", 1.6f}),
            Map.entry("uriel.wings", new Object[]{"entity.phantom.flap", 0.5f}),
            Map.entry("uriel.hurt", new Object[]{"entity.iron_golem.hurt", 1.2f}),
            Map.entry("uriel.death", new Object[]{"block.beacon.deactivate", 0.5f}),
            Map.entry("seal.warning", new Object[]{"block.bell.resonate", 0.6f}),
            Map.entry("seal.breaking", new Object[]{"block.respawn_anchor.charge", 0.6f}),
            Map.entry("seal.broken", new Object[]{"entity.wither.spawn", 0.5f}),
            Map.entry("seal.held", new Object[]{"block.beacon.activate", 1.2f}),
            Map.entry("seal.witness_wail", new Object[]{"entity.vex.ambient", 0.5f}),
            Map.entry("seal.rite_chant", new Object[]{"entity.evoker.prepare_summon", 0.6f}),
            Map.entry("seal.thunder_far", new Object[]{"entity.lightning_bolt.thunder", 0.5f}),
            Map.entry("blood.drink", new Object[]{"entity.generic.drink", 0.8f}),
            Map.entry("blood.push", new Object[]{"entity.breeze.wind_burst", 0.7f}),
            Map.entry("blood.exorcism", new Object[]{"entity.blaze.shoot", 0.4f}),
            Map.entry("blood.smite", new Object[]{"entity.warden.heartbeat", 0.6f}),
            Map.entry("blood.craving", new Object[]{"entity.warden.heartbeat", 1.2f}),
            Map.entry("blood.door_lock", new Object[]{"block.iron_door.close", 0.6f}),
            Map.entry("apocalypse.cage_open", new Object[]{"block.end_portal.spawn", 0.5f}),
            Map.entry("apocalypse.cage_fall", new Object[]{"entity.wither.death", 0.6f}));
    /** Heard from further off. */
    static final Set<String> FAR = Set.of("seal.broken", "seal.thunder_far", "seal.warning", "apocalypse.cage_open", "apocalypse.cage_fall",
            "uriel.palm", "alastair.rack_rise");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.APOCALYPSE_SOUNDS.forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 64 : 20));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.vex.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
        add.accept(AllSounds.MUSIC_ALASTAIR, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft.music.alastair")
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace("music.nether.basalt_deltas"), SoundDefinition.SoundType.EVENT)
                        .pitch(0.85f).volume(0.85f)));
        add.accept(AllSounds.MUSIC_URIEL, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft.music.uriel")
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace("music.end"), SoundDefinition.SoundType.EVENT)
                        .pitch(0.9f).volume(0.85f)));
    }
}
