"""v0.18 items: Naomi's Drill and Zachariah's Blade (GeckoLib + icons), the diadem, the seal, the form, the stamp, the crossroads
box, the busts. 3D art work for the GeckoLib parts (`_geo_items`), 2D art work for the flat sprites (`_sprites`); each side keeps
to its own functions.

Flat sprites (16 px, textures/item/<id>.png):
  naomis_diadem    a slim white-gold circlet seen from above the front, a pale cyan gem in a pointed setting.
  heavens_seal     a gold medallion with a ridged rim and Heaven's star raised on it, two white ribbon tails.
  heavenly_form    a sheet of Heaven's stationery: gold letterhead and seal, typed lines, a signature box, a dog-ear.
  approval_stamp   a rubber stamp: a turned wooden knob and neck, a brass collar, the block and its red rubber face.
  crossroads_box   an old tin tobacco box, dented and rusting, a crossed pair of lines scratched into its lid.
"""

import math

from common import save
from pixelkit import Ramp, Tex, hexc, item_outline, mix

O = hexc("#14100f")
CLEAR = (0, 0, 0, 0)
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
WGOLD = Ramp("#5e5a50", "#8d887b", "#b8b2a2", "#d8d3c4", "#eeeadf", "#ffffff")
CYAN = Ramp("#0b3540", "#11535f", "#1a8a96", "#4fd0dc", "#a6f2f8", "#ffffff")
PAPER = Ramp("#8f897c", "#b4ad9e", "#d2ccbd", "#e6e1d4", "#f3f0e7", "#fffdf8")
WOOD = Ramp("#2a1a0e", "#432b17", "#5c3d22", "#76512e", "#8f673d", "#a97e4f")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
RUBBER = Ramp("#3a0a0a", "#5e1010", "#851a18", "#a82822", "#c8402f", "#e46a4c")
TIN = Ramp("#25231f", "#3d3a33", "#57534a", "#726d61", "#8f897b", "#aea797")
RUST = Ramp("#3d1a0a", "#5e2a12", "#7a3d1c", "#9c5426", "#b86d36", "#d08a4c")


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


# =====================================================================================================
# GeckoLib items (3D art work): Naomi's Drill, Zachariah's Blade, the busts
# =====================================================================================================
#
# Naomi's Drill: her surgical bone drill made a weapon, upright and centred like weapon_models: a battery pack at the
# base, a white rubber-gripped handle with a steel trigger, the motor housing (vents, a cyan light ring, Heaven's gilt
# sigil on its side), a tapered chrome nose, the chuck and a long fluted bit pointing up. Bones `drill` -- `bit` (the
# chuck and bit, which spin). Clips: idle (loop), spin.
#
# Zachariah's Blade: an angel blade made grand: a three-sided silver spike (three plates round its axis, tapering in
# segments to the point) engraved with Enochian down every face (`engraving`: a shell just over the faces carrying the
# script, full of faint gold light in the glowmask, so the `file` clip can flare it), a filigree silver guard with curled
# quillons and a burnt-gold inlay, a grip wrapped in pale leather, an ornate pommel. Clips: idle (loop), file.
#
# The busts: block models on the trophies' pedestal (models/block/<id>.json + textures/block/<id>_<part>.png).

import math as _m

import blockmodel as _BM
from animkit import AnimFile as _AnimFile
from chuck_art import Model as _Model, h01 as _h01, solid as _solid
from common import ASSETS as _ASSETS, art as _art, save as _save
from michael_art import tone as _tone
from pixelkit import Ramp as _Ramp, Tex as _Tex, fbm as _fbm, hexc as _hexc, mix as _mix, shade as _shade

_GEO_ITEM = _ASSETS + "/geo/item/"
_ANIM_ITEM = _ASSETS + "/animations/item/"
_G_WHITE = _Ramp("#6c737b", "#939aa2", "#b7bdc4", "#d3d8dd", "#e8ebee", "#f8f9fa")
_G_CHROME = _Ramp("#22262b", "#454c55", "#727b86", "#a3acb7", "#cfd6de", "#f3f6f9")
_G_RUBBER = _Ramp("#0c0d10", "#15171b", "#1f2227", "#2a2e34", "#363b42", "#454b53")
_G_CYAN = _Ramp("#1f6f86", "#2f95b0", "#4fbfd8", "#86def0", "#bff1fa", "#effdff")
_G_GILT = _Ramp("#5a3a0c", "#8a5c14", "#b8851f", "#ddaf38", "#f3d36a", "#fff1b8")
_G_SILVER = _Ramp("#3a414b", "#5e6773", "#8a94a0", "#b6bec8", "#dce1e7", "#fbfcfe")
_G_BURNT = _Ramp("#3a2208", "#5e3a10", "#86561a", "#ad7527", "#cf9a3e", "#ecc46a")
_G_HIDE = _Ramp("#5c4f3f", "#7e6e59", "#a08e75", "#bdac90", "#d4c6ab", "#e8dfca")
_G_INK = _hexc("#14100f")


def _full(f, x, y, w, h, c):
    return (c[0], c[1], c[2], 255)


def _octo(m, bone, c, d, h, mat, glow=None, density=4, tag="cyl"):
    """An octagonal column along Y: a square and the same square turned 45 deg."""
    cx, cy, cz = c
    s = d * 0.86
    m.cube(bone, (cx - d / 2, cy, cz - d / 2), (d, h, d), mat, glow, density=density, tag=tag)
    m.cube(bone, (cx - s / 2, cy, cz - s / 2), (s, h, s), mat, glow, density=density, tag=tag, rotation=(0, 45, 0), pivot=(cx, cy, cz))


def _centre(m):
    """Centre the model on the origin (GeoItemRenderer lifts it to the block's centre); returns its height."""
    lo, hi = m.rig.height_span()
    dy = -(lo + hi) / 2
    m.rig.translate(0, dy, 0)
    for _, c in m.rig.cubes():
        if c.pivot:
            c.pivot = (c.pivot[0], c.pivot[1] + dy, c.pivot[2])
    return hi - lo


