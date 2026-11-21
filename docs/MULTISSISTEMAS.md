# Arquitetura multissistemas

## Princípio
A campanha escolhe o sistema (`systemId`). Personagens e monstros carregam o mesmo identificador, e a ficha é montada com base em uma definição declarativa do sistema. Não se deve presumir que todos os sistemas tenham seis atributos, níveis, classe, CA ou PV.

## Componentes de domínio
- `RuleSystemDefinition`: nome, descrição, atributos, recursos, dados e capacidades.
- `AttributeDefinition`: chave estável, rótulo, abreviação, limites e configuração de modificador.
- `ResourceDefinition`: recursos variáveis (PV, vitalidade, sanidade, mana etc.).
- `DiceDefinition`: dados aceitos pelo sistema.
- `CampaignSystemSettings`: sistema escolhido e regras da casa da campanha.
- `customFields`: extensão de ficha sem alterar o modelo central para cada sistema.

## Perfis iniciais
- `dnd5e`: seis atributos, modificador de atributo, PV, dados de vida e CA.
- `dnd2024`: perfil independente para acomodar regras da edição.
- `vampire`: atributos e recursos diferentes, sem assumir nível ou CA.
- `custom`: permite que o mestre defina a ficha da mesa.
- `generic`: ponto de partida neutro.

## Próximos passos técnicos
1. Criar editor de sistema próprio (atributos, recursos, perícias, dados e campos).
2. Implementar adaptadores de cálculo isolados por sistema e testes unitários.
3. Persistir definições e versões de regras no Room e Firestore.
4. Ao alterar uma definição, versionar o esquema para preservar fichas antigas.
5. Implementar importador do JSON legado e mapear campos ausentes sem descartar os valores originais.

Os perfis atuais são estruturas iniciais, não implementações completas das regras oficiais de cada jogo.
