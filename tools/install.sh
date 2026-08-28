#!/usr/bin/env bash
# Instala o datapack num save. Uso: tools/install.sh "NomeDoMundo"
set -euo pipefail
WORLD="${1:?uso: tools/install.sh \"NomeDoMundo\"}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SAVE="$HOME/Library/Application Support/minecraft/saves/$WORLD"

[ -f "$ROOT/datapack/pack.mcmeta" ] || { echo "ERRO: rode tools/set_pack_format.sh antes."; exit 1; }
[ -d "$SAVE" ] || { echo "ERRO: mundo nao encontrado em $SAVE"; echo "Mundos disponiveis:"; ls "$HOME/Library/Application Support/minecraft/saves" 2>/dev/null; exit 1; }

DEST="$SAVE/datapacks/mayhem_stage"
mkdir -p "$DEST"
rsync -a --delete --exclude pack.mcmeta.template "$ROOT/datapack/" "$DEST/"
echo "instalado em $DEST"
echo "no jogo: /reload  e depois  /function mayhem:build_blockout"
