"""The Four Horsemen (v0.11): the shared human rig, materials and clip framework.

Each Horseman's module (war_art, famine_art, pestilence_art, death_art) builds one `chuck_art.Model` with this
skeleton plus his own clothes and props, his steed under a top-level `steed` group (steed_art.build_steed, hidden by
the renderer until he mounts), and writes `animation.<id>.*`.

Skeleton (Bedrock px; +X is the entity's LEFT; the model faces north):
  root -- rider -- body -- head
                        -- right_arm -- right_forearm -- right_hand   (props hang from the hands)
                        -- left_arm  -- left_forearm  -- left_hand
                -- right_leg -- right_shin,  left_leg -- left_shin
       -- steed -- steed_body -- ... (see steed_art)
`rider` carries the whole man: the mounted clips lift it into the saddle (steed_art.seat_follow keeps it there
while the horse moves). Contract groups (`steed`, `wheelchair`, `scythe`, `cane`) are never keyed themselves.
"""

import math

import steed_art as ST
from animkit import AnimFile
from chuck_art import Model, cloth, h01, hair_mat, none_on, solid, tone
from common import ASSETS, save
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"

E_IO, E_OUT, E_IN, E_BACK = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack"
COMMON = ("idle", "walk", "intro", "transition", "death", "mount", "mounted_idle", "mounted_gallop", "mounted_charge")

SHIRT_WHITE = Ramp("#8d939b", "#a9afb7", "#c2c8cf", "#d6dbe1", "#e6eaee", "#f4f6f8")
BLACK = Ramp("#050506", "#0b0b0e", "#121216", "#1a1a20", "#24242b", "#303038")
EYE_WHITE = hexc("#e6ddd2")


# --- the skeleton -----------------------------------------------------------------------------------

def skeleton(m):
    m.bone("root", (0, 0, 0))
    m.bone("rider", (0, 0, 0), "root")
    m.bone("body", (0, 12, 0), "rider")
    m.bone("head", (0, 24, 0), "body")
    for side, s in (("right", -1), ("left", 1)):
        m.bone(f"{side}_arm", (5 * s, 22, 0), "body")
        m.bone(f"{side}_forearm", (6 * s, 18, 0), f"{side}_arm")
        m.bone(f"{side}_hand", (6 * s, 13.5, 0), f"{side}_forearm")
        m.bone(f"{side}_leg", (2 * s, 12, 0), "rider")
        m.bone(f"{side}_shin", (2 * s, 6, 0), f"{side}_leg")


def head_cube(m, face_rows, legend, side_mat, glow=None, size=8, y0=24, inflate=0.0):
    """The head: an 8-px cube at density 2; its north face is drawn from 16 rows of legend characters."""
    def mat(face, x, y, w, h):
        if face == "north":
            ch = face_rows[y][x] if y < len(face_rows) and x < len(face_rows[y]) else "s"
            v = legend.get(ch)
            if v is None:
                v = legend["s"]
            return v(face, x, y, w, h) if callable(v) else v
        return side_mat(face, x, y, w, h)
    h = size / 2
    return m.cube("head", (-h, y0, -h), (size, size, size), mat, glow=glow, density=2, inflate=inflate, tag="head")


def arm_cubes(m, side, sleeve, hand, w=4.0, glow=None, cuff=None, sleeve_inflate=0.0):
    s = -1 if side == "right" else 1
    cx = 6 * s
    m.cube(f"{side}_arm", (cx - w / 2, 17.5, -2), (w, 6.5, 4), sleeve, glow=glow, density=2, inflate=sleeve_inflate, tag="upper_arm")
    m.cube(f"{side}_forearm", (cx - w / 2, 13.5, -2), (w, 4.5, 4), sleeve, glow=glow, density=2, inflate=sleeve_inflate * 0.9,
           tag="forearm")
    if cuff is not None:
        m.cube(f"{side}_forearm", (cx - w / 2, 13.5, -2), (w, 1, 4), none_on(("up", "down"), cuff), glow=glow, density=2,
               inflate=sleeve_inflate + 0.12, tag="cuff")
    hw = w - 1
    m.cube(f"{side}_hand", (cx - hw / 2, 11, -1.5), (hw, 2.5, 3), hand, glow=glow, density=2, tag="hand")


