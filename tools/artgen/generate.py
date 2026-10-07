#!/usr/bin/env python3
"""Regenerates every texture (and, later, the GeckoLib models) the mod ships.

Pure stdlib; deterministic, so re-running produces byte-identical files.
Usage:  python3 tools/artgen/generate.py
"""

import blocks
import demon_art
import fx
import gui
import lucifer_art
import amara_art
import chorus_art
import colt_art
import player_anims
import spire_art
import items
import particles
import weapons_art
import weapon_models
from common import WRITTEN

MODULES = [items, weapons_art, weapon_models, blocks, particles, gui, fx, demon_art, lucifer_art, amara_art, chorus_art, spire_art, colt_art, player_anims]

if __name__ == "__main__":
    for m in MODULES:
        m.generate()
    print(f"wrote {len(WRITTEN)} files")