def _chrome(f, x, y, w, h):
    if f == "up":
        return _G_CHROME[5]
    if f == "down":
        return _G_CHROME[1]
    t = x / max(1, w - 1)
    v = 2.6 + 2.2 * _m.exp(-((t - 0.3) / 0.14) ** 2) - 1.3 * _m.exp(-((t - 0.72) / 0.1) ** 2)
    return _tone(_G_CHROME, v - (0.3 if f in ("east", "west") else 0))


def _white(base=3.7):
    def mat(f, x, y, w, h):
        if f == "up":
            return _tone(_G_WHITE, base + 0.9)
        if f == "down":
            return _tone(_G_WHITE, base - 1.6)
        t = x / max(1, w - 1)
        v = base + 0.5 * _m.exp(-((t - 0.32) / 0.2) ** 2) - 0.5 * t - (0.3 if f in ("east", "west") else 0)
        return _tone(_G_WHITE, v)
    return mat


# --- Naomi's Drill ---------------------------------------------------------------------------------------------------

def drill_rig():
    m = _Model("naomis_drill", 256, 256)
    root = m.bone("drill", (0, 0, 0))
    # The battery pack: a squared pale block, a darker clip band, a cyan charge gauge on its front.
    def battery(f, x, y, w, h):
        if f == "north" and y == h // 2 and 1 <= x <= w - 2:
            return _G_CYAN[4] if x < w * 0.7 else _G_RUBBER[2]
        if f in ("north", "south", "east", "west") and y == 1:
            return _G_RUBBER[3]
        return _white(3.2)(f, x, y, w, h)
    m.cube(root, (-2.2, 0.0, -2.8), (4.4, 2.6, 5.6), battery, lambda f, x, y, w, h, c: _full(f, x, y, w, h, c) if c == _G_CYAN[4] else None,
           density=4, tag="battery")
    m.cube(root, (-1.9, -0.3, -2.5), (3.8, 0.3, 5.0), _solid(_G_RUBBER[2]), None, density=4, tag="battery_foot")
    # The grip: rubber-panelled white handle, finger grooves on the front.
    def grip(f, x, y, w, h):
        if f in ("east", "west") and 1 <= x <= w - 2 and 1 <= y <= h - 2:
            return _G_RUBBER[3] if (y % 3) else _G_RUBBER[1]
        if f == "north" and y % 4 == 0 and y > 0:
            return _G_WHITE[1]
        return _white(3.5)(f, x, y, w, h)
    m.cube(root, (-1.3, 2.6, -1.6), (2.6, 6.6, 3.2), grip, None, density=4, tag="grip")
    # The trigger: a steel lever on the front of the grip's top, its guard below it.
    m.cube(root, (-0.45, 7.0, -2.5), (0.9, 1.8, 0.9), _chrome, None, density=4, tag="trigger")
    m.cube(root, (-0.35, 6.2, -2.7), (0.7, 0.4, 1.3), _solid(_G_WHITE[2]), None, density=4, tag="trigger_guard")
    # The motor housing: a white octagonal body, vents round its waist, a cyan light ring at its top, the sigil.
    def housing(f, x, y, w, h):
        if 4 <= y <= 9 and x % 3 == 1 and f in ("north", "south", "east", "west"):
            return _G_WHITE[1]
        return _white(3.8)(f, x, y, w, h)
    _octo(m, root, (0, 9.2, 0), 4.0, 6.0, housing, None, tag="housing")
    m.cube(root, (-2.05, 9.0, -2.05), (4.1, 0.5, 4.1), _solid(_G_CHROME[3]), None, density=4, tag="housing_band")
    for rot in (0, 45):
        m.cube(root, (-1.95, 14.6, -1.95), (3.9, 0.55, 3.9), lambda f, x, y, w, h: _G_CYAN[4] if (x + y) % 3 else _G_CYAN[3], _full,
               density=4, tag="light_ring", rotation=(0, rot, 0), pivot=(0, 14.6, 0))

    def sigil(f, x, y, w, h):
        c = (w - 1) / 2
        r = _m.hypot(x - c, y - c)
        if abs(r - (c - 0.4)) < 0.6:
            return _G_GILT[4]
        if (abs(x - c) < 0.6 or abs(y - c) < 0.6) and r < c - 1:
            return _G_GILT[5]
        return None
    m.cube(root, (2.04, 10.6, -1.1), (0, 2.2, 2.2), sigil, lambda f, x, y, w, h, c: _shade(c, 0.6), faces=("east",), density=8,
           tag="sigil")
    m.cube(root, (-2.04, 10.6, -1.1), (0, 2.2, 2.2), sigil, lambda f, x, y, w, h, c: _shade(c, 0.6), faces=("west",), density=8,
           tag="sigil")
    # The nose: chrome, tapering in three steps to the chuck.
    _octo(m, root, (0, 15.15, 0), 3.2, 1.1, _chrome, None, tag="nose")
    _octo(m, root, (0, 16.25, 0), 2.4, 1.1, _chrome, None, tag="nose")
    # The chuck and the bit spin together.
    bit = m.bone("bit", (0, 17.35, 0), "drill")

    def knurl(f, x, y, w, h):
        return _G_CHROME[4] if (x + y) % 2 else _G_CHROME[2]
    _octo(m, bit, (0, 17.35, 0), 1.8, 1.9, knurl, None, tag="chuck")
    _octo(m, bit, (0, 19.25, 0), 1.2, 0.6, _chrome, None, tag="chuck_jaws")

    def flute(f, x, y, w, h):
        k = (y + {"north": 0, "east": 2, "south": 4, "west": 6}.get(f, 0)) % 8
        return _G_CHROME[5] if k < 2 else (_G_CHROME[1] if k in (4, 5) else _G_CHROME[3])
    m.cube(bit, (-0.4, 19.85, -0.4), (0.8, 7.0, 0.8), flute, None, density=8, tag="bit")
    m.cube(bit, (-0.25, 26.85, -0.25), (0.5, 0.9, 0.5), _solid(_G_CHROME[4]), None, density=8, tag="bit_tip")
    m.cube(bit, (-0.1, 27.75, -0.1), (0.2, 0.5, 0.2), _solid(_G_CHROME[5]), None, density=8, tag="bit_point")
    m.height = _centre(m)
    return m


