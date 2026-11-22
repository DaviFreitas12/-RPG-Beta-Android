# Motor de RPG — Beta 0.5.0

## Sintaxe de fórmulas
- `d20` ou `1d20`: um dado de 20 lados.
- `2d6 + 3`: dois dados de seis lados mais três.
- `1d20 + FOR + PROF`: rolagem com variáveis fornecidas pela ficha/tela.
- `(1d8 + FOR) * 2`: parênteses e operações `+`, `-`, `*` e `/` (divisão inteira).

Variáveis não cadastradas geram erro visível; divisão por zero também é bloqueada. A quantidade de dados aceita vai de 1 a 100 e os lados de 2 a 1000.

## Funcionalidades da tela Motor
- Executar teste contra CD e exibir sucesso/falha e margem.
- Rolagem rápida dos dados comuns.
- Criar combatentes com iniciativa rolada e ordenar por resultado.
- Avançar turno, aplicar/remover 1 PV e encerrar combate.
- Persistir a lista de combate no armazenamento local do aplicativo.

## Próximas camadas
1. Adaptadores de regra para cada sistema, sem presumir que todos usam CA, níveis ou PV.
2. Ataques, dano, resistência, condições, ações e recursos configuráveis.
3. Fichas dinâmicas conectadas às definições de atributos e recursos.
4. Persistência Room, migrações/versionamento e depois sincronização Firebase.

Este módulo é um motor inicial executável, não uma implementação completa das regras oficiais de qualquer sistema comercial.
