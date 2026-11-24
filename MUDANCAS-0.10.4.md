# 0.10.4 · Nível controlado pelo mestre (etapa 5)

Plano completo em `docs/roadmap/PLANO-CAMPANHAS.pdf`. Esta versão entrega a etapa 5.

## O que muda para quem usa
- **Em campanha de D&D o campo Nível fica travado** (Ficha > Perfil). Fichas soltas e de outros sistemas continuam livres.
- **Mestre sobe o nível**: na aba Campanha, em cada personagem (**Subir** / **Remover**) ou em **Subir nível de todos**; também em Ficha > Perfil. Quem já está no nível 20, tem level up pendente ou ainda está em criação não sobe (o app diz o motivo).
- **Jogador conclui o level up**: a ficha mostra "Subiu de nível" com o botão **Concluir level up**:
  - **Vida** pelo método da campanha. *Média fixa* (padrão): metade do dado de vida arredondada para cima, mais o modificador de Constituição. *Rolagem*: você digita o valor tirado no dado da mesa (o app não rola). Nunca ganha menos de 1. O dado de vida da classe (d6, d8, d10, d12) fica guardado na ficha.
  - **Atributos**: +2 pontos, só nos níveis 4, 8, 12, 16 e 19, com teto 20 por atributo. Os pontos entram antes da vida (aumentar Constituição já conta no nível).
  - **Características e magias** novas: só uma anotação no histórico; o app não aplica nada.
  - **Bônus de proficiência** muda sozinho com o nível (2 a 6).
- **Remover nível** (mestre, com confirmação) desfaz exatamente o que o nível concedeu: vida e pontos de atributo, inclusive os já gastos. O dado de vida da classe continua guardado.
- **Histórico**: Perfil mostra os três últimos níveis (ex.: "Nível 4: +9 PV, +2 FOR").
- **Método de vida** em Campanha > Vida no level up (mestre escolhe).

## Limites desta versão
- Subir nível não dá vida nem atributos sozinho: o jogador precisa concluir o assistente. Os dois celulares só se falam na etapa 7; por enquanto tudo acontece neste aparelho.
- Fichas anteriores a esta versão não têm histórico. Remover o nível delas só volta o nível e a proficiência (o app avisa).
- O aumento por classe fora da regra (Guerreiro nos níveis 6 e 14, Ladino no 10) fica para a etapa 6.
- Aumento retroativo de vida por Constituição não é calculado.

## Código
- `data/LevelRules.kt`: `levelRulesFor`, `grantLevelUp`, `completeLevelUp`, `removeLevel`, `pendingLevelUp`, `levelLocked`, `HpMethod`.
- `CharacterSheet.levelHistory` e `hitDie`; `Campaign.hpMethod`. A calculadora de criação (0.10.3) passou a somar os aumentos de nível ao reabrir.
- Telas: `ui/characters/LevelUpAssistant.kt` (nova), `CharacterSheetScreen.kt`, `CampaignScreen.kt`.
- Teste: `LevelRulesTest` (33 casos). Motor compilado e testado na JVM; a interface **não foi compilada**.
