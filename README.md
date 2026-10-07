# SupernaturalCraft

Mod de NeoForge 1.21.1 inspirado en *Supernatural*: cazar demonios, componer hechizos con sigilos
enoquianos, dibujar círculos rituales y, si te atreves, abrir la Jaula y enfrentarte a **Lucifer**.

## Contenido (v0.1.0)

- **Cazador**: sal (línea que los demonios no cruzan), tiza blanca y de sangre, agua bendita,
  trampa del diablo 3×3, cuchillo de Ruby, hoja de ángel, amuleto del cazador (Curios opcional).
- **Demonios**: demonio de ojos negros (2 recipientes, telequinesis) y ocultista demoníaco
  (lanza fuego infernal). Huyen como humo al quedar malheridos salvo que estén atrapados.
- **Magia modular**: Forma (Touch, Bolt, Burst, Ward) + hasta 3 Efectos (Smite, Hellfire, Frost,
  Exorcise, Bind, Mend, Repel, Reveal) + hasta 3 Modificadores (Empower, Extend, Widen, Echo).
  Maná (barra junto a la hotbar) y reactivos. Sigilos *data-driven*:
  `data/<ns>/supernaturalcraft/sigil/*.json`.
- **Rituales**: altar + patrón de suelo (`ritual_pattern`) + ofrendas + activador. Recetas
  *data-driven* de tipo `supernaturalcraft:ritual`. Consagración, Atadura, Exorcismo, forjas, Llave
  de la Jaula e Invocación.
- **Lucifer** (4 fases, 17 ataques telegrafiados, arena con cúpula y restauración del terreno,
  cinemáticas). P1 Recipiente → P2 Caído (alas de ceniza) → P3 Ira de la Jaula (hielo, ilusiones)
  → P4 Arcángel Desatado (6 alas, vuelo, Smite de arena).
- **Recompensas**: Archangel Blade, The Colt (+ balas), Lucifer's Grace, trofeo Morningstar.
- **Diario del Cazador** (botón *Journal* en el compositor del grimorio), logros, JEI (rituales y
  sigilos).

## Desarrollo

```bash
./gradlew build                 # compila + JUnit
./gradlew runData               # regenera src/generated (modelos, lang, loot, tags, recetas, sonidos)
./gradlew runGameTestServer     # GameTests (hunter, magic, rituals, boss)
./gradlew runClient
```

### Arte (todo generado por código, Python sin dependencias)

```bash
python3 tools/artgen/generate.py      # texturas, modelos GeckoLib (.geo.json) y animaciones
python3 tools/structgen/empty_template.py
```

- `tools/artgen/geomodel.py` — rigs declarativos con UV por cara (convenciones de GeckoLib documentadas).
- `tools/artgen/animkit.py` — DSL de keyframes para `.animation.json`.
- `tools/artgen/preview3d.py` — previsualización 3D con z-buffer (misma matemática que GeckoLib).
- `tools/artgen/sheet.py` — hojas de contacto de texturas.

### Previsualización en el juego

```bash
cp -R runs/gameTestServer/world runs/client/saves/sn_preview
SN_PREVIEW=lucifer1,lucifer2,lucifer3,lucifer4,demons,ritual,arena ./gradlew runClient -Ppreview
SN_PREVIEW=fight ./gradlew runClient -Ppreview   # combate completo con capturas cada 3 s
SN_PREVIEW=gui ./gradlew runClient -Ppreview     # HUD, compositor y diario
```

Las capturas quedan en `runs/client/screenshots/sn_*.png`.

### Comandos de prueba (nivel 2)

`/supernatural boss summon|phase <2-4>|health <fracción>`, `/supernatural arena restore`,
`/supernatural mana fill`, `/supernatural sigil learn <id|all>`, `/supernatural grace <bool>`.

### Configuración (servidor)

`config/supernaturalcraft-server.toml`: vida y escalado de Lucifer, tope por golpe, multiplicador
de daño no sagrado, radio de la arena, destrucción real de bloques (desactivada por defecto),
límite del snapshot, segundos hasta el fracaso, demonio en el contragolpe ritual.
