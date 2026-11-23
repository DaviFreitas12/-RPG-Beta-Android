# Character Studio — pipeline de artes V1

## Objetivo

O Character Studio utiliza artes PNG prontas, transparentes e compostas pelo aplicativo. O jogador escolhe opções do catálogo; não existe edição manual de pixels, desenho, deformação ou reposicionamento livre.

## Local dos arquivos

- Manifesto: `app/src/main/assets/studio/catalog.json`
- Artes: `app/src/main/assets/studio/<categoria>/<arquivo>.png`

## Canvas e alinhamento

Na V1, o manifesto define um canvas de referência de 512 × 768. Todas as camadas que pertencem ao mesmo conjunto corporal devem ser exportadas com exatamente as mesmas dimensões, origem e enquadramento. A arte pode ocupar apenas parte do canvas, mas o PNG deve preservar o canvas completo e transparência no restante.

Não recortar cada peça até sua borda visível: isso quebra o alinhamento automático entre camadas.

## Ordem de composição

O renderer usa `zIndex` do asset quando diferente de zero. Caso contrário, aplica a ordem padrão por slot definida em `StudioCharacterPreview.kt`. Use `zIndex` explícito quando uma peça precisar atravessar outra (por exemplo, cabelo atrás do corpo ou uma capa atrás do tronco).

## Exemplo de entrada no catálogo

```json
{
  "id": "body_masculine_base_01",
  "name": "Base masculina — 01",
  "slot": "BODY",
  "resourcePath": "studio/body/body_masculine_base_01.png",
  "bodyTypes": ["MASCULINE"],
  "styles": ["DARK_FANTASY"],
  "rarity": "COMMON",
  "zIndex": 0,
  "tags": ["base", "humanoide"],
  "handsRequired": 0,
  "isStarterAsset": true
}
```

## Regras de produção

1. PNG com transparência real (RGBA).
2. Mesmo canvas e enquadramento para todas as camadas compatíveis.
3. Sem texto, molduras ou fundo incorporado à arte do personagem.
4. Não usar recoloração automática como substituto de variantes de material.
5. Nomear IDs de forma estável; não reutilizar um ID para outra arte.
6. Declarar `bodyTypes`, `slot`, `zIndex` e `resourcePath` corretamente.
7. Validar visualmente todas as combinações antes de publicar o lote.

## Estado atual

O carregador de manifesto e o renderer de camadas foram adicionados como fundação técnica. O catálogo começa vazio de propósito: nenhuma arte provisória ou genérica é apresentada como conteúdo final. O próximo passo é ligar a tela de criação ao modelo de domínio `StudioCharacter`/`StudioProject` e adicionar o primeiro lote de artes reais.
