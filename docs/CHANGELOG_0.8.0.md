# RPG Sombrio — Beta 0.8.0 (itens estruturados e inventário da ficha)

## Itens
- Item com identidade (nome, tipo com atalhos, raridade), detalhes (peso numérico, valor, bônus, dano/efeito, propriedades, sintonização) e descrição.
- Lista com busca, filtro por tipo e etiquetas de bônus, dano, peso e valor.
- Registros simples antigos (`records_items`) são migrados; o peso é extraído de textos como "Peso: 0,5".

## Inventário na ficha
- A seção "Equipamento" virou **Inventário**: itens vinculados à biblioteca, com quantidade (−/+) e marcação de equipado.
- Peso total calculado (peso × quantidade) e resumo "N itens • X kg • Y equipados".
- Itens apagados da biblioteca aparecem como "Item removido da biblioteca" até você remover a linha.
- O campo livre de antes continua disponível como "Itens avulsos, moedas e anotações" (dados antigos preservados).

## Limpeza
- Removido o antigo CRUD genérico de registros (`LocalRecordsScreen`/`SimpleLibraryScreen`): todas as abas da Biblioteca e Personagens agora usam telas estruturadas.

## Não validado
Escrito sem Android SDK/Gradle. Compile e teste pelo Android Studio ou GitHub Actions antes de usar.
