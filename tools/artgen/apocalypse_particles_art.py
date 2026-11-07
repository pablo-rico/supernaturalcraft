"""The Apocalypse's particles (v0.40): 16x16, white with alpha (the code tints them).

  blood_mote    a soft drop of light (the demon blood's powers)
  ring_light    a ring of light (Swan Song: the four rings opening the Cage)
  blood_drop    a falling drop with a short tail (the blood rain of the seals' omens)
  hex_mote      a spark with a crooked cross (a seal's hex focus)
  witness_wisp  a pale wisp curling upward (the Witnesses)
"""

import math

from common import save
from pixelkit import Tex


def _white(a):
    return (255, 255, 255, int(max(0, min(255, a))))


def blood_mote():
    t = Tex(16, 16, 4001)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            a = 255 if d < 2.2 else 140 * (1 - (d - 2.2) / 4.0) if d < 6.2 else 0
            if a > 6:
                t.set(x, y, _white(a))
    return t


def ring_light():
    t = Tex(16, 16, 4002)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            a = 255 * max(0.0, 1 - abs(d - 5.0) / 1.4)
            a = max(a, 60 * max(0.0, 1 - d / 7.5))
            if a > 6:
                t.set(x, y, _white(a))
    return t


def blood_drop():
    t = Tex(16, 16, 4003)
    for y in range(16):
        for x in range(16):
            dx = abs(x - 7.5)
            if y >= 9:
                d = math.hypot(dx, y - 11.5)
                a = 255 if d < 2.6 else 0
            else:
                a = 180 * (y / 9) if dx < 0.8 else 0
            if a > 6:
                t.set(x, y, _white(a))
    return t


def hex_mote():
    t = Tex(16, 16, 4004)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            a = 255 if d < 1.6 else 0
            if abs(dx) < 0.7 and abs(dy) < 6:
                a = max(a, 220 * (1 - abs(dy) / 6))
            if abs(dy + 2) < 0.7 and abs(dx) < 4:
                a = max(a, 200 * (1 - abs(dx) / 4))
            if abs(dx - dy * 0.3 - 2) < 0.6 and 1 < dy < 5:
                a = max(a, 150)
            if a > 6:
                t.set(x, y, _white(a))
    return t


def witness_wisp():
    t = Tex(16, 16, 4005)
    for y in range(16):
        for x in range(16):
            s = (15 - y) / 15
            cx = 7.5 + 3.0 * math.sin(s * math.pi * 1.6)
            w = 3.2 * (1 - s) + 0.6
            d = abs(x - cx)
            a = 200 * (1 - d / w) * (0.4 + 0.6 * (1 - s)) if d < w else 0
            if a > 6:
                t.set(x, y, _white(a))
    return t


PARTICLES = {"blood_mote": blood_mote, "ring_light": ring_light, "blood_drop": blood_drop, "hex_mote": hex_mote,
             "witness_wisp": witness_wisp}


def generate():
    for name, fn in PARTICLES.items():
        save(fn(), "particle", name)


if __name__ == "__main__":
    generate()
