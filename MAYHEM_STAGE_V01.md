# Mayhem Stage V03: planta tecnica do blockout giga

Palco de concerto de estetica teatral barroca para Minecraft Java Edition.
Alvo: **26.2** (release, `pack_format` 107). Escala do V03: **3 blocos = 1 metro**
(o palco construido tem 3x o tamanho real; a fachada mede 129 blocos). Foi o
caminho para caber detalhe de verdade em bloco: coluna 2x2 com base e capitel,
balaustrada de postes individuais, cornija em balanco e brasao com escudo,
volutas e querubins. Um jogador de 1,8 bloco equivale a 60 cm nessa escala.

A V01 tinha uma fachada plana com colunas soltas na frente. As referencias
fotograficas mostraram outra coisa: o castelo real sao **tres pecas
esculturais** encostadas no telao, sem parede continua. A V02 refaz o castelo
inteiro; deck, telao e passarela ficaram como estavam.

## Sistema de coordenadas

`(0, 0, 0)` e o centro da frente do deck, no piso acabado. `+Z` backstage,
`-Z` passarela/publico, `+X` stage right visto da plateia. A conversao para o
mundo e uma linha unica em `build_blockout.mcfunction` (ancora `Y -59`,
superflat com grama em `Y -61`). `demolish.mcfunction` usa a mesma ancora.

## O castelo (tres pecas)

Gerado por `tools/gen_castle.py`. **Nao editar os `.mcfunction` gerados a mao**;
mudar o gerador e rodar de novo.

| Peca | Arquivo | X | Y | Z |
| --- | --- | --- | --- | --- |
| Pavilhao esquerdo | `castle_pavilion_left` | -21..-8 | 0..11 | 5..9 |
| Arco central | `castle_arch` | -8..+8 | 0..16 | 8..12 |
| Pavilhao direito | `castle_pavilion_right` | +8..+21 | 0..11 | 5..9 |

**Pavilhoes**: frente em arco de circulo (corda 13, flecha 4, raio 7.3), dois
niveis de camarote. Faixas horizontais entalhadas em `Y 1`, `5` e `9`; colunas
`quartz_pillar` em `dx = -6, -3, 0, +3, +6` seguindo a curva; guarda-corpo
`diorite_wall` entre elas; cornija de `quartz_stairs` invertida em balanco;
acroterios nos cantos. Interior: pisos e fundo em `polished_blackstone`,
trelica vertical de `polished_blackstone_wall`, como as torres de andaime que
aparecem dentro dos camarotes reais. O direito e espelho exato do esquerdo
(verificado voxel a voxel pelo proprio check).

**Arco central**: vao de 9 de largura por 10 de altura (`X -4..+4`, `Y 0..9`),
ombreiras macicas de 3 de profundidade com pilastras e moldura entalhada,
stairs invertidos nos cantos superiores sugerindo o arco, entablamento e
cornija em `Y 11..12` atravessando as emendas (`X -8..+8`). Brasao em
`Y 13..16`: escudo dourado, volutas de stairs e figura no topo. Cortina de
fios: paineis de vidro isolados (um sim, um nao) em `Z 10`, tunel preto com
fundo em `Z 12`.

## Modulos que vieram da V01

| Modulo | Arquivo | X | Y | Z |
| --- | --- | --- | --- | --- |
| Deck principal | `deck` | -24..+24 | -1 | 0..17 |
| Plintos (2x) | `plinths` | -8..-6 e +6..+8 | 0..1 | 1..3 |
| Telao e moldura | `led_wall` | -31..+31 | -1..19 | 18..19 |
| Passarela reta | `runway` | -4..+4 | -1 | -1..-28 |
| Cabeca da passarela | `runway` | -11..+11 | -1 | -28..-43 |

Os plintos mudaram: eram pretos e afastados; nas fotos sao caixas de marfim
junto a boca da passarela.

## Regra de construcao e a excecao

O build **so adiciona blocos**: nenhum `fill` de ar em nenhum modulo. A
excecao explicita e `mayhem:demolish`, que limpa o envelope do palco
(`X -32..32`, `Y -1..19`, `Z -44..20`, uma camada por comando) para permitir
reconstruir. E a unica funcao destrutiva do pack e nao toca o terreno: o chao
do superflat fica abaixo de `Y -1`.

Fluxo de iteracao no jogo:

```mcfunction
/reload
/function mayhem:demolish
/function mayhem:build_blockout
```

## Paleta provisoria

| Papel | Bloco |
| --- | --- |
| Marfim estrutural | `smooth_quartz` |
| Faixa entalhada / acroterio | `chiseled_quartz_block` |
| Coluna | `quartz_pillar[axis=y]` |
| Guarda-corpo | `diorite_wall` |
| Cornija | `quartz_stairs` invertida |
| Interior dos camarotes | `polished_blackstone` + `_wall` |
| Piso preto / tunel | `black_concrete` |
| Cortina de fios | `white_stained_glass_pane` |
| Escudo do brasao | `gold_block` |
| Line art do telao | `white_concrete` |
| Bordas e luzes | `red_concrete` |

Vermelho e branco ainda nao sao emissivos; emissao vem na fase do mod Fabric.

## O que o blockout ainda nao resolve

- Ornamento de baixo relevo (textura/normal map na fase de props 3D).
- Anjos, cariatides e o entalhe fino do brasao: aqui sao sugestoes de 1 bloco.
- Moving heads, haze, laser, elevador, NPC e qualquer sincronia de show.
