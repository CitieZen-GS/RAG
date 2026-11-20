#!/bin/bash
#
# Ajudante Familiar - mantém os scripts de infraestrutura iguais nos 3 repositórios
#
# O scripts/azure-infra.sh (e este próprio script) fica copiado em API-Funcoes,
# API-autentificador e RAG. Este script confere se as cópias estão idênticas ou
# replica a cópia do repositório atual nos outros dois.
#
# Requisito: os 3 repositórios clonados lado a lado, na mesma pasta (ex.: GS/API-Funcoes,
# GS/autentificador, GS/RAG). O nome das pastas não importa: os outros repositórios são
# reconhecidos pelo cabeçalho do azure-infra.sh.
#
# Uso (a partir de qualquer um dos repositórios):
#   bash scripts/sincronizar-infra.sh            confere se as cópias estão iguais (erro se não estiverem)
#   bash scripts/sincronizar-infra.sh --copiar   copia os scripts DESTE repositório para os outros dois

set -Eeuo pipefail
trap 'echo "ERRO na linha $LINENO, o comando falhou: $BASH_COMMAND" >&2' ERR

falhar() {
  echo "ERRO: $1" >&2
  exit 1
}

ARQUIVOS=("azure-infra.sh" "sincronizar-infra.sh")
CABECALHO="# Ajudante Familiar - infraestrutura completa na Azure"   # 1ª linha de comentário do azure-infra.sh
OUTROS_ESPERADOS=2

case "${1:-}" in
  "")         MODO="conferir" ;;
  --copiar)   MODO="copiar" ;;
  *)          falhar "Opção desconhecida: $1. Use sem opção (conferir) ou --copiar." ;;
esac

PASTA_SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ATUAL="$(dirname "$PASTA_SCRIPTS")"
PASTA_RAIZ="$(dirname "$REPO_ATUAL")"

# Outros repositórios: pastas vizinhas com um scripts/azure-infra.sh deste projeto.
# Checar o cabeçalho evita sobrescrever o script de outro projeto que esteja na mesma pasta.
OUTROS_REPOS=()
for repo in "$PASTA_RAIZ"/*/; do
  repo="${repo%/}"
  [ "$repo" = "$REPO_ATUAL" ] && continue
  if grep -qF "$CABECALHO" "$repo/scripts/azure-infra.sh" 2>/dev/null; then
    OUTROS_REPOS+=("$repo")
  fi
done

if [ ${#OUTROS_REPOS[@]} -ne $OUTROS_ESPERADOS ]; then
  MENSAGEM="Esperava $OUTROS_ESPERADOS repositórios vizinhos em $PASTA_RAIZ, achei ${#OUTROS_REPOS[@]}. Clone os 3 lado a lado."
  # Conferir com repositório faltando daria um falso "tudo igual"; copiar para os que existem é seguro.
  [ "$MODO" = "conferir" ] && falhar "$MENSAGEM"
  echo "AVISO: $MENSAGEM" >&2
  [ ${#OUTROS_REPOS[@]} -eq 0 ] && exit 1
fi

DIFERENTES=0
for repo in "${OUTROS_REPOS[@]}"; do
  for arquivo in "${ARQUIVOS[@]}"; do
    origem="$PASTA_SCRIPTS/$arquivo"
    destino="$repo/scripts/$arquivo"
    nome="$(basename "$repo")/scripts/$arquivo"

    if [ "$MODO" = "copiar" ]; then
      cp "$origem" "$destino"
      chmod +x "$destino"
      echo "copiado:   $nome"
    elif cmp -s "$origem" "$destino"; then
      echo "igual:     $nome"
    else
      echo "DIFERENTE: $nome (ou ausente)"
      DIFERENTES=1
    fi
  done
done

echo
if [ "$MODO" = "copiar" ]; then
  echo "Pronto. Faça commit da mudança em cada um dos 3 repositórios."
elif [ "$DIFERENTES" -eq 1 ]; then
  echo "Há cópias diferentes. Veja com (na raiz do repositório): diff scripts/<arquivo> ../<outro repositório>/scripts/<arquivo>"
  echo "Depois, a partir do repositório com a versão certa: bash scripts/sincronizar-infra.sh --copiar"
  exit 1
else
  echo "As cópias estão iguais nos 3 repositórios."
fi
