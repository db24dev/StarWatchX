#!/usr/bin/env bash
# STARWATCH-X convenience script:
#  * Creates/uses a virtual environment
#  * Installs ML dependencies
#  * Trains YOLOv8
#  * Exports the ONNX model for the Java engine

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${PROJECT_ROOT}/.venv"
PY_BIN="python3"

# Print helper
log() {
  printf "\n[%s] %s\n" "STARWATCH-X" "$*"
}

# Ensure Python is available
if ! command -v "${PY_BIN}" >/dev/null 2>&1; then
  echo "Error: python3 is required but not found in PATH." >&2
  exit 1
fi

# Create venv if missing
if [ ! -d "${VENV_DIR}" ]; then
  log "Creating virtual environment at ${VENV_DIR}"
  "${PY_BIN}" -m venv "${VENV_DIR}" || {
    echo "Failed to create virtual environment." >&2
    exit 1
  }
fi

# Activate venv
# shellcheck source=/dev/null
source "${VENV_DIR}/bin/activate"

# Upgrade pip and install dependencies
log "Installing dependencies (ultralytics, torch, onnx, onnxruntime)"
pip install --upgrade pip
pip install ultralytics torch torchvision onnx onnxruntime

# Run training
log "Starting YOLO training run"
python "${PROJECT_ROOT}/train_yolo.py" || {
  echo "Training failed. See output above." >&2
  exit 1
}

# Export ONNX
log "Exporting ONNX model"
python "${PROJECT_ROOT}/export_onnx.py" || {
  echo "ONNX export failed. See output above." >&2
  exit 1
}

log "STARWATCH-X ONNX model generated successfully."
echo "STARWATCH-X ONNX model generated successfully."

