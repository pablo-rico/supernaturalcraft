"""Shared kit for the v0.41 art (seasons 12-14): reads the `*Assets` contracts straight from the Java sources (clips, loops,
hit ticks, lengths, bones), checks every rig and clip file against them before writing, and adds the pieces apoc_vessel does
not have (skirts, buns, long hair, glasses, a hat), the common clip shapes (breathing idles, walks, recoils) and the trophy
bust builder every v0.41 boss shares.

Every v0.41 humanoid is built on apoc_vessel's skeleton (root, body, head, eyes, brows, arms with forearms, hands and finger
bones, legs with shins), so the contract's humanoid bones (body, head, right_arm ... left_leg) are always there.

Rule of the contracts: the OPTIONAL_BONES are the renderer's -- no clip keys them. Where a clip must move such a part (Dagon's
coat tail, the Shadow's tendrils), the named bone is an unkeyed container and the clips move its children.
"""

import math
import os
import re

import blockmodel as BM
from apoc_vessel import E_IO, E_OUT, SIDES, tone
from chuck_art import h01, none_on, solid
from common import ASSETS, ROOT, save
from michael_art import P_, write_compact
from pixelkit import Tex, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
JAVA = os.path.join(ROOT, "src", "main", "java", "org", "papiricoh", "supernaturalcraft")
SCRATCH = os.environ.get("SN_ART_PREVIEW", os.path.join(ROOT, "build", "artpreview"))


# =====================================================================================================
# The contract
# =====================================================================================================

class Contract:
    """The numbers of one `*Assets` class, parsed from its Java source."""

    def __init__(self, rel):
        path = os.path.join(JAVA, *rel.split("/")) + ".java"
        with open(path, encoding="utf-8") as f:
            src = f.read()
        self.src = src

        def lst(name):
            m = re.search(r"\b" + name + r"\s*=\s*List\.of\(([^)]*)\)", src)
            return re.findall(r'"([^"]+)"', m.group(1)) if m else []

        def mp(name):
            m = re.search(r"\b" + name + r"\s*=\s*Map\.ofEntries\((.*?)\);", src, re.S)
            return {k: int(v) for k, v in re.findall(r'Map\.entry\("([^"]+)",\s*(\d+)\)', m.group(1))} if m else {}

        def st(name):
            m = re.search(r"\b" + name + r'\s*=\s*"([^"]+)"', src)
            return m.group(1) if m else None
        self.rig = st("RIG")
        self.clips = lst("CLIPS")
        self.loops = lst("LOOPS")
        self.bones = lst("BONES")
        self.optional = lst("OPTIONAL_BONES")
        self.hits = mp("HIT_TICKS")
        self.ticks = mp("CLIP_TICKS")
        m = re.search(r"MIN_BONES\s*=\s*(\d+)", src)
        self.min_bones = int(m.group(1)) if m else 0
        self.st = st
        self.lst = lst

    def hit(self, clip):
        """The clip's moment, in seconds."""
        return self.hits[clip] / 20.0

    def length(self, clip, default=None):
        if clip in self.ticks:
            return self.ticks[clip] / 20.0
        if default is None:
            raise KeyError(f"{self.rig}: no length for {clip}")
        return default


def verify(model, f, prefix, clips, loops, ticks=None, optional=(), bones=(), min_bones=0):
    """Raises unless the rig and its clips meet the contract: bones present (and enough of them), every clip there with its
    loop flag and length, no clip keying an optional bone, every keyed bone existing."""
    names = {b.name for b in model.rig.bones}
    missing = [b for b in list(bones) + list(optional) if b not in names]
    if missing:
        raise ValueError(f"{model.rig.identifier}: missing bones {missing}")
    if len(names) < min_bones:
        raise ValueError(f"{model.rig.identifier}: {len(names)} bones < {min_bones}")
    if len(names) != len(model.rig.bones):
        raise ValueError(f"{model.rig.identifier}: duplicate bone names")
    have = {a.name: a for a in f.anims}
    for c in clips:
        a = have.get(prefix + c)
        if a is None:
            raise ValueError(f"missing clip {prefix + c}")
        if (c in loops) != (a.loop is True):
            raise ValueError(f"{prefix + c}: loop flag wrong")
        if ticks and c in ticks and abs(a.length - ticks[c] / 20.0) > 0.051:
            raise ValueError(f"{prefix + c}: length {a.length} != {ticks[c] / 20.0}")
        bad = set(a.bones) & set(optional)
        if bad:
            raise ValueError(f"{prefix + c} keys renderer bones {sorted(bad)}")
    for a in f.anims:
        unknown = set(a.bones) - names
        if unknown:
            raise ValueError(f"{a.name} keys missing bones {sorted(unknown)}")


