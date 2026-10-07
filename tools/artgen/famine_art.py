"""Famine, the Black Horseman (v0.11): a frail, ancient man -- bald and liver-spotted, white wisps over the ears,
sunken eyes -- in a buttoned oatmeal cardigan over a white shirt, slacks and slippers, the Ring of Famine on his right
hand. Phase 1 he sits in a wheelchair (`wheelchair`, a top-level group the code hides once he stands); he devours
souls through a mouth that gapes open (`mouth`, scaled by the clips; its inside glows).

Clips (animation.famine.*): the common set plus wheel_idle (loop, seated), wheel_roll (loop, seated, pushing the
wheels: extra, for moving in phase 1), stand_up, devour, grab, drain (loop), mounted_grab.
"""

import math

import horsemen_art as H
import steed_art as ST
from chuck_art import cloth, h01, none_on, solid
from hell_art import FAMINE_GEM
from horsemen_art import CHAIR_LEGS, E_BACK, E_IN, E_IO, E_OUT, P
from pixelkit import Ramp, fbm, hexc, mix, shade

SKIN = Ramp("#5a4038", "#86655a", "#a8857a", "#c3a296", "#d6b8ac", "#e6cdc2")
SPOT = hexc("#8a6248")
WISP = Ramp("#8f8c86", "#aaa79f", "#c2bfb8", "#d6d3cc", "#e6e3dd", "#f4f2ee")
KNIT = Ramp("#4e4536", "#6c614d", "#8a7e66", "#a59980", "#bdb299", "#d3c9b2")
SLACKS = Ramp("#24221f", "#33302b", "#433f38", "#544f46", "#666056", "#797267")
SLIPPER = Ramp("#2a1a10", "#3c2617", "#4f331f", "#634128", "#775033", "#8c613f")
CHROME = Ramp("#2a2c30", "#4a4e55", "#70757e", "#9aa0a9", "#c4c9d0", "#eef1f4")
VINYL = Ramp("#07080b", "#0e1015", "#151820", "#1d212b", "#272c38", "#323846")
TYRE = Ramp("#050505", "#0b0b0c", "#121214", "#1a1a1d", "#232327", "#2e2e33")
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")

FACE = [
    "sslsssssssslssss",
    "ssssssslssssssss",
    "ssssssssssssssss",
    "ssrrrrssssrrrrss",
    "ssssssssssssssss",
    "hBBBBBssssBBBBBh",
    "hddddddssddddddh",
    "hdwipdssssdpiwdh",
    "hsddddssssddddsh",
    "sssssssnnsssssss",
    "skkssnNNNNnsskks",
    "skksssssssssskks",
    "sskrssmmmmssrkss",
    "ssssrssMMssrssss",
    "ssssssssssssssss",
    "sssssrrrrrrsssss",
]

SEATED = P(CHAIR_LEGS, {"body": [10, 0, 0], "head": [-6, 0, 0], "right_arm": [-22, 0, 12], "right_forearm": [-52, 0, 0],
                        "left_arm": [-22, 0, -12], "left_forearm": [-52, 0, 0], "@rider": [0, 0, 0.8]})


