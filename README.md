# RPG — Beta 0.3.0 (projeto Android)

Aplicativo Android de RPG multissistemas, com identidade visual medieval sombria.

## O que esta versão contém
- Interface Android em Kotlin + Jetpack Compose.
- Navegação inicial: Campanha, Personagens, Bestiário, Grimório, Itens e Sistemas.
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
- CRUD completo para bestiário, magias e itens.
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
- Versão: 0.3.0 (versionCode 3)
- minSdk: 26 (Android 8.0)

## Roteiro de teste da beta
1. Abra o app e navegue por todas as abas.
2. Entre em Personagens, reduza/aumente o HP e feche/reabra o app para verificar persistência.
3. Entre em Sistemas, adicione/remova atributos, recursos e perícias; altere dados e fórmulas.
4. Toque em “Salvar sistema neste aparelho”, feche/reabra o app e confirme que os campos continuam salvos.
5. Teste em outro aparelho apenas depois da implementação da sincronização: nesta versão os dados não são compartilhados entre celulares.

## Limitação desta entrega
O ambiente de preparação não possui Android SDK nem Gradle instalados, portanto não foi possível compilar ou validar um APK nesta sessão. O ZIP contém o projeto-fonte atualizado para compilação no Android Studio; não é um APK instalável.
