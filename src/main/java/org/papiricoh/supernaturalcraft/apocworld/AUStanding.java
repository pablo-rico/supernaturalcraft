package org.papiricoh.supernaturalcraft.apocworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A hunter's standing with the Apocalypse World (v0.41), an attachment kept through death: whether they have been there, how many
 * of Michael's prisoner camps they have freed, whether they have seen him flee, and the way back (where they stood before they
 * crossed, so a rift home takes them back there). Read with {@code optionalFieldOf} defaults, so old saves and new fields agree.
 *
 * @param visited     been in the Apocalypse World at least once
 * @param campsFreed  prisoner camps they have freed
 * @param michaelFled AU Michael broke off from them and fled into our world
 * @param returnDim   the dimension they came from ({@code ""} if none is known)
 * @param returnPos   where they stood there
 */
public record AUStanding(boolean visited, int campsFreed, boolean michaelFled, String returnDim, Vec3 returnPos) {

    public static final AUStanding EMPTY = new AUStanding(false, 0, false, "", Vec3.ZERO);

    private static final Codec<Vec3> VEC = Codec.DOUBLE.listOf().xmap(l -> l.size() == 3 ? new Vec3(l.get(0), l.get(1), l.get(2)) : Vec3.ZERO,
            v -> java.util.List.of(v.x, v.y, v.z));

    public static final Codec<AUStanding> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("visited", false).forGetter(AUStanding::visited),
            Codec.INT.optionalFieldOf("camps_freed", 0).forGetter(AUStanding::campsFreed),
            Codec.BOOL.optionalFieldOf("michael_fled", false).forGetter(AUStanding::michaelFled),
            Codec.STRING.optionalFieldOf("return_dim", "").forGetter(AUStanding::returnDim),
            VEC.optionalFieldOf("return_pos", Vec3.ZERO).forGetter(AUStanding::returnPos)
    ).apply(i, AUStanding::new));

    public AUStanding withVisited() {
        return new AUStanding(true, campsFreed, michaelFled, returnDim, returnPos);
    }

    public AUStanding withCampFreed() {
        return new AUStanding(visited, campsFreed + 1, michaelFled, returnDim, returnPos);
    }

    public AUStanding withMichaelFled() {
        return new AUStanding(visited, campsFreed, true, returnDim, returnPos);
    }

    public AUStanding withReturn(ResourceKey<Level> dim, Vec3 pos) {
        return new AUStanding(visited, campsFreed, michaelFled, dim.location().toString(), pos);
    }

    /** The dimension of the way back, if any is known. */
    public Optional<ResourceKey<Level>> returnLevel() {
        if (returnDim.isEmpty()) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(returnDim);
        return id == null ? Optional.empty() : Optional.of(ResourceKey.create(Registries.DIMENSION, id));
    }
}
