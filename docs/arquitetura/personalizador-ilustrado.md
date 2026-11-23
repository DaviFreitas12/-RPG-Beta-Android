# Character Studio — implementação incremental

## Direção de produto
O Studio é um módulo de criação visual separado dos atributos de jogo. A interface e o catálogo serão ilustrados em estética medieval Dark Fantasy, conforme a direção visual definida para esta evolução. O planejamento original descreve um editor de pixel art; essa representação será adaptada para assets ilustrados PNG com transparência e camadas, preservando os fluxos e critérios funcionais que continuam pertinentes.

## Entregue na 0.9.12
- Rota Character Studio acessível pela tela inicial.
- Criação, seleção e exclusão de projetos.
- Escolha inicial de base corporal (masculina, feminina e neutra), tom de pele, cabelo e armadura.
- Salvamento local automático em SharedPreferences/JSON.
- Nenhuma escolha visual altera atributos, defesa, recursos ou estatísticas da ficha.

## Ainda pendente
1. Produzir e validar assets originais ilustrados em PNG transparente, com proporções e pontos de ancoragem compartilhados.
2. Criar catálogo pesquisável por categoria, tags, favoritos e conjuntos.
3. Compor a prévia frontal por camadas, com cenários, zoom e inspeção/ocultação de camadas.
4. Ampliar personalização facial/corporal, paletas, materiais, marcas e efeitos estáticos.
5. Implementar versões, autosave atômico, backup, recuperação, lixeira e importação/exportação.
6. Permitir finalizar um personagem e vinculá-lo manualmente a uma ficha, sem alterar estatísticas.
7. Validar em Android real e executar testes de regressão do restante do RPG.

## Limites desta entrega
A área de prévia da tela atual é explicitamente provisória e não representa uma ilustração final. A persistência atual cobre os campos básicos do rascunho, mas ainda não é o sistema completo de proteção/backup previsto no planejamento. A meta de 220 peças deverá ser construída e validada em lotes; não serão usados sprites genéricos apenas para preencher quantidade.
