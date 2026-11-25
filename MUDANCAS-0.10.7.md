# 0.10.7 · Fichas dos jogadores chegam ao mestre

Na 0.10.6 o jogador entrava na campanha, mas as fichas que ele criava ficavam só no celular dele. Agora elas vão para o mestre.

## Como funciona
- **Jogador**: ao sair da lista de Fichas, o app manda as fichas da campanha ao mestre (em silêncio, se a mesa dele estiver aberta e na mesma rede). Em Campanha > Membros há o botão **Enviar minhas fichas ao mestre**, que mostra o resultado.
- **Mestre**: a ficha aparece em Fichas, dentro da campanha, com o jogador como dono. Se a lista de Fichas estiver aberta, ela se atualiza sozinha.
- Para quem já tinha entrado na 0.10.6: entre de novo na campanha (Entrar em campanha), porque o endereço e o código passam a ser guardados nesse momento.

## Limites desta versão
- O envio é **de ida só**: jogador > mestre. O que o mestre mudar na cópia dele (subir nível, atributo extra, edição livre) **não volta** para o jogador, e um novo envio do jogador **sobrescreve** a cópia do mestre. Até existir sincronização nos dois sentidos, as mudanças do mestre na ficha de um jogador podem se perder.
- A foto da ficha não é enviada.
- O mestre precisa estar com a mesa aberta, e os dois na mesma rede Wi-Fi.
- **A interface não foi compilada** nesta análise.
