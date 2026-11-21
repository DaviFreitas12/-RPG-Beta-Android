# RPG — Beta 0.4.0 (projeto Android)

Aplicativo Android de RPG multissistemas, com identidade visual medieval sombria.

## O que esta versão contém
- Interface Android em Kotlin + Jetpack Compose.
- Navegação entre Campanha, Personagens, Bestiário, Grimório, Itens e Sistemas.\n- CRUD local de personagens, criaturas, magias e itens: criar, listar, buscar, editar e excluir.\n- Edição local do nome, descrição e sistema da campanha em destaque.\n- Persistência dos registros em SharedPreferences/JSON, disponível offline.
- Brasão RPG incorporado na tela inicial.
- Ficha de demonstração de Davi / Hemomante.
- Controle de HP com persistência local no aparelho.
- Construtor inicial de sistemas: atributos, recursos, perícias, dados e fórmulas editáveis.
- Salvamento local do sistema personalizado em SharedPreferences/JSON.
- Modelos de dados para personagens, monstros, magias, itens e membros de campanha.

## O que ainda NÃO está pronto
- Login real e contas de usuário.
- Campanhas multiusuário e convites reais.
- Sincronização em nuvem (Firebase ainda não configurado).
- Banco Room; nesta beta, persistência local é limitada a preferências do Android.
- Motor que interprete e execute fórmulas personalizadas. As fórmulas são armazenadas como texto.
- Banco Room; a persistência desta beta usa SharedPreferences/JSON e é adequada apenas para um volume pequeno de dados.\n- Login real, contas e sincronização em nuvem (Firebase ainda não configurado).\n- Campanhas múltiplas, convites e permissões multiusuário.\n- CRUD completo de fichas com atributos estruturados e motor de combate.
- Publicação/importação comunitária de sistemas.
- Testes em dispositivos físicos e APK assinado.

## Como gerar o APK no Android Studio
1. Instale Android Studio estável com Android SDK Platform 35 e JDK 17 ou 21.
2. Extraia este ZIP e abra a pasta `RPGSombrio` (a pasta que contém `settings.gradle.kts`).
3. Aguarde o Gradle sincronizar e aceite a instalação das dependências.
4. Execute em um emulador ou conecte um celular com Depuração USB ativada.
5. Para gerar APK de teste: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. O APK debug normalmente será criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Identidade
- Nome exibido: RPG
- Application ID: `com.example.rpgsombrio`
- Versão: 0.3.0 (versionCode 4)
- minSdk: 26 (Android 8.0)

## Roteiro de teste da beta
1. Abra o app e navegue por todas as abas.
2. Entre em Personagens, reduza/aumente o HP e feche/reabra o app para verificar persistência.
3. Entre em Sistemas, adicione/remova atributos, recursos e perícias; altere dados e fórmulas.
4. Toque em “Salvar sistema neste aparelho”, feche/reabra o app e confirme que os campos continuam salvos.
5. Teste em outro aparelho apenas depois da implementação da sincronização: nesta versão os dados não são compartilhados entre celulares.

## Limitação desta entrega
O ambiente de preparação não possui Android SDK nem Gradle instalados, portanto não foi possível compilar ou validar um APK nesta sessão. O ZIP contém o projeto-fonte atualizado para compilação no Android Studio; não é um APK instalável.


## Beta 0.5.0 — Motor de RPG inicial
A aba **Motor** executa rolagens (d4 a d100), fórmulas aritméticas com dados e variáveis (`1d20 + FOR + PROF`), testes contra CD e iniciativa com acompanhamento básico de PV. Consulte `docs/CHANGELOG_0.5.0.md` e `docs/MOTOR_RPG.md` para o escopo e as limitações atuais.


## Beta 0.6.0

A navegação foi reorganizada para cinco destinos principais, com o Motor de RPG sempre visível e uma central Biblioteca para Bestiário, Grimório e Itens. O Motor agora permite resolver ataques entre combatentes, comparar o resultado com a defesa/CA e aplicar automaticamente o dano aos pontos de vida, persistindo a alteração localmente.

Esta versão é uma etapa incremental: atributos ainda são informados manualmente no motor e as fichas/bibliotecas ainda não estão totalmente integradas. Consulte `docs/CHANGELOG_0.6.0.md` para limitações e próximos passos.

## Atualização visual medieval

Esta edição inclui uma primeira etapa de renovação visual da interface:
- paleta de madeira escura, couro, vinho e dourado;
- cartões com contornos dourados discretos e cantos mais suaves;
- seleção de retrato pela galeria nas fichas de personagens;
- persistência do URI do retrato junto aos registros locais, preservando registros antigos sem imagem.

A compilação Android deve ser validada no ambiente Android/Gradle do projeto antes de gerar o APK final.