def verify_contract(model, f, con, prefix=None):
    verify(model, f, prefix or f"animation.{con.rig}.", con.clips, con.loops, con.ticks, con.optional, con.bones, con.min_bones)


def write_entity(model, f, name, tex, glow=None, compact=True):
    model.rig.write(GEO + name + ".geo.json")
    save(tex, "entity", name)
    if glow is not None:
        save(glow, "entity", name + "_glowmask")
    if f is not None:
        if compact:
            write_compact(f, ANIM + name + ".animation.json")
        else:
            f.write(ANIM + name + ".animation.json")


# =====================================================================================================
# Pieces apoc_vessel does not have
# =====================================================================================================

def skirt(m, b, mat, hem, flare=0.35, front=True, tag="skirt", inflate=0.35):
    """A skirt from the hips to `hem` (px from the ground): a tube on each thigh (and shin, if it falls below the knee) so it
    swings with the legs, flared a little at the hem, and a panel at the front and back between the legs that closes the
    gap while they stand together."""
    for side, s in SIDES:
        x0 = b.LX * s - b.LW / 2 - 0.15
        w = b.LW + 0.3
        top = b.HIP + 0.4
        knee = max(hem, b.KNEE)
        m.cube(f"{side}_leg", (x0, knee, -b.LW / 2 - 0.15), (w, top - knee, b.LW + 0.3), none_on(("up",), mat), inflate=inflate,
               density=2, tag=tag)
        if hem < b.KNEE:
            m.cube(f"{side}_shin", (x0 - flare / 2, hem, -b.LW / 2 - 0.15 - flare / 2), (w + flare, b.KNEE - hem, b.LW + 0.3 + flare),
                   none_on(("up",), mat), inflate=inflate, density=2, tag=tag + "_low")
    if front:
        h = b.HIP + 0.4 - max(hem, b.KNEE - 1.0)
        for z, face in ((-b.LW / 2 - 0.55, "north"), (b.LW / 2 + 0.25, "south")):
            m.cube("body", (-0.9, b.HIP + 0.4 - h, z), (1.8, h, 0.3), lambda f, x, y, w, hh: mat(f, x, y, w, hh) if f == face else None,
                   density=2, tag=tag + "_gap")


def bun(m, b, hair, size=3.2, height=None, back=True, tag="bun"):
    """A tight bun at the back of the crown (bone `bun`, under the head)."""
    y = b.crown - 2.6 if height is None else height
    bn = m.bone("bun", (0, y, b.HEAD / 2), "head")
    z0 = b.HEAD / 2 - 0.4 if back else -size / 2
    m.cube(bn, (-size / 2, y - size / 2, z0), (size, size, size * 0.8), hair, density=2, tag=tag)
    return bn


def glasses(m, b, frame, lens, bone_name="glasses", parent="head", y=None, rimless=True):
    """Thin glasses on the face: two lenses (clear-ish glass) and a bridge; temples back to the ears."""
    hs = b.HEAD / 2
    yy = b.NECK + b.HEAD * 0.47 if y is None else y
    g = m.bone(bone_name, (0, yy, -hs), parent)
    for s in (-1, 1):
        def ln(f, x, yv, w, h, s=s):
            if f != "north":
                return None
            edge = x in (0, w - 1) or yv in (0, h - 1)
            return frame if (edge and not rimless) or (edge and yv == 0) else lens
        m.cube(g, (s * 1.8 - 1.3, yy - 0.2, -hs - 0.25), (2.6, 1.4, 0), ln, faces=("north",), density=4, tag="lens")
        m.cube(g, (s * hs - (0.1 if s > 0 else -0.1) - 0.1, yy + 0.8, -hs - 0.2), (0.2, 0.2, 3.6), solid(frame), density=4,
               tag="temple")
    m.cube(g, (-0.5, yy + 0.9, -hs - 0.3), (1.0, 0.25, 0.2), solid(frame), density=4, tag="bridge")
    return g


