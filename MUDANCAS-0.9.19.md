# 0.9.19 · Bestiário SRD

- **Importação automática**: na primeira abertura do app com esta versão, as 28 criaturas entram sozinhas no Bestiário (uma vez só; o que você apagar depois não volta).
- Botão **Importar bestiário SRD 5.1 (28 criaturas)** na tela inicial do Bestiário (abaixo das categorias).
- As criaturas vêm do SRD 5.1 (CC-BY-4.0), traduzidas e resumidas, em todas as 14 categorias padrão. As ações já seguem o formato que o motor rola (Ataque: +4 para acertar. Dano: 1d6+2 cortante).
- Cada criatura tem id fixo (`srd-...`): importar de novo não duplica e só traz o que estiver faltando. O botão some quando tudo já foi importado. Criaturas apagadas voltam a aparecer para importar.
- Quem edita uma criatura importada mantém a edição (a importação nunca sobrescreve).
- Sem imagens no catálogo: use o campo de retrato da ficha.
- Código: `data/SrdBestiary.kt`; teste: `SrdBestiaryTest`. **Não foi compilado** nesta análise.
