# Plano da Beta 0.3.0

## Incluído nesta iteração
- Persistência local do HP de demonstração.
- Persistência local do sistema personalizado (nome, descrição, atributos, recursos, perícias, dados e fórmulas).
- Instruções de build e roteiro de teste.

## Bloqueios para uma beta multiusuário
1. Adicionar Room e camada Repository.
2. Definir autenticação e recuperação de conta.
3. Configurar Firebase Authentication e Firestore.
4. Criar regras de segurança: mestre administra campanha; jogador edita apenas personagens autorizados.
5. Criar sincronização com fila offline, timestamps e resolução de conflitos.
6. Criar motor de regras com parser restrito (não executar código arbitrário).
7. Implementar CRUD e testes automatizados.
8. Gerar APK debug, testar em dispositivos reais e só depois preparar assinatura de release.

## Nota de escopo
A beta 0.3.0 é uma primeira versão local de interface e interação. Não deve ser apresentada como produto multiusuário pronto: login, nuvem e compartilhamento ainda não estão implementados.
