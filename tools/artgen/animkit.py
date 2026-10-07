"""A small keyframe DSL that writes GeckoLib 1.8.0 animation files.

    a = Anim("animation.demon.walk", 1.0, loop=True)
    a.rot("left_leg", (0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0]))
    a.pos("body", (0, [0, 0, 0]), (0.5, [0, -0.5, 0], "easeInOutSine"))

Values are Bedrock: degrees and pixels, relative to the rest pose. A value may also be a
Molang string, e.g. "math.sin(query.anim_time * 360) * 4".
"""

import json
import os


def _fmt(t):
    return f"{t:.4f}".rstrip("0").rstrip(".") if "." in f"{t:.4f}" else f"{t}"


class Anim:
    def __init__(self, name, length, loop=False, hold=False):
        self.name = name
        self.length = length
        self.loop = loop
        self.hold = hold
        self.bones = {}

    def _channel(self, bone, channel, keys):
        ch = self.bones.setdefault(bone, {}).setdefault(channel, {})
        for k in keys:
            t, v = k[0], k[1]
            ease = k[2] if len(k) > 2 else None
            if t > self.length + 1e-6:
                raise ValueError(f"{self.name}: key at {t}s beyond length {self.length}s")
            vec = list(v) if not isinstance(v, str) else v
            ch[_fmt(float(t))] = {"vector": vec, "easing": ease} if ease else {"vector": vec}
        return self

    def rot(self, bone, *keys):
        return self._channel(bone, "rotation", keys)

    def pos(self, bone, *keys):
        return self._channel(bone, "position", keys)

    def scale(self, bone, *keys):
        return self._channel(bone, "scale", keys)

    def const_rot(self, bone, v):
        """A pose held for the whole clip."""
        return self.rot(bone, (0, v), (self.length, v))

    def to_json(self):
        j = {"animation_length": self.length, "bones": self.bones}
        if self.loop:
            j["loop"] = True
        elif self.hold:
            j["loop"] = "hold_on_last_frame"
        return j


class AnimFile:
    def __init__(self):
        self.anims = []

    def add(self, anim):
        if any(a.name == anim.name for a in self.anims):
            raise ValueError("duplicate animation " + anim.name)
        self.anims.append(anim)
        return anim

    def new(self, name, length, loop=False, hold=False):
        return self.add(Anim(name, length, loop, hold))

    def names(self):
        return [a.name for a in self.anims]

    def write(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            json.dump({"format_version": "1.8.0", "animations": {a.name: a.to_json() for a in self.anims}}, f, indent=1)
            f.write("\n")
