package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.UUID;

/**
 * Creatures bound by the Contract Book (v0.35): the contract is two tags on the creature (whose it is, until when) and one on
 * the hunter (the bound creature, and when they may bind again). While it holds, the creature never turns on its master and
 * goes for whatever their master fights (or whatever fights them); it keeps near them. When it lapses, it is itself again.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ContractBindings {

    public static final String OWNER = "sn_contract_owner", UNTIL = "sn_contract_until";
    public static final String BOUND = "sn_contract_bound", READY = "sn_contract_ready";

    private ContractBindings() {
    }

    public static @Nullable UUID owner(Entity e) {
        CompoundTag t = e.getPersistentData();
        return t.hasUUID(OWNER) ? t.getUUID(OWNER) : null;
    }

    public static boolean resting(ServerPlayer p, long now) {
        return p.getPersistentData().getLong(READY) > now;
    }

    public static void bind(Mob mob, ServerPlayer owner, long now) {
        if (owner.getPersistentData().hasUUID(BOUND) && owner.serverLevel().getEntity(owner.getPersistentData().getUUID(BOUND)) instanceof Mob old) {
            lapse(old);
        }
        mob.getPersistentData().putUUID(OWNER, owner.getUUID());
        mob.getPersistentData().putLong(UNTIL, now + CrowleyBalance.BOOK_SERVE_TICKS);
        mob.setTarget(null);
        mob.setPersistenceRequired();
        owner.getPersistentData().putUUID(BOUND, mob.getUUID());
        owner.getPersistentData().putLong(READY, now + CrowleyBalance.BOOK_SERVE_TICKS + CrowleyBalance.BOOK_COOLDOWN);
    }

    /** The contract lapses: smoke, and it is its own creature again. */
    public static void lapse(Mob mob) {
        mob.getPersistentData().remove(OWNER);
        mob.getPersistentData().remove(UNTIL);
        mob.setTarget(null);
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || mob.tickCount % 10 != 0) return;
        UUID ownerId = owner(mob);
        if (ownerId == null) return;
        ServerLevel level = (ServerLevel) mob.level();
        if (level.getGameTime() > mob.getPersistentData().getLong(UNTIL)) {
            ServerPlayer o = level.getServer().getPlayerList().getPlayer(ownerId);
            if (o != null) o.displayClientMessage(Component.translatable("message.supernaturalcraft.contract_book.lapsed", mob.getDisplayName())
                    .withStyle(ChatFormatting.GRAY), true);
            lapse(mob);
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level) return;
        LivingEntity foe = owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive() ? owner.getLastHurtByMob()
                : owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive() ? owner.getLastHurtMob() : null;
        if (foe != null && foe != mob && !ownerId.equals(owner(foe)) && foe.distanceToSqr(mob) < 24 * 24) mob.setTarget(foe);
        else if (mob.getTarget() == null && mob.distanceToSqr(owner) > 6 * 6) mob.getNavigation().moveTo(owner, 1.1);
        if (mob.distanceToSqr(owner) > 32 * 32) mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        UUID ownerId = owner(event.getEntity());
        LivingEntity t = event.getNewAboutToBeSetTarget();
        if (ownerId == null || t == null) return;
        if (t.getUUID().equals(ownerId) || ownerId.equals(owner(t))) event.setCanceled(true);
    }
}
