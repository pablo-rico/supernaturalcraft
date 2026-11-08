package org.papiricoh.supernaturalcraft.hands.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.Hands;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The altar at a reliquary's heart (v0.42): it keeps its kind's Hand for every hunter, once each. It remembers who has taken
 * theirs (its own list, saved here), and the hunter's own record ({@code HandClaims}) says the same across every reliquary of the
 * kind: a hunter takes each kind of Hand once in the whole world. Never part of an arena (arenas cannot restore block entities).
 */
public class HandAltarBlockEntity extends BlockEntity {

    /** What a hunter's touch came to. */
    public enum Result {
        /** The Hand is theirs now. */
        GIVEN,
        /** They already took this kind of Hand (here or at another reliquary of its kind). */
        ALREADY,
        /** The Darkness is not out for them yet: the Hand will not come. */
        NOT_YET,
        /** An altar with no kind (placed by hand, not by a reliquary). */
        EMPTY
    }

    private @Nullable S11Assets.Hand kind;
    private final Set<UUID> claimed = new HashSet<>();

    public HandAltarBlockEntity(BlockPos pos, BlockState state) {
        super(HandsRegistry.HAND_ALTAR_BE.get(), pos, state);
    }

    public @Nullable S11Assets.Hand kind() {
        return kind;
    }

    public void setKind(@Nullable S11Assets.Hand kind) {
        this.kind = kind;
        setChanged();
    }

    /**
     * Tells the world's index where this altar stands, when its chunk loads on the server (never from worldgen: setting the kind
     * there only writes the block entity; {@code ReliquaryBuilder.placeAt} notes the ones it builds itself).
     */
    private void note() {
        if (kind != null && level instanceof net.minecraft.server.level.ServerLevel server && server.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            org.papiricoh.supernaturalcraft.hands.ReliquaryIndex.get(server.getServer()).note(worldPosition, kind);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        note();
    }

    public boolean claimedBy(UUID id) {
        return claimed.contains(id);
    }

    public int claims() {
        return claimed.size();
    }

    /**
     * {@code player} touches the altar. The seam for tests: {@code hunts} is whether the Darkness is out for them (the block asks
     * {@code Darknesses.huntsHands}); giving the Hand, the record and the core's hook are {@link Hands#take}'s.
     */
    public Result claim(ServerPlayer player, boolean hunts) {
        if (kind == null) return Result.EMPTY;
        if (claimed.contains(player.getUUID()) || Hands.claims(player).has(kind)) return Result.ALREADY;
        if (!hunts) return Result.NOT_YET;
        claimed.add(player.getUUID());
        setChanged();
        Hands.take(player, kind);
        return Result.GIVEN;
    }

    /** Forgets one hunter (the reset command). */
    public void forgetOne(UUID id) {
        if (claimed.remove(id)) setChanged();
    }

    /** Forgets every hunter. */
    public void forget() {
        claimed.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (kind != null) tag.putString("Kind", kind.reliquary);
        ListTag list = new ListTag();
        for (UUID id : claimed) list.add(NbtUtils.createUUID(id));
        tag.put("Claimed", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kind = null;
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h.reliquary.equals(tag.getString("Kind"))) kind = h;
        claimed.clear();
        for (Tag t : tag.getList("Claimed", Tag.TAG_INT_ARRAY)) claimed.add(NbtUtils.loadUUID(t));
    }
}
