"""Abaddon's retinue (v0.21) and the Knights' Cleaver: two GeckoLib mobs and a GeckoLib melee weapon (names in AbaddonAssets).

  abaddon_knight  a Knight of Hell's soldier, a head taller than a man: plate blackened as if pulled from a fire, the
                  seams and the cracks in it still glowing (glowmask red), a great helm with a T slit (two red points in it)
                  and a pair of black horns sweeping out and up, spiked pauldrons, a mail skirt under a tattered crimson
                  tabard with the Knights' sigil, a ragged half-cape; the Knights' Cleaver in his right hand (`blade`) and a
                  black heater shield with the sigil on his left forearm (`shield`). `possessed` loops while Abaddon is in
                  him (the renderer adds her smoke and the inner light).
  abaddon_herald  a lean, tall demon in a black cassock and a deep cowl, the face grey and gaunt under it, black eyes with a
                  red point; a crimson stole stitched with sigils (faint glow), a rope cincture; long grey fingers with
                  black nails; a great tarnished bronze handbell in his left hand (`bell` -- `clapper`) that he rings when
                  he marks a target.
  knights_cleaver the Knights' Cleaver (Tier IV melee): a massive black cleaver-sword, upright and centred, a broad blade
                  clipped square at the point with a ring through the spine, red veins running through the steel (a shell,
                  `veins`, that the clips pulse), a horned guard with a red eye, a grip bound in black leather and red cord,
                  a caged ruby pommel; and its 16-px inventory icon.

Skeletons (Bedrock px; +X is the entity's LEFT; models face north) -- cain_art.man_skeleton plus:
  knight: head -- horn_r, horn_l, visor;  body -- tabard_front, tabard_back, cape;  right_arm -- pauldron_r;
          left_arm -- pauldron_l;  right_hand -- blade;  left_forearm -- shield
  herald: head -- hood -- hood_tip;  body -- stole, cord;  skirt -- skirt_lower (from the hips);  right_forearm -- sleeve_r;
          left_forearm -- sleeve_l;  left_hand -- bell -- clapper
"""

import math
import random

from animkit import AnimFile
from cain_art import man_hands, man_skeleton, man_walk
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, art, save
from legacy_art import SIDES, Build, done, ears_nose, fix, head_cube, reach as _reach
from michael_art import (ANIM, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, GEO, P_, Clip, Plate, aim, aim2, crack_texels, to_preview, tone,
                         world_point, write_compact)
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade
from witchkit import (CLAW_L, CLAW_R, CURL_L, CURL_R, FIST_L, FIST_R, GRACE_L, GRACE_R, OPEN_L, OPEN_R, POINT_R, bell as bell_tiers,
                      glow_k, gown_follow, slim_hands)
import horsemen_art as H

KNIGHT, HERALD, CLEAVER = "abaddon_knight", "abaddon_herald", "knights_cleaver"

BLACK_STEEL = Ramp("#07070a", "#0f0f13", "#19191e", "#25252c", "#34343d", "#4a4a55")
HELL_IRON = Ramp("#1c0605", "#330b08", "#4f120c", "#6b1b12", "#89281a", "#a83a24")
TRIM = Ramp("#120606", "#1f0a09", "#2e100d", "#3f1712", "#521f17", "#68291d")
EMBER = hexc("#ff4a1c")
EMBER_HOT = hexc("#ffb060")
EMBER_DIM = hexc("#7a1206")
CRIMSON = Ramp("#1e0306", "#36060c", "#520b14", "#6e111d", "#8a1a28", "#a82737")
MAIL = Ramp("#0a0a0c", "#141418", "#202026", "#2d2d35", "#3c3c46", "#50505c")
HORN = Ramp("#0a0807", "#15110e", "#221b16", "#30261f", "#41342a", "#574639")
LEATHER = Ramp("#0c0808", "#160f0f", "#211717", "#2d2020", "#3b2a2a", "#4c3737")
RUBY = Ramp("#2a0206", "#55050d", "#850b17", "#b81627", "#e0384a", "#ff8a92")


# =====================================================================================================
# Hell-forged plate: black steel, dark red iron trim, cracks of ember along the seams
# =====================================================================================================

class HellPlate(Plate):
    """michael_art.Plate in blackened steel with hell-iron trim; every plate cracked, the cracks glowing ember."""

    def __init__(self, seed, d=2, motif="plain", scallop=False, base=2.9, cracked=True):
        super().__init__(seed, d, ramp=BLACK_STEEL, trim=TRIM, engrave=False, rivets=True, base=base, band=True, glow=0.0,
                         motif=motif, scallop=scallop)
        self.cracked = cracked

    def kind(self, face, x, y, w, h):
        if self.cracked and face not in ("up", "down") and h01("cracked", self.seed, face) < 0.4:
            cr = crack_texels(self.seed * 7 + len(face) * 31 + w * 3 + h, w, h, self.d)
            if (x, y) in cr:
                return "crack"
            if (x - 1, y) in cr or (x + 1, y) in cr or (x, y - 1) in cr or (x, y + 1) in cr:
                return "crack_edge"
        return super().kind(face, x, y, w, h)

    def mat(self, face, x, y, w, h):
        k = self.kind(face, x, y, w, h)
        if k == "crack":
            return hexc("#9a2a12")
        if k == "crack_edge":
            return hexc("#1e0604")
        if k in ("glyph", "glyph_faint"):
            return HELL_IRON[1]
        return super().mat(face, x, y, w, h)

    def glow(self, face, x, y, w, h, c):
        k = self.kind(face, x, y, w, h)
        if k == "crack":
            return EMBER_HOT if h01("ch", self.seed, x, y) < 0.25 else EMBER
        if k == "crack_edge":
            return EMBER_DIM
        if k in ("gold", "gold_hi", "rivet"):
            return shade(c, 0.12)
        return None


def ember_glow(f, x, y, w, h, c):
    return c


# =====================================================================================================
# The Knights' Cleaver (shared by the item and the knight's hand)
# =====================================================================================================

