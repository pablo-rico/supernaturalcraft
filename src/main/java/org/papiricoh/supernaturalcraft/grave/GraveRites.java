package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostBalance;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** Salt and burn: what the hunters do to the bones, and what it does to the ghost bound to them. */
public final class GraveRites {

    private GraveRites() {
    }

    /** Salts bared bones (the caller has checked they are bare and not yet salted, and pays the salt). */
    public static void salt(ServerLevel level, BlockPos pos, @Nullable Player player) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof GraveBonesBlock)) return;
        level.setBlock(pos, s.setValue(GraveBonesBlock.SALTED, true), Block.UPDATE_ALL);
        level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                new ItemStack(AllItems.SALT.get())), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 18, 0.3, 0.1, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0f, 1.3f);
        GraveBonesBlock.tell(player, "salted");
    }

    /**
     * Burns salted bones: the ghost bound to them is laid to rest for good (or, if it is not about,
     * the bones simply rest). Fire, a wail, ectoplasm, one spell page, and the Salt and Burn advancement.
     */
    public static void burn(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!(level.getBlockEntity(pos) instanceof GraveBonesBlockEntity be)) return;
        for (GhostEntity g : boundGhosts(level, pos, be)) g.layToRest();
        be.markRested();

        double x = pos.getX() + 0.5, y = pos.getY() + 0.7, z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 50, 0.35, 0.4, 0.35, 0.03);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.3, z, 25, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.8, z, 20, 0.3, 0.6, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.2, 0.1, 0.2, 0);
        level.playSound(null, pos, AllSounds.GRAVE_BURN.get(), SoundSource.BLOCKS, 1.2f, 1.0f);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);

        BlockPos drop = pos.above();
        Block.popResource(level, drop, new ItemStack(AllItems.ECTOPLASM.get(), 2));
        ResourceLocation spell = org.papiricoh.supernaturalcraft.bowl.BowlSpells.randomSpell(level.getRecipeManager(), level.random);
        if (spell != null) {
            ItemStack page = new ItemStack(AllItems.SPELL_PAGE.get());
            page.set(AllDataComponents.BOWL_SPELL.get(), spell);
            Block.popResource(level, drop, page);
        }
        GraveBonesBlock.tell(player, "burned");
        if (player instanceof ServerPlayer sp) org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(sp, "main/salt_and_burn");
    }

    /** The ghost these bones raised, plus any other loaded ghost that still answers to them. */
    static java.util.List<GhostEntity> boundGhosts(ServerLevel level, BlockPos pos, GraveBonesBlockEntity be) {
        java.util.List<GhostEntity> out = new java.util.ArrayList<>();
        GhostEntity own = be.ghost(level);
        if (own != null) out.add(own);
        double r = GhostBalance.LEASH + 16;
        for (GhostEntity g : level.getEntitiesOfClass(GhostEntity.class, new AABB(pos).inflate(r))) {
            if (!out.contains(g) && pos.equals(g.bones())) out.add(g);
        }
        return out;
    }
}
