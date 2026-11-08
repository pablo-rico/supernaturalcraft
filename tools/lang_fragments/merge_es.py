"""Merges tools/lang_fragments/<name>.json (flat key -> Spanish, or key -> {en, es}) into es_es.json, keeping its CRLF format."""
import json, sys, collections
p = "src/main/resources/assets/supernaturalcraft/lang/es_es.json"
es = json.loads(open(p, encoding="utf-8", newline="").read(), object_pairs_hook=collections.OrderedDict)
n = 0
for frag in sys.argv[1:]:
    d = json.load(open(frag, encoding="utf-8"))
    for k, v in d.items():
        es[k] = v["es"] if isinstance(v, dict) else v
        n += 1
open(p, "w", encoding="utf-8", newline="").write((json.dumps(es, ensure_ascii=False, indent=2) + "\n").replace("\n", "\r\n"))
print(n, "merged")
