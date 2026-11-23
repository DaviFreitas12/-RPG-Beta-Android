# Histórico de versões

Todas as mudanças relevantes do RPG Sombrio, da mais recente para a mais antiga. Formato inspirado em [Keep a Changelog](https://keepachangelog.com/pt-BR/).

## 0.9.8 — Mais opções no criador de personagens

### Adicionado
- 8 novos estilos de cabelo: franja lateral, ondulado, afro, dreadlocks, tranças duplas, undercut, longo repartido e desgrenhado.
- 3 estilos de barba, 3 detalhes faciais, 6 modelos de roupa, 3 capas, 3 acessórios de cabeça e 3 armas adicionais.
- 5 novas cores para roupas e 4 novas cores para cabelo.
- Novos detalhes visuais em pixel art para diferenciar mantos, cota de malha, armadura real, roupas de patrulheiro, traje arcano e roupa de viajante.

### Compatibilidade
- As opções existentes mantêm seus índices; personagens já salvos continuam compatíveis.
- Versão do app atualizada para 0.9.8 (versionCode 18).

## Não lançado — Criação de personagem mais guiada

### Corrigido
- A ficha nova abria sempre na aba *Ficha*: o código previa abrir no *Perfil* quando o nome está vazio, mas toda ficha nascia chamada "Novo personagem". Agora a ficha recém-criada abre no *Perfil*, onde ficam o retrato e a aparência.
- **Gerar descrição** (Aparência → História) apagava o texto existente sem avisar; agora pede confirmação quando já há um texto diferente do gerado.
- PV temporários passam a aparecer em qualquer sistema com pontos de vida (exceto reservas de dados). Inspiração e salvaguardas contra a morte continuam só nos sistemas no estilo D&D.

### Adicionado
- **Criação guiada**: depois de escolher o sistema, um diálogo pede o nome (obrigatório) e, se quiser, classe e origem.
- **Escolher atributos** por ficha (aba *Ficha* → Atributos): cada ficha pode esconder atributos do sistema. Os valores continuam guardados e valendo nas fórmulas. Criar atributos novos continua sendo em Campanha → Sistemas.
- Testes da criação de ficha, dos atributos escondidos e da aba inicial (`NewCharacterTest`).

### Alterado
- A lista de fichas é gravada no aparelho ~0,3 s depois da última edição (antes, a cada letra digitada), e sempre ao sair da tela.

## 0.9.5 a 0.9.7 — Ficha em abas, Motor em abas e telas vazias

> Estas três versões estão registradas juntas: o conteúdo foi levantado comparando o código da 0.9.3 com o da 0.9.7, sem os pacotes intermediários. O build subiu de 14 para 17 (`versionCode`) e a versão para 0.9.7.

### Adicionado
- **Ficha de personagem em abas**: *Ficha* (recursos, atributos, salvaguardas, perícias, traços), *Combate* (painel de rolagem, ataques, magias, resistências e vulnerabilidades), *Itens* (aparece só se o sistema usa inventário) e *Perfil* (identidade, aparência, proficiências, história e anotações).
- **Cabeçalho da ficha** com avatar, PV, CA e iniciativa, com rolagem de iniciativa direto dele. Ao rolar a página, uma **barra compacta** (nome, PV e CA) fica fixa junto das abas.
- A aba aberta é lembrada na ficha. Ao trocar de aba, o conteúdo volta ao topo sem reabrir o cabeçalho.
- Seções *Proficiências* e *História* no Perfil da ficha.
- **Motor em abas** (*Rolar* e *Combate*, com a contagem de combatentes). Abre em *Combate* quando já há combatentes, e lembra a ficha ou criatura escolhida para rolar.
- **Estados vazios** com ícone, título e uma frase de orientação nas listas de fichas, criaturas, magias, itens e traços (diferente de "busca sem resultado").

### Alterado
- Arena de combate: barra de PV mais fina, destaque em âmbar para crítico, cinza para erro e fundo em degradê.
- Dados de exemplo renomeados para o visual neutro: "Lobo sombrio" virou "Lobo selvagem" e "Chama sombria" virou "Chama viva".
- Pequenos ajustes de layout nas telas de Campanha, Biblioteca, Sistemas, Itens, Traços, Bestiário e Grimório, e no criador de aparência.

## 0.9.4 — Novo visual neutro, com modo claro e escuro

### Adicionado
- Tela **Início**, enxuta: campanha atual, atalho para personagens e para o motor, e acesso às configurações.
- Tela **Configurações** (aparência e versão do app). A escolha do modo claro/escuro saiu da tela Campanha.

### Alterado
- A tela Campanha ficou só com nome, descrição e sistema; os atalhos foram para o Início e a Biblioteca.
- Identidade visual redesenhada para servir a qualquer tipo de RPG: base neutra (fundo quente no claro, grafite no escuro) e destaque verde-água, no lugar do índigo sombrio.
- **Modo claro, escuro ou do sistema**, escolhido em Configurações e lembrado entre aberturas. Ícones da barra de status acompanham o modo.
- Cartões de acesso rápido com ícone colorido por categoria (personagens, criaturas, magias, itens, motor, traços).
- Cabeçalho da Campanha sem brasão medieval; textos e nomes padrão neutros ("Minha campanha", "Sua mesa, suas regras").
- Textos pequenos (9–11sp) aumentados para 11–12sp.
- Cores fixas da arena de combate e do criador de aparência passaram a seguir o tema.

### Removido
- Imagem `rpg_crest.png` (2,6 MB), que deixava o APK maior sem necessidade.

## 0.9.3 — Aparência e organização do projeto

### Adicionado
- **Criador de aparência em pixel art**, corpo inteiro, desenhado por código (sem imagens): raça (humano, elfo, anão, orc, tiefling, halfling), corpo, pele, rosto, cabelo, olhos, boca, barba, detalhes, roupa, capa, elmo/chapéu e arma, com luz, sombra e contorno automáticos.
- Campos de descrição do personagem. O avatar aparece na lista de personagens e na arena de combate quando não há retrato da galeria.

### Alterado
- Código reorganizado em pacotes por responsabilidade (`engine`, `data`, `ui/*`); `MainActivity.kt` dividido em tema, navegação, tela de campanha, biblioteca e componentes. Nenhuma regra de negócio foi alterada.
- Documentação reorganizada em `docs/` e changelogs unificados neste arquivo.

### Removido
- `google-services.json` (apontava para outro pacote e não era usado) e cópias `.backup` antigas.

## 0.9.2 — Fichas mais completas

### Resistências, vulnerabilidades e imunidades
- Personagens e criaturas ganharam a seção **Resistências e vulnerabilidades**: tipos de dano por categoria (resistência, vulnerabilidade, imunidade a dano), com sugestões (Fogo, Frio, Perfurante…) e campo para tipos próprios.
- **Imunidades a condições** (texto) em personagens e criaturas.
- Criaturas antigas: o texto livre de "Resistências / imunidades / vulnerabilidades" continua salvo e aparece como **Outras defesas**; nada foi apagado.
- `damageMultiplierFor` (SheetModels.kt) calcula o multiplicador por tipo de dano: imunidade 0, resistência 0,5, vulnerabilidade 2; resistência + vulnerabilidade se anulam.

### Ficha do personagem mais completa (tudo em seções recolhidas)
- **Identidade**: deslocamento e XP.
- **Recursos**: PV temporários, inspiração, salvaguardas contra a morte (aparecem só com PV em 0) e **condições ativas** (toque para marcar). Esses extras aparecem só em sistemas no estilo D&D; as condições valem para todos.
- **Salvaguardas** (sistemas com modificador): proficiência por atributo, bônus final e botão de rolar.
- **Inventário**: campo de dinheiro separado dos itens avulsos.
- **Proficiências e idiomas**: idiomas, proficiências (armas, armaduras, ferramentas) e sentidos.
- **História e personalidade**: antecedente, alinhamento, aparência, traços, ideais, vínculos e defeitos.

### Ficha da criatura
- Alinhamento, salvaguardas e perícias (texto), e tesouro/saque nas anotações.

### Ligado ao combate (Motor)
- **Resistências e vulnerabilidades valem no ataque**: ao resolver um ataque, o dano é ajustado pelo tipo (imune 0, resistência metade arredondando para baixo, vulnerabilidade dobro) e o registro mostra "alvo resiste a fogo: dano 7 → 3". Funciona quando a ação tem tipo de dano ("2d4+2 perfurante").
- As defesas do alvo são copiadas da ficha ao entrar no combate. Personagens releem a ficha ao abrir o Motor.
- **Condições no combate**: botão de informação em cada combatente abre a lista de condições. As ativas aparecem no cartão. Condições citadas em "Imunidades a condições" ficam bloqueadas. Nos personagens, as condições são as mesmas da ficha (mudar em um lugar muda no outro).

### Condições com efeito de verdade (Motor)
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

### Batalha com visual novo (Motor)
- **Arena** (`BattleArena.kt`): faixa de ordem de turnos com retratos, palco com atacante e alvo lado a lado, barras de PV coloridas e emojis das condições. Sem retrato, o combatente ganha um avatar colorido com a inicial.
- **Animação do ataque**: o dado (d20, ou o dado do dano nas ações sem teste) gira com números passando e para com um "quique"; o atacante recua e avança; no acerto o alvo treme, pisca em vermelho, a barra de PV desce e o dano sobe flutuando (crítico em dourado, mais forte); no erro o alvo se esquiva e aparece "ERROU". Vantagem/desvantagem aparecem no resultado ("vantagem: 17 (descartou 9)").
- O **atacante padrão é quem está na vez** e o alvo padrão é o primeiro inimigo em pé; toque no retrato (ou na faixa de turnos) para trocar.
- O histórico só recebe o resultado quando o golpe acerta, para não estragar a animação. Há um interruptor **Animações da batalha** (guardado no aparelho) que mostra tudo instantaneamente.
- A lista de participantes ganhou retrato, barra de PV animada e condições com emoji; "(caído)" no nome virou "(derrotado)" para não confundir com a condição Caído.

### Salvaguardas no combate e condições com duração (Motor)
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

### PV temporários e salvaguardas contra a morte (Motor)
Lógica em `Death.kt` e `withDamage` (testes em `DeathAndTempHpTest`).
- **PV temporários** da ficha entram no combate: o dano gasta primeiro os temporários, a cura não os restaura. Aparecem na lista ("PV 8/10 (+3 temp.)") e voltam para a ficha.
- **Personagem a 0 PV** fica "morrendo" (criaturas a 0 PV continuam só "derrotadas"). Quando a vez dele passa, a **salvaguarda contra a morte é rolada sozinha**: 10+ sucesso, 9 ou menos falha, 1 natural = 2 falhas, 20 natural = volta com 1 PV. 3 sucessos estabilizam, 3 falhas matam. O cartão SALVAGUARDA tem um botão para rolar na hora.
- **Dano a 0 PV** (acima dos PV temporários) é uma falha. Curar acima de 0 zera os marcadores. Os marcadores são os mesmos da ficha (mudar em um lugar muda no outro).
- Não implementado: falhas dobradas em acerto crítico e morte instantânea por dano massivo.

### Outros ajustes
- `splitFormula`: "1d6 + fogo" deixava um "+" sobrando no tipo de dano; corrigido (o teste antigo já apontava isso).
- Nova dependência explícita `androidx.compose.animation:animation` (versão vem do BOM).

### Ainda não faz
- Criaturas antigas só com o texto livre de resistências não são lidas pelo combate (precisam das defesas estruturadas).
- Salvaguardas no combate não têm "repetir a cada turno" nem "metade do dano se passar"; as magias ainda não trazem CD/atributo/condição próprios (o painel é manual).
- Condições marcadas na ficha do personagem (fora do combate) não têm duração: só as aplicadas no Motor contam rodadas.
- Não há "alcance" nas ações: corpo a corpo ou à distância é escolhido à mão na hora de atacar.
- Resistência a dano "não mágico" ou "prata" não distingue se o ataque é mágico.

### Validação
- A lógica pura (motor, fichas, condições, turnos) foi compilada e testada fora do Android: 126 testes passando (72 anteriores + 36 em `SavesAndDurationTest` + 18 em `DeathAndTempHpTest`).
- **A interface (`BattleArena.kt`, `EngineScreen.kt`, `SheetExtras.kt`, `SavePanel.kt`) NÃO foi compilada nem rodada**: faltava o Android SDK. Compile com `gradle assembleDebug` e confira na tela, principalmente as animações (tempos, tamanhos e o quique do dado).

## 0.9.1 — Ficha ↔ motor, bestiário e grimório por sistema

### Ficha ligada ao motor
- Os valores da ficha viram variáveis do motor: `SIGLA` (modificador; o valor, se o atributo não tem modificador), `NOME`, `SIGLA_VAL` (valor bruto), `PROF`, `NIVEL`, `CA`, cada recurso (`PV`, `PV_MAX`, …) e cada perícia pelo nome (`ATLETISMO`).
- Seção **Rolagens** na ficha: iniciativa, teste por fórmula (ou reserva de dados), CA do alvo e **Ao combate**. Botões de rolar em cada atributo, perícia e ataque. Resultado fixo no rodapé (últimas rolagens).
- Itens equipados com dano aparecem como ataques.
- **Itens equipados alteram a CA**: o item tem o campo "CA". Em itens do tipo Armadura é o valor base (a maior armadura equipada substitui a CA base da ficha); nos demais tipos (escudo, anel…) é um bônus somado. A ficha mostra a CA efetiva e como ela foi composta, e ela vale em `CA`, nos ataques sofridos no Motor e ao entrar no combate. A CA base continua editável.
- **Conjurar** magias: rola o efeito com os valores da ficha e debita o custo quando ele aponta para um recurso do sistema ("3 Mana"). Sem recurso suficiente ou com fórmula inválida, nada é gasto.
- Tela **Motor** reescrita: escolhe uma ficha/criatura (ou valores manuais `FOR=3, DES=2`), usa fórmulas e dados do sistema, adiciona personagens e criaturas ao combate e resolve ataques com os ataques da ficha do atacante.
- Os PV dos personagens ficam sincronizados entre o combate e a ficha.

### Sistemas
- Novo estilo de rolagem **reserva de dados** (conta sucessos): lados do dado e valor de sucesso configuráveis. Vampiro vem como reserva de d10 (sucesso em 6+).
- Fórmulas de teste e de iniciativa do sistema agora são usadas (dado-base das rolagens, sugestão do Motor, iniciativa). Dados dos atalhos vêm do sistema.

### Bestiário
- Rola ações a partir do texto ("+4 para acertar. Dano: 2d4+2 perfurante"); ações só com dano rolam só o dano.
- Rolar PV pela fórmula, iniciativa, rolar atributo, **Ao combate** com quantidade (cada cópia rola PV e iniciativa próprios).
- Rótulos de PV/CA seguem o sistema; filtro por sistema; **Trocar sistema** da criatura.

### Grimório
- Magias têm sistema (vazio = todos). Filtro por sistema, seletor na magia, nova magia nasce no sistema da campanha; a ficha só oferece magias do seu sistema.
- **Rolar efeito** no Grimório escolhendo um conjurador.

### Tudo por sistema (itens, magias, traços e perícias)
- **Itens** ganharam sistema (vazio = todos). Cada sistema usa o item de um jeito, então o item de um sistema tem os seus próprios dano, bônus e CA. O editor tem **Duplicar para outro sistema** (cria a cópia e abre para ajustar). Novos itens nascem no sistema da campanha.
- **Itens, Grimório e Vantagens/desvantagens** abrem filtrados pelo sistema da campanha, com chips para trocar ou ver todos. Registros sem sistema aparecem em qualquer filtro. Magias e traços também têm **Duplicar para outro sistema**.
- **Perícias** pertencem ao sistema: cada ficha guarda as perícias por sistema. Ao trocar o sistema da ficha, as perícias do sistema antigo saem da vista (e voltam se ela for ligada de novo) e as do novo entram. O Vampiro agora traz a lista de perícias (sem atributo fixo; a reserva é montada por você).
- Na ficha, inventário, magias, traços e perícias de outros sistemas ficam ocultos e não contam: peso, CA, ataques, variáveis de fórmula e pontos de vantagens só consideram o sistema da ficha. Nada é apagado.
- Fichas antigas: perícias sem sistema continuam aparecendo em qualquer sistema até a primeira troca de sistema.

### Novo visual
- Paleta "Meia-noite": fundo azul-ardósia, cartões suaves com contorno discreto, um único destaque índigo (botões) e lilás claro (títulos e ícones), vermelho suave para dano. Substitui o marrom/dourado medieval.
- Cantos mais arredondados, textos pequenos um pouco maiores e cartão da campanha com gradiente.
- Todas as cores ficam em `MainActivity.kt` (Night, Panel, Panel2, Brand, Accent, Ink, Muted, Danger, Hairline) e em `res/values/colors.xml`.

### Compatibilidade
- Dados antigos continuam válidos (campos novos são opcionais). Magias antigas ficam "para todos os sistemas".

### Limites conhecidos
- Reserva de dados: sem dados de fome, pares de 10 ou ataque em combate por reserva (ataques usam fórmula).
- Itens equipados alteram a CA, mas ainda não alteram atributos. O limite de DES de armaduras médias/pesadas não é aplicado (digite o valor final da armadura no item).
- Dano em texto livre só é lido no começo ("1d8 + FOR cortante").

### Não validado
Escrito sem Android SDK/Gradle. Compile e rode `gradle testDebugUnitTest` (testes novos em `SheetEngineTest`) antes de usar.

## 0.9.0 — Sistemas livres e vantagens/desvantagens

### Sistemas
- Vários sistemas próprios, em vez de um único "Sistema próprio". Aba Sistemas: lista, novo em branco, duplicar e editar.
- D&D 5e, D&D 2024 e Vampiro viram modelos de fábrica: duplique para criar a sua variante.
- Atributos com sigla, valor padrão, mínimo/máximo e modificador configurável (piso((valor − base) ÷ divisor)); o modificador deixou de ser sempre o do D&D.
- Recursos, perícias ligadas a atributo, rótulos de conceito/origem, dados e fórmulas por sistema.
- Blocos da ficha ligáveis: nível, CA, perícias, ataques, magias, inventário, vantagens e desvantagens.
- Um sistema em uso por fichas não pode ser excluído.
- O "Sistema próprio" antigo (chaves `custom_*`) é migrado automaticamente para um sistema com id `custom`, mantendo as mesmas chaves; as fichas existentes não mudam.

### Vínculo
- A campanha guarda o id do sistema (`active_campaign_system_id`); o nome antigo continua sendo aceito.
- "Nova ficha" pergunta o sistema (o da campanha vem primeiro). Na ficha, "Trocar sistema" vincula a outro sem apagar valores.
- A ficha oferece "Adicionar perícias do sistema" para trazer perícias criadas depois.

### Vantagens e desvantagens
- Nova biblioteca (Biblioteca > Vantagens e desvantagens): nome, tipo, custo, categoria, sistema (ou todos) e descrição.
- Vantagens gastam pontos, desvantagens devolvem. Seção na ficha com gastos, devolvidos e livres; aviso quando passa do orçamento.

### Não validado
Escrito sem Android SDK/Gradle. Compile e rode `gradle testDebugUnitTest` (há testes novos em `SystemsTest`) antes de usar.

## 0.8.0 — Itens estruturados e inventário da ficha

### Itens
- Item com identidade (nome, tipo com atalhos, raridade), detalhes (peso numérico, valor, bônus, dano/efeito, propriedades, sintonização) e descrição.
- Lista com busca, filtro por tipo e etiquetas de bônus, dano, peso e valor.
- Registros simples antigos (`records_items`) são migrados; o peso é extraído de textos como "Peso: 0,5".

### Inventário na ficha
- A seção "Equipamento" virou **Inventário**: itens vinculados à biblioteca, com quantidade (−/+) e marcação de equipado.
- Peso total calculado (peso × quantidade) e resumo "N itens • X kg • Y equipados".
- Itens apagados da biblioteca aparecem como "Item removido da biblioteca" até você remover a linha.
- O campo livre de antes continua disponível como "Itens avulsos, moedas e anotações" (dados antigos preservados).

### Limpeza
- Removido o antigo CRUD genérico de registros (`LocalRecordsScreen`/`SimpleLibraryScreen`): todas as abas da Biblioteca e Personagens agora usam telas estruturadas.

### Não validado
Escrito sem Android SDK/Gradle. Compile e teste pelo Android Studio ou GitHub Actions antes de usar.

## 0.7.0 — Fichas, bestiário e grimório estruturados

### Fichas de personagem
- Ficha guiada pelo sistema da campanha: atributos, recursos, perícias e rótulos mudam conforme o sistema (D&D 5e/2024, Vampiro, Sistema próprio/Outro, que usa o Construtor de sistemas).
- Tela única com seções recolhíveis: Identidade, Recursos, Atributos, Perícias, Ataques, Magias, Equipamento e Anotações.
- Recursos com barra e botões -/+ (PV, dados de vida, mana…); modificadores de atributo calculados; perícias com proficiência.
- Magias da ficha são vinculadas ao Grimório (escolha por checklist).
- Edição ao vivo: tudo é salvo a cada alteração.

### Bestiário
- Criatura com identidade, combate (CA, PV, fórmula de PV, deslocamento), atributos, defesas/sentidos/idiomas.
- Traços, Ações, Reações e Ações lendárias como listas de entradas nome + descrição.
- Filtro por tipo e busca.

### Grimório
- Magias agrupadas por nível, com filtro por nível e busca.
- Card expansível com execução, alcance, componentes, duração, custo, classes e descrição; marcadores de concentração e ritual.
- Editor em seções: Básico, Conjuração e Efeito.

### Dados
- Novos formatos JSON em SharedPreferences (`sheets_characters`, `sheets_bestiary`, `sheets_spells`).
- Registros simples antigos (`records_*`) são migrados na primeira abertura; as chaves antigas não são apagadas.
- Itens continuam no formato simples (próxima etapa).

### Não validado
Escrito sem Android SDK/Gradle. Compile e teste pelo Android Studio ou GitHub Actions antes de usar.

## 0.6.1 — Refinamento, fase 1

### Correções no motor
- Variáveis que começam com "D" (como `DES`) eram lidas como dado e quebravam a fórmula. Agora "d" só inicia um dado quando seguido de dígitos.
- Divisão arredonda para baixo (`-3 / 2 = -2`, `(8 - 10) / 2 = -1`), como no modificador de atributo.
- Ataque: 20 natural sempre acerta e dobra os dados do dano; 1 natural sempre erra. O resultado agora é um `AttackResult`.
- Nomes de variáveis ignoram acentos e maiúsculas; aceitam dígitos (`ATR1`).
- O motor recebe um `DiceRoller` injetável, o que permite testes determinísticos.
- Novo: `app/src/test/.../RpgEngineTest.kt` (aritmética, dados, variáveis, divisão, erros, ataques, críticos, `dndModifier`).

### Combate
- O turno ativo é guardado por id e persistido: adicionar/remover combatentes não troca a vez.
- Desempate de iniciativa por nome; "Próximo turno" pula combatentes caídos (0 PV).
- Mensagens de crítico e falha crítica.

### Interface e sistema
- Tema escuro no Manifest (sem flash branco), edge-to-edge, ícone adaptativo com o brasão.
- Brasão convertido de PNG (2,6 MB) para WebP (~40 KB).
- Retratos: copiados e reduzidos (512 px, JPEG) para o armazenamento do app, carregados fora da thread principal; arquivos órfãos são apagados. Retratos antigos (URIs da galeria) continuam funcionando.
- Navegação preservada ao girar a tela; botão Voltar retorna à Biblioteca.
- `google-services.json` removido (não usado; package inconsistente).
- CI agora roda `testDebugUnitTest` antes de `assembleDebug`.

### Não validado
Esta fase foi escrita sem acesso a Android SDK/Gradle. A primeira compilação e os testes devem ser confirmados pelo GitHub Actions ou pelo Android Studio.

## 0.6.0

### Fundação e navegação
- Navegação principal reduzida a cinco destinos para evitar abas cortadas em telas estreitas.
- Motor de RPG promovido a destino principal e sempre visível.
- Nova central Biblioteca com atalhos para Bestiário, Grimório e Itens.
- A navegação antiga e as chaves de armazenamento local foram preservadas.

### Combate executável
- Novo painel de resolução de ataque no Motor de RPG.
- Seleção de atacante e alvo entre os combatentes da iniciativa.
- Fórmulas configuráveis de ataque e dano.
- Comparação do resultado de ataque com a defesa/CA do alvo.
- Dano aplicado automaticamente aos PV do alvo e salvo no armazenamento local.
- Mensagens distintas para acerto, erro e falha de validação.

### Limitações conhecidas desta etapa
- Os valores FOR, DES e PROF do teste ainda são informados manualmente no Motor.
- Fichas, itens, magias e criaturas ainda não estão conectados entre si.
- A persistência continua usando SharedPreferences/JSON; migração Room exige migrações e testes próprios e será feita em etapa separada para preservar dados.
- Não há sincronização em nuvem ou multijogador nesta versão.

### Validação
- O workflow GitHub Actions executa `gradle assembleDebug` e publica o APK como artefato.
- A compilação final precisa ser confirmada pelo workflow após o push.

## 0.5.0

### Motor de RPG inicial
- Novo núcleo Kotlin `RpgEngine` para executar expressões de dados e aritmética com variáveis.
- Notação suportada: `d20`, `2d6 + 3`, `1d20 + FOR + PROF` e expressões com parênteses.
- Testes contra Classe de Dificuldade (CD), com sucesso/falha e margem.
- Atalhos de rolagem d4, d6, d8, d10, d12, d20 e d100.
- Primeira tela de iniciativa, ordenação automática e controle básico de PV.
- Lista de combate e fórmula preferida persistidas localmente via SharedPreferences.

### Limites conhecidos desta versão
- O motor ainda não implementa todas as regras oficiais de D&D, Vampiro ou outros sistemas.
- Ataques, ações, condições, magias, progressão, fichas dinâmicas e regras específicas precisam de adaptadores por sistema.
- A persistência ainda usa SharedPreferences; Room e sincronização Firebase não estão implementados nesta versão.
- A compilação Android final precisa ser validada pelo GitHub Actions e instalada para testes no aparelho.

## 0.4.0

### Implementado nesta revisão
- Cartões de acesso rápido da tela Campanha agora navegam para as respectivas abas.
- A tela da campanha permite editar e salvar nome e descrição.
- O sistema selecionado na campanha é persistido no aparelho.
- Personagens, criaturas, magias e itens possuem cadastro, listagem, busca, edição e exclusão local.
- Os registros são persistidos em SharedPreferences como JSON e permanecem disponíveis sem internet.
- Registros iniciais demonstrativos podem ser editados ou excluídos.

### Limitações conhecidas
- Esta versão é offline-first, mas ainda não é híbrida com sincronização em nuvem.
- Firebase Authentication/Firestore e regras de segurança ainda precisam ser configurados.
- Os registros usam SharedPreferences/JSON; Room é recomendado antes de crescer o volume de dados.
- O construtor de sistemas ainda armazena fórmulas como texto; não há interpretador seguro de fórmulas.
- Não foi possível compilar nesta sessão porque o ambiente não possui Android SDK/Gradle.
- É necessário compilar e testar no GitHub Actions e em aparelho Android antes de considerar a versão validada.

## 0.9.9 — Expansão de vestimentas do criador pixel art
- Categoria de roupas ampliada de 10 para 20 opções, preservando os índices anteriores dos personagens salvos.
- Adicionados modelos com desenho próprio: armadura de placas, armadura de escamas, brigandina, cavaleiro negro, guarda real, guerreiro tribal, manto de necromante, vestes de clérigo, traje de assassino e traje de nobre.
- Incluídos detalhes de pixel art como ombreiras, rebites, placas, faixas, golas, símbolos e bordados.
- Atualizados `versionCode` para 19 e `versionName` para 0.9.9.
- Validação estática dos índices e alterações realizada; compilação Android não executada neste ambiente.


## 0.9.10
- Adicionados 4 estilos de cabelo pixel art: raspado lateral, trança lateral, topete e coques duplos.
- Adicionadas 6 vestimentas pixel art: armadura rúnica, couraça de gladiador, manto de viajante, vestes druídicas, armadura de sentinela e traje de alquimista.
- Adicionados 3 acessórios de cabeça: máscara de couro, elmo com chifres e capuz aberto.
- Mantida a correção do import `signed` em EngineScreen.


## 0.9.11
- Adicionadas 10 variações masculinas de cabelo: corte militar, pompadour, penteado para trás, mullet, shag médio, cortina repartida, franja bagunçada, risco lateral, cachos curtos e cabelo longo preso.
- Mantidos os índices das opções anteriores para preservar personagens já salvos.
