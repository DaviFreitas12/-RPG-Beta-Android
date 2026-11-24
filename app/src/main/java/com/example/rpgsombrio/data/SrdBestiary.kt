package com.example.rpgsombrio.data

/**
 * Criaturas do D&D 5e baseadas no SRD 5.1 (Wizards of the Coast, licença CC-BY-4.0), traduzidas e resumidas.
 * Cada criatura tem id fixo ("srd-...") para que importar de novo nunca duplique. As ações seguem o estilo
 * "Ataque corpo a corpo: +4 para acertar. Dano: 2d4+2 perfurante." que o motor da mesa sabe rolar.
 * Distâncias em metros (1,5 m = 5 pés). Imagens não vêm no catálogo: use o campo de retrato da ficha.
 */

private const val SOURCE = "Fonte: SRD 5.1 (CC-BY-4.0)."

private fun tr(name: String, text: String) = NamedEntry(EntryKind.TRAIT, name, text)
private fun ac(name: String, text: String) = NamedEntry(EntryKind.ACTION, name, text)

private fun srd(
    id: String, name: String, type: String, size: String, cr: String, alignment: String,
    armor: Int, hp: Int, hpFormula: String, speed: String,
    str: Int, dex: Int, con: Int, int: Int, wis: Int, cha: Int,
    senses: String, languages: String,
    description: String, entries: List<NamedEntry>,
    saves: String = "", skills: String = "",
    defenses: List<DamageDefense> = emptyList(), conditionImmunities: String = ""
) = MonsterSheet(
    id = "srd-$id", name = name, systemId = "dnd5e", type = type, size = size, challenge = cr,
    armorClass = armor, hp = hp, hpFormula = hpFormula, speed = speed,
    attributes = mapOf(
        "strength" to str, "dexterity" to dex, "constitution" to con,
        "intelligence" to int, "wisdom" to wis, "charisma" to cha
    ),
    defenses = defenses, conditionImmunities = conditionImmunities, alignment = alignment,
    savingThrows = saves, skillsText = skills, senses = senses, languages = languages,
    entries = entries, notes = "$description\n\n$SOURCE"
)

private fun resist(vararg t: String) = t.map { DamageDefense(DefenseKind.RESISTANCE, it) }
private fun vuln(vararg t: String) = t.map { DamageDefense(DefenseKind.VULNERABILITY, it) }
private fun immune(vararg t: String) = t.map { DamageDefense(DefenseKind.IMMUNITY, it) }

