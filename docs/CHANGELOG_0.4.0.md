# RPG Sombrio — Beta 0.4.0

## Implementado nesta revisão
- Cartões de acesso rápido da tela Campanha agora navegam para as respectivas abas.
- A tela da campanha permite editar e salvar nome e descrição.
- O sistema selecionado na campanha é persistido no aparelho.
- Personagens, criaturas, magias e itens possuem cadastro, listagem, busca, edição e exclusão local.
- Os registros são persistidos em SharedPreferences como JSON e permanecem disponíveis sem internet.
- Registros iniciais demonstrativos podem ser editados ou excluídos.

## Limitações conhecidas
- Esta versão é offline-first, mas ainda não é híbrida com sincronização em nuvem.
- Firebase Authentication/Firestore e regras de segurança ainda precisam ser configurados.
- Os registros usam SharedPreferences/JSON; Room é recomendado antes de crescer o volume de dados.
- O construtor de sistemas ainda armazena fórmulas como texto; não há interpretador seguro de fórmulas.
- Não foi possível compilar nesta sessão porque o ambiente não possui Android SDK/Gradle.
- É necessário compilar e testar no GitHub Actions e em aparelho Android antes de considerar a versão validada.