def long_hair(m, b, strand, seed, length=10.0, width=8.4, locks=True, parent="head", name="hair", lock_len=8.5, volume=True,
              part_bone=True):
    """Long hair down the back (`hair` -- `hair_back` -- `hair_tip`) and side locks over the shoulders (`hair_r`, `hair_l`).
    With `part_bone` false the container `name` holds nothing that moves: its children carry the strands (so a clip may key
    `hair_back` while the renderer owns `hair`)."""
    hb = m.bone(name, (0, b.NECK + 6, 1.5), parent)
    back = m.bone(name + "_back", (0, b.NECK + 0.5, 3.4), hb.name)
    tip = m.bone(name + "_tip", (0, b.NECK + 0.5 - length * 0.65, 3.6), back.name)
    if volume:
        m.cube(back if not part_bone else hb, (-width / 2, b.NECK + 0.5, 1.6), (width, 6.0, 2.6), none_on(("up",), strand), density=2,
               tag="hair_volume")
    seg = length * 0.65
    m.cube(back, (-width / 2 + 0.2, b.NECK + 0.5 - seg, 2.4), (width - 0.4, seg, 1.4), strand, density=2, tag="hair_fall")

    def ends(f, x, y, w, h):
        if f in ("north", "south", "east", "west") and y > h - 3 and h01("tip", seed, f, x) < 0.5 + (y - (h - 3)) * 0.2:
            return None
        return strand(f, x, y, w, h)
    rest = length - seg
    m.cube(tip, (-width / 2 + 0.6, b.NECK + 0.5 - seg - rest, 2.5), (width - 1.2, rest, 1.2), ends, density=2, tag="hair_ends")
    out = [hb, back, tip]
    if locks:
        for nm, s in ((name + "_r", -1), (name + "_l", 1)):
            lock = m.bone(nm, (s * 3.6, b.NECK + 4, -1.0), hb.name)
            m.cube(lock, (s * 3.9 - 0.9, b.NECK + 4 - lock_len, -2.6), (1.8, lock_len, 2.6), ends, density=2, tag="hair_lock",
                   rotation=(4, 0, -4 * s), pivot=(s * 3.9, b.NECK + 4, -1.0))
            out.append(lock)
    return out


def brows(m, b, col, tilt=6, w=2.2, h=0.4, y=None):
    for s in (-1, 1):
        m.cube("brows", (s * 2.0 - w / 2, (b.NECK + b.HEAD * 0.62) if y is None else y, -b.HEAD / 2 - 0.1), (w, h, 0.2), solid(col),
               density=4, tag="brow", rotation=(0, 0, -tilt * s), pivot=(s * 1.0, b.NECK + 5.0, -b.HEAD / 2 - 0.1))


def legend(sk, hair, eye_white, iris, lips, skin_ramp, brow=None, extra=None):
    """The usual face legend: s skin, h hair, b brow, U under-eye shadow, u lid, k crease, w eye white, I iris, n nose light,
    N nose shade, l lip line, L lip, j jaw shade, M lip light."""
    sh = lambda k: (lambda f, x, y, w, h: shade(sk(f, x, y, w, h), k))
    lg = {"s": sk, "h": hair, "b": brow if brow is not None else hexc("#2a2420"), "U": sh(0.78), "u": sh(0.86), "k": sh(0.8),
          "w": eye_white, "I": iris, "n": skin_ramp[4], "N": skin_ramp[1], "l": lips[1], "L": lips[3], "M": lips[4], "j": sh(0.9)}
    lg.update(extra or {})
    return lg


# =====================================================================================================
# Clips
# =====================================================================================================

def fist(side, k=1.0):
    return {f"{side}_fingers": [80 * k, 0, 0], f"{side}_index": [70 * k, 0, 0], f"{side}_middle": [75 * k, 0, 0],
            f"{side}_thumb": [-20 * k, 0, 20 * k if side == "right" else -20 * k]}


def relax(side):
    return {f"{side}_fingers": [18, 0, 0], f"{side}_index": [10, 0, 0], f"{side}_middle": [14, 0, 0]}