def leg_cubes(m, side, trousers, shoe, w=4.0, glow=None, shoe_h=1.5):
    s = -1 if side == "right" else 1
    cx = 2 * s
    m.cube(f"{side}_leg", (cx - w / 2 + (-0.05 * s), 6, -2), (w, 6, 4), trousers, glow=glow, density=2, tag="thigh")
    m.cube(f"{side}_shin", (cx - w / 2 + (-0.05 * s), shoe_h, -2), (w, 6 - shoe_h, 4), trousers, glow=glow, density=2, tag="shin")
    m.cube(f"{side}_shin", (cx - w / 2 - 0.1 + (-0.05 * s), 0, -2.6), (w + 0.2, shoe_h, 4.6), shoe, glow=glow, density=2, tag="shoe")


# --- materials --------------------------------------------------------------------------------------

def skin_mat(ramp, seed, base=3.1, amp=0.8, blotch=None, blotch_p=0.0, sores=0.0):
    def m(face, x, y, w, h):
        n = fbm(x + len(face) * 3, y, seed, 64, 64, 2, 7.0)
        c = ramp[base + (n - 0.5) * amp]
        r = h01("skin", seed, face, x, y)
        if blotch is not None and r < blotch_p:
            c = mix(c, blotch, 0.45)
        if sores and fbm(x * 3 + 5, y * 3 + len(face), seed + 7, 64, 64, 2, 9.0) > 1 - sores:
            c = mix(hexc("#7a2c22"), hexc("#b8a640"), 0.35 if r < 0.5 else 0.0)
        return c
    return m


def suit_mat(ramp, seed, base=2.7, pinstripe=None, period=4):
    base_m = cloth(ramp, seed, base, 0.35, 0.08)

    def m(face, x, y, w, h):
        c = base_m(face, x, y, w, h)
        if pinstripe is not None and face in ("north", "south", "east", "west") and x % period == 0:
            c = mix(c, pinstripe, 0.25)
        return c
    return m


def jacket_front(material, lapel, shirt, tie=None, gap=(6, 9), v_depth=10, buttons=None, button_rows=(12, 16)):
    """A suit jacket's north face at density 2: a V opening down to `v_depth` rows over `shirt`, lapels, buttons."""
    def m(face, x, y, w, h):
        if face == "north":
            mid = (w - 1) / 2
            half = (gap[1] - gap[0]) / 2 * max(0.0, 1 - y / max(1, v_depth))
            if y < v_depth and abs(x - mid) <= half + 0.5:
                if tie is not None and abs(x - mid) <= 1.0 and y >= 1:
                    return tie(face, x, y, w, h)
                return shirt(face, x, y, w, h)
            if y < v_depth + 1 and abs(x - mid) <= half + 2.0:
                return lapel
            if buttons is not None and abs(x - mid) <= 0.5 and y in button_rows:
                return buttons
            if y >= v_depth and abs(x - mid) < 0.5 and y < h - 1:
                return shade(material(face, x, y, w, h), 0.8)  # the closing seam
        return material(face, x, y, w, h)
    return m


def glow_faint(f):
    return lambda face, x, y, w, h, col: shade(col, f)


# --- clip framework ---------------------------------------------------------------------------------

class Clip:
    """Pose keyframes: key(t, pose, ease). A pose maps bone -> [rx, ry, rz]; "@bone" -> position.
    Every bone named in any key is keyed at every key time (absent = rest), so poses blend predictably."""

    def __init__(self, f, name, length, loop=False, hold=False):
        self.anim = f.new(name, length, loop=loop, hold=hold)
        self.frames = []

    def key(self, t, pose, ease=None):
        self.frames.append((t, pose, ease))
        return self

    def done(self, skip=()):
        names = []
        for _, p, _ in self.frames:
            for k in p:
                if k not in names and k.lstrip("@") not in skip:
                    names.append(k)
        for k in names:
            keys = []
            for t, p, e in self.frames:
                v = [round(float(c), 3) for c in p.get(k, [0, 0, 0])]
                keys.append((round(t, 4), v, e) if e else (round(t, 4), v))
            if k.startswith("@"):
                self.anim.pos(k[1:], *keys)
            else:
                self.anim.rot(k, *keys)
        return self.anim


