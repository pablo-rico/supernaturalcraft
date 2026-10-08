package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.OpenRecitationPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.Optional;
import java.util.UUID;

/**
 * A spell bowl's contents and, while it burns, its casting session.
 *
 * <p>Filling: a liquid pours in a dose (the bottle comes back), a glass bottle scoops the last dose
 * out, anything else goes in as one ingredient, an empty hand takes the last ingredient back.
 *
 * <p>Casting: flint and steel or a fire charge {@link #tryLight lights} the bowl. A mix no recipe
 * answers to backfires at once; a spell the lighter has not learned does nothing at all. Otherwise
 * the conditions, the effect's own precheck and the mana cost are checked, the bowl catches, and
 * the caster gets the incantation to type ({@link OpenRecitationPayload}). The client reports back
 * and the server {@link #resolveRecitation re-validates}: the right player, close enough, in time,
 * and a plausible time for the typos claimed. Success performs the spell and consumes the bowl;
 * anything else (and the deadline passing, checked every tick) is a {@link BowlBacklash}.
 */
public class SpellBowlBlockEntity extends BlockEntity {

    /** Ticks of slack on the deadline, for the round trip to the client. */
    public static final int GRACE_TICKS = 40;
    /** How far the caster may stand from the bowl while reciting. */
    public static final double MAX_DISTANCE = 6;

    public enum LightResult {
        /** Lit: the recitation is under way. */
        LIT,
        /** No recipe answers to this mix: it blew up. */
        BACKLASH,
        /** Nothing in the bowl to light. */
        EMPTY,
        /** The caster does not know the spell: nothing happened. */
        UNKNOWN,
        /** Conditions, precheck or mana said no: nothing consumed. */
        BLOCKED
    }

    public enum Outcome {
        /** No session for that player: ignored. */
        NONE,
        /** The spell worked and consumed the bowl. */
        CAST,
        /** The spell found nothing to act on: mana spent, contents kept. */
        FIZZLED,
        /** The recitation failed or could not be trusted. */
        BACKLASH
    }

    /** One lit bowl's recitation. The caster is held by reference so fake players work in tests. */
    private static final class Session {
        final UUID caster;
        @Nullable
        ServerPlayer ref;
        final ResourceLocation recipe;
        long litAt;
        final int allowed, penalty, letters;

        Session(ServerPlayer caster, ResourceLocation recipe, long litAt, int allowed, int penalty, int letters) {
            this.caster = caster.getUUID();
            this.ref = caster;
            this.recipe = recipe;
            this.litAt = litAt;
            this.allowed = allowed;
            this.penalty = penalty;
            this.letters = letters;
        }
    }

    private BowlContents contents = BowlContents.EMPTY;
    @Nullable
    private Session session;
    /** The lit spell's smoke colour (0xRRGGBB), synced for the client's particles; -1 when unlit. */
    private int smokeColor = -1;

