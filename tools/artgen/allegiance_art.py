"""v0.13 Allegiance: everything a sworn player wears or meets, but the rival hunter (rival_hunter_art).

  allegiance_wings  angel wings hung from a player's back (ranks II+): one rig, two looks -- shadow (ranks I-II: smoke and
                    a cold rim of light, translucent) and light (III-IV: white and gold, with a glowmask);
  (more in later sections: the messenger, the King's regalia, the true form, eye overlays, GUI, items, the holy oil fire)

Wings rig (Bedrock px; +X is the wearer's LEFT; origin = the middle of the upper back, as the Seraph Wings: the renderer
hangs the model from the player's body bone the way SeraphWingsDraw does):
  wings -- wing_l, wing_r                       mount roots at the shoulder blades: no clip keys them (free for the renderer)
        wing_<s> -- wing_<s>_arm -- wing_<s>_fore -- wing_<s>_hand      the arm of the wing: shoulder, elbow, wrist
                     wing_<s>_t0..t2 (tertials, on the arm), wing_<s>_s0..s6 (secondaries, on the forearm),
                     wing_<s>_p0..p6 (primaries, on the hand), wing_<s>_c0..c7 (coverts over their roots)
Each feather is its own bone, pivot at its root, hanging down (-Y) and fanned by a rest rotation about Z: the clips close the
fan along the arm to fold the wing and open it to spread. Feathers are broad (4.5-5 px) and overlap by half or more, the
inner ones lying on top (dorsal shingling), each cupped back a little and twisted about its shaft so the wing keeps volume
edge-on. Built spread flat; every clip poses it (rest is the half-folded wing of an angel at rest).
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01
from common import ASSETS, save
from michael_art import E_IN, E_IO, E_OUT, Clip, P_, mirror_rot, ruffle, to_preview, tone
from pixelkit import Ramp, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
CLEAR = (0, 0, 0, 0)

# --- palettes -----------------------------------------------------------------------------------------------
SMOKE = Ramp("#06070b", "#0d0f16", "#151824", "#1f2333", "#2b3045", "#3a405a")
RIM = Ramp("#2c4f8a", "#4570b0", "#6c97d4", "#9cbfec", "#c9defa", "#eef6ff")
PEARL = Ramp("#a59a7c", "#c7bd9f", "#ddd5bb", "#ece6d2", "#f7f3e6", "#ffffff")
GOLD = Ramp("#6a4810", "#956a1a", "#bf8f2a", "#ddb245", "#f2d272", "#fff0b0")
SUN = hexc("#fff6d8")
ASHBLUE = hexc("#5d6880")

# --- the wing's layout (left wing; the right mirrors X) -------------------------------------------------------
ROOT = (1.5, 1.0, 2.6)
SEG = {"arm": 8.0, "fore": 7.0, "hand": 6.0}


def _j(tag, i, amp):
    """A small stable jitter so no two feathers line up into rows."""
    return (h01("wingjit", tag, i) - 0.5) * 2 * amp


# (name, bone, t along the bone, length, width, angle from straight down towards the tip, depth above the arm, kind).
# Depth order seen from behind (+Z towards the viewer): median coverts, greater coverts, tertials, secondaries (inner over
# outer), primaries (inner over outer); the thick leading edge sits under the coverts' roots.
FEATHERS = (
    [(f"t{i}", "arm", 0.22 + 0.33 * i, 12.0 + 0.8 * i + _j("tl", i, 0.5), 5.2, -9 + 6 * i + _j("ta", i, 2), 1.15 - 0.09 * i + _j("tz", i, 0.04),
      "tertial") for i in range(3)]
    + [(f"s{i}", "fore", i / 6, 15.0 + 0.3 * i + _j("sl", i, 0.7), 5.2, 2 + 2.3 * i + _j("sa", i, 2.5), 0.80 - 0.08 * i + _j("sz", i, 0.04),
        "secondary") for i in range(7)]
    + [(f"p{i}", "hand", 0.05 + 0.95 * i / 6, [17.0, 18.2, 19.0, 20.0, 20.4, 20.6, 19.2][i] + _j("pl", i, 0.5), 5.8,
        20 + 8.5 * i + _j("pa", i, 2), 0.18 - 0.07 * i + _j("pz", i, 0.04), "primary") for i in range(7)]
    # Greater coverts: a row over the flight feathers' roots, half their length, broad and overlapping.
    + [(f"g{i}", seg, t, L + _j("gl", i, 0.5), 4.8, a + _j("ga", i, 2.5), 1.55 + _j("gz", i, 0.05), "greater") for i, (seg, t, L, a) in enumerate((
        ("arm", 0.45, 7.0, -4), ("arm", 0.9, 7.4, 0), ("fore", 0.15, 7.8, 4), ("fore", 0.45, 8.0, 7), ("fore", 0.75, 8.0, 10),
        ("fore", 1.0, 8.2, 14), ("hand", 0.3, 8.4, 24), ("hand", 0.62, 8.6, 37), ("hand", 0.95, 8.4, 52)))]
    # Median coverts: a shorter row on top, hugging the arm (it makes the arm read as a limb from behind).
    + [(f"m{i}", seg, t, L + _j("ml", i, 0.4), 4.4, a + _j("ma", i, 3), 1.95 + _j("mz", i, 0.05), "median") for i, (seg, t, L, a) in enumerate((
        ("arm", 0.2, 4.4, -6), ("arm", 0.6, 4.8, -2), ("arm", 1.0, 5.0, 2), ("fore", 0.3, 5.2, 5), ("fore", 0.7, 5.2, 9),
        ("hand", 0.1, 5.0, 16), ("hand", 0.55, 4.8, 30)))]
    # The alula: a little thumb of feathers at the wrist.
    + [("a0", "hand", 0.0, 6.0, 3.6, 70, 1.3, "alula")]
)
KINDS = ("tertial", "secondary", "primary", "greater", "median", "alula")
CUP = {"tertial": 9.0, "secondary": 7.0, "primary": 5.0, "greater": 12.0, "median": 14.0, "alula": 8.0}
TWIST = {"tertial": 8.0, "secondary": 10.0, "primary": 14.0, "greater": 6.0, "median": 4.0, "alula": 10.0}
COVERTS = ("greater", "median", "alula")
RIMMED = ("p6", "p5")  # the outermost primaries: their outer edge is the wing's silhouette


def feather_names(side):
    return [f"wing_{side}_{n}" for n, *_ in FEATHERS]


# --- feather painting ---------------------------------------------------------------------------------------

class Feather:
    """The shape and colour of one feather on a flat plane (y = 0 at the root, y = h-1 at the tip).

    Broad: a short quill, full width a fifth of the way down, a rounded tip (an ellipse) -- never a spike; the outer vane
    the narrower. Shaded as a SURFACE: darker at the root (where the row above covers it), brightening to the tip, a soft
    shade on the inner vane where its inner neighbour overlaps it, a per-feather tint. No outlines down the length; only
    the outermost feathers (primaries, alula) carry a rim of light on their outer edge, which is the wing's silhouette."""

    def __init__(self, look, kind, seed, side, rim=False):
        self.look, self.kind, self.seed, self.side, self.rim = look, kind, seed, side, rim
        self.tint = (h01("tint", seed) - 0.5) * 0.7

    def half(self, t):
        quill = 0.12 if self.kind not in COVERTS else 0.06
        if t < quill:
            return 0.3 + 0.7 * (t / quill) ** 0.6
        tip0 = 0.6 if self.kind not in COVERTS else 0.42
        if t <= tip0:
            return 1.0
        u = (t - tip0) / (1 - tip0)
        return math.sqrt(max(0.0, 1 - u * u)) * 0.97 + 0.03

    def sample(self, face, x, y, w, h):
        if face == "south":
            x = w - 1 - x                           # the plane seen from behind: world-consistent coordinates
        t = (y + 0.5) / h
        out = self.side
        shaft = (w - 1) / 2 + out * 0.1 * w
        hw_in, hw_out = (w / 2) * 1.1, (w / 2) * 0.82
        d = x - shaft
        outer = d * out > 0
        lim = (hw_out if outer else hw_in) * self.half(t)
        if abs(d) > lim:
            return None
        q = abs(d) / max(0.5, lim)
        n = fbm(x * 3 + len(face), y * 2, self.seed, 64, 64, 2, 6.0)
        barb = math.sin(abs(d) * 1.4 + y * 0.8 + self.seed % 5)
        edge = (lim - abs(d)) < 0.9 or (1 - t) * h < 1.2  # within a texel of the outline
        return t, q, outer, edge, n, barb

    def rimmed(self, s):
        t, q, outer, edge, n, barb = s
        return self.rim and outer and edge and t > 0.3

    def mat(self, face, x, y, w, h):
        if face not in ("north", "south"):
            return None
        s = self.sample(face, x, y, w, h)
        if s is None:
            return None
        t, q, outer, edge, n, barb = s
        cov = self.kind in COVERTS
        # Surface value: root in shade, light gathering to the tip; the overlapped inner vane a little darker.
        v = 2.5 + self.tint + 1.3 * t + 0.45 * (n - 0.5) * 2 + 0.12 * barb
        if not outer:
            v -= 0.55 * q * q
        if edge and not self.rimmed(s):
            v -= 0.35                                # a soft contour, never a bright line
        if self.look == "shadow":
            v -= 0.2 if cov else 0.0
            col = mix(SMOKE[v], ASHBLUE, 0.12 + 0.25 * t * t)
            a = 215 - 55 * t - (25 if edge else 0)
            if cov:
                a += 15
            if self.rimmed(s):
                col, a = mix(RIM[3], RIM[4], n), 200
            if t > 0.78 and h01("wisp", self.seed, face, x, y) < (t - 0.78) * 2.0:
                a *= 0.5                             # smoke unravelling at the tip
            return (col[0], col[1], col[2], max(60, min(235, int(a))))
        col = PEARL[v + (0.3 if cov else 0.0)]
        if t < 0.25:
            col = mix(GOLD[3], col, 0.55 + 0.45 * t / 0.25)  # warm at the root
        if cov and t < 0.5:
            col = mix(GOLD[4], col, 0.75 + 0.5 * t)
        if self.rimmed(s):
            col = SUN
        return (col[0], col[1], col[2], 250)

    glow_rims = False

    def glow(self, face, x, y, w, h, col):
        if self.look == "shadow":
            if self.glow_rims and col[2] > 150:
                return (col[0], col[1], col[2], 255)   # the messenger's wings: their rims of light shine
            return None
        s = self.sample(face, x, y, w, h)
        if s is None:
            return None
        if self.rimmed(s):
            return shade(col, 0.75)
        if self.kind in COVERTS:
            return shade(col, 0.22 + 0.12 * s[0])    # the coverts hold a warm glow
        return shade(col, 0.05 + 0.10 * s[0] * s[0])


