package org.papiricoh.supernaturalcraft.arena;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The arena's own floor: placed and removed by the fight, never dropped. The hellfire variant
 * burns whoever stands on it, the frost variant slowly freezes them.
 */
public class ArenaBlock extends Block {

    public enum Kind { HELLFIRE, FROST, PLAIN }

    public static final MapCodec<ArenaBlock> CODEC = simpleCodec(p -> new ArenaBlock(p, Kind.PLAIN));
    private final Kind kind;

    public ArenaBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !living.getType().is(AllTags.Entities.CAGE_DWELLERS)) {
            if (kind == Kind.HELLFIRE && !living.isSteppingCarefully() && living.tickCount % 10 == 0) {
                living.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, null), 2.0f);
            } else if (kind == Kind.FROST && living.canFreeze()) {
                // Chills (and slows) but stops short of a full freeze; only the Cage attack freezes solid.
                int cap = (int) (living.getTicksRequiredToFreeze() * 0.7f);
                if (living.getTicksFrozen() < cap) living.setTicksFrozen(Math.min(cap, living.getTicksFrozen() + 4));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
