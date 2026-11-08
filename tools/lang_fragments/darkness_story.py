# v0.42 season 11 story text: (key, english, spanish). Applied with darkness_core.py by apply_core_lang.py.
ENTRIES = []

BAR = ["The Mass", "The Girl", "The Woman", "Siblings", "The Void", "The Dying Sun"]
BAR_ES = ["La Masa", "La Niña", "La Mujer", "Hermanos", "El Vacío", "El Sol que muere"]
for i in range(6):
    ENTRIES.append((f"entity.supernaturalcraft.amara.bar.phase{i+1}", f"Amara · {BAR[i]}", f"Amara · {BAR_ES[i]}"))

ENTRIES += [
    ("entity.supernaturalcraft.casifer", "Casifer", "Casifer"),
    ("entity.supernaturalcraft.chuck_ally", "Chuck", "Chuck"),
    ("entity.supernaturalcraft.amara_visitor", "Amara", "Amara"),
    ("entity.supernaturalcraft.fogged_villager", "Fogged Villager", "Aldeano de la niebla"),
    ("item.supernaturalcraft.amaras_light", "Amara's Light", "Luz de Amara"),
    ("tooltip.supernaturalcraft.amaras_light", "A mote of the dawn she gave back. No darkness snuffs it.",
     "Una mota del amanecer que ella devolvió. Ninguna oscuridad la apaga."),
    ("item.supernaturalcraft.superior_compass", "Compass to Superior", "Brújula a Superior"),
    ("message.supernaturalcraft.amara.chuck_dark", "She puts her brother's light out. It will come back, if you hold on.",
     "Apaga la luz de su hermano. Volverá, si aguantáis."),
    ("message.supernaturalcraft.darkness.superior_compass",
     "Castiel, in your ear: \"The fog came out of a town called Superior. %s, %s. Start there.\" A compass turns in your pack.",
     "Castiel, a tu oído: «La niebla salió de un pueblo llamado Superior. %s, %s. Empieza por ahí». Una brújula gira en tu mochila."),
    ("message.supernaturalcraft.darkness.superior",
     "Superior. The fog has eaten the whole town: the people are still here, and they are not people any more.",
     "Superior. La niebla se ha comido el pueblo entero: la gente sigue aquí, y ya no es gente."),
    ("message.supernaturalcraft.darkness.castiel_cage",
     "Castiel: \"Nothing in Heaven can stand against her. There is one who has. He is in the Cage.\"",
     "Castiel: «Nada en el Cielo puede plantarle cara. Hay alguien que ya lo hizo. Está en la Jaula»."),
    ("message.supernaturalcraft.darkness.cage.0",
     "Lucifer, behind the bars: \"Auntie's out? Oh, I'd love to help. I just need a ride.\"",
     "Lucifer, tras los barrotes: «¿La tía ha salido? Me encantaría ayudar. Solo necesito un vehículo»."),
    ("message.supernaturalcraft.darkness.cage.1", "Castiel, quietly: \"Yes.\"", "Castiel, en voz baja: «Sí»."),
    ("message.supernaturalcraft.darkness.cage.2",
     "Castiel's eyes burn red. Lucifer rolls his new shoulders and smiles: \"Let's go find some Hands.\"",
     "Los ojos de Castiel arden en rojo. Lucifer mueve sus hombros nuevos y sonríe: «Vamos a buscar unas Manos»."),
    ("message.supernaturalcraft.darkness.chuck.0",
     "A man in a flannel shirt is standing in the dawn, holding a coffee. \"Hi. We should talk.\"",
     "Un hombre con camisa de franela te espera al amanecer, con un café en la mano. «Hola. Tenemos que hablar»."),
    ("message.supernaturalcraft.darkness.chuck.1",
     "Chuck: \"Don't call me Shurley. Don't call me God either, if you can help it. Chuck is fine.\"",
     "Chuck: «No me llames Shurley. Tampoco Dios, si puedes evitarlo. Con Chuck basta»."),
    ("message.supernaturalcraft.darkness.chuck.2",
     "Chuck: \"She's my sister. I locked her away to make all of this. She has every right to be angry.\"",
     "Chuck: «Es mi hermana. La encerré para poder crear todo esto. Tiene todo el derecho a estar furiosa»."),
    ("message.supernaturalcraft.darkness.chuck.3",
     "Chuck: \"Bring the Hands to a void circle, under an open sky. Call her. I'll be there. I owe her that.\"",
     "Chuck: «Lleva las Manos a un círculo del vacío, a cielo abierto. Llámala. Allí estaré. Se lo debo»."),
    ("message.supernaturalcraft.darkness.taken.0",
     "Amara: \"He caged me once. Let him see what a cage is like.\" Smoke closes around Casifer, and he is gone.",
     "Amara: «Él me enjauló una vez. Que vea lo que es una jaula». El humo se cierra sobre Casifer, y ya no está."),
    ("message.supernaturalcraft.darkness.taken.1", "Only the rite is left. Bring the Hands, and call her.",
     "Solo queda el rito. Lleva las Manos y llámala."),
    ("message.supernaturalcraft.amara.visit.hungry", "She turns toward the villager. \"I'm so hungry.\"",
     "Se vuelve hacia el aldeano. «Tengo tanta hambre…»"),
    ("message.supernaturalcraft.amara.visit.fed", "She breathes in, and the villager falls. \"Thank you. You understand me.\"",
     "Inspira, y el aldeano cae. «Gracias. Tú me entiendes»."),
    ("message.supernaturalcraft.amara.visit.refused", "She looks at you a long while. \"You would deny me. I will remember.\"",
     "Te mira un largo rato. «Me lo negarías. Lo recordaré»."),
]

