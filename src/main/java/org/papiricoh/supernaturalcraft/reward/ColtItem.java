package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.network.ColtActionPayload;
import org.papiricoh.supernaturalcraft.network.ColtInputPayload;
import org.papiricoh.supernaturalcraft.network.ColtShotPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.colt.ColtAnimations;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import org.papiricoh.supernaturalcraft.reward.colt.ColtShot;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * The Colt: a Paterson revolver of 1836, five consecrated rounds, and "there's nothing it can't
 * kill". A round executes any demon or boss's summon outright and hits a boss for exact damage
 * (see {@link org.papiricoh.supernaturalcraft.entity.boss.BossDamage}).
 *
 * <p>The server decides every shot; the shooter's client predicts its own (animation, recoil,
 * sound) so the gun answers at once. Reloading seats one round at a time from the inventory and
 * is cut short by firing or by putting the gun away.
 */
public class ColtItem extends Item implements GeoItem {

    public static final int CAPACITY = 5;
    public static final int DRY_COOLDOWN = 7;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ColtItem(Properties properties) {
        super(properties.stacksTo(1).component(AllDataComponents.COLT_AMMO, 0).component(AllDataComponents.COLT_CHAMBER, 0));
    }

    public static int rounds(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.COLT_AMMO, 0);
    }

    public static int chamber(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.COLT_CHAMBER, 0);
    }

    public static boolean reloading(ItemStack stack) {
        return stack.has(AllDataComponents.COLT_RELOAD);
    }

    public static int cooldown() {
        return SNConfig.SPEC.isLoaded() ? SNConfig.COLT_COOLDOWN.get() : 16;
    }

    /** Whether a pull of the trigger fires a round (and is worth predicting on the client). */
    public static boolean canFire(Player player, ItemStack stack) {
        return rounds(stack) > 0 || player.getAbilities().instabuild;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) startReload(sp, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (!canFire(player, stack)) {
            if (player instanceof ServerPlayer sp) {
                abortReload(sp, stack);
                dryFire(sp, stack);
            }
            player.getCooldowns().addCooldown(this, DRY_COOLDOWN);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) {
            org.papiricoh.supernaturalcraft.client.SNClientHooks.predictColtShot(player, stack);
        } else if (player instanceof ServerPlayer sp) {
            abortReload(sp, stack);
            fire(sp, stack);
        }
        player.getCooldowns().addCooldown(this, cooldown());
        return InteractionResultHolder.consume(stack);
    }

    /** Fires one round from {@code stack}: the trace, the blow, the spent round, and word to everyone near. */
    public static ColtShot.Outcome fire(ServerPlayer sp, ItemStack stack) {
        ServerLevel level = sp.serverLevel();
        HitResult hit = ColtShot.trace(sp, SNConfig.COLT_RANGE.get());
        ColtShot.Outcome outcome;
        int target = 0;
        if (hit instanceof EntityHitResult ehr) {
            Entity struck = ehr.getEntity();
            target = struck.getId() + 1;
            outcome = ColtShot.strike(level, sp, struck);
        } else {
            outcome = hit.getType() == HitResult.Type.MISS ? ColtShot.Outcome.MISS : ColtShot.Outcome.BLOCK;
        }
        int left = rounds(stack) - (sp.getAbilities().instabuild ? 0 : 1);
        stack.set(AllDataComponents.COLT_AMMO, Math.max(0, left));
        stack.set(AllDataComponents.COLT_CHAMBER, (chamber(stack) + 1) % CAPACITY);
        muzzleLight(level, sp);

        Vec3 eye = sp.getEyePosition(), end = hit.getLocation(), mid = eye.add(end).scale(0.5);
        ColtShotPayload payload = new ColtShotPayload(sp.getId(), GeoItem.getOrAssignId(stack, level), end, target,
                (byte) outcome.ordinal(), left <= 0);
        PacketDistributor.sendToPlayersNear(level, null, mid.x, mid.y, mid.z, 64 + eye.distanceTo(end) / 2, payload);
        return outcome;
    }

    private static void muzzleLight(ServerLevel level, ServerPlayer sp) {
        if (!SNConfig.COLT_MUZZLE_LIGHT.get()) return;
        BlockPos at = BlockPos.containing(sp.getEyePosition().add(sp.getLookAngle()));
        ArenaController arena = ArenaSavedData.get(level).at(at.getCenter());
        // The Darkness swallows the flash.
        if (arena != null && arena.theme() == ArenaTheme.DARKNESS) return;
        TempLights.place(level, at, 13, 3);
    }

    private static void dryFire(ServerPlayer sp, ItemStack stack) {
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.colt.empty").withStyle(ChatFormatting.GRAY), true);
        action(sp, stack, ColtActionPayload.DRY_FIRE, 0);
    }

    // --- reloading --------------------------------------------------------------------------

    /** Begins seating rounds from the inventory, one at a time. False if there is nothing to do. */
    public static boolean startReload(ServerPlayer sp, ItemStack stack) {
        if (reloading(stack) || sp.getCooldowns().isOnCooldown(stack.getItem())) return false;
        int missing = CAPACITY - rounds(stack);
        if (missing <= 0) return false;
        int have = sp.getAbilities().instabuild ? missing : countBullets(sp);
        if (have <= 0) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.colt.no_bullets").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int n = Math.min(missing, have);
        stack.set(AllDataComponents.COLT_RELOAD, new ColtReload.State(sp.serverLevel().getGameTime(), n, 0));
        action(sp, stack, ColtActionPayload.RELOAD, n);
        return true;
    }

    /**
     * Advances a reload to {@code now}: seats every round due (each one taken from the inventory),
     * and ends it when the gun is closed. Public so tests can drive it; FakePlayers are never ticked.
     */
    public static void tickReload(ServerPlayer sp, ItemStack stack, boolean selected, long now) {
        ColtReload.State state = stack.get(AllDataComponents.COLT_RELOAD);
        if (state == null) return;
        if (!selected) {
            abortReload(sp, stack);
            return;
        }
        int due = ColtReload.seatedBy(state.start(), state.count(), now);
        while (state.seated() < due) {
            if (!sp.getAbilities().instabuild && !takeBullet(sp)) {
                // The rounds ran out mid-reload (dropped, stored): close the gun on what is in.
                abortReload(sp, stack);
                return;
            }
            stack.set(AllDataComponents.COLT_AMMO, Math.min(CAPACITY, rounds(stack) + 1));
            state = state.seatOne();
        }
        if (state.finishedBy(now)) stack.remove(AllDataComponents.COLT_RELOAD);
        else stack.set(AllDataComponents.COLT_RELOAD, state);
    }

    public static void abortReload(ServerPlayer sp, ItemStack stack) {
        if (!reloading(stack)) return;
        stack.remove(AllDataComponents.COLT_RELOAD);
        action(sp, stack, ColtActionPayload.RELOAD_ABORT, 0);
    }

    private static int countBullets(Player p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(AllItems.COLT_BULLET.get())) n += s.getCount();
        }
        return n;
    }

    private static boolean takeBullet(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(AllItems.COLT_BULLET.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    // --- keys and broadcast -----------------------------------------------------------------

    /** The reload or inspect key, pressed with the Colt in hand. */
    public static void input(ServerPlayer sp, byte action) {
        ItemStack stack = sp.getMainHandItem();
        if (!(stack.getItem() instanceof ColtItem)) return;
        if (action == ColtInputPayload.RELOAD) {
            startReload(sp, stack);
        } else if (action == ColtInputPayload.INSPECT && !reloading(stack) && !sp.getCooldowns().isOnCooldown(stack.getItem())) {
            action(sp, stack, ColtActionPayload.INSPECT, 0);
        }
    }

    private static void action(ServerPlayer sp, ItemStack stack, byte action, int arg) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp,
                new ColtActionPayload(sp.getId(), GeoItem.getOrAssignId(stack, sp.serverLevel()), action, (byte) arg));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer sp)) return;
        if (selected) GeoItem.getOrAssignId(stack, sp.serverLevel());
        tickReload(sp, stack, selected, level.getGameTime());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Rounds, chamber and reload all change the stack; only a different gun is drawn anew.
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // --- animation --------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation cocked = RawAnimation.begin().thenLoop(ColtAnimations.PREFIX + ColtAnimations.IDLE_COCKED);
        RawAnimation uncocked = RawAnimation.begin().thenLoop(ColtAnimations.PREFIX + ColtAnimations.IDLE_UNCOCKED);
        AnimationController<ColtItem> main = new AnimationController<>(this, "main", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            return state.setAndContinue(stack != null && rounds(stack) > 0 ? cocked : uncocked);
        });
        for (String name : ColtAnimations.triggerables(CAPACITY)) {
            main.triggerableAnim(name, RawAnimation.begin().thenPlay(ColtAnimations.PREFIX + name));
        }
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- tooltip and bar --------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.the_colt.rounds", rounds(stack), CAPACITY).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.the_colt").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.the_colt.controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * rounds(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD9A92B;
    }
}
