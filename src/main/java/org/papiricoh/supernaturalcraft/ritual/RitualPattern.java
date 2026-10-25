package org.papiricoh.supernaturalcraft.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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

    /** The outcome of checking the world against this pattern; on failure, the first wrong cell and what it wants. */
    public record Match(boolean matches, int rotation, @Nullable BlockPos firstMismatch, @Nullable BlockPredicate expected) {
    }

    /**
     * Tries all four rotations around {@code altar}. On failure, reports the first wrong block of
     * the rotation that came closest, so the player can be shown what to fix.
     */
    public Match match(LevelReader level, BlockPos altar) {
        List<PatternGeometry.Cell> cells = cells();
        int bestRotation = 0, bestCorrect = -1;
        BlockPos bestMismatch = null;
        BlockPredicate bestExpected = null;
        for (int rot = 0; rot < 4; rot++) {
            int correct = 0;
            BlockPos mismatch = null;
            BlockPredicate expected = null;
            for (PatternGeometry.Cell c : cells) {
                int[] o = PatternGeometry.rotate(c.dx(), c.dz(), rot);
                BlockPos p = altar.offset(o[0], 0, o[1]);
                if (key.get(c.symbol()).matches(new BlockInWorld(level, p, false))) {
                    correct++;
                } else if (mismatch == null) {
                    mismatch = p;
                    expected = key.get(c.symbol());
                }
            }
            if (mismatch == null) return new Match(true, rot, null, null);
            if (correct > bestCorrect) {
                bestCorrect = correct;
                bestRotation = rot;
                bestMismatch = mismatch;
                bestExpected = expected;
            }
        }
        return new Match(false, bestRotation, bestMismatch, bestExpected);
    }

    /**
     * What is wrong at a failed match's first wrong cell, for the player: an unlit candle, or which
     * block the circle wants there and which one it found (salt is not chalk, though both are white).
     */
    public static Component explain(LevelReader level, Match match) {
        if (match.firstMismatch() == null || match.expected() == null) {
            return Component.translatable("message.supernaturalcraft.ritual.broken_circle");
        }
        BlockState found = level.getBlockState(match.firstMismatch());
        BlockPredicate expected = match.expected();
        boolean rightBlock = expected.blocks().map(found::is).orElse(true);
        boolean wantsLit = expected.properties().flatMap(props -> StatePropertiesPredicate.CODEC.encodeStart(JsonOps.INSTANCE, props).result())
                .map(json -> json.isJsonObject() && json.getAsJsonObject().has("lit")).orElse(false);
        if (rightBlock) {
            return Component.translatable(wantsLit ? "message.supernaturalcraft.ritual.broken_circle_unlit"
                    : "message.supernaturalcraft.ritual.broken_circle_state", name(found.getBlock()));
        }
        // A candle of the wrong colour, or none at all: name the colour and, if the rite wants it, that it burns.
        Component want = expected.blocks().flatMap(set -> set.stream().findFirst()).map(h -> name(h.value()))
                .orElse(Component.translatable("message.supernaturalcraft.ritual.something"));
        String lit = wantsLit ? "_lit" : "";
        return found.isAir()
                ? Component.translatable("message.supernaturalcraft.ritual.broken_circle_missing" + lit, want)
                : Component.translatable("message.supernaturalcraft.ritual.broken_circle_wrong" + lit, want, name(found.getBlock()));
    }

    /** The name a player knows a block by: its item's (chalk, not "chalk line"), else the block's own. */
    private static Component name(Block block) {
        Item item = block.asItem();
        return item == Items.AIR ? block.getName() : item.getDefaultInstance().getHoverName();
    }

    public int size() {
        return Math.max(pattern.size(), pattern.stream().mapToInt(String::length).max().orElse(0));
    }
}
