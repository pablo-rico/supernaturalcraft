package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.UUID;

/**
 * Hex bags: how a curse bag afflicts whoever lingers by it (or opens the chest it hides in, or
 * carries it unknowingly), what a protection bag wards off, and how either kind is burned.
 *
 * <p>A curse spares its maker. Each pulse (every {@link #PULSE} ticks) it lays {@code JINXED} and
 * {@code BLEEDING} on its victims; bleeding grows with exposure, one level for every
 * {@link #PULSES_PER_LEVEL} pulses, and exposure ebbs away once the victim is out of reach. Now and
 * then a curse also draws misfortune: lightning, a swarm of vermin, or after dark a demon.
 */
public final class HexBags {

    /** How close a hidden curse bag reaches. */
    public static final double RADIUS = 8;
    /** Ticks between a curse bag's pulses. */
    public static final int PULSE = 40;
    /** A pulse's jinx lasts this long (refreshed while exposed). */
    public static final int JINX_TICKS = 200;
    /** Opening a chest with a curse bag inside jinxes for this long. */
    public static final int CHEST_JINX_TICKS = 1200;
    /** A pulse's bleeding lasts this long (refreshed while exposed). */
    public static final int BLEED_TICKS = 100;
    public static final int MAX_BLEEDING = 4;
    public static final int PULSES_PER_LEVEL = 4;
    /** Exposure lost per this many ticks away from any curse. */
    public static final int DECAY_TICKS = 100;
    /** A new protection bag holds 24000 ticks (20 minutes) of demons nearby. */
    public static final int PROTECTION_CHARGE = 24000;
    /** A hit on a demon provokes it for this long, protection bag or not. */
    public static final int PROVOKE_TICKS = 200;

    private static final String EXPOSURE = "supernaturalcraft.hex_exposure";
    private static final String LAST = "supernaturalcraft.hex_last";
    private static final String PROVOKED = "supernaturalcraft.provoked_until";

    private HexBags() {
    }

    // --- the bags ------------------------------------------------------------------------------

    public static ItemStack curseBag(UUID maker) {
        ItemStack stack = new ItemStack(AllItems.CURSE_BAG.get());
        stack.set(AllDataComponents.HEX_BAG.get(), new HexBag(maker, 0));
        return stack;
    }

    public static ItemStack protectionBag(UUID maker) {
        ItemStack stack = new ItemStack(AllItems.PROTECTION_BAG.get());
        stack.set(AllDataComponents.HEX_BAG.get(), new HexBag(maker, PROTECTION_CHARGE));
        return stack;
    }

    /** Who made a bag, or null for one without a maker (creative, commands): such a curse spares nobody. */
    @Nullable
    public static UUID maker(ItemStack stack) {
        HexBag bag = stack.get(AllDataComponents.HEX_BAG.get());
        return bag == null ? null : bag.maker();
    }

