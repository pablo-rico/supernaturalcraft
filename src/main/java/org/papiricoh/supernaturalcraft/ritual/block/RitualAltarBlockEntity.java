package org.papiricoh.supernaturalcraft.ritual.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.magic.ArcanaCost;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.ritual.effect.BossSummonEffect;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.RitualInput;
import org.papiricoh.supernaturalcraft.ritual.RitualPattern;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds up to eight offerings and runs one ritual at a time. While channelling, the circle is
 * re-checked twice a second: smudge a line or snuff a candle and the ritual backfires.
 */
public class RitualAltarBlockEntity extends BlockEntity {

    private static final int RECHECK_INTERVAL = 10;

    private final NonNullList<ItemStack> offerings = NonNullList.withSize(RitualRecipe.MAX_OFFERINGS, ItemStack.EMPTY);
    private @Nullable ResourceLocation activeRitual;
    private int progress;
    private int duration;
    private @Nullable UUID ritualist;
    /** The activator a running ritual spends (v0.19): only spent if the ritual takes; otherwise it goes back. */
    private ItemStack reserved = ItemStack.EMPTY;
    /** What the running rite cost its ritualist (v0.21: trains their mastery when it takes). */
    private float paid;

    public RitualAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.RITUAL_ALTAR.get(), pos, state);
    }

    public List<ItemStack> offerings() {
        return offerings;
    }

    public boolean isChanneling() {
        return activeRitual != null;
    }

    public float progressFraction() {
        return duration <= 0 ? 0 : progress / (float) duration;
    }

    // --- interaction ----------------------------------------------------------------------

    /** @return true if the click was handled */
    public boolean onUse(Player player, InteractionHand hand, ItemStack held) {
        if (isChanneling()) {
            message(player, "message.supernaturalcraft.ritual.busy", ChatFormatting.GRAY);
            return true;
        }
        Optional<RecipeHolder<RitualRecipe>> recipe = findRecipe(held);
        if (recipe.isPresent()) {
            tryStart(player, hand, held, recipe.get());
            return true;
        }
        if (held.isEmpty()) return false;
        for (int i = 0; i < offerings.size(); i++) {
            if (offerings.get(i).isEmpty()) {
                offerings.set(i, held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 0.8f);
                changed();
                return true;
            }
        }
        message(player, "message.supernaturalcraft.ritual.altar_full", ChatFormatting.GRAY);
        return true;
    }

    public boolean takeLast(Player player) {
        if (isChanneling()) return false;
        for (int i = offerings.size() - 1; i >= 0; i--) {
            if (!offerings.get(i).isEmpty()) {
                ItemStack stack = offerings.get(i);
                offerings.set(i, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                changed();
                return true;
            }
        }
        return false;
    }

    public Optional<RecipeHolder<RitualRecipe>> findRecipe(ItemStack activator) {
        if (level == null) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(AllRecipes.RITUAL.get(), new RitualInput(List.copyOf(offerings), activator), level);
    }

    private void tryStart(Player player, InteractionHand hand, ItemStack held, RecipeHolder<RitualRecipe> holder) {
        RitualRecipe recipe = holder.value();
        ServerLevel server = (ServerLevel) level;
        RitualPattern pattern = server.registryAccess().registryOrThrow(SNRegistries.RITUAL_PATTERN).get(recipe.pattern());
        if (pattern == null) {
            message(player, "message.supernaturalcraft.ritual.unknown_pattern", ChatFormatting.RED);
            return;
        }
        RitualPattern.Match match = pattern.match(server, worldPosition);
        if (!match.matches()) {
            message(player, "message.supernaturalcraft.ritual.broken_circle", ChatFormatting.RED);
            if (match.firstMismatch() != null && player instanceof ServerPlayer sp) {
                BlockPos p = match.firstMismatch();
                server.sendParticles(sp, new DustParticleOptions(new Vector3f(1f, 0.1f, 0.1f), 1.5f), true,
                        p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 20, 0.25, 0.2, 0.25, 0);
            }
            return;
        }
        String condition = recipe.conditions().check(server, player instanceof ServerPlayer sp ? sp : null);
        if (condition != null) {
            message(player, condition, ChatFormatting.RED);
            return;
        }
        // A summoning that could not take place (another fight holds this world…) is refused before anything is spent.
        Component refusal = recipe.effect().precheck(server, worldPosition, player instanceof ServerPlayer sp ? sp : null);
        if (refusal != null) {
            player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
            return;
        }
        float cost = ArcanaCost.apply(player, ArcanaCost.Tool.RITUAL, recipe.school(), recipe.manaCost());
        if (!ManaManager.tryConsume(player, cost)) {
            message(player, "message.supernaturalcraft.ritual.no_mana", ChatFormatting.RED);
            return;
        }
        if (!player.getAbilities().instabuild) {
            if (recipe.consumeActivator()) {
                reserved = held.copyWithCount(1);
                held.shrink(1);
            } else if (held.isDamageableItem()) {
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }
        activeRitual = holder.id();
        duration = recipe.duration();
        progress = 0;
        ritualist = player.getUUID();
        // Nothing is paid in creative, so nothing trains (v0.21).
        paid = player.getAbilities().instabuild ? 0 : cost;
        // v0.21: the client lights the circle line by line (it has no patterns: the cells travel once).
        MagicFx.ritualCharge(server, worldPosition, recipe.school(), circleCells(pattern, match.rotation()), duration, 0f);
        changed();
    }

    /** The circle's drawn cells (everything but "anything" and the altar), turned as it was matched. */
    private List<BlockPos> circleCells(RitualPattern pattern, int rotation) {
        List<BlockPos> out = new ArrayList<>();
        for (org.papiricoh.supernaturalcraft.ritual.PatternGeometry.Cell c : pattern.cells()) {
            int[] o = org.papiricoh.supernaturalcraft.ritual.PatternGeometry.rotate(c.dx(), c.dz(), rotation);
            out.add(worldPosition.offset(o[0], 0, o[1]));
        }
        return out;
    }

    // --- ticking --------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
        if (!altar.isChanneling()) return;
        ServerLevel server = (ServerLevel) level;
        RecipeHolder<?> holder = server.getRecipeManager().byKey(altar.activeRitual).orElse(null);
        if (holder == null || !(holder.value() instanceof RitualRecipe recipe)) {
            MagicFx.ritualStop(server, pos);
            altar.reset();
            return;
        }
        altar.progress++;
        if (altar.progress % RECHECK_INTERVAL == 0) {
            RitualPattern pattern = server.registryAccess().registryOrThrow(SNRegistries.RITUAL_PATTERN).get(recipe.pattern());
            if (pattern == null || !pattern.match(server, pos).matches()) {
                altar.backlash(server);
                return;
            }
        }
        if (altar.progress >= altar.duration) {
            altar.complete(server, recipe);
        }
    }

    /** The charge is drawn by the client from {@code RitualChargePayload} (v0.21): nothing to do here any more. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
    }

    private void complete(ServerLevel server, RitualRecipe recipe) {
        ServerPlayer player = ritualist == null ? null : (ServerPlayer) org.papiricoh.supernaturalcraft.util.Players.byUuid(server, ritualist);
        // What is spent on a summoning is only put up: the arena it opens gives the progression keys back unless it is won.
        List<ItemStack> spent = new ArrayList<>(offerings);
        spent.add(reserved);
        Stakes.offer(ritualist == null ? null : Stakes.of(ritualist, spent));
        MagicFx.ritualStop(server, worldPosition);
        // A summoning tears a rift over the altar just before what it calls arrives (v0.21).
        if (recipe.effect() instanceof BossSummonEffect summon && summon.precheck(server, worldPosition, player) == null) {
            summon.openRift(server, worldPosition, recipe.school());
        }
        boolean done;
        try {
            done = recipe.effect().perform(server, worldPosition, player);
        } finally {
            Stakes.offer(null);
        }
        if (done) {
            reserved = ItemStack.EMPTY;
            for (int i = 0; i < offerings.size(); i++) {
                ItemStack remainder = offerings.get(i).hasCraftingRemainingItem() ? offerings.get(i).getCraftingRemainingItem() : ItemStack.EMPTY;
                offerings.set(i, remainder);
            }
            MagicFx.ritualComplete(server, worldPosition, recipe.school(), COMPLETE_PILLAR, COMPLETE_WAVE, COMPLETE_TICKS);
            if (SNConfig.MAGIC_TEMP_LIGHTS.get()) TempLights.place(server, worldPosition.above(2), 15, COMPLETE_LIGHT_TICKS);
            if (player != null) Mastery.ritualDone(player, recipe.school(), paid);
        } else {
            if (player != null) message(player, "message.supernaturalcraft.ritual.unanswered", ChatFormatting.GRAY);
            giveBackReserved(server);
        }
        reset();
    }

    /** The activator of a ritual that did not take goes back to its ritualist (or onto the altar's step). */
    private void giveBackReserved(ServerLevel server) {
        if (reserved.isEmpty()) return;
        ItemStack back = reserved;
        reserved = ItemStack.EMPTY;
        if (ritualist != null && org.papiricoh.supernaturalcraft.util.Players.byUuid(server, ritualist) instanceof ServerPlayer p) {
            if (!p.getInventory().add(back)) p.drop(back, false);
        } else if (ritualist != null) {
            Stakes.give(server.getServer(), ritualist, List.of(back), false);
        } else {
            Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, back);
        }
    }

    /** The pillar of a completed rite (blocks), its shockwave's radius, how long it plays, how long the altar stays lit. */
    public static final float COMPLETE_PILLAR = 12f, COMPLETE_WAVE = 6f;
    public static final int COMPLETE_TICKS = 40, COMPLETE_LIGHT_TICKS = 30, FAIL_TICKS = 30;

    /** The circle broke mid-ritual: thunder, a burn, and something answers that shouldn't have. */
    private void backlash(ServerLevel server) {
        MagicFx.ritualStop(server, worldPosition);
        RecipeHolder<?> running = activeRitual == null ? null : server.getRecipeManager().byKey(activeRitual).orElse(null);
        org.papiricoh.supernaturalcraft.magic.school.School school = running != null && running.value() instanceof RitualRecipe r
                ? r.school() : org.papiricoh.supernaturalcraft.magic.school.School.NONE;
        // v0.21: a Ward the ritualist raised over the altar takes the backlash: no burn, and nothing comes through.
        org.papiricoh.supernaturalcraft.entity.magic.WardEntity ward = org.papiricoh.supernaturalcraft.entity.magic.WardEntity
                .shelteringWard(server, ritualist, net.minecraft.world.phys.Vec3.atBottomCenterOf(worldPosition.above()));
        MagicFx.ritualFail(server, worldPosition, school, 4f, ward == null && SNConfig.BACKLASH_SPAWNS_DEMON.get(), FAIL_TICKS);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        giveBackReserved(server);
        ServerPlayer player = ritualist != null && org.papiricoh.supernaturalcraft.util.Players.byUuid(server, ritualist) instanceof ServerPlayer p ? p : null;
        if (ward != null) {
            MagicFx.ward(server, ward.getSchool(), ward, ward.position(), ward.getRadius(),
                    org.papiricoh.supernaturalcraft.network.MagicFxPayload.WARD_HIT, org.papiricoh.supernaturalcraft.magic.spell.form.SpellForms.WARD_PULSE_TICKS);
            if (player != null) message(player, "message.supernaturalcraft.ritual.backlash_warded", ChatFormatting.GOLD);
            reset();
            return;
        }
        if (player != null) {
            player.hurt(AllDamageTypes.source(server, AllDamageTypes.HELLFIRE, null), 4.0f);
            message(player, "message.supernaturalcraft.ritual.backlash", ChatFormatting.DARK_RED);
        }
        if (SNConfig.BACKLASH_SPAWNS_DEMON.get()) {
            AllEntities.BLACK_EYED_DEMON.get().spawn(server, worldPosition.above(), MobSpawnType.EVENT);
        }
        reset();
    }

    private void reset() {
        activeRitual = null;
        progress = 0;
        duration = 0;
        ritualist = null;
        paid = 0;
        changed();
    }

    void dropOfferings() {
        if (level instanceof ServerLevel server && isChanneling()) MagicFx.ritualStop(server, worldPosition);
        if (level != null) {
            List<ItemStack> drops = new ArrayList<>();
            offerings.forEach(s -> {
                if (!s.isEmpty()) drops.add(s);
            });
            if (!reserved.isEmpty()) drops.add(reserved);
            reserved = ItemStack.EMPTY;
            drops.forEach(s -> Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, s));
            offerings.replaceAll(s -> ItemStack.EMPTY);
        }
    }

    private void message(Player player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- persistence & sync ---------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, offerings, true, registries);
        if (activeRitual != null) {
            tag.putString("Ritual", activeRitual.toString());
            tag.putInt("Progress", progress);
            tag.putInt("Duration", duration);
            if (ritualist != null) tag.putUUID("Ritualist", ritualist);
            if (!reserved.isEmpty()) tag.put("Reserved", reserved.save(registries));
            tag.putFloat("Paid", paid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        offerings.replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, offerings, registries);
        activeRitual = tag.contains("Ritual") ? ResourceLocation.tryParse(tag.getString("Ritual")) : null;
        progress = tag.getInt("Progress");
        duration = tag.getInt("Duration");
        ritualist = tag.hasUUID("Ritualist") ? tag.getUUID("Ritualist") : null;
        paid = tag.getFloat("Paid");
        reserved = tag.contains("Reserved") ? ItemStack.parseOptional(registries, tag.getCompound("Reserved")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
