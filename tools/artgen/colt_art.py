"""The Colt: a Colt Paterson of 1836 as a GeckoLib item, with its animations, and its HUD art.

Built at four units to the pixel (the item model scales it by a quarter), so a hammer spur or a
front sight can be a quarter-pixel thing. Origin at the gun hand's grip; the barrel points NORTH
(-Z), the butt hangs down, Bedrock +X is the gun's left side (the side the shooter sees), where
NON TIMEBO MALA is engraved along the barrel.

Bones the Java renderer poses itself and no animation may key: cylinder, chamber_*, round_*,
muzzle_flash (see reward/colt/ColtAnimations.PROCEDURAL).
"""

import math

import palette as P
import preview3d
from animkit import AnimFile
from common import ASSETS, save
from geomodel import Rig, box
from paint import paint_cube
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/item/"
ANIM = ASSETS + "/animations/item/"
NAME = "the_colt"
CAPACITY = 5

BLUED = Ramp("#0a0e16", "#18202e", "#243248", "#34496a", "#4d6a92", "#86a3c8")
BRIGHT = Ramp("#2b2f36", "#4a5059", "#6f7782", "#959daa", "#bfc6d0", "#e8edf2")
WALNUT = Ramp("#1d0f07", "#34190b", "#4d2711", "#6a3818", "#8a4d22", "#a8662f")
LEAD = Ramp("#2a2c31", "#4c4f57", "#6e727c", "#8f949e", "#b3b8c1", "#d9dde3")
GOLD = P.GOLD
GERMAN_SILVER = hexc("#d8dccf")
BORE = hexc("#050608")
ENGRAVE_DARK = hexc("#070a10")
FLASH = [hexc("#fffbe6"), hexc("#ffe58a"), hexc("#ffb43a"), hexc("#ff7a1a")]

# Geometry, in units (a quarter pixel).
CYL_Y, CYL_R, CHAMBER_R = 9.0, 4, 2.5
CYL_FRONT, CYL_BACK = -8.0, 1.0
BARREL_Y = CYL_Y + CHAMBER_R
MUZZLE_Z = -33.5
# Where the gun hand closes on the grip (before the rig is re-centred on it).
HAND = (0, -3, 2)


# --- materials ---------------------------------------------------------------------------

def blued(seed, base=2.2):
    """Deep blued steel: brushed lengthwise, a cold sheen along the top edges."""
    def m(face, x, y, w, h):
        n = fbm(x * 2 + seed, y * 7, seed, 64, 64, 2, 7.0)
        v = base + (n - 0.5) * 0.9
        if face == "up":
            v += 0.9
        elif face in ("north", "south", "east", "west") and y == 0:
            v += 1.5
        elif face in ("north", "south", "east", "west") and y == h - 1:
            v -= 0.7
        elif face == "down":
            v -= 0.6
        return BLUED[v]
    return m


def case_hardened(seed):
    """Hammer and trigger: case-hardened, mottled straw, blue and violet."""
    tints = [hexc("#4a3a62"), hexc("#8a7448"), hexc("#3d5578"), hexc("#a08a5a"), hexc("#5a4a70")]

    def m(face, x, y, w, h):
        n = fbm(x * 5, y * 5, seed, 32, 32, 3, 3.0)
        c = tints[int(n * len(tints)) % len(tints)]
        return mix(c, BRIGHT[4], 0.25 if face == "up" or y == 0 else 0.0)
    return m


def bright(seed):
    def m(face, x, y, w, h):
        n = fbm(x * 3, y * 3, seed, 32, 32, 2, 4.0)
        return BRIGHT[2.6 + (n - 0.5) * 1.4 + (1 if face == "up" else 0)]
    return m


def walnut(seed):
    """Oiled walnut: long wavy grain down the grip, a polished highlight."""
    def m(face, x, y, w, h):
        warp = fbm(x * 2, y * 2, seed, 48, 48, 3, 5.0)
        g = math.sin((x * 0.9 + warp * 9.0)) * 0.5 + 0.5
        v = 2.0 + g * 1.6 + (fbm(x * 9, y * 2, seed + 1, 48, 48, 2, 3.0) - 0.5) * 0.8
        if face in ("east", "west") and 1 <= x <= 2:
            v += 0.8
        if face == "down":
            v -= 0.8
        return WALNUT[v]
    return m


