package org.papiricoh.supernaturalcraft.compat.pal.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.layered.modifier.MirrorIfLeftHandModifier;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.colt.ColtPlayerAnims;

/**
 * PlayerAnimationLib (required since v0.20): one animation layer for the Colt on every player
 * (reloading, inspecting, the dry click), mirrored for the left-handed, never drawn in first
 * person (the Colt draws its own hands there); under it, the flight layer ({@link PalFlight}),
 * which the Colt's arms override while it plays.
 */
public final class PalClient {

    public static final String MOD_ID = "player_animation_library";
    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("colt");
    public static final ResourceLocation FLIGHT = SupernaturalCraft.asResource("flight");

    private PalClient() {
    }

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1500, player -> {
            PlayerAnimationController controller = new PlayerAnimationController(player, (c, data, setter) -> PlayState.STOP);
            controller.addModifierLast(new MirrorIfLeftHandModifier());
            controller.setFirstPersonMode(FirstPersonMode.DISABLED);
            return controller;
        });
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(FLIGHT, 1000, PalFlight::new);
        ColtPlayerAnims.install(new PalColtAnims());
    }
}
