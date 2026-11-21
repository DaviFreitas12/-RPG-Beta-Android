# Construtor de sistemas — escopo funcional

## Editor incluído no protótipo
- Nome e descrição do sistema.
- Adicionar e remover atributos.
- Adicionar e remover recursos (PV, mana, sanidade etc.).
- Adicionar e remover perícias.
- Selecionar dados disponíveis (d4, d6, d8, d10, d12, d20, d100).
- Campos de fórmula de teste e iniciativa.
- Resumo da estrutura da ficha.

**Limitação atual:** o editor demonstrativo mantém as alterações em memória durante a tela. Ainda não salva no Room/Firestore, não executa fórmulas, não valida expressões e não sincroniza com outros usuários.

## Modelo persistente recomendado
Cada definição deve possuir `id`, `ownerUserId`, `name`, `description`, `version`, `createdAt`, `updatedAt`, `attributes[]`, `resources[]`, `skills[]`, `dice[]`, `formulas{}`, `customFields[]` e `ruleOptions{}`.

Cada campanha aponta para `systemDefinitionId` e `systemVersion`. Ao atualizar um sistema, publicar nova versão em vez de alterar definições já usadas por campanhas existentes. Isso protege fichas e históricos antigos.

## Motor de regras
As fórmulas não devem ser avaliadas com `eval`. Implementar um parser limitado com tokens permitidos (números, operadores, parênteses, identificadores autorizados e expressões de dados), limites de complexidade e testes unitários. Nunca executar código arbitrário enviado por usuários.

## Próximas entregas
1. Persistência Room e exportação/importação JSON.
2. Validação e motor de fórmulas com testes.
3. Construtor de campos personalizados (texto, número, lista, booleano).
4. Adaptadores de ficha por versão de sistema.
5. Sincronização Firestore e regras de acesso por campanha.


## Atualização 0.9.0
O construtor passou a ser uma biblioteca de sistemas persistida no aparelho (`user_systems`, JSON). Veja `CHANGELOG_0.9.0.md`. Continuam pendentes: execução das fórmulas no Motor, versionamento de sistemas e Room/Firestore.
