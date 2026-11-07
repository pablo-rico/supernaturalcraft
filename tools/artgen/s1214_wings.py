"""Wings for the v0.41 Michaels: allegiance_art's feathered wing (the same skeleton, feather table and pose maths) with a bone
prefix (so one rig can carry two pairs), a size factor, a subset of feathers (a lighter pair), and a palette of its own:

  AU Michael   two pairs of vast dark-gold wings of light (`wing_*` full, `wing2_*` lighter and lower);
  Michael      faint blue-white wing-shadows (`wing_*`, translucent).

Poses come from allegiance_art.wing_pose (right-wing values, the left mirrored) through `pose(prefix, names, **kw)`, which
renames the keys and keeps only the bones the rig has.
"""

import allegiance_art as AL
from chuck_art import h01
from pixelkit import Ramp, hexc, mix, shade


class Plume(AL.Feather):
    def __init__(self, pal, kind, seed, side, rim=False):
        super().__init__("light", kind, seed, side, rim)
        self.pal = pal

    def mat(self, face, x, y, w, h):
        if face not in ("north", "south"):
            return None
        s = self.sample(face, x, y, w, h)
        if s is None:
            return None
        t, q, outer, edge, n, barb = s
        cov = self.kind in AL.COVERTS
        v = 2.2 + self.tint + 1.6 * t + 0.5 * (n - 0.5) * 2 + 0.15 * barb
        if not outer:
            v -= 0.55 * q * q
        if edge and not self.rimmed(s):
            v -= 0.35
        col = self.pal["ramp"][v + (0.3 if cov else 0.0)]
        if t < 0.25:
            col = mix(self.pal["root"], col, 0.55 + 0.45 * t / 0.25)
        if self.rimmed(s):
            col = self.pal["rim"]
        a = self.pal["alpha"] - self.pal.get("fade", 0) * t - (20 if edge else 0)
        if self.pal.get("wisp") and t > 0.75 and h01("wisp", self.seed, face, x, y) < (t - 0.75) * 2.4:
            a *= 0.45
        return (col[0], col[1], col[2], max(40, min(255, int(a))))

    def glow(self, face, x, y, w, h, col):
        s = self.sample(face, x, y, w, h)
        if s is None:
            return None
        g0, g1 = self.pal["glow"]
        if self.rimmed(s):
            return shade(col, 0.85)
        if self.kind in AL.COVERTS:
            return shade(col, g0 + g1 * 0.5)
        return shade(col, g0 + g1 * s[0] * s[0])


def edge_mat(pal, seed):
    def m(face, x, y, w, h):
        from pixelkit import fbm
        n = fbm(x * 3 + len(face), y * 3, seed, 64, 64, 2, 8.0)
        top = face == "up" or (face != "down" and y == 0)
        c = pal["ramp"][3.4 + 0.9 * n - 1.3 * (y / max(1, h))]
        if top:
            c = pal["rim"]
        elif (x + 2 * ((y // 2) % 2)) % 4 == 0 or y % 2 == 1:
            c = mix(c, pal["root"], 0.3)
        return (c[0], c[1], c[2], max(60, pal["alpha"]))
    return m


def edge_glow(pal):
    g0, g1 = pal["glow"]
    return lambda face, x, y, w, h, c: shade(c, g0 + g1 * 0.6)


def build(m, parent, prefix, pal, off=(0.0, 0.0, 0.0), k=1.0, kinds=None, seed=0):
    """Both wings under `parent`: bones `<prefix>_r`, `<prefix>_r_arm` ... `<prefix>_r_<feather>` (and `_l`)."""
    ox_, oy_, oz_ = off
    feathers = [f for f in AL.FEATHERS if kinds is None or f[7] in kinds]
    for side, s in (("r", -1), ("l", 1)):
        x0, y0, z0 = AL.ROOT[0] * s + ox_, AL.ROOT[1] + oy_, AL.ROOT[2] + oz_
        root = m.bone(f"{prefix}_{side}", (x0, y0, z0), parent)
        joints = {}
        parent_b, x = root.name, x0
        for seg in ("arm", "fore", "hand"):
            b = m.bone(f"{prefix}_{side}_{seg}", (x, y0, z0), parent_b)
            joints[seg] = (x, b)
            L = AL.SEG[seg] * k
            hgt, dep = {"arm": (4.0, 2.0), "fore": (3.4, 1.8), "hand": (2.4, 1.4)}[seg]
            hgt, dep = hgt * min(1.0, k), dep * min(1.0, k)
            ox = x if s > 0 else x - L
            ext = 0.8 if seg != "hand" else 0.0
            m.cube(b, (ox - (ext if s < 0 else 0), y0 - hgt / 2 - 0.4, z0 - dep / 2 + 0.3), (L + ext, hgt, dep), edge_mat(pal, seed + 8100 + len(seg)),
                   edge_glow(pal), density=2, tag="wing_edge")
            parent_b, x = b.name, x + s * L
        for i, (name, seg, t, L, wd, ang, dz, kind) in enumerate(feathers):
            jx, jb = joints[seg]
            px = jx + s * AL.SEG[seg] * k * t
            ry = y0 - (0.2 if kind not in ("median",) else -0.6)
            fb = m.bone(f"{prefix}_{side}_{name}", (px, ry, z0 + dz), jb.name, rotation=(AL.CUP[kind], -s * AL.TWIST[kind], -s * ang))
            f = Plume(pal, kind, seed + 8200 + i * 13 + (s > 0) * 5, s, rim=name in AL.RIMMED)
            m.cube(fb, (px - wd * k / 2, ry - L * k, z0 + dz), (wd * k, L * k, 0), f.mat, f.glow, faces=("north", "south"), density=2,
                   tag=kind)


def pose(prefix, names, **kw):
    """allegiance_art.wing_pose(**kw), its keys renamed to `prefix` and kept only for bones in `names`."""
    out = {}
    for key, v in AL.wing_pose(**kw).items():
        mark = key[0] if key[0] in "@%" else ""
        bone = key[len(mark):]
        if not bone.startswith("wing_"):
            continue
        nb = prefix + "_" + bone[len("wing_"):]
        if nb in names:
            out[mark + nb] = v
    return out


FOLD, OPEN, FLAP_UP, FLAP_DOWN, GLIDE, REST = AL.FOLD, AL.OPEN, AL.FLAP_UP, AL.FLAP_DOWN, AL.GLIDE, AL.REST
WIDE = dict(AL.OPEN, arm=(0, 10, 42), spread=2.0)
HIGH = dict(arm=(0, 8, 70), fore=(0, -4, -30), hand=(0, -4, -36), fan={"primary": 12, "secondary": 6})
UNFURL = dict(arm=(0, 12, 74), fore=(0, 2, -72), hand=(0, 0, -40), fan={"primary": 30, "secondary": 22, "tertial": 10, "greater": 16,
                                                                          "median": 10})
# Wrapped round the body (forward and closed): the arms swept forward, the hands folded in front.
WRAP = dict(arm=(0, -40, 30), fore=(0, -50, -40), hand=(0, -30, -60), fan={"primary": 10, "secondary": 8}, spread=0.5, lift=-10)
# Beaten forward: the whole wing driven forward and down.
BEAT = dict(arm=(0, -30, 10), fore=(0, -20, 0), hand=(0, -10, -10), fan={"primary": -6, "secondary": -4}, spread=1.6, lift=-8)
