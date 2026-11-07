package org.papiricoh.supernaturalcraft.client.arena;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** How each kind of arena looks and sounds: dome colour per phase and the fight music. */
public final class ArenaStyles {

    private static final int[][] COLORS = {
            {0xFF3B1F, 0xFF7A2A, 0x8FD8FF, 0xFFF3C4},   // the Cage: hellfire, embers, ice, grace
            {0x6A3FA8, 0x8A2F9A, 0xB48CFF, 0x2A1240},   // the Darkness
            {0xFFE7A0, 0xFFC45A, 0xFF8A3A, 0xFF4A2A},   // the Chorus: gold cracking to red
            {0x8A0E14, 0xFF3B1F, 0x8FD8FF, 0x6A0F2A, 0xFFF3C4, 0xFFFFFF},   // the Abyss: chains, hellfire, cold, legion, star, light
            {0xE8C22E, 0xFF8A1E},   // Azazel: sulphur, then smoke and fire
            {0xD9DCE6, 0xBFD6FF, 0xFFFFFF},   // Lilith: pale, cold, then white
            {0xFFD978, 0xFFC94A, 0xF2E6C4, 0xFFFFFF},   // Metatron: gold, gold, parchment, the Tablet's white
    };

    private ArenaStyles() {
    }

    public static int color(int theme, int phase) {
        if (theme == ArenaTheme.AUTHOR) return org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckArenaStyles.color(phase);
        if (theme == ArenaTheme.HEAVEN) return org.papiricoh.supernaturalcraft.client.michael.MichaelArenaStyles.color(phase);
        if (theme == ArenaTheme.TV_LAND) return org.papiricoh.supernaturalcraft.client.gabriel.GabrielArenaStyles.color(phase);
        if (theme == ArenaTheme.STORM) return org.papiricoh.supernaturalcraft.client.raphael.RaphaelArenaStyles.color(phase);
        if (theme == ArenaTheme.FARM) return org.papiricoh.supernaturalcraft.client.cain.CainArenaStyles.color(phase);
        if (theme == ArenaTheme.KNIGHTS) return org.papiricoh.supernaturalcraft.client.abaddon.AbaddonArenaStyles.color(phase);
        if (theme == ArenaTheme.BOARDROOM) return org.papiricoh.supernaturalcraft.client.leviathan.DickArenaStyles.color(phase);
        if (theme == ArenaTheme.ASYLUM) return org.papiricoh.supernaturalcraft.client.crowley.CrowleyArenaStyles.color(phase);
        if (theme == ArenaTheme.LAB) return org.papiricoh.supernaturalcraft.client.godstiel.GodstielArenaStyles.color(phase);
        if (theme == ArenaTheme.GLADE) return org.papiricoh.supernaturalcraft.client.eve.EveArenaStyles.color(phase); // v0.39 purgatory
        if (theme == ArenaTheme.WAREHOUSE) return phase >= 3 ? 0xFFE8E8E8 : phase == 2 ? 0xFF8A2A2A : 0xFF6A6A72; // v0.40: Alastair
        if (theme == ArenaTheme.GARRISON) return phase >= 2 ? 0xFFF0D890 : 0xFFB8A070; // v0.40 seals: Uriel
        // v0.41: seasons 12-14.
        if (theme == ArenaTheme.BRITISH_HQ) return phase >= 3 ? 0xFF9FD8D0 : phase == 2 ? 0xFF6FA8A0 : 0xFF7A8488; // Hess: cold teal, grey
        if (theme == ArenaTheme.AMBUSH) return 0xFF8A8A8E; // Ketch: smoke grey
        if (theme == ArenaTheme.SAFEHOUSE) return phase >= 3 ? 0xFFFFB040 : 0xFFC0301E; // Dagon: hellfire red, amber
        if (theme == ArenaTheme.FORTRESS) return phase >= 3 ? 0xFFF0D890 : phase == 2 ? 0xFFC8A050 : 0xFF6A5040; // AU Michael: gold, smoke-brown
        if (theme == ArenaTheme.PRINCES) return phase >= 2 ? 0xFFFFE38A : 0xFFE8E4D8; // Asmodeus: white suit, then stolen gold
        if (theme == ArenaTheme.VESSEL || theme == ArenaTheme.MINDSCAPE) return phase >= 3 ? 0xFFFFFFFF : 0xFF9FD8FF; // Michael: ice blue
        if (theme == ArenaTheme.NEPHILIM) return phase >= 3 ? 0xFFFFF0A0 : 0xFFFFD24A; // soulless Jack: gold
        if (theme == ArenaTheme.THE_EMPTY) return phase >= 3 ? 0xFF6A3FA0 : 0xFF3A2058; // the Shadow: violet-black
        if (org.papiricoh.supernaturalcraft.client.witchboss.WitchBossArenaStyles.handles(theme)) {
            return org.papiricoh.supernaturalcraft.client.witchboss.WitchBossArenaStyles.color(theme, phase);
        }
        if (org.papiricoh.supernaturalcraft.client.heaven.HeavenArenaStyles.handles(theme)) {
            return org.papiricoh.supernaturalcraft.client.heaven.HeavenArenaStyles.color(theme, phase);
        }
        if (org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.handles(theme)) {
            return org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.color(theme, phase);
        }
        int[] row = COLORS[Mth.clamp(theme, 0, COLORS.length - 1)];
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    /** The music for a theme at a phase: the same for the whole fight, except TV Land's (the channel's) and the Heaven themes'. */
    public static SoundEvent music(int theme, int phase) {
        if (theme == ArenaTheme.TV_LAND) return org.papiricoh.supernaturalcraft.client.gabriel.GabrielArenaStyles.music(phase);
        if (theme == ArenaTheme.FARM) return org.papiricoh.supernaturalcraft.client.cain.CainArenaStyles.music(phase);
        if (org.papiricoh.supernaturalcraft.client.heaven.HeavenArenaStyles.handles(theme)) {
            return org.papiricoh.supernaturalcraft.client.heaven.HeavenArenaStyles.music(theme, phase);
        }
        return music(theme);
    }

    public static SoundEvent music(int theme) {
        return switch (theme) {
            case ArenaTheme.DARKNESS -> AllSounds.MUSIC_AMARA.get();
            case ArenaTheme.CHORUS -> AllSounds.MUSIC_CHORUS.get();
            case ArenaTheme.ABYSS -> AllSounds.MUSIC_UNCAGED.get();
            case ArenaTheme.SULFUR -> AllSounds.MUSIC_AZAZEL.get();
            case ArenaTheme.SEAL -> AllSounds.MUSIC_LILITH.get();
            case ArenaTheme.SCRIPTORIUM -> AllSounds.MUSIC_METATRON.get();
            case ArenaTheme.AUTHOR -> org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckArenaStyles.music();
            case ArenaTheme.HEAVEN -> org.papiricoh.supernaturalcraft.client.michael.MichaelArenaStyles.music();
            case ArenaTheme.STORM -> org.papiricoh.supernaturalcraft.client.raphael.RaphaelArenaStyles.music();
            case ArenaTheme.FARM -> org.papiricoh.supernaturalcraft.client.cain.CainArenaStyles.music();
            case ArenaTheme.KNIGHTS -> org.papiricoh.supernaturalcraft.client.abaddon.AbaddonArenaStyles.music();
            case ArenaTheme.BOARDROOM -> org.papiricoh.supernaturalcraft.client.leviathan.DickArenaStyles.music();
            case ArenaTheme.ASYLUM -> org.papiricoh.supernaturalcraft.client.crowley.CrowleyArenaStyles.music();
            case ArenaTheme.LAB -> org.papiricoh.supernaturalcraft.client.godstiel.GodstielArenaStyles.music();
            case ArenaTheme.GLADE -> org.papiricoh.supernaturalcraft.client.eve.EveArenaStyles.music(); // v0.39 purgatory
            case ArenaTheme.WAREHOUSE -> AllSounds.MUSIC_ALASTAIR.get(); // v0.40
            case ArenaTheme.GARRISON -> AllSounds.MUSIC_URIEL.get(); // v0.40 seals
            // v0.41 (provisional: the existing scores)
            case ArenaTheme.BRITISH_HQ, ArenaTheme.AMBUSH -> org.papiricoh.supernaturalcraft.client.crowley.CrowleyArenaStyles.music();
            case ArenaTheme.SAFEHOUSE, ArenaTheme.PRINCES -> AllSounds.MUSIC_AZAZEL.get();
            case ArenaTheme.FORTRESS, ArenaTheme.VESSEL, ArenaTheme.MINDSCAPE -> org.papiricoh.supernaturalcraft.client.michael.MichaelArenaStyles.music();
            case ArenaTheme.NEPHILIM -> AllSounds.MUSIC_UNCAGED.get();
            case ArenaTheme.THE_EMPTY -> AllSounds.MUSIC_AMARA.get();
            case ArenaTheme.ROWENA_HOUSE, ArenaTheme.COVEN -> org.papiricoh.supernaturalcraft.client.witchboss.WitchBossArenaStyles.music(theme);
            case ArenaTheme.REPROGRAMMING, ArenaTheme.OFFICE, ArenaTheme.MEMORY ->
                    org.papiricoh.supernaturalcraft.client.heaven.HeavenArenaStyles.music(theme, 1);
            case ArenaTheme.WAR, ArenaTheme.FAMINE, ArenaTheme.PLAGUE, ArenaTheme.DEATH ->
                    org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.music(theme);
            default -> AllSounds.MUSIC_LUCIFER.get();
        };
    }
}
