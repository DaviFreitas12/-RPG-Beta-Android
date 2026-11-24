# 0.9.17 · Mesa ágil

Foco: jogo presencial. Menos toques e menos rolagem de tela pro mestre.

- **A mesa em rede não para mais** quando o mestre troca de aba. O estado do combate agora vive em `CombatController` (antes ficava dentro da tela).
- Aba "Dados" virou **Mesa**. Abre direto no Combate.
- **Vez atual no topo**, já aberta, com as ações da criatura prontas ("Cimitarra: +4 acerto · 1d6+2 cortante"). Abaixo, os próximos na ordem de turno.
- **Próximo turno e Desfazer fixos no rodapé.** Desfazer volta o último dano, cura ou remoção.
- **Iniciativa digitada direto na linha** (sem diálogo). A lista reordena ao confirmar. Criaturas do mesmo tipo acompanham a primeira iniciativa digitada.
- Ninguém rola iniciativa no app: personagens e criaturas entram com "INI ?" (dado físico).
- **Encontros preparados**: monte antes da sessão (ou salve a luta atual) e inicie com um toque.
- **Teclado grande** para dano/cura ("Outro valor…") e histórico curto de PV ("Goblin 2: −5 PV (12 → 7)").
- Jogador: celular **vibra na vez dele**, botão **"Terminei meu turno"** e **modo simples** (só PV e turno, sem calculadora de ações).
- Jogador que o mestre já tinha adicionado assume a própria linha ao conectar (não duplica).
- Home: arena animada fora do caminho; Character Studio virou link discreto.

Conexão continua por IP + código.
