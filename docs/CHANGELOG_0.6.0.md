# RPG Sombrio — Beta 0.6.0

## Fundação e navegação
- Navegação principal reduzida a cinco destinos para evitar abas cortadas em telas estreitas.
- Motor de RPG promovido a destino principal e sempre visível.
- Nova central Biblioteca com atalhos para Bestiário, Grimório e Itens.
- A navegação antiga e as chaves de armazenamento local foram preservadas.

## Combate executável
- Novo painel de resolução de ataque no Motor de RPG.
- Seleção de atacante e alvo entre os combatentes da iniciativa.
- Fórmulas configuráveis de ataque e dano.
- Comparação do resultado de ataque com a defesa/CA do alvo.
- Dano aplicado automaticamente aos PV do alvo e salvo no armazenamento local.
- Mensagens distintas para acerto, erro e falha de validação.

## Limitações conhecidas desta etapa
- Os valores FOR, DES e PROF do teste ainda são informados manualmente no Motor.
- Fichas, itens, magias e criaturas ainda não estão conectados entre si.
- A persistência continua usando SharedPreferences/JSON; migração Room exige migrações e testes próprios e será feita em etapa separada para preservar dados.
- Não há sincronização em nuvem ou multijogador nesta versão.

## Validação
- O workflow GitHub Actions executa `gradle assembleDebug` e publica o APK como artefato.
- A compilação final precisa ser confirmada pelo workflow após o push.