def spread(side):
    return {f"{side}_fingers": [-18, 0, 0], f"{side}_index": [-24, 0, 0], f"{side}_middle": [-20, 0, 0],
            f"{side}_thumb": [0, 0, -30 if side == "right" else 30]}


def breathe(c, base, length, keys=4, head_turn=4.0, sway=None, extra=None):
    """A breathing idle: the chest lifts, the head drifts; `sway(u)` and `extra(u, b)` add per-key poses (u in 0..1)."""
    for i in range(keys + 1):
        t = length * i / keys
        u = i / keys
        bb = math.sin(u * 2 * math.pi)
        p = P_(base, {"@body": [0, 0.12 * (1 - math.cos(u * 2 * math.pi)) * 0.5, 0],
                      "head": [base.get("head", [0, 0, 0])[0] + 1.2 * bb, base.get("head", [0, 0, 0])[1] + head_turn * math.sin(u * 2 * math.pi + 0.7),
                               base.get("head", [0, 0, 0])[2]]})
        if sway:
            p = P_(p, sway(u))
        if extra:
            p = P_(p, extra(u, bb))
        c.key(t, p, E_IO if i else None)


def walk(c, base, length, stride=20, arm=10, bob=0.45, lean=0, twist=5, knee=(6, 18), extra=None, arms=True):
    """A walk cycle: legs scissor, the knees bend through the passing pose, the body bobs up on the passing pose; `extra(s)`
    adds per-step poses (s = +1 / -1)."""
    for i, (t, s) in enumerate(((0, 1), (length / 2, -1), (length, 1))):
        p = {"right_leg": [stride * s, 0, 0], "left_leg": [-stride * s, 0, 0],
             "right_shin": [knee[0] if s < 0 else knee[1], 0, 0], "left_shin": [knee[1] if s < 0 else knee[0], 0, 0],
             "body": [lean + base.get("body", [0, 0, 0])[0], twist * s, 0]}
        if arms:
            ra = base.get("right_arm", [0, 0, 0])
            la = base.get("left_arm", [0, 0, 0])
            p["right_arm"] = [ra[0] - arm * s, ra[1], ra[2]]
            p["left_arm"] = [la[0] + arm * s, la[1], la[2]]
        p = P_(base, p)
        if extra:
            p = P_(p, extra(s))
        c.key(t, p, E_IO if i else None)
    c.key(length / 4, P_(c.frames[0][1], {"@body": [0, bob, 0]}), E_OUT)
    c.key(3 * length / 4, P_(c.frames[1][1], {"@body": [0, bob, 0]}), E_OUT)


def recoil(c, base, length, hit, pose):
    """A hurt flinch: snaps to `pose` by the hit tick, shudders, eases back."""
    c.key(0, base)
    c.key(hit, P_(base, pose), "easeOutQuad")
    c.key(min(length * 0.45, hit + 0.15), P_(base, pose, {"head": [v * 0.8 for v in pose.get("head", [0, 0, 0])]}))
    c.key(length, base, E_IO)


def jit(seed, i, amp):
    return [amp * (2 * h01(seed, i, k) - 1) for k in range(3)]


# =====================================================================================================
# Trophy busts (on the trophies' shared pedestal, like alastair_trophy)
# =====================================================================================================