def solid(c):
    return lambda face, x, y, w, h: c


def none(face, x, y, w, h):
    return None


def cylinder_face(origin, size, seed):
    """Blued cylinder with the faint roll engraving round its middle and five mouths in front."""
    x0, y0, z0 = origin
    w, h, d = size
    base = blued(seed, 2.9)
    centres = [(CHAMBER_R * math.sin(math.radians(72 * j)), CYL_Y + CHAMBER_R * math.cos(math.radians(72 * j)))
               for j in range(CAPACITY)]

    def m(face, x, y, wt, ht):
        if face == "north":
            px, py = x0 + x + 0.5, y0 + h - y - 0.5
            if any(math.hypot(px + cx, py - cy) < 1.2 for cx, cy in centres):
                return BORE
        if face in ("east", "west"):
            # Bright bands at the cylinder's ends, and between them the faint roll-engraved scene.
            if x in (1, wt - 2):
                return BLUED[4]
            if 2 < x < wt - 3 and 1 <= y < ht - 1 and (x * 5 + y * 3) % 11 == 0:
                return BLUED[3]
        return base(face, x, y, wt, ht)
    return m


def barrel_face(origin, size, seed):
    x0, y0, z0 = origin
    w, h, d = size
    base = blued(seed, 2.5)

    def m(face, x, y, wt, ht):
        if face == "north":
            px, py = x0 + x + 0.5, y0 + h - y - 0.5
            if math.hypot(px, py - BARREL_Y) < 1.0:
                return BORE
        return base(face, x, y, wt, ht)
    return m


# The engraving's micro-font: three or more texels wide, five tall.
FONT = {
    "N": ["X..X", "XX.X", "X.XX", "X..X", "X..X"],
    "O": ["XXX", "X.X", "X.X", "X.X", "XXX"],
    "T": ["XXX", ".X.", ".X.", ".X.", ".X."],
    "I": ["XXX", ".X.", ".X.", ".X.", "XXX"],
    "M": ["X...X", "XX.XX", "X.X.X", "X...X", "X...X"],
    "E": ["XXX", "X..", "XX.", "X..", "XXX"],
    "B": ["XX.", "X.X", "XX.", "X.X", "XX."],
    "A": [".X.", "X.X", "XXX", "X.X", "X.X"],
    "L": ["X..", "X..", "X..", "X..", "XXX"],
    " ": ["..", "..", "..", "..", ".."],
}
MOTTO = "NON TIMEBO MALA"


def motto_mask(w, h):
    """Texels of the motto, centred on a w x h plate."""
    cols = []
    for i, ch in enumerate(MOTTO):
        glyph = FONT[ch]
        for gx in range(len(glyph[0])):
            cols.append([glyph[gy][gx] == "X" for gy in range(5)])
        if i < len(MOTTO) - 1:
            cols.append([False] * 5)
    total = len(cols)
    if total > w or h < 5:
        raise ValueError(f"motto {total} texels wide does not fit a {w}x{h} plate")
    ox, oy = (w - total) // 2, (h - 5) // 2
    out = set()
    for x, col in enumerate(cols):
        for y in range(5):
            if col[y]:
                # The plate's texels run from the muzzle back (u grows toward the frame as seen
                # from outside), so the text is laid in mirrored to read the right way.
                out.add((w - 1 - (ox + x), oy + y))
    return out


# --- the rig -------------------------------------------------------------------------------

class Colt:
    def __init__(self):
        self.rig = Rig(NAME, 256, 128)
        self.paints = []   # (cube, base material, glow material, engraving material)

    def cube(self, bone, origin, size, material, glow=None, engraving=None, **kw):
        c = box(bone, origin, size, **kw)
        self.paints.append((c, material, glow, engraving))
        return c