P = ST.add_pose
L = ST.lerp_pose


def scale_pose(p, k):
    return {b: [c * k for c in v] for b, v in p.items()}


# Seated in a saddle: thighs forward and splayed round the barrel, shins hanging to the stirrups.
SADDLE_LEGS = {
    "right_leg": [-50, 6, 27], "right_shin": [46, 0, -12],
    "left_leg": [-50, -6, -27], "left_shin": [46, 0, 12],
}
# Seated on a chair: thighs level, shins straight down.
CHAIR_LEGS = {"right_leg": [-88, 0, 3], "right_shin": [86, 0, 0], "left_leg": [-88, 0, -3], "left_shin": [86, 0, 0]}
# Both hands on the reins in front of the pommel.
REINS = {"right_arm": [-38, 8, 4], "right_forearm": [-34, 0, 0], "left_arm": [-38, -8, -4], "left_forearm": [-34, 0, 0]}


def mounted_rider(steed_pose, lean=0.0):
    pos, rot = ST.seat_follow(steed_pose, lean)
    return {"@rider": pos, "rider": rot}


class Horseman:
    """Subclasses set `id`, `steed` (steed_art scheme name) and implement build(m), STANCE, ARMED_MOUNT and
    their own clips. The common clips come from here, shaped by a few personality numbers."""

    id = "horseman"
    steed = "war"
    tex = (512, 256)
    stride = 1.0         # walk amplitude
    walk_period = 1.2
    hunch = 0.0          # degrees the body leans forward at rest
    breathe = 1.0
    tails = False        # Death's coat skirt bones

    STANCE = {}          # rest pose layered on idle/walk
    ARMED_MOUNT = {}     # arm pose while riding (replaces REINS for the weapon arm)

    def __init__(self):
        self.m = Model(self.id, *self.tex)
        skeleton(self.m)
        self.build(self.m)
        ST.build_steed(self.m, ST.SCHEMES[self.steed], parent="root")
        self.t, self.g = self.m.build(gutter=1, seed=1100 + sum(map(ord, self.id)))
        self.f = AnimFile()
        self.A = f"animation.{self.id}."

    def build(self, m):
        raise NotImplementedError

    def clip(self, name, length, loop=False, hold=False):
        return Clip(self.f, self.A + name, length, loop, hold)

    def stance(self):
        return P(self.STANCE, {"body": [self.hunch, 0, 0], "head": [-self.hunch * 0.7, 0, 0]})

    def tail_follow(self, pose):
        """Coat skirt halves follow the thighs at half strength (only for models with tails)."""
        if not self.tails:
            return pose
        out = dict(pose)
        for side, tail in (("right", "coat_tail_r"), ("left", "coat_tail_l")):
            lg = pose.get(f"{side}_leg", [0, 0, 0])
            out[tail] = [max(-60, lg[0] * 0.55), 0, lg[2] * 0.7]
        return out

    # --- common clips ---------------------------------------------------------------------------

    def idle(self, length=4.0):
        c = self.clip("idle", length, loop=True)
        st = self.stance()
        b = self.breathe
        for i, t in enumerate((0, length * 0.25, length * 0.5, length * 0.75, length)):
            k = math.sin(2 * math.pi * t / length)
            pose = P(st, {"body": [0.8 * b * k, 0, 0], "head": [-1.5 * b * k, 5 * math.sin(math.pi * t / length * 2 + 1.0), 0],
                          "right_arm": [-1.2 * b * k, 0, 1.5 * b * k], "left_arm": [-1.2 * b * k, 0, -1.5 * b * k],
                          "@body": [0, -0.15 * b * (k + 1) / 2, 0]})
            c.key(t, self.tail_follow(pose), E_IO if i else None)
        return c

    def walk(self):
        L_ = self.walk_period
        c = self.clip("walk", L_, loop=True)
        st = self.stance()
        a = self.stride
        n = 8
        for i in range(n + 1):
            t = L_ * i / n
            ph = 2 * math.pi * t / L_
            s = math.sin(ph)
            lift_r = max(0.0, math.sin(ph + math.pi / 2))
            lift_l = max(0.0, -math.sin(ph + math.pi / 2))
            pose = P(st, {
                "right_leg": [-28 * a * s, 0, 0], "left_leg": [28 * a * s, 0, 0],
                "right_shin": [26 * a * lift_r, 0, 0], "left_shin": [26 * a * lift_l, 0, 0],
                "right_arm": [self.arm_swing("right") * a * s, 0, 2], "left_arm": [-self.arm_swing("left") * a * s, 0, -2],
                "right_forearm": [-8 * a * max(0, s), 0, 0], "left_forearm": [-8 * a * max(0, -s), 0, 0],
                "body": [2 * a, 3 * a * s, 0], "head": [0, -3 * a * s, 0],
                "@rider": [0, -0.6 * a * abs(math.cos(ph)), 0],
            })
            c.key(t, self.tail_follow(pose))
        return c

    def arm_swing(self, side):
        return 24.0

    def mount(self, length=1.6):
        """He steps up and swings into the saddle; the horse braces and tosses its head as he lands."""
        c = self.clip("mount", length)
        st = self.stance()
        seated = ST.idle_pose(0)
        top = mounted_rider(seated)
        crouch = P(st, {"body": [18, 0, 0], "right_leg": [-30, 0, 0], "left_leg": [-30, 0, 0], "right_shin": [55, 0, 0],
                        "left_shin": [55, 0, 0], "@rider": [11, -3, 1], "right_arm": [20, 0, 10], "left_arm": [-40, 0, -20]})
        leap = P(self.armed_mount_arms(), {"body": [6, 0, -8], "right_leg": [-40, 0, 40], "left_leg": [-20, 0, -10],
                                         "right_shin": [30, 0, 0], "left_shin": [40, 0, 0],
                                         "@rider": [5, top["@rider"][1] + 4, 0.5], "rider": [0, 0, -10]})
        land = P(self.mounted_base(seated), {"body": [10, 0, 0], "@rider": [0, -0.8, 0]})
        c.key(0, self.tail_follow(P(st, {"@rider": [11, 0, 1]})))
        c.key(0.35, self.tail_follow(crouch), E_IO)
        c.key(0.8, self.tail_follow(leap), E_OUT)
        c.key(1.1, self.tail_follow(P(land, top)), E_IN)
        c.key(length, self.tail_follow(P(self.mounted_base(seated), top)), E_IO)
        c.done()

        def steed(t):
            if t < 1.05:
                return ST.lerp_pose(ST.idle_pose(t), {"steed_neck": [-6, 0, 0], "steed_ear_r": [-20, 0, 0], "steed_ear_l": [-20, 0, 0]},
                                    ST.smooth(t / 0.8))
            k = math.sin(min(1.0, (t - 1.05) / 0.5) * math.pi)
            return ST.add_pose(ST.idle_pose(t), {"steed_body": [3 * k, 0, 0], "@steed_body": [0, -0.8 * k, 0],
                                                 "steed_neck": [-14 * k, 6 * k, 0], "steed_head": [-12 * k, 0, 0],
                                                 "steed_jaw": [12 * k, 0, 0]})
        ST.sample(c.anim, steed, 0, length, 0.1)
        return c

    def armed_mount_arms(self):
        return P(REINS, self.ARMED_MOUNT)

    def mounted_base(self, steed_pose, lean=0.0):
        """Legs round the barrel and hands on the reins/weapon; `rider`/@rider follow the saddle."""
        return self.tail_follow(P(SADDLE_LEGS, self.armed_mount_arms(), {"body": [6 + lean, 0, 0], "head": [-6 - lean * 0.6, 0, 0]},
                                  self.mounted_extra()))

    def mounted_extra(self):
        return {}

    def mounted_loop(self, name, length, steed_fn, step, lean=0.0, bob=1.0, arm_keys=None):
        c = self.clip(name, length, loop=True)
        n = max(1, int(round(length / step)))
        for i in range(n + 1):
            t = length * i / n
            sp = steed_fn(t)
            base = self.mounted_base(sp, lean)
            ride = mounted_rider(sp, 0)
            # The rider absorbs the bounce a little late: a counter-swing of the torso and head.
            swing = sp.get("steed_body", [0, 0, 0])[0]
            pose = P(base, ride, {"body": [-swing * 0.6 * bob, 0, 0], "head": [swing * 0.5 * bob, 0, 0]})
            if arm_keys:
                pose = P(pose, arm_keys(t))
            c.key(t, pose)
        c.done()
        ST.sample(c.anim, steed_fn, 0, length, step)
        return c

    def mounted_idle(self):
        return self.mounted_loop("mounted_idle", 4.0, ST.idle_pose, 0.25, lean=0.0, bob=0.2,
                                 arm_keys=lambda t: {"body": [0.8 * math.sin(2 * math.pi * t / 4.0), 0, 0],
                                                     "head": [0, 8 * math.sin(2 * math.pi * t / 4.0 + 0.5), 0]})

    def mounted_gallop(self):
        return self.mounted_loop("mounted_gallop", 0.6, ST.gallop_pose, 0.05, lean=14, bob=1.0)

    def mounted_action(self, name, length, steed_fn, frames, step=0.05, lean=8.0):
        """A one-shot riding clip: the steed follows steed_fn, the rider keys `frames` [(t, arm/torso pose, ease)]
        over the riding base, and stays in the saddle."""
        c = self.clip(name, length)
        times = sorted(set([round(t, 4) for t, _, _ in frames] + [round(length * i / max(1, int(length / 0.1)), 4)
                                                                  for i in range(int(length / 0.1) + 1)]))
        fr = sorted(frames, key=lambda x: x[0])

        def acting(t):
            if t <= fr[0][0]:
                return fr[0][1]
            for (t0, a, _), (t1, b, _) in zip(fr, fr[1:]):
                if t0 <= t <= t1:
                    return ST.lerp_pose(a, b, ST.smooth((t - t0) / max(1e-6, t1 - t0)))
            return fr[-1][1]
        for t in times:
            sp = steed_fn(t)
            pose = P(self.mounted_base(sp, lean), mounted_rider(sp), acting(t))
            c.key(t, pose)
        c.done()
        ST.sample(c.anim, steed_fn, 0, length, step)
        return c

    def mounted_charge(self):
        """The steed rears and screams, then drives forward; the rider lifts his weapon and levels it."""
        L_ = 1.8

        def steed(t):
            if t < 0.9:
                k = ST.smooth(t / 0.35) if t < 0.6 else ST.smooth((0.9 - t) / 0.3)
                return ST.rear_pose(k * 0.8, t * 10)
            return ST.gallop_pose(t - 0.9, 0.6, 1.15, 1.1)
        raise_ = self.charge_raise()
        level = self.charge_level()
        return self.mounted_action("mounted_charge", L_, steed, [(0, {}, None), (0.35, raise_, None), (0.8, raise_, None),
                                                                 (1.05, level, None), (L_, level, None)], lean=10)

    def charge_raise(self):
        return {"right_arm": [-150, 0, -10], "right_forearm": [-10, 0, 0], "head": [-15, 0, 0]}

    def charge_level(self):
        return {"right_arm": [-80, 0, 10], "right_forearm": [-5, 0, 0], "body": [12, 0, 0]}

    def death(self, length=4.0):
        """Staggers, drops to his knees, slumps forward and lies still (held)."""
        c = self.clip("death", length, hold=True)
        st = self.stance()
        c.key(0, self.tail_follow(st))
        c.key(0.4, self.tail_follow(P(st, {"body": [-14, 0, 6], "head": [-25, 10, 0], "right_arm": [-30, 0, 30], "left_arm": [-20, 0, -40],
                                          "@rider": [0, 0, 1.5]})), E_OUT)
        c.key(1.3, self.tail_follow(P(st, {"body": [8, 0, -3], "head": [10, -6, 0], "right_arm": [10, 0, 6], "left_arm": [-30, 0, -10],
                                          "left_forearm": [-40, 0, 0], "right_leg": [-6, 0, 0], "left_leg": [-50, 0, 0], "left_shin": [60, 0, 0],
                                          "@rider": [0, -1.5, 0]})), E_IO)
        kneel = {"body": [14, 0, 0], "head": [20, 0, 0], "right_leg": [0, 0, 0], "left_leg": [0, 0, 0],
                 "right_shin": [90, 0, 0], "left_shin": [90, 0, 0], "right_arm": [-10, 0, 8], "left_arm": [-10, 0, -8],
                 "@rider": [0, -6, 0]}
        c.key(2.0, self.tail_follow(kneel), E_IN)
        c.key(2.6, self.tail_follow(P(kneel, {"body": [10, 0, 0], "head": [10, 0, 0]})), E_IO)
        fall = {"rider": [88, 0, 0], "@rider": [0, -2.5, -7], "body": [2, 0, 0], "head": [-10, 25, 0],
                "right_leg": [0, 0, 0], "left_leg": [0, 0, 0], "right_shin": [20, 0, 0], "left_shin": [35, 0, 0],
                "right_arm": [-160, 0, 15], "left_arm": [-10, 0, -50]}
        c.key(3.3, self.tail_follow(fall), E_IN)
        c.key(length, self.tail_follow(P(fall, {"@rider": [0, -2.8, -7]})), E_OUT)
        c.done()
        return c

    # --- everything -----------------------------------------------------------------------------

    def clips(self):
        raise NotImplementedError

    def anims(self):
        self.idle().done()
        self.walk().done()
        self.mount()
        self.mounted_idle()
        self.mounted_gallop()
        self.mounted_charge()
        self.clips()
        return self.f

    def generate(self):
        self.m.rig.write(GEO + f"{self.id}.geo.json")
        save(self.t, "entity", self.id)
        save(self.g, "entity", f"{self.id}_glowmask")
        self.anims().write(ANIM + f"{self.id}.animation.json")


