# Pixel Art Studio — Etapa 1: núcleo 64×64

## Objetivo
Estabelecer um motor independente da interface Compose para editar documentos 64×64 com camadas RGBA, operações puras, histórico e persistência validada.

## Contratos principais
- `PixelDocument`: resolução fixa 64×64, entre 1 e 64 camadas e até 64 cores de paleta.
- `PixelLayer`: pixels encapsulados; `pixelsCopy()` sempre devolve cópia defensiva.
- `PixelDrawing`: operações imutáveis, preservando a camada original.
- `PixelHistory`: snapshots profundos e limite de histórico configurável (padrão recomendado: 40).
- `PixelViewport`: mapeamento de coordenadas sem suavização.
- `PixelProjectCodec`: formato binário versionado com CRC32 e validação de dimensões/limites.
- `PixelProjectRepository`: escrita temporária, validação antes da troca, sincronização e backup do arquivo anterior.

## Limites desta entrega
A etapa não inclui interface Compose, tela de projetos, lixeira de 30 dias, importação pelo Storage Access Framework, exportação PNG, biblioteca de assets, montagem de personagens ou integração com a ficha. Esses módulos serão implementados após validação do núcleo.

## Próxima etapa
Criar a tela Compose mobile-first e conectar o editor à navegação existente sem remover nem renomear destinos atuais do RPG.
