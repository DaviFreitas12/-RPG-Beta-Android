# RPG Sombrio — Beta 0.5.0

## Motor de RPG inicial
- Novo núcleo Kotlin `RpgEngine` para executar expressões de dados e aritmética com variáveis.
- Notação suportada: `d20`, `2d6 + 3`, `1d20 + FOR + PROF` e expressões com parênteses.
- Testes contra Classe de Dificuldade (CD), com sucesso/falha e margem.
- Atalhos de rolagem d4, d6, d8, d10, d12, d20 e d100.
- Primeira tela de iniciativa, ordenação automática e controle básico de PV.
- Lista de combate e fórmula preferida persistidas localmente via SharedPreferences.

## Limites conhecidos desta versão
- O motor ainda não implementa todas as regras oficiais de D&D, Vampiro ou outros sistemas.
- Ataques, ações, condições, magias, progressão, fichas dinâmicas e regras específicas precisam de adaptadores por sistema.
- A persistência ainda usa SharedPreferences; Room e sincronização Firebase não estão implementados nesta versão.
- A compilação Android final precisa ser validada pelo GitHub Actions e instalada para testes no aparelho.