def drill_anims():
    f = _AnimFile()
    a = f.new("animation.naomis_drill.idle", 3.0, loop=True)
    a.rot("bit", (0, [0, 0, 0]), (3.0, [0, 0, 0]))
    s = f.new("animation.naomis_drill.spin", 0.6)
    s.rot("bit", (0, [0, 0, 0]), (0.1, [0, 360, 0]), (0.45, [0, 2160, 0]), (0.6, [0, 2520, 0], "easeOutQuad"))
    s.pos("drill", (0, [0, 0, 0]), (0.05, [0, 0, 0.25]), (0.1, [0, 0, -0.15]), (0.15, [0, 0, 0.2]), (0.2, [0, 0, -0.1]),
          (0.25, [0, 0, 0.15]), (0.45, [0, 0, 0]))
    return f


def drill_icon():
    """Drawn along the diagonal (handle bottom left, bit top right), profile by profile like a vanilla tool."""
    t = _Tex(16, 16, 23050)
    parts = [  # (from, to, half width, colour fn(q, p))
        (0.0, 1.6, 1.6, lambda q, p: _G_WHITE[2] if q > 0.4 else (_G_CYAN[4] if abs(q) < 0.6 and p > 0.6 else _G_WHITE[3])),
        (1.6, 4.4, 0.9, lambda q, p: _G_RUBBER[3] if q > -0.2 else _G_WHITE[4]),
        (4.4, 7.6, 1.9, lambda q, p: _G_WHITE[2] if q > 1.0 else (_G_WHITE[5] if q < -0.6 else _G_WHITE[4])),
        (7.6, 8.4, 1.7, lambda q, p: _G_CYAN[4] if q < 0.8 else _G_CYAN[2]),
        (8.4, 9.6, 1.0, lambda q, p: _G_CHROME[5] if q < 0 else _G_CHROME[3]),
        (9.6, 10.6, 0.7, lambda q, p: _G_CHROME[4] if int(p * 2) % 2 else _G_CHROME[2]),
        (10.6, 13.6, 0.45, lambda q, p: _G_CHROME[5] if int(p * 2) % 2 else _G_CHROME[3]),
    ]
    for y in range(16):
        for x in range(16):
            p = (x - y + 13) / 2.0
            q = (x + y - 15) / 2.0
            for p0, p1, hw, col in parts:
                if p0 <= p < p1 and abs(q) <= hw:
                    t.set(x, y, col(q, p))
    t.set(4, 9, _G_CHROME[4])                    # the trigger
    t.set(9, 8, _G_GILT[4])                      # the sigil's glint
    from pixelkit import item_outline as _outline
    _outline(t, _G_INK)
    return t


# --- Zachariah's Blade ----------------------------------------------------------------------------------------------

# Enochian-looking glyphs, 3 x 4 texels (rows top to bottom), for the script down the blade.
_GLYPHS = ["111100111100", "010111010010", "110010011110", "101101111001", "111001001111", "011100110011", "100111101011",
           "010110011111"]


