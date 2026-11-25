# 0.10.2 · Campanhas com mestre (etapas 1 a 3)

Plano completo em `docs/roadmap/PLANO-CAMPANHAS.pdf`. Esta versão entrega as três primeiras etapas.

## O que muda para quem usa
- **Aba Campanha** agora é uma lista. **Criar campanha** faz de você o mestre dela. Cada campanha tem nome, história, sistema de regras e um código de 4 dígitos. O papel (Mestre ou Jogador) vale por campanha.
- Uma campanha fica **ativa** (a da Home e a que define o sistema padrão de bestiário, itens e magias). Dá para **arquivar**, restaurar e **excluir**; ao excluir, as fichas dela voltam para "Sem campanha" (nada é apagado).
- **Novo personagem**: o app pergunta onde criar. Dentro de uma campanha, a ficha já nasce no sistema dela. Também dá para criar solta, em "Sem campanha". Na aba Campanha há o atalho "Criar personagem nesta campanha".
- **Lista de personagens** agrupada por campanha, com "Sem campanha" no fim.
- **Mover ficha**: em Ficha > Identidade, "Mover para campanha" (ou "Mover", se já está numa). Se o sistema da campanha é outro, o app avisa; nada é apagado (os dados do sistema antigo ficam guardados). Em campanha, o sistema da ficha é o da campanha (some o "Trocar sistema").
- **Sistema da campanha** só muda enquanto ela não tem personagens. Em Campanha > Sistemas, "Usar na campanha" avisa quando isso bloqueia.

## Migração (automática, na primeira abertura)
- A campanha única antiga (nome, descrição e sistema) vira a primeira da lista, com este aparelho como mestre e já ativa.
- As fichas que já existiam continuam em "Sem campanha". Mova para a campanha pelo Perfil da ficha.
- Recomendado: exporte/faça backup dos dados antes de instalar.

## Código
- `data/CampaignStore.kt` (armazenamento em JSON, migração), `data/CampaignRules.kt` (regras puras: papel, agrupamento, trava de sistema, fichas soltas).
- `CharacterSheet.campaignId` (campo novo, opcional: fichas antigas abrem sem campanha).
- `SheetStore.activeSystemId` agora segue a campanha ativa; `setActiveSystem` devolve `false` quando o sistema está travado; `moveToCampaign` move a ficha.
- Telas: `ui/campaign/CampaignScreen.kt` (reescrita), `CharacterSheetScreen.kt`, `HomeScreen.kt`, `SystemsScreen.kt`.
- Teste: `CampaignRulesTest`. **Não foi compilado** nesta análise.

## Ainda não existe (próximas etapas do plano)
Calculadora de pontos e trava de atributos (etapa 4), nível controlado pelo mestre (5), poderes do mestre (6) e conexão da campanha com a mesa em rede (7). Por enquanto todo mundo é mestre das campanhas que cria; jogadores aparecem na etapa 7.