class Famine(H.Horseman):
    id = "famine"
    steed = "famine"
    stride = 0.55
    walk_period = 1.6
    hunch = 16.0
    breathe = 1.4
    STANCE = {"right_arm": [-14, 0, 6], "right_forearm": [-24, 0, 0], "left_arm": [-14, 0, -6], "left_forearm": [-24, 0, 0],
              "right_leg": [-4, 0, 0], "left_leg": [-4, 0, 0], "right_shin": [8, 0, 0], "left_shin": [8, 0, 0], "@rider": [0, -0.4, 0]}

    def arm_swing(self, side):
        return 10.0

    def build(self, m):
        glow = H.glow_faint(0.14)
        skin = H.skin_mat(SKIN, 3201, 3.0, 0.9, blotch=SPOT, blotch_p=0.05)
        wisp = lambda f, x, y, w, h: None if h01("wisp", f, x, y) < 0.25 else WISP[2.5 + (fbm(x * 3, y, 3202, 32, 32, 2, 8.0) - 0.5) * 2.5]
        legend = {"s": skin, "l": SPOT, "r": SKIN[1.8], "B": WISP[4], "h": lambda f, x, y, w, h: WISP[3],
                  "d": mix(SKIN[2], hexc("#6a4a52"), 0.3), "w": hexc("#d8d0b8"), "i": hexc("#7f8e98"), "p": hexc("#202020"),
                  "k": SKIN[2], "n": SKIN[2], "N": SKIN[1], "m": hexc("#3a1c1a"), "M": hexc("#8a5a52")}

        def sides(face, x, y, w, h):
            if face in ("up", "down"):
                return skin(face, x, y, w, h)
            fx = x if face == "west" else (w - 1 - x if face == "east" else 8)
            if face == "south":
                return WISP[3] if 5 <= y <= 11 and h01("bk", x, y) > 0.2 else skin(face, x, y, w, h)
            if 4 <= y <= 11 and fx > 9:
                return WISP[3 + (x + y) % 2]
            if fx in (7, 8, 9) and 6 <= y <= 11:
                return shade(skin(face, x, y, w, h), 0.85 if fx == 8 else 1.05)  # big old ears
            return skin(face, x, y, w, h)
        H.head_cube(m, FACE, legend, sides, glow=glow)
        # Wisps standing off the sides of the skull, long ears, a long nose.
        for s in (-1, 1):
            m.cube("head", (3.6 * s - 0.5, 27, -0.5), (1, 3.5, 4.5), none_on(("up", "down"), wisp), glow=glow, inflate=0.15,
                   density=2, tag="wisp")
            m.cube("head", (4.0 if s > 0 else -4.6, 26, -1), (0.6, 3, 2), solid(SKIN[2]), glow=glow, density=2, tag="ear")
        m.cube("head", (-0.5, 26.5, -4.75), (1, 2.5, 0.75), solid(SKIN[3]), glow=glow, density=2, tag="nose")
        # The mouth: a dark slot the clips scale open when he feeds; inside, a faint pale glow.
        m.bone("mouth", (0, 26, -4.05), "head")
        m.cube("mouth", (-1.0, 25.75, -4.15), (2, 0.5, 0.1), solid(hexc("#1a0a0a")),
               glow=lambda f, x, y, w, h, col: hexc("#9a8a6a"), density=2, faces=("north",), tag="mouth")
        m.cube("body", (-1.25, 23.5, -1.25), (2.5, 1, 2.5), skin, glow=glow, density=2, tag="neck")
        # Shirt and cardigan (ribbed knit, buttons, pockets), slacks, slippers.
        shirt = cloth(H.SHIRT_WHITE, 3203, 3.4, 0.4, 0.1)
        m.cube("body", (-3.5, 12, -1.75), (7, 12, 3.5), shirt, glow=glow, density=2, tag="torso")

        def knit(face, x, y, w, h):
            n = fbm(x * 2 + len(face), y * 2, 3204, 64, 64, 2, 10.0)
            c = KNIT[2.8 + (n - 0.5) * 0.8 + (0.35 if x % 2 == 0 else -0.15)]
            if face in ("north", "south", "east", "west") and y >= h - 2:
                c = shade(c, 0.85)  # ribbed hem
            return c
        front = H.jacket_front(knit, KNIT[4], shirt, gap=(5, 8), v_depth=9, buttons=hexc("#5a4632"), button_rows=(10, 13, 16, 19))

        def cardigan(face, x, y, w, h):
            if face == "north" and y > 13 and ((2 <= x <= 4) or (w - 5 <= x <= w - 3)) and y in (14, 18):
                return KNIT[1]  # pocket tops
            return front(face, x, y, w, h)
        m.cube("body", (-3.5, 11, -1.75), (7, 13, 3.5), none_on(("down",), cardigan), glow=glow, inflate=0.45, density=2, tag="cardigan")
        m.cube("body", (-3.5, 22.5, -1.75), (7, 1.5, 3.5), none_on(("down",), lambda f, x, y, w, h: None if f in ("north", "up") and 4 <= x <= 9
                                                       else H.SHIRT_WHITE[4]), glow=glow, inflate=0.6, density=2, tag="collar")
        hand = H.skin_mat(SKIN, 3205, 2.9, 1.0, blotch=SPOT, blotch_p=0.08)
        for side in ("right", "left"):
            H.arm_cubes(m, side, knit, hand, w=3.0, glow=glow, cuff=lambda f, x, y, w, h: KNIT[1], sleeve_inflate=0.35)
            H.leg_cubes(m, side, cloth(SLACKS, 3206, 2.7, 0.4, 0.1), lambda f, x, y, w, h: SLIPPER[2 + (1 if f == "up" else 0)],
                        w=3.5, glow=glow)
        H.ring_cube(m, "right", FAMINE_GEM[3], GOLD[3])
        self.wheelchair(m, glow)

    def wheelchair(self, m, glow):
        m.bone("wheelchair", (0, 0, 0), "root")
        m.bone("wheelchair_wheel_r", (-6, 6, 1), "wheelchair")
        m.bone("wheelchair_wheel_l", (6, 6, 1), "wheelchair")
        chrome = lambda f, x, y, w, h: CHROME[3.2 + (1.2 if f == "up" else 0) - (0.8 if f in ("down", "south") else 0)]
        vinyl = lambda f, x, y, w, h: VINYL[2.6 + (fbm(x, y, 3210, 32, 32, 2, 6.0) - 0.5) + (0.5 if y == 0 else 0)]
        c = lambda origin, size, mat, tag: m.cube("wheelchair", origin, size, mat, glow=glow, density=2, tag=tag)
        c((-4.5, 9, -3.5), (9, 1, 7.5), vinyl, "seat")
        c((-4.5, 10, 3), (9, 10, 0.75), vinyl, "backrest")
        for s in (-1, 1):
            c((4.75 * s - 0.25, 1, 3.75), (0.5, 21, 0.5), chrome, "back_post")
            c((4.75 * s - 0.25, 21.5, 3.75), (0.5, 0.5, 2.5), chrome, "push_handle")
            c((4.75 * s - 0.4, 21.3, 5.75), (0.8, 0.9, 1.2), solid(TYRE[3]), "grip")
            c((4.75 * s - 0.25, 8.5, -4), (0.5, 0.5, 8), chrome, "seat_rail")
            c((4.75 * s - 0.25, 9, -3.5), (0.5, 5, 0.5), chrome, "arm_post")
            c((4.75 * s - 0.5, 13.5, -3.5), (1, 0.75, 6.5), vinyl, "armrest")
            c((3.5 * s - 0.25, 1, -4.5), (0.5, 8, 0.5), chrome, "front_post")
            c((3.5 * s - 0.5, 0, -5.5), (1, 2, 2), lambda f, x, y, w, h: TYRE[2], "caster")
            c((3.5 * s - 0.25, 1.5, -9), (0.5, 0.5, 5), chrome, "leg_rest")
        c((-3.75, 0.75, -9.5), (7.5, 0.5, 3), lambda f, x, y, w, h: CHROME[2] if f != "up" else CHROME[3 + (x % 3 == 0)], "footrest")

        def wheel(face, x, y, w, h):
            if face not in ("east", "west"):
                return None
            cx, cy = (w - 1) / 2, (h - 1) / 2
            d = math.hypot(x - cx, y - cy)
            R = w / 2
            if d > R:
                return None
            if d > R - 1.6:
                return TYRE[1 + (1 if d < R - 1 else 0)]
            if d > R - 2.6:
                return CHROME[4]  # rim
            if d < 1.6:
                return CHROME[3]  # hub
            ang = math.atan2(y - cy, x - cx)
            if min(abs(((ang * 12 / (2 * math.pi)) % 1) - 0.5), 0.5) > 0.38:
                return CHROME[2]  # spokes
            return None
        for side, s in (("r", -1), ("l", 1)):
            m.cube(f"wheelchair_wheel_{side}", (6 * s - 0.5, 0, -5), (1, 12, 12), wheel, glow=glow, density=2, tag="wheel")
            m.cube(f"wheelchair_wheel_{side}", (6 * s + 0.6 * s - 0.25, 1.5, -3.5), (0.5, 9, 9),
                   lambda f, x, y, w, h: (CHROME[4] if f in ("east", "west") and abs(math.hypot(x - (w - 1) / 2, y - (h - 1) / 2) - w / 2 + 0.7) < 0.8
                                          else None), glow=glow, density=2, tag="hand_rim")

    # --- clips -----------------------------------------------------------------------------------

    def mouth_open(self, anim, keys):
        """keys: [(t, openness 0..1)] -> scale of `mouth` (the slot grows tall and a little wide)."""
        anim.scale("mouth", *[(round(t, 4), [1 + 0.6 * k, 1 + 7 * k, 1]) for t, k in keys])

    def seated_idle(self, name, length, roll=False):
        c = self.clip(name, length, loop=True)
        n = 8
        for i in range(n + 1):
            t = length * i / n
            ph = 2 * math.pi * t / length
            k = math.sin(ph)
            tremor = 1.5 * math.sin(ph * 6)
            pose = P(SEATED, {"body": [1.2 * k, 0, 0], "head": [-2 * k + 1.5 * math.sin(ph * 3), 10 * math.sin(ph + 0.6), 0],
                              "right_hand": [tremor, 0, 0], "left_hand": [-tremor, 0, 0]})
            if roll:
                push = math.sin(ph)
                pose = P(pose, {"right_arm": [-18 * push - 8, 0, 6], "left_arm": [-18 * push - 8, 0, -6],
                                "right_forearm": [10 * push, 0, 0], "left_forearm": [10 * push, 0, 0], "body": [4 * max(0, push), 0, 0]})
            c.key(t, pose)
        a = c.done()
        if roll:
            a.rot("wheelchair_wheel_r", (0, [0, 0, 0]), (length, [-360, 0, 0]))
            a.rot("wheelchair_wheel_l", (0, [0, 0, 0]), (length, [-360, 0, 0]))
        return c

    def clips(self):
        st = self.stance()
        A = self.clip
        self.seated_idle("wheel_idle", 4.0)
        self.seated_idle("wheel_roll", 1.6, roll=True)

        # intro (in the wheelchair): asleep, chin on chest; he lifts his head, a slow terrible smile, a beckoning finger.
        c = A("intro", 3.6)
        asleep = P(SEATED, {"head": [32, 0, 0], "body": [16, 0, 0]})
        c.key(0, asleep)
        c.key(1.2, P(SEATED, {"head": [10, 0, 0], "body": [12, 0, 0]}), E_IO)
        c.key(1.8, P(SEATED, {"head": [-6, -12, 0]}), E_IO)
        beckon = P(SEATED, {"head": [-4, 0, 6], "right_arm": [-70, 10, 6], "right_forearm": [-30, 0, 0], "right_hand": [-20, 0, 0]})
        c.key(2.4, beckon, E_OUT)
        c.key(2.7, P(beckon, {"right_hand": [30, 0, 0]}), E_IO)
        c.key(3.0, P(beckon, {"right_hand": [-25, 0, 0]}), E_IO)
        c.key(3.6, P(beckon, {"right_hand": [20, 0, 0]}), E_IO)
        c.done()

        # stand_up: grips the armrests, shakes, pushes himself up out of the chair and straightens (crack of the back).
        c = A("stand_up", 2.6)
        push = P(SEATED, {"body": [30, 0, 0], "head": [-20, 0, 0], "right_arm": [10, 0, 14], "left_arm": [10, 0, -14],
                          "right_forearm": [-30, 0, 0], "left_forearm": [-30, 0, 0]})
        half = P(st, {"body": [40, 0, 0], "head": [-30, 0, 0], "right_leg": [-50, 0, 0], "left_leg": [-50, 0, 0],
                      "right_shin": [60, 0, 0], "left_shin": [60, 0, 0], "right_arm": [10, 0, 14], "left_arm": [10, 0, -14],
                      "@rider": [0, -2.5, -0.5]})
        c.key(0, SEATED)
        c.key(0.5, push, E_IO)
        for i, t in enumerate((0.6, 0.7, 0.8, 0.9)):
            c.key(t, P(push, {"body": [(1.5 if i % 2 else -1.5), 0, 0], "head": [(-2 if i % 2 else 2), 0, 0]}))
        c.key(1.5, half, E_IN)
        c.key(2.1, P(st, {"body": [-6, 0, 0], "head": [-14, 0, 0], "right_arm": [-10, 0, 20], "left_arm": [-10, 0, -20]}), E_OUT)
        c.key(2.6, st, E_IO)
        c.done()

        # transition: a fit of hunger: doubles over clutching his stomach, then rises taller, arms spread.
        c = A("transition", 2.4)
        doubled = P(st, {"body": [40, 0, 0], "head": [10, 0, 0], "right_arm": [-60, 30, -20], "left_arm": [-60, -30, 20],
                         "right_forearm": [-70, 0, 0], "left_forearm": [-70, 0, 0], "@rider": [0, -1.5, 0],
                         "right_leg": [-20, 0, 0], "left_leg": [-20, 0, 0], "right_shin": [25, 0, 0], "left_shin": [25, 0, 0]})
        tall = P(st, {"body": [-24, 0, 0], "head": [-30, 0, 0], "right_arm": [-60, 0, 70], "left_arm": [-60, 0, -70],
                      "right_forearm": [-10, 0, 0], "left_forearm": [-10, 0, 0]})
        c.key(0, st)
        c.key(0.6, doubled, E_IN)
        c.key(1.1, P(doubled, {"body": [44, 0, 0]}), E_IO)
        c.key(1.6, tall, E_OUT)
        c.key(2.4, st, E_IO)
        a = c.done()
        self.mouth_open(a, [(0, 0), (1.3, 0), (1.6, 1), (2.0, 1), (2.4, 0)])

        # devour: head back, mouth agape, arms out with palms up: the souls stream into him.
        c = A("devour", 2.6)
        feed = P(st, {"body": [-20, 0, 0], "head": [-40, 0, 0], "right_arm": [-50, 20, 45], "left_arm": [-50, -20, -45],
                      "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0], "right_hand": [0, 0, 30], "left_hand": [0, 0, -30]})
        c.key(0, st)
        c.key(0.5, feed, E_OUT)
        for i, t in enumerate((0.8, 1.1, 1.4, 1.7, 2.0)):
            c.key(t, P(feed, {"head": [(-4 if i % 2 else 3), 0, 0], "body": [(-2 if i % 2 else 1), 0, 0]}), E_IO)
        c.key(2.6, st, E_IO)
        a = c.done()
        self.mouth_open(a, [(0, 0), (0.5, 1), (2.1, 1), (2.6, 0)])

        # grab: a sudden lunge, the right hand closes on something and drags it to his mouth.
        c = A("grab", 1.6)
        reach = P(st, {"body": [30, -10, 0], "head": [-24, 0, 0], "right_arm": [-100, -10, -6], "right_forearm": [0, 0, 0],
                       "right_hand": [0, 0, 0], "left_arm": [-20, 0, -20], "right_leg": [-30, 0, 0], "left_leg": [20, 0, 0],
                       "left_shin": [20, 0, 0], "@rider": [0, -1, -2.5]})
        to_mouth = P(st, {"body": [6, 10, 0], "head": [-16, 0, 0], "right_arm": [-70, 30, 30], "right_forearm": [-80, 0, 0],
                          "right_hand": [-20, 0, 0]})
        c.key(0, st)
        c.key(0.25, reach, E_OUT)
        c.key(0.45, P(reach, {"right_hand": [40, 0, 0]}), E_IO)
        c.key(0.9, to_mouth, E_IO)
        c.key(1.6, st, E_IO)
        a = c.done()
        self.mouth_open(a, [(0, 0), (0.7, 0), (0.9, 1), (1.2, 1), (1.4, 0)])

        # drain (loop): leaning in, one hand clawed out at the victim, trembling; mouth open, inhaling.
        c = A("drain", 1.2, loop=True)
        base = P(st, {"body": [24, 0, 0], "head": [-22, 0, 0], "right_arm": [-90, 0, 4], "right_forearm": [-8, 0, 0],
                      "right_hand": [30, 0, 0], "left_arm": [-40, 0, -14], "left_forearm": [-40, 0, 0], "right_leg": [-14, 0, 0],
                      "left_leg": [10, 0, 0]})
        for i in range(7):
            t = 1.2 * i / 6
            k = math.sin(2 * math.pi * t / 1.2 * 3)
            c.key(t, P(base, {"right_arm": [k * 1.5, k * 1.2, 0], "right_hand": [k * 4, 0, 0], "body": [math.sin(2 * math.pi * t / 1.2) * 2, 0, 0],
                              "head": [math.sin(2 * math.pi * t / 1.2) * -3, 0, 0]}))
        a = c.done()
        self.mouth_open(a, [(0, 0.8), (0.6, 1), (1.2, 0.8)])

        # mounted_grab: at the gallop he leans out over his horse's right shoulder, snatches and lifts high.
        lean_out = {"body": [24, 0, 22], "right_arm": [-60, 0, 50], "right_forearm": [0, 0, 0], "right_hand": [30, 0, 0],
                    "head": [-10, 0, 10]}
        lift = {"body": [-6, 0, 0], "right_arm": [-160, 0, 20], "right_forearm": [-20, 0, 0], "right_hand": [40, 0, 0], "head": [-28, 0, 0]}
        a = self.mounted_action("mounted_grab", 1.8, lambda t: ST.gallop_pose(t, 0.6, 0.9), [
            (0, {}, None), (0.4, lean_out, None), (0.6, lean_out, None), (1.0, lift, None), (1.4, lift, None), (1.8, {}, None)]).anim
        self.mouth_open(a, [(0, 0), (1.0, 0), (1.2, 1), (1.5, 1), (1.8, 0)])

        self.death()

    def charge_raise(self):
        return {"right_arm": [-120, 0, 40], "left_arm": [-120, 0, -40], "right_forearm": [-20, 0, 0], "left_forearm": [-20, 0, 0],
                "head": [-20, 0, 0]}

    def charge_level(self):
        return {"right_arm": [-90, 0, 14], "left_arm": [-90, 0, -14], "right_hand": [30, 0, 0], "left_hand": [30, 0, 0], "body": [16, 0, 0]}


def generate():
    Famine().generate()
