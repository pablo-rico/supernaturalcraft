"""A soldier of the Host of Heaven (v0.12): an angel in a vessel, with a light breastplate over its clothes, an angel
blade, an engraved tower shield and folded shadow wings.

One geometry, four textures (same UV layout; the packing only depends on the cubes):
  host_angel_0        a dark suit, white shirt, dark tie; steel-grey plate;
  host_angel_1        a tan trench coat over a suit (its skirt `coat_tail_*` is transparent in the other textures);
  host_angel_2        soldier's fatigues and boots;
  host_angel_captain  white-and-gold armour over a pale suit, a white tabard and a short cloak (`tabard`, `cloak`:
                      transparent in the soldiers' textures), a helm with a nasal guard and a tall plume, open wings.
The renderer shows `helmet`, `plume` and `wings_open` only for the captain and hides `wings_folded` for him.
`host_angel_glowmask` (the eyes, the blade's edge, the wings' light, the sigils) is shared.

Skeleton: the vessel's (michael_art.skeleton) plus
  body -- breastplate, coat_tail_r, coat_tail_l, tabard, cloak, wings_folded -- wing_fold_r, wing_fold_l
       -- wings_open -- wing_open_r, wing_open_l -- <wing>_f0..f8
  head -- helmet -- plume;  right_hand -- blade;  left_forearm -- shield
"""

import math

import horsemen_art as H
from animkit import AnimFile
from chuck_art import Model, none_on, solid
from common import save
from michael_art import (ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, aim2, parent_offset, to_preview, GOLD, LIGHT, SILVER, SMOKE, WHITE_METAL, Gilt, MetalFeather, Plate, SmokeFeather,
                         add_feather, build_blade, delta_pose, edge_cube, fabric, glyph_bit, mirror_rot, skeleton, smoke_glow,
                         tone)
from pixelkit import Ramp, hexc, mix, shade

A = "animation.host_angel."
VARIANTS = ("0", "1", "2", "captain")
CLEAR = (0, 0, 0, 0)

SKIN = Ramp("#5d3e30", "#8a5f4b", "#ad7d64", "#c4947a", "#d5ab90", "#e3c0a6")
HAIR = Ramp("#0e0b09", "#1a1410", "#271e17", "#35291f", "#443428", "#544132")
SUIT = Ramp("#07080b", "#0e1015", "#16191f", "#1f232b", "#2a2f38", "#363c47")
SHIRT = Ramp("#9aa1aa", "#b5bbc3", "#cbd0d6", "#dde1e6", "#ebeef1", "#f8f9fb")
TIE = Ramp("#06070a", "#0c0e13", "#13161d", "#1b1f28", "#252a35", "#303643")
COAT = Ramp("#4a3a24", "#66512f", "#81693d", "#9b824f", "#b29a64", "#c6b07b")
FATIGUE = Ramp("#22281a", "#323a25", "#424c30", "#535e3c", "#667249", "#7b8758")
PALE = Ramp("#7c8594", "#9aa3b1", "#b7bfcb", "#d0d6de", "#e4e8ed", "#f6f8fa")
TABARD = Ramp("#a7b3c6", "#c2ccdb", "#d8e0eb", "#e8eef5", "#f3f7fb", "#ffffff")
BOOT = Ramp("#0a0807", "#120e0c", "#1a1512", "#241d18", "#2e261f", "#3a3028")
STEEL_TRIM = Ramp("#3d434d", "#59606c", "#7a828f", "#a0a8b4", "#c4cbd5", "#e6eaef")
EYE = hexc("#dff1ff")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hssssssssssssssh",
    "ssssssssssssssss",
    "sbbbbbssssbbbbbs",
    "sswwwwssssswwwws",
    "ssuuuussssuuuuss",
    "ssssssnnnsssssss",
    "ssssssNNNNssssss",
    "ssssssssssssssss",
    "sssssmmmmmmsssss",
    "ssssssssssssssss",
    "jssssssssssssssj",
    "jjssssssssssssjj",
]


