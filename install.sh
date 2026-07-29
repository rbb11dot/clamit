#!/bin/bash
set -euo pipefail

REPO="rbb11dot/clamit"
TAG="continuous"
BASE="https://github.com/$REPO/releases/download/$TAG"

# Detect platform
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$OS" in
  linux)
    case "$ARCH" in
      aarch64|arm64)  FILE="clamit-linux-arm64.tar.gz" ;;
      x86_64|amd64)   FILE="clamit-linux-amd64.tar.gz" ;;
      *)              echo "Unsupported architecture: $ARCH"; exit 1 ;;
    esac
    ;;
  darwin)
    case "$ARCH" in
      arm64)          FILE="clamit-darwin-arm64.tar.gz" ;;
      x86_64)         FILE="clamit-darwin-amd64.tar.gz" ;;
      *)              echo "Unsupported architecture: $ARCH"; exit 1 ;;
    esac
    ;;
  mingw*|msys*|cygwin*)
    FILE="clamit-windows-amd64.tar.gz"
    ;;
  *)
    echo "Unsupported OS: $OS"
    echo "Download manually: $BASE"
    exit 1
    ;;
esac

echo "=== clamit install ==="
echo "Platform: $OS $ARCH"
echo "Downloading: $FILE"

# Download
curl -fL "$BASE/$FILE" -o "/tmp/$FILE"
tar xzf "/tmp/$FILE" -C /tmp

BINARY="/tmp/${FILE%.tar.gz}"
chmod +x "$BINARY"

# Install path
INSTALL_DIR="${CLAMIT_HOME:-$HOME/.local/bin}"
mkdir -p "$INSTALL_DIR"
mv "$BINARY" "$INSTALL_DIR/clamit"

# Also download APK if on Termux/Android
if [ "$OS" = "linux" ] && [ -d /data/data/com.termux ]; then
  echo "Detected Termux. Downloading APK..."
  curl -fL "$BASE/app-debug.apk" -o "$HOME/storage/downloads/clamit.apk" 2>/dev/null || true
fi

echo ""
echo "Installed to: $INSTALL_DIR/clamit"
echo "Make sure $INSTALL_DIR is in your PATH."
echo ""
echo "Run: clamit"
