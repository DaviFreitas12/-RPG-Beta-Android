# RPG — análise geral e arquitetura

## Direção do produto
Aplicativo Android para campanhas de RPG de mesa. Mestre e jogadores usam celulares próprios, com contas individuais, permissões por campanha e sincronização. A experiência deve ser offline-first: dados já baixados continuam consultáveis e alterações locais entram em fila para sincronização.

## O que existe nesta entrega
- Catálogo inicial multissistemas com D&D 5e, D&D 2024, Vampiro, Sistema próprio e Outro sistema.
- Seletor visual de sistema na campanha de demonstração.
- Modelos de campanha, configurações de regras, definições de atributos/recursos/dados e campos personalizados.
- Projeto Android inicial em Kotlin + Jetpack Compose.
- Navegação visual entre Campanha, Personagens, Bestiário, Grimório e Itens.
- Identidade visual neutra, com modo claro e escuro (ver `ui/theme/Theme.kt`).
- Modelos Kotlin iniciais para atributos, personagens, monstros, magias, itens e membros.
- Nome de aplicativo configurado como RPG.

## Auditoria: o que ainda falta para produção

### Essencial
1. **Autenticação**: cadastro, login, recuperação de senha e encerramento de sessão.
2. **Campanhas**: criar, editar, arquivar, gerar convite/código e aceitar convite.
3. **Autorização**: regras no servidor para mestre, jogador e convidado. Não confiar apenas em esconder botões na interface.
4. **CRUD completo**: criar, visualizar, editar e excluir personagens, monstros, magias e itens.
5. **Persistência local**: Room, DAOs, migrações e repositórios.
6. **Sincronização**: Cloud Firestore, fila de operações offline, estados pendente/sincronizado/erro e estratégia de conflito.
7. **Segurança**: regras Firestore por membership, validação de propriedade, proteção de dados e política de privacidade.
8. **Importação do JSON existente**: conversão dos campos `hp`, `hpc`, `ac`, `attrs`, `adv`, `inv`, `abil` e demais campos para modelos tipados, sem perder dados.
9. **Testes**: regras de permissão, cálculos, persistência, sincronização e testes em aparelhos de tamanhos diferentes.

### Funcionalidades de jogo recomendadas
- Testes de atributo e rolagem de dados (d4, d6, d8, d10, d12, d20 e d100).
- Modificadores calculados a partir dos atributos, com regra configurável por sistema.
- Combate: iniciativa, ordem de turno, dano, cura, condições e controle de HP.
- Ficha: proficiência, perícias, resistências, ataques, habilidades, recursos e descanso.
- Inventário: quantidade, peso, equipamento, bônus e transferência.
- Grimório: filtros por nível/escola, preparação e espaços de magia, caso o sistema use esses conceitos.
- Histórico de alterações e registro da sessão.

## Arquitetura alvo
- **UI**: Jetpack Compose + Navigation Compose + ViewModels/StateFlow.
- **Domínio**: casos de uso para campanha, ficha, combate e biblioteca.
- **Dados locais**: Room como fonte de leitura offline.
- **Nuvem**: Firebase Authentication + Cloud Firestore.
- **Sincronização**: repositórios offline-first; operações locais com estado `PENDING`, envio quando houver rede e confirmação `SYNCED`.

### Modelo de dados sugerido
- `users/{uid}`: perfil básico.
- `campaigns/{campaignId}`: nome, descrição, criador, datas.
- `campaigns/{campaignId}/members/{uid}`: papel (`MASTER`, `PLAYER`, `GUEST`) e estado do convite.
- `campaigns/{campaignId}/characters/{characterId}`: ficha e `ownerUserId`.
- `campaigns/{campaignId}/monsters/{monsterId}`: criaturas.
- `campaigns/{campaignId}/spells/{spellId}`: magias.
- `campaigns/{campaignId}/items/{itemId}`: itens.
- `campaigns/{campaignId}/sessions/{sessionId}`: sessões e registros.

### Regras de autorização esperadas
- Apenas membros podem ler os dados privados de uma campanha.
- Apenas o mestre pode alterar configurações da campanha e biblioteca compartilhada, salvo permissões delegadas.
- Jogador pode editar a própria ficha; o mestre pode editar fichas da campanha.
- Convidado só lê os dados explicitamente compartilhados.
- Convites devem expirar ou poder ser revogados.

## Suporte a vários sistemas — decisão definida
O aplicativo será multissistemas. Cada campanha escolhe seu próprio `systemId`; uma conta pode participar de campanhas com sistemas diferentes.

### Catálogo inicial modelado
- D&D 5e: seis atributos clássicos, modificadores configuráveis, níveis, CA e PV.
- D&D 2024: perfil separado para acomodar diferenças da revisão.
- Vampiro: conjunto de atributos e trilhas de recursos diferente, sem presumir nível ou classe.
- Sistema próprio: atributos, recursos, dados e campos personalizados.
- Outro sistema: ficha neutra que poderá ser configurada.

### Regras de arquitetura
- `RuleSystemDefinition` descreve atributos, recursos, dados e capacidades do sistema.
- `CampaignSystemSettings` guarda o sistema e regras da casa por campanha.
- Personagens e monstros guardam `systemId` e `customFields` para extensões sem migração de colunas a cada sistema novo.
- Cálculos devem ser executados por módulos/adaptadores de regras, nunca presumidos globalmente pela interface.
- Os perfis D&D/Vampiro nesta etapa são modelos iniciais de ficha; não incluem ainda implementação integral, textos ou conteúdo oficial dos livros.

### Decisões ainda pendentes para produção
- Jogadores podem editar livremente a ficha ou algumas alterações precisam de aprovação do mestre?
- Monstros e magias são globais, por campanha ou ambos?
- Quais dados da ficha ficam visíveis aos convidados?
- Quais sistemas e edições terão adaptadores completos primeiro?

## Estado do APK
Esta pasta ainda não é um APK compilado. O ambiente de trabalho usado para preparar o projeto não tem Android SDK/Gradle instalado e não contém as credenciais do Firebase. Para gerar um APK instalável, abra o projeto no Android Studio com Android SDK instalado e configure o Firebase (arquivo `google-services.json`) após criar o projeto no console Firebase. O APK só deve ser considerado pronto depois de compilar e testar em dispositivo real.
