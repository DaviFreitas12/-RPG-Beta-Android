# RPG Sombrio — Beta 0.9.3

Aplicativo Android de RPG multissistemas (Kotlin + Jetpack Compose), com identidade visual medieval sombria.

## O que existe hoje
- **Campanha**: nome, descrição e sistema da campanha em destaque (salvos localmente).
- **Personagens, Bestiário, Grimório**: fichas estruturadas (seções recolhíveis, guiadas pelo sistema da campanha). Personagens e criaturas aceitam retrato da galeria; a imagem é copiada, reduzida e guardada no próprio app.
- **Itens**: biblioteca estruturada (tipo, peso, bônus, dano, propriedades). A ficha tem um **Inventário** ligado a ela, com quantidade, equipado e peso total.
- **Sistemas**: biblioteca de sistemas criados por você (atributos com limites e modificador configurável, recursos, perícias ligadas a atributos, rótulos, blocos da ficha, dados e fórmulas). D&D 5e, D&D 2024 e Vampiro são modelos que podem ser duplicados e editados.
- **Vínculo**: a campanha tem um sistema; cada ficha nasce nele, mas pode ser vinculada a qualquer outro (nada é apagado ao trocar).
- **Vantagens e desvantagens**: biblioteca de traços com custo em pontos, por sistema. A ficha mostra pontos gastos, devolvidos e livres (orçamento definido no sistema).
- **Ficha ↔ Motor (0.9.1)**: atributos, perícias, recursos e magias viram variáveis e botões de rolar; criaturas rolam ações e entram no combate; magias debitam custo; PV sincronizados com a ficha; sistemas de reserva de dados contam sucessos.
- **Aparência (0.9.3)**: criador de personagem em pixel art, corpo inteiro, desenhado por código (sem imagens): raça (humano, elfo, anão, orc, tiefling, halfling), corpo, pele, rosto, cabelo, olhos, boca, barba, detalhes, roupa, capa, elmo/chapéu e arma, com luz, sombra e contorno automáticos. Mais campos de descrição. O avatar aparece na lista de personagens e na arena quando não há retrato da galeria.
- **Motor**: rolagens (d4–d100), fórmulas com dados e variáveis (`1d20 + DES + PROF`), testes contra CD, iniciativa com turnos e resolução de ataque (20 natural acerta e dobra os dados do dano; 1 natural erra).
- Persistência em SharedPreferences/JSON, funcionando offline.

## O que ainda NÃO existe
- Contas, login, campanhas multiusuário e sincronização em nuvem (Firebase não está configurado).
- Banco Room.
- Itens equipados alterando atributos (a CA já é alterada), dados de fome e pares de 10 em reservas de dados.

Veja `docs/CHANGELOG_0.9.1.md` para o que mudou nesta versão e `docs/ANALISE_E_ARQUITETURA.md` para o roteiro.

## Compilar
1. Android Studio estável, Android SDK 35 e JDK 17 ou 21.
2. Abra a pasta que contém `settings.gradle.kts` e aguarde o sync do Gradle.
3. APK de teste: **Build > Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
4. Testes do motor: `gradle testDebugUnitTest`.

O workflow `.github/workflows/android.yml` roda os testes e gera o APK debug como artefato a cada push.

## Identidade
- Nome exibido: RPG · Application ID: `com.example.rpgsombrio` · Versão 0.9.3 (código 13) · minSdk 26

## Firebase
O `google-services.json` foi removido do projeto (não estava em uso e apontava para um package diferente). Ao ativar o Firebase, baixe um novo arquivo para o package `com.example.rpgsombrio`, coloque-o em `app/` e aplique o plugin `com.google.gms.google-services`.