def build():
    g = Colt()
    r = g.rig
    root = r.bone("root", (0, -3, 3))
    spin = r.bone("spin", (0, 1, -2), "root")

    # Frame: open-topped (no top strap over the cylinder), as the Paterson was.
    frame = r.bone("frame", (0, 4, 0), "spin")
    g.cube(frame, (-3, 2.5, -8), (6, 3, 9), blued(11))                 # under the cylinder
    g.cube(frame, (-1.5, 3, -10), (3, 4, 2), blued(12))                # up to the barrel lug
    g.cube(frame, (-4, 3.5, CYL_BACK), (8, 9, 2), blued(13, 2.0))      # recoil shield
    g.cube(frame, (-2, 4, 3), (4, 6, 4), blued(14))                    # hammer housing
    g.cube(frame, (-2, 4, 7), (4, 3, 2), blued(15))                    # sloping into the grip
    g.cube(frame, (-2, 0.5, -4), (4, 3, 6), blued(16))                 # belly over the trigger
    g.cube(frame, (-1, 8, -8.5), (2, 2, 1), bright(17))                # cylinder arbor

    # Barrel: long, slender and octagonal, from two crossed slabs; the lug, its wedge, the sight.
    barrel = r.bone("barrel", (0, BARREL_Y, -8.5), "frame")
    a_origin, a_size = (-2, BARREL_Y - 1, MUZZLE_Z), (4, 2, 25)
    b_origin, b_size = (-1, BARREL_Y - 2, MUZZLE_Z), (2, 4, 25)
    g.cube(barrel, a_origin, a_size, barrel_face(a_origin, a_size, 21))
    g.cube(barrel, b_origin, b_size, barrel_face(b_origin, b_size, 22))
    g.cube(barrel, (-1, 6.5, -16), (2, 3, 6), blued(23))               # lug
    g.cube(barrel, (-2.5, 7.5, -13.5), (5, 1, 1), bright(24))          # wedge
    g.cube(barrel, (-0.5, BARREL_Y + 2, MUZZLE_Z + 0.5), (1, 1, 2), solid(GERMAN_SILVER))   # front sight
    g.cube(barrel, (-2.5, BARREL_Y - 1, MUZZLE_Z - 0.5), (5, 2, 1), blued(25, 2.8))         # muzzle crown
    g.cube(barrel, (-1, BARREL_Y - 2.5, MUZZLE_Z - 0.5), (2, 5, 1), blued(26, 2.8))

    # NON TIMEBO MALA along the left flat, read from the frame toward the muzzle.
    engraving = r.bone("engraving", (0, BARREL_Y, -20), "barrel")
    plate_len, plate_h, density = 22, 2, 4
    mask = motto_mask(plate_len * density, plate_h * density)

    def plate_base(face, x, y, w, h):
        return ENGRAVE_DARK if (x, y) in mask else None

    def plate_gold(face, x, y, w, h):
        if (x, y) in mask:
            return GOLD[5] if (x + y) % 3 else GOLD[4]
        return None
    g.cube(engraving, (2.01, BARREL_Y - 1, -32), (0, plate_h, plate_len), plate_base, engraving=plate_gold,
           faces=("east",), density=density)

    # Loading lever under the barrel, hinged at its rear.
    lever = r.bone("loading_lever", (0, 8.5, -16), "barrel")
    g.cube(lever, (-0.5, 8, -31), (1, 1, 15), bright(31))
    g.cube(lever, (-1, 7, -18), (2, 2, 2), blued(32))
    g.cube(lever, (-0.5, 7.5, -31.5), (1, 1, 1), bright(33))

    # The cylinder: five chambers, turned by the renderer a fifth at a time.
    mount = r.bone("cylinder_mount", (0, CYL_Y, CYL_FRONT), "frame")
    cyl = r.bone("cylinder", (0, CYL_Y, (CYL_FRONT + CYL_BACK) / 2), "cylinder_mount")
    length = int(CYL_BACK - CYL_FRONT)
    for i, (o, s) in enumerate([((-CYL_R, CYL_Y - 3, CYL_FRONT), (2 * CYL_R, 6, length)),
                                ((-3, CYL_Y - CYL_R, CYL_FRONT), (6, 2 * CYL_R, length))]):
        g.cube(cyl, o, s, cylinder_face(o, s, 41 + i))
    for j in range(CAPACITY):
        a = math.radians(72 * j)
        cx, cy = -CHAMBER_R * math.sin(a), CYL_Y + CHAMBER_R * math.cos(a)
        r.bone(f"chamber_{j}", (cx, cy, CYL_FRONT), "cylinder")
        rnd = r.bone(f"round_{j}", (cx, cy, CYL_FRONT), f"chamber_{j}")

        def lead(face, x, y, w, h):
            return GOLD[4] if face == "north" and (x, y) in ((0, 0), (1, 1)) else LEAD[3 + (x + y) % 2]

        def holy(face, x, y, w, h):
            return hexc("#fff4c8") if face == "north" and (x, y) == (0, 0) else None
        g.cube(rnd, (cx - 1, cy - 1, CYL_FRONT - 0.3), (2, 2, 1), lead, holy)

    # Hammer and the folding trigger.
    hammer = r.bone("hammer", (0, 8.5, 5), "frame")
    g.cube(hammer, (-1, 8, 4), (2, 5, 2), case_hardened(51))
    g.cube(hammer, (-1, 12, 4.5), (2, 2, 4), case_hardened(52))
    g.cube(hammer, (-1.5, 13.5, 6.5), (3, 1, 2), case_hardened(53))   # chequered spur
    g.cube(hammer, (-0.5, 11.5, 3), (1, 1, 1), bright(54))            # nose
    trigger = r.bone("trigger", (0, 1, -1.5), "frame")
    g.cube(trigger, (-0.5, -3, -2), (1, 4, 1), case_hardened(55))
    g.cube(trigger, (-0.5, -3.5, -1.5), (1, 1, 1), case_hardened(56))

    # The plow-handle grip: steel backstrap, walnut, a flared butt.
    grip = r.bone("grip", (0, 3.5, 4), "spin", rotation=(14, 0, 0))
    g.cube(grip, (-1, -8.5, 4.5), (2, 12, 2), blued(61))
    g.cube(grip, (-2.5, -8, -0.5), (5, 11, 5), walnut(62))
    g.cube(grip, (-2.5, -10.5, -1), (5, 3, 7), walnut(63))
    g.cube(grip, (-1.5, -11, -0.5), (3, 1, 6), blued(64))
    g.cube(grip, (-0.5, -3, -0.9), (1, 1, 1), bright(65))             # grip screw

    # Fire at the muzzle: a white core and crossed planes, shown only for a moment after a shot.
    flash = r.bone("muzzle_flash", (0, BARREL_Y, MUZZLE_Z), "barrel")

    def flame(face, x, y, w, h):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        dx, dy = (x - cx) / max(1, cx), (y - cy) / max(1, cy)
        rr = math.hypot(dx, dy)
        # A four-pointed star: rays along the axes, ragged at the tips.
        star = min(abs(dx), abs(dy)) < 0.22 * (1 - rr) + 0.05 or rr < 0.45
        if not star or rr > 1:
            return None
        return FLASH[min(3, int(rr * 4))]
    g.cube(flash, (-2, BARREL_Y - 2, MUZZLE_Z - 6), (4, 4, 6), flame, flame)
    g.cube(flash, (-6, BARREL_Y - 6, MUZZLE_Z - 3), (12, 12, 0), flame, flame, faces=("north", "south"))
    g.cube(flash, (0, BARREL_Y - 6, MUZZLE_Z - 14), (0, 12, 14), flame, flame, faces=("east", "west"))
    g.cube(flash, (-6, BARREL_Y, MUZZLE_Z - 14), (12, 0, 14), flame, flame, faces=("up", "down"))

    # Anchors where the Java renderer draws the shooter's hands (first person).
    r.bone("hand_r", HAND, "root")
    r.bone("hand_l", (4, 3, -5), "root")
    loose = r.bone("loose_round", (4, 4, -6), "hand_l")
    g.cube(loose, (3, 3, -7), (2, 2, 2), lambda f, x, y, w, h: LEAD[3 + (x + y) % 2],
           lambda f, x, y, w, h: hexc("#fff4c8") if (x, y) == (0, 0) else None)

    # The gun hand's grip at the origin: item transforms then pivot at the hand.
    r.translate(-HAND[0], -HAND[1], -HAND[2])
    r.pack()
    tex, glow, engr = Tex(r.tex_w, r.tex_h, 3), Tex(r.tex_w, r.tex_h, 4), Tex(r.tex_w, r.tex_h, 5)
    for c, material, glow_m, engr_m in g.paints:
        paint_cube(tex, c, material)
        if glow_m:
            paint_cube(glow, c, glow_m, shading=False)
            paint_cube(tex, c, lambda f, x, y, w, h, gl=glow_m, m=material: gl(f, x, y, w, h) or m(f, x, y, w, h))
        if engr_m:
            paint_cube(engr, c, engr_m, shading=False)
    return r, tex, glow, engr


