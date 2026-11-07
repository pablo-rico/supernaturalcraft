package org.papiricoh.supernaturalcraft.entity.boss.shadow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.empty.EmptyWorld;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.boss.SeasonBossEntity;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.Map;

/**
 * The Shadow (v0.41), the Empty's keeper: woken when soulless Jack fell into the Empty (and a hunter came in after him). Three
 * phases ({@link ShadowBalance}), a tendril more each. Beaten, it sinks back to sleep: every fighter gets the page of the Empty's
 * own sleep ({@link #SIGIL}) and its trophy, the road reaches the Author ({@code DONE}), and they are sent home.
 */
public class ShadowEntity extends SeasonBossEntity {

    public static final ResourceLocation SIGIL = SupernaturalCraft.asResource("eternal_sleep");

    public ShadowEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected String rig() {
        return ShadowAssets.RIG;
    }

    @Override
    protected List<String> clips() {
        return ShadowAssets.TRIGGERED;
    }

    @Override
    protected int theme() {
        return ArenaTheme.THE_EMPTY;
    }

    @Override
    protected int phases() {
        return ShadowBalance.PHASES;
    }

    @Override
    protected int arenaRadius() {
        return ShadowBalance.ARENA_RADIUS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> attacks(int phase) {
        return ShadowAttacks.pool(phase);
    }

    @Override
    protected int gap(int phase) {
        return ShadowBalance.attackGap(phase);
    }

    @Override
    protected Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds() {
        return NephilimSounds.SHADOW;
    }

    @Override
    protected List<ItemStack> spoils() {
        return List.of(SigilPageItem.of(AllItems.SIGIL_PAGE.get(), SIGIL), new ItemStack(NephilimRegistry.SHADOW_TROPHY_ITEM.get()));
    }

    @Override
    protected void afterDefeat(ServerLevel level, List<ServerPlayer> fighters) {
        EmptyWorld.shadowFell(level.getServer(), fighters);
    }

    @Override
    protected @Nullable String transitionClip(int phase) {
        return "roar";
    }

    @Override
    protected ChatFormatting colour(int phase) {
        return ChatFormatting.DARK_PURPLE;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.SQUID_INK;
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.SQUID_INK, getRandomX(0.8), getY() + random.nextDouble() * 2.5, getRandomZ(0.8), 0, 0.03, 0);
    }
}