def tex16(fn):
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def bust(name, face, hair, skin, chest, coat, hair_cap=True, extra=(), extra_tex=None, shoulders_w=7.4, head_w=4.2, neck_tex=None):
    """A bust: `face`, `hair`, `skin`, `chest`, `coat` are (x, y) -> colour fns for 16x16 textures; `extra` more BM cubes,
    their textures in `extra_tex` (variable -> (x, y) fn)."""
    tex = {"face": face, "hair": hair, "skin": skin, "chest": chest, "coat": coat}
    tex.update(extra_tex or {})
    for n, fn in tex.items():
        save(tex16(fn), "block", f"{name}_trophy_{n}")
    full = [0, 0, 16, 16]
    hw = head_w / 2
    sw = shoulders_w / 2
    NK = "neck" if neck_tex else "skin"
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": "coat"}),
        BM.cube("chest", (8 - sw + 0.6, 3.4, 6.2), (8 + sw - 0.6, 7.6, 9.8), {"north": "chest", "*": "coat"}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("shoulders", (8 - sw, 6.0, 6.4), (8 + sw, 7.8, 9.6), {"*": "coat"}),
        BM.cube("neck", (7.2, 7.4, 7.2), (8.8, 8.8, 8.8), {"*": NK}),
        BM.cube("head", (8 - hw, 8.6, 8 - hw - 0.2), (8 + hw, 13.6, 8 + hw - 0.2), {"north": "face", "up": "hair", "south": "hair",
                                                                                   "east": "skin", "west": "skin"}, uv={"north": full}),
        BM.cube("nose", (7.7, 10.4, 8 - hw - 0.45), (8.3, 11.3, 8 - hw - 0.2), {"*": "skin"}),
    ]
    if hair_cap:
        e.append(BM.cube("hair_cap", (8 - hw - 0.15, 12.6, 8 - hw - 0.1), (8 + hw + 0.15, 13.8, 8 + hw + 0.15),
                         {"north": None, "down": None, "*": "hair"}))
    e += list(extra)
    textures = {"base": "trophy_base"}
    for n in tex:
        textures[n] = f"{name}_trophy_{n}"
    BM.write(f"{name}_trophy", BM.model(textures, e, "trophy_base"))


def face_fn(rows, lg, skin):
    def f(x, y):
        ch = rows[y][x] if y < len(rows) and x < len(rows[y]) else "s"
        v = lg.get(ch, skin)
        return v(x, y) if callable(v) else v
    return f


def xy(mat):
    """A face material (f, x, y, w, h) as a 16x16 (x, y) fn."""
    return lambda x, y: mat("north", x, y, 16, 16)


# =====================================================================================================
# Previews
# =====================================================================================================

def render(model, tex, clips, out_dir, name, poses, hidden=(), scale=4, views=("front", "three_quarter", "side", "back")):
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    by = {c.anim.name.split(".")[-1]: c for c in clips}
    paths = []
    for clip, tm in poses:
        p = os.path.join(out_dir, f"{name}_{clip}_{tm}.png")
        preview3d.render(model.rig, tex, p, pose=by[clip].pose_at(tm) if clip in by else None, hidden=hidden, scale=scale, views=views)
        paths.append(p)
    return paths


def lie(side=1, lift=3.0, face_down=False, extra=None):
    """Lying on the floor: the root tipped over about the feet (on her/his side, `side` +1 = onto the left, or face down), lifted
    by half the body's thickness so nothing sinks into the ground. Limbs slack."""
    if face_down:
        p = {"root": [-88, 0, 0], "@root": [0, lift, 0], "head": [-20, 10 * side, 0], "right_arm": [-160, 0, 10],
             "left_arm": [-20, 0, -30], "right_forearm": [-20, 0, 0], "left_forearm": [-30, 0, 0]}
    else:
        p = {"root": [0, 0, 86 * side], "@root": [0, lift, 0], "head": [0, 0, -18 * side], "body": [10, 0, 0],
             "right_arm" if side > 0 else "left_arm": [-30, 0, 10 * side],
             "left_arm" if side > 0 else "right_arm": [-160, 0, -10 * side],
             "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0], "right_leg": [-20, 0, 0], "left_leg": [-6, 0, 0],
             "right_shin": [36, 0, 0], "left_shin": [20, 0, 0]}
    p.update(relax("right"))
    p.update(relax("left"))
    p.update(extra or {})
    return p


def stack(paths, out):
    """Stacks preview PNGs vertically into one (for review)."""
    from pngio import read_png, write_png
    imgs = [read_png(p) for p in paths]
    W = max(w for w, _, _ in imgs)
    rows = []
    for w, h, r in imgs:
        for y in range(h):
            rows.append(list(r[y]) + [(30, 30, 34, 255)] * (W - w))
    write_png(out, rows)
    return out


def move_cubes(m, src, dst_name):
    """A new bone `dst_name` under `src` with the same pivot, taking all of src's cubes (so clips can key the child while the
    renderer owns the named parent)."""
    sb = m.rig.get(src)
    d = m.bone(dst_name, sb.pivot, src)
    d.cubes, sb.cubes = sb.cubes, []
    return d