# --- animations ----------------------------------------------------------------------------

COCKED = {"hammer": [-40, 0, 0], "trigger": [0, 0, 0]}
UNCOCKED = {"hammer": [0, 0, 0], "trigger": [-85, 0, 0]}
POSED = ["root", "spin", "hammer", "trigger", "loading_lever", "cylinder_mount", "hand_l", "hand_r"]


def settle(anim, cocked):
    """Holds every posed bone the clip leaves alone at its rest, so nothing drifts mid-clip."""
    rest = COCKED if cocked else UNCOCKED
    for bone in POSED:
        chans = anim.bones.get(bone, {})
        if "rotation" not in chans:
            anim.const_rot(bone, rest.get(bone, [0, 0, 0]))
        if bone in ("root", "hand_l", "hand_r", "cylinder_mount") and "position" not in chans:
            anim.pos(bone, (0, [0, 0, 0]), (anim.length, [0, 0, 0]))


def reload_clip(f, n):
    """Half-cock, drop the lever, seat n rounds one by one (the left hand brings each), close, cock."""
    intro, per, seat, outro = 10, 8, 6, 8          # ticks, as reward/colt/ColtReload
    length = (intro + per * n + outro) / 20
    a = f.new(f"animation.{NAME}.reload_{n}", length)
    s = 1 / 20
    end_hold = (intro + per * n) * s
    tilt = [-22, 8, -35]
    a.rot("root", (0, [0, 0, 0]), (0.35, tilt, "easeOutCubic"), (end_hold, tilt), (length, [0, 0, 0], "easeInOutCubic"))
    a.pos("root", (0, [0, 0, 0]), (0.35, [-3, 4, -2], "easeOutCubic"), (end_hold, [-3, 4, -2]), (length, [0, 0, 0], "easeInOutCubic"))
    a.rot("hammer", (0, [-40, 0, 0]), (0.15, [-20, 0, 0]), (end_hold, [-20, 0, 0]), (length - 0.1, [-48, 0, 0]), (length, [-40, 0, 0]))
    a.rot("trigger", (0, [0, 0, 0]), (0.15, [-85, 0, 0]), (end_hold, [-85, 0, 0]), (length - 0.1, [0, 0, 0]), (length, [0, 0, 0]))
    lever = [(0, [0, 0, 0]), (0.2, [0, 0, 0]), (0.45, [55, 0, 0], "easeOutBack")]
    hand = [(0, [-6, -10, 8]), (0.3, [-2, -6, 4], "easeOutCubic")]
    for k in range(n):
        t0 = (intro + per * k) * s
        t_in = t0 + 0.25
        t_seat = (intro + per * k + seat) * s
        hand += [(t0 + 0.02, [-6, -9, 6]), (t_in, [0, 0, 0], "easeOutCubic"), (min(t_seat + 0.05, t0 + per * s), [-1, -2, 1])]
        lever += [(max(t_in, lever[-1][0] + 0.01), [55, 0, 0]), (t_seat, [30, 0, 0], "easeInQuad"), (t0 + per * s - 0.01, [55, 0, 0])]
    lever += [(end_hold + 0.05, [55, 0, 0]), (end_hold + 0.3, [0, 0, 0], "easeInOutCubic"), (length, [0, 0, 0])]
    hand += [(end_hold + 0.25, [-6, -10, 8], "easeInCubic"), (length, [-6, -10, 8])]
    a.rot("loading_lever", *lever)
    a.pos("hand_l", *_monotone(hand))
    a.rot("hand_l", (0, [30, 0, 0]), (0.3, [0, 0, 0]), (end_hold + 0.25, [0, 0, 0]), (length, [30, 0, 0]))
    settle(a, True)