def _glyph_on(seed, x, y):
    """The script: a column of glyphs down the middle of each face, a glyph every 5 texels."""
    gx, gy = x, y % 5
    if gy == 4 or not (0 <= gx < 3):
        return False
    g = _GLYPHS[int(_h01("glyph", seed, y // 5) * len(_GLYPHS))]
    return g[gy * 3 + gx] == "1"


def blade_rig():
    m = _Model("zachariahs_blade", 256, 256)
    root = m.bone("blade", (0, 0, 0))
    eng = m.bone("engraving", (0, 8.0, 0), "blade")
    # The pommel: a silver octagonal knob, a burnt-gold cap and a finial.
    _octo(m, root, (0, 0.6, 0), 2.0, 1.4, lambda f, x, y, w, h: _tone(_G_SILVER, 4.2 - 1.6 * y / max(1, h) - (0.4 if x % 3 == 0 else 0)),
          None, tag="pommel")
    _octo(m, root, (0, 0.0, 0), 1.3, 0.6, _solid(_G_BURNT[4]), lambda f, x, y, w, h, c: _shade(c, 0.2), tag="pommel_cap")
    m.cube(root, (-0.3, -0.5, -0.3), (0.6, 0.5, 0.6), _solid(_G_SILVER[5]), None, density=8, tag="finial")
    _octo(m, root, (0, 2.0, 0), 1.4, 0.5, _solid(_G_SILVER[4]), None, tag="ferrule")
    # The grip: pale leather wound in a spiral, a silver wire in its grooves.
    def wrap(f, x, y, w, h):
        off = {"north": 0, "east": 2, "south": 4, "west": 6}.get(f, 0)
        k = (y - x + off) % 5
        if k == 0:
            return _G_SILVER[4]
        return _tone(_G_HIDE, 3.6 - 0.6 * k / 4 - (0.3 if f in ("east", "west") else 0))
    m.cube(root, (-0.75, 2.5, -0.75), (1.5, 5.0, 1.5), wrap, None, density=8, tag="grip")
    # The guard: a silver bar with a central boss inlaid with burnt gold, quillons curling down at their ends.
    def guard(f, x, y, w, h):
        if f == "up":
            return _G_SILVER[5]
        if f == "down":
            return _G_SILVER[1]
        if f in ("north", "south") and y == 1 and x % 3 == 1:
            return _G_SILVER[2]               # the filigree's pierced scrolls
        return _tone(_G_SILVER, 4.2 - 1.4 * y / max(1, h))
    m.cube(root, (-3.6, 7.5, -0.6), (7.2, 0.9, 1.2), guard, None, density=8, tag="guard")
    for s in (-1, 1):
        # Each quillon: a scroll that curls down and back in, ending in a little gold bead.
        m.cube(root, (3.6 * s - 0.45, 7.6, -0.5), (0.9, 1.8, 1.0), guard, None, density=8, tag="quillon",
               rotation=(0, 0, 24 * s), pivot=(3.6 * s, 7.9, 0))
        m.cube(root, (3.0 * s - 0.4, 9.15, -0.4), (0.8, 0.8, 0.8), _solid(_G_BURNT[4]), lambda f, x, y, w, h, c: _shade(c, 0.25),
               density=8, tag="quillon_bead")
        # Filigree loops rising beside the blade's root.
        m.cube(root, (1.4 * s - 0.25, 8.4, -0.25), (0.5, 1.4, 0.5), _solid(_G_SILVER[4]), None, density=8, tag="filigree",
               rotation=(0, 0, -22 * s), pivot=(1.4 * s, 8.4, 0))
    # The boss: a diamond of burnt gold set in the guard's centre on both faces.
    for z, face in ((-0.62, "north"), (0.62, "south")):
        m.cube(root, (-0.9, 7.1, z), (1.8, 1.8, 0), lambda f, x, y, w, h: None if abs(x - (w - 1) / 2) + abs(y - (h - 1) / 2) > w / 2
               else (_G_BURNT[5] if abs(x - (w - 1) / 2) + abs(y - (h - 1) / 2) < w / 5 else _G_BURNT[3]),
               lambda f, x, y, w, h, c: _shade(c, 0.45), faces=(face,), density=8, tag="inlay", rotation=(0, 0, 45), pivot=(0, 8.0, z))
    # The spike: three plates round the axis, tapering in segments to the point (a three-sided blade).
    segs = [(8.4, 4.0, 2.2), (12.4, 4.4, 2.0), (16.8, 4.0, 1.7), (20.8, 3.0, 1.25), (23.8, 1.8, 0.75), (25.6, 1.0, 0.3)]
    for i, (y0, L, a) in enumerate(segs):
        r = a / (2 * _m.sqrt(3))
        for k in range(3):
            ang = 120 * k

            def face(f, x, y, w, h, i=i, k=k):
                if f in ("up", "down", "east", "west"):
                    return _G_SILVER[3]
                if f == "south":
                    return _G_SILVER[1]
                t = x / max(1, w - 1)
                v = 3.0 + 1.9 * _m.exp(-((t - (0.35 + 0.1 * k)) / 0.18) ** 2) - 0.3 * (y / max(1, h))
                if x in (0, w - 1):
                    v = 5.0                          # the keen edges
                return _tone(_G_SILVER, v)
            m.cube(root, (-a / 2, y0, -r - 0.2), (a, L, 0.2), face, lambda f, x, y, w, h, c: _shade(c, 0.12), density=8, tag="spike",
                   rotation=(0, ang, 0), pivot=(0, y0, 0))
            # The script over this face (the engraving shell): only where the glyphs are; faint gold light.
            if a >= 1.2:
                seed = i * 3 + k

                def script(f, x, y, w, h, seed=seed):
                    gx = x - (w // 2 - 1)
                    if _glyph_on(seed, gx, y):
                        return _mix(_G_SILVER[1], _G_GILT[3], 0.45)
                    return None
                m.cube(eng, (-a / 2, y0, -r - 0.24), (a, L, 0), script, lambda f, x, y, w, h, c: _shade(_G_GILT[4], 0.7), faces=("north",), density=8,
                       tag="script", rotation=(0, ang, 0), pivot=(0, y0, 0))
    m.height = _centre(m)
    return m


def blade_anims():
    f = _AnimFile()
    a = f.new("animation.zachariahs_blade.idle", 4.0, loop=True)
    a.scale("engraving", (0, [1, 1, 1]), (2.0, [1.02, 1, 1.02], "easeInOutSine"), (4.0, [1, 1, 1], "easeInOutSine"))
    fl = f.new("animation.zachariahs_blade.file", 0.6)
    fl.scale("engraving", (0, [1, 1, 1]), (0.08, [1.35, 1.04, 1.35]), (0.3, [1.12, 1.02, 1.12], "easeOutQuad"),
             (0.6, [1, 1, 1], "easeInOutSine"))
    fl.rot("blade", (0, [0, 0, 0]), (0.08, [0, 25, 0]), (0.6, [0, 0, 0], "easeOutQuad"))
    return f


_BLADE_ICON = [
    "..............KK",
    ".............KwK",
    "............KwsK",
    "...........KwsK.",
    "..........KwgK..",
    ".........KwsK...",
    "........KwgK....",
    ".......KwsK.....",
    "..KK..KwgK......",
    "..KbK.KsK.......",
    "...KbKKK........",
    "....KoKbK.......",
    "...KhKKbK.......",
    "..KhHK..KK......",
    ".KpKK...........",
    ".KK.............",
]


def blade_icon():
    pal = {"K": _G_INK, "w": _G_SILVER[5], "s": _G_SILVER[3], "g": _G_GILT[4], "b": _G_SILVER[4], "o": _G_BURNT[5],
           "h": _G_HIDE[4], "H": _G_HIDE[2], "p": _G_BURNT[3]}
    return _art(_BLADE_ICON, pal)


# --- the busts --------------------------------------------------------------------------------------------------------

def _tex16(fn, seed=0):
    t = _Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


_N_SKIN = _Ramp("#6f4a3f", "#a5786a", "#c99b8a", "#dfb6a4", "#ecc9b9", "#f6ddd0")
_N_HAIR = _Ramp("#1e0c06", "#34160b", "#4e2312", "#6a321b", "#864428", "#a35a38")
_N_SUIT = _Ramp("#1b1d22", "#26292f", "#33363d", "#41454d", "#52565f", "#666a74")
_N_WHITE = _Ramp("#8b9199", "#aab0b8", "#c6cbd1", "#dbdfe3", "#ebedf0", "#f9fafb")
_Z_SKIN = _Ramp("#6b4637", "#9a6b58", "#bb8a74", "#cfa18a", "#ddb59f", "#e9c9b5")
_Z_HAIR = _Ramp("#2a241f", "#3b332c", "#4f463d", "#655b50", "#7c7166", "#948a7f")
_Z_SUIT = _Ramp("#2c2f35", "#3c4048", "#4e535c", "#626872", "#787e88", "#90969f")
_Z_TIE = _Ramp("#1e070b", "#320c13", "#4a121c", "#621a26", "#7c2533", "#963443")

_NAOMI_BUST_FACE = [   # x = 0 is the viewer's left
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhHhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hsssssssssssssss",
    "ssbBbbssssbbBbss",
    "ssKKKKssssKKKKss",
    "ssWIEwsssswEIWss",
    "ssuuuussssuuuuss",
    "sssssssnnsssssss",
    "ssCssssnnssssCss",
    "ssssssNnnNssssss",
    "ssssssssssssssss",
    "sssssmMMMMmsssss",
    "jssssslLLlsssssj",
    "jjssssssssssssjj",
]

_ZACH_BUST_FACE = [
    "ssssssssssssssss",
    "ssssssssssssssss",
    "ssssffssssffssss",
    "ssssssssssssssss",
    "ssssssssssssssss",
    "sbbbBsssssBbbbss",
    "ssssssssssssssss",
    "sswEEssssswEEsss",
    "ssuuussssssuuuss",
    "sssssssnnsssssss",
    "sssssssnnsssssss",
    "ssssssNnnNssssss",
    "sssslssssssslsss",
    "ssssmmmmmmmmssss",
    "jsssssMMMMsssssj",
    "jjssssssssssssjj",
]


def _bust_face(rows, legend, base):
    def fn(x, y):
        v = legend.get(rows[y][x])
        return v if v is not None else base(x, y)
    return _tex16(fn)


def naomi_bust():
    def skin(x, y):
        return _tone(_N_SKIN, 3.5 + 0.3 * _m.sin(x * 0.8 + y * 0.5) - 0.5 * y / 15)

    def hair(x, y):
        # Sleek, pulled straight back: strands along y, a soft sheen band.
        n = _fbm(x * 2, y, 23001, 16, 16, 2, 4.0)
        return _tone(_N_HAIR, 3.0 + 0.5 * _m.sin(x * 1.7 + n * 2) + (0.8 if 4 <= y <= 6 else 0) - 0.4 * (y > 12))
    legend = {"h": None, "H": _N_HAIR[5], "s": None, "b": _N_HAIR[2], "B": _N_HAIR[1], "K": _N_HAIR[0], "W": _N_WHITE[4],
              "w": _N_WHITE[3], "I": _hexc("#5d7e98"), "E": _hexc("#2c3e52"), "u": _N_SKIN[2], "n": _N_SKIN[4], "N": _N_SKIN[1],
              "C": _mix(_N_SKIN[3], _hexc("#d07a74"), 0.25), "m": _hexc("#9a5550"), "M": _hexc("#b4665f"), "l": _hexc("#a35c56"),
              "L": _hexc("#bd726a"), "j": _N_SKIN[2]}

    def base(x, y):
        return hair(x, y) if _NAOMI_BUST_FACE[y][x] == "h" else skin(x, y)
    face = _bust_face(_NAOMI_BUST_FACE, legend, base)
    skin_t = _tex16(skin)
    hair_t = _tex16(hair)
    # The bun: hair wound round in a spiral, a pin through it.
    def bun(x, y):
        a = _m.atan2(y - 7.5, x - 7.5)
        r = _m.hypot(x - 7.5, y - 7.5)
        if (x, y) in ((3, 4), (4, 5), (5, 6), (6, 7), (7, 8)):
            return _hexc("#c9ccd1")
        return _tone(_N_HAIR, 2.4 + 1.2 * _m.sin(a * 2 + r * 1.3) - r / 10)
    bun_t = _tex16(bun)

    def chest(x, y):
        # The white coat's lapels outermost, the grey jacket's notch lapels, the white blouse and its collar points.
        d = abs(x - 7.5)
        if d < 1.9 - y * 0.06 and y < 11:
            return _N_WHITE[2] if (d < 0.6 and y % 3 == 2) else _N_WHITE[5 if d < 1.0 else 4]
        if d < 2.6 and y < 2:
            return _N_WHITE[4]
        if d < 3.6:
            return _tone(_N_SUIT, 5.0 - y / 9) if d > 2.7 - y * 0.05 else _tone(_N_SUIT, 3.8 - y / 12)
        if d < 4.5:
            return _tone(_N_SUIT, 4.0 - y / 12)
        if d < 5.2:
            return _N_WHITE[5] if y < 12 else _N_WHITE[4]
        return _tone(_N_WHITE, 3.8 - y / 14 + 0.2 * _m.sin(x * 1.1))
    chest_t = _tex16(chest)
    coat_t = _tex16(lambda x, y: _tone(_N_WHITE, 3.9 - y / 12 + 0.25 * _m.sin(x * 0.9 + y * 0.3)))
    for name, t in (("face", face), ("skin", skin_t), ("hair", hair_t), ("bun", bun_t), ("chest", chest_t), ("coat", coat_t)):
        _save(t, "block", f"naomi_trophy_{name}")
    full = [0, 0, 16, 16]
    F, S, HR, BN, CH, C = "face", "skin", "hair", "bun", "chest", "coat"
    e = [
        _BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        _BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": C}),
        # Narrow shoulders in the white coat, the open front with the suit and blouse, the coat's turned collar.
        _BM.cube("chest", (4.9, 3.4, 6.2), (11.1, 7.4, 9.8), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        _BM.cube("shoulders", (4.2, 5.8, 6.4), (11.8, 7.5, 9.6), {"*": C}),
        _BM.cube("collar", (5.4, 7.2, 8.4), (10.6, 8.3, 9.7), {"*": C}),
        _BM.cube("neck", (7.1, 7.3, 7.1), (8.9, 8.6, 8.9), {"*": S}),
        # The head (a little narrower than the men's), the hair a close cap, the bun at the back of the crown.
        _BM.cube("head", (5.7, 8.4, 5.7), (10.3, 13.2, 10.3), {"north": F, "up": HR, "south": HR, "east": HR, "west": HR},
                 uv={"north": full}),
        _BM.cube("hair_cap", (5.55, 11.6, 5.85), (10.45, 13.45, 10.5), {"north": None, "down": None, "*": HR}),
        _BM.cube("bun", (6.7, 10.9, 10.3), (9.3, 13.1, 11.6), {"*": BN}, uv={"south": full, "north": full}),
        _BM.cube("ear_r", (5.45, 10.0, 7.8), (5.7, 11.2, 8.6), {"*": S}),
        _BM.cube("ear_l", (10.3, 10.0, 7.8), (10.55, 11.2, 8.6), {"*": S}),
        _BM.cube("pearl_r", (5.35, 9.75, 8.05), (5.6, 10.0, 8.3), {"*": C}),
        _BM.cube("pearl_l", (10.4, 9.75, 8.05), (10.65, 10.0, 8.3), {"*": C}),
        _BM.cube("nose", (7.7, 9.9, 5.45), (8.3, 10.8, 5.7), {"*": S}),
    ]
    model = _BM.model({"base": "trophy_base", F: "naomi_trophy_face", S: "naomi_trophy_skin", HR: "naomi_trophy_hair",
                       BN: "naomi_trophy_bun", CH: "naomi_trophy_chest", C: "naomi_trophy_coat"}, e, "trophy_base")
    _BM.write("naomi_trophy", model)


def zachariah_bust():
    def skin(x, y):
        return _tone(_Z_SKIN, 3.3 + 0.3 * _m.sin(x * 0.9 + y * 0.6) - 0.6 * y / 15)

    def scalp(x, y):
        # A side of the head (the back at x = 0): the bald dome above, the grey-brown fringe round the back and over the
        # ear, a short sideburn; the ear itself is a cube.
        c = skin(x, y)
        if y < 4:
            return _mix(c, _Z_SKIN[5], 0.25 * (4 - y) / 4)
        if (x < 9 and y < 12) or (9 <= x <= 10 and y < 9):
            return _tone(_Z_HAIR, 3.2 + 0.7 * _m.sin(y * 1.6 + x * 0.5) - (0.6 if y == 4 else 0))
        return c
    legend = {"s": None, "f": _Z_SKIN[2], "b": _Z_HAIR[2], "B": _Z_HAIR[1], "w": _hexc("#e3dcd2"), "E": _hexc("#4a5a6c"),
              "u": _Z_SKIN[2], "n": _Z_SKIN[4], "N": _Z_SKIN[1], "l": _Z_SKIN[1], "m": _hexc("#7d4a40"), "M": _hexc("#94584c"),
              "j": _Z_SKIN[2]}
    face = _bust_face(_ZACH_BUST_FACE, legend, skin)
    # The top of the head: a shiny crown, the fringe hugging the back edge.
    top = _tex16(lambda x, y: _tone(_Z_HAIR, 3.0 + 0.6 * _m.sin(x * 1.7)) if y >= 13 else _mix(skin(x, y), _Z_SKIN[5],
                 max(0.0, 0.55 - _m.hypot(x - 7.5, y - 6) / 9)))
    sides = _tex16(scalp)
    back = _tex16(lambda x, y: _tone(_Z_HAIR, 3.2 + 0.7 * _m.sin(y * 1.6 + x * 0.3)) if 4 <= y < 13 else
                  (_mix(skin(x, y), _Z_SKIN[5], 0.2) if y < 4 else skin(x, y)))

    def chest(x, y):
        d = abs(x - 7.5)
        if d < 0.9 and y < 15:
            return _Z_TIE[4] if (y + int(x)) % 4 == 0 else _Z_TIE[2]          # a striped burgundy tie
        if d < 3.2 - y * 0.18 and y < 13:
            return _hexc("#e9edf1") if d > 1.3 else _hexc("#d6dbe1")
        if d < 4.1 - y * 0.12:
            return _Z_SUIT[4]                                                 # the lapels
        if 9.6 < x < 12.4 and 4 <= y <= 5:
            return _hexc("#c9b26a") if (x + y) % 2 else _hexc("#e7d79a")     # the pocket square
        return _tone(_Z_SUIT, 3.2 - y / 12 + 0.15 * _m.sin(x * 1.3))
    chest_t = _tex16(chest)
    suit_t = _tex16(lambda x, y: _tone(_Z_SUIT, 3.1 - y / 14 + 0.15 * _m.sin(x * 1.1 + y * 0.4)))

    def wing(x, y):
        # A burnt-gold wing seen from behind: the root at the right edge, the long feathers falling to the bottom left,
        # each feather's shaft drawn and its edge darkened.
        top = (x - 4) * 0.5 if x >= 4 else (4 - x) * 1.1
        if x >= 13:
            top = 4.5 + (x - 13) * 1.6
        bottom = 15 - (x / 15) * 5 - (1 if x % 2 else 0)
        if y < top or y > bottom:
            return None
        if (x + y) % 3 == 0 and y > top + 2:
            return _G_BURNT[2]                                                # feather edges
        v = 4.6 - 2.6 * (y - top) / max(1.0, bottom - top) + 0.3 * _m.sin(x * 2.1)
        return _tone(_G_BURNT, v)
    wing_t = _tex16(wing)
    silver_t = _tex16(lambda x, y: _tone(_G_BURNT, 4.2 - 2.0 * y / 15))
    for name, t in (("face", face), ("skin", _tex16(skin)), ("top", top), ("sides", sides), ("back", back), ("chest", chest_t),
                    ("suit", suit_t), ("wing", wing_t), ("gold", silver_t)):
        _save(t, "block", f"zachariah_trophy_{name}")
    F, S, TP, SD, BK, CH, C, W, AU = "face", "skin", "top", "sides", "back", "chest", "suit", "wing", "gold"
    full = [0, 0, 16, 16]
    e = [
        _BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        _BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": AU}),
        _BM.cube("wing_r", (0.0, 3.4, 10.4), (7.2, 17.2, 10.4), {"north": W, "south": W}, uv={"north": [16, 0, 0, 16], "south": full},
                 rot=("y", 22.5, (7.2, 8, 10.4))),
        _BM.cube("wing_l", (8.8, 3.4, 10.4), (16.0, 17.2, 10.4), {"north": W, "south": W}, uv={"north": full, "south": [16, 0, 0, 16]},
                 rot=("y", -22.5, (8.8, 8, 10.4))),
        # A paunchy middle manager: broad, a little round at the front.
        _BM.cube("chest", (4.4, 3.4, 5.8), (11.6, 7.6, 10.0), {"north": CH, "*": C}, uv={"north": [0, 2, 16, 14]}),
        _BM.cube("shoulders", (3.6, 6.0, 6.2), (12.4, 7.7, 9.8), {"*": C}),
        _BM.cube("collar", (5.8, 7.5, 8.2), (10.2, 8.4, 9.4), {"*": C}),
        _BM.cube("neck", (6.8, 7.6, 6.8), (9.2, 8.5, 9.2), {"*": S}),
        _BM.cube("head", (5.4, 8.3, 5.4), (10.6, 13.5, 10.6), {"north": F, "up": TP, "south": BK, "east": SD, "west": SD},
                 uv={"north": full, "up": full, "south": full, "east": full, "west": [16, 0, 0, 16]}),
        _BM.cube("ear_r", (5.1, 10.0, 7.6), (5.4, 11.5, 8.6), {"*": S}),
        _BM.cube("ear_l", (10.6, 10.0, 7.6), (10.9, 11.5, 8.6), {"*": S}),
        _BM.cube("nose", (7.55, 9.7, 5.1), (8.45, 10.9, 5.4), {"*": S}),
    ]
    model = _BM.model({"base": "trophy_base", F: "zachariah_trophy_face", S: "zachariah_trophy_skin", TP: "zachariah_trophy_top",
                       SD: "zachariah_trophy_sides", BK: "zachariah_trophy_back", CH: "zachariah_trophy_chest", C: "zachariah_trophy_suit",
                       W: "zachariah_trophy_wing", AU: "zachariah_trophy_gold"}, e, "trophy_base")
    _BM.write("zachariah_trophy", model)


def _geo_items():
    for rig_fn, anim_fn, icon_fn, name in ((drill_rig, drill_anims, drill_icon, "naomis_drill"),
                                           (blade_rig, blade_anims, blade_icon, "zachariahs_blade")):
        m = rig_fn()
        t, g = m.build(gutter=1, seed=23000)
        m.rig.write(_GEO_ITEM + name + ".geo.json")
        _save(t, "item", name)
        _save(g, "item", name + "_glowmask")
        anim_fn().write(_ANIM_ITEM + name + ".animation.json")
        _save(icon_fn(), "item", name + "_icon")
        print(f"{name}: height {m.height:.1f}")
    naomi_bust()
    zachariah_bust()


# =====================================================================================================
# Flat sprites (2D art work)
# =====================================================================================================

def diadem():
    t = Tex(16, 16, 20001)
    cx, cy = 7.5, 9.0
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            outer = (dx / 7.0) ** 2 + (dy / 3.6) ** 2
            inner = (dx / 5.6) ** 2 + ((dy + 0.7) / 2.2) ** 2
            if outer <= 1.0 and inner > 1.0:
                if dy < -0.5:
                    c = tone(WGOLD, 2.2 + dx / 10)                     # the far side of the band, in shade
                else:
                    c = tone(WGOLD, 4.0 - dy / 3.0 + (0.7 if abs(dx) < 2 else 0) - abs(dx) / 9)
                t.set(x, y, c)
    # A fine gold line along the front of the band.
    for x in range(2, 14):
        for y in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if dy > 0.5 and abs((dx / 6.3) ** 2 + (dy / 2.9) ** 2 - 1) < 0.16:
                t.set(x, y, GOLD[4])
    # The setting: a pointed crest rising from the front, the gem in it, two small side points.
    crest = {(7, 5): WGOLD[5], (8, 5): WGOLD[4], (6, 6): WGOLD[4], (9, 6): WGOLD[3], (6, 7): WGOLD[4], (9, 7): WGOLD[3],
             (7, 4): WGOLD[5], (8, 4): WGOLD[3], (7, 3): WGOLD[5],
             (7, 6): CYAN[4], (8, 6): CYAN[3], (7, 7): CYAN[3], (8, 7): CYAN[2], (7, 8): CYAN[2], (8, 8): CYAN[1],
             (6, 8): WGOLD[4], (9, 8): WGOLD[3], (7, 9): WGOLD[4], (8, 9): WGOLD[3]}
    for (x, y), c in crest.items():
        t.set(x, y, c)
    t.set(7, 6, CYAN[5])
    for sx in (3, 12):
        t.set(sx, 9, WGOLD[5])
        t.set(sx, 8, CYAN[3])
    item_outline(t, O)
    return t


def heavens_seal():
    t = Tex(16, 16, 20002)
    cx, cy = 7.5, 6.5
    # Ribbon tails first (behind the medallion), notched at their ends.
    ribbon = hexc("#e9eef4")
    rib_d = hexc("#9fb0c4")
    for y in range(10, 16):
        lx = 5 - (y - 10) // 2
        rx = 10 + (y - 10) // 2
        for x in (lx, lx + 1):
            t.set(x, y, ribbon if x == lx else rib_d)
        for x in (rx, rx + 1):
            t.set(x, y, ribbon if x == rx + 1 else rib_d)
    t.set(lx, 15, CLEAR)
    t.set(rx + 1, 15, CLEAR)
    # The medallion.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5)
            if d <= 6.2:
                lit = 1 - ((x - cx) + (y - cy)) / 12
                if d > 5.2:
                    c = GOLD[2] if (math.degrees(math.atan2(y - cy, x - cx)) % 30) < 15 else GOLD[3]   # the milled rim
                elif d > 4.5:
                    c = tone(GOLD, 3.0 + lit * 1.4)
                else:
                    c = mix(hexc("#1f4a8a"), hexc("#4f86cc"), max(0.0, min(1.0, lit * 0.8)))   # azure enamel
                t.set(x, y, c)
    # Heaven's star on the enamel: a four-pointed star of light, small sparks between its arms.
    sx, sy = 7, 6
    for k in range(1, 4):
        c = GOLD[5] if k < 3 else GOLD[4]
        for (dx, dy) in ((k, 0), (-k, 0), (0, k), (0, -k)):
            t.set(sx + dx + (1 if dx > 0 else 0), sy + dy + (1 if dy > 0 else 0), c)
            if k == 1:
                t.set(sx + dx + (1 if dx >= 0 else 0), sy + dy + (1 if dy >= 0 else 0), c)
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        t.set(sx + dx, sy + dy, hexc("#ffffff") if (dx, dy) == (0, 0) else GOLD[5])
    for (x, y) in ((5, 4), (10, 4), (5, 9), (10, 9)):
        t.set(x, y, GOLD[4])
    item_outline(t, O)
    return t


def heavenly_form():
    t = Tex(16, 16, 20003)
    for y in range(1, 15):
        for x in range(3, 13):
            t.set(x, y, tone(PAPER, 4.4 - (x - 3) / 12 - (y - 1) / 18))
    # The dog-eared corner (top right).
    for (x, y) in ((12, 1), (11, 1), (12, 2)):
        t.set(x, y, CLEAR)
    t.set(11, 2, PAPER[2])
    t.set(10, 1, PAPER[3])
    # Letterhead: a gold seal and a gold rule.
    for (x, y) in ((4, 2), (5, 2), (4, 3), (5, 3)):
        t.set(x, y, GOLD[4] if (x, y) != (5, 3) else GOLD[2])
    for x in range(7, 10):
        t.set(x, 2, PAPER[1])
    for x in range(4, 12):
        t.set(x, 4, GOLD[3])
    # Typed lines.
    ink = hexc("#5e6470")
    for y, (a, b) in ((6, (4, 11)), (8, (4, 10)), (10, (4, 11))):
        for x in range(a, b + 1):
            if (x * 7 + y * 3) % 9 != 0:
                t.set(x, y, ink if (x + y) % 3 else PAPER[1])
    # The signature box and a pen flourish in it.
    for x in range(7, 12):
        t.set(x, 12, ink)
        t.set(x, 13, PAPER[2])
    t.set(8, 11, hexc("#2c3550"))
    t.set(9, 11, hexc("#2c3550"))
    t.set(10, 10, hexc("#2c3550"))
    for y in range(12, 14):
        t.set(4, y, PAPER[1])
        t.set(5, y, PAPER[1])
    item_outline(t, O)
    return t


def approval_stamp():
    from common import art
    pal = {"o": O, "k": WOOD[5], "K": WOOD[4], "w": WOOD[3], "W": WOOD[2], "d": WOOD[1], "b": BRASS[4], "B": BRASS[5],
           "e": BRASS[2], "r": RUBBER[4], "R": RUBBER[2], "x": RUBBER[1]}
    return art([
        "................",
        ".....oooooo.....",
        "....okKKKwwo....",
        "....oKKKwwWo....",
        "....owwwwWWo....",
        ".....oWWWdo.....",
        "......owWo......",
        "......owdo......",
        "......owdo......",
        "....oobbbeoo....",
        "...oBbbbbbeeo...",
        "..oKKKKwwwwWWo..",
        "..owwwwwwwWWdo..",
        "..oddddddddddo..",
        "..orrrrrRRRRxo..",
        "...oooooooooo...",
    ], pal)


def crossroads_box():
    t = Tex(16, 16, 20005)
    front = (1, 11, 8, 13)            # x0, x1, y0, y1
    # The front face: tin, the lid's lip (row 8-9), a latch, rust creeping up from the foot.
    for y in range(front[2], front[3] + 1):
        for x in range(front[0], front[1] + 1):
            v = 2.8 - (0.6 if y == 10 else 0) + (0.5 if y == 8 else 0) - x / 30
            c = tone(TIN, v)
            if y >= 12 and (x * 5 + y) % 4 == 0:
                c = RUST[3]
            t.set(x, y, c)
    t.set(6, 10, BRASS[4])
    t.set(6, 11, BRASS[2])
    # The right side, receding up and to the right.
    for i, x in enumerate((12, 13, 14)):
        for y in range(7 - i, 13 - i):
            c = tone(TIN, 1.6 - (0.4 if y == 9 - i else 0))
            if y >= 11 - i and (x + y) % 3 == 0:
                c = RUST[2]
            t.set(x, y, c)
    # The lid, a parallelogram above.
    for r, y in enumerate((7, 6, 5, 4)):
        for x in range(2 + r, 12 + r):
            if x > 14:
                continue
            c = tone(TIN, 4.0 - r * 0.2 + (0.6 if r == 3 else 0))
            t.set(x, y, c)
    # A crossroads scratched into the lid, and a dent.
    for (x, y) in ((5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (7, 4), (7, 5), (7, 7)):
        t.set(x, y, TIN[1])
    for x in range(2, 12):
        t.set(x, 8, TIN[5] if x % 4 else TIN[4])          # the lid's bright rolled edge
    t.set(11, 5, TIN[2])
    t.set(12, 5, TIN[5])
    t.set(3, 7, RUST[4])
    t.set(4, 7, RUST[3])
    item_outline(t, O)
    return t


def _sprites():
    save(diadem(), "item", "naomis_diadem")
    save(heavens_seal(), "item", "heavens_seal")
    save(heavenly_form(), "item", "heavenly_form")
    save(approval_stamp(), "item", "approval_stamp")
    save(crossroads_box(), "item", "crossroads_box")


def generate():
    _geo_items()
    _sprites()


if __name__ == "__main__":
    generate()