def head_sides(skin, hair, top=4, back=12, temple=6, sideburn=8, beard=None, beard_from=11, ear=True):
    """Every face of the head but the front, at density 2 (16 texels a side): hair on top and to `top` rows down the
    sides, to `back` rows behind; sideburns; an ear; an optional beard/stubble from row `beard_from`."""
    def m(face, x, y, w, h):
        if face == "up":
            return hair(face, x, y, w, h) if hair else skin(face, x, y, w, h)
        if face == "down":
            return beard(face, x, y, w, h) if beard and y < 10 else skin(face, x, y, w, h)
        if face == "south":
            return hair(face, x, y, w, h) if hair and y < back else skin(face, x, y, w, h)
        fx = x if face == "west" else w - 1 - x  # 0 at the front
        if hair and (y < top or (fx > 10 and y < back) or (fx > temple and y < top + 3)):
            return hair(face, x, y, w, h)
        if hair and fx in (7, 8) and y < sideburn:
            return hair(face, x, y, w, h)
        if ear and fx in (8, 9, 10) and 6 <= y <= 10:
            c = skin(face, x, y, w, h)
            return shade(c, 0.8) if fx == 9 and 7 <= y <= 9 else shade(c, 1.05)
        if beard and y >= beard_from and fx < 9:
            return beard(face, x, y, w, h)
        return skin(face, x, y, w, h)
    return m


def stubble(skin, hair_ramp, seed, amount=0.45):
    def m(face, x, y, w, h):
        c = skin(face, x, y, w, h)
        return mix(c, hair_ramp[1], amount) if h01("stub", seed, face, x, y) < 0.55 else mix(c, hair_ramp[2], amount * 0.4)
    return m


def ring_cube(m, side, gem, band, glow=1.0):
    """A ring on the index finger: a band round the knuckles and its gem, both on `side`_hand."""
    s = -1 if side == "right" else 1
    cx = 6 * s
    m.cube(f"{side}_hand", (cx - 1.0, 11.2, -1.75), (2.0, 0.5, 0.5), solid(band), glow=glow_faint(0.3), density=4, tag="ring")
    m.cube(f"{side}_hand", (cx - 0.25, 11.1, -2.0), (0.5, 0.75, 0.5), solid(gem), glow=lambda f, x, y, w, h, col: col,
           density=4, tag="ring_gem")
