# RPG Sombrio — Beta 0.9.1 (ficha ↔ motor, bestiário e grimório por sistema)

## Ficha ligada ao motor
- Os valores da ficha viram variáveis do motor: `SIGLA` (modificador; o valor, se o atributo não tem modificador), `NOME`, `SIGLA_VAL` (valor bruto), `PROF`, `NIVEL`, `CA`, cada recurso (`PV`, `PV_MAX`, …) e cada perícia pelo nome (`ATLETISMO`).
- Seção **Rolagens** na ficha: iniciativa, teste por fórmula (ou reserva de dados), CA do alvo e **Ao combate**. Botões de rolar em cada atributo, perícia e ataque. Resultado fixo no rodapé (últimas rolagens).
- Itens equipados com dano aparecem como ataques.
- **Itens equipados alteram a CA**: o item tem o campo "CA". Em itens do tipo Armadura é o valor base (a maior armadura equipada substitui a CA base da ficha); nos demais tipos (escudo, anel…) é um bônus somado. A ficha mostra a CA efetiva e como ela foi composta, e ela vale em `CA`, nos ataques sofridos no Motor e ao entrar no combate. A CA base continua editável.
- **Conjurar** magias: rola o efeito com os valores da ficha e debita o custo quando ele aponta para um recurso do sistema ("3 Mana"). Sem recurso suficiente ou com fórmula inválida, nada é gasto.
- Tela **Motor** reescrita: escolhe uma ficha/criatura (ou valores manuais `FOR=3, DES=2`), usa fórmulas e dados do sistema, adiciona personagens e criaturas ao combate e resolve ataques com os ataques da ficha do atacante.
- Os PV dos personagens ficam sincronizados entre o combate e a ficha.

## Sistemas
- Novo estilo de rolagem **reserva de dados** (conta sucessos): lados do dado e valor de sucesso configuráveis. Vampiro vem como reserva de d10 (sucesso em 6+).
- Fórmulas de teste e de iniciativa do sistema agora são usadas (dado-base das rolagens, sugestão do Motor, iniciativa). Dados dos atalhos vêm do sistema.

## Bestiário
- Rola ações a partir do texto ("+4 para acertar. Dano: 2d4+2 perfurante"); ações só com dano rolam só o dano.
- Rolar PV pela fórmula, iniciativa, rolar atributo, **Ao combate** com quantidade (cada cópia rola PV e iniciativa próprios).
- Rótulos de PV/CA seguem o sistema; filtro por sistema; **Trocar sistema** da criatura.

## Grimório
- Magias têm sistema (vazio = todos). Filtro por sistema, seletor na magia, nova magia nasce no sistema da campanha; a ficha só oferece magias do seu sistema.
- **Rolar efeito** no Grimório escolhendo um conjurador.

## Tudo por sistema (itens, magias, traços e perícias)
- **Itens** ganharam sistema (vazio = todos). Cada sistema usa o item de um jeito, então o item de um sistema tem os seus próprios dano, bônus e CA. O editor tem **Duplicar para outro sistema** (cria a cópia e abre para ajustar). Novos itens nascem no sistema da campanha.
- **Itens, Grimório e Vantagens/desvantagens** abrem filtrados pelo sistema da campanha, com chips para trocar ou ver todos. Registros sem sistema aparecem em qualquer filtro. Magias e traços também têm **Duplicar para outro sistema**.
- **Perícias** pertencem ao sistema: cada ficha guarda as perícias por sistema. Ao trocar o sistema da ficha, as perícias do sistema antigo saem da vista (e voltam se ela for ligada de novo) e as do novo entram. O Vampiro agora traz a lista de perícias (sem atributo fixo; a reserva é montada por você).
- Na ficha, inventário, magias, traços e perícias de outros sistemas ficam ocultos e não contam: peso, CA, ataques, variáveis de fórmula e pontos de vantagens só consideram o sistema da ficha. Nada é apagado.
- Fichas antigas: perícias sem sistema continuam aparecendo em qualquer sistema até a primeira troca de sistema.

## Novo visual
- Paleta "Meia-noite": fundo azul-ardósia, cartões suaves com contorno discreto, um único destaque índigo (botões) e lilás claro (títulos e ícones), vermelho suave para dano. Substitui o marrom/dourado medieval.
- Cantos mais arredondados, textos pequenos um pouco maiores e cartão da campanha com gradiente.
- Todas as cores ficam em `MainActivity.kt` (Night, Panel, Panel2, Brand, Accent, Ink, Muted, Danger, Hairline) e em `res/values/colors.xml`.

## Compatibilidade
- Dados antigos continuam válidos (campos novos são opcionais). Magias antigas ficam "para todos os sistemas".

## Limites conhecidos
- Reserva de dados: sem dados de fome, pares de 10 ou ataque em combate por reserva (ataques usam fórmula).
- Itens equipados alteram a CA, mas ainda não alteram atributos. O limite de DES de armaduras médias/pesadas não é aplicado (digite o valor final da armadura no item).
- Dano em texto livre só é lido no começo ("1d8 + FOR cortante").

## Não validado
Escrito sem Android SDK/Gradle. Compile e rode `gradle testDebugUnitTest` (testes novos em `SheetEngineTest`) antes de usar.
