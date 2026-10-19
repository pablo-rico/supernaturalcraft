package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.structure.HymnalSpireStructure;
import org.papiricoh.supernaturalcraft.structure.SpirePiece;

/** Structure types and their piece types (the structures themselves are datapack entries, see datagen). */
public class AllStructures {

    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SupernaturalCraft.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, SupernaturalCraft.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<HymnalSpireStructure>> HYMNAL_SPIRE =
            TYPES.register("hymnal_spire", () -> () -> HymnalSpireStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPIRE_SUMMIT =
            PIECES.register("spire_summit", () -> (StructurePieceType.ContextlessType) SpirePiece.Summit::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPIRE_ASCENT =
            PIECES.register("spire_ascent", () -> (StructurePieceType.ContextlessType) SpirePiece.Ascent::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPIRE_TEMPLE =
            PIECES.register("spire_temple", () -> (StructurePieceType.ContextlessType) SpirePiece.Temple::new);

    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.hell.cage.CageStructure>> LUCIFERS_CAGE =
            TYPES.register("lucifers_cage", () -> () -> org.papiricoh.supernaturalcraft.hell.cage.CageStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsStructure>> CROWLEYS_CORRIDORS =
            TYPES.register("crowleys_corridors", () -> () -> org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAGE_PIECE =
            PIECES.register("lucifers_cage", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.hell.cage.CageStructure.Piece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CORRIDORS_PIECE =
            PIECES.register("crowleys_corridors", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsStructure.Piece::new);

    // v0.8: a lonely graveyard with one restless grave.
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.grave.GraveStructure>> GRAVE =
            TYPES.register("grave", () -> () -> org.papiricoh.supernaturalcraft.grave.GraveStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> GRAVE_PIECE =
            PIECES.register("grave", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.grave.GravePiece::new);

    // v0.10: the Author's cabin, one per world.
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.author.AuthorCabinStructure>> AUTHOR_CABIN =
            TYPES.register("author_cabin", () -> () -> org.papiricoh.supernaturalcraft.author.AuthorCabinStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> AUTHOR_CABIN_PIECE =
            PIECES.register("author_cabin", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.author.CabinPiece::new);

    // v0.17: the Men of Letters' bunker, one per world.
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure>> BUNKER =
            TYPES.register("bunker", () -> () -> org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BUNKER_PIECE =
            PIECES.register("bunker", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.legacy.bunker.BunkerPiece::new);

    // v0.18: natural crossroads, where a box can be buried.
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure>> CROSSROADS =
            TYPES.register("crossroads", () -> () -> org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CROSSROADS_PIECE =
            PIECES.register("crossroads", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsPiece::new);

    // v0.21: the abandoned church, the witches' coven, and Rowena's house (one per world).
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.coven.church.ChurchStructure>> ABANDONED_CHURCH =
            TYPES.register("abandoned_church", () -> () -> org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ABANDONED_CHURCH_PIECE =
            PIECES.register("abandoned_church", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.coven.church.ChurchPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure>> WITCH_COVEN =
            TYPES.register("witch_coven", () -> () -> org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> WITCH_COVEN_PIECE =
            PIECES.register("witch_coven", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.coven.covenstead.CovenPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouseStructure>> ROWENA_HOUSE =
            TYPES.register("rowena_house", () -> () -> org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouseStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ROWENA_HOUSE_PIECE =
            PIECES.register("rowena_house", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.coven.rowena.RowenaHousePiece::new);

    // v0.21 (P4): Cain's farm, one per world.
    public static final DeferredHolder<StructureType<?>, StructureType<org.papiricoh.supernaturalcraft.cain.CainFarmStructure>> CAIN_FARM =
            TYPES.register("cain_farm", () -> () -> org.papiricoh.supernaturalcraft.cain.CainFarmStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAIN_FARM_PIECE =
            PIECES.register("cain_farm", () -> (StructurePieceType.ContextlessType) org.papiricoh.supernaturalcraft.cain.CainFarmPiece::new);

    public static void init() {
    }
}
