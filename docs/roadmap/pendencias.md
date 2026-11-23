# Pendências e roadmap

> Análise feita sobre a 0.9.2, atualizada na 0.9.3 e revisada na 0.9.7. Itens riscados já foram resolvidos.

Feita lendo o código e os documentos do projeto e rodando a lógica pura (126 testes passando na análise da 0.9.3; não foram reexecutados na revisão da 0.9.7). Cada item abaixo foi conferido no código, não deduzido dos changelogs.

## 1. Antes de tudo: compilar a interface
A lógica pura é testada, mas **a interface não foi compilada nesta análise**: a revisão da 0.9.7 só conferiu arquivos, imports, versão e ligação da navegação. Vale compilar principalmente as telas mais mexidas desde a 0.9.2 (`CharacterSheetScreen.kt` com as abas, `EngineScreen.kt`, `BattleArena.kt`, `SheetExtras.kt`, `SavePanel.kt`, `HomeScreen.kt`, `SettingsScreen.kt`). Primeiro passo: `gradle assembleDebug`, abrir o Motor e passar por: adicionar combatentes → atacar → salvaguarda com condição → "Próximo turno" até a condição expirar.
- O projeto **não tem Gradle Wrapper** (`gradlew` e a pasta `gradle/` não existem). O CI usa o Gradle instalado na máquina; gere o wrapper (`gradle wrapper --gradle-version 8.9`) para a build ser reproduzível.
- ~~`build.gradle.kts` com versão desatualizada~~ — resolvido: versão 0.9.7 (código 17), igual ao README.

## 2. Higiene do repositório
~~Resolvido na 0.9.3~~: `google-services.json` e os arquivos `.backup` foram removidos e o `.gitignore` agora cobre `google-services.json` em qualquer pasta. **Ainda vale** restringir/rotacionar a chave de API no console do Firebase (o arquivo está no histórico do git).

## 3. Combate — o que ainda não funciona
Em ordem do que mais aparece numa mesa:
1. ~~Salvaguardas contra a morte~~ e 2. ~~PV temporários~~: feitos (ver changelog). Faltam falha dupla em crítico e morte instantânea por dano massivo.
3. **Magia com CD**: a magia não guarda atributo de salvaguarda, CD nem condição, e não existe a variável de CD de magia (8 + PROF + modificador). Hoje o mestre preenche o painel de salvaguarda à mão. O passo natural é a magia/ação da criatura trazer "salvaguarda de DES CD 14, metade do dano" e a tela abrir o painel já preenchido.
4. **Salvaguarda de efeito**: falta "repetir a salvaguarda no fim do turno para acabar com o efeito" e "metade do dano se passar" (sopros, bolas de fogo). Também falta atingir vários alvos de uma vez.
5. **Concentração**: a magia tem o marcador, mas não há controle de concentração nem a salvaguarda de CON ao tomar dano.
6. **Condições só marcadoras**: Agarrado, Enfeitiçado, Exausto e Surdo não mudam nada. Exausto precisa de níveis (varia por sistema).
7. **Duração das condições**: só contam rodadas as aplicadas no Motor; condições marcadas na ficha são indefinidas. A contagem é por turno do afetado (ver changelog), sem opção de "até o início do turno de quem lançou".
8. **Economia de ação**: sem reações, ataque de oportunidade, ações lendárias, rodada de surpresa ou "esperar". Corpo a corpo e à distância ainda são escolhidos à mão.
9. **Resistência "não mágico"/prata** não distingue ataque mágico; criaturas antigas só com o texto livre de resistências não entram no cálculo de dano.
10. As regras de condição seguem o D&D 5e. Em outros sistemas (Vampiro e sistemas próprios) a tabela de condições não é editável.

## 4. Ficha e dados
- Atributos novos por ficha: hoje a ficha só **esconde/mostra** os atributos do sistema (Escolher atributos). Criar um atributo só para um personagem exige um modelo novo (atributos próprios na ficha, ligados às fórmulas) e ainda não existe.
- A criação de ficha ainda não tem teste de interface, nem de gravação/leitura do visual (o `org.json` não roda em teste de unidade comum) nem da chave do avatar (fica em arquivo Compose).
- O app não tem papéis (mestre e jogador): qualquer pessoa que abre o app edita tudo. É o ponto mais distante do objetivo de mesa compartilhada.
- Inspiração existe como marcador e não é usada.
- Não existe **exportar/importar** (backup em arquivo). Tudo mora em `SharedPreferences` como JSON; reinstalar sem o backup automático do Android perde as campanhas. É a proteção mais barata que falta antes de qualquer beta com jogadores.
- `SharedPreferences` com todas as fichas em uma única string não escala bem; o plano já prevê Room.
- Textos fixos em português no código (sem `strings.xml`).

## 5. Produto (já listado em [`../arquitetura/visao-geral.md`](../arquitetura/visao-geral.md), continua tudo pendente)
Autenticação, campanhas multiusuário com convite, regras de autorização no servidor, sincronização offline-first (Room + Firestore), importação do JSON antigo, política de privacidade. Decisões em aberto: jogador edita a ficha livremente ou com aprovação do mestre; monstros e magias globais ou por campanha; o que o convidado enxerga; quais sistemas ganham adaptador completo primeiro.

## 6. Testes
- Cobertos: rolagens, fórmulas, fichas, condições, turnos, salvaguardas, durações.
- Não cobertos: `CombatStore` (grava e lê o combate, inclusive as durações), telas Compose e fluxo de ponta a ponta. Vale adicionar testes de `CombatStore` com Robolectric ou instrumentados, e um teste de interface do fluxo "falha → condição → expira".

## Sugestão de ordem
1. Compilar, corrigir o que a interface acusar, gerar o wrapper e limpar o repositório (itens 1 e 2).
2. Exportar/importar backup (item 4).
3. Magias e ações com CD próprio, metade do dano e repetição de salvaguarda (3.3 e 3.4).
