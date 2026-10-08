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
        if (!ManaManager.tryConsume(player, org.papiricoh.supernaturalcraft.allegiance.Allegiances.ritualCost(player, recipe.manaCost()))) {
            message(player, "message.supernaturalcraft.ritual.no_mana", ChatFormatting.RED);
            return;
        }
        if (!player.getAbilities().instabuild) {
            if (recipe.consumeActivator()) {
                held.shrink(1);
            } else if (held.isDamageableItem()) {
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }
        activeRitual = holder.id();
        duration = recipe.duration();
        progress = 0;
        ritualist = player.getUUID();
        server.playSound(null, worldPosition, AllSounds.RITUAL_CHANNEL.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
        changed();
    }

    // --- ticking --------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
        if (!altar.isChanneling()) return;
        ServerLevel server = (ServerLevel) level;
        RecipeHolder<?> holder = server.getRecipeManager().byKey(altar.activeRitual).orElse(null);
        if (holder == null || !(holder.value() instanceof RitualRecipe recipe)) {
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
        if (altar.progress % 40 == 0) {
            server.playSound(null, pos, AllSounds.RITUAL_CHANNEL.get(), SoundSource.BLOCKS, 0.7f, 0.8f + altar.progressFraction() * 0.6f);
        }
        if (altar.progress >= altar.duration) {
            altar.complete(server, recipe);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
        if (!altar.isChanneling()) return;
        float f = altar.progressFraction();
        double a = level.getGameTime() * 0.3;
        double r = 1.6 - f * 1.2;
        level.addParticle(AllParticles.SIGIL.get(), pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 1.0 + f, pos.getZ() + 0.5 + Math.sin(a) * r,
                0, 0.02, 0);
        if (level.random.nextFloat() < 0.3f + f) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.05, 0.05 + f * 0.1, (level.random.nextDouble() - 0.5) * 0.05);
        }
    }

    private void complete(ServerLevel server, RitualRecipe recipe) {
        ServerPlayer player = ritualist == null ? null : (ServerPlayer) server.getPlayerByUUID(ritualist);
        boolean done = recipe.effect().perform(server, worldPosition, player);
        if (done) {
            for (int i = 0; i < offerings.size(); i++) {
                ItemStack remainder = offerings.get(i).hasCraftingRemainingItem() ? offerings.get(i).getCraftingRemainingItem() : ItemStack.EMPTY;
                offerings.set(i, remainder);
            }
            server.playSound(null, worldPosition, AllSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.2f, 1.0f);
            server.sendParticles(AllParticles.GRACE.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.08);
        } else if (player != null) {
            message(player, "message.supernaturalcraft.ritual.unanswered", ChatFormatting.GRAY);
        }
        reset();
    }

    /** The circle broke mid-ritual: thunder, a burn, and something answers that shouldn't have. */
    private void backlash(ServerLevel server) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        server.playSound(null, worldPosition, AllSounds.RITUAL_BACKLASH.get(), SoundSource.BLOCKS, 1.5f, 0.8f);
        if (ritualist != null && server.getPlayerByUUID(ritualist) instanceof ServerPlayer player) {
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
        changed();
    }

    void dropOfferings() {
        if (level != null) {
            List<ItemStack> drops = new ArrayList<>();
            offerings.forEach(s -> {
                if (!s.isEmpty()) drops.add(s);
            });
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