def _monotone(keys):
    """Drops keys that would go back in time (when rounds come quickly)."""
    out, last = [], -1
    for k in keys:
        if k[0] > last + 1e-4:
            out.append(k)
            last = k[0]
    return out


def animations():
    f = AnimFile()
    idle = f.new(f"animation.{NAME}.idle_cocked", 2.0, loop=True)
    idle.rot("root", (0, [0, 0, 0]), (1.0, [0.6, 0, 0], "easeInOutSine"), (2.0, [0, 0, 0], "easeInOutSine"))
    settle(idle, True)
    idle = f.new(f"animation.{NAME}.idle_uncocked", 2.0, loop=True)
    idle.rot("root", (0, [0, 0, 0]), (1.0, [0.6, 0, 0], "easeInOutSine"), (2.0, [0, 0, 0], "easeInOutSine"))
    settle(idle, False)

    for name, recock in (("fire", True), ("fire_last", False)):
        a = f.new(f"animation.{NAME}.{name}", 0.6)
        a.rot("hammer", (0, [-40, 0, 0]), (0.03, [0, 0, 0], "easeInQuad"), *(
            [(0.36, [0, 0, 0]), (0.48, [-46, 0, 0], "easeOutCubic"), (0.6, [-40, 0, 0])] if recock else [(0.6, [0, 0, 0])]))
        a.rot("trigger", (0, [0, 0, 0]), (0.03, [9, 0, 0]), (0.12, [-85, 0, 0], "easeOutCubic"), *(
            [(0.42, [-85, 0, 0]), (0.5, [0, 0, 0], "easeOutBack"), (0.6, [0, 0, 0])] if recock else [(0.6, [-85, 0, 0])]))
        a.rot("root", (0, [0, 0, 0]), (0.05, [-28, 0, -4], "easeOutExpo"), (0.14, [-22, 0, -3]),
              (0.38, [2, 0, 0.5], "easeInOutCubic"), (0.6, [0, 0, 0], "easeOutCubic"))
        a.pos("root", (0, [0, 0, 0]), (0.05, [0, 2, 6], "easeOutExpo"), (0.38, [0, 0, -0.5], "easeInOutCubic"), (0.6, [0, 0, 0]))
        settle(a, recock)

    a = f.new(f"animation.{NAME}.dry_fire", 0.35)
    a.rot("hammer", (0, [0, 0, 0]), (0.12, [-40, 0, 0], "easeOutCubic"), (0.16, [0, 0, 0], "easeInQuad"), (0.35, [0, 0, 0]))
    a.rot("trigger", (0, [-85, 0, 0]), (0.1, [0, 0, 0]), (0.16, [9, 0, 0]), (0.26, [-85, 0, 0]), (0.35, [-85, 0, 0]))
    a.rot("root", (0, [0, 0, 0]), (0.16, [0, 0, 0]), (0.19, [-3, 0, 0]), (0.35, [0, 0, 0], "easeOutCubic"))
    settle(a, False)

    for n in range(1, CAPACITY + 1):
        reload_clip(f, n)

    a = f.new(f"animation.{NAME}.reload_abort", 0.3)
    a.rot("root", (0, [-22, 8, -35]), (0.3, [0, 0, 0], "easeInOutCubic"))
    a.pos("root", (0, [-3, 4, -2]), (0.3, [0, 0, 0], "easeInOutCubic"))
    a.rot("loading_lever", (0, [55, 0, 0]), (0.2, [0, 0, 0], "easeInCubic"), (0.3, [0, 0, 0]))
    a.rot("hammer", (0, [-20, 0, 0]), (0.25, [-46, 0, 0]), (0.3, [-40, 0, 0]))
    a.rot("trigger", (0, [-85, 0, 0]), (0.25, [0, 0, 0], "easeOutBack"), (0.3, [0, 0, 0]))
    a.pos("hand_l", (0, [0, 0, 0]), (0.2, [-6, -10, 8], "easeInCubic"), (0.3, [-6, -10, 8]))
    settle(a, True)

    for name, cocked in (("inspect", True), ("inspect_empty", False)):
        a = f.new(f"animation.{NAME}.{name}", 2.4)
        show = [-12, -18, -70]
        a.rot("root", (0, [0, 0, 0]), (0.4, show, "easeOutCubic"), (0.8, [-8, -8, -66], "easeInOutSine"),
              (1.15, [-12, -22, -72], "easeInOutSine"), (1.25, [-4, 0, -20], "easeInOutCubic"), (2.05, [-4, 0, -20]),
              (2.2, [3, 0, 2], "easeOutCubic"), (2.4, [0, 0, 0], "easeOutBack"))
        a.pos("root", (0, [0, 0, 0]), (0.4, [-6, 5, -3], "easeOutCubic"), (1.15, [-6, 5, -3]), (1.25, [-2, 2, 0]),
              (2.05, [-2, 2, 0]), (2.4, [0, 0, 0], "easeOutCubic"))
        a.rot("spin", (0, [0, 0, 0]), (1.25, [0, 0, 0]), (2.0, [-720, 0, 0], "easeInOutCubic"),
              (2.12, [-728, 0, 0], "easeOutCubic"), (2.24, [-720, 0, 0], "easeInOutSine"), (2.4, [-720, 0, 0]))
        rest = COCKED if cocked else UNCOCKED
        a.const_rot("hammer", rest["hammer"])
        a.const_rot("trigger", rest["trigger"])
        a.pos("hand_l", (0, [-6, -10, 8]), (0.4, [1, -3, 2], "easeOutCubic"), (1.15, [1, -3, 2]), (1.3, [-6, -10, 8], "easeInCubic"),
              (2.4, [-6, -10, 8]))
        settle(a, cocked)
    return f


