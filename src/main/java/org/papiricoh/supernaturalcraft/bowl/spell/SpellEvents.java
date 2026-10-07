package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * Game-bus rules for the bowl spells: drawing blood, collaring pets and keeping the pet ledger,
 * Concealment, and holding bound creatures.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SpellEvents {

    /** Collared pets report where they are this often. */
    public static final int LEDGER_INTERVAL = 200;

    private SpellEvents() {
    }

    // --- blood and collars ----------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        boolean client = event.getLevel().isClientSide;
        if (held.is(Items.GLASS_BOTTLE) && event.getTarget() instanceof Player target && target.isAlive()) {
            if (!client) BloodVialItem.draw(player, event.getHand(), target);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(client));
        } else if (held.is(AllItems.PET_COLLAR.get()) && event.getTarget() instanceof TamableAnimal pet) {
            // Before the animal's own interaction (which would sit it down).
            event.setCanceled(true);
            event.setCancellationResult(PetCollarItem.interact(held, player, pet));
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !event.getItemStack().is(Items.GLASS_BOTTLE)) return;
        if (aimsAtWater(player)) return; // filling a bottle with water works as ever
        if (!event.getLevel().isClientSide) BloodVialItem.draw(player, event.getHand(), player);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    /** Whether a glass bottle used now would scoop up water (what {@code BottleItem} looks for). */
    static boolean aimsAtWater(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        FluidState fluid = player.level().getFluidState(hit.getBlockPos());
        return fluid.is(FluidTags.WATER);
    }

    // --- the pet ledger ------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.level().isClientSide) return;
        if (e instanceof TamableAnimal pet && e.tickCount % LEDGER_INTERVAL == 0 && collared(pet) && pet.isAlive()) {
            PetLedger.get(e.getServer()).seen(pet);
        }
        if (e instanceof LivingEntity living && e.tickCount % Bindings.INTERVAL == 0 && e.hasData(AllAttachments.BINDING)) {
            Bindings.tick(living);
        }
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof TamableAnimal pet)) return;
        if (pet.isAlive() && !pet.isDeadOrDying() && collared(pet)) PetLedger.get(pet.getServer()).seen(pet);
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof TamableAnimal pet)) return;
        if (pet.isAlive() && collared(pet) && pet.getServer() != null) PetLedger.get(pet.getServer()).seen(pet);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof TamableAnimal pet)) return;
        if (!pet.isTame() || pet.getOwnerUUID() == null) return;
        boolean wearsCollar = collared(pet);
        if (!wearsCollar && !pet.hasCustomName()) return;
        PetLedger.get(pet.getServer()).died(pet);
        // A named pet that wore no collar leaves one behind, bound to it: the way back to it.
        if (!wearsCollar) pet.spawnAtLocation(PetCollarItem.boundTo(pet));
    }

    static boolean collared(Entity e) {
        return e.hasData(AllAttachments.COLLARED) && e.getData(AllAttachments.COLLARED);
    }

    // --- Concealment ---------------------------------------------------------------------------

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (Concealment.hides(event.getEntity(), event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (Concealment.hides(event.getLookingEntity(), event.getEntity())) event.modifyVisibility(0);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide || !event.getEffectInstance().is(AllMobEffects.CONCEALED)) return;
        Concealment.scatter(event.getEntity());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker && Concealment.affects(event.getEntity())) {
            Concealment.broken(attacker);
        }
    }
}
