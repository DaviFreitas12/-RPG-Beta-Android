# 0.9.20 · Aba Jogar

A ficha do jogador era um formulário de edição: os bônus existiam, mas ficavam recolhidos, longe e misturados com campos. Agora a ficha abre na aba **Jogar**.

- **Jogar** (nova, abre por padrão): só leitura, tudo calculado. Atributos com o modificador grande e o valor bruto pequeno; pílulas de Prof., Deslocamento, Percepção passiva, PV temporários e Inspiração; salvaguardas (as proficientes destacadas); perícias; ataques com bônus e dano; magias com Conjurar; e o equipamento ativo (CA com a conta e bônus dos itens equipados). Tocar em qualquer item rola o dado.
- **Editar** (antes "Ficha"): os mesmos campos de antes, sem mudança.
- **Detalhe do bônus**: cada perícia mostra de onde vem o número ("FOR +3 • prof +2 • extra +1") e cada salvaguarda também ("DES +2 • prof +2").
- **Perícias favoritas**: estrela em cada perícia. A aba Jogar mostra as favoritas e as proficientes; "Ver todas as perícias" mostra o resto. Fichas antigas abrem sem favoritas (campo novo, opcional).
- **Condições valem na ficha**: aviso no topo da aba Jogar com o que a condição muda; ataques mostram "Vantagem"/"Desvantagem" e **rolam assim de verdade** (também na aba Combate); salvaguardas de Força/Destreza avisam "falha automática" ou "desvantagem".
- **Mini-painel fixo**: ao rolar a ficha, o topo passa a mostrar também os modificadores dos atributos (além de nome, PV e CA).
- **Combate**: Ataques e Magias vêm primeiro; o painel de Rolagens livres (fórmula, CD, CA do alvo) foi para o fim.
- Código: `engine/PlayView.kt` (lógica), `ui/characters/PlayTab.kt` (tela); teste: `PlayViewTest`. `NewCharacterTest` ajustado (a ficha abre em "Jogar"). **Não foi compilado** nesta análise.
