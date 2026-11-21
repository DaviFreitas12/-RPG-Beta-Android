# RPG Sombrio — Beta 0.9.0 (sistemas livres e vantagens/desvantagens)

## Sistemas
- Vários sistemas próprios, em vez de um único "Sistema próprio". Aba Sistemas: lista, novo em branco, duplicar e editar.
- D&D 5e, D&D 2024 e Vampiro viram modelos de fábrica: duplique para criar a sua variante.
- Atributos com sigla, valor padrão, mínimo/máximo e modificador configurável (piso((valor − base) ÷ divisor)); o modificador deixou de ser sempre o do D&D.
- Recursos, perícias ligadas a atributo, rótulos de conceito/origem, dados e fórmulas por sistema.
- Blocos da ficha ligáveis: nível, CA, perícias, ataques, magias, inventário, vantagens e desvantagens.
- Um sistema em uso por fichas não pode ser excluído.
- O "Sistema próprio" antigo (chaves `custom_*`) é migrado automaticamente para um sistema com id `custom`, mantendo as mesmas chaves; as fichas existentes não mudam.

## Vínculo
- A campanha guarda o id do sistema (`active_campaign_system_id`); o nome antigo continua sendo aceito.
- "Nova ficha" pergunta o sistema (o da campanha vem primeiro). Na ficha, "Trocar sistema" vincula a outro sem apagar valores.
- A ficha oferece "Adicionar perícias do sistema" para trazer perícias criadas depois.

## Vantagens e desvantagens
- Nova biblioteca (Biblioteca > Vantagens e desvantagens): nome, tipo, custo, categoria, sistema (ou todos) e descrição.
- Vantagens gastam pontos, desvantagens devolvem. Seção na ficha com gastos, devolvidos e livres; aviso quando passa do orçamento.

## Não validado
Escrito sem Android SDK/Gradle. Compile e rode `gradle testDebugUnitTest` (há testes novos em `SystemsTest`) antes de usar.
