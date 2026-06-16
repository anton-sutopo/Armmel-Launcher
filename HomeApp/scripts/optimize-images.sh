#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
printf "Checking for image optimization tools...\n"
if command -v optipng >/dev/null 2>&1; then
  printf "Running optipng on drawable PNGs...\n"
  find src/main/res -type f -name "*.png" -print0 | xargs -0 optipng -o3 || true
elif command -v pngquant >/dev/null 2>&1; then
  printf "Running pngquant on drawable PNGs (will create -fs8 output)...\n"
  find src/main/res -type f -name "*.png" -print0 | xargs -0 -I {} pngquant --force --ext .png 256 "{}" || true
else
  printf "No PNG optimizers (optipng/pngquant) found. Install optipng or pngquant to auto-optimize images.\n"
fi

if command -v cwebp >/dev/null 2>&1; then
  printf "Converting PNGs to WebP (lossless) where beneficial...\n"
  for f in $(find src/main/res -type f -name "*.png"); do
    out="${f%.*}.webp"
    cwebp -lossless "$f" -o "$out" >/dev/null 2>&1 || true
  done
else
  printf "cwebp not found. Skipping WebP conversion.\n"
fi

printf "Done. Review changes and test the app.\n"
