# RPG Sombrio — 0.9.2 (em andamento: fichas mais completas)

## Resistências, vulnerabilidades e imunidades
- Personagens e criaturas ganharam a seção **Resistências e vulnerabilidades**: tipos de dano por categoria (resistência, vulnerabilidade, imunidade a dano), com sugestões (Fogo, Frio, Perfurante…) e campo para tipos próprios.
- **Imunidades a condições** (texto) em personagens e criaturas.
- Criaturas antigas: o texto livre de "Resistências / imunidades / vulnerabilidades" continua salvo e aparece como **Outras defesas**; nada foi apagado.
- `damageMultiplierFor` (SheetModels.kt) calcula o multiplicador por tipo de dano: imunidade 0, resistência 0,5, vulnerabilidade 2; resistência + vulnerabilidade se anulam.

## Ficha do personagem mais completa (tudo em seções recolhidas)
- **Identidade**: deslocamento e XP.
- **Recursos**: PV temporários, inspiração, salvaguardas contra a morte (aparecem só com PV em 0) e **condições ativas** (toque para marcar). Esses extras aparecem só em sistemas no estilo D&D; as condições valem para todos.
- **Salvaguardas** (sistemas com modificador): proficiência por atributo, bônus final e botão de rolar.
- **Inventário**: campo de dinheiro separado dos itens avulsos.
- **Proficiências e idiomas**: idiomas, proficiências (armas, armaduras, ferramentas) e sentidos.
- **História e personalidade**: antecedente, alinhamento, aparência, traços, ideais, vínculos e defeitos.

## Ficha da criatura
- Alinhamento, salvaguardas e perícias (texto), e tesouro/saque nas anotações.

## Ligado ao combate (Motor)
- **Resistências e vulnerabilidades valem no ataque**: ao resolver um ataque, o dano é ajustado pelo tipo (imune 0, resistência metade arredondando para baixo, vulnerabilidade dobro) e o registro mostra "alvo resiste a fogo: dano 7 → 3". Funciona quando a ação tem tipo de dano ("2d4+2 perfurante").
- As defesas do alvo são copiadas da ficha ao entrar no combate. Personagens releem a ficha ao abrir o Motor.
- **Condições no combate**: botão de informação em cada combatente abre a lista de condições. As ativas aparecem no cartão. Condições citadas em "Imunidades a condições" ficam bloqueadas. Nos personagens, as condições são as mesmas da ficha (mudar em um lugar muda no outro).

## Condições com efeito de verdade (Motor)
Regras no estilo D&D 5e, em `Conditions.kt` (lógica pura, com testes em `ConditionsTest`):
- **Perdem o turno** (e não conseguem atacar): Atordoado, Paralisado, Inconsciente, Incapacitado, Petrificado. "Próximo turno" pula quem está assim e registra no histórico ("💫 Goblin 1 está Atordoado e perde o turno."). Se for a vez de alguém incapacitado, aparece um aviso com o botão **Pular turno**.
- **Desvantagem nos próprios ataques**: Caído, Amedrontado, Cego, Envenenado, Imobilizado.
- **Vantagem contra o alvo**: Atordoado, Cego, Imobilizado, Paralisado, Inconsciente, Petrificado. Caído dá vantagem corpo a corpo e desvantagem à distância (novo seletor **Corpo a corpo / À distância** na tela de ataque).
- **Invisível**: vantagem ao atacar e desvantagem para quem o ataca.
- Vantagem e desvantagem **se anulam**; várias fontes do mesmo tipo não acumulam. O d20 é rolado duas vezes de verdade (fica o maior ou o menor) e o histórico mostra os dois valores.
- **Paralisado e Inconsciente**: todo acerto corpo a corpo é crítico (dados de dano dobrados). 1 natural continua errando.
- **Petrificado**: resiste a todo dano (resistência não acumula; junto com vulnerabilidade, se anulam).
- Agarrado, Enfeitiçado, Exausto e Surdo continuam só marcadores (o efeito depende de contexto ou do sistema). A lista de condições agora mostra o efeito de cada uma.
- Condições livres (texto fora da lista) não têm efeito automático.

## Batalha com visual novo (Motor)
- **Arena** (`BattleArena.kt`): faixa de ordem de turnos com retratos, palco com atacante e alvo lado a lado, barras de PV coloridas e emojis das condições. Sem retrato, o combatente ganha um avatar colorido com a inicial.
- **Animação do ataque**: o dado (d20, ou o dado do dano nas ações sem teste) gira com números passando e para com um "quique"; o atacante recua e avança; no acerto o alvo treme, pisca em vermelho, a barra de PV desce e o dano sobe flutuando (crítico em dourado, mais forte); no erro o alvo se esquiva e aparece "ERROU". Vantagem/desvantagem aparecem no resultado ("vantagem: 17 (descartou 9)").
- O **atacante padrão é quem está na vez** e o alvo padrão é o primeiro inimigo em pé; toque no retrato (ou na faixa de turnos) para trocar.
- O histórico só recebe o resultado quando o golpe acerta, para não estragar a animação. Há um interruptor **Animações da batalha** (guardado no aparelho) que mostra tudo instantaneamente.
- A lista de participantes ganhou retrato, barra de PV animada e condições com emoji; "(caído)" no nome virou "(derrotado)" para não confundir com a condição Caído.

