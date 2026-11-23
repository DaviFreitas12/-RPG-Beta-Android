# RPG

Aplicativo Android de RPG de mesa **para qualquer sistema e qualquer estilo de jogo** (fantasia, terror, ficção científica, investigação...), feito em Kotlin e Jetpack Compose. Funciona 100% offline, com modo claro e escuro.

**Versão:** 0.9.9 (beta) · **Application ID:** `com.example.rpgsombrio` · **minSdk:** 26 · **targetSdk:** 35

## Funcionalidades

- **Início** — tela de abertura enxuta: campanha atual e atalhos para personagens e dados.
- **Campanha** — nome, descrição e sistema de regras.
- **Configurações** — aparência (claro, escuro ou do celular) e informações do app.
- **Personagens, Bestiário e Grimório** — criação guiada (nome, classe e origem), escolha de quais atributos aparecem em cada ficha, fichas com cabeçalho (avatar, PV, CA, iniciativa) e abas Ficha, Combate, Itens e Perfil, guiadas pelo sistema da campanha. Retrato da galeria ou avatar em pixel art.
- **Itens e Traços** — biblioteca de itens (tipo, peso, bônus, dano) e de vantagens/desvantagens com custo em pontos; a ficha tem inventário e orçamento de pontos.
- **Sistemas** — construtor de sistemas próprios (atributos, recursos, perícias, dados, fórmulas). D&D 5e, D&D 2024 e Vampiro vêm como modelos editáveis.
- **Motor de regras** — rolagens (d4–d100), fórmulas com variáveis (`1d20 + DES + PROF`), testes contra CD, iniciativa, turnos, ataques, salvaguardas, condições com duração e salvaguardas contra a morte.
- **Arena de combate** — combate com criaturas e personagens, PV sincronizados com a ficha.
- **Visual neutro e colorido** — modo claro e escuro; cada tipo de conteúdo tem a sua cor.
- **Criador de aparência** — personagem em pixel art de corpo inteiro, desenhado por código.

Os dados ficam no aparelho (SharedPreferences/JSON).

## Estrutura do projeto

```
app/src/main/java/com/example/rpgsombrio/
├── MainActivity.kt        Ponto de entrada
├── engine/                Regras do jogo (Kotlin puro, sem Android) — testadas
│   ├── RpgEngine.kt         dados, fórmulas, ataques, iniciativa
│   ├── SheetEngine.kt       ficha → variáveis e rolagens
│   ├── Conditions.kt        condições e turnos
│   ├── Saves.kt             salvaguardas
│   └── Death.kt             morte e estabilização
├── data/                  Modelos e persistência local
│   ├── RpgModels.kt · SheetModels.kt · SystemModels.kt
│   └── UserSystems.kt · CombatStore.kt
└── ui/                    Interface (Jetpack Compose)
    ├── RpgApp.kt            navegação principal
    ├── theme/               paleta clara/escura e tema
    ├── components/          componentes reutilizáveis
    ├── home/ · campaign/    tela de abertura e campanha
    ├── library/ · settings/ biblioteca e configurações
    ├── characters/ · bestiary/ · spellbook/ · items/ · traits/ · systems/
    ├── combat/              motor, arena de batalha e painel de salvaguardas
    └── avatar/              criador de aparência e sprites em pixel art

app/src/test/              Testes unitários do motor e dos modelos
docs/                      Arquitetura, roadmap e exemplos
CHANGELOG.md               Histórico de versões
```

A pasta `engine/` não depende de Android, o que permite testar as regras sem emulador.

## Como compilar

Requisitos: Android Studio estável, Android SDK 35 e JDK 17 ou 21.

1. Abra a pasta que contém `settings.gradle.kts` e aguarde o sync do Gradle.
2. APK de teste: **Build > Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
3. Testes: `gradle testDebugUnitTest`.

O workflow [`.github/workflows/android.yml`](.github/workflows/android.yml) roda os testes e gera o APK debug a cada push.

## Limitações conhecidas

- Sem contas, login, campanhas multiusuário ou sincronização em nuvem.
- Sem banco Room; sem exportar/importar backup (reinstalar o app pode perder os dados).
- Itens equipados ainda não alteram atributos (a CA já é alterada).

Veja o [roadmap](docs/roadmap/pendencias.md) para o plano completo.

## Firebase

Não está configurado. Ao ativar, baixe um novo `google-services.json` para o pacote `com.example.rpgsombrio`, coloque-o em `app/` e aplique o plugin `com.google.gms.google-services`. O arquivo é ignorado pelo git de propósito.

## Documentação

- [Índice da documentação](docs/README.md)
- [Histórico de versões](CHANGELOG.md)
