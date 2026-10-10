#!/usr/bin/env python3
"""Regenerates every texture (and, later, the GeckoLib models) the mod ships.

Pure stdlib; deterministic, so re-running produces byte-identical files.
Usage:  python3 tools/artgen/generate.py
"""

import blocks
import book_art
import demon_art
import fx
import gui
import lucifer_art
import amara_art
import chorus_art
import colt_art
import hell_art
import hellhound_art
import uncaged_art
import azazel_art
import lilith_art
import metatron_art
import bowl_art
import hex_art
import grave_art
import ghost_art
import crossroads_art
import effect_icons
import player_anims
import spire_art
import items
import particles
import weapons_art
import weapon_models
import author_items_art
import author_blocks_art
import author_fx_art
import chuck_art
import chuck_divine_art
import author_hand_art
import typewriter_key_art
import allies_art
import ink_echo_art
import steed_art
import war_art
import famine_art
import pestilence_art
import death_art
import reaper_art
import horsemen_items_art
import michael_art
import michael_archangel_art
import host_angel_art
import michael_lance_art
import general_armor_art
import michael_items_art
import michael_gui_art
import allegiance_art
import rival_hunter_art
import gabriel_art
import gabriel_items_art
import gabriel_gui_art
import balance_art
import raphael_art
import raphael_items_art
import legacy_art
import legacy_items_art
import bunker_banner_art
import naomi_art
import zachariah_art
import ash_art
import chair_art
import heaven_items_art
import heaven_blocks_art
import heaven_gui_art
import heaven_sky_art
from common import WRITTEN

MODULES = [items, weapons_art, weapon_models, blocks, particles, gui, fx, demon_art, lucifer_art, amara_art, chorus_art, spire_art, colt_art, player_anims, hell_art, hellhound_art, uncaged_art, azazel_art, lilith_art, metatron_art,
           bowl_art, hex_art, grave_art, ghost_art, crossroads_art, effect_icons, book_art,
           author_items_art, author_blocks_art, author_fx_art, chuck_art, chuck_divine_art, author_hand_art, typewriter_key_art,
           allies_art, ink_echo_art,
           steed_art, war_art, famine_art, pestilence_art, death_art, reaper_art, horsemen_items_art,
           michael_art, michael_archangel_art, host_angel_art, michael_lance_art, general_armor_art,
           michael_items_art, michael_gui_art,
           allegiance_art, rival_hunter_art,
           gabriel_art, gabriel_items_art, gabriel_gui_art,
           balance_art,
           raphael_art, raphael_items_art,
           legacy_art, legacy_items_art, bunker_banner_art,
           naomi_art, zachariah_art, ash_art, chair_art, heaven_items_art, heaven_blocks_art, heaven_gui_art, heaven_sky_art]

if __name__ == "__main__":
    for m in MODULES:
        m.generate()
    print(f"wrote {len(WRITTEN)} files")
