package org.papiricoh.supernaturalcraft.purgatory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A hunter's standing with Purgatory (v0.39), an attachment kept through death: whether they have been there, whether they got out
 * the hard way (Eve fell to them: from then on the way is open both ways), and the return link (where they stood before they went
 * in, so a rift home takes them back there). Read with {@code optionalFieldOf} defaults, so old saves and new fields agree.
 *
 * @param visited  been in Purgatory at least once
 * @param escaped  beaten Eve: rifts and the rite take them in and out freely
 * @param returnDim the dimension they came from ({@code ""} if none is known)
 * @param returnPos where they stood there
 */
public record PurgatoryStanding(boolean visited, boolean escaped, String returnDim, Vec3 returnPos) {

    public static final PurgatoryStanding EMPTY = new PurgatoryStanding(false, false, "", Vec3.ZERO);

    private static final Codec<Vec3> VEC = Codec.DOUBLE.listOf().xmap(l -> l.size() == 3 ? new Vec3(l.get(0), l.get(1), l.get(2)) : Vec3.ZERO,
            v -> java.util.List.of(v.x, v.y, v.z));

    public static final Codec<PurgatoryStanding> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("visited", false).forGetter(PurgatoryStanding::visited),
            Codec.BOOL.optionalFieldOf("escaped", false).forGetter(PurgatoryStanding::escaped),
            Codec.STRING.optionalFieldOf("return_dim", "").forGetter(PurgatoryStanding::returnDim),
            VEC.optionalFieldOf("return_pos", Vec3.ZERO).forGetter(PurgatoryStanding::returnPos)
    ).apply(i, PurgatoryStanding::new));

    public PurgatoryStanding withVisited() {
        return new PurgatoryStanding(true, escaped, returnDim, returnPos);
    }

    public PurgatoryStanding withEscaped() {
        return new PurgatoryStanding(true, true, returnDim, returnPos);
    }

    public PurgatoryStanding withReturn(ResourceKey<Level> dim, Vec3 pos) {
        return new PurgatoryStanding(visited, escaped, dim.location().toString(), pos);
    }

    /** The dimension of the way back, if any is known. */
    public Optional<ResourceKey<Level>> returnLevel() {
        if (returnDim.isEmpty()) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(returnDim);
        return id == null ? Optional.empty() : Optional.of(ResourceKey.create(Registries.DIMENSION, id));
    }
}