## Salvaguardas no combate e condições com duração (Motor)
Lógica em `Saves.kt` e `Conditions.kt` (pura, com testes em `SavesAndDurationTest`).

**Salvaguarda**
- Novo cartão **SALVAGUARDA** na batalha. Quem rola é o **alvo** escolhido na arena: atributo, CD, bônus extra e Normal / Vantagem / Desvantagem manual.
- Bônus: personagem usa a proficiência da ficha; criatura lê o texto "Salvaguardas" ("DES +3, Sab: +2") e, se o atributo não está na lista, usa o modificador; combatente avulso usa só o bônus extra. Sistemas de reserva de dados (Vampiro) não têm salvaguarda.
- **As condições mexem na rolagem**: Atordoado, Paralisado, Inconsciente e Petrificado **falham sozinhos** em Força e Destreza (nem rola); Imobilizado tem **desvantagem** em Destreza. Vantagem e desvantagem se anulam; o d20 é rolado duas vezes de verdade e o histórico mostra os dois. 20 e 1 naturais não decidem a salvaguarda (como no D&D 5e).
- **Se falhar, aplica uma condição**: escolha a condição e a duração no próprio cartão. Alvo imune (campo "Imunidades a condições") não recebe e o histórico avisa. Passou ou deu erro de fórmula: nada muda.

**Duração em rodadas**
- Toda condição pode ter duração: no cartão de salvaguarda ou no diálogo de condições (novo campo "Duração ao marcar", 0 = até remover).
- A contagem **desce ao fim de cada turno de quem está afetado**, inclusive quando o turno dele é pulado (Atordoado 1 rodada = perde um turno e acaba). Quando chega a zero a condição sai sozinha e o histórico mostra "⏳ … acabou". Em personagens a remoção também vai para a ficha.
- A lista de participantes e o diálogo mostram o que falta ("Atordoado (2 rodadas)").
- Novo **contador de rodadas** ("Rodada N • Turno x de y"): sobe quando a vez dá a volta na ordem e volta a 1 em "Encerrar combate". A rodada e as durações ficam guardadas no aparelho junto com o combate.
- Aplicar uma condição no turno do próprio afetado gasta uma rodada logo no fim desse turno (ex.: buff em si mesmo com 1 rodada dura só até o fim do turno atual). Use uma rodada a mais se quiser o contrário.
- Dados antigos continuam válidos: combates salvos sem duração carregam normalmente (todas as condições ficam "até remover").

## PV temporários e salvaguardas contra a morte (Motor)
Lógica em `Death.kt` e `withDamage` (testes em `DeathAndTempHpTest`).
- **PV temporários** da ficha entram no combate: o dano gasta primeiro os temporários, a cura não os restaura. Aparecem na lista ("PV 8/10 (+3 temp.)") e voltam para a ficha.
- **Personagem a 0 PV** fica "morrendo" (criaturas a 0 PV continuam só "derrotadas"). Quando a vez dele passa, a **salvaguarda contra a morte é rolada sozinha**: 10+ sucesso, 9 ou menos falha, 1 natural = 2 falhas, 20 natural = volta com 1 PV. 3 sucessos estabilizam, 3 falhas matam. O cartão SALVAGUARDA tem um botão para rolar na hora.
- **Dano a 0 PV** (acima dos PV temporários) é uma falha. Curar acima de 0 zera os marcadores. Os marcadores são os mesmos da ficha (mudar em um lugar muda no outro).
- Não implementado: falhas dobradas em acerto crítico e morte instantânea por dano massivo.

## Outros ajustes
- `splitFormula`: "1d6 + fogo" deixava um "+" sobrando no tipo de dano; corrigido (o teste antigo já apontava isso).
- Nova dependência explícita `androidx.compose.animation:animation` (versão vem do BOM).

## Ainda não faz
- Criaturas antigas só com o texto livre de resistências não são lidas pelo combate (precisam das defesas estruturadas).
- Salvaguardas no combate não têm "repetir a cada turno" nem "metade do dano se passar"; as magias ainda não trazem CD/atributo/condição próprios (o painel é manual).
- Condições marcadas na ficha do personagem (fora do combate) não têm duração: só as aplicadas no Motor contam rodadas.
- Não há "alcance" nas ações: corpo a corpo ou à distância é escolhido à mão na hora de atacar.
- Resistência a dano "não mágico" ou "prata" não distingue se o ataque é mágico.

## Validação
- A lógica pura (motor, fichas, condições, turnos) foi compilada e testada fora do Android: 126 testes passando (72 anteriores + 36 em `SavesAndDurationTest` + 18 em `DeathAndTempHpTest`).
- **A interface (`BattleArena.kt`, `EngineScreen.kt`, `SheetExtras.kt`, `SavePanel.kt`) NÃO foi compilada nem rodada**: faltava o Android SDK. Compile com `gradle assembleDebug` e confira na tela, principalmente as animações (tempos, tamanhos e o quique do dado).
