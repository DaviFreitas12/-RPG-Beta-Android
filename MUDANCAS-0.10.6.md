# 0.10.6 · Entrar na campanha pelo código

Corrige o problema "o jogador não consegue entrar na campanha": até a 0.10.5 o código da campanha era só exibido, não havia onde digitá-lo.

## Como usar
**Mestre**
1. Aba **Campanha**: deixe a campanha ativa (a ativa é a que a mesa usa).
2. Aba **Mesa** (Motor) > **Abrir mesa**. O código mostrado é o mesmo código de convite da campanha.
3. Passe ao jogador o **endereço** e o **código**. Em Campanha > Membros aparecem os jogadores que entraram.

**Jogador**
1. Aba **Campanha** > **Entrar em campanha (com código do mestre)**.
2. Digite endereço, código e seu nome > **Entrar**.
3. A campanha fica ativa e os personagens novos nascem nela. Para a luta, use a aba Mesa > **Entrar na mesa** (mesmo endereço e código).

Os dois precisam estar na **mesma rede Wi-Fi** (ou no hotspot do mestre), com a mesa aberta e o app do mestre em primeiro plano.

## O que mudou no código
- `data/CampaignRules.kt`: `CampaignInfo`, `CampaignPlayer`, `campaignFromMaster`, `upsertPlayer`.
- `data/CampaignStore.kt`: `joinRemote` (guarda a campanha do mestre e a ativa), `players`/`addPlayer` (lista de membros do mestre).
- `engine/TableSync.kt`: mensagem `JoinCampaign` (`JOINC|...`), linha `CAMP|...`, `splitAddress`.
- `engine/TableTransport.kt`: o host responde ao pedido de entrada e fecha; `requestCampaign` (jogador); o cliente da mesa também recebe a campanha.
- `ui/combat/TableSession.kt` e `TableLinkScreen.kt`: o código da mesa é o da campanha ativa.
- `ui/campaign/CampaignScreen.kt`: botão e diálogo de entrada, lista de membros.

## Limites desta versão
- Só a campanha (nome, sistema, método de criação e de vida) vai ao jogador. Fichas e inventário continuam em cada aparelho.
- A campanha do jogador é uma cópia: se o mestre mudar algo, o jogador entra de novo para atualizar. Mesa aberta com campanha antiga manda a versão do momento em que foi aberta.
- Sistemas personalizados criados só no aparelho do mestre não existem no do jogador.
- Os dois aparelhos precisam estar na versão 0.10.6.
- **A interface não foi compilada** nesta análise.
