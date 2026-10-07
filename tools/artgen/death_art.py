"""Death, the Fourth Horseman (v0.11): tall and gaunt, a pale hollow-cheeked face under thin black hair slicked
back, a black suit, black shirt and slim black tie under a long black overcoat (its skirt halves `coat_tail_r/l`
follow the legs), the Ring of Death on his left hand. Phase 1 he carries a silver-knobbed cane (`cane`), from
phase 2 his scythe (`scythe`; the clips move its child `scythe_spin`, never the group). Rides the pale horse.

Clips (animation.death.*): the common set plus cane_strike, scythe_reap, scythe_throw, summon_reapers,
shadow_step, world_flip, mounted_reap.
"""

import math

import horsemen_art as H
import steed_art as ST
from chuck_art import cloth, hair_mat, none_on, solid
from hell_art import DEATH_GEM
from horsemen_art import E_BACK, E_IN, E_IO, E_OUT, P
from pixelkit import Ramp, fbm, hexc, mix, shade

SKIN = Ramp("#4e4c52", "#77757b", "#9c9a9f", "#b9b7bc", "#cfcdd2", "#e2e1e5")
HAIR = Ramp("#030304", "#08080a", "#0e0e11", "#151519", "#1e1e23", "#2a2a31")
SUIT = Ramp("#040405", "#09090b", "#0f0f12", "#16161a", "#1e1e23", "#28282e")
COAT = Ramp("#030304", "#070709", "#0c0c0f", "#121216", "#19191e", "#222228")
SHOE = Ramp("#020203", "#060607", "#0b0b0d", "#121214", "#1a1a1d", "#242428")
SILVER = Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff")
SNATH = Ramp("#070605", "#0f0c0a", "#181310", "#221b16", "#2d241d", "#3a2f26")

FACE = [
    "hhhhhhhhhhhhhhhh",
    "hhhhhhhhhhhhhhhh",
    "hhhsssssssssshhh",
    "hssssssssssssssh",
    "ssssssssssssssss",
    "ssbbbbssssbbbbss",
    "sddddddssdddddds",
    "sdwiipdssdpiiwds",
    "ssddddssssddddss",
    "sssssssnnsssssss",
    "kkssssnNNnsssskk",
    "kkksssssssssskkk",
    "kkkssmmmmmmsskkk",
    "skksssssssssskks",
    "ssssssssssssssss",
    "ssssssssssssssss",
]


