package org.papiricoh.supernaturalcraft.reward.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.trickster.TricksterMarks;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * The Trickster's Remote (v0.14), left by Gabriel: point it at a creature and "change its channel" at random: it shrinks to
 * half its size, it wears a disguise (a party hat and a name off the TV), or it turns into another creature of about its
 * size from a safe list (keeping how hurt it was). Each lasts {@link #CHANNEL_TICKS} but the swap, which stays. Never a
 * player, a boss, a named creature, a working villager or one of Gabriel's doubles; never a swap for someone's pet or a
 * creature of the supernatural. {@link #COOLDOWN} between uses. Drawn with its own GeckoLib model (renderer by the client).
 */
public class TricksterRemoteItem extends Item implements GeoItem {

    public static final int COOLDOWN = 600;
    /** How long a shrink or a disguise lasts. */
    public static final int CHANNEL_TICKS = 600;

    /** What the remote does to a creature. */
    public enum Change {
        SHRINK, DISGUISE, SWAP
    }

    /** The disguises' names. */
    public static final List<String> TV_NAMES = List.of("Dr. Sexy", "Dr. Piccolo", "Nurse Ellen", "Agent Dean Cassidy", "Sexy Dr. Sam",
            "Herpexia Guy", "Lieutenant Columbo", "Your Host", "Contestant No. 3", "The Lead");

    /** Swaps by size: small, middling, tall (no hostile, nothing that remembers an owner). */
    public static List<EntityType<?>> swapsFor(float height) {
        if (height < 0.8f) {
            return List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.FOX, EntityType.FROG, EntityType.PARROT, EntityType.CAT,
                    EntityType.OCELOT);
        }
        if (height < 1.5f) return List.of(EntityType.PIG, EntityType.SHEEP, EntityType.COW, EntityType.GOAT, EntityType.MOOSHROOM);
        return List.of(EntityType.HORSE, EntityType.DONKEY, EntityType.LLAMA, EntityType.SNOW_GOLEM);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TricksterRemoteItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    /** Whether the remote works on {@code target} at all. */
    public static boolean changeable(LivingEntity target) {
        if (!(target instanceof Mob) || target instanceof GabrielDoubleEntity) return false;
        if (target.getType().is(AllTags.Entities.BOSSES) || target.hasCustomName() || !target.isAlive()) return false;
        if (target instanceof Villager v) {
            VillagerProfession job = v.getVillagerData().getProfession();
            if (job != VillagerProfession.NONE && job != VillagerProfession.NITWIT) return false;
        }
        return true;
    }

    /** Whether {@code target} may be swapped for another creature (no pets, nothing supernatural). */
    public static boolean swappable(LivingEntity target) {
        if (target instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        return !target.getType().is(AllTags.Entities.SUPERNATURAL);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!changeable(target) || player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        RandomSource random = player.getRandom();
        Change change = Change.values()[random.nextInt(swappable(target) ? 3 : 2)];
        if (change(level, target, change, random) == null) return InteractionResult.PASS;
        player.getCooldowns().addCooldown(this, COOLDOWN);
        level.playSound(null, player.blockPosition(), AllSounds.GABRIEL_SNAP.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        level.playSound(null, target.blockPosition(), AllSounds.GABRIEL_STATIC.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    /**
     * Changes {@code target}'s channel.
     *
     * @return what it is now (the new creature after a swap), or null if nothing changed
     */
    public static @Nullable LivingEntity change(ServerLevel level, LivingEntity target, Change change, RandomSource random) {
        level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
        switch (change) {
            case SHRINK -> {
                return TricksterMarks.shrink(target, CHANNEL_TICKS) ? target : null;
            }
            case DISGUISE -> {
                TricksterMarks.hat(target, CHANNEL_TICKS);
                TricksterMarks.tvName(target, TV_NAMES.get(random.nextInt(TV_NAMES.size())), CHANNEL_TICKS);
                return target;
            }
            default -> {
                return swap(level, target, random);
            }
        }
    }

    /** Swaps {@code target} for another creature of about its size, as hurt as it was. */
    public static @Nullable LivingEntity swap(ServerLevel level, LivingEntity target, RandomSource random) {
        List<EntityType<?>> options = swapsFor(target.getBbHeight()).stream().filter(t -> t != target.getType()).toList();
        if (options.isEmpty()) return null;
        EntityType<?> type = options.get(random.nextInt(options.size()));
        if (!(type.create(level) instanceof Mob next)) return null;
        float share = target.getHealth() / Math.max(1f, target.getMaxHealth());
        next.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        next.finalizeSpawn(level, level.getCurrentDifficultyAt(next.blockPosition()), MobSpawnType.CONVERSION, null);
        next.setHealth(Math.max(1f, share * next.getMaxHealth()));
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
        target.discard();
        level.addFreshEntity(next);
        return next;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.trickster_remote").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.trickster_remote.use").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