# Her visits, by milestone (DarknessRules.milestone): 0 the taking of Casifer, 1 the release (the baby), 2 Superior (the girl),
# 3 the Cage, 4-6 each Hand, 7 Chuck's reveal.
VISITS = {
    0: [("Amara: \"You and I have unfinished business.\"", "Amara: «Tú y yo tenemos asuntos pendientes»."),
        ("Amara: \"He will come. My brother always comes, in the end.\"", "Amara: «Vendrá. Mi hermano siempre viene, al final»."),
        ("Amara: \"I will be waiting.\"", "Amara: «Te estaré esperando»."),],
    1: [("A baby lies in a curl of black smoke, looking at you with ancient eyes.", "Un bebé yace en un rizo de humo negro y te mira con ojos antiguos."),
        ("It reaches for you. Where the Mark was, your arm aches.", "Alarga la mano hacia ti. Donde estuvo la Marca, el brazo te duele."),
        ("A voice in your head, older than anything: \"Thank you.\"", "Una voz en tu cabeza, más antigua que todo: «Gracias»."),],
    2: [("A girl in a black dress: \"I ate the town. Everyone in it. Is that wrong?\"", "Una niña de vestido negro: «Me comí el pueblo. A todos. ¿Está mal?»"),
        ("Amara: \"They taste of him. Of my brother. Everything does.\"", "Amara: «Saben a él. A mi hermano. Todo sabe a él»."),
        ("Amara: \"You let me out. We are bound, you and I.\"", "Amara: «Tú me liberaste. Estamos unidos, tú y yo»."),],
    3: [("She is taller now. \"You went to the Devil for help. Against me.\"", "Ahora es más alta. «Fuiste al Diablo a pedir ayuda. Contra mí»."),
        ("Amara: \"He was always my brother's favourite toy.\"", "Amara: «Siempre fue el juguete favorito de mi hermano»."),
        ("Amara: \"I'm not angry with you. Not yet.\"", "Amara: «No estoy enfadada contigo. Todavía no»."),],
    4: [("Amara: \"You found one of his Hands. Do you think it will be enough?\"", "Amara: «Encontraste una de sus Manos. ¿Crees que bastará?»"),
        ("Amara: \"He made the light so he could look away from me.\"", "Amara: «Hizo la luz para no tener que mirarme»."),
        ("Amara: \"Keep looking. I like watching you look.\"", "Amara: «Sigue buscando. Me gusta verte buscar»."),],
    5: [("A young woman in black: \"Two Hands. He's afraid now. I can feel it.\"", "Una joven de negro: «Dos Manos. Ahora tiene miedo. Lo siento»."),
        ("Amara: \"Before all this there was only us. Then he wanted more.\"", "Amara: «Antes de todo esto solo estábamos nosotros. Luego él quiso más»."),
        ("Amara: \"Will you stand with him, when I come?\"", "Amara: «¿Estarás de su lado cuando venga?»"),],
    6: [("Amara, grown: \"Three Hands. He'll come out of hiding now.\"", "Amara, ya adulta: «Tres Manos. Ahora saldrá de su escondite»."),
        ("Amara: \"I don't want to unmake you. You were kind to me.\"", "Amara: «No quiero deshacerte. Fuiste bueno conmigo»."),
        ("Amara: \"But I will unmake everything he made.\"", "Amara: «Pero desharé todo lo que él hizo»."),],
    7: [("Amara: \"So he finally showed you his face.\"", "Amara: «Así que por fin te enseñó su cara»."),
        ("Amara: \"Did he tell you why? He never tells anyone why.\"", "Amara: «¿Te dijo por qué? Él nunca le dice a nadie por qué»."),
        ("Amara: \"Call me, then. I'll come. I've waited long enough.\"", "Amara: «Llámame, entonces. Vendré. Ya he esperado bastante»."),],
}
for m, lines in VISITS.items():
    for n, (en, es) in enumerate(lines):
        ENTRIES.append((f"message.supernaturalcraft.amara.visit.{m}.{n}", en, es))

