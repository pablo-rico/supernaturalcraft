package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.spell.Bindings;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.UUID;

/**
 * The mob effects of the v0.21 witchcraft (the bowl's curses and protections, and two sigils). Registered in
 * {@code registry.AllMobEffects}; their names come from their ids.
 */
public final class CurseEffects {

    private CurseEffects() {
    }

    /** A plain effect that only marks its bearer (the rules that read it live elsewhere). */
    public static class Marker extends MobEffect {
        public Marker(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    /**
     * A protection that others must be able to see (v0.21: Ward of the Name, Mirror Hex): as it starts and every
     * {@link #SHOWN_EVERY} ticks after, the players tracking its bearer are told what guards them
     * ({@code MagicFx.wardState}: HEX_PULSE / {@code HEX_WARDS}); its end is sent by {@code HexEvents}.
     */
    public static class Shield extends Marker {
        /** How often the state is refreshed for onlookers (each refresh is valid a little longer than this). */
        public static final int SHOWN_EVERY = 40;

        public Shield(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % SHOWN_EVERY == 0;
        }

        @Override
        public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
            if (!entity.level().isClientSide) org.papiricoh.supernaturalcraft.magic.fx.MagicFx.wardState(entity, null);
            return true;
        }

        @Override
        public void onEffectStarted(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
            if (!entity.level().isClientSide) org.papiricoh.supernaturalcraft.magic.fx.MagicFx.wardState(entity, null);
        }
    }

    /**
     * The Curse of Pain: a pulse of magic harm every {@link #PULSE} ticks. Like poison it never kills: it stops at one heart's
     * half. Bosses never carry it (the curse refuses them).
     */
    public static class Agony extends MobEffect {
        public static final int PULSE = 40;
        public static final float DAMAGE = 1.5f;

        public Agony() {
            super(MobEffectCategory.HARMFUL, 0x8E1F4A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % PULSE == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || Bindings.isBoss(entity)) return true;
            float dmg = Math.min(DAMAGE * (amplifier + 1), entity.getHealth() - 1f);
            if (dmg > 0) entity.hurt(entity.damageSources().magic(), dmg);
            return true;
        }
    }

    /** The Hex of Rot: now and then a bite of food in the bearer's pack turns to rotten flesh. */
    public static class Rot extends MobEffect {
        public static final int PULSE = 200;

        public Rot() {
            super(MobEffectCategory.HARMFUL, 0x5B6A2A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % PULSE == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!entity.level().isClientSide && entity instanceof Player player) rotOne(player);
            return true;
        }

        /** Turns one item of food (not already rotten) into rotten flesh. @return whether anything rotted */
        public static boolean rotOne(Player player) {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.has(DataComponents.FOOD) && !s.is(Items.ROTTEN_FLESH)) {
                    s.shrink(1);
                    ItemStack rot = new ItemStack(Items.ROTTEN_FLESH);
                    if (!inv.add(rot)) player.drop(rot, false);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Compelled (the {@code compel} spell): a creature follows the one who compelled it and will not raise a hand against them
     * ({@code CurseEvents}); a player is drawn, slowly, toward the bowl that spoke it. Who and where are kept in the bearer's
     * persistent data ({@link #COMPELLER}, {@link #ANCHOR}).
     */
    public static class Compelled extends MobEffect {
        public static final String COMPELLER = "supernaturalcraft_compeller", ANCHOR = "supernaturalcraft_compel_anchor";
        /** How hard a compelled player is drawn each tick (blocks/tick). */
        public static final double PULL = 0.06;

        public Compelled() {
            super(MobEffectCategory.HARMFUL, 0xE8A33A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return true;
            CompoundTag data = entity.getPersistentData();
            if (entity instanceof Player player) {
                if (!data.contains(ANCHOR)) return true;
                Vec3 to = Vec3.atBottomCenterOf(BlockPos.of(data.getLong(ANCHOR))).subtract(player.position()).multiply(1, 0, 1);
                if (to.lengthSqr() > 2.25) {
                    Vec3 step = to.normalize().scale(PULL);
                    player.setDeltaMovement(player.getDeltaMovement().add(step.x, 0, step.z));
                    player.hurtMarked = true;
                }
                return true;
            }
            if (entity instanceof Mob mob && entity.tickCount % 10 == 0) {
                Player master = compeller(mob);
                if (master != null && master.level() == mob.level() && mob.distanceToSqr(master) > 9) {
                    mob.getNavigation().moveTo(master, 1.0);
                }
                if (master != null && mob.getTarget() == master) mob.setTarget(null);
            }
            return true;
        }

        /** Who compelled {@code entity}, if they are about. */
        public static @Nullable Player compeller(LivingEntity entity) {
            CompoundTag data = entity.getPersistentData();
            if (!data.hasUUID(COMPELLER)) return null;
            UUID id = data.getUUID(COMPELLER);
            return Players.byUuid(entity.level(), id);
        }
    }
}
