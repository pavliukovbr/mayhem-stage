# Mayhem Stage

Palco de concerto para Minecraft Java Edition, versao alvo **26.2**.
A planta tecnica esta em [MAYHEM_STAGE_V01.md](MAYHEM_STAGE_V01.md).

## Rodar o blockout

1. Abra o Minecraft **26.2** uma vez no launcher. Isso baixa o jar do cliente,
   de onde sai o `pack_format` correto.
2. Gere o `pack.mcmeta`:

   ```bash
   ./tools/set_pack_format.sh 26.2
   ```

3. Crie um mundo **superflat, criativo, sem estruturas**. O blockout assume a
   grama do superflat em `Y -61`.
4. Instale o datapack no save:

   ```bash
   ./tools/install.sh "NomeDoMundo"
   ```

5. No jogo:

   ```mcfunction
   /reload
   /function mayhem:build_blockout
   ```

   Para reconstruir depois de mudar o gerador ou os modulos:
   `/function mayhem:demolish` e depois `/function mayhem:build_blockout`.

Voce vai aparecer perto de `X 0, Z 0`, que e a boca de cena. Olhe para `+Z` para
ver a fachada e para `-Z` para ver a passarela.

## Estrutura

```
datapack/                    o datapack instalavel
  pack.mcmeta.template       vira pack.mcmeta com o numero real da versao
  data/mayhem/function/
    build_blockout.mcfunction   ancora do palco no mundo, uma linha so
    demolish.mcfunction         limpa o envelope do palco para reconstruir
    blockout/                   um arquivo por modulo do palco
tools/gen_castle.py          gera os modulos do castelo; nao editar os gerados
tools/set_pack_format.sh     le o pack_format do jar da versao
tools/install.sh             copia o datapack para um save
sources/                     material de referencia, nao alterar, fora do git
```

## Mod Fabric (mod/)

O mod `mayhem-show` e quem transforma o mapa em concerto: relogio de show no
servidor, cues por musica em JSON, luzes, telao e efeitos renderizados no
cliente. Alvo: Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.158.0+26.2,
Java 25. A era 26.x vem sem ofuscacao, entao o build nao tem linha de
mappings.

```bash
cd mod && JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./gradlew build
```

`./gradlew runClient` abre um cliente de teste com o mod carregado.

## Estado

Blockout giga V03 no datapack (3 blocos = 1 m) e esqueleto do mod compilando.
Ainda nao existem modelos Blockbench nem o sistema de cues; e o proximo passo.

## Licenca e assets

Nada de musica, video, logo ou arte protegida entra neste repositorio. A versao
publica usa material original ou licenciado.
