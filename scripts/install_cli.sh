#!/usr/bin/env bash
# Build the Shipkit local dev CLI (skdev) into the repo root.
# - Builds from source using the vendored CLI at tools/shipkit-cli
# - Outputs the binary into the repo root as ./skdev 

set -euo pipefail

INFO()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
WARN()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
ERROR() { echo -e "\033[1;31m[ERR ]\033[0m  $*"; }

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLI_SRC_DIR="$PROJECT_ROOT/tools/shipkit-cli"
TARGET_BIN="$PROJECT_ROOT/skdev"

if ! command -v go >/dev/null 2>&1; then
  ERROR "Go toolchain is required to build the CLI. Install Go >= 1.22 and re-run."
  echo "Try: sudo pacman -S go   # Arch"
  echo "   or sudo apt-get install golang-go   # Debian/Ubuntu"
  exit 1
fi

INFO "Building Shipkit CLI from source …"
(
  cd "$CLI_SRC_DIR"
  rm -f go.sum || true
  go mod tidy
  go build -o "$TARGET_BIN"
)

chmod +x "$TARGET_BIN"
INFO "Built: $TARGET_BIN"

if command -v bash >/dev/null 2>&1; then
  mkdir -p "$HOME/.bash_completion.d"
  "$TARGET_BIN" completion bash > "$HOME/.bash_completion.d/skdev"
  if ! grep -q "~/.bash_completion.d" "$HOME/.bashrc" 2>/dev/null; then
    echo 'for f in ~/.bash_completion.d/*; do source "$f"; done' >> "$HOME/.bashrc"
  fi
  if ! grep -q "__start_skdev ./skdev" "$HOME/.bash_completion.d/skdev" 2>/dev/null; then
    echo 'complete -o bashdefault -o default -o nospace -F __start_skdev ./skdev' >> "$HOME/.bash_completion.d/skdev"
  fi
  if ! grep -q "__start_skdev $TARGET_BIN" "$HOME/.bash_completion.d/skdev" 2>/dev/null; then
    echo "complete -o bashdefault -o default -o nospace -F __start_skdev $TARGET_BIN" >> "$HOME/.bash_completion.d/skdev"
  fi
  if ! grep -q "alias skdev=\"$TARGET_BIN\"" "$HOME/.bashrc" 2>/dev/null; then
    echo "alias skdev=\"$TARGET_BIN\"" >> "$HOME/.bashrc"
  fi
fi

if command -v zsh >/dev/null 2>&1; then
  mkdir -p "$HOME/.zfunc"
  fpath_entry='fpath+=(~/.zfunc)'
  "$TARGET_BIN" completion zsh > "$HOME/.zfunc/_skdev"
  if ! grep -q "$fpath_entry" "$HOME/.zshrc" 2>/dev/null; then
    echo "$fpath_entry" >> "$HOME/.zshrc"
    echo "autoload -Uz compinit && compinit" >> "$HOME/.zshrc"
  fi
  if ! grep -q "compdef _skdev ./skdev" "$HOME/.zshrc" 2>/dev/null; then
    echo "compdef _skdev ./skdev" >> "$HOME/.zshrc"
  fi
  if ! grep -q "compdef _skdev $TARGET_BIN" "$HOME/.zshrc" 2>/dev/null; then
    echo "compdef _skdev $TARGET_BIN" >> "$HOME/.zshrc"
  fi
  if ! grep -q "alias skdev=\"$TARGET_BIN\"" "$HOME/.zshrc" 2>/dev/null; then
    echo "alias skdev=\"$TARGET_BIN\"" >> "$HOME/.zshrc"
  fi
fi

if command -v fish >/dev/null 2>&1; then
  mkdir -p "$HOME/.config/fish/completions"
  "$TARGET_BIN" completion fish > "$HOME/.config/fish/completions/skdev.fish"
  mkdir -p "$HOME/.config/fish/functions"
  if [ ! -f "$HOME/.config/fish/functions/skdev.fish" ]; then
    cat > "$HOME/.config/fish/functions/skdev.fish" <<EOF
function skdev --wraps $TARGET_BIN --description 'Shipkit local dev CLI'
  "$TARGET_BIN" \$argv
end
EOF
  fi
fi

INFO "Shell completions installed (bash/zsh/fish). Restart your shell or run 'exec \$SHELL'."
INFO "Run './skdev --help' for usage."
