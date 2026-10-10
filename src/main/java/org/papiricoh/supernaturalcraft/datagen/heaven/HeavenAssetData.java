package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * v0.18's 2D assets and sounds as datagen sees them (owned by the 2D art and sound work): block states and models, flat item
 * models, sound definitions and subtitles. Called from the shared providers; names in {@code HeavenAssets}. The 3D side (GeckoLib
 * items, the busts' item models, spawn eggs) is {@link HeavenGeoAssetData}.
 * <p>The art is tools/artgen {@code heaven_blocks_art.py} (block textures and the hearth's and the console's block models),
 * {@code heaven_items_art.py} (sprites), the sounds tools/soundgen {@code heaven_sfx.py}, {@code naomi_sfx.py},
 * {@code zachariah_sfx.py}. Block states read the blocks' properties by name ({@code axis}, {@code facing}, {@code number}), so a
 * block that gains or lacks one still gets a complete state file.
 */
public final class HeavenAssetData {

    /** Subtitles of v0.18's sounds, by event path ({@code heaven.gate_open}, {@code naomi.drill}...). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("heaven.gate_open", "A gate of light opens"),
            Map.entry("heaven.gate_hum", "A gate of light hums"),
            Map.entry("heaven.arrive", "You arrive in Heaven"),
            Map.entry("heaven.memory_enter", "A memory unfolds"),
            Map.entry("heaven.memory_collect", "A memory is gathered"),
            Map.entry("heaven.memory_leave", "The memory fades"),
            Map.entry("heaven.hearth_rest", "The hearth's warmth settles"),
            Map.entry("heaven.seal_open", "A seal of light opens"),
            Map.entry("heaven.ash_greet", "Ash greets you"),
            Map.entry("naomi.ambient", "Naomi hums to herself"),
            Map.entry("naomi.hurt", "Naomi is struck"),
            Map.entry("naomi.death", "Naomi falls"),
            Map.entry("naomi.drill", "A drill whines"),
            Map.entry("naomi.chair_strap", "Straps buckle shut"),
            Map.entry("naomi.chair_struggle", "Straps strain"),
            Map.entry("naomi.chair_free", "Straps snap open"),
            Map.entry("naomi.wipe", "Memory is wiped"),
            Map.entry("naomi.guards", "Guards are called"),
            Map.entry("naomi.console", "A console beeps"),
            Map.entry("naomi.test_bell", "A test bell rings"),
            Map.entry("zachariah.ambient", "Zachariah sighs"),
            Map.entry("zachariah.hurt", "Zachariah is struck"),
            Map.entry("zachariah.death", "Zachariah falls"),
            Map.entry("zachariah.stamp", "A stamp slams down"),
            Map.entry("zachariah.paper_storm", "Papers storm"),
            Map.entry("zachariah.file", "A drawer is filed"),
            Map.entry("zachariah.denied", "Denied"),
            Map.entry("zachariah.approved", "Approved"),
            Map.entry("zachariah.termination", "A termination notice is served"),
            Map.entry("zachariah.wings", "Great wings unfold"),
            Map.entry("zachariah.wrap", "The office folds back on itself"),
            Map.entry("zachariah.docket", "A typewriter bell dings"),
            Map.entry("crossroads.bury", "Earth is dug"),
            Map.entry("crossroads.wild_arrive", "A demon comes to the crossroads"),
            Map.entry("music.heaven", "Heaven's music"),
            Map.entry("music.naomi", "Naomi's music"),
            Map.entry("music.zachariah", "Zachariah's music"));

    private HeavenAssetData() {
    }

    /** The sprites, and the block items as their block models. */
    public static void itemModels(SNItemModelProvider p) {
        for (DeferredItem<? extends Item> item : List.of(AllItems.NAOMIS_DIADEM, AllItems.HEAVENS_SEAL, AllItems.HEAVENLY_FORM,
                AllItems.APPROVAL_STAMP, AllItems.CROSSROADS_BOX)) {
            p.basicItem(item.get());
        }
        block(p, AllItems.HEARTH, "hearth");
        block(p, AllItems.CLOUD_STONE, "cloud_stone");
        block(p, AllItems.CLOUD_BRICKS, "cloud_bricks");
        block(p, AllItems.FILING_CABINET, "filing_cabinet_1");
        block(p, AllItems.CROSSROADS_SOIL, "crossroads_soil");
    }

    private static void block(SNItemModelProvider p, DeferredItem<? extends Item> item, String model) {
        p.withExistingParent(item.getId().getPath(), p.modLoc("block/" + model));
    }

    public static void blockStates(SNBlockStateProvider p) {
        // The gate and the veil: thin panes of light like a nether portal's, along their axis (a full block if they have none).
        pane(p, AllBlocks.HEAVEN_GATE.get(), "heaven_gate");
        pane(p, AllBlocks.MEMORY_VEIL.get(), "memory_veil");
        ModelFile seal = p.models().cubeAll("celestial_seal", p.modLoc("block/celestial_seal")).renderType("translucent");
        states(p, AllBlocks.CELESTIAL_SEAL.get(), st -> seal, false);

        p.simpleBlock(AllBlocks.CLOUD_STONE.get(), p.models().cubeAll("cloud_stone", p.modLoc("block/cloud_stone")));
        p.simpleBlock(AllBlocks.CLOUD_BRICKS.get(), p.models().cubeAll("cloud_bricks", p.modLoc("block/cloud_bricks")));
        p.simpleBlock(AllBlocks.CROSSROADS_SOIL.get(), p.models().cubeBottomTop("crossroads_soil", p.modLoc("block/crossroads_soil"),
                p.mcLoc("block/dirt"), p.modLoc("block/crossroads_soil_top")));

        // tools/artgen block models, front to the north, turned by FACING.
        ModelFile hearth = new ModelFile.UncheckedModelFile(p.modLoc("block/hearth"));
        states(p, AllBlocks.HEARTH.get(), st -> hearth, true);
        ModelFile console = new ModelFile.UncheckedModelFile(p.modLoc("block/reprogramming_console"));
        states(p, AllBlocks.REPROGRAMMING_CONSOLE.get(), st -> console, true);

        // The filing cabinets: one model per number (front_1..4), turned by FACING.
        ModelFile[] cabinets = new ModelFile[5];
        cabinets[0] = p.models().orientable("filing_cabinet", p.modLoc("block/filing_cabinet_side"),
                p.modLoc("block/filing_cabinet_front"), p.modLoc("block/filing_cabinet_top"));
        for (int n = 1; n <= 4; n++) {
            cabinets[n] = p.models().orientable("filing_cabinet_" + n, p.modLoc("block/filing_cabinet_side"),
                    p.modLoc("block/filing_cabinet_front_" + n), p.modLoc("block/filing_cabinet_top"));
        }
        Block cabinet = AllBlocks.FILING_CABINET.get();
        Property<?> number = cabinet.getStateDefinition().getProperty("number");
        states(p, cabinet, st -> {
            int n = number == null ? 0 : Math.max(0, Math.min(4, ((Number) st.getValues().get(number)).intValue()));
            return cabinets[n];
        }, true);

        // The busts: ART-3D's block models (HeavenAssets.NAOMI_TROPHY_MODEL / ZACHARIAH_TROPHY_MODEL), turned by FACING.
        ModelFile naomi = new ModelFile.UncheckedModelFile(p.modLoc("block/naomi_trophy"));
        states(p, AllBlocks.NAOMI_TROPHY.get(), st -> naomi, true);
        ModelFile zachariah = new ModelFile.UncheckedModelFile(p.modLoc("block/zachariah_trophy"));
        states(p, AllBlocks.ZACHARIAH_TROPHY.get(), st -> zachariah, true);
    }

    /** A pane of light: vanilla's nether portal planes (translucent) along the block's horizontal axis or facing. */
    private static void pane(SNBlockStateProvider p, Block block, String name) {
        ResourceLocation tex = p.modLoc("block/" + name);
        Property<?> axis = block.getStateDefinition().getProperty("axis");
        Property<?> facing = block.getStateDefinition().getProperty("facing");
        if (axis == null && facing == null) {
            ModelFile full = p.models().cubeAll(name, tex).renderType("translucent");
            states(p, block, st -> full, false);
            return;
        }
        ModelFile ns = p.models().withExistingParent(name + "_ns", p.mcLoc("block/nether_portal_ns"))
                .texture("portal", tex).texture("particle", tex).renderType("translucent");
        ModelFile ew = p.models().withExistingParent(name + "_ew", p.mcLoc("block/nether_portal_ew"))
                .texture("portal", tex).texture("particle", tex).renderType("translucent");
        states(p, block, st -> {
            Object v = st.getValues().get(axis != null ? axis : facing);
            Direction.Axis a = v instanceof Direction.Axis ax ? ax : v instanceof Direction d ? d.getClockWise().getAxis() : Direction.Axis.X;
            return a == Direction.Axis.Z ? ew : ns;
        }, false);
    }

    /** Every state of the block gets {@code model(state)}, turned by its horizontal {@code facing} when {@code turn} (north = 0). */
    private static void states(SNBlockStateProvider p, Block block, Function<BlockState, ModelFile> model, boolean turn) {
        Property<?> facing = block.getStateDefinition().getProperty("facing");
        p.getVariantBuilder(block).forAllStates(st -> {
            int y = 0;
            if (turn && facing != null && st.getValues().get(facing) instanceof Direction d && d.getAxis().isHorizontal()) {
                y = ((int) d.toYRot() + 180) % 360;
            }
            return ConfiguredModel.builder().modelFile(model.apply(st)).rotationY(y).build();
        });
    }

    /**
     * Every sound is tools/soundgen's own mono file: {@code sounds/<group>/<id>.ogg} for {@code <group>.<id>}, and the music
     * ({@code music.<name>}) {@code sounds/heaven/music_<name>.ogg}, streamed.
     */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (String event : HeavenAssets.allSoundEvents()) {
            DeferredHolder<SoundEvent, SoundEvent> holder = AllSounds.HEAVEN_SOUNDS.get(event);
            if (holder == null) continue;
            String group = event.substring(0, event.indexOf('.'));
            String id = event.substring(event.indexOf('.') + 1);
            boolean music = group.equals("music");
            String file = music ? "heaven/music_" + id : group + "/" + id;
            SoundDefinition.Sound sound = SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, file),
                    SoundDefinition.SoundType.SOUND).volume(volume(event)).attenuationDistance(distance(event));
            if (music) sound.stream(true);
            SoundDefinition def = SoundDefinition.definition();
            if (!music) def.subtitle("subtitles.supernaturalcraft." + event);
            add.accept(holder, def.with(sound));
        }
    }

    private static float volume(String event) {
        if (event.startsWith("music.")) return 0.7f;
        if (event.endsWith(".ambient") || event.equals("heaven.gate_hum")) return 0.7f;
        return switch (event) {
            case "naomi.chair_struggle", "naomi.console", "zachariah.file", "zachariah.docket" -> 0.85f;
            default -> 1.0f;
        };
    }

    private static int distance(String event) {
        if (event.startsWith("music.")) return 16;
        return switch (event) {
            case "heaven.arrive", "heaven.memory_enter", "heaven.memory_collect", "heaven.memory_leave", "heaven.hearth_rest",
                 "naomi.console", "zachariah.file", "zachariah.docket", "zachariah.approved", "zachariah.denied" -> 16;
            case "heaven.gate_hum", "heaven.ash_greet", "naomi.ambient", "zachariah.ambient", "naomi.hurt", "zachariah.hurt",
                 "naomi.chair_strap", "naomi.chair_struggle", "naomi.chair_free" -> 24;
            case "naomi.death", "zachariah.death", "zachariah.wings", "zachariah.termination", "crossroads.wild_arrive",
                 "heaven.gate_open", "heaven.seal_open" -> 64;
            default -> 32;
        };
    }
}