def clothes(variant):
    """(jacket, trousers, shoe ramp, coat tails or None, tie ramp or None, shirt)."""
    if variant in ("0", "captain"):
        ramp = SUIT if variant == "0" else PALE
        return fabric(ramp, 9101, 3.0, 0.3), fabric(ramp, 9102, 2.7, 0.15, crease=True), BOOT, None, TIE, fabric(SHIRT, 9103, 3.6, 0.15)
    if variant == "1":
        return fabric(COAT, 9111, 3.0, 0.4), fabric(SUIT, 9112, 2.7, 0.15, crease=True), BOOT, fabric(COAT, 9113, 2.8, 0.45), TIE, \
            fabric(SHIRT, 9114, 3.6, 0.15)
    camo = fabric(FATIGUE, 9121, 2.8, 0.55)
    return camo, fabric(FATIGUE, 9122, 2.6, 0.5), BOOT, None, None, fabric(FATIGUE, 9123, 2.2, 0.3)


def sigil(face, x, y, w, h):
    """A sigil of the Host: a ring, a cross, four dots between its arms."""
    c = (w - 1) / 2
    r = math.hypot(x - c, y - c)
    if abs(r - (c - 0.5)) < 0.7:
        return GOLD[4]
    if (abs(x - c) < 0.6 or abs(y - c) < 0.6) and r < c - 1:
        return LIGHT[4]
    if abs(abs(x - c) - c * 0.5) < 0.6 and abs(abs(y - c) - c * 0.5) < 0.6:
        return GOLD[5]
    return None


def rig(variant="0"):
    cap = variant == "captain"
    m = Model("host_angel", 256, 256)
    skeleton(m)
    glow = H.glow_faint(0.08)
    skin = H.skin_mat(SKIN, 9001, 3.0, 0.35)
    hair = lambda f, x, y, w, h: tone(HAIR, 2.5 + 0.9 * math.sin(x * 1.4 + y * 0.6) - 0.4 * (y / max(1, h)))
    eye = lambda f, x, y, w, h, c: EYE if c == EYE else shade(c, 0.08)
    legend = {"h": hair, "s": skin, "b": HAIR[2], "w": EYE, "u": SKIN[2], "n": SKIN[4], "N": SKIN[1], "m": hexc("#7f4f45"),
              "j": SKIN[2]}
    H.head_cube(m, FACE, legend, H.head_sides(skin, hair, top=4, back=12), glow=eye)
    m.cube("head", (-0.5, 26.5, -4.5), (1, 2, 0.5), lambda f, x, y, w, h: SKIN[4] if f == "north" else SKIN[2], glow=glow,
           density=2, tag="nose")
    m.cube("body", (-1.5, 23.5, -1.5), (3, 1, 3), skin, glow=glow, density=2, tag="neck")
    jacket, trousers, shoe, tail, tie, shirt = clothes(variant)
    m.cube("body", (-4, 12, -2), (8, 12, 4), shirt, glow=glow, density=2, tag="torso")
    tie_m = (lambda f, x, y, w, h: tie[4] if x == 7 else tie[2]) if tie else None
    lapel = jacket("north", 0, 0, 1, 2)
    front = H.jacket_front(jacket, shade(lapel, 1.12), shirt, tie=tie_m, gap=(6, 9), v_depth=8)
    m.cube("body", (-4, 12, -2), (8, 12, 4), none_on(("down",), front), glow=glow, inflate=0.3, density=2, tag="jacket")
    hand = H.skin_mat(SKIN, 9004, 3.1, 0.35)
    for side in ("right", "left"):
        H.arm_cubes(m, side, jacket, hand, glow=glow, sleeve_inflate=0.25)
        H.leg_cubes(m, side, trousers, lambda f, x, y, w, h: shoe[4] if f == "up" and y < 3 else shoe[2], glow=glow, shoe_h=2.0)
    # Trench-coat skirt (transparent unless it is the coat).
    for side, s in (("r", -1), ("l", 1)):
        b = m.bone(f"coat_tail_{side}", (2 * s, 12, 0), "body")
        mat = tail if tail else (lambda f, x, y, w, h: CLEAR)
        m.cube(b, (2 * s - 2.2, 3, -2.4), (4.4, 9, 4.8), none_on(("up", "down"), mat), glow=glow, density=2, tag="coat_tail")
    # The captain's tabard and short cloak (transparent for the soldiers).
    tb = m.bone("tabard", (0, 12, -2.5), "body")
    clk = m.bone("cloak", (0, 23.5, 2.6), "body")

    def tabard(f, x, y, w, h):
        if not cap:
            return CLEAR
        if f in ("north", "south") and (x in (0, w - 1) or y == h - 1):
            return GOLD[4]
        if f in ("north", "south") and abs(x - (w - 1) / 2) < 1 and y < h - 3:
            return GOLD[3]
        return tone(TABARD, 4.0 - 1.4 * y / max(1, h))
    m.cube(tb, (-3, 3.5, -2.75), (6, 8.5, 0.25), tabard, H.glow_faint(0.15), density=2, tag="tabard")
    m.cube(tb, (-3, 3.5, 2.3), (6, 8.5, 0.25), tabard, H.glow_faint(0.15), density=2, tag="tabard")

    def cloak(f, x, y, w, h):
        if not cap:
            return CLEAR
        if f in ("north", "south") and y >= h - 2:
            return GOLD[4]
        return tone(LIGHT, 3.2 - 1.2 * y / max(1, h) + 0.3 * math.sin(x * 1.2))
    m.cube(clk, (-4.75, 7.5, 2.6), (9.5, 16, 0.5), cloak, H.glow_faint(0.3), density=2, tag="cloak", rotation=(6, 0, 0),
           pivot=(0, 23.5, 2.6))
    belt = Gilt(9150, 2) if cap else None
    m.cube("body", (-4, 11.6, -2), (8, 1.2, 4), belt.mat if belt else (lambda f, x, y, w, h: BOOT[2 + (x % 7 == 0)]),
           belt.glow if belt else glow, inflate=0.38, density=2, tag="belt")
    build_armour(m, cap)
    build_blade(m, "right_hand")
    build_shield(m, cap)
    build_wings(m)
    return m


