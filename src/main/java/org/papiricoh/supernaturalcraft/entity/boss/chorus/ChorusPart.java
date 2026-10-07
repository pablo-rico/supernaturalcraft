package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * One piece of the Broken Chorus that can be struck on its own, following the Ender Dragon's pattern:
 * a fixed array on the boss, ids right after the boss's, moved on both sides every tick, never
 * saved and never sent on their own.
 */
public class ChorusPart extends PartEntity<ChorusEntity> {

    public enum Kind { FACE, WING, EYE, CORE, SHELL }

    public final Kind kind;
    public final int index;
    private final EntityDimensions size;

    public ChorusPart(ChorusEntity parent, Kind kind, int index, float width, float height) {
        super(parent);
        this.kind = kind;
        this.index = index;
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return getParent().partPickable(index);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtPart(this, source, amount);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
