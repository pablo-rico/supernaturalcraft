package org.papiricoh.supernaturalcraft.balance;

import com.mojang.serialization.Codec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The great enemies whose first defeat has already given a hunter its hearts of max health (v0.15). Immutable;
 * kept on the player (attachment {@code VITALITY}, survives death) by boss id ({@code BossProgression.Boss.id()}).
 */
public record Vitality(Set<String> granted) {

    public static final Vitality NONE = new Vitality(Set.of());
    public static final Codec<Vitality> CODEC = Codec.STRING.listOf().xmap(l -> new Vitality(Set.copyOf(l)), v -> List.copyOf(v.granted));

    public Vitality {
        granted = Set.copyOf(granted);
    }

    public boolean has(String bossId) {
        return granted.contains(bossId);
    }

    public Vitality with(String bossId) {
        Set<String> copy = new HashSet<>(granted);
        copy.add(bossId);
        return new Vitality(copy);
    }
}