CASIFER = [
    ("You know, Castiel's in here too. He says hi. Actually he says a lot of things. I've stopped listening.",
     "¿Sabes? Castiel también está aquí dentro. Te saluda. Bueno, dice muchas cosas. He dejado de escuchar."),
    ("Six thousand years in a box and the first thing I get is a road trip with you. Lucky me.",
     "Seis mil años en una caja y lo primero que consigo es un viaje por carretera contigo. Qué suerte la mía."),
    ("Dad's out there somewhere, by the way. Hiding. As usual.", "Papá anda por ahí, por cierto. Escondido. Como siempre."),
    ("This vessel is surprisingly comfortable. Don't tell him I said that.", "Este recipiente es sorprendentemente cómodo. No le digas que lo he dicho."),
    ("Auntie Amara and I go way back. She wasn't fun at parties then either.", "La tía Amara y yo nos conocemos de hace mucho. Tampoco era divertida en las fiestas."),
    ("I'm on your side. Really. For now. Mostly.", "Estoy de tu lado. De verdad. Por ahora. Casi siempre."),
    ("Smell that? That's the sun dying. Lovely, isn't it?", "¿Hueles eso? Es el sol muriéndose. Precioso, ¿verdad?"),
    ("When this is over, I'm keeping the trench coat.", "Cuando esto acabe, me quedo con la gabardina."),
    ("Humans. You get a whole world and you still complain about the plumbing.", "Humanos. Os dan un mundo entero y aún os quejáis de las tuberías."),
    ("Castiel wants me to stop snapping my fingers at things. Castiel can wait his turn.",
     "Castiel quiere que deje de chasquear los dedos a todo. Castiel puede esperar su turno."),
    ("If we find all the Hands, can I keep one? As a souvenir?", "Si encontramos todas las Manos, ¿puedo quedarme una? ¿De recuerdo?"),
    ("Every time you pray, I can hear it. You should know that.", "Cada vez que rezas, lo oigo. Deberías saberlo."),
]
for n, (en, es) in enumerate(CASIFER):
    ENTRIES.append((f"message.supernaturalcraft.casifer.line.{n}", f"Casifer: \"{en}\"", f"Casifer: «{es}»"))
