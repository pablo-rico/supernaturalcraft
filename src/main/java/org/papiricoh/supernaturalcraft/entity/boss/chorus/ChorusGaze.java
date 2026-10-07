package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The judgment of an eye: does it see you, and are you looking into it? */
public final class ChorusGaze {

    private ChorusGaze() {
    }

    public static ChorusBalance.Gaze verdict(ChorusEntity boss, Vec3 eye, LivingEntity e) {
        boolean los = ChorusLight.sees(boss, eye, e);
        Vec3 toEye = eye.subtract(e.getEyePosition()).normalize();
        return ChorusBalance.gaze(los, e.getViewVector(1.0f).dot(toEye));
    }
}
