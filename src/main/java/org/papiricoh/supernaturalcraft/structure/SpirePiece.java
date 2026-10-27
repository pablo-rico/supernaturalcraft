package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.registry.AllStructures;
import org.papiricoh.supernaturalcraft.structure.spire.SpireSites;

import java.util.ArrayList;
import java.util.List;

/**
 * One part of a remade Hymnal Spire (piece type {@code spire_part}): the summit, the ascent, or one of the plan's sites (the
 * temple first). Each piece carries the whole {@link SpireLayout.Plan} (it is small: nodes, sampled ground, sites), so it
 * rebuilds exactly the same from its saved data in whichever chunk it is asked to fill; its kit plan is drawn once per piece
 * and kept. Spires generated before the remake keep their own pieces ({@code structure/legacy}).
 */
public class SpirePiece extends StructurePiece {

    public enum Part {SUMMIT, ASCENT, TEMPLE, SITE}

    private final SpireLayout.Plan plan;
    private final Part part;
    /** The site's index in {@link SpireLayout.Plan#sites} (only for {@link Part#SITE}). */
    private final int site;
    private volatile SpireBuilder.Built built;
    private volatile long[] footings;

    public SpirePiece(SpireLayout.Plan plan, Part part, int site) {
        super(AllStructures.SPIRE_PART.get(), 0, box(plan, part, site));
        this.plan = plan;
        this.part = part;
        this.site = site;
        setOrientation(null);
    }

    public SpirePiece(CompoundTag tag) {
        super(AllStructures.SPIRE_PART.get(), tag);
        this.plan = readPlan(tag);
        this.part = Part.values()[Math.floorMod(tag.getInt("Part"), Part.values().length)];
        this.site = tag.getInt("Site");
        setOrientation(null);
    }

    static BoundingBox box(SpireLayout.Plan plan, Part part, int site) {
        return switch (part) {
            case SUMMIT -> SpireLayout.summitBox(plan);
            case ASCENT -> SpireLayout.ascentBox(plan);
            case TEMPLE -> SpireLayout.templeBox(plan);
            case SITE -> SpireLayout.siteBox(plan.sites().get(site));
        };
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("Part", part.ordinal());
        tag.putInt("Site", site);
        writePlan(plan, tag);
    }

    static void writePlan(SpireLayout.Plan plan, CompoundTag tag) {
        tag.putLong("Altar", plan.altar().asLong());
        tag.putByteArray("Melody", plan.melody());
        tag.putLongArray("Path", plan.path().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("Temple", plan.templeOrigin().asLong());
        tag.putInt("Facing", plan.templeFacing().get3DDataValue());
        tag.putLong("Seed", plan.seed());
        tag.putIntArray("GroundIn", plan.groundIn());
        tag.putIntArray("GroundMid", plan.groundMid());
        tag.putIntArray("GroundOut", plan.groundOut());
        ListTag sites = new ListTag();
        for (SpireSites.Site s : plan.sites()) {
            CompoundTag t = new CompoundTag();
            t.putInt("Kind", s.kind().ordinal());
            t.putInt("Node", s.node());
            t.putLong("Origin", s.origin().asLong());
            t.putInt("Facing", s.facing().get3DDataValue());
            t.putInt("Variant", s.variant());
            sites.add(t);
        }
        tag.put("Sites", sites);
    }

    static SpireLayout.Plan readPlan(CompoundTag tag) {
        List<BlockPos> path = new ArrayList<>();
        for (long l : tag.getLongArray("Path")) path.add(BlockPos.of(l));
        List<SpireSites.Site> sites = new ArrayList<>();
        for (Tag t : tag.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            sites.add(new SpireSites.Site(SpireSites.Kind.values()[c.getInt("Kind")], c.getInt("Node"), BlockPos.of(c.getLong("Origin")),
                    Direction.from3DDataValue(c.getInt("Facing")), c.getInt("Variant")));
        }
        return new SpireLayout.Plan(BlockPos.of(tag.getLong("Altar")), tag.getByteArray("Melody"), List.copyOf(path),
                BlockPos.of(tag.getLong("Temple")), Direction.from3DDataValue(tag.getInt("Facing")), tag.getLong("Seed"),
                tag.getIntArray("GroundIn"), tag.getIntArray("GroundMid"), tag.getIntArray("GroundOut"), List.copyOf(sites));
    }

    public SpireLayout.Plan plan() {
        return plan;
    }

    public Part part() {
        return part;
    }

    private SpireBuilder.Built built() {
        SpireBuilder.Built b = built;
        if (b == null) {
            synchronized (this) {
                if (built == null) {
                    built = switch (part) {
                        case SUMMIT -> SpireBuilder.summitPlan(plan);
                        case ASCENT -> SpireBuilder.ascentPlan(plan);
                        case TEMPLE -> SpireBuilder.sitePlan(plan, plan.temple());
                        case SITE -> SpireBuilder.sitePlan(plan, plan.sites().get(site));
                    };
                }
                b = built;
            }
        }
        return b;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        long[] f = footings;
        if (f == null) {
            synchronized (this) {
                if (footings == null) footings = SpireBuilder.footingCells(built(), SpireBuilder.generatorGround(level, generator));
                f = footings;
            }
        }
        SpireBuilder.write(level, box, built(), f, plan);
    }

    /** Every piece, in the order they must be built: the summit, the stair, then the parts beside it (they cut into the stair). */
    public static List<SpirePiece> all(SpireLayout.Plan plan) {
        List<SpirePiece> out = new ArrayList<>();
        out.add(new SpirePiece(plan, Part.SUMMIT, -1));
        out.add(new SpirePiece(plan, Part.ASCENT, -1));
        out.add(new SpirePiece(plan, Part.TEMPLE, -1));
        for (int i = 0; i < plan.sites().size(); i++) out.add(new SpirePiece(plan, Part.SITE, i));
        return out;
    }
}