    /** The first protection bag with charge left that {@code player} carries, or empty. */
    public static ItemStack protectionBag(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(AllItems.PROTECTION_BAG.get()) && ProtectionBagItem.charge(s) > 0) return s;
        }
        return ItemStack.EMPTY;
    }

    /** Whether {@code entity} carries a charged protection bag. */
    public static boolean isProtected(LivingEntity entity) {
        return entity instanceof Player p && !protectionBag(p).isEmpty();
    }

    // --- afflictions ---------------------------------------------------------------------------

    /**
     * One pulse of a curse on {@code victim}. Spares the curse's {@code maker}, spectators and
     * anyone carrying a protection bag.
     *
     * @param pulses     how much exposure this counts for (a pulse is 1)
     * @param jinxTicks  how long the jinx lasts
     * @param misfortune whether the curse may also draw misfortune this time
     * @return whether the victim was afflicted
     */
    public static boolean afflict(LivingEntity victim, @Nullable UUID maker, int pulses, int jinxTicks, boolean misfortune) {
        if (!(victim.level() instanceof ServerLevel level) || !victim.isAlive()) return false;
        if (maker != null && maker.equals(victim.getUUID())) return false;
        if (victim instanceof Player p && (p.isSpectator() || isProtected(p))) return false;
        boolean fresh = !victim.hasEffect(AllMobEffects.JINXED);
        victim.addEffect(new MobEffectInstance(AllMobEffects.JINXED, jinxTicks, 0, false, true, true));
        int exposure = expose(victim, level.getGameTime(), pulses);
        victim.addEffect(new MobEffectInstance(AllMobEffects.BLEEDING, BLEED_TICKS, bleedingLevel(exposure), false, true, true));
        if (fresh) {
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), AllSounds.HEX_CURSE.get(), SoundSource.HOSTILE, 0.7f, 1.0f);
        }
        if (misfortune) misfortune(level, victim, level.random);
        return true;
    }

    /** Bleeding amplifier for an exposure (pulses in reach of a curse, less what has ebbed away). */
    public static int bleedingLevel(int exposure) {
        return Math.max(0, Math.min(MAX_BLEEDING, (exposure - 1) / PULSES_PER_LEVEL));
    }

    /** Current exposure of {@code entity} (0 if never cursed). */
    public static int exposure(LivingEntity entity) {
        return entity.getPersistentData().getInt(EXPOSURE);
    }

    private static int expose(LivingEntity victim, long now, int pulses) {
        CompoundTag data = victim.getPersistentData();
        int exposure = data.getInt(EXPOSURE);
        if (data.contains(LAST)) {
            long gap = now - data.getLong(LAST);
            if (gap > PULSE * 2L) exposure = Math.max(0, exposure - (int) (gap / DECAY_TICKS));
        } else {
            exposure = 0;
        }
        exposure = Math.min(PULSES_PER_LEVEL * (MAX_BLEEDING + 2), exposure + pulses);
        data.putInt(EXPOSURE, exposure);
        data.putLong(LAST, now);
        return exposure;
    }

    /** A curse bag {@code holder} carries that someone else made: it afflicts the carrier. @return afflicted */
    public static boolean afflictCarrier(Player holder, boolean misfortune) {
        Inventory inv = holder.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(AllItems.CURSE_BAG.get())) continue;
            UUID maker = maker(s);
            if (maker == null || !maker.equals(holder.getUUID())) return afflict(holder, maker, 1, JINX_TICKS, misfortune);
        }
        return false;
    }

    /** {@code opener} looked into {@code menu}: a curse bag inside (not in their own pockets) gets them. */
    public static boolean afflictOpener(Player opener, AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            ItemStack s = slot.getItem();
            if (s.is(AllItems.CURSE_BAG.get())) return afflict(opener, maker(s), PULSES_PER_LEVEL, CHEST_JINX_TICKS, true);
        }
        return false;
    }

    /** The bad luck a curse draws: lightning near the victim, a swarm, or after dark a demon. Rare. */
    static void misfortune(ServerLevel level, LivingEntity victim, RandomSource random) {
        int roll = random.nextInt(600);
        if (roll > 3) return;
        BlockPos near = victim.blockPosition().offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
        if (roll == 0) {
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, near);
            if (!level.canSeeSky(ground) || Math.abs(ground.getY() - victim.getBlockY()) > 6) return;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) return;
            bolt.moveTo(Vec3.atBottomCenterOf(ground));
            level.addFreshEntity(bolt);
        } else if (roll <= 2) {
            BlockPos at = level.getBlockState(near).isAir() ? near : victim.blockPosition();
            boolean bats = random.nextBoolean();
            for (int i = 0; i < (bats ? 4 : 3); i++) {
                if (bats) EntityType.BAT.spawn(level, at.above(), MobSpawnType.EVENT);
                else EntityType.SILVERFISH.spawn(level, at, MobSpawnType.EVENT);
            }
        } else if (level.isNight() && level.canSeeSky(victim.blockPosition())) {
            double angle = random.nextDouble() * Math.PI * 2;
            BlockPos far = victim.blockPosition().offset((int) (Math.cos(angle) * 10), 0, (int) (Math.sin(angle) * 10));
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, far);
            if (Math.abs(ground.getY() - victim.getBlockY()) <= 8) AllEntities.BLACK_EYED_DEMON.get().spawn(level, ground, MobSpawnType.EVENT);
        }
    }

    // --- burning -------------------------------------------------------------------------------

    /**
     * Burns every curse bag within {@code radius} of {@code center}: bags hidden as blocks, and bags
     * tucked into chests and other containers.
     *
     * @return how many bags burned
     */
    public static int burnNear(ServerLevel level, BlockPos center, double radius) {
        int r = (int) Math.ceil(Math.min(radius, 32));
        double r2 = radius * radius;
        int burned = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (p.distSqr(center) > r2) continue;
            BlockState state = level.getBlockState(p);
            if (state.is(AllBlocks.CURSE_BAG.get())) {
                burnBlock(level, p.immutable());
                burned++;
            } else if (state.hasBlockEntity() && level.getBlockEntity(p) instanceof Container container) {
                int n = burnIn(container);
                if (n > 0) {
                    flames(level, Vec3.atCenterOf(p), 12);
                    burned += n;
                }
            }
        }
        return burned;
    }

    /** Burns the curse bags in {@code player}'s inventory. @return how many */
    public static int burnCarried(Player player) {
        int n = burnIn(player.getInventory());
        if (n > 0 && player.level() instanceof ServerLevel level) {
            flames(level, player.position().add(0, 1, 0), 16);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.hex_bag.burned_carried").withStyle(ChatFormatting.GOLD), true);
        }
        return n;
    }

    private static int burnIn(Container container) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.is(AllItems.CURSE_BAG.get())) {
                n += s.getCount();
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        if (n > 0) container.setChanged();
        return n;
    }

    /** Burns the curse bag block at {@code pos} to nothing, in a burst of flame. */
    public static void burnBlock(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        flames(level, Vec3.atCenterOf(pos), 24);
    }

    static void flames(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, count, 0.25, 0.3, 0.25, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.2, at.z, count / 2, 0.2, 0.3, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.3, at.z, 3, 0.1, 0.2, 0.1, 0.03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.6f, 1.3f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.8f);
    }

    // --- slipping a bag to someone -------------------------------------------------------------

    /**
     * {@code from} slips one curse bag from {@code bag} into {@code to}'s inventory. A protection bag
     * turns it away. The victim is not told.
     *
     * @return whether the bag changed hands
     */
    public static boolean slip(Player from, Player to, ItemStack bag) {
        if (to == from || !to.isAlive() || !bag.is(AllItems.CURSE_BAG.get())) return false;
        if (isProtected(to)) {
            from.displayClientMessage(Component.translatable("message.supernaturalcraft.hex_bag.slip_warded", to.getDisplayName())
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ItemStack one = bag.copyWithCount(1);
        if (one.get(AllDataComponents.HEX_BAG.get()) == null) one.set(AllDataComponents.HEX_BAG.get(), new HexBag(from.getUUID(), 0));
        if (!to.getInventory().add(one)) {
            from.displayClientMessage(Component.translatable("message.supernaturalcraft.hex_bag.slip_full", to.getDisplayName())
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!from.hasInfiniteMaterials()) bag.shrink(1);
        from.displayClientMessage(Component.translatable("message.supernaturalcraft.hex_bag.slipped", to.getDisplayName())
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    // --- demons and protection bags ------------------------------------------------------------

    /** Whether a protection bag works against {@code mob}: a demon, not a boss. */
    public static boolean wardedOff(Mob mob) {
        return mob.getType().is(AllTags.Entities.DEMONS) && !mob.getType().is(AllTags.Entities.BOSSES);
    }

    public static void provoke(Mob demon, int ticks) {
        demon.getPersistentData().putLong(PROVOKED, demon.level().getGameTime() + ticks);
    }

    public static boolean provoked(Mob demon) {
        return demon.getPersistentData().getLong(PROVOKED) > demon.level().getGameTime();
    }

    /** Whether {@code demon} may set its sights on {@code target}. */
    public static boolean mayTarget(Mob demon, LivingEntity target) {
        return !(target instanceof Player p) || !wardedOff(demon) || provoked(demon) || !isProtected(p);
    }
}