# --- HUD and screen ------------------------------------------------------------------------

def hud():
    """64x32 atlas: the cylinder's face (28x28), a loaded chamber, an empty one, the hammer mark."""
    t = Tex(64, 32, 7)
    c = 13.5
    for y in range(28):
        for x in range(28):
            d = math.hypot(x - c, y - c)
            if d > 13.6:
                continue
            ang = math.atan2(y - c, x - c)
            # Fluted octagon edge, blued body, a little light from the top left.
            edge = 12.4 + 0.7 * abs(math.cos(ang * 4))
            if d > edge:
                continue
            light = 0.5 - 0.5 * math.sin(ang + 0.8)
            v = 2.0 + light * 1.4 + (fbm(x, y, 70, 28, 28, 2, 4.0) - 0.5) * 0.5
            if d > edge - 1.2:
                v = 4.3 if light > 0.5 else 1.2
            t.set(x, y, BLUED[v])
    for y in range(28):
        for x in range(28):
            if math.hypot(x - c, y - c) < 2.0:
                t.set(x, y, BRIGHT[3])
    for x0, kind in ((32, "round"), (40, "empty")):
        for y in range(6):
            for x in range(6):
                d = math.hypot(x - 2.5, y - 2.5)
                if d > 3.0:
                    continue
                if kind == "empty":
                    t.set(x0 + x, y, BORE if d < 2.2 else BLUED[1])
                else:
                    col = GOLD[5] if d < 1.0 else LEAD[4] if d < 2.0 else GOLD[2]
                    t.set(x0 + x, y, col)
    for y, row in enumerate(["XXXXX", ".XXX.", "..X..", "....."]):
        for x, ch in enumerate(row):
            if ch == "X":
                t.set(48 + x, y, GOLD[4] if y == 0 else GOLD[3])
    return t


