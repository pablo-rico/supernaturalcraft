# v0.42: Casifer's hints to the Hands (key, english, spanish).
ENTRIES = [
    ("message.supernaturalcraft.casifer.hint", "Casifer: \"I can feel one. %s, about %s blocks. Smells like %s.\"",
     "Casifer: «Siento una. Al %s, a unos %s bloques. Huele a %s»."),
]
DIRS = [("north", "norte"), ("north-east", "noreste"), ("east", "este"), ("south-east", "sureste"), ("south", "sur"),
        ("south-west", "suroeste"), ("west", "oeste"), ("north-west", "noroeste")]
for i, (en, es) in enumerate(DIRS):
    ENTRIES.append((f"message.supernaturalcraft.casifer.dir.{i}", en.capitalize(), es))
