# 0.10.5 · Poderes e exceções do mestre (etapa 6)

Plano completo em `docs/roadmap/PLANO-CAMPANHAS.pdf`. Esta versão entrega a etapa 6. Parte da 0.10.4 (etapas 1 a 5 já validadas por você).

## O que muda para quem usa

**Edição livre (mestre)**
- Na ficha, em **Perfil > Ferramentas do mestre**, o mestre da campanha liga **Edição livre**. Ligada, os atributos de uma ficha finalizada ficam editáveis (itens mágicos, bênçãos, regras da casa) e aparece **Definir nível livremente…**.
- Definir nível para baixo desfaz exatamente o que cada nível removido concedeu (vida e pontos de atributo, mesmo já gastos). Para cima, só sobe o nível e a proficiência: vida e atributos você concede por **Subir nível** ou por **Atributo extra**. Pede confirmação antes de aplicar.
- A chave é por ficha e desliga sozinha ao sair da ficha. Jogador nunca vê esta área.

**Atributo extra (exceções de classe)**
- O mestre concede de 1 a 8 pontos de atributo fora dos níveis 4, 8, 12, 16 e 19, com um motivo opcional (atalhos: Guerreiro nível 6 e 14, Ladino nível 10, Bênção ou item mágico). Pela ficha (Perfil > Ferramentas do mestre) ou pela aba Campanha, botão **Atributo extra** em cada personagem.
- O jogador vê o aviso **"O mestre concedeu"** no topo da ficha e distribui os pontos (teto 20 por atributo, precisa gastar tudo, a menos que não caiba).
- **Retirar** desfaz exatamente o que foi somado, mesmo se já distribuído. As concessões entram na conta dos atributos: reabrir a criação não as perde.
- Só em sistemas com regra de nível (D&D 5e e 2024); nos outros a ficha já é livre.

**Reabrir a criação**
- Além do botão que já existia em Editar > Atributos, o mestre tem **Reabrir criação** nas Ferramentas do mestre. Reabrir mantém base e bônus.

**Visibilidade**
- Em campanha, o **jogador vê só as próprias fichas** e o **mestre vê todas**. Vale na lista de personagens, na aba Campanha e na escolha de personagem da Mesa. Fichas soltas aparecem sempre.
- **Bestiário, encontros e combate são ferramentas do mestre.** Se este aparelho é jogador na campanha ativa, o atalho do Bestiário some, a tela mostra um aviso e a Mesa entra como jogador (sem criaturas, encontros nem controle de combate).

## Importante
- **Hoje todo mundo é mestre das campanhas que cria**, então no seu uso do dia a dia a visibilidade e as travas de jogador só aparecem de verdade na etapa 7, quando um segundo celular entrar numa campanha como jogador.
- Fichas novas passam a gravar quem as criou (`ownerUserId`). Fichas antigas ficam sem dono e valem como do aparelho que as tem.
- Edição livre de atributo e Definir nível livre são **de interface** (decisão 10 do plano): a revalidação pelo celular do mestre chega na etapa 7. Um valor digitado na edição livre é recalculado se a criação for reaberta (a base, os bônus e os aumentos prevalecem).
- Migração: nenhuma. Campos novos opcionais (`ownerUserId`, `asiGrants`); fichas antigas abrem como antes.

## Código
- `data/MasterRules.kt` (novo, Kotlin puro): `canUseFreeEdit`, `attributesLockedFor`, `freeSetLevel`, `AsiGrant` + `grantExtraAsi`/`completeAsiGrant`/`revokeAsiGrant`, `canViewSheet`/`visibleSheets`, `masterToolsAvailable`.
- `CharacterSheet.asiGrants` e `CharacterSheet.ownerUserId` (campos novos, opcionais, no JSON); `attributeImprovements` soma níveis e concessões; `canReopenCreation` passa a usar `isCampaignMaster`.
- Telas: `ui/characters/MasterTools.kt` (novo), `CharacterSheetScreen.kt`, `ui/campaign/CampaignScreen.kt`, `ui/combat/EngineScreen.kt`, `ui/library/LibraryScreen.kt`, `ui/RpgApp.kt`, `ui/components/MasterOnly.kt` (novo).
- Teste novo: `MasterRulesTest` (26 testes: edição livre, nível livre de 1 a 20 e de volta, concessão/teto 20/desfazer exato, interação com reabrir criação e remover nível, visibilidade).
- **Verificação:** as regras, o motor e a suíte inteira (298 testes, 26 novos) foram compilados e executados na JVM e passam. A leitura e gravação das fichas com os campos novos foi conferida. **As telas (Compose) não foram compiladas** aqui, sem o Android SDK: rode o build no GitHub/Android Studio antes de instalar.

## Próxima etapa
Etapa 7 (0.10.6): conexão da campanha com a mesa em rede (código unificado, envio da ficha do jogador, liberação de level up, revalidação no celular do mestre).
