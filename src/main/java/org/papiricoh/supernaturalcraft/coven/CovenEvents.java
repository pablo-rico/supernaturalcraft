package org.papiricoh.supernaturalcraft.coven;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodVialItem;
import org.papiricoh.supernaturalcraft.coven.church.Sanctuaries;
import org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HedgeWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.hex.HexHooks;

/**
 * The coven's server wiring (v0.21): Break Hex and salt free hexed villagers and shatter coven witches' shields
 * ({@link HexHooks}); Rowena joins the shared dialogue; once a second the sanctuaries drive out demons and spirits and Rowena's
 * house and host are looked after; witches bleed into vials and coven witches carry Rowena's calling card.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CovenEvents {

    /** Chance a witch killed by a player leaves a vial of her blood (Hedge, Coven). */
    public static final float HEDGE_BLOOD = 0.1f, COVEN_BLOOD = 0.25f;
    /** Chance a Coven Witch killed by a player carries Rowena's calling card. */
    public static final float CALLING_CARD = 0.35f;

    private CovenEvents() {
    }

    /** At mod construction. */
    public static void bootstrap() {
        RowenaServerHandlers.register();
        org.papiricoh.supernaturalcraft.coven.ending.GrandWitchServerHandlers.register();
        HexHooks.onBreakHex(CovenEvents::unhex);
        HexHooks.onSalt(CovenEvents::unhex);
    }

    /** What breaking a hex (by spell or salt) undoes among the coven's: a victim's hex, a coven witch's shield. */
    static boolean unhex(LivingEntity e, LivingEntity by) {
        if (e instanceof HexedVillagerEntity v) return v.free(by);
        if (e instanceof CovenWitchEntity w) return w.breakShield();
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % Sanctuaries.TICK_EVERY != 7) return;
        Sanctuaries.tick(level);
        if (level.dimension() == Level.OVERWORLD) RowenaWorld.tick(level, level.players());
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player)) return;
        float blood = dead instanceof CovenWitchEntity ? COVEN_BLOOD : dead instanceof HedgeWitchEntity ? HEDGE_BLOOD : 0;
        if (blood > 0 && dead.getRandom().nextFloat() < blood) {
            event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(),
                    BloodVialItem.of(dead.getUUID(), dead.getName().getString())));
        }
        if (dead instanceof CovenWitchEntity && dead.getRandom().nextFloat() < CALLING_CARD) {
            event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(), CallingCardItem.pointed(level.getServer().overworld())));
        }
    }
}
