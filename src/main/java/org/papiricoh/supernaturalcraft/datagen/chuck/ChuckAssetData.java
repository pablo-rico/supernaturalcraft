package org.papiricoh.supernaturalcraft.datagen.chuck;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Author's assets as datagen sees them: item models, block states, particle sprites, sound events and their
 * subtitles. Owned by the art work (it follows what tools/artgen and tools/soundgen write). Called from the shared
 * providers ({@code SNItemModelProvider}, {@code SNBlockStateProvider}, {@code SNParticleDescriptions},
 * {@code SNSoundDefinitions}, {@code SNLanguageProvider}).
 */
public final class ChuckAssetData {

    /** Subtitles of the Author's sounds, by event path. */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("entity.chuck.type", "Typewriter clatters"),
            Map.entry("entity.chuck.carriage", "Carriage returns"),
            Map.entry("entity.chuck.bell", "Typewriter bell rings"),
            Map.entry("entity.chuck.snap", "Fingers snap"),
            Map.entry("entity.chuck.backspace", "Backspace"),
            Map.entry("entity.chuck.key_impact", "A key slams down"),
            Map.entry("entity.chuck.page_tear", "A page tears"),
            Map.entry("entity.chuck.rewrite", "The rules are rewritten"),
            Map.entry("entity.chuck.hurt", "The Author winces"),
            Map.entry("entity.chuck.laugh", "The Author laughs"),
            Map.entry("entity.chuck.reveal", "Light pours out"),
            Map.entry("entity.chuck.crack", "The script cracks"),
            Map.entry("entity.chuck.erase", "Something is erased"),
            Map.entry("entity.chuck.write", "Something is written"),
            Map.entry("entity.chuck.echo", "An old enemy is written in ink"),
            Map.entry("entity.chuck.approve", "The Author approves"),
            Map.entry("entity.hunter_ally.arrive", "Hunters step out of the light"),
            Map.entry("entity.author_npc.ambient", "The Author hums"),
            Map.entry("item.authors_pen.write", "The Pen writes"),
            Map.entry("music.chuck", "Music plays"));

    private ChuckAssetData() {
    }

    /** A particle's sprite set: {@code count} sprites named {@code <name>_0} … */
    @FunctionalInterface
    public interface ParticleSink {
        void spriteSet(ParticleType<?> type, String name, int count);
    }

    public static void itemModels(SNItemModelProvider p) {
        for (var item : java.util.List.of(AllItems.THE_END_MANUSCRIPT, AllItems.AUTHORS_PEN, AllItems.SAMS_AMULET)) {
            String id = item.getId().getPath();
            p.withExistingParent(id, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + id));
        }
        p.withExistingParent(AllItems.CHUCK_SPAWN_EGG.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        p.withExistingParent(AllItems.TYPEWRITER.getId().getPath(), p.modLoc("block/typewriter"));
    }

    /**
     * The typewriter is tools/artgen's block model ({@code author_blocks_art.py}: keyboard toward the north). If its block
     * has a horizontal FACING, the state turns the model so the keys face that way; otherwise one model.
     */
    public static void blockStates(SNBlockStateProvider p) {
        var typewriter = AllBlocks.TYPEWRITER.get();
        ModelFile model = unchecked(p.modLoc("block/typewriter"));
        if (typewriter.getStateDefinition().getProperties().contains(BlockStateProperties.HORIZONTAL_FACING)) {
            p.horizontalBlock(typewriter, model);
        } else {
            p.simpleBlock(typewriter, model);
        }
        p.simpleBlock(AllBlocks.PAGE_BLOCK.get());
        p.simpleBlock(AllBlocks.INK_BLOCK.get());
        p.simpleBlock(AllBlocks.BURNING_INK.get());
    }

    public static void particles(ParticleSink sink) {
        sink.spriteSet(AllParticles.INK_LETTER.get(), "ink_letter", 8);
        sink.spriteSet(AllParticles.PAGE_SCRAP.get(), "page_scrap", 4);
        sink.spriteSet(AllParticles.GOLDEN_MOTE.get(), "golden_mote", 4);
    }

    /**
     * The Author's sounds: tools/soundgen's OGG files in {@code sounds/chuck/} (mono, so they are positional; variants are
     * picked at random). The loud set pieces carry farther than the default 16 blocks: the arena is 34 blocks in radius.
     */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        own(add, AllSounds.CHUCK_TYPE, 0.8f, 16, "type_1", "type_2", "type_3", "type_4");
        own(add, AllSounds.CHUCK_CARRIAGE, 0.9f, 24, "carriage_1", "carriage_2");
        own(add, AllSounds.CHUCK_BELL, 1.0f, 32, "bell");
        own(add, AllSounds.CHUCK_SNAP, 1.0f, 48, "snap_1", "snap_2");
        own(add, AllSounds.CHUCK_BACKSPACE, 1.0f, 32, "backspace");
        own(add, AllSounds.CHUCK_KEY_IMPACT, 1.0f, 48, "key_impact_1", "key_impact_2");
        own(add, AllSounds.CHUCK_PAGE_TEAR, 0.9f, 24, "page_tear_1", "page_tear_2");
        own(add, AllSounds.CHUCK_REWRITE, 1.0f, 64, "rewrite");
        own(add, AllSounds.CHUCK_HURT, 0.9f, 24, "hurt_1", "hurt_2");
        own(add, AllSounds.CHUCK_LAUGH, 0.9f, 24, "laugh");
        own(add, AllSounds.CHUCK_REVEAL, 1.0f, 64, "reveal");
        own(add, AllSounds.CHUCK_CRACK, 1.0f, 48, "crack");
        own(add, AllSounds.CHUCK_ERASE, 1.0f, 48, "erase");
        own(add, AllSounds.CHUCK_WRITE, 0.8f, 24, "write_1", "write_2");
        own(add, AllSounds.CHUCK_ECHO, 1.0f, 48, "echo");
        own(add, AllSounds.CHUCK_APPROVE, 1.0f, 48, "approve");
        own(add, AllSounds.HUNTER_ALLY_ARRIVE, 1.0f, 64, "ally_arrive");
        own(add, AllSounds.AUTHOR_NPC_AMBIENT, 0.7f, 16, "npc_hum_1", "npc_hum_2");
        own(add, AllSounds.PEN_WRITE, 0.9f, 16, "pen_write");
        add.accept(AllSounds.MUSIC_CHUCK, SoundDefinition.definition()
                .subtitle("subtitles.supernaturalcraft." + AllSounds.MUSIC_CHUCK.getId().getPath())
                .with(SoundDefinition.Sound.sound(file("music"), SoundDefinition.SoundType.SOUND).stream(true)));
    }

    /** One event over its generated variants, each at the given volume and hearing distance. */
    private static void own(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add,
                            DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance, String... files) {
        SoundDefinition definition = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event.getId().getPath());
        for (String name : files) {
            definition.with(SoundDefinition.Sound.sound(file(name), SoundDefinition.SoundType.SOUND)
                    .volume(volume).attenuationDistance(distance));
        }
        add.accept(event, definition);
    }

    /** {@code supernaturalcraft:chuck/<name>} = {@code sounds/chuck/<name>.ogg}. */
    public static ResourceLocation file(String name) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "chuck/" + name);
    }

    static ModelFile unchecked(ResourceLocation model) {
        return new ModelFile.UncheckedModelFile(model);
    }
}
