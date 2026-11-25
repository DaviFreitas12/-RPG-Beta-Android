# 0.10.3 · Calculadora de pontos e trava de criação (etapa 4)

Plano completo em `docs/roadmap/PLANO-CAMPANHAS.pdf`. Esta versão entrega a etapa 4.

## O que muda para quem usa
- **Ficha nova de D&D 5e / 2024 já nasce "em criação".** Em Editar > Atributos aparece a calculadora: cada atributo tem o valor **base** (compra), o **bônus de raça/antecedente** e o **valor final**.
- **Compra de pontos (padrão do D&D)**: 27 pontos, valores de 8 a 15 antes dos bônus. Custos: 8 = 0, 9 = 1, 10 = 2, 11 = 3, 12 = 4, 13 = 5, 14 = 7, 15 = 9. Os botões − e + não deixam passar do orçamento nem sair da faixa.
- **Array padrão**: 15, 14, 13, 12, 10, 8, um valor para cada atributo (dá para trocar entre eles; o app avisa se repetir ou usar valor fora do array).
- **Rolagem**: você digita o valor tirado nos dados da mesa (de 3 a 18). O app não rola nada.
- **Bônus de raça e antecedente** entram depois da compra (de −2 a +6 por atributo). O valor final **para em 20**, e os pontos gastos não mudam com o bônus.
- **Finalizar criação** trava os atributos. Só é possível com a regra cumprida (nunca com mais de 27 pontos gastos).
- **Reabrir criação**: o mestre da campanha (ou o dono de uma ficha solta) vê o botão na ficha finalizada. É um adiantamento mínimo da etapa 6, para uma ficha finalizada por engano não ficar presa. A edição livre completa do mestre continua na etapa 6.
- **O mestre escolhe o método** em Campanha > Criação de personagem. No método **Livre** os atributos não têm limite e fichas finalizadas destravam.
- **Outros sistemas** (Vampiro, Sistema próprio, sistemas criados por você) ficam livres, como no plano. O motor já aceita um orçamento definido pelo sistema (`linearPointBuyRule`) para ativar depois.

## O que NÃO muda
- **Fichas que já existem** continuam com edição livre (não entram em criação nem travam). Mover uma ficha de campanha também não a trava.
- Rolagens, aba Jogar e combate seguem lendo o valor final em `attributes`, sem mudança.

## Código
- `data/CreationRules.kt` (regras puras): `CreationRule`, `creationRuleFor`, `checkCreation`, `startCreation`, `stepBaseScore`, `setBaseScore`, `setAttributeBonus`, `finalizeCreation`, `reopenCreation`, `attributesLocked`.
- `CharacterSheet.creation` / `baseAttributes` / `attributeBonuses` e `Campaign.creationMethod` (campos novos, opcionais).
- Telas: `ui/characters/CreationPanel.kt` (nova), `CharacterSheetScreen.kt`, `SheetComponents.kt` (`AttributeGrid` ganhou `locked`), `CampaignScreen.kt`.
- Teste: `CreationRulesTest` (36 casos). Os testes do motor foram compilados e rodados na JVM; a interface **não foi compilada** nesta análise.

## Ainda não existe (próximas etapas)
Nível controlado pelo mestre (5), poderes do mestre (6) e conexão da campanha com a mesa em rede (7).
