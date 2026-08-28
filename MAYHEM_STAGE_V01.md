# Mayhem Stage V01: planta tecnica do blockout

Palco de concerto de estetica teatral barroca para Minecraft Java Edition.
Alvo: **26.2** (release de 16/06/2026, runtime Java 25).
Escala: **1 bloco = 1 metro**.

## Sistema de coordenadas

O ponto `(0, 0, 0)` do palco e o centro da **frente** do deck, no piso acabado,
ou seja, no plano onde o jogador pisa.

- `+Z` vai para o backstage.
- `-Z` segue pela passarela ate o publico.
- `+X` e stage right para quem olha o palco da plateia.
- `Y 0` e o piso acabado. Os blocos do deck ficam em `Y -1`.

O datapack inteiro e escrito nessas coordenadas de palco. A conversao para o
mundo acontece numa unica linha, em `build_blockout.mcfunction`:

```mcfunction
execute positioned 0.0 -59.0 0.0 run function mayhem:blockout/all
```

Ancora `-59` porque num superflat padrao a grama fica em `Y -61`. O deck assenta
em cima dela, em `Y -60`, e o jogador pisa em `Y -59`. Nenhum bloco do mundo e
substituido. Para mover o palco de lugar, mude so essa linha.

## Modulos e volumes

| Modulo | Arquivo | X | Y | Z |
| --- | --- | --- | --- | --- |
| Deck principal | `blockout/deck` | -24..+24 | -1 | 0..17 |
| Fachada | `blockout/facade` | -21..+21 | 0..13 | 9..11 |
| Portal e cortina | `blockout/portal` | -7..+7 | 0..9 | 9..11 |
| Varanda esquerda | `blockout/balconies` | -21..-9 | 0..8 | 5..8 |
| Varanda direita | `blockout/balconies` | +9..+21 | 0..8 | 5..8 |
| Colunas (4x, 2x2) | `blockout/columns` | eixos -20/-12/+11/+19 | 0..11 | 2..4 |
| Plintos (2x) | `blockout/plinths` | -17..-14 e +14..+17 | 0..2 | 1..4 |
| Telao e moldura | `blockout/led_wall` | -31..+31 | -1..19 | 18..19 |
| Passarela reta | `blockout/runway` | -4..+4 | -1 | -1..-28 |
| Cabeca da passarela | `blockout/runway` | -11..+11 | -1 | -28..-43 |

### Onde os numeros mudaram, e por que

- **Deck 49 blocos, nao 48.** Largura par nao tem coluna central. Com 49 existe
  um eixo verdadeiro em `X 0`, que a passarela, o portal e o brasao usam.
- **Telao 63 blocos, nao 62.** Mesmo motivo, e assim ele fica centrado no deck.
- **Portal com 15 blocos de largura.** Mesma regra de simetria: `X -7..+7`.
- **Varandas em `X -21..-9`.** Isso concilia a tabela original (13 m de largura)
  com o diagrama, que trazia a borda interna em `-9`.
- **Varanda com 9 m de altura incluindo o guarda-corpo.** Nivel 1 pisa em `Y 4`
  com peitoril em `Y 5`; nivel 2 pisa em `Y 7` com peitoril em `Y 8`.

## Regra de construcao

O datapack **so adiciona blocos**. Nao existe um unico `fill` de ar, nem
`setblock air`, nem limpeza de area. O portal nasce vazado porque a fachada e
montada em tres pecas ao redor do vao, nao porque alguma coisa foi escavada.

Existem tres sobreposicoes, todas intencionais, e a ordem de execucao em
`blockout/all` decide quem vence em cada uma:

1. **Cabeca da passarela.** A elipse e montada em vermelho e depois recebida por
   dentro em preto. Isso produz a borda vermelha de 1 bloco em todo o contorno
   com 20 comandos em vez de centenas.
2. **Ombreiras do portal.** `portal` roda depois de `facade` e troca as duas
   colunas de fachada em `X -8` e `X +8` por ornamento, 30 blocos de cada lado.
3. **Pe da varanda.** `balconies` roda depois de `facade` e cobre 13 blocos do
   embasamento em `Y 0, Z 8` de cada lado, que e a emenda entre a varanda e a
   fachada.

Fora essas tres, nenhum modulo escreve por cima de outro. Isso foi verificado
comparando os 80 volumes de `fill` par a par.

## Paleta provisoria de blocos

Blocos de blockout, nao acabamento. Cada um vira prop ou textura depois.

| Papel no projeto | Bloco provisorio |
| --- | --- |
| Marfim da fachada | `smooth_quartz` |
| Cornija e ornamento | `chiseled_quartz_block` |
| Coluna canelada | `quartz_pillar[axis=y]` |
| Piso preto brilhante | `black_concrete` |
| Friso preto fosco | `polished_blackstone` |
| Cortina do portal | `white_stained_glass` |
| Line art do telao | `white_concrete` |
| Borda da passarela e luzes | `red_concrete` |
| Brasao | `gold_block` |

O vermelho e o branco ainda **nao sao emissivos**. Emissao de verdade vem com o
mod, na fase de render no cliente, nao com bloco de luz vanilla.

## O que este blockout ainda nao resolve

- Ornamento de baixo relevo. Fica em textura e normal map, nao em geometria.
- Curvatura real das varandas. Aqui elas sao retas.
- Moving heads, haze, laser, elevador e NPC dancarino.
- Qualquer sincronia de show. Cues, relogio de servidor e render de cliente
  entram so na fase do mod Fabric.