def _veins(seed, w, h):
    """Branching red veins over a w x h face (texel set)."""
    rng = random.Random(seed)
    pts = set()
    for _ in range(max(2, (w * h) // 180)):
        x, y = rng.randrange(w), rng.randrange(h)
        dx, dy = rng.choice(((0, 1), (0, -1), (1, 1), (-1, 1)))
        for _ in range(rng.randint(h // 4, h // 2 + 2)):
            if not (0 <= x < w and 0 <= y < h):
                break
            pts.add((x, y))
            if rng.random() < 0.4:
                x += rng.choice((-1, 1))
            y += dy if dy else rng.choice((-1, 1))
            if rng.random() < 0.08:
                bx, by = x, y
                for _ in range(rng.randint(2, 5)):
                    bx += rng.choice((-1, 1))
                    by += 1
                    if 0 <= bx < w and 0 <= by < h:
                        pts.add((bx, by))
    return pts


def cleaver_parts(m, bone, veins_bone, o=(0.0, 0.0, 0.0), k=1.0, density=4):
    """The cleaver upright with its pommel's foot at `o`: pommel 0-1.6, grip 1.6-7, guard 7-8.4, blade 8.4-27.6 (px x k).
    Edge on the -X side, spine on +X. Returns the grip's centre."""
    ox, oy, oz = o

    def at(x, y, z):
        return (ox + x * k, oy + y * k, oz + z * k)

    def cube(b, org, size, mat, glow=None, rot=None, piv=None, tag="cleaver", d=density, faces=None):
        m.cube(b, at(*org), tuple(v * k for v in size), mat, glow, density=d, tag=tag, rotation=rot, pivot=at(*piv) if piv else None,
               faces=faces)
    iron = HellPlate(24101, d=density, cracked=False)
    # The pommel: a black iron cage (two boxes turned) round a ruby.
    for rot in (0, 45):
        cube(bone, (-0.85, 0.0, -0.85), (1.7, 1.6, 1.7), lambda f, x, y, w, h: tone(BLACK_STEEL, 3.6 - 1.6 * y / max(1, h)) if (x + y) % 3 else BLACK_STEEL[1],
             rot=(0, rot, 0), piv=(0, 0.8, 0), tag="pommel")
    cube(bone, (-0.55, 0.25, -0.95), (1.1, 1.1, 1.9), lambda f, x, y, w, h: RUBY[4] if (x + y) % 3 == 0 else RUBY[2], ember_glow, tag="pommel_gem")
    cube(bone, (-0.95, 0.25, -0.55), (1.9, 1.1, 1.1), lambda f, x, y, w, h: RUBY[3] if (x + y) % 2 else RUBY[2], ember_glow, tag="pommel_gem")
    # The grip: black leather, a red cord wound over it.
    def grip(f, x, y, w, h):
        off = {"north": 0, "east": 2, "south": 4, "west": 6}.get(f, 0)
        if (y - x + off) % 6 == 0:
            return CRIMSON[4]
        return tone(LEATHER, 3.2 - 0.9 * ((y - x + off) % 6) / 5)
    cube(bone, (-0.65, 1.6, -0.65), (1.3, 5.4, 1.3), grip, tag="grip")
    # The guard: a heavy iron bar, its ends swept down into horns; an eye of ember in the middle.
    cube(bone, (-3.8, 7.0, -0.85), (7.6, 1.4, 1.7), iron.mat, iron.glow, tag="guard")
    for s in (-1, 1):
        cube(bone, (3.4 * s - 0.6, 5.6, -0.6), (1.2, 2.0, 1.2), lambda f, x, y, w, h: tone(HORN, 3.4 - 1.4 * y / max(1, h)),
             rot=(0, 0, -28 * s), piv=(3.4 * s, 7.4, 0), tag="guard_horn")
        cube(bone, (4.2 * s - 0.35, 4.3, -0.35), (0.7, 1.6, 0.7), lambda f, x, y, w, h: tone(HORN, 3.0 - 1.4 * y / max(1, h)),
             rot=(0, 0, -10 * s), piv=(4.2 * s, 5.8, 0), tag="guard_horn_tip")
    def eye(f, x, y, w, h):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        d = abs(x - cx_) / max(1, w / 2) + abs(y - cy_) / max(1, h / 2) * 1.6
        if d < 0.45:
            return hexc("#1a0302")
        if d < 1.0:
            return EMBER
        return HELL_IRON[2]
    cube(bone, (-1.1, 6.8, -1.0), (2.2, 1.8, 2.0), eye, lambda f, x, y, w, h, c: c if c == EMBER else None, tag="guard_eye")
    # The blade: a narrow ricasso, then the broad cleaver, the spine thick, the clipped point stepped on the edge side.
    rng = random.Random(24102)
    nicks = {rng.randrange(60) for _ in range(9)}

    def flat(f, x, y, w, h):
        if f in ("up", "down"):
            return BLACK_STEEL[3]
        if f in ("east", "west"):
            return BLACK_STEEL[2]
        t = x / max(1, w - 1)
        # The edge (the -X side: the viewer's right on the north face, left on the south) honed paler, with nicks.
        e = (1 - t) if f == "north" else t
        v = 2.4 + 0.9 * math.sin(y * 0.15 + x * 0.3) - 0.5 * y / max(1, h)
        if e > 0.82:
            if y in nicks:
                return BLACK_STEEL[0]
            return tone(Ramp("#2c2c32", "#45454e", "#62626d", "#82828e", "#a6a6b2", "#cfcfd8"), 3.4 + (0.8 if e > 0.92 else 0))
        if 0.74 < e <= 0.82:
            return BLACK_STEEL[1]                  # the bevel line
        return tone(BLACK_STEEL, v)

    def spine(f, x, y, w, h):
        if f == "up":
            return BLACK_STEEL[4]
        v = 3.0 + (0.8 if f in ("east", "west") and x == 1 else 0) - 0.4 * y / max(1, h)
        return tone(BLACK_STEEL, v)
    cube(bone, (-1.5, 8.4, -0.4), (3.2, 2.2, 0.8), lambda f, x, y, w, h: tone(BLACK_STEEL, 2.6 + (0.9 if y == 0 else 0)), tag="ricasso")
    cube(bone, (-2.3, 10.6, -0.3), (4.2, 15.0, 0.6), flat, tag="blade_flat")
    cube(bone, (1.9, 10.2, -0.5), (1.1, 15.4, 1.0), spine, tag="blade_spine")
    cube(bone, (-2.3, 25.6, -0.3), (3.6, 1.2, 0.6), flat, tag="blade_tip")
    cube(bone, (-2.3, 26.8, -0.3), (2.2, 0.9, 0.6), flat, tag="blade_tip")
    cube(bone, (-2.3, 27.7, -0.25), (1.0, 0.8, 0.5), flat, tag="blade_tip")
    cube(bone, (1.0, 25.6, -0.4), (2.0, 0.6, 0.8), spine, rot=(0, 0, 30), piv=(3.0, 25.6, 0), tag="spine_cap")
    # The ring through the spine near the point: an iron ring, a coal at its heart.
    def ring(f, x, y, w, h):
        cx_, cy_ = (w - 1) / 2, (h - 1) / 2
        r = math.hypot(x - cx_, y - cy_)
        if r < min(w, h) * 0.22:
            return hexc("#140202")
        if r < min(w, h) * 0.32:
            return EMBER
        return HELL_IRON[3] if (x + y) % 2 else HELL_IRON[2]
    cube(bone, (1.75, 21.6, -0.55), (1.4, 1.4, 1.1), ring, lambda f, x, y, w, h, c: c if c == EMBER else None, tag="spine_ring")
    # Veins of hell-fire through the steel: a shell just over both flats and the spine's sides, glow only where they run.
    for (org, size, faces, seed) in (((-2.3, 10.6, -0.32), (4.2, 15.0, 0.64), ("north", "south"), 24110),
                                     ((1.9, 10.2, -0.52), (1.1, 15.4, 1.04), ("east",), 24111)):
        pts = {}

        def vm(f, x, y, w, h, seed=seed, pts=pts):
            key = (f, w, h)
            if key not in pts:
                pts[key] = _veins(seed + len(f), w, h)
            if (x, y) in pts[key]:
                return hexc("#c8240e") if (x * 3 + y) % 5 else hexc("#ff7a3a")
            return None
        cube(veins_bone, org, size, vm, lambda f, x, y, w, h, c: shade(c, 1.15), faces=faces, tag="veins")
    return at(0, 4.3, 0)


def cleaver_item_rig():
    m = Model(CLEAVER, 128, 128)
    m.bone("cleaver", (0, 0, 0))
    m.bone("veins", (0, 18.0, 0), "cleaver")
    cleaver_parts(m, "cleaver", "veins")
    lo, hi = m.rig.height_span()
    dy = -(lo + hi) / 2
    m.rig.translate(0, dy, 0)
    for _, c in m.rig.cubes():
        if c.pivot:
            c.pivot = (c.pivot[0], c.pivot[1] + dy, c.pivot[2])
    m.height = hi - lo
    return m


def cleaver_item_anims():
    f = AnimFile()
    a = f.new(f"animation.{CLEAVER}.idle", 3.0, loop=True)
    a.scale("veins", (0, [1, 1, 1]), (1.5, [1.06, 1, 1.25], E_IO), (3.0, [1, 1, 1], E_IO))
    cl = f.new(f"animation.{CLEAVER}.cleave", 0.6)
    cl.scale("veins", (0, [1, 1, 1]), (0.08, [1.25, 1.02, 2.2]), (0.3, [1.1, 1, 1.4], E_OUT), (0.6, [1, 1, 1], E_IO))
    return f


def cleaver_icon():
    """The GUI icon, on the diagonal like a vanilla sword (pommel bottom left): the broad black blade squared at the point,
    its pale edge on the lower side, the spine above with the ember ring, red veins, the horned guard with its eye, the
    red-corded grip, the ruby pommel."""
    t = Tex(16, 16, 0)
    veins = {(11, 3), (10, 4), (9, 4), (8, 6), (7, 7), (12, 2)}
    nicks = {(13, 2), (10, 5)}
    for y in range(1, 9):
        sx = 11 - y
        for i in range(5):
            x = sx + i
            if i == 0:
                c = hexc("#4e4e5a") if y % 3 else hexc("#5e5e6c")          # the spine
            elif i == 4:
                c = hexc("#55555f") if (x, y) in nicks else hexc("#d2d2da")   # the honed edge
            elif i == 3:
                c = hexc("#8a8a96")                                     # the bevel
            else:
                c = hexc("#2b2b34") if (x + y) % 3 else hexc("#35353f")
            if (x, y) in veins and 0 < i < 4:
                c = hexc("#ff5a2a") if (x, y) in ((10, 4), (8, 6)) else hexc("#a81c0c")
            t.set(x, y, c)
    t.set(11, 1, EMBER)                                                 # the ring by the spine near the point
    # The guard across the blade's root, its horns hooking back, the eye in the middle.
    for (x, y) in ((2, 7), (3, 8), (5, 10), (6, 11)):
        t.set(x, y, HELL_IRON[3])
    t.set(1, 7, HORN[4])
    t.set(1, 8, HORN[3])
    t.set(6, 12, HORN[3])
    t.set(7, 12, HORN[4])
    t.set(4, 9, EMBER)
    t.set(3, 9, HELL_IRON[2])
    t.set(4, 10, HELL_IRON[2])
    # The grip and the pommel.
    for (x, y), c in (((3, 10), CRIMSON[4]), ((2, 11), LEATHER[4]), ((3, 11), CRIMSON[3]), ((2, 12), CRIMSON[4]), ((1, 12), LEATHER[4]),
                      ((1, 13), RUBY[4]), ((0, 13), RUBY[2]), ((1, 14), RUBY[2]), ((0, 14), RUBY[1])):
        t.set(x, y, c)
    item_outline(t, hexc("#050406"))
    return t


# =====================================================================================================
# The Knight
# =====================================================================================================

KB = Build(hip=13.0, neck=26.0, tw=4.6, depth=5.0, aw=4.2, ax=6.7, sx=6.2, sy=25.0, elbow=19.6, wrist=14.8, palm=12.6, lx=2.3,
           lw=4.2, knee=6.8, shoe_h=1.5)
K_GRIP = (-KB.ax, KB.palm - 0.6, -0.4)
K_BLADE_REST = (90, 10, -90)        # the cleaver in the fist: forward, a touch down, the edge down


def knight_rig():
    m = Model(KNIGHT, 256, 256)
    b = KB
    man_skeleton(m, b)
    y0 = b.neck
    for side, s in SIDES:
        m.bone(f"horn_{side[0]}", (4.2 * s, y0 + 5.6, 0.0), "head")
        m.bone(f"pauldron_{side[0]}", (b.sx * s, b.sy + 0.4, 0), f"{side}_arm")
    m.bone("visor", (0, y0 + 4.0, -4.4), "head")
    m.bone("tabard_front", (0, b.hip - 0.4, -2.8), "body")
    m.bone("tabard_back", (0, b.hip - 0.4, 2.8), "body")
    m.bone("cape", (0, b.neck - 0.4, 2.8), "body")
    m.bone("blade", K_GRIP, "right_hand", rotation=K_BLADE_REST)
    m.bone("blade_veins", (K_GRIP[0], K_GRIP[1] + 14, K_GRIP[2]), "blade")
    m.bone("shield", (b.ax + 2.3, b.wrist + 2.0, 0), "left_forearm")
    knight_head(m)
    knight_body(m)
    knight_limbs(m)
    knight_gear(m)
    return m


def knight_head(m):
    b = KB
    y0 = b.neck
    helm = HellPlate(24201, 2, motif="ridge")

    def face(f, x, y, w, h):
        # The T slit: a band across the eyes, a slot down to the chin; two red points in the dark.
        if f == "north":
            mid = (w - 1) / 2
            if 6 <= y <= 7 and 2 <= x <= w - 3:
                if y == 7 and abs(abs(x - mid) - 3.0) < 0.8:
                    return EMBER
                return hexc("#050304")
            if 7 < y <= 13 and abs(x - mid) < 1.0:
                return hexc("#050304")
            if y % 3 == 0 and 9 <= y <= 14 and 3 <= abs(x - mid) <= 5:
                return BLACK_STEEL[0]                  # breaths
        return helm.mat(f, x, y, w, h)

    def face_glow(f, x, y, w, h, c):
        if c == EMBER:
            return EMBER_HOT
        return helm.glow(f, x, y, w, h, c)
    m.cube("head", (-4.4, y0 - 0.4, -4.4), (8.8, 9.0, 8.8), face, face_glow, density=2, tag="helm")
    # The brow ridge and a crest of iron over the crown.
    m.cube("head", (-4.6, y0 + 5.2, -4.8), (9.2, 1.0, 1.2), HellPlate(24202, 2).mat, HellPlate(24202, 2).glow, density=2, tag="brow")
    m.cube("head", (-0.5, y0 + 8.6, -4.0), (1.0, 1.2, 8.0), lambda f, x, y, w, h: HELL_IRON[3] if f == "up" else HELL_IRON[2], density=2, tag="crest")
    # The horns: black, ridged, sweeping out from the temples then up and forward, the tips red-hot.
    for side, s in SIDES:
        hb = f"horn_{side[0]}"
        segs = [(2.3, 2.6, (0, 18, 64 * s)), (2.0, 2.6, (0, 14, 44 * s)), (1.7, 2.4, (-8, 8, 18 * s)), (1.35, 2.2, (-18, 0, -6 * s)),
                (1.0, 2.0, (-28, 0, -20 * s)), (0.6, 1.6, (-36, 0, -30 * s))]
        px, py, pz = 4.0 * s, y0 + 5.4, 0.4
        for i, (wd, L, rot) in enumerate(segs):
            def hm(f, x, y, w, h, i=i):
                c = tone(HORN, 3.4 - 0.25 * i + (0.7 if f == "up" else 0) - (0.7 if f in ("down", "south") else 0))
                if f not in ("up", "down") and y % 2 == 0:
                    c = shade(c, 0.78)                 # the ridges
                if i == len(segs) - 1:
                    return mix(c, EMBER, 0.55 if y < 2 else 0.25)
                return c

            def hg(f, x, y, w, h, c, i=i):
                return EMBER_DIM if i == len(segs) - 1 else None
            m.cube(hb, (px - wd / 2, py - 0.3, pz - wd / 2), (wd, L + 0.3, wd), hm, hg, density=4, tag="horn", rotation=rot, pivot=(px, py, pz))
            az, ax_ = math.radians(rot[2]), math.radians(rot[0])
            px, py, pz = px + math.sin(az) * L * 0.92, py + math.cos(az) * math.cos(ax_) * L * 0.92, pz + math.sin(ax_) * L * 0.92
    # The visor's red points again, on a bone of their own just in front (the renderer can flare them).
    m.cube("visor", (-3.3, y0 + 2.7, -4.45), (6.6, 0.5, 0), lambda f, x, y, w, h: EMBER if (abs(x - 3) < 1 or abs(x - (w - 4)) < 1) else None,
           lambda f, x, y, w, h, c: EMBER_HOT, faces=("north",), density=2, tag="visor_points")
    # The gorget.
    g = HellPlate(24203, 2)
    m.cube("body", (-3.0, y0 - 1.6, -3.0), (6.0, 1.8, 6.0), g.mat, g.glow, density=2, tag="gorget")


def knight_body(m):
    b = KB
    cuirass = HellPlate(24301, 2, motif="frame")
    back = HellPlate(24302, 2, motif="plain")
    m.cube("body", (-b.tw, b.hip + 2.0, -b.depth / 2), (2 * b.tw, b.neck - b.hip - 2.0, b.depth),
           lambda f, x, y, w, h: cuirass.mat(f, x, y, w, h) if f != "south" else back.mat(f, x, y, w, h),
           lambda f, x, y, w, h, c: cuirass.glow(f, x, y, w, h, c) if f != "south" else back.glow(f, x, y, w, h, c), density=2, tag="cuirass")
    # Pectoral plates swelling over the chest.
    for s in (-1, 1):
        p = HellPlate(24310 + s, 2)
        m.cube("body", (s * 2.1 - 2.1, 19.6, -b.depth / 2 - 0.6), (4.2, 4.6, 0.8), p.mat, p.glow, density=2, tag="pectoral",
               rotation=(0, 6 * s, 0), pivot=(s * 2.1, 21.0, -b.depth / 2))
    # Abdominal lames.
    for i in range(3):
        p = HellPlate(24320 + i, 2)
        m.cube("body", (-b.tw + 0.4, b.hip + 2.0 + i * 1.4, -b.depth / 2 - 0.25), (2 * b.tw - 0.8, 1.6, b.depth + 0.5), p.mat, p.glow,
               density=2, tag="lame")
    # The belt and the mail skirt under the tabard.
    m.cube("body", (-b.tw - 0.1, b.hip + 0.8, -b.depth / 2 - 0.1), (2 * b.tw + 0.2, 1.2, b.depth + 0.2),
           lambda f, x, y, w, h: None if f in ("up", "down") else (HELL_IRON[4] if y == 0 else LEATHER[2]), density=2, tag="belt")
    m.cube("body", (-1.0, b.hip + 0.6, -b.depth / 2 - 0.35), (2.0, 1.6, 0.3), lambda f, x, y, w, h: HELL_IRON[4] if (x in (0, w - 1) or y in (0, h - 1))
           else EMBER_DIM, lambda f, x, y, w, h, c: EMBER_DIM if c == EMBER_DIM else None, density=4, tag="buckle")

    def mail(f, x, y, w, h):
        if f in ("up",):
            return None
        return MAIL[4] if (x + 2 * y) % 4 == 0 else (MAIL[2] if (x + y) % 2 else MAIL[1])
    m.cube("body", (-b.tw, b.hip - 2.6, -b.depth / 2), (2 * b.tw, 3.6, b.depth), mail, inflate=0.2, density=2, tag="mail_skirt")

    # The tabard: crimson, tattered at the hem, the Knights' sigil on its front (a horned ring over an inverted cross).
    def tabard(front):
        def mm(f, x, y, w, h):
            if f in ("up", "down"):
                return None
            if f in ("east", "west"):
                return CRIMSON[1]
            # Ragged hem: tongues cut into the last rows.
            if y >= h - 4 and ((x * 7 + 3) % 5) < (y - (h - 5)):
                return None
            c = tone(CRIMSON, 2.8 + 0.6 * math.sin(x * 0.9) - 0.8 * y / max(1, h) + (fbm(x * 2, y * 2, 24330, 64, 64, 2, 6.0) - 0.5))
            if front and f == "north":
                cx_, cy_ = (w - 1) / 2, 6.0
                r = math.hypot(x - cx_, (y - cy_) * 1.1)
                if 2.0 < r < 2.9 and y < cy_ + 1:
                    return hexc("#0a0406")
                if abs(x - cx_) < 0.6 and cy_ - 2 < y < cy_ + 6:
                    return hexc("#0a0406")
                if abs(y - (cy_ + 3)) < 0.6 and abs(x - cx_) < 2.0:
                    return hexc("#0a0406")
                if 2.0 < r < 4.2 and y < cy_ - 1.5 and abs(x - cx_) > 2.2 and abs(abs(x - cx_) - (cy_ - y) * 0.5 - 2.4) < 0.7:
                    return hexc("#0a0406")              # the horns
            if (x + y) % 9 == 0:
                return shade(c, 0.8)
            return c
        return mm
    m.cube("tabard_front", (-2.4, b.hip - 7.4, -b.depth / 2 - 0.65), (4.8, 8.4, 0.3), tabard(True), density=2, tag="tabard")
    m.cube("tabard_back", (-2.4, b.hip - 7.4, b.depth / 2 + 0.35), (4.8, 8.4, 0.3), tabard(False), density=2, tag="tabard")

    # The ragged half-cape from the shoulders.
    def cape(f, x, y, w, h):
        if f in ("up", "north"):
            return None
        if y >= h - 6 and ((x * 5 + 1) % 7) < (y - (h - 7)):
            return None
        c = tone(Ramp("#060406", "#0c080b", "#140d12", "#1d141a", "#271b23", "#33232d"), 3.0 + 0.8 * math.sin(x * 0.8) - 1.2 * y / max(1, h))
        if y >= h - 8 and (x + y) % 5 == 0:
            return CRIMSON[1]
        return c
    m.cube("cape", (-4.6, b.hip - 2.0, b.depth / 2 + 0.7), (9.2, b.neck - b.hip + 1.4, 0.3), cape, density=2, tag="cape")


def knight_limbs(m):
    b = KB
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        reb = HellPlate(24401 + s, 2, motif="plain")
        vam = HellPlate(24411 + s, 2, motif="ridge")
        cop = HellPlate(24421 + s, 2, motif="rosette")
        m.cube(f"{side}_arm", (cx - hw, b.elbow + 0.4, -hw), (b.aw, b.sy - b.elbow - 0.2, b.aw), reb.mat, reb.glow, density=2, tag="rerebrace")
        m.cube(f"{side}_arm", (cx - hw, b.elbow - 0.6, -hw), (b.aw, 1.6, b.aw), mail_m, density=2, inflate=0.05, tag="mail_elbow")
        m.cube(f"{side}_forearm", (cx - hw + 0.1, b.wrist + 0.2, -hw + 0.1), (b.aw - 0.2, b.elbow - b.wrist - 0.2, b.aw - 0.2), vam.mat, vam.glow,
               density=2, tag="vambrace")
        m.cube(f"{side}_forearm", (cx - 1.4, b.elbow - 1.2, -hw - 0.5), (2.8, 2.4, 1.0), cop.mat, cop.glow, density=2, tag="couter",
               rotation=(0, 0, 0))
        # Pauldrons: a dome and two lames flaring over the shoulder, three spikes along the top.
        pd = f"pauldron_{side[0]}"
        dome = HellPlate(24431 + s, 2, scallop=True)
        m.cube(pd, (cx - 2.8, b.sy - 1.2, -3.0), (5.6, 3.0, 6.0), dome.mat, dome.glow, density=2, tag="pauldron", rotation=(0, 0, -14 * s),
               pivot=(cx, b.sy + 0.4, 0))
        for i in range(2):
            lm = HellPlate(24441 + s * 3 + i, 2)
            m.cube(pd, (cx - 2.9 + 0.3 * i * s, b.sy - 2.4 - i * 1.1, -3.1 + 0.2 * i), (5.8 - 0.4 * i, 1.4, 6.2 - 0.4 * i), lm.mat, lm.glow,
                   density=2, tag="pauldron_lame", rotation=(0, 0, -(20 + 8 * i) * s), pivot=(cx, b.sy + 0.4, 0))
        for i, z in enumerate((-1.8, 0.0, 1.8)):
            ht = 2.2 if i == 1 else 1.6
            m.cube(pd, (cx + 0.2 * s - 0.4, b.sy + 1.6, z - 0.4), (0.8, ht, 0.8), lambda f, x, y, w, h: mix(tone(HORN, 3.4 - 2 * y / max(1, h)), EMBER, 0.4) if y < 1
                   else tone(HORN, 3.4 - 2 * y / max(1, h)), density=4, tag="spike", rotation=(0, 0, -30 * s), pivot=(cx, b.sy + 1.6, z))
    # Gauntlets: plate over the hand.
    gaunt = HellPlate(24451, 2)
    man_hands(m, b, lambda f, x, y, w, h: tone(BLACK_STEEL, 3.2 - (0.8 if y % 2 else 0) + (0.6 if f == "up" else 0)), knuckles=True)
    for side, s in SIDES:
        cx = b.ax * s
        m.cube(f"{side}_hand", (cx - 1.45, b.wrist - 0.9, -1.6), (2.9, 1.4, 3.2), gaunt.mat, gaunt.glow, inflate=0.1, density=2, tag="cuff")
    # Legs: cuisses, knee cops, greaves, sabatons.
    for side, s in SIDES:
        x0 = b.lx * s
        cu = HellPlate(24461 + s, 2)
        gr = HellPlate(24471 + s, 2, motif="ridge")
        kc = HellPlate(24481 + s, 2, motif="rosette")
        sb = HellPlate(24491 + s, 2)
        m.cube(f"{side}_leg", (x0 - b.lw / 2, b.knee + 0.4, -b.lw / 2), (b.lw, b.hip - b.knee - 0.4, b.lw), cu.mat, cu.glow, density=2, tag="cuisse")
        m.cube(f"{side}_shin", (x0 - b.lw / 2, 1.4, -b.lw / 2), (b.lw, b.knee - 1.0, b.lw), gr.mat, gr.glow, density=2, tag="greave")
        m.cube(f"{side}_shin", (x0 - 1.4, b.knee - 1.0, -b.lw / 2 - 0.6), (2.8, 2.6, 1.0), kc.mat, kc.glow, density=2, tag="knee_cop")
        m.cube(f"{side}_shin", (x0 - b.lw / 2 - 0.1, 0.0, -b.lw / 2 - 1.8), (b.lw + 0.2, 1.6, b.lw + 2.0), sb.mat, sb.glow, density=2, tag="sabaton")
        m.cube(f"{side}_shin", (x0 - 0.9, 0.0, -b.lw / 2 - 2.8), (1.8, 1.1, 1.2), sb.mat, sb.glow, density=4, tag="sabaton_toe")


def mail_m(f, x, y, w, h):
    return MAIL[4] if (x + 2 * y) % 4 == 0 else (MAIL[2] if (x + y) % 2 else MAIL[1])


def knight_gear(m):
    b = KB
    # The cleaver in his fist (built upright from its pommel, so that its grip's centre sits in the fist).
    gx, gy, gz = K_GRIP
    cleaver_parts(m, "blade", "blade_veins", o=(gx, gy - 4.3 * 0.82, gz), k=0.82, density=2)
    # The heater shield on the outside of his left forearm: black iron, a hell-iron rim, the Knights' sigil in ember.
    sx = b.ax + 2.3
    sh = HellPlate(24501, 2)

    def face(f, x, y, w, h):
        if f in ("east",):
            # The heater's outline: square top, sides curving to a point at the bottom (w across Z, h down Y).
            cx_ = (w - 1) / 2
            t = y / max(1, h - 1)
            half = (w / 2) * (1 if t < 0.45 else max(0.0, 1 - ((t - 0.45) / 0.55) ** 1.6))
            dx = abs(x - cx_)
            if dx > half:
                return None
            if dx > half - 1.2 or y == 0:
                return HELL_IRON[4] if (x + y) % 5 else HELL_IRON[2]
            # The sigil: a ring with horns and the inverted cross.
            cy_ = h * 0.38
            r = math.hypot(x - cx_, (y - cy_) * 1.05)
            if 2.6 < r < 3.6 and y < cy_ + 1.5:
                return hexc("#ff5a24")
            if abs(x - cx_) < 0.6 and cy_ - 3 < y < cy_ + 8:
                return hexc("#ff5a24")
            if abs(y - (cy_ + 4.5)) < 0.6 and abs(x - cx_) < 2.6:
                return hexc("#ff5a24")
            if y < cy_ - 2 and abs(abs(x - cx_) - (cy_ - y) * 0.45 - 3.0) < 0.7:
                return hexc("#ff5a24")
            return sh.mat("north", x, y, w, h)
        if f == "west":
            return LEATHER[2] if (y % 4) else LEATHER[4]     # the straps inside
        return HELL_IRON[2]

    def fglow(f, x, y, w, h, c):
        if c == hexc("#ff5a24"):
            return EMBER
        if f == "east":
            return sh.glow("north", x, y, w, h, c)
        return None
    m.cube("shield", (sx - 0.5, b.wrist - 4.0, -3.6), (1.0, 10.0, 7.2), face, fglow, density=2, tag="shield")


# --- the Knight's clips ---------------------------------------------------------------------------------------------

def knight_follow(pose):
    out = dict(pose)
    rl = pose.get("right_leg", [0, 0, 0])[0]
    ll = pose.get("left_leg", [0, 0, 0])[0]
    body = pose.get("body", [0, 0, 0])
    if "tabard_front" not in pose:
        out["tabard_front"] = [min(0.0, min(rl, ll)) * 0.7 - body[0] * 0.6, 0, 0]
    if "tabard_back" not in pose:
        out["tabard_back"] = [max(0.0, max(rl, ll)) * 0.6 - body[0] * 0.6, 0, 0]
    if "cape" not in pose:
        out["cape"] = [max(0.0, max(rl, ll)) * 0.2 + max(0.0, -body[0]) * 0.4 + 4, 0, 0]
    return out


class KClip(Clip):
    def key(self, t, pose, ease=None):
        return super().key(t, knight_follow(pose), ease)


def knight_anims():
    m = knight_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = f"animation.{KNIGHT}."

    def clip(name, length, loop=False, hold=False):
        clips.append(KClip(f, A + name, length, loop, hold))
        return clips[-1]

    G = {"right_fingers": [-88, 0, 0], "right_index": [-84, 0, 0], "right_thumb": [-30, 0, 24]}
    # His guard stance: feet apart, the cleaver held low and forward, the shield arm across his body.
    stance = P_(G, FIST_L, {"body": [4, 6, 0], "head": [2, -6, 0], "right_leg": [-6, 10, 6], "left_leg": [6, -6, -6],
                            "left_shin": [6, 0, 0], "right_shin": [8, 0, 0], "@root": [0, -0.4, 0],
                            "right_arm": [-10, 0, 10], "right_forearm": [-30, 0, 0], "right_hand": [0, 0, 0],
                            "left_arm": [-24, -10, -8], "left_forearm": [-50, 0, 0], "left_hand": [0, 0, 0]})

    # --- idle: heavy breaths, the blade's point lifting and settling, a slow turn of the helm. ---
    L = 4.0
    c = clip("idle", L, loop=True)
    for t, k in ((0, 0), (1.0, 1), (2.0, 0), (3.0, -1), (L, 0)):
        c.key(t, P_(stance, {"head": [2 + 2 * abs(k), -6 + 14 * k, 0], "body": [4 + abs(k), 6, 0], "@root": [0, -0.4 - 0.12 * abs(k), 0]}),
              E_IO if t else None)
    c.layer("right_forearm", lambda t: [3 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])
    c.layer("cape", lambda t: [2 * math.sin(t * 2 * math.pi / L), 0, 0])

    # --- walk: a heavy tread, the shield steady, the blade swinging low. ---
    L, n = 1.4, 8
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = man_walk(n, i % n, stride=20, arm=8, knee=28, bob=0.5, sway=3.0, lean=4)
        c.key(L * i / n, P_(stance, w, {"left_arm": [-24, -10, -8], "left_forearm": [-50, 0, 0], "right_arm": [w["right_arm"][0] - 10, 0, 10]}),
              E_IO if i else None)
    c.layer("cape", lambda t: [6 + 4 * math.sin(2 * math.pi * t / L * 2), 0, 0])

    # --- possessed: Abaddon inside -- the head jerks and lolls, the shoulders roll, the free hand clutches, the blade
    # drags; a rising on the toes now and then. ---
    L = 2.0
    c = clip("possessed", L, loop=True)
    p0 = P_(stance, CLAW_L, {"head": [-10, 20, 16], "body": [-4, -6, 4], "left_arm": [-40, -20, -30], "left_forearm": [-30, 0, 0]})
    p1 = P_(stance, CLAW_L, {"head": [16, -10, -24], "body": [10, 10, -4], "right_arm": [10, 0, 16], "right_forearm": [-10, 0, 0],
                             "left_arm": [-60, 10, -10], "left_forearm": [-70, 0, 0], "@root": [0, 0.6, 0]})
    p2 = P_(stance, OPEN_L, {"head": [-24, 4, 6], "body": [-8, 2, 0], "left_arm": [-10, 0, -50], "left_forearm": [-10, 0, 0],
                             "@root": [0, 1.0, 0]})
    c.key(0, p0)
    c.key(0.35, p0, E_IO)
    c.key(0.45, p1, E_SNAP)
    c.key(0.95, P_(p1, {"head": [20, -14, -28]}), E_IO)
    c.key(1.1, p2, E_SNAP)
    c.key(1.6, P_(p2, {"head": [-20, 10, 10]}), E_IO)
    c.key(L, p0, E_IO)
    c.layer("head", lambda t: [3 * math.sin(t * 53), 4 * math.sin(t * 41), 0] if (0.4 < t < 0.6 or 1.05 < t < 1.25) else [0, 0, 0])
    c.layer("right_hand", lambda t: [0, 0, 6 * math.sin(t * 37)] if 1.2 < t < 1.8 else [0, 0, 0])

    # --- slash: the cleaver hauled up over the right shoulder, then a diagonal cut down across the body (tick 10). ---
    c = clip("slash", 1.0)
    up = P_(stance, {"body": [-8, 30, 0], "head": [-4, -20, 0], "right_arm": [-160, 30, 10], "right_forearm": [-40, 0, 0],
                     "right_hand": [-40, 0, 0], "left_arm": [-40, -30, -10], "left_forearm": [-60, 0, 0], "right_leg": [10, 10, 6],
                     "left_leg": [-14, -6, -6], "left_shin": [14, 0, 0]})
    cut = P_(stance, {"body": [26, -26, 0], "head": [-10, 18, 0], "right_arm": [-40, -50, 20], "right_forearm": [-10, 0, 0],
                      "right_hand": [40, 0, 0], "left_arm": [10, 10, -30], "left_forearm": [-40, 0, 0], "right_leg": [14, 0, 6],
                      "left_leg": [-34, 0, -6], "left_shin": [40, 0, 0], "@root": [0, -1.4, -1.0], "cape": [30, 0, 0]})
    c.key(0, stance)
    c.key(0.35, up, E_OUT)
    c.key(0.5, cut, E_SNAP)
    c.key(0.7, P_(cut, {"body": [24, -30, 0]}), E_OUT)
    c.key(1.0, stance, E_IO)

    # --- guard: the shield brought up square before him, braced, the blade drawn back (up by tick 5, held). ---
    c = clip("guard", 1.2)
    braced = P_(stance, {"left_arm": [-72, 30, 0], "left_hand": [0, 0, 0], "body": [10, 18, 0],
                         "head": [6, -16, 0], "right_arm": [10, 20, 16], "right_forearm": [-60, 0, 0], "right_leg": [-12, 10, 6],
                         "left_leg": [-24, 0, -6], "left_shin": [30, 0, 0], "right_shin": [20, 0, 0], "@root": [0, -1.6, 0]})
    # The forearm hanging before the chest from a raised elbow, the shield's face square to the front.
    braced["left_forearm"] = aim2(r, P_(braced, {"left_forearm": [0, 0, 0]}), "left_forearm", (0, -1, 0), (-0.3, -1, -0.15), (1, 0, 0), (0, 0, -1))
    c.key(0, stance)
    c.key(0.25, braced, E_OUT)
    c.key(0.9, P_(braced, {"body": [12, 18, 0]}), E_IO)
    c.key(1.2, stance, E_IO)

    done(clips)
    return f, clips


# =====================================================================================================
# The Herald
# =====================================================================================================

HB = Build(hip=12.5, neck=25.0, tw=3.3, depth=3.4, aw=2.9, ax=4.9, sx=4.5, sy=24.0, elbow=19.0, wrist=14.4, palm=12.6, lx=1.7,
           lw=3.0, knee=6.4, shoe_h=1.0)
H_SKIN = Ramp("#2c2a2a", "#454241", "#5e5a57", "#77726d", "#8f8983", "#a8a29b")
CASSOCK = Ramp("#050406", "#0b090c", "#141016", "#1d1820", "#28212c", "#362c3a")
BRONZE = Ramp("#1e1309", "#36230f", "#523516", "#6e4a20", "#8c602b", "#ab7a3a")
VERD = hexc("#3e6a52")
CORD = Ramp("#2a1d10", "#3e2b18", "#544023", "#6b542f", "#82683c", "#9a7d4a")

HERALD_FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhhhhvhhhhhhhhh",
    "hhhhhvhhhhvhhhhh",
    "hhhhvhhhhhhvhhhh",
    "hsddddsssdddddsh",
    "hsuXXuduuduXXush",
    "hsuXRuddsduRXush",
    "hssdddssssdddssh",
    "hssssssnnssssssh",
    "hcssssnnnnsssscs",
    "hcsssssNNssssscs",
    "hccsssssssssscch",
    "hcssslmmmmlssscs",
    "hjcsssssssssscjh",
    "hjjccssssssccjjh",
]


def herald_rig():
    m = Model(HERALD, 256, 256)
    b = HB
    man_skeleton(m, b)
    y0 = b.neck
    m.bone("hood", (0, y0 + 8.0, 0.5), "head")
    m.bone("hood_tip", (0, y0 + 6.0, 4.8), "hood")
    m.bone("stole", (0, b.neck - 0.6, -1.9), "body")
    m.bone("cord", (2.0, b.hip + 0.6, -1.9), "body")
    m.bone("skirt", (0, b.hip, 0), "root")
    m.bone("skirt_lower", (0, b.knee + 0.4, 0), "skirt")
    for side, s in SIDES:
        m.bone(f"sleeve_{side[0]}", (b.ax * s, b.elbow, 0), f"{side}_forearm")
    m.bone("bell", (b.ax, b.palm - 0.6, -0.3), "left_hand")
    m.bone("clapper", (b.ax, b.palm - 3.6, -0.3), "bell")
    herald_head(m)
    herald_body(m)
    herald_bell(m)
    return m


def herald_head(m):
    b = HB
    y0 = b.neck
    sk = H.skin_mat(H_SKIN, 24601, 3.0, 0.6)
    legend = {"h": sk, "s": sk, "v": hexc("#1e1416"), "d": H_SKIN[1], "u": H_SKIN[1], "X": hexc("#030202"), "R": hexc("#d0201a"),
              "n": H_SKIN[4], "N": H_SKIN[0], "c": H_SKIN[1], "l": hexc("#1a1012"), "m": hexc("#2a1416"), "j": H_SKIN[2]}

    def eye_glow(f, x, y, w, h, c):
        if c == hexc("#d0201a"):
            return hexc("#ff3a24")
        return None
    head_cube(m, "head", y0, fix(HERALD_FACE), legend, lambda f, x, y, w, h: sk(f, x, y, w, h), eye_glow)
    ears_nose(m, "head", y0, H_SKIN, nose_h=2.0, ears=False)
    m.cube("body", (-1.3, y0 - 0.8, -1.3), (2.6, 1.2, 2.6), sk, density=2, tag="neck")
    # The cowl: a deep hood round the head, open at the face, falling to a point down the back.
    def cowl(f, x, y, w, h):
        if f == "north":
            mid = (w - 1) / 2
            if abs(x - mid) < w / 2 - 1.5 and y > 1:
                return None
            return tone(CASSOCK, 3.6 if y < 2 else 2.8)
        if f == "down":
            return None
        return tone(CASSOCK, 3.0 + 0.7 * math.sin(x * 0.9 + y * 0.3) - 0.5 * y / max(1, h) + (0.6 if f == "up" else 0))
    m.cube("hood", (-4.6, y0 - 0.6, -4.8), (9.2, 9.6, 9.4), cowl, density=2, tag="cowl")
    m.cube("hood", (-2.4, y0 + 7.6, -3.6), (4.8, 1.8, 7.6), lambda f, x, y, w, h: None if f == "down" else tone(CASSOCK, 3.2 + (0.6 if f == "up" else 0)),
           density=2, tag="cowl_peak", rotation=(0, 0, 45), pivot=(0, y0 + 8.4, 0))
    m.cube("hood", (-4.9, y0 - 2.2, -4.0), (9.8, 2.0, 8.6), lambda f, x, y, w, h: None if f in ("up", "north") else tone(CASSOCK, 2.6),
           density=2, tag="cowl_drape")
    m.cube("hood_tip", (-1.8, y0 + 1.0, 4.6), (3.6, 5.6, 1.4), lambda f, x, y, w, h: None if f == "north" else tone(CASSOCK, 2.8 - y / max(1, h)),
           density=2, tag="hood_tip", rotation=(-20, 0, 0), pivot=(0, y0 + 6.0, 4.8))


def herald_body(m):
    b = HB
    cas = lambda f, x, y, w, h: tone(CASSOCK, 2.9 + 0.5 * math.sin(x * 1.1) - 0.4 * y / max(1, h) + (0.6 if f == "up" else 0))
    m.cube("body", (-b.tw, b.hip, -b.depth / 2), (2 * b.tw, b.neck - b.hip, b.depth), cas, density=2, tag="cassock")
    # A column of small buttons down the front.
    m.cube("body", (-0.25, b.hip + 0.6, -b.depth / 2 - 0.08), (0.5, b.neck - b.hip - 1.6, 0.1),
           lambda f, x, y, w, h: BRONZE[3] if y % 3 == 0 else CASSOCK[1], density=4, tag="buttons")
    # The stole: two crimson bands from the neck to the knees, sigils stitched in gold-red.
    def stole(f, x, y, w, h):
        if f in ("up", "down", "south"):
            return CRIMSON[1]
        c = tone(CRIMSON, 3.0 - 0.6 * y / max(1, h))
        if f == "north" and 2 <= y and (y - 2) % 7 < 5 and 1 <= x <= w - 2 and h01("sig", x, (y - 2) // 7, (y - 2) % 7) < 0.45:
            return hexc("#c8762e")
        if f == "north" and y >= h - 2:
            return hexc("#b8862e") if x % 2 else CRIMSON[4]       # the fringe
        return c

    def sglow(f, x, y, w, h, c):
        return hexc("#6a1a06") if c == hexc("#c8762e") else None
    for s in (-1, 1):
        m.cube("stole", (s * 1.4 - 0.7, b.knee + 0.4, -b.depth / 2 - 0.35), (1.4, b.neck - b.knee - 1.0, 0.3), stole, sglow, density=2,
               tag="stole")
    # The rope cincture and its knotted tails.
    m.cube("body", (-b.tw, b.hip + 0.1, -b.depth / 2), (2 * b.tw, 0.7, b.depth), lambda f, x, y, w, h: None if f in ("up", "down")
           else CORD[3 + (x + y) % 2], inflate=0.25, density=2, tag="cincture")
    m.cube("cord", (1.6, b.hip - 4.4, -2.0), (0.6, 5.0, 0.6), lambda f, x, y, w, h: CORD[2 + (y % 3 == 0) * 2], density=4, tag="cord_tail")
    m.cube("cord", (2.4, b.hip - 3.2, -2.0), (0.6, 3.8, 0.6), lambda f, x, y, w, h: CORD[2 + (y % 3 == 0) * 2], density=4, tag="cord_tail")
    # The robe's skirt to the floor (a bell from the hips).
    bell_tiers(m, "skirt", b.hip + 0.4, b.knee + 0.4, (3.45, 1.9), (3.9, 2.3), cas, n=3, tag="robe")
    def hem(f, x, y, w, h):
        if f not in ("up", "down") and y >= h - 3:
            return CRIMSON[2] if y == h - 3 else CRIMSON[1]
        c = cas(f, x, y, w, h)
        return shade(c, 1.35) if f in ("north", "south") and x % 5 == 0 else c   # the folds
    bell_tiers(m, "skirt_lower", b.knee + 0.5, 0.15, (3.95, 2.35), (4.6, 2.9), hem, n=3, tag="robe_lower", cap=False)
    # Feet: black slippers at the hem.
    for side, s in SIDES:
        m.cube(f"{side}_leg", (b.lx * s - 1.3, b.knee, -1.3), (2.6, b.hip - b.knee, 2.6), cas, density=2, tag="leg")
        m.cube(f"{side}_shin", (b.lx * s - 1.2, 1.0, -1.2), (2.4, b.knee - 1.0, 2.4), cas, density=2, tag="shin")
        m.cube(f"{side}_shin", (b.lx * s - 1.2, 0.0, -2.8), (2.4, 1.1, 3.8), lambda f, x, y, w, h: CASSOCK[1 + (f == "up")], density=2, tag="slipper")
    # Arms: narrow sleeves flaring into wide bell cuffs; long grey hands, black nails.
    for side, s in SIDES:
        cx = b.ax * s
        hw = b.aw / 2
        m.cube(f"{side}_arm", (cx - hw, b.elbow, -hw), (b.aw, b.sy + 0.6 - b.elbow, b.aw), cas, density=2, inflate=0.15, tag="sleeve")
        m.cube(f"{side}_forearm", (cx - hw, b.wrist + 0.6, -hw), (b.aw, b.elbow - b.wrist - 0.4, b.aw), none_on(("up",), cas), density=2,
               inflate=0.1, tag="forearm")

        def bellm(f, x, y, w, h):
            if f in ("up", "down"):
                return None
            if y >= h - 1:
                return CRIMSON[2]                      # the crimson lining at the edge
            return cas(f, x, y, w, h)
        m.cube(f"sleeve_{side[0]}", (cx - 2.1, b.wrist - 0.6, -2.1), (4.2, 3.4, 4.2), bellm, density=2, tag="bell_sleeve")
    slim_hands(m, b, H.skin_mat(H_SKIN, 24701, 3.1, 0.5), nails=hexc("#050304"))


def herald_bell(m):
    """The handbell: a turned wooden handle up through his fist, the bronze bell below it (tarnished green in the
    grooves, a ring of sigils round the waist), the clapper inside."""
    b = HB
    bx, by, bz = b.ax, b.palm - 0.6, -0.3
    m.cube("bell", (bx - 0.45, by - 0.4, bz - 0.45), (0.9, 3.0, 0.9), lambda f, x, y, w, h: tone(Ramp("#1a0f08", "#2b1a0e", "#3e2715", "#52351d",
           "#674426", "#7e5631"), 3.2 - (y % 3 == 0) * 1.0), density=4, tag="handle")

    def bronze(f, x, y, w, h):
        if f == "down":
            return hexc("#0a0604")                     # the mouth, dark inside
        n = fbm(x * 2 + len(f), y * 2, 24801, 64, 64, 2, 6.0)
        c = tone(BRONZE, 2.4 + (n - 0.5) * 1.4 - 0.6 * y / max(1, h) + (0.9 if f == "up" else 0) + (0.8 if x == 1 else 0))
        if n < 0.32:
            c = mix(c, VERD, 0.5)
        if f != "up" and y == h // 2 and x % 3 != 0 and h > 2:
            return hexc("#7a1a10")                     # the sigil band
        return c

    def bglow(f, x, y, w, h, c):
        return hexc("#5a1006") if c == hexc("#7a1a10") else None
    tiers = [(0.75, 0.55), (0.95, 0.75), (1.15, 0.8), (1.45, 0.8), (1.9, 0.6)]
    y = by - 0.4
    for i, (hw, hh) in enumerate(tiers):
        y -= hh
        m.cube("bell", (bx - hw, y, bz - hw), (2 * hw, hh + 0.02, 2 * hw), bronze, bglow, density=4, tag="bell")
    m.cube("bell", (bx - 2.2, y - 0.35, bz - 2.2), (4.4, 0.45, 4.4), lambda f, x, y_, w, h: BRONZE[4] if f != "down" else hexc("#0a0604"),
           density=4, tag="bell_lip")
    m.cube("clapper", (bx - 0.2, by - 4.4, bz - 0.2), (0.4, 1.6, 0.4), solid(BRONZE[1]), density=4, tag="clapper")
    m.cube("clapper", (bx - 0.45, by - 4.9, bz - 0.45), (0.9, 0.7, 0.9), solid(BRONZE[2]), density=4, tag="clapper_ball")


def herald_follow(pose):
    def extra(p, out):
        body = p.get("body", [0, 0, 0])[0]
        if "stole" not in p:
            out["stole"] = [-body * 0.8, 0, 0]
        if "cord" not in p:
            out["cord"] = [-body * 0.8, 0, 0]
        if "hood_tip" not in p:
            out["hood_tip"] = [-p.get("head", [0, 0, 0])[0] * 0.5, 0, 0]
    return gown_follow(pose, extra=extra)


class HClip(Clip):
    def key(self, t, pose, ease=None):
        return super().key(t, herald_follow(pose), ease)


def herald_anims():
    m = herald_rig()
    r = m.rig
    f = AnimFile()
    clips = []
    A = f"animation.{HERALD}."

    def clip(name, length, loop=False, hold=False):
        clips.append(HClip(f, A + name, length, loop, hold))
        return clips[-1]

    BELL = {"left_fingers": [-88, 0, 0], "left_index": [-84, 0, 0], "left_thumb": [-30, 0, -24]}
    # Stooped a little, the head cocked, the bell held before him at the waist.
    stance = P_(BELL, GRACE_R, {"body": [8, 0, 0], "head": [6, 0, 8], "right_arm": [-6, 0, 6], "right_forearm": [-24, 0, 0],
                                "left_arm": [-20, 0, -6], "left_forearm": [-50, 0, 0], "left_hand": [50, 0, 0]})

    # --- idle: a sway, the head turning bird-like in jerks, the bell barely stirring. ---
    L = 4.0
    c = clip("idle", L, loop=True)
    for t, yaw in ((0, 0), (0.9, 0), (1.0, 24), (2.0, 24), (2.1, -18), (3.2, -18), (3.3, 0), (L, 0)):
        c.key(t, P_(stance, {"head": [6, yaw, 8 - abs(yaw) * 0.2]}), E_SNAP if t in (1.0, 2.1, 3.3) else (E_IO if t else None))
    c.layer("body", lambda t: [1.0 * math.sin(t * 2 * math.pi / L), 0, 1.0 * math.sin(t * 2 * math.pi / L + 1)])
    c.layer("clapper", lambda t: [0, 0, 3 * math.sin(t * 2 * math.pi * 2 / L)])

    # --- walk: a gliding, stooped step; the bell swings and tolls softly. ---
    L, n = 1.4, 8
    c = clip("walk", L, loop=True)
    for i in range(n + 1):
        w = man_walk(n, i % n, stride=14, arm=8, knee=20, bob=0.2, sway=2.0, lean=10)
        c.key(L * i / n, P_(stance, w, {"left_arm": [-20, 0, -6], "left_forearm": [-50, 0, 0], "head": [2, w["head"][1], 8]}),
              E_IO if i else None)
    c.layer("bell", lambda t: [6 * math.sin(2 * math.pi * t / L), 0, 0])
    c.layer("clapper", lambda t: [14 * math.sin(2 * math.pi * t / L - 0.8), 0, 0])

    # --- mark: the long arm levelled at the target, the finger pointing (tick 9), the bell rung high once. ---
    c = clip("mark", 1.0)
    point = P_(stance, POINT_R, {"right_arm": [-90, -8, 0], "right_forearm": [-2, 0, 0], "right_hand": [0, 0, 0], "body": [4, -10, 0],
                                 "head": [-4, 12, 6], "left_hand": [0, 0, 0]})
    point = _reach(r, P_(point, {"left_arm": [-40, 0, -60]}), "left", (10.5, 25.0, -1.0), (HB.ax, HB.palm - 0.6, -0.3))
    c.key(0, stance)
    c.key(0.3, P_(point, {"right_arm": [-70, 10, 10], "right_forearm": [-30, 0, 0]}), E_OUT)
    c.key(0.45, point, E_SNAP)
    c.key(0.75, P_(point, {"head": [-6, 10, 10]}), E_IO)
    c.key(1.0, stance, E_IO)
    c.layer("bell", lambda t: [0, 0, 22 * math.sin((t - 0.4) * 28) * math.exp(-(t - 0.4) * 4)] if t > 0.4 else [0, 0, 0])
    c.layer("clapper", lambda t: [0, 0, 30 * math.sin((t - 0.42) * 30) * math.exp(-(t - 0.42) * 3)] if t > 0.42 else [0, 0, 0])

    # --- cast: both arms raised, head thrown back, the bell shaken over his head (tick 14). ---
    c = clip("cast", 1.4)
    up = P_(stance, OPEN_R, {"right_arm": [-150, 0, 20], "right_forearm": [-20, 0, 0], "left_arm": [-160, 0, -16], "left_forearm": [-10, 0, 0],
                             "left_hand": [10, 0, 0], "head": [-30, 0, 0], "body": [-10, 0, 0], "@root": [0, 0.4, 0]})
    c.key(0, stance)
    c.key(0.45, P_(stance, CLAW_R, {"right_arm": [-40, 0, 20], "left_arm": [-60, 0, -10], "body": [14, 0, 0], "head": [16, 0, 0]}), E_IO)
    c.key(0.7, up, E_BACK)
    c.key(1.0, P_(up, {"head": [-34, 0, 0]}), E_IO)
    c.key(1.4, stance, E_IO)
    c.layer("bell", lambda t: [0, 0, 18 * math.sin(t * 34)] if 0.65 < t < 1.1 else [0, 0, 0])
    c.layer("clapper", lambda t: [0, 0, 26 * math.sin(t * 34 - 0.6)] if 0.65 < t < 1.2 else [0, 0, 0])

    done(clips)
    return f, clips


ANIMS = {"knight_rig": knight_anims, "herald_rig": herald_anims}


def generate():
    for name, rigfn, animfn, seed in ((KNIGHT, knight_rig, knight_anims, 24000), (HERALD, herald_rig, herald_anims, 24600)):
        m = rigfn()
        t, g = m.build(gutter=1, seed=seed)
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        f, _ = animfn()
        write_compact(f, ANIM + name + ".animation.json")
    cm = cleaver_item_rig()
    ct, cg = cm.build(gutter=1, seed=24100)
    cm.rig.write(ASSETS + "/geo/item/" + CLEAVER + ".geo.json")
    save(ct, "item", CLEAVER)
    save(cg, "item", CLEAVER + "_glowmask")
    cleaver_item_anims().write(ASSETS + "/animations/item/" + CLEAVER + ".animation.json")
    save(cleaver_icon(), "item", CLEAVER + "_icon")
    print(f"{CLEAVER}: {cm.height:.1f} px tall")


if __name__ == "__main__":
    generate()