def flash_vignette():
    """White at the edges fading clear toward the middle; tinted warm in game."""
    n = 128
    t = Tex(n, n, 8)
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            d = math.sqrt(dx * dx * 0.8 + dy * dy)
            a = max(0.0, min(1.0, (d - 0.55) / 0.6))
            t.set(x, y, (255, 255, 255, int(255 * a * a)))
    return t


# --- output --------------------------------------------------------------------------------

def generate():
    rig, tex, glow, engr = build()
    rig.write(GEO + NAME + ".geo.json")
    animations().write(ANIM + NAME + ".animation.json")
    save(tex, "item", NAME)
    save(glow, "item", NAME + "_glowmask")
    save(engr, "item", NAME + "_engraving")
    hidden = ("muzzle_flash", "loose_round", "hand_l")
    save(preview3d.icon(rig, tex, angle_deg=0, view="side", hidden=hidden), "item", NAME + "_icon")
    save(hud(), "gui", "colt_hud")
    save(flash_vignette(), "misc", "colt_flash")


def preview(path, anim=None, t=0.0, views=("side", "three_quarter", "front", "back"), scale=6):
    """Debug render of the rig (optionally posed at time t of an animation) for review."""
    rig, tex, glow, engr = build()
    pose = preview3d.pose_from(animations_json(), f"animation.{NAME}.{anim}", t) if anim else None
    preview3d.render(rig, tex, path, pose=pose, hidden=("muzzle_flash", "loose_round", "hand_l") if not anim else ("muzzle_flash",), views=views, scale=scale)


def animations_json():
    f = animations()
    return {"animations": {a.name: a.to_json() for a in f.anims}}


if __name__ == "__main__":
    generate()