def build_armour(m, cap):
    ramp = WHITE_METAL if cap else SILVER
    bp = m.bone("breastplate", (0, 18, 0), "body")
    plate = Plate(9201, 2, ramp=ramp, base=3.0 if cap else 2.8, rows=1)
    m.cube(bp, (-4.5, 14.5, -2.9), (9, 8.5, 1), plate.mat, plate.glow, density=2, tag="breastplate")
    for s in (-1, 1):
        pec = Plate(9202 + s, 2, ramp=ramp, engrave=False, rivets=False, base=3.0 if cap else 2.8)
        m.cube(bp, (s * 2.3 - 2.2, 18.5, -3.5), (4.4, 4.2, 0.8), pec.mat, pec.glow, density=2, tag="pec",
               rotation=(-6, -14 * s, 0), pivot=(s * 2.3, 20.5, -3.0))
        strap = Gilt(9205, 2) if cap else Plate(9205, 2, ramp=ramp, engrave=False, rivets=False)
        m.cube(bp, (s * 5 - 2.25, 22.5, -2.6), (4.5, 1.6, 5.2), strap.mat, strap.glow, density=2, tag="pauldron",
               rotation=(0, 0, -14 * s), pivot=(s * 5, 23, 0))
    m.cube(bp, (-1.5, 15.5, -3.45), (3, 3, 0.25), sigil, lambda f, x, y, w, h, c: c, density=4, faces=("north",), tag="sigil")
    back = Plate(9206, 2, ramp=ramp, engrave=False)
    m.cube(bp, (-4.5, 14.5, 2.2), (9, 8.5, 0.7), back.mat, back.glow, density=2, tag="backplate")
    # The captain's helm: a rounded bowl with a gold rim, a nasal guard and cheek pieces; the plume rises from a crest.
    hm = m.bone("helmet", (0, 28, 0), "head")
    helm = Plate(9210, 2, ramp=WHITE_METAL, engrave=False, rivets=False)

    def bowl(f, x, y, w, h):
        if f == "down":
            return None
        if f in ("north", "south", "east", "west") and y >= h - 2:
            return GOLD[4] if y == h - 2 else GOLD[2]
        return helm.mat(f, x, y, w, h)
    m.cube(hm, (-4.5, 28.0, -4.5), (9, 4.5, 9), bowl, helm.glow, density=2, tag="helmet")
    m.cube(hm, (-0.6, 25.0, -4.95), (1.2, 3.5, 0.5), solid(GOLD[4]), H.glow_faint(0.3), density=2, tag="nasal")
    for s in (-1, 1):
        m.cube(hm, (s * 4.25 - 0.4, 25.5, -3.5), (0.8, 3.0, 4.5), lambda f, x, y, w, h: GOLD[4] if y == 0 else WHITE_METAL[3],
               H.glow_faint(0.15), density=2, tag="cheek")
    m.cube(hm, (-0.75, 32.3, -3.5), (1.5, 1.2, 7), solid(GOLD[4]), H.glow_faint(0.3), density=2, tag="crest_holder")
    pm = m.bone("plume", (0, 33.4, -2.5), hm.name, rotation=(-10, 0, 0))
    # A tall plume: five white feathers tipped gold, each in two joints that curl back and fan out a little.
    for i in range(5):
        z = -2.6 + i * 1.3
        spread = (i - 2) * 7
        L1, L2 = 5.0 - abs(i - 2) * 0.6, 4.5 - abs(i - 2) * 0.5
        r1, r2 = -8 - i * 6, -40 - i * 8
        base = (0.0, 33.4, z)
        mf = MetalFeather("crest", -1, 9220 + i, WHITE_METAL)
        m.cube(pm, (-0.8, 33.4, z - 0.25), (1.6, L1, 0.5), lambda f, x, y, w, h: WHITE_METAL[5] if f in ("north", "south")
               else GOLD[3], H.glow_faint(0.4), density=3, tag="plume", rotation=(r1, 0, spread), pivot=base)
        a1 = math.radians(r1)
        az = math.radians(spread)
        joint = (L1 * math.cos(a1) * math.sin(az), 33.4 + L1 * math.cos(a1) * math.cos(az), z - L1 * math.sin(a1))
        m.cube(pm, (joint[0] - 0.8, joint[1], joint[2] - 0.25), (1.6, L2, 0.5), mf.mat, H.glow_faint(0.45), density=3, tag="plume",
               rotation=(r2, 0, spread), pivot=joint)