def arm_mat(look, seed, side):
    """The leading edge, a limb under its feathers: a rounded bar of small scale-like marginal coverts, lit along its top."""
    def m(face, x, y, w, h):
        n = fbm(x * 3 + len(face), y * 3, seed, 64, 64, 2, 8.0)
        scallop = (x + 2 * ((y // 2) % 2)) % 4 == 0 or y % 2 == 1
        top = face == "up" or (face != "down" and y == 0)
        if look == "shadow":
            c = SMOKE[2.8 + 1.0 * n - 1.2 * (y / max(1, h))]
            if top:
                c = mix(RIM[2], RIM[3], n)
                return (c[0], c[1], c[2], 220)
            if scallop:
                c = mix(c, RIM[0], 0.15)
            return (c[0], c[1], c[2], 235)
        c = PEARL[3.6 + 0.9 * n - 1.3 * (y / max(1, h))]
        if top:
            c = SUN
        elif scallop:
            c = mix(c, GOLD[3], 0.25)
        return (c[0], c[1], c[2], 255)
    return m


def arm_glow(look, rims=False):
    def g(face, x, y, w, h, col):
        if look == "shadow":
            return (col[0], col[1], col[2], 255) if rims and col[2] > 150 else None
        return shade(col, 0.3)
    return g


# --- the rig --------------------------------------------------------------------------------------------------

def wings_rig(look="light"):
    m = Model("allegiance_wings", 256, 256)
    top = m.bone("wings", (0, 0, 0))
    build_wings(m, top.name, look)
    return m


def build_wings(m, parent, look, off=(0.0, 0.0, 0.0), glow_rims=False):
    """Both wings under bone `parent`, their origin moved by `off` (the messenger wears them on his own body)."""
    ox_, oy_, oz_ = off
    for side, s in (("r", -1), ("l", 1)):
        x0, y0, z0 = ROOT[0] * s + ox_, ROOT[1] + oy_, ROOT[2] + oz_
        root = m.bone(f"wing_{side}", (x0, y0, z0), parent)
        joints = {}
        parent_b, x = root.name, x0
        for seg in ("arm", "fore", "hand"):
            b = m.bone(f"wing_{side}_{seg}", (x, y0, z0), parent_b)
            joints[seg] = (x, b)
            # The leading edge of this segment: a thick limb at the shoulder and elbow, thinning to the wingtip.
            L = SEG[seg]
            hgt, dep = {"arm": (4.0, 2.0), "fore": (3.4, 1.8), "hand": (2.4, 1.4)}[seg]
            ox = x if s > 0 else x - L
            ext = 0.8 if seg != "hand" else 0.0
            m.cube(b, (ox - (ext if s < 0 else 0), y0 - hgt / 2 - 0.4, z0 - dep / 2 + 0.3), (L + ext, hgt, dep),
                   arm_mat(look, 8100 + len(seg) + (s > 0) * 7, s), arm_glow(look, glow_rims), density=2, tag="wing_edge")
            parent_b, x = b.name, x + s * L
        hand_b = joints["hand"][1]
        m.cube(hand_b, (x - 0.9 if s > 0 else x - 0.5, y0 - 1.1, z0 - 0.3), (1.4, 1.8, 1.2), arm_mat(look, 8120, s),
               arm_glow(look, glow_rims), density=2, tag="wing_edge")
        for i, (name, seg, t, L, wd, ang, dz, kind) in enumerate(FEATHERS):
            jx, jb = joints[seg]
            px = jx + s * SEG[seg] * t
            ry = y0 - (0.2 if kind not in ("median",) else -0.6)
            fb = m.bone(f"wing_{side}_{name}", (px, ry, z0 + dz), jb.name,
                        rotation=(CUP[kind], -s * TWIST[kind], -s * ang))
            f = Feather(look, kind, 8200 + i * 13 + (s > 0) * 5, s, rim=name in RIMMED)
            if glow_rims:
                f.glow_rims = True
            m.cube(fb, (px - wd / 2, ry - L, z0 + dz), (wd, L, 0), f.mat, f.glow, faces=("north", "south"), density=2, tag=kind)


# --- wing poses (deltas from the built pose, right-wing values; the left mirrors Y and Z) --------------------------
#
# Directions in the wing's plane are angles phi: 0 = straight out from the body, +90 = up, -90 = straight down, beyond -90
# = in towards the spine. A joint's Z raises (+) or lowers (-) everything outboard of it; a feather of rest fan angle a on a
# bone whose chain sums to theta points at phi = theta - 90 + a.

CHAIN = {"arm": ("arm",), "fore": ("arm", "fore"), "hand": ("arm", "fore", "hand")}


def wing_pose(arm=(0, 0, 0), fore=(0, 0, 0), hand=(0, 0, 0), aim=None, fan=None, lift=0.0, spread=0.0, squash=None):
    """A pose for both wings. `aim` maps a feather kind to (phi of its first feather, phi of its last): those feathers are
    turned to point there (folded wings: the feathers lie along the arm). Kinds not aimed keep their fan, plus `fan`
    degrees (+ closes towards the tip) and `spread` degrees per feather index (an open wing fanning wider). `lift` tips
    every feather out of the wing's plane (+ = back); `squash` shortens a kind's feathers (foreshortened as they tuck)."""
    aim, fan, squash = aim or {}, fan or {}, squash or {}
    joints = {"arm": arm, "fore": fore, "hand": hand}
    out = {}
    for side, s in (("r", -1), ("l", 1)):
        for seg, r in joints.items():
            mr = mirror_rot(r, s)
            if any(mr):
                out[f"wing_{side}_{seg}"] = list(mr)
        index = {}
        totals = {}
        for name, seg, t, L, wd, ang, dz, kind in FEATHERS:
            totals[kind] = totals.get(kind, 0) + 1
        for name, seg, t, L, wd, ang, dz, kind in FEATHERS:
            k = index.get(kind, 0)
            index[kind] = k + 1
            if kind in aim:
                lo, hi = aim[kind]
                u = k / max(1, totals[kind] - 1)
                theta = sum(joints[j][2] for j in CHAIN[seg])
                d = (lo + (hi - lo) * u) - (theta - 90 + ang)
                d = (d + 180) % 360 - 180   # the short way round (the joints may be wound past 180)
            else:
                d = fan.get(kind, 0.0) + spread * k
            if d or lift:
                out[f"wing_{side}_{name}"] = list(mirror_rot((lift, 0, d), s))
            if kind in squash:
                out[f"%wing_{side}_{name}"] = [1, squash[kind], 1]
    return out


# Right-wing joint rotations. Folding happens in the wing's plane (Z); Y sweeps the wing back behind the wearer.
REST = dict(arm=(0, 16, 58), fore=(0, 6, -150), hand=(0, 0, -188),
            aim={"tertial": (-90, -88), "secondary": (-92, -88), "primary": (-88, -80), "greater": (-94, -84), "median": (-96, -86),
                 "alula": (-84, -84)})
FOLD = dict(arm=(20, 8, 78), fore=(0, 0, -150), hand=(0, 0, -210),
            aim={"tertial": (-92, -90), "secondary": (-92, -90), "primary": (-91, -88), "greater": (-93, -90), "median": (-94, -90),
                 "alula": (-90, -90)}, squash={"primary": 0.82, "secondary": 0.88, "tertial": 0.9})
OPEN = dict(arm=(0, 12, 32), fore=(0, -4, -6), hand=(0, -2, -22), fan={"secondary": -2, "primary": -4}, spread=1.2)


FLAP_UP = dict(arm=(0, 2, 64), fore=(0, -8, -34), hand=(0, -6, -44), fan={"primary": 16, "secondary": 8, "tertial": 4}, lift=6)
FLAP_DOWN = dict(arm=(0, 18, -20), fore=(0, 2, 10), hand=(0, 2, 8), fan={"primary": -8, "secondary": -4}, spread=1.8, lift=-4)
GLIDE = dict(arm=(0, 10, 24), fore=(0, -2, -4), hand=(0, -2, -14), fan={"secondary": -3, "primary": -6}, spread=1.5)


def _mix(a, b, k):
    """Linear blend of two poses (missing channels are rest; scale channels blend from 1)."""
    out = {}
    for key in set(a) | set(b):
        d = [1, 1, 1] if key.startswith("%") else [0, 0, 0]
        va, vb = a.get(key, d), b.get(key, d)
        out[key] = [va[i] + (vb[i] - va[i]) * k for i in range(3)]
    return out


def wing_anims():
    """animation.allegiance_wings.*: rest (loop), fold (hold), open (hold), flap (loop), glide (loop). No clip keys the
    mount roots wing_l / wing_r."""
    f = AnimFile()
    A = "animation.allegiance_wings."
    forbid = ("wings", "wing_l", "wing_r")
    rest, fold, opn = wing_pose(**REST), wing_pose(**FOLD), wing_pose(**OPEN)
    up, down, glide = wing_pose(**FLAP_UP), wing_pose(**FLAP_DOWN), wing_pose(**GLIDE)
    tips = [f"wing_{sd}_{n}" for sd in ("l", "r") for n in ("p3", "p4", "p5", "p6", "s5", "s6")]
    clips = []

    # rest: the wings at ease, breathing -- the arm lifts a touch, the coverts stir.
    c = Clip(f, A + "rest", 4.0, loop=True, forbid=forbid)
    breath = _mix(rest, wing_pose(**dict(REST, arm=(0, 18, 62), fore=(0, 6, -153))), 1.0)
    c.key(0, rest).key(2.0, breath, E_IO).key(4.0, rest, E_IO)
    clips.append(c)

    # fold: from rest, tucked flat down the back (crouching); holds.
    c = Clip(f, A + "fold", 0.5, hold=True, forbid=forbid)
    c.key(0, rest).key(0.5, fold, E_OUT)
    clips.append(c)

    # open: the wings swing out and up, the hand unfolds last, the primaries fan open past their mark and settle; holds.
    c = Clip(f, A + "open", 0.9, hold=True, forbid=forbid)
    unfurl = wing_pose(arm=(0, 12, 74), fore=(0, 2, -72), hand=(0, 0, -40), fan={"primary": 30, "secondary": 22, "tertial": 10,
                                                                                 "greater": 16, "median": 10})
    c.key(0, rest).key(0.3, unfurl, E_IO).key(0.65, _mix(opn, wing_pose(**dict(OPEN, spread=2.2)), 1.0), E_OUT).key(0.9, opn, E_IO)
    clips.append(c)

    # flap: a beat -- the power stroke down (0 -> 0.5), wrists leading, feathers spread and pressed; the recovery up with
    # the hand bent and the primaries closed (feathered) so they slip through the air.
    c = Clip(f, A + "flap", 0.9, loop=True, forbid=forbid)
    c.key(0, up)
    c.key(0.18, _mix(up, opn, 0.8), E_IN)
    c.key(0.45, down, E_OUT)
    c.key(0.65, _mix(down, up, 0.45), E_IO)
    c.key(0.9, up, E_IO)
    clips.append(c)

    # glide: held wide, a slow rise and fall, the wingtips trembling in the air.
    c = Clip(f, A + "glide", 3.0, loop=True, forbid=forbid)
    lifted = wing_pose(**dict(GLIDE, arm=(0, 10, 30), hand=(0, -2, -18)))
    c.key(0, glide).key(1.5, lifted, E_IO).key(3.0, glide, E_IO)
    ruffle(c, tips, 3.0, 6, 8301, axis=(1.0, 0.0, 0.3))
    clips.append(c)
    for cl in clips:
        cl.done()
    return f


def preview_poses():
    return {"built": {}, "rest": to_preview(wing_pose(**REST)), "fold": to_preview(wing_pose(**FOLD)),
            "open": to_preview(wing_pose(**OPEN))}


# =====================================================================================================
# Heaven's messenger: Castiel's rig and paint (allies_art) in his own files, with shadow wings and a vial of grace
# =====================================================================================================
#
# Bones: Castiel's (root, body, head, pendant, right_arm, right_forearm, left_arm, left_forearm, right_leg, left_leg,
# coat_tail_r, coat_tail_l) plus
#   body -- shadow_wings -- wing_l, wing_r -- ... (the wing rig above, shadow look, rims glowing)
#   right_forearm -- vial (a phial of grace in his palm: upright when the forearm points forward)
# The renderer hides `shadow_wings` except during `appear` and `wing_spread`, and shows `vial` only when he offers it.

GRACE = Ramp("#2f63c4", "#5a8ee8", "#8db8fb", "#bfdcff", "#e2f0ff", "#ffffff")
VIAL_GLASS = Ramp("#46525e", "#6d7d8c", "#9db0c0", "#c9d8e4", "#e6eff6", "#ffffff")
CORK = Ramp("#4a3220", "#634530", "#7d5a3e", "#96704e", "#ad8660", "#c29c74")


def _sampler(tex, cube):
    """A material reading a cube's faces back out of an already painted texture (its old atlas place)."""
    rects = dict(cube.rects)

    def m(face, x, y, w, h):
        if face not in rects:
            return None
        u, v, rw, rh = rects[face]
        c = tex.rows[v + min(y, rh - 1)][u + min(x, rw - 1)]
        return c if c[3] else CLEAR
    return m


def messenger_rig():
    import allies_art as AL
    pt = AL.Painter("castiel")
    tex = pt.paint()
    src = pt.r
    m = Model("messenger", 256, 512)
    for b in src.bones:
        m.bone(b.name, b.pivot, b.parent, b.rotation)
    eye = hexc("#3f8fe0")

    def eye_glow(face, x, y, w, h, col):
        return hexc("#cfeaff") if col[:3] == eye[:3] else None
    for b in src.bones:
        for c in b.cubes:
            m.cube(b.name, c.origin, c.size, _sampler(tex, c), eye_glow if c.tag == "head" else None, inflate=c.inflate,
                   faces=c.faces, rotation=c.rotation, pivot=c.pivot, density=getattr(c, "density", 1), shading=False)
    sw = m.bone("shadow_wings", (0, 20, 2.8), "body")
    build_wings(m, sw.name, "shadow", off=(0.0, 19.8, 0.6), glow_rims=True)
    # The vial: a small glass phial, a cork, the grace inside a swirl of blue-white light.
    vb = m.bone("vial", (-6, 12.5, -2), "right_forearm")

    def glass(f, x, y, w, h):
        if f in ("up", "down"):
            return VIAL_GLASS[3]
        edge = x == 0 or x == w - 1
        g = GRACE[3.2 + 1.6 * math.sin(y * 1.3 + x * 0.9)]
        return mix(VIAL_GLASS[4], g, 0.25) if edge else g

    def vial_glow(f, x, y, w, h, col):
        return col if f not in ("up", "down") else None
    m.cube(vb, (-6.5, 12.0, -5.0), (1.0, 1.0, 2.6), glass, vial_glow, density=4, tag="vial")
    m.cube(vb, (-6.35, 12.15, -5.6), (0.7, 0.7, 0.6), lambda f, x, y, w, h: CORK[3 - (f == "down")], density=4, tag="vial_cork")
    return m


def messenger_anims():
    """animation.messenger.*: idle (loop), appear, talk, offer_vial, nod, fade (holds), wing_spread (holds)."""
    import allies_art as AL
    f = AnimFile()
    A = "animation.messenger."
    clips = []

    def clip(name, length, loop=False, hold=False):
        clips.append(Clip(f, A + name, length, loop, hold))
        return clips[-1]
    rest, opn = wing_pose(**REST), wing_pose(**OPEN)

    # idle: Castiel's stillness -- a slow breath, the head turning to study you, the coat barely stirring.
    c = clip("idle", 4.0, loop=True)
    for i, t in enumerate((0, 1, 2, 3, 4.0)):
        k = math.sin(math.pi * t / 2)
        c.key(t, {"@body": [0, -0.15 * abs(k), 0], "head": [-2 * abs(k), [0, 8, 0, -6, 0][i], [0, 6, 0, -4, 0][i]],
                  "right_arm": [0, 0, 4], "left_arm": [0, 0, -4], "right_forearm": [-8 - 3 * abs(k), 0, 0],
                  "left_forearm": [-8 - 3 * abs(k), 0, 0], "coat_tail_r": [1.5 * abs(k), 0, 1], "coat_tail_l": [1.5 * abs(k), 0, -1]},
              E_IO if i else None)

    # appear: wing shadows thrown wide as he arrives (the lights die), he steps forward out of them; the shadows fold and
    # shrink away to nothing (the renderer may hide them after).
    c = clip("appear", 2.6)
    big = {k: v for k, v in opn.items()}
    gone = {"%shadow_wings": [0.01, 0.01, 0.01]}
    c.key(0, P_(big, {"@root": [0, 0, 10], "%shadow_wings": [1.15, 1.15, 1.15], "head": [16, 0, 0], "body": [6, 0, 0]}))
    c.key(0.5, P_(big, {"@root": [0, 0, 7], "%shadow_wings": [1.1, 1.1, 1.1], "right_leg": [-22, 0, 0], "left_leg": [20, 0, 0],
                        "head": [10, 0, 0], "body": [4, 0, 0]}), E_OUT)
    c.key(1.0, P_(_mix(opn, rest, 0.5), {"@root": [0, 0, 3], "right_leg": [18, 0, 0], "left_leg": [-18, 0, 0], "head": [4, 0, 0]}), E_IO)
    c.key(1.6, P_(rest, {"@root": [0, 0, 0], "head": [-2, -8, 0]}), E_IO)
    c.key(2.2, P_(rest, gone, {"head": [0, -4, 6]}), E_IN)
    c.key(2.6, P_(rest, gone), E_IO)

    # talk: plain words, small movements: the head tilts (his way), the right hand opens a little.
    c = clip("talk", 2.4)
    c.key(0, {})
    c.key(0.5, {"head": [4, -6, 10], "right_forearm": [-24, 0, 0], "right_arm": [-8, 0, 4]}, E_IO)
    c.key(1.1, {"head": [0, 4, 12], "right_forearm": [-30, 0, 0], "right_arm": [-10, 0, 4]}, E_IO)
    c.key(1.7, {"head": [6, 0, 2], "right_forearm": [-14, 0, 0]}, E_IO)
    c.key(2.4, {}, E_IO)

    # offer_vial: he holds out the vial of grace on his palm (held 0.6 - 2.0), then draws the hand back.
    c = clip("offer_vial", 2.6)
    offer = {"right_arm": [-42, 8, 2], "right_forearm": [-46, 0, 0], "head": [10, 6, 0], "body": [4, 0, 0], "@root": [0, 0, -1]}
    c.key(0, {})
    c.key(0.6, offer, E_OUT)
    c.key(1.3, P_(offer, {"head": [12, 0, 6]}), E_IO)
    c.key(2.0, offer, E_IO)
    c.key(2.6, {}, E_IO)

    # nod: once, slowly.
    c = clip("nod", 1.4)
    c.key(0, {}).key(0.4, {"head": [20, 0, 0]}, E_OUT).key(0.8, {"head": [2, 0, 0]}, E_IO).key(1.4, {}, E_IO)

    # fade: he looks up to Heaven and lets go; holds (the renderer fades him out).
    c = clip("fade", 2.0, hold=True)
    c.key(0, {}).key(0.9, {"head": [-22, 0, 0], "right_arm": [-6, 0, 14], "left_arm": [-6, 0, -14], "@root": [0, 0, 1]}, E_IO)
    c.key(2.0, {"head": [-26, 0, 0], "right_arm": [-8, 0, 18], "left_arm": [-8, 0, -18], "@root": [0, 0.8, 1.5]}, E_IO)

    # wing_spread: the shadows of his wings unfold across the ground and the wall behind him; holds.
    c = clip("wing_spread", 1.6, hold=True)
    unfurl = wing_pose(arm=(0, 12, 74), fore=(0, 2, -72), hand=(0, 0, -40), fan={"primary": 30, "secondary": 22, "tertial": 10,
                                                                                 "greater": 16, "median": 10})
    c.key(0, P_(rest, {"%shadow_wings": [0.6, 0.6, 0.6]}))
    c.key(0.5, P_(unfurl, {"%shadow_wings": [1.0, 1.0, 1.0], "head": [-6, 0, 0], "body": [-3, 0, 0]}), E_IO)
    c.key(1.1, P_(_mix(opn, wing_pose(**dict(OPEN, spread=2.2)), 1.0), {"%shadow_wings": [1.2, 1.2, 1.2], "head": [-10, 0, 0],
                                                                         "body": [-4, 0, 0], "right_arm": [0, 0, 10], "left_arm": [0, 0, -10]}), E_OUT)
    c.key(1.6, P_(opn, {"%shadow_wings": [1.2, 1.2, 1.2], "head": [-8, 0, 0], "body": [-3, 0, 0], "right_arm": [0, 0, 8],
                        "left_arm": [0, 0, -8]}), E_IO)
    for cl in clips:
        cl.done()
    return f


# =====================================================================================================
# The King of Hell's regalia: a crown of black iron and a cloak
# =====================================================================================================
#
# Vanilla player-model space (Bedrock px, feet at the origin, facing north):
#   crown    pivot (0, 24, 0) = the player's head pivot: the renderer turns it with the head (or draws it on the head part)
#   cloak_0  pivot (0, 24, 2.2) = the top of the back, on the body: a high spiked collar and the cloak's top
#     -- cloak_1 (y 18.5) -- cloak_2 (y 13) -- cloak_3 (y 7.5)   each 5.5 px, hem at y 2; swung by the renderer only
# No animation file: nothing keys these bones.

IRON = Ramp("#07070a", "#0f0f13", "#18181e", "#22222a", "#2e2e38", "#3d3d4a")
EMBER = Ramp("#3a0604", "#6e0d06", "#a8200a", "#e0430f", "#ff7a26", "#ffc46a")
CLOAK = Ramp("#0b0306", "#16060b", "#230a12", "#330f1a", "#451523", "#5a1d2e")
FUR = Ramp("#050405", "#0b090b", "#131014", "#1c181d", "#272128", "#342c35")
CLOAK_SEG = 5.5


def regalia_rig():
    m = Model("king_regalia", 128, 128)
    crown = m.bone("crown", (0, 24, 0))

    def band(f, x, y, w, h):
        if f in ("up", "down"):
            return IRON[1]
        if y == 0:
            return IRON[4]
        if y == h - 1:
            return IRON[1]
        if x % 6 == 3 and y in (1, 2):
            return EMBER[4 if y == 1 else 3]   # ember gems set in the band
        return IRON[2.4 + 0.8 * math.sin(x * 0.7)]

    def band_glow(f, x, y, w, h, c):
        return c if c in (EMBER[3], EMBER[4], EMBER[5]) else None
    # A low circlet at the brow (above the eyes), 2 px of black iron.
    m.cube(crown, (-4.5, 29.6, -4.5), (9.0, 2.0, 9.0), band, band_glow, density=2, inflate=0.1, tag="crown_band")
    # Six thin jagged spikes of different heights, leaning out a little; tallest at the front.
    for i, (ang, hgt) in enumerate(((0, 5.0), (60, 3.0), (120, 2.2), (180, 3.6), (240, 2.2), (300, 3.0))):
        a_ = math.radians(ang)
        x, z = 4.5 * math.sin(a_), -4.5 * math.cos(a_)

        def spike(f, xx, y, w, h, hgt=hgt):
            if f not in ("north", "south", "east", "west"):
                return CLEAR
            t = (y + 0.5) / h                     # 0 at the point .. 1 at the base
            half = (w - 1) / 2 * t
            notch = 0.35 < t < 0.5                # a barb halfway up
            if abs(xx - (w - 1) / 2) > half + (0.8 if notch else 0.3):
                return CLEAR
            if y == 0:
                return EMBER[4]
            return IRON[4.2 - 2.0 * t]
        m.cube(crown, (x - 0.6, 31.4, z - 0.15), (1.2, hgt, 0.3), spike, band_glow, density=4, tag="crown_spike",
               rotation=(0, -ang, (h01("tw", i) - 0.5) * 12), pivot=(x, 31.4, z))
    # The gem at the brow: a hellfire ruby in the band.
    m.cube(crown, (-0.7, 29.8, -4.85), (1.4, 1.6, 0.4), lambda f, x, y, w, h: EMBER[5] if (x + y) % 3 == 0 else EMBER[3],
           lambda f, x, y, w, h, c: c, density=4, tag="crown_gem")
    # The cloak: four segments, each a little wider, the hem burning faintly.
    parent = None
    for i in range(4):
        y_top = 24 - CLOAK_SEG * i
        b = m.bone(f"cloak_{i}", (0, y_top, 2.2 + 0.2 * i), parent or None)
        if parent is None:
            b.parent = None
        hw = 4.6 + 0.6 * i
        depth = 0.5

        def cloth(f, x, y, w, h, i=i):
            n = fbm(x * 2 + i * 7, y * 2, 8601 + i, 64, 64, 2, 7.0)
            v = 2.6 + (n - 0.5) * 0.8 - 0.25 * (f == "north") + 0.5 * math.sin(x * 0.9 + i)
            c = CLOAK[v]
            if i == 3 and y >= h - 2:
                c = mix(EMBER[2], EMBER[4], n) if y == h - 1 else mix(c, EMBER[2], 0.5)
            return c

        def hem_glow(f, x, y, w, h, c, i=i):
            return c if i == 3 and y >= h - 2 and f in ("north", "south") else None
        m.cube(b, (-hw, y_top - CLOAK_SEG - (0.5 if i == 3 else 0), 2.2 + 0.2 * i), (2 * hw, CLOAK_SEG + (0.5 if i == 3 else 0), depth),
               cloth, hem_glow, density=2, tag="cloak")
        parent = b.name
    # The mantle: a ruff of black fur over the shoulders and a high collar of iron spikes behind the head.
    c0 = "cloak_0"
    fur = lambda f, x, y, w, h: FUR[2.2 + 1.4 * h01("fur", f, x, y)]
    m.cube(c0, (-5.2, 22.4, -2.6), (10.4, 2.2, 5.6), lambda f, x, y, w, h: None if f == "down" or (f == "north" and 5 <= x <= w - 6)
           else fur(f, x, y, w, h), density=2, tag="mantle")
    for j in range(5):
        x = -3.2 + 1.6 * j
        hgt = 3.0 + 1.2 * (2 - abs(j - 2))
        m.cube(c0, (x - 0.5, 24.0, 2.7), (1.0, hgt, 0.5), lambda f, xx, y, w, h: IRON[4 - 2 * y / max(1, h)] if y else EMBER[3],
               band_glow, density=4, tag="collar_spike", rotation=(-14, 0, (x) * 4), pivot=(x, 24.0, 2.9))
    return m


# =====================================================================================================
# True form: a winged silhouette of light around the player (10 s)
# =====================================================================================================
#
# Drawn full bright and translucent around the player at scale 1 (player-model space, feet at the origin):
#   true_form -- body, head, right_arm, left_arm, right_leg, left_leg   (a shell of light 0.75 px proud of each player
#                limb, same pivots and names as the vanilla player: the renderer may copy the player's limb rotations)
#                head -- halo;  body -- tf_wing_l1..l3, tf_wing_r1..r3 (three pairs of wings of light, six planes each)
# A glowmask (true_form_glowmask.png) equal to the texture is written too.
# Clips animation.true_form.*: blaze (in, 0.6 s, holds the last frame), burn (loop 1.6 s), fade (out, 0.8 s, holds).

LUX = Ramp("#ffd98a", "#ffe6a8", "#fff0c8", "#fff7e0", "#fffcf2", "#ffffff")
TF_LIMBS = {  # bone: (pivot, cube origin, size) in vanilla player space; the shell is inflated round the player's limbs
    "body": ((0, 24, 0), (-4, 12, -2), (8, 12, 4)),
    "head": ((0, 24, 0), (-4, 24, -4), (8, 8, 8)),
    "right_arm": ((-5, 22, 0), (-8, 12, -2), (4, 12, 4)),
    "left_arm": ((5, 22, 0), (4, 12, -2), (4, 12, 4)),
    "right_leg": ((-1.9, 12, 0), (-3.9, 0, -2), (4, 12, 4)),
    "left_leg": ((1.9, 12, 0), (-0.1, 0, -2), (4, 12, 4)),
}


def aura_mat(seed, strength=1.0):
    """A soft shell of light: a white core in the middle of every face fading to clear at its edges; no dark texel."""
    def m(f, x, y, w, h):
        # An elliptical glow on every face (rounded corners, no square bands), brightest in the middle.
        dx, dy = (x + 0.5 - w / 2) / (w / 2), (y + 0.5 - h / 2) / (h / 2)
        e = max(0.0, 1 - math.sqrt(dx * dx * 0.8 + dy * dy * 0.8))
        n = fbm(x * 2 + len(f), y * 2, seed, 64, 64, 2, 6.0)
        c = mix(LUX[2], LUX[5], min(1.0, 0.35 + e) * (0.85 + 0.15 * n))
        a = int(strength * (120 + 110 * e ** 0.7) * (0.9 + 0.15 * n))
        return (c[0], c[1], c[2], max(0, min(235, a)))
    return m


def true_form_rig():
    m = Model("true_form", 256, 512)
    root = m.bone("true_form", (0, 0, 0))
    for i, (name, (piv, o, sz)) in enumerate(TF_LIMBS.items()):
        b = m.bone(name, piv, root.name)
        m.cube(b, o, sz, aura_mat(8701 + i, 1.0 if name in ("body", "head") else 0.85), lambda f, x, y, w, h, c: c,
               inflate=0.75, density=1, tag="aura")
    halo = m.bone("halo", (0, 35, 0), "head")

    def ring(f, x, y, w, h):
        if f not in ("up", "down"):
            return None
        cx, cy = (w - 1) / 2, (h - 1) / 2
        r = math.hypot(x - cx, y - cy) / (w / 2)
        if 0.82 < r < 0.98:
            c = LUX[5 if r < 0.9 else 3]
            return (c[0], c[1], c[2], 240)
        return None
    m.cube(halo, (-4.5, 35, -4.5), (9, 0, 9), ring, lambda f, x, y, w, h, c: c, density=4, faces=("up", "down"), tag="halo")
    # Three pairs of wings of light behind the shoulders, larger than a sworn angel's: high, middle, low; each a fan of six
    # broad rounded planes, bright at the root and thinning to clear.
    for k, (rise, length, spread, y0) in enumerate(((34, 28, 70, 22), (6, 24, 62, 19.5), (-24, 18, 52, 16))):
        for side, s in (("r", -1), ("l", 1)):
            wb = m.bone(f"tf_wing_{side}{k + 1}", (1.5 * s, y0, 3.2), "body", rotation=mirror_rot((0, 18, rise), s))
            for j in range(6):
                ang = 8 + spread * j / 5
                L = length * (0.72 + 0.28 * math.sin(math.pi * j / 5))
                x = 1.5 * s + s * j * 2.4

                def plane(f, xx, y, w, h):
                    if f not in ("north", "south"):
                        return None
                    t = (y + 0.5) / h
                    tip0 = 0.55
                    half = (w - 1) / 2 * (1.0 if t < tip0 else math.sqrt(max(0.0, 1 - ((t - tip0) / (1 - tip0)) ** 2)))
                    q = abs(xx - (w - 1) / 2) / max(0.5, half)
                    if q > 1.06:
                        return None
                    c = mix(LUX[5], LUX[1], 0.6 * t + 0.25 * q)
                    return (c[0], c[1], c[2], int(235 - 120 * t - 50 * q * q))
                m.cube(wb, (x - 4, y0 - L, 3.2 + 0.12 * j), (8, L, 0), plane, lambda f, xx, y, w, h, c: c, density=2,
                       faces=("north", "south"), tag="tf_feather", rotation=(6, 0, -s * ang), pivot=(x, y0, 3.2 + 0.12 * j))
    return m


TF_WINGS = [f"tf_wing_{sd}{k}" for k in (1, 2, 3) for sd in ("l", "r")]


def tf_wings(open_k, beat=0.0):
    """Deltas for the six light wings: 0 folded behind, 1 their rest spread; `beat` lifts them (degrees)."""
    out = {}
    for name in TF_WINGS:
        s = 1 if "_l" in name else -1
        out[name] = list(mirror_rot((0, 50 * (1 - open_k), -40 * (1 - open_k) + beat), s))
    return out


def true_form_anims():
    f = AnimFile()
    A = "animation.true_form."
    clips = []
    c = Clip(f, A + "blaze", 0.6, hold=True)
    c.key(0, P_(tf_wings(0), {"%true_form": [0.3, 0.3, 0.3], "%halo": [0.2, 0.2, 0.2]}))
    c.key(0.3, P_(tf_wings(1.15, 8), {"%true_form": [1.25, 1.25, 1.25], "%halo": [1.3, 1.3, 1.3]}), E_OUT)
    c.key(0.6, P_(tf_wings(1), {"%true_form": [1.0, 1.0, 1.0]}), E_IO)
    clips.append(c)
    c = Clip(f, A + "burn", 1.6, loop=True)
    c.key(0, P_(tf_wings(1), {"halo": [0, 0, 0]}))
    c.key(0.8, P_(tf_wings(1.05, 6), {"%body": [1.04, 1.02, 1.04], "%head": [1.05, 1.05, 1.05], "halo": [0, 180, 0]}), E_IO)
    c.key(1.6, P_(tf_wings(1), {"halo": [0, 360, 0]}), E_IO)
    clips.append(c)
    c = Clip(f, A + "fade", 0.8, hold=True)
    c.key(0, tf_wings(1))
    c.key(0.8, P_(tf_wings(1.3, 20), {"%true_form": [1.45, 1.45, 1.45], "%halo": [1.6, 1.6, 1.6]}), E_OUT)
    clips.append(c)
    for cl in clips:
        cl.done()
    return f


# =====================================================================================================
# Eye overlays: 64x64 in the player-skin layout, only the eye pixels painted (head front: x 8..15, y 8..15)
# =====================================================================================================

EYE_PIXELS = ((9, 12), (10, 12), (13, 12), (14, 12))   # Steve's eyes: a white and an iris each


def eye_overlay(kind):
    from pixelkit import Tex
    t = Tex(64, 64)
    cols = {
        "black": (hexc("#050506"), hexc("#0d0d10")),
        "yellow": (hexc("#f7d23b"), hexc("#c9861a")),
        "red": (hexc("#ff3b1f"), hexc("#b31208")),
        "light": (hexc("#eaf6ff"), hexc("#a9d6ff")),
    }[kind]
    for i, (x, y) in enumerate(EYE_PIXELS):
        # The iris (inner pixel of each eye) a shade deeper; demon black fills the whole eye.
        inner = (x, y) in ((10, 12), (13, 12))
        t.set(x, y, cols[1] if inner and kind != "black" else cols[0])
    return t


# =====================================================================================================
# Interface (textures/gui/allegiance/), items and the holy oil fire
# =====================================================================================================
#
#   emblem_<angel|demon|hunter>  32x32   a gold wing / a black horn / a salt-ringed pentagram
#   ring                          64x32   the HUD ring round the emblem: left 32x32 empty, right 32x32 full (light grey to
#                                         white so the code can tint it: Grace gold, Corruption black-red)
#   wheel                         256x256 the power wheel: the ring centred (128,128), outer r 120, inner r 52, no dividers;
#                                         a 32x32 slice-highlight glow at (0,0) (white, the code tints/rotates/stretches it);
#                                         a 40x40 centre medallion at (216,216) (drawn by the code in the middle)
#   power/<id>                    24x24   one per Power, in a frame of its side's colour
#   title_<angel|demon|hunter>    256x64  rank title cards: celestial / hellfire / ink; text area x 40..216, y 20..44 clear-ish

import os
import json

from pixelkit import Tex, item_outline

INK = hexc("#1b1612")
PARCH = Ramp("#6b5636", "#8f7650", "#b39b70", "#cfba8e", "#e3d3ac", "#f2e7c8")
HELL = Ramp("#1a0303", "#330606", "#57100a", "#8c1c0c", "#cc3d10", "#ff8a2a")
SALTC = Ramp("#8a8590", "#b4aeb8", "#d4cfd6", "#e8e4ea", "#f6f3f6", "#ffffff")
IRONG = Ramp("#0c0c0f", "#17171c", "#24242b", "#33333c", "#45454f", "#5c5c68")
BRASSG = Ramp("#4a3510", "#6e5118", "#93702a", "#b38f3d", "#cfae58", "#e8cd80")
LEATH = Ramp("#1e120b", "#352015", "#4c2f1f", "#65412b", "#7e5638", "#986c48")
SKY = Ramp("#0e1a33", "#1d3460", "#2f5591", "#4f82c0", "#86b6e3", "#cfe9ff")
VIOLET = Ramp("#2a0f45", "#45186e", "#6a2aa0", "#9450cc", "#bd86ec", "#e6caff")


def _in_poly(x, y, pts):
    inside = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            inside = not inside
        j = i
    return inside


class Ink:
    """Pixel-centre shape filling on a Tex (pixel art: no antialiasing)."""

    def __init__(self, t):
        self.t = t

    def fill(self, test, col):
        for y in range(self.t.h):
            for x in range(self.t.w):
                if test(x + 0.5, y + 0.5):
                    c = col(x, y) if callable(col) else col
                    if c is not None:
                        self.t.set(x, y, c)
        return self

    def disc(self, cx, cy, r, col):
        return self.fill(lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r, col)

    def ring(self, cx, cy, r0, r1, col):
        return self.fill(lambda x, y: r0 * r0 <= (x - cx) ** 2 + (y - cy) ** 2 <= r1 * r1, col)

    def poly(self, pts, col):
        return self.fill(lambda x, y: _in_poly(x, y, pts), col)

    def line(self, x0, y0, x1, y1, w, col):
        L = math.hypot(x1 - x0, y1 - y0) or 1
        ux, uy = (x1 - x0) / L, (y1 - y0) / L

        def test(x, y):
            t = (x - x0) * ux + (y - y0) * uy
            d = abs((x - x0) * uy - (y - y0) * ux)
            return -0.01 <= t <= L + 0.01 and d <= w / 2
        return self.fill(test, col)

    def ellipse(self, cx, cy, rx, ry, col, rot=0.0):
        c, s = math.cos(rot), math.sin(rot)

        def test(x, y):
            dx, dy = x - cx, y - cy
            u, v = dx * c + dy * s, -dx * s + dy * c
            return (u / rx) ** 2 + (v / ry) ** 2 <= 1
        return self.fill(test, col)


def lit(ramp, x0, y0, x1, y1):
    """A colour function lighting a shape from the top-left across its bounding box."""
    def col(x, y):
        k = ((x - x0) / max(1, x1 - x0) + (y - y0) / max(1, y1 - y0)) / 2
        return ramp[4.6 - 3.2 * k]
    return col


# --- emblems ------------------------------------------------------------------------------------------------

def wing_glyph(ink, ox, oy, k, ramp, feathers=5):
    """A single wing (pointing up-left) of `feathers` broad feathers fanned from a root at (ox, oy), size factor k."""
    for i in range(feathers):
        a = math.radians(200 + i * 22)
        L = k * (11 - i * 1.2)
        ex, ey = ox + L * math.cos(a), oy + L * math.sin(a)
        ink.ellipse((ox + ex) / 2, (oy + ey) / 2, L / 2, k * 2.0, lambda x, y, i=i: ramp[4.5 - i * 0.55], rot=a)
    ink.ellipse(ox - k * 3, oy - k * 4.5, k * 4.5, k * 3.2, ramp[5], rot=math.radians(-30))


def emblem(kind):
    t = Tex(32, 32, 9900)
    ink = Ink(t)
    if kind == "angel":
        ink.disc(16, 16, 14.5, SKY[1]).ring(16, 16, 13.2, 14.5, GOLD[3])
        ink.ring(16, 16, 14.5, 15.5, GOLD[1])
        # One gold wing raised from the lower right: five broad feathers fanning up and out, a covert over their roots.
        for i, (deg, L) in enumerate(((-178, 14.0), (-155, 15.0), (-132, 14.0), (-110, 12.0), (-90, 9.0))):
            a_ = math.radians(deg)
            ex, ey = 22 + L * math.cos(a_), 23 + L * math.sin(a_)
            ink.ellipse((22 + ex) / 2, (23 + ey) / 2, L / 2 + 0.3, 2.4, GOLD[1], rot=a_)
            ink.ellipse((22 + ex) / 2, (23 + ey) / 2, L / 2 - 0.3, 1.6, lambda x, y, i=i: GOLD[3.0 + i * 0.5], rot=a_)
        ink.ellipse(20, 20.5, 3.6, 2.4, GOLD[5], rot=math.radians(-35))
        ink.disc(22, 23, 1.8, PEARL[5])
    elif kind == "demon":
        ink.disc(16, 16, 14.5, HELL[1]).ring(16, 16, 13.2, 14.5, IRONG[4]).ring(16, 16, 14.5, 15.5, IRONG[0])
        # A black horn swept up from the lower left: discs of shrinking radius along a curve, ridged, the tip burning.
        for i in range(60):
            u = i / 59
            ang = math.radians(150 + 175 * u)
            cx, cy = 16 + 8.5 * math.cos(ang), 19 + 10 * math.sin(ang)
            r = 3.6 * (1 - u) + 0.6
            ink.disc(cx, cy, r, lambda x, y, u=u: IRONG[3.2 - 2.0 * u - (1.0 if int(u * 12) % 2 else 0)])
        ink.disc(16 + 8.5 * math.cos(math.radians(325)), 19 + 10 * math.sin(math.radians(325)), 1.0, HELL[5])
    else:
        ink.disc(16, 16, 14.5, LEATH[1]).ring(16, 16, 12.4, 14.5, SALTC[3]).ring(16, 16, 14.5, 15.5, LEATH[0])
        for i in range(36):
            a = math.radians(i * 10 + 3)
            if i % 3 == 0:
                t.set(round(16 + 13.4 * math.cos(a)), round(16 + 13.4 * math.sin(a)), SALTC[5])
        star = [(16 + 10.5 * math.cos(math.radians(-90 + 144 * i)), 16 + 10.5 * math.sin(math.radians(-90 + 144 * i))) for i in range(6)]
        for (x0, y0), (x1, y1) in zip(star, star[1:]):
            ink.line(x0, y0, x1, y1, 1.3, BRASSG[4])
    item_outline(t, (12, 9, 8, 255))
    return t


def hud_ring():
    t = Tex(64, 32, 9901)
    ink = Ink(t)
    ink.ring(16, 16, 13.0, 15.5, IRONG[2]).ring(16, 16, 13.0, 13.6, IRONG[0]).ring(16, 16, 15.0, 15.5, IRONG[4])
    ink.ring(48, 16, 13.0, 15.5, lambda x, y: SALTC[3 + 2 * (y < 16)])
    ink.ring(48, 16, 13.0, 13.6, SALTC[1])
    # Ticks at the quarters on the empty ring.
    for a in (0, 90, 180, 270):
        r = math.radians(a)
        t.set(round(16 + 14.2 * math.cos(r) - 0.5), round(16 + 14.2 * math.sin(r) - 0.5), IRONG[5])
    return t


def wheel():
    t = Tex(256, 256, 9902)
    ink = Ink(t)

    def ringcol(x, y):
        r = math.hypot(x + 0.5 - 128, y + 0.5 - 128)
        if r > 117.5:
            return GOLD[3] if r < 119 else GOLD[1]
        if r < 54.5:
            return GOLD[3] if r > 53 else GOLD[1]
        n = fbm(x, y, 9903, 256, 256, 3, 8.0)
        c = mix(hexc("#0d0f16"), hexc("#1c2030"), n)
        return (c[0], c[1], c[2], 205)
    ink.ring(128, 128, 52, 120, ringcol)
    # Engraved script round the outer band.
    for i in range(96):
        a = math.radians(i * 3.75)
        if h01("glyph", i) < 0.55:
            t.set(round(128 + 113 * math.cos(a)), round(128 + 113 * math.sin(a)), mix(GOLD[2], hexc("#1c2030"), 0.4))
    # Slice highlight glow (32x32 at 0,0): a soft white wedge-ish blob.
    for y in range(32):
        for x in range(32):
            d = math.hypot((x + 0.5 - 16) / 16, (y + 0.5 - 16) / 16)
            if d < 1:
                a = int(255 * (1 - d) ** 1.5)
                t.set(x, y, (255, 255, 255, a))
    # Centre medallion (40x40 at 216,216).
    for y in range(40):
        for x in range(40):
            r = math.hypot(x + 0.5 - 20, y + 0.5 - 20)
            if r <= 19:
                c = GOLD[3] if r > 17.5 else (GOLD[1] if r > 16.5 else mix(hexc("#141824"), hexc("#262c40"), r / 16))
                t.set(216 + x, 216 + y, c)
    return t


# --- power icons --------------------------------------------------------------------------------------------

FRAMES = {"angel": (SKY, GOLD), "demon": (HELL, IRONG), "human": (LEATH, BRASSG)}


def icon_frame(side):
    t = Tex(24, 24, 9910)
    bg, rim = FRAMES[side]
    for y in range(24):
        for x in range(24):
            e = min(x, y, 23 - x, 23 - y)
            corner = (x in (0, 23) and y in (0, 1, 22, 23)) or (y in (0, 23) and x in (0, 1, 22, 23))
            if corner:
                continue
            if e == 0:
                t.set(x, y, rim[1])
            elif e == 1:
                t.set(x, y, rim[4] if x + y < 23 else rim[2])
            else:
                r = math.hypot(x - 11.5, y - 11.5) / 12
                t.set(x, y, mix(bg[2], bg[0], min(1.0, r * 0.9)))
    return t


def power_icon(pid):
    side = {"teleport": "angel", "healing_touch": "angel", "angel_blade": "angel", "angel_radio": "angel", "wings": "angel",
            "smite": "angel", "true_form": "angel", "host_squad": "angel", "light_lance": "angel",
            "hunter_mana": "human", "hunter_sense": "human", "hunter_edge": "human"}.get(pid, "demon")
    t = icon_frame(side)
    k = Ink(t)
    W, LT = hexc("#ffffff"), LUX
    if pid == "teleport":
        for i in range(3):
            k.ring(12, 12, 3 + 2.6 * i, 4 + 2.6 * i, lambda x, y, i=i: (SKY[5] if (x + y + i) % 3 else PEARL[5]) if (math.atan2(y - 12, x - 12) + i) % 2.0 < 1.3 else None)
        k.ellipse(12, 12, 1.6, 3.2, PEARL[5], rot=0.6)
    elif pid == "healing_touch":
        k.poly([(7, 20), (7, 11), (9, 9), (10, 10), (10, 5), (12, 4), (13, 5), (13, 10), (15, 5), (17, 5), (16, 12), (15, 20)], lit(SKIN_G, 7, 4, 17, 20))
        k.disc(14, 5.5, 2.4, (255, 250, 210, 255)).disc(14, 5.5, 1.2, W)
    elif pid == "angel_blade":
        k.poly([(12, 2.5), (14, 5), (13.4, 16), (10.6, 16), (10, 5)], lit(SILVERG, 10, 3, 14, 16))
        k.line(12, 3.5, 12, 15.5, 0.6, W)
        k.line(8, 16.5, 16, 16.5, 1.4, GOLD[3])
        k.line(12, 17, 12, 21.5, 1.6, IRONG[3])
    elif pid == "angel_radio":
        k.disc(12, 17, 2.2, GOLD[4])
        for i in range(3):
            k.ring(12, 17, 4 + 3 * i, 5 + 3 * i, lambda x, y: SKY[5] if y < 16 and abs(x - 12) < (y - 17) * -1.4 + 1 else None)
        k.ellipse(12, 7, 4.5, 1.4, None)
    elif pid == "wings":
        # A pair of wings spread from the middle: four broad feathers each, fanning out and up.
        for side in (-1, 1):
            for i, (deg, L) in enumerate(((-172, 11.0), (-146, 12.0), (-120, 11.0), (-96, 8.5))):
                a = math.radians(deg if side < 0 else -180 - deg)
                ex, ey = 12 + side * 1.0 + L * math.cos(a), 19 + L * math.sin(a)
                k.ellipse((12 + side * 1.0 + ex) / 2, (19 + ey) / 2, L / 2 + 0.4, 2.3, lambda x, y, i=i: PEARL[2.2 + i * 0.9], rot=a)
                k.ellipse((12 + side * 1.0 + ex) / 2, (19 + ey) / 2, L / 2 - 0.6, 1.0, lambda x, y, i=i: PEARL[3.2 + i * 0.6], rot=a)
        k.disc(12, 19, 1.8, GOLD[5])
    elif pid == "smite":
        for i in range(8):
            a = math.radians(i * 45 + 22)
            k.line(12, 12, 12 + 10 * math.cos(a), 12 + 10 * math.sin(a), 1.2, GOLD[5])
        k.poly([(8, 19), (8, 10), (10, 8), (11, 4), (13, 4), (13, 8), (15, 5), (17, 6), (16, 11), (15, 19)], lit(SKIN_G, 8, 4, 17, 19))
        k.disc(12, 12, 2, W)
    elif pid == "true_form":
        k.disc(12, 12, 9.5, lambda x, y: mix(LT[3], SKY[2], math.hypot(x - 12, y - 12) / 10))
        k.poly([(12, 4), (14, 9), (14, 19), (10, 19), (10, 9)], W)
        k.disc(12, 6, 2.2, W)
        for s in (-1, 1):
            k.poly([(12 + 2 * s, 10), (12 + 10 * s, 5), (12 + 9 * s, 12), (12 + 2 * s, 14)], LT[4])
    elif pid == "host_squad":
        for i, x in enumerate((6, 12, 18)):
            y0 = 9 if i == 1 else 11
            k.poly([(x - 3.5, y0), (x + 3.5, y0), (x + 3.5, y0 + 8), (x, y0 + 11), (x - 3.5, y0 + 8)], lit(SILVERG, x - 3, y0, x + 3, y0 + 11))
            k.line(x, y0 + 1, x, y0 + 8, 0.9, GOLD[4])
            k.line(x - 2.5, y0 + 3.5, x + 2.5, y0 + 3.5, 0.9, GOLD[4])
    elif pid == "light_lance":
        k.line(4, 20, 19, 5, 1.6, LT[3])
        k.poly([(17, 3), (21, 3), (21, 7), (18.5, 8.5), (15.5, 5.5)], W)
        for i in range(4):
            t.set(5 + i * 3, 21 - i * 3 + 2, LT[5])
    elif pid == "smoke":
        for i, (x, y, r) in enumerate(((8, 17, 4.5), (13, 14, 5), (16, 9, 4), (11, 7, 3), (7, 11, 3.5))):
            k.disc(x, y, r, lambda xx, yy, i=i: IRONG[1.5 + 1.5 * h01("sm", xx, yy)])
        k.disc(9.5, 15.5, 1.2, IRONG[4])
    elif pid == "fire_immunity":
        k.poly([(12, 3), (17, 10), (17, 16), (12, 21), (7, 16), (7, 10)], lit(HELL, 7, 3, 17, 21))
        k.poly([(12, 9), (15, 13), (14, 18), (10, 18), (9, 13)], HELL[5])
        k.ring(12, 12, 9.5, 10.6, IRONG[4])
    elif pid == "summon_hound":
        k.poly([(4, 13), (8, 8), (10, 4), (12, 8), (16, 8), (20, 12), (19, 15), (14, 15), (13, 19), (8, 19), (5, 16)], lit(IRONG, 4, 4, 20, 19))
        t.set(14, 10, HELL[5])
        t.set(15, 10, HELL[4])
        k.line(16, 15, 19, 15, 0.8, SALTC[4])
    elif pid in ("black_eyes", "yellow_eyes"):
        iris = IRONG[0] if pid == "black_eyes" else hexc("#f2c52e")
        k.ellipse(12, 12, 9, 5, SALTC[3] if pid == "yellow_eyes" else IRONG[1])
        k.disc(12, 12, 4.2, iris)
        if pid == "yellow_eyes":
            k.ellipse(12, 12, 0.9, 3.4, IRONG[0])
        else:
            k.disc(10.5, 10.5, 0.9, IRONG[5])
        k.ellipse(12, 12, 9, 5, None)
    elif pid == "telekinesis":
        k.poly([(5, 21), (5, 13), (7, 12), (8, 8), (10, 8), (10, 12), (12, 9), (14, 10), (13, 14), (12, 21)], lit(SKIN_G, 5, 8, 14, 21))
        for i in range(2):
            k.ring(16, 7, 3 + 2 * i, 3.8 + 2 * i, lambda x, y: VIOLET[4] if x < 17 or y > 8 else None)
        k.poly([(15, 4), (19, 4), (20, 8), (16, 9)], lit(STONE_G, 15, 4, 20, 9))
    elif pid == "possess":
        k.poly([(9, 21), (9, 11), (12, 9), (15, 11), (15, 21)], lit(SALTC, 9, 9, 15, 21))
        k.disc(12, 6, 2.8, SALTC[3])
        for i in range(5):
            k.disc(4 + i * 1.6, 4 + i * 1.3, 1.6 + 0.2 * i, IRONG[1 + (i % 2)])
        k.disc(11.5, 6, 0.8, IRONG[0]).disc(13, 6, 0.8, IRONG[0])
    elif pid == "first_blade":
        k.poly([(5, 20), (7, 14), (10, 9), (14, 5), (19, 3), (17, 7), (13, 11), (9, 16), (7, 21)], lit(BONE_G, 5, 3, 19, 21))
        for i in range(4):
            t.set(9 + i * 2, 15 - i * 2, BONE_G[1])
        k.line(5, 20, 8, 22, 1.4, LEATH[3])
    elif pid == "kill_regen":
        k.poly([(12, 20), (4, 12), (4, 7), (7, 4), (10, 4), (12, 7), (14, 4), (17, 4), (20, 7), (20, 12)], lit(HELL, 4, 4, 20, 20))
        k.disc(12, 11, 3, SALTC[4]).disc(11, 10.5, 0.7, IRONG[0]).disc(13, 10.5, 0.7, IRONG[0])
    elif pid == "bloodlust":
        k.poly([(12, 3), (17, 12), (17.5, 15), (15.5, 19), (12, 20.5), (8.5, 19), (6.5, 15), (7, 12)], lit(HELL, 7, 3, 17, 20))
        k.disc(10, 14, 1.2, HELL[5])
    elif pid == "dominion":
        k.poly([(4, 17), (4, 8), (8, 12), (12, 5), (16, 12), (20, 8), (20, 17)], lit(IRONG, 4, 5, 20, 17))
        k.line(4, 18.5, 20, 18.5, 2, IRONG[3])
        for x in (8, 12, 16):
            t.set(x, 15, HELL[5])
    elif pid == "throne":
        k.poly([(6, 21), (6, 4), (9, 2), (12, 4), (15, 2), (18, 4), (18, 21), (16, 21), (16, 14), (8, 14), (8, 21)], lit(IRONG, 6, 2, 18, 21))
        k.line(6, 13.5, 18, 13.5, 1.6, HELL[3])
        t.set(12, 7, HELL[5])
    elif pid == "hunter_mana":
        star = [(12 + (9 if i % 2 == 0 else 4) * math.cos(math.radians(-90 + 36 * i)),
                 12 + (9 if i % 2 == 0 else 4) * math.sin(math.radians(-90 + 36 * i))) for i in range(10)]
        k.poly(star, lit(SKY, 3, 3, 21, 21))
        k.disc(12, 12, 1.6, W)
    elif pid == "hunter_sense":
        for i in range(2):
            k.ring(12, 12, 7.5 + 2.5 * i, 8.3 + 2.5 * i, lambda x, y: BRASSG[4] if abs(x - 12) > abs(y - 12) * 1.6 else None)
        k.ellipse(12, 12, 7, 4, SALTC[3])
        k.disc(12, 12, 3.2, hexc("#3f7a4a")).disc(12, 12, 1.3, INK)
    elif pid == "hunter_edge":
        k.line(5, 19, 18, 6, 2.2, lit(SILVERG, 5, 6, 18, 19))
        k.line(4, 20, 7, 17, 2.4, LEATH[3])
        k.line(6, 6, 19, 19, 1.8, IRONG[4])
        k.line(16, 17, 20, 21, 2.6, WALNUT_G[3])
    item_outline(t, (10, 8, 8, 255))
    return t


SKIN_G = Ramp("#5e3d2c", "#8c604a", "#b07e63", "#c69579", "#d8ac90", "#e7c3a8")
SILVERG = Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff")
STONE_G = Ramp("#3a3a3e", "#5b5b60", "#6e6e73", "#7f7f84", "#929297", "#a5a5aa")
BONE_G = Ramp("#6b6352", "#8f8670", "#b3a98e", "#d1c8ac", "#e6dfc8", "#f6f1e2")
WALNUT_G = Ramp("#24130a", "#3a1f10", "#512c17", "#673a1f", "#7d4a29", "#935b34")
POWERS = ("teleport", "healing_touch", "angel_blade", "angel_radio", "wings", "smite", "true_form", "host_squad", "light_lance",
          "smoke", "fire_immunity", "summon_hound", "black_eyes", "telekinesis", "possess", "yellow_eyes", "first_blade",
          "kill_regen", "bloodlust", "dominion", "throne", "hunter_mana", "hunter_sense", "hunter_edge")


# --- title cards --------------------------------------------------------------------------------------------

def title_card(kind):
    t = Tex(256, 64, 9920)
    for y in range(64):
        for x in range(256):
            # An oval plaque fading out to the sides.
            dx, dy = (x + 0.5 - 128) / 124, (y + 0.5 - 32) / 28
            r = dx * dx + dy * dy
            if r > 1:
                continue
            fade = min(1.0, (1 - r) * 3)
            n = fbm(x, y, 9921, 256, 64, 3, 8.0)
            if kind == "angel":
                c = mix(SKY[1], SKY[2], n)
                a = int(200 * fade)
            elif kind == "demon":
                c = mix(HELL[0], HELL[2], n * n)
                a = int(220 * fade)
            else:
                c = PARCH[3.2 + 1.6 * (n - 0.5)]
                a = int(235 * fade)
            t.set(x, y, (c[0], c[1], c[2], a))
    k = Ink(t)
    if kind == "angel":
        # Gold rules above and below, rays from the centre, a pair of small wings at each end.
        k.line(30, 13, 226, 13, 1.0, GOLD[4]).line(30, 51, 226, 51, 1.0, GOLD[4])
        for i in range(24):
            a = math.radians(i * 15)
            x, y = 128 + 120 * math.cos(a), 32 + 26 * math.sin(a)
            if 14 < y < 50:
                k.line(128 + 70 * math.cos(a), 32 + 15 * math.sin(a), x, y, 0.6, (255, 244, 200, 90))
        for s in (-1, 1):
            tt = Tex(32, 32)
            wing_glyph(Ink(tt), 22, 22, 1.0, GOLD)
            for y in range(32):
                for x in range(32):
                    c = tt.rows[y][x]
                    if c[3]:
                        t.set((8 + x) if s < 0 else (248 - x), 4 + y, c)
    elif kind == "demon":
        k.line(30, 13, 226, 13, 1.0, HELL[4]).line(30, 51, 226, 51, 1.0, HELL[4])
        for i in range(60):
            x = 20 + h01("em", i) * 216
            y = 54 - h01("ey", i) * 12
            t.set(int(x), int(y), HELL[5 - (i % 2)])
        for s in (-1, 1):
            for j in range(8):
                x0 = 128 + s * (100 + j * 2)
                k.line(x0, 46 - j * 3, x0 + s * 6, 40 - j * 4, 1.4, IRONG[3])
    else:
        # Ink: brush strokes either side, a salt-ringed pentagram seal on each end.
        for i in range(3):
            y = 14 + i * 0.7
            k.fill(lambda x, yy, i=i: 30 < x < 226 and abs(yy - (14 + i * 18 + 3 * math.sin(x / 23 + i))) < 0.8 + 0.6 * math.sin(x / 9) ** 2
                   and i != 1, (INK[0], INK[1], INK[2], 200))
        for s in (-1, 1):
            cx = 22 if s < 0 else 234
            k.ring(cx, 32, 10, 11.4, (INK[0], INK[1], INK[2], 220))
            star = [(cx + 8 * math.cos(math.radians(-90 + 144 * i)), 32 + 8 * math.sin(math.radians(-90 + 144 * i))) for i in range(6)]
            for (x0, y0), (x1, y1) in zip(star, star[1:]):
                k.line(x0, y0, x1, y1, 1.0, (INK[0], INK[1], INK[2], 220))
    return t


# --- items --------------------------------------------------------------------------------------------------

def item_vial_of_grace():
    t = Tex(16, 16, 9930)
    k = Ink(t)
    # A slender phial: silver cap, glass, a swirl of grace glowing blue-white inside.
    k.fill(lambda x, y: 6 <= x <= 10 and 4 <= y <= 14 and not (y >= 13 and x in (6, 10)), lambda x, y: mix(VIAL_GLASS[2], GRACE[3], 0.3))
    k.fill(lambda x, y: 7 <= x <= 9 and 5 <= y <= 13, lambda x, y: GRACE[2.5 + 2.5 * (0.5 + 0.5 * math.sin(y * 1.6 + x * 1.2))])
    t.set(8, 7, hexc("#ffffff"))
    t.set(7, 10, GRACE[5])
    k.fill(lambda x, y: 6 <= x <= 10 and 2 <= y <= 3, lambda x, y: SILVERG[4 if x < 9 else 2])
    t.set(8, 1, SILVERG[5])
    t.set(6, 5, VIAL_GLASS[5])
    item_outline(t, (18, 14, 22, 255))
    return t


def item_holy_oil():
    t = Tex(16, 16, 9931)
    k = Ink(t)
    clay = Ramp("#3d2a1a", "#5a3e26", "#7a5634", "#966c43", "#b18655", "#c9a06c")
    k.ellipse(8, 10.5, 5.2, 4.2, lit(clay, 3, 6, 13, 15))
    k.fill(lambda x, y: 6 <= x <= 10 and 3 <= y <= 7, lit(clay, 6, 3, 10, 7))
    k.fill(lambda x, y: 6 <= x <= 10 and 2 <= y <= 2, clay[1])
    k.fill(lambda x, y: 7 <= x <= 9 and 1 <= y <= 1, hexc("#c9a74a"))
    # A gold band and a cross; a drip of golden oil.
    k.fill(lambda x, y: 3 <= x <= 13 and y == 9, GOLD[3])
    for (x, y) in ((8, 10), (8, 11), (8, 12), (7, 11), (9, 11)):
        t.set(x, y, GOLD[5])
    t.set(11, 3, GOLD[4])
    t.set(11, 4, GOLD[3])
    item_outline(t, (20, 14, 10, 255))
    return t


def item_purified_blood():
    import items
    t = items.vial(Ramp("#3a0d14", "#6a1a26", "#9a2d3c", "#c44a58", "#e2737c", "#f6a9ae"), hexc("#fff3c8"))
    # Blessed: a little gold cross glinting on the glass.
    for (x, y) in ((11, 6), (11, 7), (11, 8), (10, 7), (12, 7)):
        t.set(x, y, GOLD[5] if (x, y) == (11, 7) else GOLD[3])
    return t


# --- the holy oil fire (two cross-model textures, 8-frame animated strips) --------------------------------------

def holy_fire(variant):
    frames = 8
    t = Tex(16, 16 * frames, 9940 + variant)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                # Tongues of flame rising: noise scrolled upwards per frame, a height envelope, gold-white with a blue root.
                n = fbm(x * 2 + variant * 11, (y + f * 3) * 2, 9941 + variant, 32, 128, 3, 6.0)
                h = 1 - y / 15                       # 0 at the bottom row .. 1 at the top
                cols = 0.6 + 0.4 * math.sin(x * 0.9 + variant * 2 + f * 0.5)
                v = n * 1.25 + 0.55 - h * 1.05 * (1.2 - 0.4 * cols)
                if v < 0.32:
                    continue
                k = min(1.0, (v - 0.32) / 0.6)
                if y >= 13:
                    c = mix(hexc("#4f86ff"), hexc("#c9e4ff"), k)
                else:
                    c = mix(hexc("#e8a52a"), hexc("#fff7d6"), k)
                t.set(x, f * 16 + y, (c[0], c[1], c[2], 255))
    return t


def _mcmeta(path, frametime):
    with open(path + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": frametime}}, fh, indent=1)
        fh.write("\n")


def generate_gui_items():
    for kind in ("angel", "demon", "hunter"):
        save(emblem(kind), "gui/allegiance", f"emblem_{kind}")
        save(title_card(kind), "gui/allegiance", f"title_{kind}")
    save(hud_ring(), "gui/allegiance", "ring")
    save(wheel(), "gui/allegiance", "wheel")
    for pid in POWERS:
        save(power_icon(pid), "gui/allegiance/power", pid)
    save(item_vial_of_grace(), "item", "vial_of_grace")
    save(item_holy_oil(), "item", "holy_oil")
    save(item_purified_blood(), "item", "purified_blood")
    for v in (0, 1):
        _mcmeta(save(holy_fire(v), "block", f"holy_oil_fire_{v}"), 2)


def preview_target(name):
    """(model, preview texture, AnimFile, hidden(clip)) for the scratch clip sheets."""
    from chuck_art import composite
    if name == "messenger":
        m = messenger_rig()
        t, g = m.build(gutter=1, seed=8400)

        def hidden(clip):
            out = [] if clip in ("appear", "wing_spread") else ["shadow_wings"]
            return out + ([] if clip == "offer_vial" else ["vial"])
        return m, composite(t, g, 0.85), messenger_anims(), hidden
    if name == "true_form":
        m = true_form_rig()
        t, g = m.build(gutter=1, seed=8700)
        return m, t, true_form_anims(), lambda clip: []
    raise KeyError(name)


def counts(m):
    return len(m.rig.bones), sum(len(b.cubes) for b in m.rig.bones)


def generate():
    for look in ("shadow", "light"):
        m = wings_rig(look)
        t, g = m.build(gutter=1, seed=8000)
        save(t, "entity", f"allegiance_wings_{look}")
        if look == "light":
            save(g, "entity", "allegiance_wings_light_glowmask")
    m.rig.write(GEO + "allegiance_wings.geo.json")
    wing_anims().write(ANIM + "allegiance_wings.animation.json")
    m = messenger_rig()
    t, g = m.build(gutter=1, seed=8400)
    m.rig.write(GEO + "messenger.geo.json")
    save(t, "entity", "messenger")
    save(g, "entity", "messenger_glowmask")
    messenger_anims().write(ANIM + "messenger.animation.json")
    m = regalia_rig()
    t, g = m.build(gutter=1, seed=8600)
    m.rig.write(GEO + "king_regalia.geo.json")
    save(t, "entity", "king_regalia")
    save(g, "entity", "king_regalia_glowmask")
    m = true_form_rig()
    t, g = m.build(gutter=1, seed=8700)
    m.rig.write(GEO + "true_form.geo.json")
    save(t, "entity", "true_form")
    save(g, "entity", "true_form_glowmask")
    true_form_anims().write(ANIM + "true_form.animation.json")
    for kind in ("black", "yellow", "red", "light"):
        save(eye_overlay(kind), "entity/player", f"eyes_{kind}")
    generate_gui_items()


if __name__ == "__main__":
    generate()
