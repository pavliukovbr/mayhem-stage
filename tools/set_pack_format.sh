#!/usr/bin/env bash
# Gera datapack/pack.mcmeta com o pack_format REAL da versao alvo.
# O numero sai de version.json dentro do jar do cliente, que so existe depois
# de voce abrir a versao uma vez no launcher. Nada aqui e chutado.
set -euo pipefail
VERSION="${1:-26.2}"
JAR="$HOME/Library/Application Support/minecraft/versions/$VERSION/$VERSION.jar"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -f "$JAR" ]; then
  echo "ERRO: jar nao encontrado em:"
  echo "  $JAR"
  echo "Abra a versao $VERSION uma vez no launcher e rode este script de novo."
  exit 1
fi

# O formato de version.json ja mudou tres vezes. Na 26.2 ele traz
# pack_version.data_major, mas versoes antigas usavam pack_version.data como
# inteiro ou como objeto {major, minor}. As tres formas sao aceitas aqui.
read -r FMT MINOR <<< "$(unzip -p "$JAR" version.json | python3 -c '
import json,sys
p=json.load(sys.stdin).get("pack_version") or {}
if isinstance(p,dict):
    if "data_major" in p:
        print(p["data_major"], p.get("data_minor",0))
    else:
        v=p.get("data")
        print(*( (v.get("major"),v.get("minor",0)) if isinstance(v,dict) else (v,0) ))
else:
    print(p,0)
')"

case "$FMT" in
  ''|*[!0-9]*) echo "ERRO: nao consegui ler o pack_format (valor lido: '$FMT')"; exit 1 ;;
esac

sed "s/__PACK_FORMAT__/$FMT/" "$ROOT/datapack/pack.mcmeta.template" > "$ROOT/datapack/pack.mcmeta"
echo "pack_format de dados do $VERSION = $FMT.$MINOR (pack.mcmeta usa $FMT)"
echo "escrito em $ROOT/datapack/pack.mcmeta"