def build_shield(m, cap):
    sh = m.bone("shield", (8.3, 15, 0), "left_forearm")
    ramp = WHITE_METAL if cap else SILVER

    def face(f, x, y, w, h):
        if f != "east":
            return GOLD[2] if f in ("up", "down") else tone(ramp, 2.4)
        cx = (w - 1) / 2
        e = min(x, y, w - 1 - x, h - 1 - y)
        if e == 0:
            return GOLD[5] if y < h / 2 else GOLD[2]
        if e == 1:
            return GOLD[3]
        # Engraved script along the top and bottom.
        for r0 in (3, h - 8):
            if 0 <= y - r0 < 5 and 3 <= x < w - 3 and (x - 3) % 4 < 3 and glyph_bit(9300 + r0, (x - 3) // 4, 0, (x - 3) % 4, y - r0):
                return GOLD[2]
        # The emblem: a sword, point down, between two wings.
        sy0, sy1 = 9, h - 10
        if sy0 <= y <= sy1 and abs(x - cx) < 0.8:
            return SILVER[5] if y > sy0 + 3 else GOLD[4]
        if y == sy0 + 3 and abs(x - cx) < 3:
            return GOLD[4]  # the guard
        for s in (-1, 1):
            # Each wing: three feathers fanning out and up from beside the guard.
            for k in range(3):
                ang = math.radians(25 + k * 22)
                for t in range(2, 7 - k):
                    px = cx + s * (1.5 + t * math.cos(ang))
                    py = sy0 + 3 - t * math.sin(ang) + k * 1.5 + 2
                    if abs(x - px) < 0.6 and abs(y - py) < 0.6:
                        return GOLD[4] if t < 5 - k else GOLD[5]
        return tone(ramp, 3.4 - 1.2 * y / max(1, h))

    def glow(f, x, y, w, h, c):
        return shade(c, 0.35) if c in (GOLD[4], GOLD[5], SILVER[5]) else shade(c, 0.1)
    m.cube(sh, (8.2, 5, -5), (1.2, 16, 10), face, glow, density=2, tag="shield")
    boss = Gilt(9310, 2)
    m.cube(sh, (9.3, 12, -1), (0.8, 2, 2), boss.mat, boss.glow, density=2, tag="shield_boss")


def build_wings(m):
    """Folded: a closed wing down each side of the back to the knees (an arc above the shoulder, coverts, secondaries,
    long primaries). Open (the captain): a fan of nine shadow feathers on an arm of smoke."""
    fold = m.bone("wings_folded", (0, 21, 2.5), "body")
    for side, s in (("r", -1), ("l", 1)):
        w = m.bone(f"wing_fold_{side}", (1.6 * s, 22.5, 2.6), fold.name, rotation=mirror_rot((6, 20, 3), s))
        x0 = 1.6 * s
        layers = [  # (width, top y, length, z, x offset, shape)
            (4.2, 26.5, 6.0, 2.8, 1.4, True),     # the wrist arc above the shoulder
            (5.0, 23.5, 8.0, 3.0, 2.0, True),     # coverts
            (5.2, 22.0, 12.5, 3.3, 2.4, False),   # secondaries
            (4.6, 21.5, 16.0, 3.6, 2.8, False),   # primaries, to the knees
        ]
        for i, (wd, top, L, z, xo, broad) in enumerate(layers):
            sf = SmokeFeather(9400 + i + (0 if s < 0 else 10), 235, broad=broad)

            def mat(f, x, y, w_, h, sf=sf):
                if f not in ("north", "south"):
                    return (SMOKE[3][0], SMOKE[3][1], SMOKE[3][2], 230)
                return sf.mat(f, x, y, w_, h)
            px = x0 + s * xo
            m.cube(w, (px - wd / 2, top - L, z), (wd, L, 0.5), mat, smoke_glow, density=2, tag="fold_feather",
                   rotation=(0, 0, -s * (2 + i * 2.5)), pivot=(px, top, z))
    op = m.bone("wings_open", (0, 21, 2.5), "body")
    for side, s in (("r", -1), ("l", 1)):
        name = f"wing_open_{side}"
        root = (1.6 * s, 22, 2.8)
        w = m.bone(name, root, op.name, rotation=mirror_rot((0, 20, 14), s))
        out, rise = 22.0, 5.0
        tip = (root[0] + s * out, root[1] + rise, root[2])

        def arm(f, x, y, w_, h, s=s):
            t = y / max(1, h - 1)
            q = x / max(1, w_ - 1)
            if (f == "north") == (s < 0):
                q = 1 - q
            if t > 0.5 + 0.4 * (1 - q) or q > 0.97:
                return None
            if t < 0.2:
                col = mix(LIGHT[3], LIGHT[1], q)
                return (col[0], col[1], col[2], int(185 - 300 * t))
            col = mix(SMOKE[3], LIGHT[0], 0.2)
            return (col[0], col[1], col[2], 215)
        edge_cube(m, w, root, tip, 3.2, s, arm, smoke_glow, tag="wing_arm", dz=-0.1)
        for i in range(9):
            f = i / 8
            sf = SmokeFeather(9500 + i + (0 if s < 0 else 20), 220)
            add_feather(m, w, f"{name}_f{i}", (root[0] + s * out * (0.1 + 0.9 * f), root[1] + rise * (0.1 + 0.9 * f) - 0.4,
                        2.9 + 0.12 * i), 12 + 10 * f, 5.5, 6 + 78 * f ** 1.2, s, sf.mat, smoke_glow, thick=0,
                        faces=("north", "south"), tag="open_feather")


HIT_TIMES = {"slash": 0.32, "charge": 0.5, "block": 0.25, "shield_wall": 0.5, "march": 0.25, "die": 2.0}
WING_ROOTS = ("wings_folded", "wings_open")


def preview_hidden(clip):
    return ["helmet", "wings_open"]


def anims():
    m = rig()
    r = m.rig
    f = AnimFile()
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]

    def shield_front(pose):
        # The shield's face (+X, outward) turned to the front, its long side upright.
        p = P_(pose, {"shield": aim2(r, pose, "shield", (1, 0, 0), (0, 0, -1), (0, 1, 0), (0, 1, 0))})
        # ...and carried out in front of the fist, towards the middle of the body.
        return P_(p, {"@shield": parent_offset(r, to_preview(p), "shield", (-1.5, 0, -3.0))})

    # idle: at ease, the blade low, the shield grounded; a slow breath, a glance.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        c.key(t, {"body": [0.8 * abs(k), 0, 0], "head": [-1, 6 * k, 0], "right_arm": [-8, 0, 4], "right_forearm": [-20, 0, 0],
                  "left_arm": [-4, 0, -4], "@body": [0, -0.15 * abs(k), 0], "wings_folded": [1.5 * abs(k), 0, 0]}, E_IO if i else None)

    # march: in step, blade at the shoulder, shield carried; every soldier the same clip, so they move as one.
    c = clip("march", 1.0, loop=True)
    for i in range(5):
        t = i / 4
        sn = math.sin(2 * math.pi * t)
        c.key(t, {"right_leg": [-30 * sn, 0, 0], "left_leg": [30 * sn, 0, 0], "right_shin": [40 * max(0, math.sin(2 * math.pi * t + 1.2)), 0, 0],
                  "left_shin": [40 * max(0, -math.sin(2 * math.pi * t + 1.2)), 0, 0], "right_arm": [-12, 0, 6], "right_forearm": [-45, 0, 0],
                  "left_arm": [-12 + 6 * sn, 0, -4], "left_forearm": [-30, 0, 0], "body": [3, 0, 0], "head": [-2, 0, 0],
                  "@root": [0, 0.5 * abs(math.cos(2 * math.pi * t)), 0], "coat_tail_r": [10 * max(0, -sn), 0, 0],
                  "coat_tail_l": [10 * max(0, sn), 0, 0]}, E_IO if i else None)

    # shield_wall: crouched behind the shield, locked forward, blade drawn back over it; a steady breath.
    wall = shield_front({"@root": [0, -1.2, 0], "left_arm": [-58, -10, 22], "left_forearm": [-40, 0, 0], "right_arm": [-60, 30, 30],
                         "right_forearm": [-50, 0, 0], "body": [10, 0, 0], "head": [-8, 0, 0], "right_leg": [14, 0, 0], "left_leg": [-26, 0, 0],
                         "left_shin": [24, 0, 0], "right_shin": [18, 0, 0]})
    c = clip("shield_wall", 1.5, loop=True)
    c.key(0, wall)
    c.key(0.75, P_(wall, {"body": [11.5, 0, 0], "@root": [0, -1.4, 0]}), E_IO)
    c.key(1.5, wall, E_IO)

    # charge: head down, shield first, blade ready -- a lunge that peaks at 0.5.
    c = clip("charge", 1.0)
    run0 = shield_front({"body": [18, 0, 0], "head": [-14, 0, 0], "left_arm": [-60, -10, 20], "left_forearm": [-40, 0, 0], "right_arm": [-40, 20, 20],
                         "right_forearm": [-60, 0, 0], "right_leg": [24, 0, 0], "left_leg": [-36, 0, 0], "left_shin": [30, 0, 0]})
    run1 = P_(run0, {"right_leg": [-34, 0, 0], "left_leg": [26, 0, 0], "right_shin": [30, 0, 0], "left_shin": [10, 0, 0], "@root": [0, 0.8, -3]})
    c.key(0, {})
    c.key(0.25, run0, E_OUT)
    c.key(0.5, run1, E_IO)
    c.key(0.75, P_(run0, {"@root": [0, 0, -2]}), E_IO)
    c.key(1.0, {}, E_IO)

    # slash: blade raised over the shield, cut down across (0.32), follow through.
    c = clip("slash", 0.8)
    c.key(0, {})
    c.key(0.18, {"right_arm": [-140, 20, 30], "right_forearm": [-40, 0, 0], "body": [-4, 22, 0], "head": [0, -10, 0], "left_arm": [-30, 0, -10]}, E_OUT)
    c.key(0.32, {"right_arm": [-50, -40, -10], "right_forearm": [-8, 0, 0], "body": [12, -24, 0], "head": [0, 10, 0], "left_arm": [-20, 0, -14],
                 "@root": [0, -0.8, -1.5], "left_leg": [-20, 0, 0], "right_leg": [12, 0, 0]}, E_SNAP)
    c.key(0.5, {"right_arm": [-20, -50, -14], "body": [10, -28, 0], "@root": [0, -0.6, -1.5], "left_leg": [-16, 0, 0], "right_leg": [10, 0, 0]}, E_OUT)
    c.key(0.8, {}, E_IO)

    # block: the shield snapped up in front (by 0.25), braced, lowered.
    c = clip("block", 0.8)
    guard = shield_front({"left_arm": [-80, -10, 22], "left_forearm": [-30, 0, 0], "body": [-6, 0, 0], "head": [6, 0, 0], "@root": [0, -1, 1],
                          "right_leg": [16, 0, 0], "left_leg": [-10, 0, 0]})
    c.key(0, {})
    c.key(0.2, guard, E_SNAP)
    c.key(0.3, P_(guard, {"@root": [0, -1.3, 1.6], "body": [-9, 0, 0]}), E_OUT)
    c.key(0.55, guard, E_IO)
    c.key(0.8, {}, E_IO)

    # disordered: the formation broken -- out of step, looking about, blade and shield low.
    c = clip("disordered", 2.0, loop=True)
    seq = [(0, 0, 0), (0.5, 40, 1), (1.0, -10, -1), (1.5, -38, 1), (2.0, 0, 0)]
    for i, (t, look, step) in enumerate(seq):
        c.key(t, {"head": [-4, look, 4 * step], "body": [4, look * 0.35, 0], "right_leg": [-10 * step, 0, 2], "left_leg": [6 * step, 0, -2],
                  "right_shin": [8 * abs(step), 0, 0], "right_arm": [-10, 0, 10], "left_arm": [-6, 0, -12], "left_forearm": [-10, 0, 0]},
              E_IO if i else None)

    # die (held): the blade drops, he falls to his knees and then forward; the wings fade.
    c = clip("die", 2.0, hold=True)
    c.key(0, {})
    c.key(0.25, {"body": [-14, 0, 0], "head": [-20, 0, 0], "right_arm": [-30, 0, 30], "left_arm": [-20, 0, -30], "@root": [0, 0, 1.5]}, E_SNAP)
    c.key(0.9, {"@root": [0, -5.5, 1], "right_leg": [-10, 0, 0], "left_leg": [-10, 0, 0], "right_shin": [90, 0, 0], "left_shin": [90, 0, 0],
                "body": [20, 0, 0], "head": [24, 0, 0], "right_arm": [10, 0, 10], "left_arm": [10, 0, -10], "%wings_folded": [0.7, 0.7, 0.7]}, E_IN)
    c.key(1.6, {"@root": [0, -4.5, -4], "root": [70, 0, 0], "right_leg": [-60, 0, 0], "left_leg": [-60, 0, 0], "right_shin": [100, 0, 0],
                "left_shin": [100, 0, 0], "body": [6, 0, 0], "head": [-20, 0, 10], "right_arm": [-150, 0, 20], "left_arm": [-150, 0, -10],
                "%wings_folded": [0.05, 0.05, 0.05]}, E_IN)
    c.key(2.0, {"@root": [0, -4.5, -4], "root": [72, 0, 0], "right_leg": [-62, 0, 0], "left_leg": [-60, 0, 0], "right_shin": [100, 0, 0],
                "left_shin": [100, 0, 0], "body": [6, 0, 0], "head": [-22, 0, 12], "right_arm": [-152, 0, 20], "left_arm": [-150, 0, -10],
                "%wings_folded": [0.01, 0.01, 0.01]}, E_OUT)

    for cl in clips:
        cl.done()
    return f


def open_pose(m):
    return delta_pose(m, {f"wing_open_{sd}": mirror_rot((0, 6, 22), s) for sd, s in (("r", -1), ("l", 1))})


def textures():
    out = {}
    for v in VARIANTS:
        m = rig(v)
        out[v] = m.build(gutter=1, seed=9000)
    return m, out


def generate():
    m, tex = textures()
    m.rig.write(GEO + "host_angel.geo.json")
    for v, (t, g) in tex.items():
        save(t, "entity", f"host_angel_{v}")
    save(tex["0"][1], "entity", "host_angel_glowmask")
    anims().write(ANIM + "host_angel.animation.json")


if __name__ == "__main__":
    generate()
