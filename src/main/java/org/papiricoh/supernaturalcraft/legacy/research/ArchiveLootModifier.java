package org.papiricoh.supernaturalcraft.legacy.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts;

import java.util.List;

/**
 * The Men of Letters' leavings in the world's chests (v0.17): every chest loot table (path {@code chests/...}) may also hold field
 * notes (arcane, relic or place) and, rarely, an unidentified cursed artifact.
 *
 * @param notesChance chance of 1–2 field notes
 * @param artifactChance chance of an artifact (up to rare)
 */
public class ArchiveLootModifier extends LootModifier {

    public static final MapCodec<ArchiveLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(i.group(
            Codec.floatRange(0, 1).optionalFieldOf("notes_chance", 0.25f).forGetter(m -> m.notesChance),
            Codec.floatRange(0, 1).optionalFieldOf("artifact_chance", 0.03f).forGetter(m -> m.artifactChance)
    )).apply(i, ArchiveLootModifier::new));

    static final List<String> TOPICS = List.of(FieldNotesItem.ARCANE, FieldNotesItem.RELIC, FieldNotesItem.PLACE);

    private final float notesChance;
    private final float artifactChance;

    public ArchiveLootModifier(LootItemCondition[] conditions, float notesChance, float artifactChance) {
        super(conditions);
        this.notesChance = notesChance;
        this.artifactChance = artifactChance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) return loot;
        RandomSource r = context.getRandom();
        if (r.nextFloat() < notesChance) loot.add(FieldNotesItem.stack(TOPICS.get(r.nextInt(TOPICS.size())), 1 + r.nextInt(2)));
        if (r.nextFloat() < artifactChance) loot.add(Artifacts.roll(r.nextLong(), 2));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
