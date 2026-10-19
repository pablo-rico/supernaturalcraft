package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

/**
 * Writes sign text from structure code. {@link SignBlockEntity#setText} notifies the block entity's level, which a sign in a
 * chunk still being generated doesn't have yet, so this goes through NBT as structure templates do.
 */
public final class GenSigns {
    private GenSigns() {
    }

    public static void write(WorldGenLevel level, SignBlockEntity sign, SignText front, SignText back, boolean waxed) {
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = sign.saveWithoutMetadata(level.registryAccess());
        SignText.DIRECT_CODEC.encodeStart(ops, front).ifSuccess(t -> tag.put("front_text", t));
        SignText.DIRECT_CODEC.encodeStart(ops, back).ifSuccess(t -> tag.put("back_text", t));
        tag.putBoolean("is_waxed", waxed);
        sign.loadWithComponents(tag, level.registryAccess());
    }
}
