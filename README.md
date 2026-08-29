# Mayhem Stage

Projeto de concerto dentro do Minecraft Java Edition. A ideia e montar um show
completo no mapa: palco de teatro barroco, telao, passarela com bordas de LED,
luzes sincronizadas com a musica e publico entrando por multiplayer. Este
repositorio guarda o palco, que e a primeira fase.

O castelo da fachada nao e feito de blocos. Ele e um modelo 3D de 300 mil
triangulos que o mod desenha direto na cena, ancorado no palco, com bake de
cor e oclusao por vertice. Blocos ficam por conta do chao, da passarela e do
telao, que sao as partes onde alguem pisa ou que viram tela depois.

Alvo: Minecraft **26.2** com Fabric. Escala do cenario: 3 blocos por metro,
com o palco real de 15,2 m virando 45 blocos de altura.

## Como rodar

1. Abra o Minecraft 26.2 uma vez no launcher para baixar o jar do cliente.
2. Gere o `pack.mcmeta` do datapack:

   ```bash
   ./tools/set_pack_format.sh 26.2
   ```

3. Crie um mundo superflat criativo sem estruturas.
4. Instale o datapack no save:

   ```bash
   ./tools/install.sh "NomeDoMundo"
   ```

5. Compile o mod e copie para a pasta `mods/` do seu launcher com Fabric
   Loader e Fabric API instalados:

   ```bash
   cd mod && ./gradlew build
   ```

6. No jogo:

   ```mcfunction
   /reload
   /function mayhem:build_blockout
   ```

   Para reconstruir depois de mudar algo, rode antes
   `/function mayhem:demolish`, que limpa somente o envelope do palco.

O castelo aparece sozinho quando o mod esta carregado. Ele ainda nao tem
colisao, entao da para atravessar andando.

## Estrutura

```
datapack/   palco em blocos: deck, passarela, plintos e telao
mod/        mod Fabric: materiais do palco e renderer do castelo 3D
tools/      geradores do blockout, texturas e pipeline do modelo no Blender
sources/    arquivos de trabalho pesados, fora do git
```

A planta tecnica com medidas e decisoes esta em
[MAYHEM_STAGE_V01.md](MAYHEM_STAGE_V01.md).

## Sobre os assets

O modelo do castelo foi gerado no Meshy a partir de referencias visuais do
cenario e processado no Blender: decimacao, bake de cor do modelo denso para
cor por vertice e bake de oclusao. As texturas dos blocos sao desenhadas por
script neste repositorio. Nada aqui usa material oficial de artista ou
gravadora, e musica nao acompanha o projeto.
