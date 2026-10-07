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

    public static void init() {
    }
}
