# RPG Sombrio — Beta 0.7.0 (fichas, bestiário e grimório estruturados)

## Fichas de personagem
- Ficha guiada pelo sistema da campanha: atributos, recursos, perícias e rótulos mudam conforme o sistema (D&D 5e/2024, Vampiro, Sistema próprio/Outro, que usa o Construtor de sistemas).
- Tela única com seções recolhíveis: Identidade, Recursos, Atributos, Perícias, Ataques, Magias, Equipamento e Anotações.
- Recursos com barra e botões -/+ (PV, dados de vida, mana…); modificadores de atributo calculados; perícias com proficiência.
- Magias da ficha são vinculadas ao Grimório (escolha por checklist).
- Edição ao vivo: tudo é salvo a cada alteração.

## Bestiário
- Criatura com identidade, combate (CA, PV, fórmula de PV, deslocamento), atributos, defesas/sentidos/idiomas.
- Traços, Ações, Reações e Ações lendárias como listas de entradas nome + descrição.
- Filtro por tipo e busca.

## Grimório
- Magias agrupadas por nível, com filtro por nível e busca.
- Card expansível com execução, alcance, componentes, duração, custo, classes e descrição; marcadores de concentração e ritual.
- Editor em seções: Básico, Conjuração e Efeito.

## Dados
- Novos formatos JSON em SharedPreferences (`sheets_characters`, `sheets_bestiary`, `sheets_spells`).
- Registros simples antigos (`records_*`) são migrados na primeira abertura; as chaves antigas não são apagadas.
- Itens continuam no formato simples (próxima etapa).

## Não validado
Escrito sem Android SDK/Gradle. Compile e teste pelo Android Studio ou GitHub Actions antes de usar.
