package org.papiricoh.supernaturalcraft.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A floor layout drawn around a ritual altar, from {@code data/<ns>/supernaturalcraft/ritual_pattern/}.
 * One layer at the altar's height; {@code 'A'} marks the altar, a space means "anything", every
 * other symbol maps to a vanilla block predicate (block, tag and/or block-state properties).
 */
public record RitualPattern(List<String> pattern, Map<Character, BlockPredicate> key) {

    private static final Codec<Character> SYMBOL = Codec.STRING.comapFlatMap(
            s -> s.length() == 1 ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "Key must be one character: " + s),
            String::valueOf);

    public static final Codec<RitualPattern> CODEC = RecordCodecBuilder.<RitualPattern>create(i -> i.group(
            Codec.STRING.listOf().fieldOf("pattern").forGetter(RitualPattern::pattern),
            Codec.unboundedMap(SYMBOL, BlockPredicate.CODEC).fieldOf("key").forGetter(RitualPattern::key)
    ).apply(i, RitualPattern::new)).validate(RitualPattern::validate);

    private DataResult<RitualPattern> validate() {
        try {
            for (PatternGeometry.Cell c : PatternGeometry.cells(pattern)) {
                if (!key.containsKey(c.symbol())) {
                    return DataResult.error(() -> "Pattern symbol '" + c.symbol() + "' has no key entry");
                }
            }
            return DataResult.success(this);
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }

    public List<PatternGeometry.Cell> cells() {
        return PatternGeometry.cells(pattern);
    }

    /** The outcome of checking the world against this pattern. */
    public record Match(boolean matches, int rotation, @Nullable BlockPos firstMismatch) {
    }

    /**
     * Tries all four rotations around {@code altar}. On failure, reports the first wrong block of
     * the rotation that came closest, so the player can be shown what to fix.
     */
    public Match match(LevelReader level, BlockPos altar) {
        List<PatternGeometry.Cell> cells = cells();
        int bestRotation = 0, bestCorrect = -1;
        BlockPos bestMismatch = null;
        for (int rot = 0; rot < 4; rot++) {
            int correct = 0;
            BlockPos mismatch = null;
            for (PatternGeometry.Cell c : cells) {
                int[] o = PatternGeometry.rotate(c.dx(), c.dz(), rot);
                BlockPos p = altar.offset(o[0], 0, o[1]);
                if (key.get(c.symbol()).matches(new BlockInWorld(level, p, false))) {
                    correct++;
                } else if (mismatch == null) {
                    mismatch = p;
                }
            }
            if (mismatch == null) return new Match(true, rot, null);
            if (correct > bestCorrect) {
                bestCorrect = correct;
                bestRotation = rot;
                bestMismatch = mismatch;
            }
        }
        return new Match(false, bestRotation, bestMismatch);
    }

    public int size() {
        return Math.max(pattern.size(), pattern.stream().mapToInt(String::length).max().orElse(0));
    }
}
