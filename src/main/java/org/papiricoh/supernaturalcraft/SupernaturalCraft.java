package org.papiricoh.supernaturalcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.command.SNCommands;
import org.papiricoh.supernaturalcraft.hunter.AmuletEvents;
import org.papiricoh.supernaturalcraft.hunter.CombatEvents;
import org.papiricoh.supernaturalcraft.magic.SigilPageDrops;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehaviors;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;
import org.papiricoh.supernaturalcraft.registry.*;
import org.slf4j.Logger;

/**
 * SupernaturalCraft — demons, Enochian sigil magic, ritual circles and the Cage.
 *
 * <p>Registries follow the {@code All*} convention: static holder fields plus a no-op
 * {@code init()} that forces class loading. Blocks register before items because block items
 * dereference the block holders.
 */
@Mod(SupernaturalCraft.MODID)
public class SupernaturalCraft {

    public static final String MODID = "supernaturalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public SupernaturalCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SNRegistries::registerDatapackRegistries);
        SpellBehaviors.init();

        AllDataComponents.init();
        AllDataComponents.DATA_COMPONENTS.register(modEventBus);

        AllAttachments.init();
        AllAttachments.ATTACHMENT_TYPES.register(modEventBus);

        AllMobEffects.init();
        AllMobEffects.MOB_EFFECTS.register(modEventBus);

        AllSounds.init();
        AllSounds.SOUND_EVENTS.register(modEventBus);

        AllParticles.init();
        AllParticles.PARTICLE_TYPES.register(modEventBus);

        // Blocks before items: block items dereference the block holders.
        AllBlocks.init();
        AllBlocks.BLOCKS.register(modEventBus);

        AllArmorMaterials.init();
        AllArmorMaterials.ARMOR_MATERIALS.register(modEventBus);

        AllItems.init();
        AllItems.ITEMS.register(modEventBus);

        AllBlockEntities.init();
        AllBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        RitualEffect.bootstrap();
        org.papiricoh.supernaturalcraft.allegiance.AllegianceEffect.bootstrap();
        org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect.bootstrap();
        AllRecipes.init();
        AllRecipes.RECIPE_TYPES.register(modEventBus);
        AllRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        AllRecipes.INGREDIENT_TYPES.register(modEventBus);

        AllLootFunctions.init();
        AllLootFunctions.LOOT_FUNCTIONS.register(modEventBus);
        AllLootFunctions.LOOT_MODIFIERS.register(modEventBus);

        AllMenus.init();
        AllMenus.MENUS.register(modEventBus);
        modEventBus.addListener(org.papiricoh.supernaturalcraft.weapon.WeaponProfiles::register);

        AllEntities.init();
        AllEntities.ENTITY_TYPES.register(modEventBus);

        AllMapDecorations.init();
        AllMapDecorations.TYPES.register(modEventBus);

        AllWorldgen.init();
        AllWorldgen.DENSITY_FUNCTIONS.register(modEventBus);
        AllWorldgen.BIOME_SOURCES.register(modEventBus);
        AllWorldgen.PLACEMENTS.register(modEventBus);
        AllWorldgen.FEATURES.register(modEventBus);

        AllStructures.init();
        AllStructures.TYPES.register(modEventBus);
        AllStructures.PIECES.register(modEventBus);

        AllCreativeTabs.init();
        AllCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ManaManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ServerScheduler::onServerTick);
        NeoForge.EVENT_BUS.addListener(SNNetworking::onLogin);
        NeoForge.EVENT_BUS.addListener(SNNetworking::onRespawn);
        NeoForge.EVENT_BUS.addListener(SNNetworking::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(ArenaEvents::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ArenaEvents::onEnderPearl);
        NeoForge.EVENT_BUS.addListener(ArenaEvents::onChorusFruit);
        NeoForge.EVENT_BUS.addListener(ArenaEvents::onServerStopping);
        NeoForge.EVENT_BUS.addListener(SNCommands::register);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onIncomingDamage);
        // v0.15: the last word on every blow to a boss, after every bonus and every other mod.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                org.papiricoh.supernaturalcraft.entity.boss.BossDamage::onFinalDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.ascension.AscensionEvents::onAttributes);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.balance.DefenceEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.balance.DefenceEvents::onDamagePre);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.balance.DefenceEvents::onAdvancement);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.WeaponEvents::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.WeaponEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.WeaponEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.curse.CurseEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.curse.CurseEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.RuneEvents::onAttributes);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.RuneEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(SigilPageDrops::onDrops);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.reward.EclipseSightItem::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.cinematic.CinematicLocks::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onLevelTick);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onPotentialSpawns);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onEntityTickPre);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(org.papiricoh.supernaturalcraft.eclipse.EclipseEvents::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(AmuletEvents::onPlayerTick);

        modContainer.registerConfig(ModConfig.Type.SERVER, SNConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, SNClientConfig.SPEC);
    }
}