fun srdMonsters(): List<MonsterSheet> = listOf(
    // ---------------- Humanoides ----------------
    srd("goblin", "Goblin", "Humanoide", "Pequeno", "1/4", "Neutro e mau", 15, 7, "2d6", "9 m",
        8, 14, 10, 10, 8, 8, "Visão no escuro 18 m, Percepção passiva 9", "Comum, Goblin",
        "Saqueador pequeno e covarde, que luta em bando e foge quando a coisa aperta.",
        listOf(
            tr("Fuga ágil", "Pode usar Desengajar ou Esconder como ação bônus em cada um dos seus turnos."),
            ac("Cimitarra", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 1d6+2 cortante."),
            ac("Arco curto", "Ataque à distância: +4 para acertar, alcance 24/96 m. Dano: 1d6+2 perfurante.")
        ), skills = "Furtividade +6"),
    srd("orc", "Orc", "Humanoide", "Médio", "1/2", "Caótico e mau", 13, 15, "2d8+6", "9 m",
        16, 12, 16, 7, 11, 10, "Visão no escuro 18 m, Percepção passiva 10", "Comum, Orc",
        "Guerreiro brutal que vive para a pilhagem e a batalha.",
        listOf(
            tr("Agressivo", "Como ação bônus, pode se mover até o seu deslocamento em direção a uma criatura hostil que consiga ver."),
            ac("Machado grande", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d12+3 cortante."),
            ac("Azagaia", "Ataque corpo a corpo ou à distância: +5 para acertar, alcance 1,5 m ou 9/36 m. Dano: 1d6+3 perfurante.")
        ), skills = "Intimidação +2"),
    srd("kobold", "Kobold", "Humanoide", "Pequeno", "1/8", "Leal e mau", 12, 5, "2d6-2", "9 m",
        7, 15, 9, 8, 7, 8, "Visão no escuro 18 m, Percepção passiva 8", "Comum, Dracônico",
        "Pequeno reptiliano que compensa a fraqueza com armadilhas e números.",
        listOf(
            tr("Sensibilidade à luz solar", "Sob luz do sol, tem desvantagem em jogadas de ataque e em testes de Percepção que dependam da visão."),
            tr("Táticas de matilha", "Tem vantagem em jogadas de ataque contra uma criatura se pelo menos um aliado dele, não incapacitado, estiver a 1,5 m dela."),
            ac("Adaga", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 1d4+2 perfurante."),
            ac("Funda", "Ataque à distância: +4 para acertar, alcance 9/36 m. Dano: 1d4+2 contundente.")
        )),
    srd("bandit", "Bandido", "Humanoide", "Médio", "1/8", "Qualquer alinhamento não leal", 12, 11, "2d8+2", "9 m",
        11, 12, 12, 10, 10, 10, "Percepção passiva 10", "Um idioma qualquer (geralmente Comum)",
        "Salteador de estrada que vive de emboscadas e assaltos.",
        listOf(
            ac("Cimitarra", "Ataque corpo a corpo: +3 para acertar, alcance 1,5 m. Dano: 1d6+1 cortante."),
            ac("Besta leve", "Ataque à distância: +3 para acertar, alcance 24/96 m. Dano: 1d8+1 perfurante.")
        )),
    srd("cultist", "Cultista", "Humanoide", "Médio", "1/8", "Qualquer alinhamento não bom", 12, 9, "2d8", "9 m",
        11, 12, 10, 10, 11, 10, "Percepção passiva 10", "Um idioma qualquer (geralmente Comum)",
        "Devoto fanático de um poder sombrio, pronto a morrer pelo culto.",
        listOf(
            tr("Devoção sombria", "Tem vantagem em testes de resistência contra ser enfeitiçado ou amedrontado."),
            ac("Cimitarra", "Ataque corpo a corpo: +3 para acertar, alcance 1,5 m. Dano: 1d6+1 cortante.")
        ), skills = "Enganação +2, Religião +2"),
    srd("gnoll", "Gnoll", "Humanoide", "Médio", "1/2", "Caótico e mau", 15, 22, "5d8", "9 m",
        14, 12, 11, 6, 10, 7, "Visão no escuro 18 m, Percepção passiva 10", "Gnoll",
        "Caçador faminto com cabeça de hiena, movido por uma fome que nunca passa.",
        listOf(
            tr("Frenesi", "Quando reduz uma criatura a 0 pontos de vida com um ataque corpo a corpo no seu turno, pode usar uma ação bônus para se mover até metade do deslocamento e fazer um ataque de mordida."),
            ac("Mordida", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 1d4+2 perfurante."),
            ac("Lança", "Ataque corpo a corpo ou à distância: +4 para acertar, alcance 1,5 m ou 6/18 m. Dano: 1d6+2 perfurante, ou 1d8+2 se usada com duas mãos."),
            ac("Arco longo", "Ataque à distância: +3 para acertar, alcance 45/180 m. Dano: 1d8+1 perfurante.")
        )),
    srd("bugbear", "Bugbear", "Humanoide", "Médio", "1", "Caótico e mau", 16, 27, "5d8+5", "9 m",
        15, 14, 13, 8, 11, 9, "Visão no escuro 18 m, Percepção passiva 10", "Comum, Goblin",
        "Goblinoide grande e peludo, mestre em emboscadas silenciosas.",
        listOf(
            tr("Bruto", "Uma arma corpo a corpo causa um dado de dano extra quando ele acerta (já incluído no ataque)."),
            tr("Ataque surpresa", "Se surpreender uma criatura e acertá-la no primeiro turno do combate, o ataque causa 2d6 de dano extra."),
            ac("Maça estrela", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 2d8+2 perfurante."),
            ac("Azagaia", "Ataque corpo a corpo ou à distância: +4 para acertar, alcance 1,5 m ou 9/36 m. Dano: 2d6+2 perfurante.")
        ), skills = "Furtividade +6, Sobrevivência +2"),

    // ---------------- Mortos-vivos ----------------
    srd("skeleton", "Esqueleto", "Morto-vivo", "Médio", "1/4", "Leal e mau", 13, 13, "2d8+4", "9 m",
        10, 14, 15, 6, 8, 5, "Visão no escuro 18 m, Percepção passiva 9", "Entende os idiomas que conhecia em vida, mas não fala",
        "Ossos animados por magia sombria, que obedecem sem hesitar.",
        listOf(
            ac("Espada curta", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 1d6+2 perfurante."),
            ac("Arco curto", "Ataque à distância: +4 para acertar, alcance 24/96 m. Dano: 1d6+2 perfurante.")
        ), defenses = vuln("Contundente") + immune("Venenoso"), conditionImmunities = "Exaustão, Envenenado"),
    srd("zombie", "Zumbi", "Morto-vivo", "Médio", "1/4", "Neutro e mau", 8, 22, "3d8+9", "6 m",
        13, 6, 16, 3, 6, 5, "Visão no escuro 18 m, Percepção passiva 8", "Entende os idiomas que conhecia em vida, mas não fala",
        "Cadáver lento e persistente, difícil de derrubar de vez.",
        listOf(
            tr("Fortitude morta-viva", "Se o dano o reduzir a 0 pontos de vida, faz um teste de resistência de Constituição CD 5 + o dano sofrido, a menos que o dano seja radiante ou um acerto crítico. Com sucesso, fica com 1 ponto de vida."),
            ac("Pancada", "Ataque corpo a corpo: +3 para acertar, alcance 1,5 m. Dano: 1d6+1 contundente.")
        ), saves = "SAB +0", defenses = immune("Venenoso"), conditionImmunities = "Envenenado"),
    srd("ghoul", "Carniçal", "Morto-vivo", "Médio", "1", "Caótico e mau", 12, 22, "5d8", "9 m",
        13, 15, 10, 7, 10, 6, "Visão no escuro 18 m, Percepção passiva 10", "Comum",
        "Devorador de cadáveres cujas garras paralisam as vítimas.",
        listOf(
            ac("Mordida", "Ataque corpo a corpo: +2 para acertar, alcance 1,5 m. Dano: 2d6+2 perfurante."),
            ac("Garras", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 2d4+2 cortante. Se o alvo não for elfo nem morto-vivo, faz um teste de resistência de Constituição CD 10 ou fica paralisado por 1 minuto, repetindo o teste ao fim de cada turno dele.")
        ), defenses = immune("Venenoso"), conditionImmunities = "Enfeitiçado, Exaustão, Envenenado"),

    // ---------------- Feras ----------------
    srd("dire-wolf", "Lobo atroz", "Fera", "Grande", "1", "Sem alinhamento", 14, 37, "5d10+10", "15 m",
        17, 15, 15, 3, 12, 7, "Percepção passiva 13", "—",
        "Predador enorme que caça em matilhas e derruba presas maiores que ele.",
        listOf(
            tr("Audição e olfato aguçados", "Tem vantagem em testes de Percepção baseados em audição ou olfato."),
            tr("Táticas de matilha", "Tem vantagem em jogadas de ataque contra uma criatura se pelo menos um aliado dele, não incapacitado, estiver a 1,5 m dela."),
            ac("Mordida", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 2d6+3 perfurante. Se o alvo for uma criatura, faz um teste de resistência de Força CD 13 ou cai no chão.")
        ), skills = "Percepção +3, Furtividade +4"),
    srd("giant-spider", "Aranha gigante", "Fera", "Grande", "1", "Sem alinhamento", 14, 26, "4d10+4", "9 m, escalada 9 m",
        14, 16, 12, 2, 11, 4, "Visão cega 3 m, visão no escuro 18 m, Percepção passiva 10", "—",
        "Aranha do tamanho de um cavalo que prende as presas em teias antes de picá-las.",
        listOf(
            tr("Escalada de aranha", "Pode escalar superfícies difíceis, inclusive de cabeça para baixo no teto, sem testes."),
            tr("Sentir teia", "Quando em contato com uma teia, sabe a localização exata de qualquer outra criatura em contato com a mesma teia."),
            tr("Andarilha de teias", "Ignora restrições de movimento causadas por teias."),
            ac("Mordida", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d8+3 perfurante. O alvo faz um teste de resistência de Constituição CD 11, sofrendo 2d8 de dano venenoso se falhar, ou metade se passar. Se o veneno reduzir o alvo a 0 pontos de vida, ele fica estável, mas envenenado e paralisado por 1 hora."),
            ac("Teia (Recarga 5-6)", "Ataque à distância: +5 para acertar, alcance 9/18 m. Alvo: uma criatura Grande ou menor, que fica impedida pela teia. Com uma ação, o alvo faz um teste de Força CD 12 para rompê-la. A teia tem CA 10, 5 pontos de vida, vulnerabilidade a fogo e imunidade a contundente, venenoso e psíquico.")
        ), skills = "Furtividade +7"),
    srd("brown-bear", "Urso-pardo", "Fera", "Grande", "1", "Sem alinhamento", 11, 34, "4d10+12", "12 m, escalada 9 m",
        19, 10, 16, 2, 13, 7, "Percepção passiva 13", "—",
        "Urso enorme, territorial e imprevisível quando protege a toca ou os filhotes.",
        listOf(
            tr("Olfato aguçado", "Tem vantagem em testes de Percepção baseados em olfato."),
            ac("Ataques múltiplos", "Faz dois ataques: um com a mordida e um com as garras."),
            ac("Mordida", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 1d8+4 perfurante."),
            ac("Garras", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 2d6+4 cortante.")
        ), skills = "Percepção +3"),

    // ---------------- Gigantes ----------------
    srd("ogre", "Ogro", "Gigante", "Grande", "2", "Caótico e mau", 11, 59, "7d10+21", "12 m",
        19, 8, 16, 5, 7, 7, "Visão no escuro 18 m, Percepção passiva 8", "Comum, Gigante",
        "Brutamontes guloso e burro, que resolve tudo com um porrete.",
        listOf(
            ac("Clava grande", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 2d8+4 contundente."),
            ac("Azagaia", "Ataque corpo a corpo ou à distância: +6 para acertar, alcance 1,5 m ou 9/36 m. Dano: 2d6+4 perfurante.")
        )),
    srd("troll", "Troll", "Gigante", "Grande", "5", "Caótico e mau", 15, 84, "8d10+40", "9 m",
        18, 13, 20, 7, 9, 7, "Visão no escuro 36 m, Percepção passiva 12", "Gigante",
        "Gigante esguio e feroz, cujas feridas se fecham diante dos olhos de quem o ataca.",
        listOf(
            tr("Olfato aguçado", "Tem vantagem em testes de Percepção baseados em olfato."),
            tr("Regeneração", "Recupera 10 pontos de vida no início do seu turno. Se sofrer dano ácido ou de fogo, esta característica não funciona no início do próximo turno dele. Só morre se começar o turno com 0 pontos de vida e sem regenerar."),
            ac("Ataques múltiplos", "Faz três ataques: um com a mordida e dois com as garras."),
            ac("Mordida", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 1d6+4 perfurante."),
            ac("Garra", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 2d6+4 cortante.")
        ), skills = "Percepção +2"),

    // ---------------- Limo ----------------
    srd("gelatinous-cube", "Cubo gelatinoso", "Limo", "Grande", "2", "Sem alinhamento", 6, 84, "8d10+40", "4,5 m",
        14, 3, 20, 1, 6, 1, "Visão cega 18 m (cego além disso), Percepção passiva 8", "—",
        "Massa transparente que desliza pelos corredores, dissolvendo tudo o que engole.",
        listOf(
            tr("Cubo gosmento", "Ocupa todo o seu espaço. Outras criaturas podem entrar nele, mas ficam sujeitas à Engolir e têm desvantagem no teste. Quem está dentro é visível, mas tem cobertura total. Uma criatura a 1,5 m pode usar uma ação para puxar alguém para fora com um teste de Força CD 12, sofrendo 3d6 de dano ácido. Comporta uma criatura Grande ou até quatro Médias ou menores."),
            tr("Transparente", "Mesmo à vista, é preciso passar num teste de Sabedoria (Percepção) CD 15 para notá-lo se ele não tiver se movido nem atacado. Uma criatura que tente entrar no espaço dele sem saber que está ali fica surpresa."),
            ac("Pseudópode", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 3d6 ácido."),
            ac("Engolir", "Move-se até o seu deslocamento, podendo entrar no espaço de criaturas Grandes ou menores. A criatura faz um teste de Destreza CD 12: se passar, escolhe ser empurrada 1,5 m para trás ou para o lado; se falhar, sofre 3d6 de dano ácido e é engolida, ficando impedida, sem conseguir respirar e sofrendo 6d6 de dano ácido no início de cada turno do cubo. Para escapar, usa uma ação e passa num teste de Força CD 12.")
        ), conditionImmunities = "Cego, Enfeitiçado, Surdo, Exaustão, Amedrontado, Caído"),

    // ---------------- Monstruosidades ----------------
    srd("mimic", "Mímico", "Monstruosidade", "Médio", "2", "Neutro", 12, 58, "9d8+18", "4,5 m",
        17, 12, 15, 5, 13, 8, "Visão no escuro 18 m, Percepção passiva 11", "—",
        "Predador que imita baús e portas, esperando que alguém encoste.",
        listOf(
            tr("Metamorfo", "Pode usar uma ação para se transformar em um objeto ou voltar à sua forma verdadeira, que é uma massa gosmenta. Suas estatísticas são as mesmas em cada forma."),
            tr("Adesivo (só na forma de objeto)", "Gruda em qualquer criatura que o toque. Uma criatura agarrada fica presa a ele e tem desvantagem em testes para escapar."),
            tr("Falsa aparência (só na forma de objeto)", "Enquanto imóvel, é indistinguível de um objeto comum."),
            tr("Agarrador", "Tem vantagem em jogadas de ataque contra qualquer criatura agarrada por ele."),
            ac("Pseudópode", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d8+3 contundente. Se estiver na forma de objeto, o alvo fica sujeito à característica Adesivo."),
            ac("Mordida", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d8+3 perfurante, mais 1d8 de dano ácido.")
        ), skills = "Furtividade +5", defenses = immune("Ácido"), conditionImmunities = "Caído"),
    srd("owlbear", "Urso-coruja", "Monstruosidade", "Grande", "3", "Sem alinhamento", 13, 59, "7d10+21", "12 m",
        20, 12, 17, 3, 12, 7, "Visão no escuro 18 m, Percepção passiva 13", "—",
        "Híbrido feroz de coruja e urso, de temperamento violento.",
        listOf(
            tr("Visão e olfato aguçados", "Tem vantagem em testes de Percepção baseados em visão ou olfato."),
            ac("Ataques múltiplos", "Faz dois ataques: um com o bico e um com as garras."),
            ac("Bico", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 1d10+5 perfurante."),
            ac("Garras", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 2d8+5 cortante.")
        ), skills = "Percepção +3"),

    // ---------------- Dragões ----------------
    srd("young-red-dragon", "Dragão vermelho jovem", "Dragão", "Grande", "10", "Caótico e mau", 18, 178, "17d10+85", "12 m, escalada 12 m, voo 24 m",
        23, 10, 21, 14, 11, 19, "Visão cega 9 m, visão no escuro 36 m, Percepção passiva 18", "Comum, Dracônico",
        "Dragão arrogante e cruel, que acumula tesouros e queima quem os cobiça.",
        listOf(
            ac("Ataques múltiplos", "Faz três ataques: um com a mordida e dois com as garras."),
            ac("Mordida", "Ataque corpo a corpo: +10 para acertar, alcance 3 m. Dano: 2d10+6 perfurante, mais 1d6 de dano de fogo."),
            ac("Garra", "Ataque corpo a corpo: +10 para acertar, alcance 1,5 m. Dano: 2d6+6 cortante."),
            ac("Sopro de fogo (Recarga 5-6)", "Exala fogo em um cone de 9 m. Cada criatura na área faz um teste de Destreza CD 17, sofrendo 16d6 de dano de fogo se falhar, ou metade se passar.")
        ), saves = "DES +4, CON +9, SAB +4, CAR +8", skills = "Percepção +8, Furtividade +4", defenses = immune("Fogo")),
    srd("wyvern", "Wyvern", "Dragão", "Grande", "6", "Sem alinhamento", 13, 110, "13d10+39", "6 m, voo 24 m",
        19, 10, 16, 5, 12, 6, "Visão no escuro 18 m, Percepção passiva 14", "—",
        "Parente menor dos dragões, de cauda venenosa e garras que mergulham do céu.",
        listOf(
            ac("Ataques múltiplos", "Faz dois ataques: um com a mordida e um com o ferrão. Enquanto voa, pode usar as garras no lugar de um dos outros ataques."),
            ac("Mordida", "Ataque corpo a corpo: +7 para acertar, alcance 3 m. Dano: 2d6+4 perfurante."),
            ac("Garras", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 2d8+4 cortante."),
            ac("Ferrão", "Ataque corpo a corpo: +7 para acertar, alcance 3 m. Dano: 2d6+4 perfurante. O alvo faz um teste de resistência de Constituição CD 15, sofrendo 7d6 de dano venenoso se falhar, ou metade se passar.")
        ), skills = "Percepção +4"),

    // ---------------- Elemental ----------------
    srd("fire-elemental", "Elemental do fogo", "Elemental", "Grande", "5", "Neutro", 13, 102, "12d10+36", "15 m",
        10, 17, 16, 6, 10, 7, "Visão no escuro 18 m, Percepção passiva 10", "Ignan (entende, mas não fala Comum)",
        "Chama viva e faminta, que consome tudo o que pode queimar.",
        listOf(
            tr("Forma de fogo", "Pode atravessar um espaço de 2,5 cm de largura sem se apertar. Quem o tocar ou acertá-lo com ataque corpo a corpo sofre 1d10 de dano de fogo. Quem terminar o turno a 1,5 m dele também. Incendeia objetos inflamáveis que toque."),
            tr("Iluminação", "Emite luz plena em 9 m e penumbra por mais 9 m."),
            tr("Suscetibilidade à água", "Sofre 1 de dano de frio a cada 1,5 m que se mover na água, ou a cada 4 litros de água jogados nele."),
            ac("Ataques múltiplos", "Faz dois ataques de toque."),
            ac("Toque", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 2d6+3 fogo. Se o alvo for uma criatura ou objeto inflamável, ele pega fogo, sofrendo 1d10 de dano de fogo no início de cada turno até alguém usar uma ação para apagar as chamas.")
        ), defenses = resist("Contundente não mágico", "Perfurante não mágico", "Cortante não mágico") + immune("Fogo", "Venenoso"),
        conditionImmunities = "Exaustão, Agarrado, Paralisado, Petrificado, Envenenado, Caído, Impedido, Inconsciente"),

    // ---------------- Corruptores ----------------
    srd("imp", "Diabrete", "Corruptor", "Miúdo", "1", "Leal e mau", 13, 10, "3d4+3", "6 m, voo 12 m",
        6, 17, 13, 11, 12, 14, "Visão no escuro 36 m, Percepção passiva 11", "Infernal, Comum",
        "Pequeno demônio ardiloso, que serve de espião e tentador.",
        listOf(
            tr("Metamorfo", "Pode usar uma ação para virar uma fera (rato, corvo ou aranha) ou voltar à forma verdadeira. Suas estatísticas são as mesmas, exceto deslocamento (rato 6 m; corvo 6 m, voo 18 m; aranha 6 m, escalada 6 m)."),
            tr("Visão do diabo", "Escuridão mágica não impede sua visão no escuro."),
            tr("Resistência à magia", "Tem vantagem em testes de resistência contra magias e outros efeitos mágicos."),
            ac("Ferrão (mordida na forma de fera)", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d4+3 perfurante. O alvo faz um teste de resistência de Constituição CD 11, sofrendo 3d6 de dano venenoso se falhar, ou metade se passar."),
            ac("Invisibilidade", "Fica invisível até atacar ou perder a concentração (como em uma magia). Equipamento que carrega também some.")
        ), skills = "Enganação +4, Intuição +3, Persuasão +4, Furtividade +5",
        defenses = resist("Frio", "Não mágico, exceto prata") + immune("Fogo", "Venenoso"), conditionImmunities = "Envenenado"),
    srd("hell-hound", "Cão infernal", "Corruptor", "Médio", "3", "Leal e mau", 15, 45, "7d8+14", "15 m",
        17, 12, 14, 6, 13, 6, "Visão no escuro 18 m, Percepção passiva 15", "Entende Infernal, mas não fala",
        "Cão de pelo fumegante e olhos em brasa, que caça em matilhas a serviço de demônios.",
        listOf(
            tr("Audição e olfato aguçados", "Tem vantagem em testes de Percepção baseados em audição ou olfato."),
            tr("Táticas de matilha", "Tem vantagem em jogadas de ataque contra uma criatura se pelo menos um aliado dele, não incapacitado, estiver a 1,5 m dela."),
            ac("Mordida", "Ataque corpo a corpo: +5 para acertar, alcance 1,5 m. Dano: 1d8+3 perfurante, mais 2d6 de dano de fogo."),
            ac("Sopro de fogo (Recarga 5-6)", "Exala fogo em um cone de 4,5 m. Cada criatura na área faz um teste de Destreza CD 12, sofrendo 6d6 de dano de fogo se falhar, ou metade se passar.")
        ), skills = "Percepção +5", defenses = immune("Fogo")),

    // ---------------- Celestial, Fada, Planta, Construto, Aberração ----------------
    srd("pegasus", "Pégaso", "Celestial", "Grande", "2", "Caótico e bom", 12, 59, "7d10+21", "18 m, voo 27 m",
        18, 15, 16, 10, 15, 13, "Percepção passiva 16", "Entende Celestial, Comum, Élfico e Silvestre, mas não fala",
        "Cavalo alado e nobre, que só aceita montaria de coração puro.",
        listOf(
            ac("Cascos", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 2d6+4 contundente.")
        ), saves = "DES +4, SAB +4, CAR +3", skills = "Percepção +6"),
    srd("dryad", "Dríade", "Fada", "Médio", "1", "Neutro", 11, 22, "5d8", "9 m",
        10, 12, 11, 14, 15, 18, "Visão no escuro 18 m, Percepção passiva 14", "Élfico, Silvestre",
        "Espírito de uma árvore antiga, que protege a floresta e encanta quem a ameaça.",
        listOf(
            tr("Conjuração inata", "Atributo de conjuração: Carisma (CD 14). À vontade: druidcraft. 3 vezes por dia cada: enredar, bagas benfazejas. 1 vez por dia cada: pele de árvore, passos sem pegadas, moldar madeira em porrete mágico."),
            tr("Resistência à magia", "Tem vantagem em testes de resistência contra magias e outros efeitos mágicos."),
            tr("Falar com bestas e plantas", "Pode se comunicar com bestas e plantas como se partilhassem um idioma."),
            tr("Caminhar entre árvores", "Uma vez por turno, usa 1,5 m de deslocamento para entrar numa árvore viva e sair de outra do mesmo tipo a até 18 m, aparecendo a até 1,5 m dela."),
            ac("Porrete", "Ataque corpo a corpo: +2 para acertar (+6 com moldar madeira), alcance 1,5 m. Dano: 1d4 contundente (1d8+4 com moldar madeira)."),
            ac("Encanto feérico", "Escolhe um humanoide ou fera a até 9 m. O alvo faz um teste de Sabedoria CD 14 ou fica enfeitiçado por 24 horas, tratando a dríade como amiga de confiança. O efeito termina se ele sofrer dano dela ou dos aliados dela.")
        ), skills = "Percepção +4, Furtividade +5"),
    srd("shambling-mound", "Monte ambulante", "Planta", "Grande", "5", "Sem alinhamento", 15, 136, "16d10+48", "6 m, natação 6 m",
        18, 8, 16, 5, 10, 5, "Visão cega 18 m (cego além disso), Percepção passiva 10", "—",
        "Pilha de vegetação podre que se arrasta pelos pântanos, esmagando e afogando quem alcança.",
        listOf(
            tr("Absorver raio", "Sempre que sofrer dano elétrico, não sofre dano e recupera pontos de vida iguais ao dano elétrico que seria sofrido."),
            ac("Ataques múltiplos", "Faz duas pancadas. Se as duas acertarem o mesmo alvo Médio ou menor, ele fica agarrado (CD 14 para escapar) e o monte usa Engolir contra ele."),
            ac("Pancada", "Ataque corpo a corpo: +7 para acertar, alcance 1,5 m. Dano: 2d8+4 contundente."),
            ac("Engolir", "Engole uma criatura Média ou menor que esteja agarrada por ele. O alvo fica cego, impedido, sem conseguir respirar e sofre 2d8+4 de dano contundente no início de cada turno do monte. Só pode ter uma criatura engolida por vez.")
        ), skills = "Furtividade +2", defenses = resist("Frio", "Fogo") + immune("Elétrico"), conditionImmunities = "Cego, Surdo, Exaustão"),
    srd("animated-armor", "Armadura animada", "Construto", "Médio", "1", "Sem alinhamento", 18, 33, "6d8+6", "7,5 m",
        14, 11, 13, 1, 3, 1, "Visão cega 18 m (cego além disso), Percepção passiva 6", "—",
        "Armadura vazia que se move sozinha, guardando salões e tesouros esquecidos.",
        listOf(
            tr("Suscetibilidade antimagia", "Fica incapacitada se estiver na área de um campo antimagia. Se for alvo de dissipar magia, faz um teste de resistência de Constituição contra a CD do conjurador ou fica inconsciente por 1 minuto."),
            tr("Falsa aparência", "Enquanto imóvel, é indistinguível de uma armadura comum."),
            ac("Ataques múltiplos", "Faz dois ataques corpo a corpo."),
            ac("Pancada", "Ataque corpo a corpo: +4 para acertar, alcance 1,5 m. Dano: 1d6+2 contundente.")
        ), defenses = immune("Venenoso", "Psíquico"),
        conditionImmunities = "Cego, Enfeitiçado, Surdo, Exaustão, Amedrontado, Paralisado, Petrificado, Envenenado"),
    srd("chuul", "Chuul", "Aberração", "Grande", "4", "Caótico e mau", 16, 93, "11d10+33", "9 m, natação 9 m",
        19, 10, 16, 5, 11, 5, "Visão no escuro 18 m, Percepção passiva 14", "Entende Língua Profunda, mas não fala",
        "Crustáceo monstruoso das águas escuras, com pinças enormes e tentáculos paralisantes.",
        listOf(
            tr("Anfíbio", "Pode respirar ar e água."),
            tr("Sentir magia", "Sente magia a até 36 m, como na magia detectar magia, mas sem precisar conjurá-la."),
            ac("Ataques múltiplos", "Faz duas pinçadas. Se estiver agarrando uma criatura, também pode usar os tentáculos uma vez."),
            ac("Pinça", "Ataque corpo a corpo: +6 para acertar, alcance 1,5 m. Dano: 2d6+4 contundente. Se o alvo for Grande ou menor, fica agarrado (CD 14 para escapar). O chuul tem duas pinças, cada uma agarrando um alvo."),
            ac("Tentáculos", "Um alvo agarrado pelo chuul faz um teste de resistência de Constituição CD 13 ou fica envenenado por 1 minuto, e paralisado enquanto durar o veneno. Repete o teste ao fim de cada turno dele.")
        ), skills = "Percepção +4", defenses = immune("Venenoso"), conditionImmunities = "Envenenado")
)

/** Criaturas do catálogo que ainda não estão no bestiário (comparando pelo id fixo). */
fun missingSrdMonsters(existing: List<MonsterSheet>): List<MonsterSheet> {
    val have = existing.map { it.id }.toSet()
    return srdMonsters().filter { it.id !in have }
}
