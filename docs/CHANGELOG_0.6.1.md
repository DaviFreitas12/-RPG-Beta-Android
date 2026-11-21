# RPG Sombrio — Beta 0.6.1 (refinamento, fase 1)

## Correções no motor
- Variáveis que começam com "D" (como `DES`) eram lidas como dado e quebravam a fórmula. Agora "d" só inicia um dado quando seguido de dígitos.
- Divisão arredonda para baixo (`-3 / 2 = -2`, `(8 - 10) / 2 = -1`), como no modificador de atributo.
- Ataque: 20 natural sempre acerta e dobra os dados do dano; 1 natural sempre erra. O resultado agora é um `AttackResult`.
- Nomes de variáveis ignoram acentos e maiúsculas; aceitam dígitos (`ATR1`).
- O motor recebe um `DiceRoller` injetável, o que permite testes determinísticos.
- Novo: `app/src/test/.../RpgEngineTest.kt` (aritmética, dados, variáveis, divisão, erros, ataques, críticos, `dndModifier`).

## Combate
- O turno ativo é guardado por id e persistido: adicionar/remover combatentes não troca a vez.
- Desempate de iniciativa por nome; "Próximo turno" pula combatentes caídos (0 PV).
- Mensagens de crítico e falha crítica.

## Interface e sistema
- Tema escuro no Manifest (sem flash branco), edge-to-edge, ícone adaptativo com o brasão.
- Brasão convertido de PNG (2,6 MB) para WebP (~40 KB).
- Retratos: copiados e reduzidos (512 px, JPEG) para o armazenamento do app, carregados fora da thread principal; arquivos órfãos são apagados. Retratos antigos (URIs da galeria) continuam funcionando.
- Navegação preservada ao girar a tela; botão Voltar retorna à Biblioteca.
- `google-services.json` removido (não usado; package inconsistente).
- CI agora roda `testDebugUnitTest` antes de `assembleDebug`.

## Não validado
Esta fase foi escrita sem acesso a Android SDK/Gradle. A primeira compilação e os testes devem ser confirmados pelo GitHub Actions ou pelo Android Studio.