class Death(H.Horseman):
    id = "death"
    steed = "death"
    stride = 0.8
    walk_period = 1.5
    hunch = 3.0
    breathe = 0.5
    tails = True
    # The cane planted beside his right foot, the left hand loose; very still.
    STANCE = {"right_arm": [-12, 0, 8], "right_forearm": [-16, 0, 0], "left_arm": [2, 0, -4], "left_forearm": [-10, 0, 0]}
    ARMED_MOUNT = {"right_arm": [-6, 0, 10], "right_forearm": [-4, 0, 0]}

    def arm_swing(self, side):
        return 6.0 if side == "right" else 16.0

    def build(self, m):
        m.bone("coat_tail_r", (-2, 12, 0), "rider")
        m.bone("coat_tail_l", (2, 12, 0), "rider")
        glow = H.glow_faint(0.16)
        skin = H.skin_mat(SKIN, 3401, 3.1, 0.7)
        hair = lambda f, x, y, w, h: HAIR[2 + (1.6 if (x * 3 + y) % 7 == 0 else 0) + (fbm(x, y * 3, 3402, 32, 32, 2, 8.0) - 0.5)]
        legend = {"h": hair, "s": skin, "b": HAIR[2], "d": mix(SKIN[1], hexc("#2a2430"), 0.45), "w": hexc("#cfcbc4"),
                  "i": hexc("#141214"), "p": hexc("#040404"), "n": SKIN[2], "N": SKIN[1], "k": SKIN[1.6],
                  "m": hexc("#4a3a40")}
        H.head_cube(m, FACE, legend, H.head_sides(skin, hair, top=3, back=12, temple=6, sideburn=6), glow=glow)
        m.cube("head", (-4, 30.5, -4), (8, 1.5, 8), none_on(("down",), lambda f, x, y, w, h: None if f == "north" and y > 0 else hair(f, x, y, w, h)),
               glow=glow, inflate=0.18, density=2, tag="hair")
        m.cube("head", (-0.5, 26.5, -4.6), (1, 2.5, 0.6), solid(SKIN[3]), glow=glow, density=2, tag="nose")
        m.cube("body", (-1.25, 23.5, -1.25), (2.5, 1, 2.5), skin, glow=glow, density=2, tag="neck")
        # Black shirt, slim tie, black suit; a long overcoat over it, open at the throat with wide lapels.
        shirt = cloth(Ramp("#060607", "#0d0d10", "#151519", "#1e1e23", "#28282e", "#33333a"), 3403, 2.8, 0.4, 0.1)
        tie = lambda f, x, y, w, h: SUIT[0] if (x + y) % 2 else SUIT[1]
        suit = H.suit_mat(SUIT, 3404, 2.6)
        m.cube("body", (-3.5, 12, -1.75), (7, 12, 3.5), shirt, glow=glow, density=2, tag="torso")
        m.cube("body", (-3.5, 12, -1.75), (7, 12, 3.5), none_on(("down",), H.jacket_front(suit, SUIT[4], shirt, tie=tie, gap=(5, 8),
                                                                                       v_depth=9)), glow=glow, inflate=0.25,
               density=2, tag="suit")
        coat = lambda f, x, y, w, h: COAT[2.6 + (fbm(x, y * 2 + len(f), 3405, 64, 64, 2, 8.0) - 0.5) * 0.8 + (0.4 if f == "up" else 0)]
        front = H.jacket_front(coat, COAT[5], suit, gap=(3, 10), v_depth=12, buttons=SILVER[1], button_rows=(14, 18))
        m.cube("body", (-3.5, 11, -1.75), (7, 13, 3.5), none_on(("down",), front), glow=glow, inflate=0.5, density=2, tag="coat")
        m.cube("body", (-3.5, 21.5, -1.75), (7, 2.5, 3.5), none_on(("down",), lambda f, x, y, w, h: None if f in ("north", "up") and 3 <= x <= 10
                                                         else COAT[4 if y == 0 else 3]), glow=glow, inflate=0.8, density=2, tag="coat_collar")
        for side, x0 in (("r", -3.75), ("l", 0.0)):
            def skirt(face, x, y, w, h, s=side):
                if face in ("up", "down"):
                    return None
                if face == "north" and ((s == "r" and x >= w - 1) or (s == "l" and x <= 0)):
                    return None
                c = coat(face, x, y, w, h)
                if y >= h - 1:
                    c = COAT[1]
                elif (x + (1 if s == "l" else 0)) % 4 == 0 and face in ("south", "east", "west"):
                    c = shade(c, 0.8)
                return c
            m.cube(f"coat_tail_{side}", (x0, 1.5, -1.75), (3.75, 10.5, 3.5), skirt, glow=glow, inflate=0.55, density=2, tag="skirt")
        hand = H.skin_mat(SKIN, 3406, 3.0, 0.7)
        for side in ("right", "left"):
            H.arm_cubes(m, side, coat, hand, w=3.0, glow=glow, cuff=lambda f, x, y, w, h: shirt(f, x, y, w, h), sleeve_inflate=0.35)
            H.leg_cubes(m, side, H.suit_mat(SUIT, 3407, 2.5), lambda f, x, y, w, h: SHOE[2 + (1.5 if f == "up" else 0)], w=3.5, glow=glow)
        H.ring_cube(m, "left", DEATH_GEM[5], SILVER[3])

        # The cane: black lacquered shaft, a silver knob over the fist and a silver tip.
        m.bone("cane", (-6, 12.25, 0), "right_hand", rotation=(28, 0, 0))
        lacquer = lambda f, x, y, w, h: SUIT[2 + (2.4 if x == 0 and f in ("north", "west") else 0)]
        m.cube("cane", (-6.5, -0.5, -0.5), (1, 12.5, 1), lacquer, glow=glow, density=2, tag="cane_shaft")
        m.cube("cane", (-6.75, 13.25, -0.75), (1.5, 1.5, 1.5), lambda f, x, y, w, h: SILVER[4 if f == "up" or y == 0 else 3],
               glow=H.glow_faint(0.4), density=2, tag="cane_knob")
        m.cube("cane", (-6.6, -0.8, -0.6), (1.2, 1, 1.2), solid(SILVER[3]), glow=glow, density=2, tag="cane_tip")

        # The scythe: a long black snath through the fist, a bound grip, and a curved silver blade at the top.
        m.bone("scythe", (-6, 12.25, 0), "right_hand", rotation=(28, 0, 0))
        m.bone("scythe_spin", (-6, 12.25, 0), "scythe")
        wood = lambda f, x, y, w, h: SNATH[2.6 + (1.5 if x == 0 and f in ("north", "west") else 0) + (fbm(x, y, 3408, 32, 64, 2, 9.0) - 0.5)]
        m.cube("scythe_spin", (-6.5, -1, -0.5), (1, 33, 1), wood, glow=glow, density=2, tag="snath")
        m.cube("scythe_spin", (-6.6, 9.5, -0.6), (1.2, 5, 1.2), lambda f, x, y, w, h: SILVER[1] if y % 2 else SNATH[3], glow=glow,
               density=2, tag="snath_grip")
        m.cube("scythe_spin", (-6.5, 20, -2.5), (1, 1, 2), wood, glow=glow, density=2, tag="nib")
        m.cube("scythe_spin", (-6.75, 30, -0.75), (1.5, 2.5, 1.5), solid(SILVER[2]), glow=H.glow_faint(0.35), density=2, tag="tang")
        blade_glow = lambda f, x, y, w, h, col: mix(col, hexc("#dfe8ff"), 0.25)

        def blade(face, x, y, w, h):
            if face in ("east", "west"):
                return SILVER[4] if y == h - 1 else (SILVER[2] if y == 0 else SILVER[3])
            return SILVER[5] if face == "down" else SILVER[3]
        # The blade sweeps forward from the top of the snath and droops, in four segments.
        z, yy, ang = -0.5, 31.0, 0.0
        for i, (L, droop) in enumerate(((4.5, 6), (4.5, 14), (4.0, 24), (3.5, 38))):
            hgt = 2.5 - i * 0.45
            m.cube("scythe_spin", (-6.4, yy - hgt / 2, z - L), (0.8, hgt, L), blade, glow=blade_glow, density=2,
                   rotation=(droop, 0, 0), pivot=(-6, yy, z), tag="blade")
            a = math.radians(droop)
            z -= L * math.cos(a) * 0.92
            yy -= L * math.sin(a) * 0.92

    # --- clips -----------------------------------------------------------------------------------

    def clips(self):
        st = self.stance()
        A = self.clip

        # intro: head bowed; lifts it slowly, two taps of the cane, a small open-handed gesture.
        c = A("intro", 3.6)
        c.key(0, P(st, {"head": [24, 0, 0]}))
        c.key(1.4, P(st, {"head": [-4, 0, 0]}), E_IO)
        tap_up = P(st, {"right_arm": [-26, 0, 8], "@rider": [0, 0, 0]})
        c.key(1.8, tap_up, E_IO)
        c.key(1.95, st, E_IN)
        c.key(2.2, tap_up, E_IO)
        c.key(2.35, st, E_IN)
        offer = P(st, {"left_arm": [-40, -20, -24], "left_forearm": [-30, 0, 0], "left_hand": [0, 0, -40], "head": [-2, -6, 4]})
        c.key(3.0, offer, E_IO)
        c.key(3.6, offer)
        c.done()

        # transition: both hands on the staff held level before him, the head bowed; then he draws himself up.
        c = A("transition", 2.6)
        level = P(st, {"right_arm": [-55, -20, -10], "right_forearm": [-40, 0, 0], "right_hand": [0, 0, -80],
                       "left_arm": [-55, 20, 10], "left_forearm": [-40, 0, 0], "head": [26, 0, 0], "body": [8, 0, 0]})
        tall = P(st, {"body": [-8, 0, 0], "head": [-14, 0, 0], "right_arm": [-30, 0, 20], "left_arm": [-30, 0, -30],
                      "@rider": [0, 0.8, 0]})
        c.key(0, st)
        c.key(0.7, level, E_IO)
        c.key(1.5, P(level, {"head": [30, 0, 0]}), E_IO)
        c.key(2.0, tall, E_OUT)
        c.key(2.6, st, E_IO)
        c.done()

        # cane_strike: lifts the cane and cracks it down across you, then a short thrust.
        c = A("cane_strike", 1.4)
        lift = P(st, {"right_arm": [-150, -10, 10], "right_forearm": [-30, 0, 0], "right_hand": [-20, 0, 0], "body": [-6, 10, 0]})
        down = P(st, {"right_arm": [-70, 20, -10], "right_forearm": [0, 0, 0], "right_hand": [40, 0, 0], "body": [14, -12, 0],
                      "right_leg": [-16, 0, 0], "left_leg": [10, 0, 0]})
        thrust = P(st, {"right_arm": [-88, 0, 0], "right_forearm": [0, 0, 0], "right_hand": [62, 0, 0], "body": [10, 0, 0],
                        "right_leg": [-24, 0, 0], "left_leg": [14, 0, 0], "@rider": [0, 0, -2]})
        c.key(0, st)
        c.key(0.35, lift, E_OUT)
        c.key(0.55, down, E_IN)
        c.key(0.85, thrust, E_OUT)
        c.key(1.4, st, E_IO)
        c.done()

        # scythe_reap: both hands on the snath, drawn back on his right, then one long flat reaping arc.
        c = A("scythe_reap", 1.8)
        back = P(st, {"right_arm": [-60, 80, 60], "right_forearm": [-20, 0, 0], "right_hand": [60, 0, -40],
                      "left_arm": [-70, 60, 20], "left_forearm": [-40, 0, 0], "body": [6, 40, 0], "head": [0, -30, 0],
                      "right_leg": [12, 0, 0], "left_leg": [-10, 0, 0]})
        through = P(st, {"right_arm": [-60, -70, 20], "right_forearm": [-10, 0, 0], "right_hand": [60, 0, -40],
                         "left_arm": [-70, -60, -40], "left_forearm": [-20, 0, 0], "body": [14, -45, 0], "head": [0, 20, 0],
                         "right_leg": [-20, 0, 0], "left_leg": [14, 0, 0], "@rider": [0, -0.8, -1]})
        c.key(0, st)
        c.key(0.6, back, E_IO)
        c.key(0.95, through, E_IN)
        c.key(1.2, P(through, {"body": [12, -50, 0]}), E_OUT)
        c.key(1.8, st, E_IO)
        c.done()

        # scythe_throw: draws back overhead and hurls it; it spins out and returns to his hand.
        c = A("scythe_throw", 2.2)
        wind = P(st, {"right_arm": [-170, 30, 20], "right_forearm": [-40, 0, 0], "body": [-12, 25, 0], "left_arm": [-60, 0, -30],
                      "right_leg": [14, 0, 0], "left_leg": [-16, 0, 0]})
        release = P(st, {"right_arm": [-80, -10, 0], "right_forearm": [0, 0, 0], "body": [16, -20, 0], "left_arm": [10, 0, -30],
                         "right_leg": [-22, 0, 0], "left_leg": [14, 0, 0]})
        catch = P(st, {"right_arm": [-70, 0, 10], "right_forearm": [-20, 0, 0], "body": [-4, 0, 0]})
        c.key(0, st)
        c.key(0.45, wind, E_OUT)
        c.key(0.65, release, E_IN)
        c.key(1.6, P(release, {"body": [6, 0, 0]}), E_IO)
        c.key(1.85, catch, E_OUT)
        c.key(2.2, st, E_IO)
        a = c.done()
        a.pos("scythe_spin", (0, [0, 0, 0]), (0.65, [0, 0, 0]), (1.1, [0, 4, -34], E_OUT), (1.85, [0, 0, 0], E_IN), (2.2, [0, 0, 0]))
        a.rot("scythe_spin", (0, [0, 0, 0]), (0.65, [0, 0, 0]), (0.9, [-90, 0, 0]), (1.15, [-180, 0, 0]), (1.4, [-270, 0, 0]),
              (1.65, [-360, 0, 0]), (1.85, [-450, 0, 0]), (1.86, [-90, 0, 0]), (2.2, [0, 0, 0], E_IO))

        # summon_reapers: left hand rises, palm up, fingers spread; it closes into a fist and drives down.
        c = A("summon_reapers", 2.4)
        rise = P(st, {"left_arm": [-150, -10, -30], "left_forearm": [-10, 0, 0], "left_hand": [0, 0, -20], "head": [-26, 0, 0],
                      "body": [-6, 0, 0]})
        down = P(st, {"left_arm": [-40, 0, -50], "left_forearm": [-10, 0, 0], "left_hand": [30, 0, 0], "head": [10, 0, 0],
                      "body": [12, 0, 0], "@rider": [0, -0.6, 0]})
        c.key(0, st)
        c.key(0.9, rise, E_IO)
        c.key(1.4, P(rise, {"left_hand": [-30, 0, -20]}), E_IO)
        c.key(1.7, down, E_IN)
        c.key(2.4, st, E_IO)
        c.done()

        # shadow_step: he folds into his own shadow (squeezed to a sliver, sinking), then rises out of it elsewhere.
        c = A("shadow_step", 1.6)
        c.key(0, st)
        c.key(0.4, P(st, {"body": [10, 0, 0], "head": [10, 0, 0], "right_arm": [-20, 0, 20], "left_arm": [-20, 0, -20]}), E_IN)
        c.key(0.6, P(st, {"@rider": [0, -2, 0]}), E_IN)
        c.key(1.0, P(st, {"@rider": [0, -2, 0], "head": [-10, 0, 0]}))
        c.key(1.6, st, E_OUT)
        a = c.done()
        a.scale("rider", (0, [1, 1, 1]), (0.4, [1.05, 0.85, 1.05], E_IN), (0.6, [0.05, 0.02, 0.05], E_IN), (1.0, [0.05, 0.02, 0.05]),
                (1.35, [0.9, 1.1, 0.9], E_OUT), (1.6, [1, 1, 1], E_IO))

        # world_flip: two hands drive the staff's butt into the ground, head bowed; he looks up as the world turns over.
        c = A("world_flip", 2.6)
        raise_ = P(st, {"right_arm": [-120, 0, -12], "right_forearm": [-40, 0, 0], "left_arm": [-120, 0, 12], "left_forearm": [-40, 0, 0],
                        "head": [-20, 0, 0], "@rider": [0, 0.6, 0]})
        slam = P(st, {"right_arm": [-50, 0, -10], "right_forearm": [-10, 0, 0], "left_arm": [-55, 0, 18], "left_forearm": [-10, 0, 0],
                      "body": [18, 0, 0], "head": [26, 0, 0], "right_leg": [-12, 0, 0], "left_leg": [-12, 0, 0],
                      "right_shin": [20, 0, 0], "left_shin": [20, 0, 0], "@rider": [0, -1.2, 0]})
        c.key(0, st)
        c.key(0.7, raise_, E_OUT)
        c.key(0.9, slam, E_IN)
        c.key(1.8, P(slam, {"head": [20, 0, 0]}), E_IO)
        c.key(2.2, P(st, {"head": [-20, 0, 0]}), E_OUT)
        c.key(2.6, st, E_IO)
        c.done()

        # mounted_reap: riding hard, the scythe swung low and wide off his right side.
        back = {"right_arm": [-40, 70, 70], "right_forearm": [-10, 0, 0], "right_hand": [70, 0, -20], "body": [0, 30, 0], "head": [0, -20, 0]}
        across = {"right_arm": [-70, -50, 40], "right_forearm": [0, 0, 0], "right_hand": [70, 0, -30], "body": [18, -30, 0]}
        self.mounted_action("mounted_reap", 1.6, lambda t: ST.gallop_pose(t, 0.6, 1.0), [
            (0, {}, None), (0.5, back, None), (0.8, across, None), (1.0, across, None), (1.6, {}, None)], lean=12)

        self.death_clip()

    def death_clip(self):
        """Death does not die: he kneels on one knee, leans on his staff, and fades into himself (held)."""
        st = self.stance()
        c = self.clip("death", 4.0, hold=True)
        kneel = P(st, {"right_leg": [-80, 0, 0], "right_shin": [80, 0, 0], "left_leg": [10, 0, 0], "left_shin": [80, 0, 0],
                       "@rider": [0, -5, 0], "body": [16, 0, 0], "head": [24, 0, 0], "right_arm": [-60, 0, 6], "right_forearm": [-30, 0, 0],
                       "left_arm": [-20, 0, -10], "left_forearm": [-30, 0, 0]})
        c.key(0, self.tail_follow(st))
        c.key(0.5, self.tail_follow(P(st, {"body": [-10, 0, 0], "head": [-20, 0, 0]})), E_OUT)
        c.key(1.6, self.tail_follow(kneel), E_IO)
        c.key(3.2, self.tail_follow(P(kneel, {"head": [34, 0, 0]})), E_IO)
        c.key(4.0, self.tail_follow(P(kneel, {"head": [34, 0, 0], "@rider": [0, -5.5, 0]})), E_IO)
        a = c.done()
        a.scale("rider", (0, [1, 1, 1]), (3.2, [1, 1, 1]), (4.0, [0.7, 0.05, 0.7], E_IN))

    def death(self, length=4.0):
        return self.death_clip()

    def charge_raise(self):
        return {"right_arm": [-170, 0, 6], "right_forearm": [-20, 0, 0], "head": [-15, 0, 0]}

    def charge_level(self):
        return {"right_arm": [-95, 0, 14], "right_forearm": [0, 0, 0], "right_hand": [60, 0, -30], "body": [14, 0, 0]}


def generate():
    Death().generate()