    public SpellBowlBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.SPELL_BOWL.get(), pos, state);
    }

    public BowlContents contents() {
        return contents;
    }

    public void setContents(BowlContents contents) {
        this.contents = contents;
        changed();
    }

    public boolean lit() {
        return getBlockState().hasProperty(SpellBowlBlock.LIT) && getBlockState().getValue(SpellBowlBlock.LIT);
    }

    /** Whether a recitation is under way (server side). */
    public boolean casting() {
        return session != null;
    }

    @Nullable
    public UUID casterId() {
        return session == null ? null : session.caster;
    }

    /** Ticks the current recitation allows, or 0. */
    public int allowedTicks() {
        return session == null ? 0 : session.allowed;
    }

    public int smokeColor() {
        return smokeColor;
    }

    /** Moves the current session's start {@code ticks} into the past (tests, commands). */
    public void ageSession(int ticks) {
        if (session != null) session.litAt -= ticks;
    }

    // --- filling ------------------------------------------------------------------------------

    /** Right-click with {@code held}. Mutates only on the server; the client gets the same answer. */
    public ItemInteractionResult onUse(ItemStack held, Player player, InteractionHand hand) {
        boolean client = level == null || level.isClientSide;
        if (lit()) {
            if (!client) message(player, "message.supernaturalcraft.bowl.busy", ChatFormatting.GOLD);
            return ItemInteractionResult.sidedSuccess(client);
        }
        if (held.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.getItem() instanceof SpellBowlItem || held.is(AllTags.Items.BOWL_REJECTS)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        Dose dose = BowlLiquids.fromStack(held);
        if (dose != null) {
            if (!contents.canAddDose()) {
                if (!client) message(player, "message.supernaturalcraft.bowl.full_liquid", ChatFormatting.GRAY);
                return ItemInteractionResult.sidedSuccess(client);
            }
            if (!client) {
                setContents(contents.withDose(dose));
                exchange(player, hand, held, BowlLiquids.remainder(dose));
                level.playSound(null, worldPosition, AllSounds.BOWL_POUR.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
            }
            return ItemInteractionResult.sidedSuccess(client);
        }
        if (held.is(Items.GLASS_BOTTLE)) {
            Dose last = contents.lastDose();
            if (last == null || !last.kind().bottled) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            if (!client) {
                setContents(contents.withoutLastDose());
                exchange(player, hand, held, BowlLiquids.bottle(last));
                level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(client);
        }
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            if (!client && player instanceof ServerPlayer sp) tryLight(sp, held, hand);
            return ItemInteractionResult.sidedSuccess(client);
        }
        if (!contents.canAddItem()) {
            if (!client) message(player, "message.supernaturalcraft.bowl.full_items", ChatFormatting.GRAY);
            return ItemInteractionResult.sidedSuccess(client);
        }
        if (!client) {
            setContents(contents.withItem(held));
            if (!player.hasInfiniteMaterials()) held.shrink(1);
            level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        return ItemInteractionResult.sidedSuccess(client);
    }

    /** Hands back the last ingredient put in. @return false if there was none */
    public boolean takeLast(Player player) {
        ItemStack last = contents.lastItem();
        if (last.isEmpty()) return false;
        setContents(contents.withoutLastItem());
        if (!player.getInventory().add(last)) player.drop(last, false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8f, 1.0f);
        return true;
    }

    /** Lifts the bowl, contents and all, into the main hand and removes the block. */
    public void pickUp(Player player) {
        if (level == null) return;
        ItemStack stack = toItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 1.1f);
        session = null;
        level.removeBlock(worldPosition, false);
    }

    /** The bowl as an item, carrying what it holds. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(AllItems.SPELL_BOWL.get());
        stack.applyComponents(collectComponents());
        return stack;
    }

    /** {@code held} spent (unless creative) for {@code result}, which goes to the hand or the inventory. */
    private static void exchange(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (result.isEmpty()) {
            if (!player.hasInfiniteMaterials()) held.shrink(1);
            return;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
    }

    // --- casting ------------------------------------------------------------------------------

    /** Sets the bowl alight with {@code igniter} (flint and steel or a fire charge) in {@code hand}. */
    public LightResult tryLight(ServerPlayer player, ItemStack igniter, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl) || lit()) return LightResult.BLOCKED;
        if (contents.isEmpty()) {
            message(player, "message.supernaturalcraft.bowl.empty", ChatFormatting.GRAY);
            return LightResult.EMPTY;
        }
        Optional<RecipeHolder<BowlSpellRecipe>> found = sl.getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(contents), sl);
        if (found.isEmpty()) {
            spendIgniter(player, igniter, hand);
            BowlBacklash.trigger(this, player);
            return LightResult.BACKLASH;
        }
        RecipeHolder<BowlSpellRecipe> holder = found.get();
        BowlSpellRecipe recipe = holder.value();
        ResourceLocation spell = recipe.spell(holder.id());
        if (!ManaManager.get(player).knowsRite(spell)) {
            message(player, "message.supernaturalcraft.bowl.unknown_words", ChatFormatting.GRAY);
            return LightResult.UNKNOWN;
        }
        String why = recipe.conditions().check(sl, player);
        if (why == null) why = recipe.effect().precheck(new BowlCast(sl, worldPosition, player, contents, recipe));
        if (why != null) {
            message(player, why, ChatFormatting.RED);
            return LightResult.BLOCKED;
        }
        if (!ManaManager.tryConsume(player, org.papiricoh.supernaturalcraft.allegiance.Allegiances.ritualCost(player, recipe.manaCost()))) {
            message(player, "message.supernaturalcraft.cast.no_mana", ChatFormatting.RED);
            return LightResult.BLOCKED;
        }
        spendIgniter(player, igniter, hand);
        int allowed = Math.round(Recitation.timeFor(recipe.incantation(), recipe.difficulty())
                * org.papiricoh.supernaturalcraft.allegiance.Allegiances.recitationScale(player));
        session = new Session(player, holder.id(), sl.getGameTime(), allowed, Recitation.PENALTY_TICKS, Recitation.letters(recipe.incantation()));
        smokeColor = recipe.smokeColor() & 0xFFFFFF;
        sl.setBlock(worldPosition, getBlockState().setValue(SpellBowlBlock.LIT, true), 3);
        changed();
        sl.playSound(null, worldPosition, AllSounds.BOWL_LIGHT.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new OpenRecitationPayload(worldPosition, spell, recipe.incantation(), allowed,
                    Recitation.PENALTY_TICKS, smokeColor));
        }
        return LightResult.LIT;
    }

    private void spendIgniter(ServerPlayer player, ItemStack igniter, InteractionHand hand) {
        if (igniter.is(Items.FIRE_CHARGE)) {
            if (!player.hasInfiniteMaterials()) igniter.shrink(1);
        } else if (igniter.isDamageableItem()) {
            igniter.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
    }

    /**
     * The caster's report on the recitation (from {@link org.papiricoh.supernaturalcraft.network.RecitationResultPayload},
     * or directly from tests). Re-validated: anything doubtful is a backlash.
     */
    public Outcome resolveRecitation(ServerPlayer player, boolean success, int typos) {
        if (session == null || !session.caster.equals(player.getUUID()) || !(level instanceof ServerLevel sl)) return Outcome.NONE;
        Session s = session;
        s.ref = player;
        long elapsed = sl.getGameTime() - s.litAt;
        boolean trusted = success && near(player)
                && Recitation.plausible(s.letters, s.allowed, s.penalty, GRACE_TICKS, elapsed, typos);
        if (!trusted) {
            BowlBacklash.trigger(this, player);
            return Outcome.BACKLASH;
        }
        Optional<RecipeHolder<?>> holder = sl.getRecipeManager().byKey(s.recipe);
        if (holder.isEmpty() || !(holder.get().value() instanceof BowlSpellRecipe recipe)) {
            extinguish(contents);
            return Outcome.NONE;
        }
        BowlCast cast = new BowlCast(sl, worldPosition, player, contents, recipe);
        if (!recipe.effect().perform(cast)) {
            message(player, "message.supernaturalcraft.bowl.fizzled", ChatFormatting.GRAY);
            sl.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.2f);
            extinguish(contents);
            return Outcome.FIZZLED;
        }
        for (ItemStack back : cast.returned()) {
            if (!player.getInventory().add(back)) player.drop(back, false);
        }
        smokeColumn(sl, recipe.smokeColor());
        sl.playSound(null, worldPosition, AllSounds.BOWL_CAST.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        ChorusRewards.award(player, "main/first_spell");
        extinguish(BowlContents.EMPTY);
        return Outcome.CAST;
    }

    private void smokeColumn(ServerLevel sl, int color) {
        ColorParticleOption smoke = ColorParticleOption.create(AllParticles.BOWL_SMOKE.get(), 0xFF000000 | (color & 0xFFFFFF));
        Vec3 c = Vec3.atBottomCenterOf(worldPosition);
        for (int i = 0; i < 10; i++) {
            sl.sendParticles(smoke, c.x, c.y + 0.4 + i * 0.35, c.z, 6, 0.12 + i * 0.03, 0.12, 0.12 + i * 0.03, 0.01);
        }
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.6, c.z, 12, 0.2, 0.6, 0.2, 0.02);
    }

    private boolean near(ServerPlayer player) {
        return player.isAlive() && player.level() == level
                && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Nullable
    private ServerPlayer caster() {
        if (session == null) return null;
        if (session.ref != null && !session.ref.isRemoved()) return session.ref;
        if (level != null && level.getServer() != null) session.ref = level.getServer().getPlayerList().getPlayer(session.caster);
        return session.ref;
    }

    /** Ends any session, puts the fire out and leaves {@code remaining} in the bowl. */
    void extinguish(BowlContents remaining) {
        session = null;
        smokeColor = -1;
        contents = remaining;
        if (level != null && lit()) level.setBlock(worldPosition, getBlockState().setValue(SpellBowlBlock.LIT, false), 3);
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpellBowlBlockEntity be) {
        if (be.session == null) {
            // Lit with no session (a reload mid-recitation): the flame just goes out.
            if (state.getValue(SpellBowlBlock.LIT)) be.extinguish(be.contents);
            return;
        }
        long elapsed = level.getGameTime() - be.session.litAt;
        ServerPlayer caster = be.caster();
        if (elapsed > be.session.allowed + GRACE_TICKS || caster == null || !be.near(caster)) {
            BowlBacklash.trigger(be, caster);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpellBowlBlockEntity be) {
        if (!state.getValue(SpellBowlBlock.LIT)) return;
        RandomSource r = level.random;
        double y = pos.getY() + SpellBowlBlock.liquidHeight(be.contents.liquids().size()) + 0.05;
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4, y,
                    pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
        }
        if (r.nextInt(2) == 0) {
            int color = be.smokeColor < 0 ? 0x808080 : be.smokeColor;
            level.addParticle(ColorParticleOption.create(AllParticles.BOWL_SMOKE.get(), 0xFF000000 | color),
                    pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.3, y + 0.1, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.3,
                    0, 0.03 + r.nextDouble() * 0.02, 0);
        }
    }

    private static void message(Player player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- components, persistence & sync -----------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        contents = input.getOrDefault(AllDataComponents.BOWL_CONTENTS.get(), BowlContents.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!contents.isEmpty()) builder.set(AllDataComponents.BOWL_CONTENTS.get(), contents);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("contents");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!contents.isEmpty()) {
            BowlContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), contents)
                    .resultOrPartial(e -> SupernaturalCraft.LOGGER.warn("Spell bowl contents not saved: {}", e))
                    .ifPresent(t -> tag.put("contents", t));
        }
        if (smokeColor >= 0) tag.putInt("smoke", smokeColor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = tag.contains("contents")
                ? BowlContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("contents"))
                        .resultOrPartial(e -> SupernaturalCraft.LOGGER.warn("Spell bowl contents not loaded: {}", e)).orElse(BowlContents.EMPTY)
                : BowlContents.EMPTY;
        smokeColor = tag.contains("smoke") ? tag.getInt("smoke") : -1;
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
