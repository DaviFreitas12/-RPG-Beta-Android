# RPG Sombrio — Beta 0.6.1

Aplicativo Android de RPG multissistemas (Kotlin + Jetpack Compose), com identidade visual medieval sombria.

## O que existe hoje
- **Campanha**: nome, descrição e sistema da campanha em destaque (salvos localmente).
- **Personagens, Bestiário, Grimório, Itens**: CRUD local (criar, buscar, editar, excluir). Personagens aceitam retrato da galeria; a imagem é copiada, reduzida e guardada no próprio app.
- **Sistemas**: construtor de atributos, recursos, perícias, dados e fórmulas, salvo no aparelho.
- **Motor**: rolagens (d4–d100), fórmulas com dados e variáveis (`1d20 + DES + PROF`), testes contra CD, iniciativa com turnos e resolução de ataque (20 natural acerta e dobra os dados do dano; 1 natural erra).
- Persistência em SharedPreferences/JSON, funcionando offline.

## O que ainda NÃO existe
- Contas, login, campanhas multiusuário e sincronização em nuvem (Firebase não está configurado).
- Banco Room.
- Fichas estruturadas: atributos e PV das fichas ainda não alimentam o Motor (os valores FOR/DES/PROF do Motor são manuais).
- O sistema escolhido na campanha ainda não altera o comportamento do Motor.

Veja `docs/CHANGELOG_0.6.1.md` para o que mudou nesta versão e `docs/ANALISE_E_ARQUITETURA.md` para o roteiro.

## Compilar
1. Android Studio estável, Android SDK 35 e JDK 17 ou 21.
2. Abra a pasta que contém `settings.gradle.kts` e aguarde o sync do Gradle.
3. APK de teste: **Build > Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
4. Testes do motor: `gradle testDebugUnitTest`.

O workflow `.github/workflows/android.yml` roda os testes e gera o APK debug como artefato a cada push.

## Identidade
- Nome exibido: RPG · Application ID: `com.example.rpgsombrio` · Versão 0.6.1 (código 7) · minSdk 26

## Firebase
O `google-services.json` foi removido do projeto (não estava em uso e apontava para um package diferente). Ao ativar o Firebase, baixe um novo arquivo para o package `com.example.rpgsombrio`, coloque-o em `app/` e aplique o plugin `com.google.gms.google-services`.
